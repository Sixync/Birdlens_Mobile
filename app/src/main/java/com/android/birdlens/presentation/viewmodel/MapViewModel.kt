// app/src/main/java/com/android/birdlens/presentation/viewmodel/MapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
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

class MapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val ebirdApiService = EbirdRetrofitInstance.api

    companion object {
        const val OVERVIEW_MAX_ZOOM = 7.5f
        const val DETAILED_MIN_ZOOM = 9.5f

        val VIETNAM_INITIAL_CENTER = LatLng(16.047079, 108.220825)
        const val VIETNAM_INITIAL_RADIUS_KM = 500
        const val VIETNAM_INITIAL_MAX_HOTSPOTS = 25

        const val OVERVIEW_RADIUS_KM = 100
        const val OVERVIEW_MAX_HOTSPOTS_TARGET = 50

        const val DETAILED_RADIUS_KM = 20

        private const val CAMERA_IDLE_DEBOUNCE_MS = 600L
        const val COUNTRY_CODE_VIETNAM = "VN"
    }

    private var currentHotspots = listOf<EbirdNearbyHotspot>()
    private var lastFetchedZoomCategory: String = "none"
    private var lastFetchedCenter: LatLng? = null
    private var fetchJob: Job? = null
    private var isInitialLoadComplete = false

    fun requestHotspotsForCurrentView(center: LatLng, zoom: Float) {
        Log.d("MapViewModel", "Request received for center: $center, zoom: $zoom. InitialLoadComplete: $isInitialLoadComplete")
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

        // Adjusted thresholds: more generous for detailed views (larger pan allowed before refetch)
        val threshold = when {
            zoom >= DETAILED_MIN_ZOOM + 1f -> 0.15 // Approx 16.5km (very zoomed in, allow more panning)
            zoom >= DETAILED_MIN_ZOOM -> 0.25  // Approx 27.5km (detailed view)
            zoom <= OVERVIEW_MAX_ZOOM - 1f-> 0.8 // Approx 88km (overview, more sensitive to pan)
            zoom <= OVERVIEW_MAX_ZOOM -> 0.5  // Approx 55km (overview)
            else -> 0.4 // Approx 44km (transition zone)
        }
        // Log.d("MapViewModel_Debug", "CenterChangeCheck: LatDiff=$latDiff, LngDiff=$lngDiff, Zoom=$zoom, Threshold=$threshold")
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

            // Skip fetch if not forced, category is same, center hasn't changed significantly, AND we already have successful data
            if (!forceFetch && currentCategory == lastFetchedZoomCategory && !centerChangedSignificantly && _uiState.value is MapUiState.Success) {
                Log.d("MapViewModel", "Skipping fetch: Conditions not met. Category: $currentCategory, CenterChanged: $centerChangedSignificantly")
                (_uiState.value as? MapUiState.Success)?.let {
                    if (it.zoomLevelContext != zoomForContext) {
                        _uiState.value = MapUiState.Success(it.hotspots, zoomForContext) // Update zoom context if map zoom changed
                    }
                }
                return@launch
            }

            _uiState.value = MapUiState.Loading
            Log.d("MapViewModel", "Fetching for category: $currentCategory, center: $center, zoomContext: $zoomForContext")

            val radiusKm: Int
            var maxHotspotsTarget: Int? = null

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
                }
                else -> {
                    Log.e("MapViewModel", "Unknown fetch category: $currentCategory")
                    _uiState.value = MapUiState.Error("Internal error: Unknown map category.")
                    return@launch
                }
            }

            try {
                Log.d("MapViewModel", "API Call: lat=${center.latitude}, lng=${center.longitude}, dist=$radiusKm")
                val response = ebirdApiService.getNearbyHotspots(
                    lat = center.latitude,
                    lng = center.longitude,
                    dist = radiusKm
                )

                if (response.isSuccessful && response.body() != null) {
                    var fetchedHotspots = response.body()!!
                    Log.d("MapViewModel", "API returned ${fetchedHotspots.size} hotspots for '$currentCategory' before country filtering.")

                    // *** Filter by country code "VN" ***
                    fetchedHotspots = fetchedHotspots.filter { it.countryCode == COUNTRY_CODE_VIETNAM }
                    Log.d("MapViewModel", "Filtered to ${fetchedHotspots.size} hotspots in Vietnam (VN).")

                    fetchedHotspots = fetchedHotspots.sortedByDescending { it.numSpeciesAllTime ?: 0 }

                    if (maxHotspotsTarget != null && fetchedHotspots.size > maxHotspotsTarget) {
                        fetchedHotspots = fetchedHotspots.take(maxHotspotsTarget)
                        Log.d("MapViewModel", "Trimmed $currentCategory VN hotspots to ${fetchedHotspots.size}")
                    }

                    currentHotspots = fetchedHotspots
                    lastFetchedCenter = center
                    lastFetchedZoomCategory = currentCategory
                    _uiState.value = MapUiState.Success(currentHotspots, zoomForContext)
                    Log.d("MapViewModel", "Success: ${currentHotspots.size} hotspots for '$currentCategory' (VN), zoomContext: $zoomForContext")

                    if (currentCategory == "initial" && !isInitialLoadComplete) {
                        isInitialLoadComplete = true
                        Log.d("MapViewModel", "Initial load marked as complete.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    val errorMsg = "Error fetching hotspots (${response.code()}): $errorBody"
                    _uiState.value = MapUiState.Error(errorMsg)
                    Log.e("MapViewModel", "$errorMsg (Center: $center, Radius: $radiusKm, Category: $currentCategory, Zoom: $zoomForContext)")
                }
            } catch (e: Exception) {
                if (kotlin.coroutines.coroutineContext[Job]?.isCancelled == true) {
                    Log.d("MapViewModel", "Fetch job cancelled.")
                    return@launch
                }
                val errorMsg = "Exception fetching hotspots: ${e.localizedMessage}"
                _uiState.value = MapUiState.Error(errorMsg)
                Log.e("MapViewModel", "Exception: $errorMsg", e)
            }
        }
    }
}