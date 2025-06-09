// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/MapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.repository.HotspotRepository
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs


sealed class MapUiState {
    data object Idle : MapUiState()
    data object Loading : MapUiState()
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

    companion object {
        // Adjusted zoom level: Hotspots will show if zoom is 7.5f or greater.
        const val SHOW_HOTSPOTS_MIN_ZOOM_LEVEL = 7.5f

        const val HOTSPOT_FETCH_RADIUS_KM = 50
        val VIETNAM_INITIAL_CENTER = LatLng(16.047079, 108.220825)
        // Define the zoom level for the "Home" button
        const val HOME_BUTTON_ZOOM_LEVEL = 6.0f // Can be same as initial or different
        private const val CAMERA_IDLE_DEBOUNCE_MS = 750L
        private const val SIGNIFICANT_PAN_THRESHOLD_DEGREES = 0.25
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
                if (areHotspotsVisible) {
                    _uiState.value = MapUiState.Success(emptyList(), zoom, center, 0)
                    areHotspotsVisible = false
                    lastFetchedCenter = null
                    Log.d(TAG, "Zoom level ($zoom) is below threshold ($SHOW_HOTSPOTS_MIN_ZOOM_LEVEL). Hiding hotspots.")
                } else {
                    // If hotspots are already hidden, just update the context if needed
                    (_uiState.value as? MapUiState.Success)?.let {
                        if (it.hotspots.isEmpty()) { // Ensure we are in a "hidden" success state
                            _uiState.value = it.copy(zoomLevelContext = zoom, fetchedCenter = center, fetchedRadiusKm = 0)
                        }
                    }
                }
                return@launch
            }

            val centerChangedSignificantly = lastFetchedCenter == null || isCenterChangeSignificant(center, lastFetchedCenter!!)

            if (!areHotspotsVisible || centerChangedSignificantly) {
                _uiState.value = MapUiState.Loading
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
            lastFetchedCenter = null
            areHotspotsVisible = false
            Log.i(TAG, "Hotspot cache cleared.")
            // Optionally, trigger a refetch for the current view if appropriate
            // For example, if _uiState.value is Success, call requestHotspotsForCurrentView with its parameters.
        }
    }
}