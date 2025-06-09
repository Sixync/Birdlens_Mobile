// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotBirdListViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdObservation
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.android.birdlens.data.model.ebird.EbirdTaxonomy
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Updated BirdSpeciesInfo
data class BirdSpeciesInfo(
    val commonName: String,
    val speciesCode: String,
    val scientificName: String,
    val observationDate: String?, // Nullable: only for recent
    val count: Int?,             // Nullable: only for recent
    val isRecent: Boolean        // New field
)

sealed class HotspotBirdListUiState {
    data object Loading : HotspotBirdListUiState()
    data class Success(val birds: List<BirdSpeciesInfo>) : HotspotBirdListUiState()
    data class Error(val message: String) : HotspotBirdListUiState()
}


open class HotspotBirdListViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val initialLocId: String?

    val _uiState = MutableStateFlow<HotspotBirdListUiState>(HotspotBirdListUiState.Loading)
    val uiState: StateFlow<HotspotBirdListUiState> = _uiState.asStateFlow()

    private val ebirdApiService = EbirdRetrofitInstance.api

    companion object {
        private const val TAG = "HotspotBirdListVM"
        private const val RECENT_DAYS_BACK = 30 // How many days back to consider "recent"
    }

    init {
        initialLocId = savedStateHandle.get<String>("hotspotId")
        Log.d(TAG, "ViewModel initialized. Attempting to retrieve hotspotId.")
        if (initialLocId.isNullOrBlank()) {
            Log.e(TAG, "INIT_ERROR: Hotspot ID (locId) is null or blank from SavedStateHandle.")
            _uiState.value = HotspotBirdListUiState.Error("Hotspot ID not provided. Cannot load bird list.")
        } else {
            Log.i(TAG, "INIT_SUCCESS: Valid Hotspot ID '$initialLocId' received. Triggering fetch for bird observations.")
            fetchBirdsForHotspot(initialLocId)
        }
    }

    private fun fetchBirdsForHotspot(locIdToFetch: String) {
        if (_uiState.value is HotspotBirdListUiState.Error && initialLocId.isNullOrBlank()) {
            Log.w(TAG, "FETCH_SKIP: ViewModel is in an initial error state due to missing locId. Skipping fetch for '$locIdToFetch'.")
            return
        }

        _uiState.value = HotspotBirdListUiState.Loading
        Log.i(TAG, "FETCH_START: Attempting to fetch bird data for locId: '$locIdToFetch'")

        viewModelScope.launch {
            try {
                coroutineScope { // Use coroutineScope for concurrent fetches
                    // Step 1: Fetch all species codes for the hotspot
                    val allSpeciesCodesDeferred = async { ebirdApiService.getSpeciesListForHotspot(locIdToFetch) }

                    // Step 2: Fetch recent observations
                    val recentObservationsDeferred = async {
                        ebirdApiService.getRecentObservationsForHotspot(
                            locId = locIdToFetch,
                            back = RECENT_DAYS_BACK,
                            detail = "simple", // simple detail is enough for names and counts
                            maxResults = 1000 // Fetch a good number of recent ones
                        )
                    }

                    val allSpeciesResponse = allSpeciesCodesDeferred.await()
                    val recentObsResponse = recentObservationsDeferred.await()

                    if (!allSpeciesResponse.isSuccessful || allSpeciesResponse.body() == null) {
                        val errorMsg = "Failed to fetch species list for hotspot '$locIdToFetch'. Code: ${allSpeciesResponse.code()}"
                        Log.e(TAG, errorMsg)
                        _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                        return@coroutineScope
                    }
                    val allSpeciesCodes = allSpeciesResponse.body()!!
                    if (allSpeciesCodes.isEmpty()) {
                        Log.i(TAG, "No species ever recorded at hotspot '$locIdToFetch'.")
                        _uiState.value = HotspotBirdListUiState.Success(emptyList())
                        return@coroutineScope
                    }

                    val recentObservations = if (recentObsResponse.isSuccessful && recentObsResponse.body() != null) {
                        recentObsResponse.body()!!
                    } else {
                        Log.w(TAG, "Failed to fetch recent observations for '$locIdToFetch'. Code: ${recentObsResponse.code()}. Proceeding without recency info for some species.")
                        emptyList<EbirdObservation>()
                    }

                    // Create a map of recent observations by species code for quick lookup
                    val recentObsMap = recentObservations.associateBy { it.speciesCode }

                    // Step 3: Fetch taxonomy for all species codes in batches if necessary
                    // eBird taxonomy endpoint can handle multiple species codes separated by commas
                    // Max URL length for GET requests is a concern. Typically around 2000 chars.
                    // Avg species code length is ~6 chars + 1 comma. So, roughly 2000 / 7 = ~280 species codes.
                    // Most hotspots won't exceed this. If they do, batching is needed. For now, assume it fits.
                    val taxonomyList: List<EbirdTaxonomy>
                    if (allSpeciesCodes.isNotEmpty()) {
                        val speciesQueryParam = allSpeciesCodes.joinToString(",")
                        Log.d(TAG, "Fetching taxonomy for ${allSpeciesCodes.size} species codes.")
                        val taxonomyResponse = ebirdApiService.getSpeciesTaxonomy(speciesCodes = speciesQueryParam)
                        if (taxonomyResponse.isSuccessful && taxonomyResponse.body() != null) {
                            taxonomyList = taxonomyResponse.body()!!
                        } else {
                            val errorMsg = "Failed to fetch taxonomy for species at '$locIdToFetch'. Code: ${taxonomyResponse.code()}"
                            Log.e(TAG, errorMsg)
                            _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                            return@coroutineScope
                        }
                    } else {
                        taxonomyList = emptyList()
                    }

                    // Step 4: Combine all data
                    val birdSpeciesInfoList = taxonomyList.mapNotNull { taxon ->
                        if (taxon.speciesCode.isBlank() || taxon.commonName.isBlank()) {
                            Log.w(TAG, "TRANSFORM_WARN: Skipping taxon due to blank speciesCode or commonName: $taxon")
                            return@mapNotNull null
                        }
                        val recentObservation = recentObsMap[taxon.speciesCode]
                        BirdSpeciesInfo(
                            commonName = taxon.commonName,
                            speciesCode = taxon.speciesCode,
                            scientificName = taxon.scientificName,
                            observationDate = recentObservation?.obsDt?.split(" ")?.firstOrNull(),
                            count = recentObservation?.howMany,
                            isRecent = recentObservation != null
                        )
                    }.sortedWith(compareByDescending<BirdSpeciesInfo> { it.isRecent } // Recent first
                        .thenBy { it.commonName }) // Then alphabetically by common name

                    _uiState.value = HotspotBirdListUiState.Success(birdSpeciesInfoList)
                    Log.i(TAG, "FETCH_PROCESS_SUCCESS: Constructed list of ${birdSpeciesInfoList.size} bird species for locId '$locIdToFetch'.")

                } // End coroutineScope
            } catch (e: Exception) {
                if (kotlin.coroutines.coroutineContext[Job]?.isCancelled == true) {
                    Log.i(TAG, "FETCH_CANCELLED: Job for fetching observations for locId '$locIdToFetch' was cancelled.")
                } else {
                    val errorMsg = "Network/Exception fetching bird data for hotspot '$locIdToFetch': ${e.javaClass.simpleName} - ${e.localizedMessage}"
                    _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                    Log.e(TAG, "FETCH_EXCEPTION: $errorMsg", e)
                }
            }
        }
    }

    fun refreshBirdsForHotspot() {
        if (!initialLocId.isNullOrBlank()) {
            Log.i(TAG, "REFRESH_TRIGGERED: Refreshing observations for initialLocId: '$initialLocId'")
            fetchBirdsForHotspot(initialLocId)
        } else {
            Log.e(TAG, "REFRESH_FAIL: Cannot refresh, initialLocId is missing or blank.")
            if (_uiState.value !is HotspotBirdListUiState.Error) {
                _uiState.value = HotspotBirdListUiState.Error("Cannot refresh: Hotspot ID is invalid.")
            }
        }
    }
}