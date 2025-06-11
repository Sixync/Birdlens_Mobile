// app/src/main/java/com/android/birdlens/presentation/viewmodel/MapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.BuildConfig
import com.android.birdlens.R
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.repository.HotspotRepository
import com.google.ai.client.generativeai.GenerativeModel
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
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val context = application.applicationContext // For string resources

    // Gemini AI model
    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    companion object {
        const val SHOW_HOTSPOTS_MIN_ZOOM_LEVEL = 7.5f
        const val HOTSPOT_FETCH_RADIUS_KM = 50
        val VIETNAM_INITIAL_CENTER = LatLng(16.047079, 108.220825)
        const val HOME_BUTTON_ZOOM_LEVEL = 6.0f
        private const val CAMERA_IDLE_DEBOUNCE_MS = 750L
        private const val SIGNIFICANT_PAN_THRESHOLD_DEGREES = 0.25
        const val COUNTRY_CODE_VIETNAM = "VN"
        private const val TAG = "MapViewModel"
    }

    private var fetchJob: Job? = null
    private var lastFetchedCenter: LatLng? = null
    private var areHotspotsVisible = false
    private var isSearchActive = false


    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onBirdSearchSubmitted() {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) {
            return
        }
        isSearchActive = true
        // Instead of directly fetching, we now go through the AI
        searchForBirdWithAI(query)
    }

    // New AI-powered search function
    private fun searchForBirdWithAI(query: String) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_ai_analyzing))
            try {
                // Construct the prompt to get a normalized English name
                val prompt = context.getString(R.string.gemini_prompt_map_search, query)
                val response = generativeModel.generateContent(prompt)
                val birdName = response.text?.trim()

                if (birdName.isNullOrBlank() || birdName.contains("Error:", ignoreCase = true)) {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_ai_failed, query))
                    isSearchActive = false // Reset search state
                    return@launch
                }

                // Now that we have a good name, proceed with the eBird search
                fetchHotspotsForBird(birdName)

            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.i(TAG, "AI search job was cancelled.")
                    return@launch
                }
                val errorMsg = "Exception during AI search: ${e.localizedMessage}"
                _uiState.value = MapUiState.Error(errorMsg)
                isSearchActive = false // Reset search state
                Log.e(TAG, errorMsg, e)
            }
        }
    }


    fun onSearchQueryCleared(currentCenter: LatLng) {
        _searchQuery.value = ""
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

    private fun fetchHotspotsForBird(birdName: String) {
        // This function is now private and part of the AI search flow.
        // The fetchJob is already active from the calling function `searchForBirdWithAI`.
        viewModelScope.launch {
            _uiState.value = MapUiState.Loading(context.getString(R.string.map_search_ai_finding, birdName))
            Log.d(TAG, "Fetching hotspots for bird: '$birdName' in country '$COUNTRY_CODE_VIETNAM'")

            try {
                val speciesCode = hotspotRepository.findSpeciesCode(birdName)

                if (speciesCode == null) {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_not_found, birdName))
                    isSearchActive = false // Reset search state on failure
                    return@launch
                }

                val hotspotsWithBird = hotspotRepository.getHotspotsForSpeciesInCountry(
                    speciesCode = speciesCode,
                    countryCode = COUNTRY_CODE_VIETNAM
                )

                if (hotspotsWithBird.isNotEmpty()) {
                    _uiState.value = MapUiState.Success(hotspotsWithBird, HOME_BUTTON_ZOOM_LEVEL, VIETNAM_INITIAL_CENTER, 0)
                    areHotspotsVisible = true
                    // Keep isSearchActive = true until the user clears it
                } else {
                    _uiState.value = MapUiState.Error(context.getString(R.string.map_search_error_no_hotspots, birdName))
                    isSearchActive = false // Reset search state on failure
                }

            } catch (e: Exception) {
                // If the job was cancelled, it's not a user-facing error, just an interruption.
                if (e is CancellationException) {
                    Log.i(TAG, "Bird search job was cancelled. This is expected if a new action occurred.")
                    return@launch // Don't set error state for cancellation
                }
                val errorMsg = "Exception searching for bird: ${e.localizedMessage}"
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