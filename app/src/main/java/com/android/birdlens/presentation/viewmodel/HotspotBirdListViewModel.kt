// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotBirdListViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BirdSpeciesInfo(
    val commonName: String,
    val speciesCode: String,
    val scientificName: String,
    val observationDate: String?,
    val count: Int?
)

sealed class HotspotBirdListUiState {
    data object Loading : HotspotBirdListUiState()
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
        // Ensure we don't restart loading if already in a definitive error state from init
        if (_uiState.value is HotspotBirdListUiState.Error && initialLocId.isNullOrBlank()) {
            Log.w(TAG, "FETCH_SKIP: ViewModel is in an initial error state due to missing locId. Skipping fetch for '$locIdToFetch'.")
            return
        }

        _uiState.value = HotspotBirdListUiState.Loading
        Log.i(TAG, "FETCH_START: Attempting to fetch bird observations for locId: '$locIdToFetch'")

        viewModelScope.launch {
            try {
                val response = ebirdApiService.getRecentObservationsForHotspot(locId = locIdToFetch, maxResults = 15) // Kept maxResults small

                if (response.isSuccessful) {
                    val observations = response.body()
                    if (observations != null) {
                        Log.i(TAG, "FETCH_API_SUCCESS: Received ${observations.size} observations for locId '$locIdToFetch'.")
                        try {
                            val distinctBirds = observations
                                .filterNotNull()
                                .mapNotNull { obs ->
                                    try {
                                        if (obs.speciesCode.isBlank() || obs.comName.isBlank()) {
                                            Log.w(TAG, "TRANSFORM_WARN: Skipping observation due to blank speciesCode or commonName: $obs")
                                            return@mapNotNull null
                                        }
                                        BirdSpeciesInfo(
                                            commonName = obs.comName,
                                            speciesCode = obs.speciesCode,
                                            scientificName = obs.sciName,
                                            observationDate = obs.obsDt.split(" ").firstOrNull(), // Basic date formatting
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
                        "No recent observations found for hotspot '$locIdToFetch', or the ID is invalid."
                    } else {
                        "API Error fetching observations (${response.code()}) for hotspot '$locIdToFetch': $errorBody"
                    }
                    _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                    Log.e(TAG, "FETCH_API_FAILURE: $errorMsg")
                }
            } catch (e: Exception) {
                if (kotlin.coroutines.coroutineContext[Job]?.isCancelled == true) {
                    Log.i(TAG, "FETCH_CANCELLED: Job for fetching observations for locId '$locIdToFetch' was cancelled.")
                } else {
                    val errorMsg = "Network/Exception fetching observations for hotspot '$locIdToFetch': ${e.javaClass.simpleName} - ${e.localizedMessage}"
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
            // _uiState.value is likely already Error from init, or should be set if somehow it wasn't
            if (_uiState.value !is HotspotBirdListUiState.Error) {
                _uiState.value = HotspotBirdListUiState.Error("Cannot refresh: Hotspot ID is invalid.")
            }
        }
    }
}