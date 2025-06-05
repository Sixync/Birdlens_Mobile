package com.android.birdlens.presentation.viewmodel

import  android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.repository.HotspotRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

sealed class MapUiState {
    object Idle : MapUiState()
    object Loading : MapUiState()
    data class Success(val hotspots: List<EbirdNearbyHotspot>, val zoomLevelContext: Float) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class MapViewModel(application: Application) : AndroidViewModel(application) { // Changed constructor
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // private val ebirdApiService = EbirdRetrofitInstance.api // Replaced by repository
    private val hotspotRepository = HotspotRepository(application.applicationContext) // Added

    companion object {
        const val OVERVIEW_MAX_ZOOM = 7.5f
        const val DETAILED_MIN_ZOOM = 9.5f

        val VIETNAM_INITIAL_CENTER = LatLng(16.047079, 108.220825)
        const val VIETNAM_INITIAL_RADIUS_KM = 500 // Keep large for initial wide view
        const val VIETNAM_INITIAL_MAX_HOTSPOTS = 30 // Limit initial fetch for performance

        const val OVERVIEW_RADIUS_KM = 100
        const val OVERVIEW_MAX_HOTSPOTS_TARGET = 50

        const val DETAILED_RADIUS_KM = 25 // Increased slightly for better detailed view

        private const val CAMERA_IDLE_DEBOUNCE_MS = 700L // Increased slightly
        const val COUNTRY_CODE_VIETNAM = "VN"
        private const val TAG = "MapViewModel" // Added for logging
    }

    private var currentHotspots = listOf<EbirdNearbyHotspot>()
    private var lastFetchedZoomCategory: String = "none"
    private var lastFetchedCenter: LatLng? = null
    private var fetchJob: Job? = null
    private var isInitialLoadComplete = false

    fun requestHotspotsForCurrentView(center: LatLng, zoom: Float) {
        Log.d(TAG, "Request received for center: $center, zoom: $zoom. InitialLoadComplete: $isInitialLoadComplete")
        if (!isInitialLoadComplete) {
            fetchHotspotsInternal(VIETNAM_INITIAL_CENTER, zoom, "initial", true)
        } else {
            fetchHotspotsInternal(center, zoom, determineZoomCategory(zoom), false)
        }
    }

    private fun determineZoomCategory(zoom: Float): String {
        return when {
            zoom <= OVERVIEW_MAX_ZOOM -> "overview"
            zoom >= DETAILED_MIN_ZOOM -> "detailed"
            else -> lastFetchedZoomCategory.let { if (it == "none" || it == "initial") "overview" else it }
        }
    }

    private fun isCenterChangeSignificant(newCenter: LatLng, oldCenter: LatLng, zoom: Float): Boolean {
        val latDiff = abs(newCenter.latitude - oldCenter.latitude)
        val lngDiff = abs(newCenter.longitude - oldCenter.longitude)

        val threshold = when {
            zoom >= DETAILED_MIN_ZOOM + 1f -> 0.10 // Approx 11km (very zoomed in, allow more panning)
            zoom >= DETAILED_MIN_ZOOM -> 0.15  // Approx 16.5km (detailed view)
            zoom <= OVERVIEW_MAX_ZOOM - 1f-> 0.6 // Approx 66km (overview, more sensitive to pan)
            zoom <= OVERVIEW_MAX_ZOOM -> 0.4  // Approx 44km (overview)
            else -> 0.3 // Approx 33km (transition zone)
        }
        return latDiff > threshold || lngDiff > threshold
    }

    private fun fetchHotspotsInternal(center: LatLng, zoomForContext: Float, category: String, forceFetch: Boolean) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            delay(CAMERA_IDLE_DEBOUNCE_MS)

            val currentCategory = if (category == "initial" && isInitialLoadComplete) {
                determineZoomCategory(zoomForContext)
            } else {
                category
            }

            val centerChangedSignificantly = lastFetchedCenter == null ||
                    isCenterChangeSignificant(center, lastFetchedCenter!!, zoomForContext)

            if (!forceFetch && currentCategory == lastFetchedZoomCategory && !centerChangedSignificantly && _uiState.value is MapUiState.Success) {
                Log.d(TAG, "Skipping fetch: Conditions not met. Category: $currentCategory, CenterChanged: $centerChangedSignificantly")
                (_uiState.value as? MapUiState.Success)?.let {
                    if (it.zoomLevelContext != zoomForContext) {
                        _uiState.value = MapUiState.Success(it.hotspots, zoomForContext)
                    }
                }
                return@launch
            }

            _uiState.value = MapUiState.Loading
            Log.d(TAG, "Fetching for category: $currentCategory, center: $center, zoomContext: $zoomForContext")

            val radiusKm: Int
            var maxHotspotsTarget: Int? = null // For applying limit after fetching all in radius

            when (currentCategory) {
                "initial" -> {
                    radiusKm = VIETNAM_INITIAL_RADIUS_KM
                    maxHotspotsTarget = VIETNAM_INITIAL_MAX_HOTSPOTS
                }
                "overview" -> {
                    radiusKm = OVERVIEW_RADIUS_KM
                    maxHotspotsTarget = OVERVIEW_MAX_HOTSPOTS_TARGET
                }
                "detailed" -> {
                    radiusKm = DETAILED_RADIUS_KM
                    // No specific max target for detailed, let API decide or set a higher one if needed.
                    // Or repository can filter based on density if too many are returned for a small radius.
                }
                else -> {
                    Log.e(TAG, "Unknown fetch category: $currentCategory")
                    _uiState.value = MapUiState.Error("Internal error: Unknown map category.")
                    return@launch
                }
            }

            try {
                // Use repository to fetch hotspots
                var fetchedHotspots = hotspotRepository.getNearbyHotspots(
                    center = center,
                    radiusKm = radiusKm,
                    countryCodeFilter = COUNTRY_CODE_VIETNAM // Apply country filter in repository call
                )
                Log.d(TAG, "Repository returned ${fetchedHotspots.size} hotspots for '$currentCategory'.")


                // Sorting and limiting logic (if still needed after repository, though repository can also handle it)
                fetchedHotspots = fetchedHotspots.sortedByDescending { it.numSpeciesAllTime ?: 0 }

                if (maxHotspotsTarget != null && fetchedHotspots.size > maxHotspotsTarget) {
                    fetchedHotspots = fetchedHotspots.take(maxHotspotsTarget)
                    Log.d(TAG, "Trimmed $currentCategory hotspots to ${fetchedHotspots.size}")
                }

                currentHotspots = fetchedHotspots
                lastFetchedCenter = center
                lastFetchedZoomCategory = currentCategory
                _uiState.value = MapUiState.Success(currentHotspots, zoomForContext)
                Log.d(TAG, "Success: ${currentHotspots.size} hotspots for '$currentCategory', zoomContext: $zoomForContext")

                if (currentCategory == "initial" && !isInitialLoadComplete) {
                    isInitialLoadComplete = true
                    Log.d(TAG, "Initial load marked as complete.")
                }

            } catch (e: Exception) {
                if (kotlin.coroutines.coroutineContext[Job]?.isCancelled == true) {
                    Log.d(TAG, "Fetch job cancelled for $currentCategory.")
                    return@launch
                }
                val errorMsg = "Exception fetching/processing hotspots for $currentCategory: ${e.localizedMessage}"
                _uiState.value = MapUiState.Error(errorMsg)
                Log.e(TAG, "Exception for $currentCategory: $errorMsg", e)
            }
        }
    }

    // Call this from settings or a debug menu if you want to clear the cache
    fun clearHotspotCache() {
        viewModelScope.launch {
            hotspotRepository.clearAllHotspotsCache()
            // Optionally, trigger a fresh fetch after clearing
            lastFetchedCenter?.let { center ->
                val currentZoom = (_uiState.value as? MapUiState.Success)?.zoomLevelContext ?: VIETNAM_INITIAL_CENTER.longitude.toFloat() // Fallback zoom
                requestHotspotsForCurrentView(center, currentZoom)
            }
        }
    }
}