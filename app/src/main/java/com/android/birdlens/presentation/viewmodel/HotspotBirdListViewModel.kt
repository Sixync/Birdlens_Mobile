// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotBirdListViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdObservation // Keep this if it's used, otherwise remove
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import kotlinx.coroutines.Job // Import Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// BirdSpeciesInfo data class (ensure it's here or imported)
data class BirdSpeciesInfo(
    val commonName: String,
    val speciesCode: String,
    val scientificName: String,
    val observationDate: String?,
    val count: Int?
)

// HotspotBirdListUiState sealed class (ensure it's here or imported)
sealed class HotspotBirdListUiState {
    object Loading : HotspotBirdListUiState()
    data class Success(val birds: List<BirdSpeciesInfo>) : HotspotBirdListUiState()
    data class Error(val message: String) : HotspotBirdListUiState()
}


class HotspotBirdListViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val initialLocId: String?

    private val _uiState = MutableStateFlow<HotspotBirdListUiState>(HotspotBirdListUiState.Loading)
    val uiState: StateFlow<HotspotBirdListUiState> = _uiState.asStateFlow()

    private val ebirdApiService = EbirdRetrofitInstance.api

    companion object {
        private const val TAG = "HotspotBirdListVM"
    }

    init {
        initialLocId = savedStateHandle.get<String>("hotspotId")
        if (initialLocId.isNullOrBlank()) {
            Log.e(TAG, "INIT_ERROR: Hotspot ID (locId) is missing or blank from SavedStateHandle.")
            _uiState.value = HotspotBirdListUiState.Error("Hotspot ID not provided. Cannot load bird list.")
        } else {
            Log.i(TAG, "INIT_SUCCESS: Valid Hotspot ID '$initialLocId' received. Fetching birds.")
            fetchBirdsForHotspot(initialLocId)
        }
    }

    private fun fetchBirdsForHotspot(locIdToFetch: String) {
        if (_uiState.value !is HotspotBirdListUiState.Error) {
            _uiState.value = HotspotBirdListUiState.Loading
        } else {
            Log.w(TAG, "FETCH_SKIP: Attempted to fetch but already in error state from init for locId: $locIdToFetch")
            return
        }

        viewModelScope.launch {
            try {
                Log.i(TAG, "FETCH_START: Fetching birds for locId: $locIdToFetch")
                // IMPORTANT CHANGE: Reduce maxResults for testing
                val response = ebirdApiService.getRecentObservationsForHotspot(locId = locIdToFetch, maxResults = 15) // Test with a very small list


                if (response.isSuccessful) {
                    val observations = response.body()
                    if (observations != null) {
                        Log.i(TAG, "FETCH_API_SUCCESS: Received ${observations.size} observations for locId '$locIdToFetch'.")
                        try {
                            val distinctBirds = observations
                                .filterNotNull()
                                .mapNotNull { obs ->
                                    try {
                                        if (obs.speciesCode.isBlank()) {
                                            Log.w(TAG, "TRANSFORM_WARN: Skipping observation due to blank speciesCode: $obs")
                                            return@mapNotNull null
                                        }
                                        if (obs.comName.isBlank()) {
                                            Log.w(TAG, "TRANSFORM_WARN: Skipping observation due to blank commonName: $obs")
                                            return@mapNotNull null
                                        }
                                        BirdSpeciesInfo(
                                            commonName = obs.comName,
                                            speciesCode = obs.speciesCode,
                                            scientificName = obs.sciName,
                                            observationDate = obs.obsDt.split(" ").firstOrNull(),
                                            count = obs.howMany
                                        )
                                    } catch (mappingEx: Exception) {
                                        Log.e(TAG, "TRANSFORM_ERROR: Error mapping a single observation: $obs", mappingEx)
                                        null
                                    }
                                }
                                .distinctBy { it.speciesCode }

                            _uiState.value = HotspotBirdListUiState.Success(distinctBirds)
                            Log.i(TAG, "FETCH_PROCESS_SUCCESS: Mapped to ${distinctBirds.size} distinct bird species for locId '$locIdToFetch'.")
                        } catch (processingEx: Exception) {
                            Log.e(TAG, "FETCH_PROCESS_EXCEPTION: Exception during data processing for locId '$locIdToFetch'.", processingEx)
                            _uiState.value = HotspotBirdListUiState.Error("Error processing bird data: ${processingEx.localizedMessage}")
                        }
                    } else {
                        Log.w(TAG, "FETCH_API_NODATA: API call for locId '$locIdToFetch' (Code: ${response.code()}) body was null.")
                        _uiState.value = HotspotBirdListUiState.Error("No data received for hotspot '$locIdToFetch'.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message()
                    val errorMsg = if (response.code() == 404) {
                        "No recent observations found for hotspot '$locIdToFetch', or ID invalid."
                    } else {
                        "API Error (${response.code()}) for hotspot '$locIdToFetch': $errorBody"
                    }
                    _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                    Log.e(TAG, "FETCH_API_FAILURE: $errorMsg")
                }
            } catch (e: Exception) {
                if (kotlin.coroutines.coroutineContext[Job]?.isCancelled == true) {
                    Log.i(TAG, "FETCH_CANCELLED: Job for locId '$locIdToFetch' cancelled.")
                } else {
                    val errorMsg = "Network/Exception for hotspot '$locIdToFetch': ${e.localizedMessage}"
                    _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                    Log.e(TAG, "FETCH_EXCEPTION: $errorMsg", e)
                }
            }
        }
    }

    fun refreshBirdsForHotspot() {
        if (!initialLocId.isNullOrBlank()) {
            Log.i(TAG, "REFRESH_TRIGGERED: Refreshing for initialLocId: '$initialLocId'")
            fetchBirdsForHotspot(initialLocId)
        } else {
            Log.e(TAG, "REFRESH_FAIL: initialLocId missing or blank.")
            _uiState.value = HotspotBirdListUiState.Error("Cannot refresh: Hotspot ID invalid.")
        }
    }
}