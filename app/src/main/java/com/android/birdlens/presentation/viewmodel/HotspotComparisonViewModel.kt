// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotComparisonViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.local.toEbirdNearbyHotspot
import com.android.birdlens.data.model.VisitingTimesAnalysis
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.repository.BirdSpeciesRepository
import com.android.birdlens.data.repository.HotspotRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Logic: Add a nullable field to hold the premium analysis data.
data class HotspotComparisonMetrics(
    val locId: String,
    val locName: String,
    val countryCode: String,
    val lat: Double,
    val lng: Double,
    val allTimeSpeciesCount: Int?,
    val latestObservationDate: String?,
    var recentObservationsSummary: List<String> = emptyList(),
    var targetSpeciesInfo: TargetSpeciesSightingInfo? = null,
    var accessibilityInfo: String = "Not available",
    var bestTimeToVisitNotes: String = "Varies",
    var visitingTimes: VisitingTimesAnalysis? = null
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
    private val birdSpeciesRepository = BirdSpeciesRepository(application.applicationContext)

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
        if (_uiState.value is HotspotComparisonUiState.Success) {
            fetchComparisonData()
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
            val ebirdApiService = hotspotRepository.getEbirdApiService()

            val recentObsResponse = ebirdApiService.getRecentObservationsForHotspot(locId)
            val recentObservations = if (recentObsResponse.isSuccessful) recentObsResponse.body() ?: emptyList() else emptyList()
            val recentObservationsSummary = recentObservations.take(5).map { "${it.comName} (${it.obsDt.substringBefore(" ")})" }

            val basicHotspotInfo = getHotspotBaseInfo(locId)
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

            // Logic: Fetch the visiting times analysis from our new backend endpoint.
            val targetSpeciesCode = _targetSpeciesName.value?.let { birdSpeciesRepository.searchBirds(it).firstOrNull()?.speciesCode }
            val analysisResponse = hotspotRepository.getVisitingTimesAnalysis(locId, targetSpeciesCode)
            if (analysisResponse.isSuccessful && analysisResponse.body()?.error == false) {
                metrics.visitingTimes = analysisResponse.body()?.data
                Log.d("ComparisonVM", "Successfully fetched visiting times for $locId")
            } else {
                // It's okay if this fails (e.g., user is not premium), the field will just be null.
                Log.w("ComparisonVM", "Failed to get visiting times for $locId: ${analysisResponse.message()}")
            }

            _targetSpeciesName.value?.let { targetName ->
                if (targetSpeciesCode != null) {
                    val speciesListResponse = ebirdApiService.getSpeciesListForHotspot(locId)
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

            metrics.accessibilityInfo = "Trail: Moderate, Parking: Limited (Sample Data)"
            metrics.bestTimeToVisitNotes = if (metrics.latestObservationDate != null) "Active as of ${metrics.latestObservationDate}" else "Check recent sightings"

            return metrics
        } catch (e: Exception) {
            Log.e("ComparisonVM", "Error fetching metrics for $locId", e)
            return null
        }
    }

    private suspend fun getHotspotBaseInfo(locId: String): EbirdNearbyHotspot? {
        val cachedHotspots = hotspotRepository.hotspotDao.getHotspotsByIds(listOf(locId))
        if (cachedHotspots.isNotEmpty()) return cachedHotspots.first().toEbirdNearbyHotspot()
        Log.w("ComparisonVM", "Cannot get base info for $locId directly without a getHotspotById equivalent or passed data.")
        return EbirdNearbyHotspot(locId, "Hotspot $locId", "", null, null, 0.0,0.0, null, null)
    }
}