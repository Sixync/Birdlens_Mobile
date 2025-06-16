// app/src/main/java/com/android/birdlens/presentation/viewmodel/MapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.R
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.repository.BirdSpeciesRepository
import com.android.birdlens.data.repository.HotspotRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs


sealed class MapUiState {
    data object Idle : MapUiState()
    data class Loading(val message: String? = null) : MapUiState()
    data class Success(
        val hotspots: List<EbirdNearbyHotspot>,
        val zoomLevelContext: Float,
        val fetchedCenter: LatLng,
        val fetchedRadiusKm: Int
    ) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val hotspotRepository = HotspotRepository(application.applicationContext)
    private val birdSpeciesRepository = BirdSpeciesRepository(application.applicationContext)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val context = application.applicationContext

    private val _searchResults = MutableStateFlow<List<BirdSpecies>>(emptyList())
    val searchResults: StateFlow<List<BirdSpecies>> = _searchResults.asStateFlow()

    private val _isCompareModeActive = MutableStateFlow(false)
    val isCompareModeActive: StateFlow<Boolean> = _isCompareModeActive.asStateFlow()

    private val _selectedHotspotsForComparison = MutableStateFlow<List<EbirdNearbyHotspot>>(emptyList())
    val selectedHotspotsForComparison: StateFlow<List<EbirdNearbyHotspot>> = _selectedHotspotsForComparison.asStateFlow()


    companion object {
        const val SHOW_HOTSPOTS_MIN_ZOOM_LEVEL = 7.5f
        const val HOTSPOT_FETCH_RADIUS_KM = 50
        val VIETNAM_INITIAL_CENTER = LatLng(16.047079, 108.220825)
        const val HOME_BUTTON_ZOOM_LEVEL = 6.0f
        private const val CAMERA_IDLE_DEBOUNCE_MS = 750L
        private const val SIGNIFICANT_PAN_THRESHOLD_DEGREES = 0.25 // Threshold for what's a "significant" move
        const val COUNTRY_CODE_VIETNAM = "VN"
        private const val TAG = "MapViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
        const val MAX_COMPARISON_ITEMS = 3
        private const val MARKER_INTERACTION_LOCK_DURATION_MS = 1500L // How long to suppress refetch after marker click
    }

    private var fetchJob: Job? = null
    private var searchJob: Job? = null
    private var lastFetchedCenter: LatLng? = null
    private var areHotspotsVisible = false
    private var isSearchActive = false

    // Flag to indicate a marker interaction is likely causing map movement
    private var isMarkerInteractionInProgress = false
    private var markerInteractionResetJob: Job? = null


    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                try {
                    val results = birdSpeciesRepository.searchBirds(query)
                    _searchResults.value = results
                    Log.d(TAG, "Local search for '$query' found ${results.size} results.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching for birds in local DB: ${e.message}", e)
                    _searchResults.value = emptyList()
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun onBirdSearchSubmitted() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return

        searchJob?.cancel()
        _searchResults.value = emptyList()
        isSearchActive = true
        fetchJob?.cancel() // Cancel any ongoing general hotspot fetching

        fetchJob = viewModelScope.launch {
            _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_local_finding, query))
            try {
                val searchResults = birdSpeciesRepository.searchBirds(query)
                if (searchResults.isNotEmpty()) {
                    val foundBird = searchResults.first()
                    _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_ai_finding, foundBird.commonName))
                    fetchHotspotsForSpeciesCode(foundBird.speciesCode, foundBird.commonName)
                } else {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_not_found, query))
                    isSearchActive = false // Reset search active if bird not found
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i(TAG, "Local bird search was cancelled.")
                    return@launch
                }
                val errorMsg = "Exception during local bird search: ${e.localizedMessage}"
                _uiState.value = MapUiState.Error(errorMsg)
                isSearchActive = false
                Log.e(TAG, errorMsg, e)
            }
        }
    }

    fun onSearchQueryCleared(currentCenter: LatLng) {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        if (isSearchActive) {
            isSearchActive = false
            fetchJob?.cancel() // Cancel species-specific hotspot fetch
            // Trigger a general hotspot fetch for the current view
            requestHotspotsForCurrentView(currentCenter, (_uiState.value as? MapUiState.Success)?.zoomLevelContext ?: SHOW_HOTSPOTS_MIN_ZOOM_LEVEL, forceRefresh = true)
        }
    }

    // Call this from MapScreen when a marker or info window is tapped
    fun notifyMarkerInteraction() {
        isMarkerInteractionInProgress = true
        Log.d(TAG, "Marker interaction notified. Suppressing refetch temporarily.")
        markerInteractionResetJob?.cancel()
        markerInteractionResetJob = viewModelScope.launch {
            delay(MARKER_INTERACTION_LOCK_DURATION_MS)
            if (isMarkerInteractionInProgress) { // Check if it wasn't cleared by some other logic
                isMarkerInteractionInProgress = false
                Log.d(TAG, "Marker interaction lock auto-expired.")
            }
        }
    }


    fun requestHotspotsForCurrentView(center: LatLng, zoom: Float, forceRefresh: Boolean = false) {
        if (isSearchActive && !forceRefresh) {
            Log.d(TAG, "Search is active. Ignoring general hotspot request. forceRefresh=$forceRefresh")
            return
        }

        // If a marker interaction just happened AND it's not a user-forced refresh,
        // skip this opportunistic fetch to prevent reloading due to map centering on marker.
        if (isMarkerInteractionInProgress && !forceRefresh) {
            Log.d(TAG, "Marker interaction lock is active. Skipping refetch for center: $center, zoom: $zoom.")
            // We don't clear the lock here; let the timer or a significant user pan do it.
            // Update zoom context if needed, but don't fetch.
            (_uiState.value as? MapUiState.Success)?.let {
                if (abs(it.zoomLevelContext - zoom) > 0.1f || it.fetchedCenter != center) { // Only update if context really changed
                    _uiState.value = it.copy(zoomLevelContext = zoom, fetchedCenter = center)
                }
            }
            return
        }


        fetchJob?.cancel()
        Log.d(TAG, "Preparing to fetch. Center: $center, Zoom: $zoom, ForceRefresh: $forceRefresh, MarkerInteraction: $isMarkerInteractionInProgress")

        fetchJob = viewModelScope.launch {
            if (!forceRefresh) {
                delay(CAMERA_IDLE_DEBOUNCE_MS)
            }

            if (!isActive) {
                Log.d(TAG, "Job for center $center, zoom $zoom was cancelled before network processing.")
                return@launch
            }

            if (zoom < SHOW_HOTSPOTS_MIN_ZOOM_LEVEL && !forceRefresh && !isSearchActive) {
                if (areHotspotsVisible || (_uiState.value as? MapUiState.Success)?.hotspots?.isNotEmpty() == true) {
                    _uiState.value = MapUiState.Success(emptyList(), zoom, center, 0) // Clear hotspots
                    Log.d(TAG, "Zoom ($zoom) < threshold. Hiding hotspots. forceRefresh=$forceRefresh")
                }
                areHotspotsVisible = false
                return@launch
            }

            val centerChangedSignificantly = lastFetchedCenter == null || isCenterChangeSignificant(center, lastFetchedCenter!!)
            // View requires update if:
            // - It's a forced refresh.
            // - Hotspots aren't currently visible (e.g., after zooming in).
            // - The center of the map has changed significantly since the last fetch.
            val viewRequiresUpdate = forceRefresh || !areHotspotsVisible || centerChangedSignificantly

            if (viewRequiresUpdate) {
                // If this fetch is happening despite a recent marker interaction (e.g. forceRefresh or significant pan),
                // then the interaction lock should be cleared as the user's intent to refresh/move is overriding.
                if (isMarkerInteractionInProgress) {
                    Log.d(TAG, "Proceeding with fetch despite recent marker interaction due to forceRefresh or significant pan. Clearing lock.")
                    isMarkerInteractionInProgress = false
                    markerInteractionResetJob?.cancel()
                }

                _uiState.value = MapUiState.Loading(if (forceRefresh) context.getString(R.string.map_toast_refreshing_hotspots) else null)
                Log.d(TAG, "Fetching hotspots. Zoom: $zoom, Force: $forceRefresh, ViewRequiresUpdate: $viewRequiresUpdate (areHotspotsVisible=$areHotspotsVisible, centerChanged=$centerChangedSignificantly)")
                try {
                    val fetchedHotspots = hotspotRepository.getNearbyHotspots(
                        center = center,
                        radiusKm = HOTSPOT_FETCH_RADIUS_KM,
                        currentBounds = null,
                        countryCodeFilter = COUNTRY_CODE_VIETNAM,
                        forceRefresh = forceRefresh
                    ).sortedByDescending { it.numSpeciesAllTime ?: 0 }

                    if (!isActive) {
                        Log.d(TAG, "Job for center $center, zoom $zoom was cancelled DURING/AFTER network call.")
                        return@launch
                    }

                    _uiState.value = MapUiState.Success(fetchedHotspots, zoom, center, HOTSPOT_FETCH_RADIUS_KM)
                    areHotspotsVisible = true
                    lastFetchedCenter = center // Update last fetched center
                    Log.d(TAG, "Fetch successful. Showing ${fetchedHotspots.size} hotspots. forceRefresh=$forceRefresh")

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.i(TAG, "Fetch job for $center, zoom $zoom was cancelled (repo level or during VM processing): ${e.message}")
                    } else {
                        val errorMsg = context.getString(R.string.map_fetch_error_generic, e.localizedMessage ?: context.getString(R.string.error_unknown))
                        _uiState.value = MapUiState.Error(errorMsg)
                        Log.e(TAG, "Exception fetching hotspots: $errorMsg", e)
                    }
                }
            } else {
                Log.d(TAG, "View has not changed enough and not forcing refresh. Not re-fetching. Zoom: $zoom. Current center: $center, Last center: $lastFetchedCenter")
                (_uiState.value as? MapUiState.Success)?.let {
                    if (abs(it.zoomLevelContext - zoom) > 0.1f || it.fetchedCenter != center) {
                        _uiState.value = it.copy(zoomLevelContext = zoom, fetchedCenter = center)
                    }
                }
            }
        }
    }

    private fun fetchHotspotsForSpeciesCode(speciesCode: String, birdDisplayName: String) {
        viewModelScope.launch {
            // Keep the specific loading message for species search
            _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_ai_finding, birdDisplayName))
            Log.d(TAG, "Fetching hotspots for speciesCode: '$speciesCode'")
            try {
                val hotspotsWithBird = hotspotRepository.getHotspotsForSpeciesInCountry(
                    speciesCode = speciesCode,
                    countryCode = COUNTRY_CODE_VIETNAM // Ensure this is the desired region
                )

                if (!isActive) {
                    Log.i(TAG, "Species hotspot fetch cancelled for $speciesCode")
                    return@launch
                }

                if (hotspotsWithBird.isNotEmpty()) {
                    // For species search, zoom level and center might be broader initially
                    _uiState.value = MapUiState.Success(hotspotsWithBird, HOME_BUTTON_ZOOM_LEVEL, VIETNAM_INITIAL_CENTER, 0) // Radius 0 indicates it's not a geo-radius fetch
                    areHotspotsVisible = true // Hotspots are now visible
                    lastFetchedCenter = null // Reset last fetched center for general browsing, or set to an average if desired
                } else {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_no_hotspots, birdDisplayName))
                    isSearchActive = false // Reset search if no hotspots found for the species
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i(TAG, "Bird hotspot fetch job for $speciesCode was cancelled: ${e.message}")
                } else {
                    val errorMsg = context.getString(R.string.map_fetch_error_generic, e.localizedMessage ?: context.getString(R.string.error_unknown))
                    _uiState.value = MapUiState.Error(errorMsg)
                    isSearchActive = false
                    Log.e(TAG, "Exception fetching hotspots for species $speciesCode: $errorMsg", e)
                }
            }
        }
    }

    private fun isCenterChangeSignificant(newCenter: LatLng, oldCenter: LatLng): Boolean {
        val latDiff = abs(newCenter.latitude - oldCenter.latitude)
        val lngDiff = abs(newCenter.longitude - oldCenter.longitude)
        val significant = latDiff > SIGNIFICANT_PAN_THRESHOLD_DEGREES || lngDiff > SIGNIFICANT_PAN_THRESHOLD_DEGREES
        if (significant) {
            Log.d(TAG, "Center changed significantly: New=$newCenter, Old=$oldCenter")
        }
        return significant
    }

    fun clearHotspotCache() {
        viewModelScope.launch {
            hotspotRepository.clearAllHotspotsCache()
            lastFetchedCenter = null
            areHotspotsVisible = false
            Log.i(TAG, "Hotspot cache cleared.")
            val currentUiStateValue = _uiState.value
            val currentCenter = (currentUiStateValue as? MapUiState.Success)?.fetchedCenter ?: VIETNAM_INITIAL_CENTER
            val currentZoom = (currentUiStateValue as? MapUiState.Success)?.zoomLevelContext ?: HOME_BUTTON_ZOOM_LEVEL
            requestHotspotsForCurrentView(currentCenter, currentZoom, forceRefresh = true)
        }
    }

    fun toggleCompareMode() {
        _isCompareModeActive.value = !_isCompareModeActive.value
        if (!_isCompareModeActive.value) {
            _selectedHotspotsForComparison.value = emptyList()
        }
        Log.d(TAG, "Compare mode toggled. Active: ${_isCompareModeActive.value}")
    }

    fun onHotspotSelectedForComparison(hotspot: EbirdNearbyHotspot) {
        if (!_isCompareModeActive.value) {
            Log.d(TAG, "Compare mode not active. Ignoring hotspot selection.")
            return
        }
        val currentSelection = _selectedHotspotsForComparison.value.toMutableList()
        val alreadySelected = currentSelection.any { it.locId == hotspot.locId }

        if (alreadySelected) {
            currentSelection.removeAll { it.locId == hotspot.locId }
            Log.d(TAG, "Hotspot deselected: ${hotspot.locName}")
        } else {
            if (currentSelection.size < MAX_COMPARISON_ITEMS) {
                currentSelection.add(hotspot)
                Log.d(TAG, "Hotspot selected: ${hotspot.locName}")
            } else {
                viewModelScope.launch {
                    val errorMsg = context.getString(R.string.map_compare_max_items_toast, MAX_COMPARISON_ITEMS)
                    Log.w(TAG, errorMsg)
                    // Consider a transient error message for the UI if possible
                    // For now, just preventing addition.
                }
            }
        }
        _selectedHotspotsForComparison.value = currentSelection
    }

    fun clearComparisonSelection() {
        _selectedHotspotsForComparison.value = emptyList()
        Log.d(TAG, "Comparison selection cleared.")
    }
}