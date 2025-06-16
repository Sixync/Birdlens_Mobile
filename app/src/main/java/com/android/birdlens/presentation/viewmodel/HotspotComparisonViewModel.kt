// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotComparisonViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.local.toEbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdObservation
import com.android.birdlens.data.repository.BirdSpeciesRepository
import com.android.birdlens.data.repository.HotspotRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Data class to hold all comparison metrics for a single hotspot
data class HotspotComparisonMetrics(
    val locId: String,
    val locName: String,
    val countryCode: String,
    val lat: Double,
    val lng: Double,
    val allTimeSpeciesCount: Int?,
    val latestObservationDate: String?,
    var recentObservationsSummary: List<String> = emptyList(), // e.g., "Common Name (Date)"
    var targetSpeciesInfo: TargetSpeciesSightingInfo? = null,
    var accessibilityInfo: String = "Not available", // Placeholder
    var bestTimeToVisitNotes: String = "Varies" // Placeholder
)

data class TargetSpeciesSightingInfo(
    val speciesName: String,
    val wasSeenRecently: Boolean,
    val lastSeenDate: String?,
    val isPotential: Boolean // True if ever recorded, even if not recent
)

sealed class HotspotComparisonUiState {
    data object Idle : HotspotComparisonUiState()
    data object Loading : HotspotComparisonUiState()
    data class Success(val comparisonData: List<HotspotComparisonMetrics>) : HotspotComparisonUiState()
    data class Error(val message: String) : HotspotComparisonUiState()
}

class HotspotComparisonViewModelFactory(
    private val application: Application,
    private val locIds: List<String>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HotspotComparisonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HotspotComparisonViewModel(application, locIds) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HotspotComparisonViewModel(
    application: Application,
    private val locIds: List<String>
) : AndroidViewModel(application) {

    private val hotspotRepository = HotspotRepository(application.applicationContext)
    private val birdSpeciesRepository = BirdSpeciesRepository(application.applicationContext) // For target species name to code

    private val _uiState = MutableStateFlow<HotspotComparisonUiState>(HotspotComparisonUiState.Idle)
    val uiState: StateFlow<HotspotComparisonUiState> = _uiState.asStateFlow()

    private val _targetSpeciesName = MutableStateFlow<String?>(null)
    val targetSpeciesName: StateFlow<String?> = _targetSpeciesName.asStateFlow()

    init {
        if (locIds.isNotEmpty()) {
            fetchComparisonData()
        } else {
            _uiState.value = HotspotComparisonUiState.Error("No hotspots selected for comparison.")
        }
    }

    fun setTargetSpecies(speciesName: String?) {
        _targetSpeciesName.value = speciesName
        // Re-fetch or re-process data if target species changes and data is already loaded
        if (_uiState.value is HotspotComparisonUiState.Success) {
            fetchComparisonData() // This will re-evaluate target species likelihood
        }
    }

    fun fetchComparisonData() {
        viewModelScope.launch {
            _uiState.value = HotspotComparisonUiState.Loading
            Log.d("ComparisonVM", "Fetching data for locIds: $locIds")
            try {
                val deferredMetrics = locIds.map { locId ->
                    async { fetchMetricsForHotspot(locId) }
                }
                val results = deferredMetrics.awaitAll().filterNotNull()
                if (results.size == locIds.size) {
                    _uiState.value = HotspotComparisonUiState.Success(results)
                } else {
                    _uiState.value = HotspotComparisonUiState.Error("Failed to fetch data for all selected hotspots.")
                }
            } catch (e: Exception) {
                Log.e("ComparisonVM", "Error fetching comparison data", e)
                _uiState.value = HotspotComparisonUiState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun fetchMetricsForHotspot(locId: String): HotspotComparisonMetrics? {
        try {
            // 1. Get basic hotspot info (assuming it might not be passed directly, or to refresh)
            //    A bit inefficient if MapScreen already has full EbirdNearbyHotspot objects.
            //    Consider passing EbirdNearbyHotspot objects directly or a map of locId to EbirdNearbyHotspot.
            //    For simplicity now, refetching minimal needed info or assuming locName is passed.
            //    Let's assume we need to fetch basic details again or have them passed.
            //    For now, we will rely on the repository's caching or fresh fetch.
            //    An alternative: MapViewModel could pass the List<EbirdNearbyHotspot> to this ViewModel via SavedStateHandle (if serializable/parcelable)
            //    or this ViewModel fetches them.
            //    A simpler way: If map screen passes locId, locName, lat, lng, numSpecies, latestObsDt, we don't need getNearbyHotspots just for one.
            //    Let's make HotspotRepository fetch one EbirdNearbyHotspot by locId if needed, or have a way to get one.
            //    Currently, HotspotRepository.getNearbyHotspots is by geo, not single ID.
            //    We'll assume for now we can get individual EbirdNearbyHotspot, or construct parts of HotspotComparisonMetrics progressively.

            // Fetch recent observations
            val recentObsResponse = hotspotRepository.ebirdApiService.getRecentObservationsForHotspot(locId)
            val recentObservations = if (recentObsResponse.isSuccessful) recentObsResponse.body() ?: emptyList() else emptyList()
            val recentObservationsSummary = recentObservations.take(5).map { "${it.comName} (${it.obsDt.substringBefore(" ")})" }


            // Fetch numSpeciesAllTime and latestObsDt (This might require a specific call if not already available)
            // Let's assume we have basic info (locId, locName) and need to enhance it.
            // The ebirdNearbyHotspot might come from a search if the user searched for the hotspot by name.
            // For this example, we'll mock parts of it if not directly available for a single locId.
            // This part needs a robust way to get EbirdNearbyHotspot for a single locId.
            // A simple way: The list of selected EbirdNearbyHotspot objects is passed to this ViewModel.

            val basicHotspotInfo = getHotspotBaseInfo(locId) // You'll need to implement or adapt this
            val metrics = HotspotComparisonMetrics(
                locId = locId,
                locName = basicHotspotInfo?.locName ?: "Hotspot $locId",
                countryCode = basicHotspotInfo?.countryCode ?: "",
                lat = basicHotspotInfo?.lat ?: 0.0,
                lng = basicHotspotInfo?.lng ?: 0.0,
                allTimeSpeciesCount = basicHotspotInfo?.numSpeciesAllTime,
                latestObservationDate = basicHotspotInfo?.latestObsDt,
                recentObservationsSummary = recentObservationsSummary
            )

            // Target Species Info (if a target is set)
            _targetSpeciesName.value?.let { targetName ->
                val targetSpeciesCode = birdSpeciesRepository.searchBirds(targetName).firstOrNull()?.speciesCode
                if (targetSpeciesCode != null) {
                    val speciesListResponse = hotspotRepository.ebirdApiService.getSpeciesListForHotspot(locId)
                    val isPotential = speciesListResponse.isSuccessful && speciesListResponse.body()?.contains(targetSpeciesCode) == true

                    val recentTargetSighting = recentObservations.find { it.speciesCode == targetSpeciesCode }
                    metrics.targetSpeciesInfo = TargetSpeciesSightingInfo(
                        speciesName = targetName,
                        wasSeenRecently = recentTargetSighting != null,
                        lastSeenDate = recentTargetSighting?.obsDt?.substringBefore(" "),
                        isPotential = isPotential
                    )
                }
            }
            // Placeholder for Accessibility and Best Time
            metrics.accessibilityInfo = "Trail: Moderate, Parking: Limited (Sample Data)"
            metrics.bestTimeToVisitNotes = if (metrics.latestObservationDate != null) "Active as of ${metrics.latestObservationDate}" else "Check recent sightings"

            return metrics
        } catch (e: Exception) {
            Log.e("ComparisonVM", "Error fetching metrics for $locId", e)
            return null // Or throw to be caught by the main try-catch
        }
    }

    // Helper to get base info for a hotspot (you might need to adjust your HotspotRepository for this)
    private suspend fun getHotspotBaseInfo(locId: String): EbirdNearbyHotspot? {
        // This is a placeholder. Your HotspotRepository needs a way to fetch a single hotspot by ID,
        // or this data should be passed from the selection process.
        // One way: if HotspotRepository caches, try to get from cache.
        // Another way: if a user selected it on the map, MapViewModel might have the full object.
        // For now, let's assume a function that could do this.
        val cachedHotspots = hotspotRepository.hotspotDao.getHotspotsByIds(listOf(locId))
        if (cachedHotspots.isNotEmpty()) return cachedHotspots.first().toEbirdNearbyHotspot()

        // Fallback: Make a broader nearby search centered on a guessed/unknown location for this locId
        // This is not ideal. It's better to have the full EbirdNearbyHotspot object from the selection.
        // Or, if only locId is passed, some metrics might be missing if not re-fetched.
        Log.w("ComparisonVM", "Cannot get base info for $locId directly without a getHotspotById equivalent or passed data.")
        return EbirdNearbyHotspot(locId, "Hotspot $locId", "", null, null, 0.0,0.0, null, null) // Dummy
    }
}