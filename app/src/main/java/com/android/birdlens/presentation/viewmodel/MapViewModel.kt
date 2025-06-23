// app/src/main/java/com/android/birdlens/presentation/viewmodel/MapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.R
import com.android.birdlens.data.CountrySetting
import com.android.birdlens.data.UserSettingsManager
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdObservation
import com.android.birdlens.data.repository.BirdSpeciesRepository
import com.android.birdlens.data.repository.HotspotRepository
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.heatmaps.WeightedLatLng
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

data class HotspotSheetDetails(
    val hotspot: EbirdNearbyHotspot,
    val recentSightingsCount: Int,
    val notableBirdImageUrl: String?,
    val isBookmarked: Boolean,
    val speciesList: List<BirdSpeciesInfo> = emptyList()
)

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val hotspotRepository = HotspotRepository(application.applicationContext)
    private val birdSpeciesRepository = BirdSpeciesRepository(application.applicationContext)
    private val userSettingsManager = UserSettingsManager
    private val context = application.applicationContext

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<BirdSpecies>>(emptyList())
    val searchResults: StateFlow<List<BirdSpecies>> = _searchResults.asStateFlow()

    private val _isCompareModeActive = MutableStateFlow(false)
    val isCompareModeActive: StateFlow<Boolean> = _isCompareModeActive.asStateFlow()

    private val _selectedHotspotsForComparison = MutableStateFlow<List<EbirdNearbyHotspot>>(emptyList())
    val selectedHotspotsForComparison: StateFlow<List<EbirdNearbyHotspot>> = _selectedHotspotsForComparison.asStateFlow()

    private val _currentHomeCountrySetting = MutableStateFlow(userSettingsManager.getHomeCountrySetting(context))
    val currentHomeCountrySetting: StateFlow<CountrySetting> = _currentHomeCountrySetting.asStateFlow()

    // For Bottom Sheet
    private val _selectedHotspotDetails = MutableStateFlow<HotspotSheetDetails?>(null)
    val selectedHotspotDetails: StateFlow<HotspotSheetDetails?> = _selectedHotspotDetails.asStateFlow()

    // Map Controls
    val _mapProperties = mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    val mapProperties: androidx.compose.runtime.State<MapProperties> = _mapProperties

    val _mapUiSettings = mutableStateOf(MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false, compassEnabled = true))
    val mapUiSettings: androidx.compose.runtime.State<MapUiSettings> = _mapUiSettings

    private val _isHeatmapVisible = MutableStateFlow(false)
    val isHeatmapVisible: StateFlow<Boolean> = _isHeatmapVisible.asStateFlow()

    private val _heatmapData = MutableStateFlow<List<WeightedLatLng>>(emptyList())
    val heatmapData: StateFlow<List<WeightedLatLng>> = _heatmapData.asStateFlow()

    private val _isMapLocked = MutableStateFlow(false)
    val isMapLocked: StateFlow<Boolean> = _isMapLocked.asStateFlow()

    private val _bookmarkedHotspotIds = MutableStateFlow<Set<String>>(emptySet()) // In-memory for now
    val bookmarkedHotspotIds: StateFlow<Set<String>> = _bookmarkedHotspotIds.asStateFlow()


    companion object {
        const val SHOW_HOTSPOTS_MIN_ZOOM_LEVEL = 5.0f
        const val HOTSPOT_FETCH_RADIUS_KM = 50
        private const val CAMERA_IDLE_DEBOUNCE_MS = 750L
        private const val SIGNIFICANT_PAN_THRESHOLD_DEGREES = 0.3
        private const val TAG = "MapViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
        const val MAX_COMPARISON_ITEMS = 3
        private const val MARKER_INTERACTION_LOCK_DURATION_MS = 1500L
    }

    private var fetchJob: Job? = null
    private var searchJob: Job? = null
    private var lastFetchedCenter: LatLng? = null
    private var areHotspotsVisible = false
    var isSearchActive = false
    private var isMarkerInteractionInProgress = false
    private var markerInteractionResetJob: Job? = null

    val initialMapCenter: LatLng
        get() = _currentHomeCountrySetting.value.center
    val initialMapZoom: Float
        get() = _currentHomeCountrySetting.value.zoom

    var cameraPositionStateHolder: CameraPositionState? = null

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                try {
                    _searchResults.value = birdSpeciesRepository.searchBirds(query)
                } catch (e: Exception) {
                    _searchResults.value = emptyList()
                    Log.e(TAG, "Error searching bird species locally: ${e.message}", e)
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
        fetchJob?.cancel()

        fetchJob = viewModelScope.launch {
            _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_local_finding, query))
            try {
                val searchResults = birdSpeciesRepository.searchBirds(query)
                if (searchResults.isNotEmpty()) {
                    val foundBird = searchResults.first()
                    val regionCodeForSearch = determineRegionCodeForSpeciesSearch()
                    _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_ai_finding_region, foundBird.commonName, regionCodeForSearch))
                    fetchHotspotsForSpeciesCode(foundBird.speciesCode, foundBird.commonName, regionCodeForSearch)
                } else {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_not_found, query))
                    isSearchActive = false
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "Bird search for '$query' was cancelled.")
            } catch (e: Exception) {
                val errorMsg = "Exception during local bird search for '$query': ${e.localizedMessage}"
                _uiState.value = MapUiState.Error(errorMsg)
                isSearchActive = false
                Log.e(TAG, errorMsg, e)
            }
        }
    }

    private fun determineRegionCodeForSpeciesSearch(): String {
        val currentMapCenter = cameraPositionStateHolder?.position?.target ?: initialMapCenter
        return userSettingsManager.getCountryCodeFromLatLng(context, currentMapCenter)
    }

    fun onSearchQueryCleared(currentMapCenter: LatLng) {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        if (isSearchActive) {
            isSearchActive = false
            fetchJob?.cancel()
            val currentZoom = cameraPositionStateHolder?.position?.zoom ?: initialMapZoom
            requestHotspotsForCurrentView(currentMapCenter, currentZoom, forceRefresh = true)
        }
    }

    fun notifyMarkerInteraction() {
        isMarkerInteractionInProgress = true
        Log.d(TAG, "Marker interaction notified.")
        markerInteractionResetJob?.cancel()
        markerInteractionResetJob = viewModelScope.launch {
            delay(MARKER_INTERACTION_LOCK_DURATION_MS)
            if (isActive && isMarkerInteractionInProgress) {
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
        if (isMarkerInteractionInProgress && !forceRefresh) {
            Log.d(TAG, "Marker interaction lock is active. Skipping refetch unless camera moved significantly.")
            (_uiState.value as? MapUiState.Success)?.let {
                if (abs(it.zoomLevelContext - zoom) > 0.1f || it.fetchedCenter != center) {
                    _uiState.value = it.copy(zoomLevelContext = zoom, fetchedCenter = center)
                }
            }
            return
        }

        fetchJob?.cancel()
        Log.d(TAG, "Preparing to fetch general hotspots. Center: $center, Zoom: $zoom, ForceRefresh: $forceRefresh")

        fetchJob = viewModelScope.launch {
            if (!forceRefresh) {
                delay(CAMERA_IDLE_DEBOUNCE_MS)
            }
            if (!isActive) {
                Log.d(TAG, "Job cancelled before network processing for general hotspots."); return@launch
            }

            val countryCodeToFilterHotspots: String? = null

            if (zoom < SHOW_HOTSPOTS_MIN_ZOOM_LEVEL && !forceRefresh && !isSearchActive) {
                if (areHotspotsVisible || (_uiState.value as? MapUiState.Success)?.hotspots?.isNotEmpty() == true) {
                    _uiState.value = MapUiState.Success(emptyList(), zoom, center, 0)
                }
                areHotspotsVisible = false
                Log.d(TAG, "Zoom ($zoom) < threshold. Hiding/Clearing general hotspots.")
                return@launch
            }

            val centerChangedSignificantly = lastFetchedCenter == null || isCenterChangeSignificant(center, lastFetchedCenter!!)
            val viewRequiresUpdate = forceRefresh || !areHotspotsVisible || centerChangedSignificantly

            if (viewRequiresUpdate) {
                if (isMarkerInteractionInProgress) {
                    isMarkerInteractionInProgress = false; markerInteractionResetJob?.cancel()
                    Log.d(TAG, "Proceeding with fetch due to forceRefresh or significant pan, clearing marker lock.")
                }
                _uiState.value = MapUiState.Loading(if (forceRefresh) context.getString(R.string.map_toast_refreshing_hotspots) else null)
                Log.d(TAG, "Fetching general hotspots (filter: $countryCodeToFilterHotspots) around $center, radius $HOTSPOT_FETCH_RADIUS_KM km")
                try {
                    val fetchedHotspots = hotspotRepository.getNearbyHotspots(
                        center = center, radiusKm = HOTSPOT_FETCH_RADIUS_KM,
                        currentBounds = null,
                        countryCodeFilter = countryCodeToFilterHotspots,
                        forceRefresh = forceRefresh
                    ).sortedByDescending { it.numSpeciesAllTime ?: 0 }

                    if (!isActive) { Log.d(TAG, "Job cancelled DURING/AFTER network call for general hotspots."); return@launch }

                    _uiState.value = MapUiState.Success(fetchedHotspots, zoom, center, HOTSPOT_FETCH_RADIUS_KM)
                    if (fetchedHotspots.isNotEmpty()) { // Generate heatmap data
                        _heatmapData.value = fetchedHotspots.map { WeightedLatLng(LatLng(it.lat, it.lng), (it.numSpeciesAllTime ?: 1).toDouble()) }
                    } else {
                        _heatmapData.value = emptyList()
                    }
                    areHotspotsVisible = true
                    lastFetchedCenter = center
                    Log.d(TAG, "General hotspot fetch successful. Showing ${fetchedHotspots.size} hotspots.")

                } catch (e: CancellationException) {
                    Log.i(TAG, "General hotspot fetch cancelled for $center.")
                } catch (e: Exception) {
                    val errorMsg = context.getString(R.string.map_fetch_error_generic, e.localizedMessage ?: context.getString(R.string.error_unknown))
                    _uiState.value = MapUiState.Error(errorMsg)
                    Log.e(TAG, "Exception fetching general hotspots for $center: $errorMsg", e)
                }
            } else {
                Log.d(TAG, "View has not changed enough for general hotspot refetch. Current Center: $center, Last Fetched: $lastFetchedCenter")
                (_uiState.value as? MapUiState.Success)?.let {
                    if (abs(it.zoomLevelContext - zoom) > 0.1f || it.fetchedCenter != center) {
                        _uiState.value = it.copy(zoomLevelContext = zoom, fetchedCenter = center)
                    }
                }
            }
        }
    }

    private suspend fun fetchHotspotsForSpeciesCode(speciesCode: String, birdDisplayName: String, regionCode: String) {
        viewModelScope.launch {
            Log.d(TAG, "Fetching hotspots for speciesCode: '$speciesCode' in region: '$regionCode'")
            try {
                val hotspotsWithBird = hotspotRepository.getHotspotsForSpeciesInCountry(speciesCode, regionCode)
                if (!isActive) {
                    Log.i(TAG, "Species hotspot fetch cancelled for $speciesCode in $regionCode"); return@launch
                }

                if (hotspotsWithBird.isNotEmpty()) {
                    val targetCountrySetting = UserSettingsManager.PREDEFINED_COUNTRIES.find { it.code == regionCode }
                    val mapCenter = targetCountrySetting?.center ?: calculateCenterOfHotspots(hotspotsWithBird) ?: initialMapCenter
                    val mapZoom = targetCountrySetting?.zoom ?: 6.0f

                    _uiState.value = MapUiState.Success(hotspotsWithBird, mapZoom, mapCenter, 0)
                    _heatmapData.value = hotspotsWithBird.map { WeightedLatLng(LatLng(it.lat, it.lng)) }
                    areHotspotsVisible = true
                    lastFetchedCenter = mapCenter
                } else {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_no_hotspots_region, birdDisplayName, regionCode))
                    isSearchActive = false
                    _heatmapData.value = emptyList()
                }
            } catch (e: CancellationException) {
                Log.i(TAG, "Species hotspot fetch for '$speciesCode' in '$regionCode' was cancelled.")
            } catch (e: Exception) {
                val errorMsg = context.getString(R.string.map_fetch_error_generic, e.localizedMessage ?: context.getString(R.string.error_unknown))
                _uiState.value = MapUiState.Error(errorMsg)
                isSearchActive = false
                _heatmapData.value = emptyList()
                Log.e(TAG, "Exception fetching hotspots for species $speciesCode in $regionCode: $errorMsg", e)
            }
        }
    }

    fun onHotspotMarkerClick(hotspot: EbirdNearbyHotspot) {
        viewModelScope.launch {
            _selectedHotspotDetails.value = HotspotSheetDetails(
                hotspot = hotspot,
                recentSightingsCount = 0, // Placeholder, fetch actual count
                notableBirdImageUrl = null, // Placeholder, fetch actual image
                isBookmarked = _bookmarkedHotspotIds.value.contains(hotspot.locId)
            )
            // Fetch detailed data for the bottom sheet
            try {
                val recentObsResponse = hotspotRepository.getEbirdApiService().getRecentObservationsForHotspot(hotspot.locId, back = 7)
                val recentSightings = if (recentObsResponse.isSuccessful) recentObsResponse.body() ?: emptyList() else emptyList()

                // Fetch species list for the hotspot
                val speciesListResponse = hotspotRepository.getEbirdApiService().getSpeciesListForHotspot(hotspot.locId)
                val speciesCodes = if (speciesListResponse.isSuccessful) speciesListResponse.body() ?: emptyList() else emptyList()

                val speciesDetails = mutableListOf<BirdSpeciesInfo>()
                if (speciesCodes.isNotEmpty()) {
                    // Fetch details for first few species as an example
                    val speciesToFetchDetailsFor = speciesCodes.take(5)
                    val taxonomyResponse = hotspotRepository.getEbirdApiService().getSpeciesTaxonomy(speciesCodes = speciesToFetchDetailsFor.joinToString(","))
                    if (taxonomyResponse.isSuccessful) {
                        taxonomyResponse.body()?.forEach { taxon ->
                            speciesDetails.add(
                                BirdSpeciesInfo(
                                    commonName = taxon.commonName,
                                    speciesCode = taxon.speciesCode,
                                    scientificName = taxon.scientificName,
                                    observationDate = null, count = null, isRecent = false, imageUrl = null // Image can be fetched here if needed
                                )
                            )
                        }
                    }
                }


                _selectedHotspotDetails.update { currentDetails ->
                    currentDetails?.copy(
                        recentSightingsCount = recentSightings.size,
                        notableBirdImageUrl = recentSightings.firstOrNull()?.let {
                            // Placeholder logic to get an image, replace with actual logic
                            "https://images.unsplash.com/photo-1518992028580-6d57bd80f2dd?w=100&q=80" // Example
                        },
                        speciesList = speciesDetails
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching details for bottom sheet for ${hotspot.locId}", e)
                // Optionally update _selectedHotspotDetails to show an error state in the sheet
            }
        }
    }

    fun clearSelectedHotspot() {
        _selectedHotspotDetails.value = null
    }

    fun toggleMapType() {
        val currentType = _mapProperties.value.mapType
        _mapProperties.value = _mapProperties.value.copy(
            mapType = when (currentType) {
                MapType.NORMAL -> MapType.SATELLITE
                MapType.SATELLITE -> MapType.HYBRID
                MapType.HYBRID -> MapType.TERRAIN
                MapType.TERRAIN -> MapType.NORMAL
                else -> MapType.NORMAL
            }
        )
    }

    fun toggleHeatmap() {
        _isHeatmapVisible.value = !_isHeatmapVisible.value
    }

    fun toggleMapLock() {
        _isMapLocked.value = !_isMapLocked.value
        _mapUiSettings.value = _mapUiSettings.value.copy(
            scrollGesturesEnabled = !_isMapLocked.value,
            zoomGesturesEnabled = !_isMapLocked.value
        )
    }

    fun toggleBookmarkCurrentHotspot() {
        _selectedHotspotDetails.value?.let { currentDetails ->
            val hotspotId = currentDetails.hotspot.locId
            _bookmarkedHotspotIds.update { currentBookmarks ->
                if (currentBookmarks.contains(hotspotId)) {
                    currentBookmarks - hotspotId
                } else {
                    currentBookmarks + hotspotId
                }
            }
            // Update the bottom sheet state to reflect bookmark change
            _selectedHotspotDetails.update {
                it?.copy(isBookmarked = _bookmarkedHotspotIds.value.contains(hotspotId))
            }
            // Persist bookmarks if using Room/SharedPreferences
        }
    }


    private fun calculateCenterOfHotspots(hotspots: List<EbirdNearbyHotspot>): LatLng? {
        if (hotspots.isEmpty()) return null
        var totalLat = 0.0
        var totalLng = 0.0
        hotspots.forEach {
            totalLat += it.lat
            totalLng += it.lng
        }
        return LatLng(totalLat / hotspots.size, totalLng / hotspots.size)
    }

    private fun isCenterChangeSignificant(newCenter: LatLng, oldCenter: LatLng): Boolean {
        val latDiff = abs(newCenter.latitude - oldCenter.latitude)
        val lngDiff = abs(newCenter.longitude - oldCenter.longitude)
        return latDiff > SIGNIFICANT_PAN_THRESHOLD_DEGREES || lngDiff > SIGNIFICANT_PAN_THRESHOLD_DEGREES
    }


    fun setHomeCountry(countrySetting: CountrySetting) {
        userSettingsManager.saveHomeCountryCode(context, countrySetting.code)
        _currentHomeCountrySetting.value = countrySetting
        Log.i(TAG, "Home country set to: ${countrySetting.name}. Map should re-center.")

        if (!isSearchActive) {
            requestHotspotsForCurrentView(countrySetting.center, countrySetting.zoom, forceRefresh = true)
        } else {
            if (_searchQuery.value.isNotBlank()){
                onBirdSearchSubmitted()
            }
        }
    }

    fun clearHotspotCache() {
        viewModelScope.launch {
            hotspotRepository.clearAllHotspotsCache()
            lastFetchedCenter = null
            areHotspotsVisible = false
            Log.i(TAG, "Hotspot cache cleared.")
            val currentCenter = cameraPositionStateHolder?.position?.target ?: initialMapCenter
            val currentZoom = cameraPositionStateHolder?.position?.zoom ?: initialMapZoom
            requestHotspotsForCurrentView(currentCenter, currentZoom, forceRefresh = true)
        }
    }

    fun toggleCompareMode() {
        _isCompareModeActive.value = !_isCompareModeActive.value
        if (!_isCompareModeActive.value) {
            _selectedHotspotsForComparison.value = emptyList()
        }
        Log.d(TAG, "Compare mode active: ${_isCompareModeActive.value}")
    }

    fun onHotspotSelectedForComparison(hotspot: EbirdNearbyHotspot) {
        if (!_isCompareModeActive.value) return

        _selectedHotspotsForComparison.update { currentSelection ->
            val alreadySelected = currentSelection.any { it.locId == hotspot.locId }
            if (alreadySelected) {
                currentSelection.filterNot { it.locId == hotspot.locId }
            } else {
                if (currentSelection.size < MAX_COMPARISON_ITEMS) {
                    currentSelection + hotspot
                } else {
                    Log.w(TAG, context.getString(R.string.map_compare_max_items_toast, MAX_COMPARISON_ITEMS))
                    currentSelection
                }
            }
        }
    }

    fun clearComparisonSelection() {
        _selectedHotspotsForComparison.value = emptyList()
    }
}