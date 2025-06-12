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
import kotlinx.coroutines.launch
import kotlin.math.abs


sealed class MapUiState {
    data object Idle : MapUiState()
    data class Loading(val message: String? = null) : MapUiState() // Add message to loading state
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
    private val context = application.applicationContext // For string resources

    // New state for search recommendations
    private val _searchResults = MutableStateFlow<List<BirdSpecies>>(emptyList())
    val searchResults: StateFlow<List<BirdSpecies>> = _searchResults.asStateFlow()

    companion object {
        const val SHOW_HOTSPOTS_MIN_ZOOM_LEVEL = 7.5f
        const val HOTSPOT_FETCH_RADIUS_KM = 50
        val VIETNAM_INITIAL_CENTER = LatLng(16.047079, 108.220825)
        const val HOME_BUTTON_ZOOM_LEVEL = 6.0f
        private const val CAMERA_IDLE_DEBOUNCE_MS = 750L
        private const val SIGNIFICANT_PAN_THRESHOLD_DEGREES = 0.25
        const val COUNTRY_CODE_VIETNAM = "VN"
        private const val TAG = "MapViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L // Debounce for search input
    }

    private var fetchJob: Job? = null
    private var searchJob: Job? = null // Job for debounced search
    private var lastFetchedCenter: LatLng? = null
    private var areHotspotsVisible = false
    private var isSearchActive = false


    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel() // Cancel previous search job
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS) // Debounce to avoid querying on every keystroke
                try {
                    val results = birdSpeciesRepository.searchBirds(query)
                    _searchResults.value = results
                    Log.d(TAG, "Local search for '$query' found ${results.size} results.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching for birds in local DB: ${e.message}", e)
                    _searchResults.value = emptyList() // Clear results on error
                }
            }
        } else {
            _searchResults.value = emptyList() // Clear results if query is blank
        }
    }

    fun onBirdSearchSubmitted() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            return
        }

        searchJob?.cancel()
        _searchResults.value = emptyList() // Hide recommendations after submission

        isSearchActive = true
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_local_finding, query))
            try {
                // Search the local database for the bird
                val searchResults = birdSpeciesRepository.searchBirds(query)

                if (searchResults.isNotEmpty()) {
                    val foundBird = searchResults.first()
                    // Update loading message to show the found bird name
                    _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_ai_finding, foundBird.commonName))
                    fetchHotspotsForSpeciesCode(foundBird.speciesCode, foundBird.commonName)
                } else {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_not_found, query))
                    isSearchActive = false
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
        _searchResults.value = emptyList() // Clear recommendations
        isSearchActive = false
        // Re-fetch all hotspots for the current view to restore normal map behavior
        requestHotspotsForCurrentView(currentCenter, (_uiState.value as? MapUiState.Success)?.zoomLevelContext ?: SHOW_HOTSPOTS_MIN_ZOOM_LEVEL)
    }


    fun requestHotspotsForCurrentView(center: LatLng, zoom: Float) {
        // If a search is active, we must not cancel the ongoing search job.
        // So we exit this function immediately.
        if (isSearchActive) {
            Log.d(TAG, "Search is active. Ignoring requestHotspotsForCurrentView to prevent cancellation of search job.")
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            delay(CAMERA_IDLE_DEBOUNCE_MS)

            if (zoom < SHOW_HOTSPOTS_MIN_ZOOM_LEVEL) {
                if (areHotspotsVisible) {
                    _uiState.value = MapUiState.Success(emptyList(), zoom, center, 0)
                    areHotspotsVisible = false
                    lastFetchedCenter = null
                    Log.d(TAG, "Zoom level ($zoom) is below threshold ($SHOW_HOTSPOTS_MIN_ZOOM_LEVEL). Hiding hotspots.")
                } else {
                    (_uiState.value as? MapUiState.Success)?.let {
                        if (it.hotspots.isEmpty()) {
                            _uiState.value = it.copy(zoomLevelContext = zoom, fetchedCenter = center, fetchedRadiusKm = 0)
                        }
                    }
                }
                return@launch
            }

            val centerChangedSignificantly = lastFetchedCenter == null || isCenterChangeSignificant(center, lastFetchedCenter!!)

            if (!areHotspotsVisible || centerChangedSignificantly) {
                _uiState.value = MapUiState.Loading()
                Log.d(TAG, "Zoom level ($zoom) is sufficient. Fetching hotspots. New view required: ${!areHotspotsVisible}, Center changed: $centerChangedSignificantly")
                try {
                    val fetchedHotspots = hotspotRepository.getNearbyHotspots(
                        center = center,
                        radiusKm = HOTSPOT_FETCH_RADIUS_KM,
                        currentBounds = null,
                        countryCodeFilter = COUNTRY_CODE_VIETNAM
                    ).sortedByDescending { it.numSpeciesAllTime ?: 0 }

                    _uiState.value = MapUiState.Success(fetchedHotspots, zoom, center, HOTSPOT_FETCH_RADIUS_KM)
                    areHotspotsVisible = true
                    lastFetchedCenter = center
                    Log.d(TAG, "Fetch successful. Showing ${fetchedHotspots.size} hotspots.")

                } catch (e: Exception) {
                    if (e is CancellationException) {
                        Log.d(TAG, "Fetch job cancelled for zoom $zoom.")
                        return@launch
                    }
                    val errorMsg = "Exception fetching hotspots: ${e.localizedMessage}"
                    _uiState.value = MapUiState.Error(errorMsg)
                    Log.e(TAG, errorMsg, e)
                }
            } else {
                Log.d(TAG, "View has not changed enough. Not re-fetching.")
                (_uiState.value as? MapUiState.Success)?.let {
                    _uiState.value = it.copy(zoomLevelContext = zoom)
                }
            }
        }
    }

    private fun fetchHotspotsForSpeciesCode(speciesCode: String, birdDisplayName: String) {
        // This function is now private and part of the search flow.
        // It's called after a speciesCode has been successfully found.
        viewModelScope.launch {
            Log.d(TAG, "Fetching hotspots for speciesCode: '$speciesCode'")
            try {
                val hotspotsWithBird = hotspotRepository.getHotspotsForSpeciesInCountry(
                    speciesCode = speciesCode,
                    countryCode = COUNTRY_CODE_VIETNAM
                )

                if (hotspotsWithBird.isNotEmpty()) {
                    _uiState.value = MapUiState.Success(hotspotsWithBird, HOME_BUTTON_ZOOM_LEVEL, VIETNAM_INITIAL_CENTER, 0)
                    areHotspotsVisible = true
                    // Keep isSearchActive = true until the user clears it
                } else {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_no_hotspots, birdDisplayName))
                    isSearchActive = false // Reset search state on failure
                }

            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i(TAG, "Bird hotspot fetch job was cancelled.")
                    return@launch
                }
                val errorMsg = "Exception fetching hotspots for species: ${e.localizedMessage}"
                _uiState.value = MapUiState.Error(errorMsg)
                isSearchActive = false // Reset search state on failure
                Log.e(TAG, errorMsg, e)
            }
        }
    }


    private fun isCenterChangeSignificant(newCenter: LatLng, oldCenter: LatLng): Boolean {
        val latDiff = abs(newCenter.latitude - oldCenter.latitude)
        val lngDiff = abs(newCenter.longitude - oldCenter.longitude)
        return latDiff > SIGNIFICANT_PAN_THRESHOLD_DEGREES || lngDiff > SIGNIFICANT_PAN_THRESHOLD_DEGREES
    }

    fun clearHotspotCache() {
        viewModelScope.launch {
            hotspotRepository.clearAllHotspotsCache()
            lastFetchedCenter = null
            areHotspotsVisible = false
            Log.i(TAG, "Hotspot cache cleared.")
        }
    }
}