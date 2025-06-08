// app/src/main/java/com/android/birdlens/presentation/viewmodel/MapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
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

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val hotspotRepository = HotspotRepository(application.applicationContext)

    companion object {
        // A single zoom level to determine if hotspots should be shown.
        // Above this level, we are "zoomed in". Below, we are "zoomed out".
        const val SHOW_HOTSPOTS_MIN_ZOOM_LEVEL = 9.0f

        // The user requested a radius of "around 100km". The eBird API's maximum radius is 50km.
        // We will use 50km, which covers a diameter of 100km.
        const val HOTSPOT_FETCH_RADIUS_KM = 50

        // Default camera position
        val VIETNAM_INITIAL_CENTER = LatLng(16.047079, 108.220825)

        private const val CAMERA_IDLE_DEBOUNCE_MS = 750L
        private const val SIGNIFICANT_PAN_THRESHOLD_DEGREES = 0.25 // Approx 27.5km, ~half of the fetch radius
        const val COUNTRY_CODE_VIETNAM = "VN"
        private const val TAG = "MapViewModel"
    }

    private var fetchJob: Job? = null
    private var lastFetchedCenter: LatLng? = null
    private var areHotspotsVisible = false

    fun requestHotspotsForCurrentView(center: LatLng, zoom: Float) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            delay(CAMERA_IDLE_DEBOUNCE_MS)

            if (zoom < SHOW_HOTSPOTS_MIN_ZOOM_LEVEL) {
                // Zoomed out: Hide hotspots
                if (areHotspotsVisible) {
                    _uiState.value = MapUiState.Success(emptyList(), zoom)
                    areHotspotsVisible = false
                    lastFetchedCenter = null // Reset to force a refetch on the next zoom-in
                    Log.d(TAG, "Zoom level ($zoom) is below threshold. Hiding hotspots.")
                }
                return@launch
            }

            // Zoomed in: Show hotspots if needed
            val centerChangedSignificantly = lastFetchedCenter == null || isCenterChangeSignificant(center, lastFetchedCenter!!)

            if (!areHotspotsVisible || centerChangedSignificantly) {
                _uiState.value = MapUiState.Loading
                Log.d(TAG, "Zoom level ($zoom) is sufficient. Fetching hotspots. New view required: ${!areHotspotsVisible}, Center changed: $centerChangedSignificantly")
                try {
                    val fetchedHotspots = hotspotRepository.getNearbyHotspots(
                        center = center,
                        radiusKm = HOTSPOT_FETCH_RADIUS_KM,
                        countryCodeFilter = COUNTRY_CODE_VIETNAM
                    ).sortedByDescending { it.numSpeciesAllTime ?: 0 }

                    _uiState.value = MapUiState.Success(fetchedHotspots, zoom)
                    areHotspotsVisible = true
                    lastFetchedCenter = center
                    Log.d(TAG, "Fetch successful. Showing ${fetchedHotspots.size} hotspots.")

                } catch (e: Exception) {
                    if (kotlin.coroutines.coroutineContext[Job]?.isCancelled == true) {
                        Log.d(TAG, "Fetch job cancelled for zoom $zoom.")
                        return@launch
                    }
                    val errorMsg = "Exception fetching hotspots: ${e.localizedMessage}"
                    _uiState.value = MapUiState.Error(errorMsg)
                    Log.e(TAG, errorMsg, e)
                }
            } else {
                Log.d(TAG, "View has not changed enough. Not re-fetching.")
                // Update zoom context even if not fetching, to keep UI state fresh
                (_uiState.value as? MapUiState.Success)?.let {
                    _uiState.value = it.copy(zoomLevelContext = zoom)
                }
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
            // Invalidate last fetch to trigger a new one on next camera idle
            lastFetchedCenter = null
            areHotspotsVisible = false
            Log.i(TAG, "Hotspot cache cleared.")
        }
    }
}