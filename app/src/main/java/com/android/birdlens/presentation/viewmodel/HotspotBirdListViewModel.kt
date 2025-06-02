// Birdlens_Mobile/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotBirdListViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdObservation
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Simplified data for the list item
data class BirdSpeciesInfo(
    val commonName: String,
    val speciesCode: String,
    val scientificName: String,
    val observationDate: String?,
    val count: Int?
)

sealed class HotspotBirdListUiState {
    object Loading : HotspotBirdListUiState()
    data class Success(val birds: List<BirdSpeciesInfo>) : HotspotBirdListUiState()
    data class Error(val message: String) : HotspotBirdListUiState()
}

class HotspotBirdListViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val locId: String = savedStateHandle.get<String>("hotspotId")
        ?: throw IllegalArgumentException("Hotspot ID (locId) not provided to HotspotBirdListViewModel")

    private val _uiState = MutableStateFlow<HotspotBirdListUiState>(HotspotBirdListUiState.Loading)
    val uiState: StateFlow<HotspotBirdListUiState> = _uiState.asStateFlow()

    private val ebirdApiService = EbirdRetrofitInstance.api

    init {
        fetchBirdsForHotspot()
    }

    fun fetchBirdsForHotspot() {
        _uiState.value = HotspotBirdListUiState.Loading
        viewModelScope.launch {
            try {
                Log.d("HotspotBirdListVM", "Fetching birds for locId: $locId")
                val response = ebirdApiService.getRecentObservationsForHotspot(locId = locId, maxResults = 200) // Limit results if necessary
                if (response.isSuccessful && response.body() != null) {
                    val observations = response.body()!!
                    // Map to BirdSpeciesInfo and remove duplicates by speciesCode, preferring most recent or higher count if needed.
                    // For simplicity, just taking distinct by speciesCode.
                    val distinctBirds = observations
                        .distinctBy { it.speciesCode }
                        .map { obs ->
                            BirdSpeciesInfo(
                                commonName = obs.comName,
                                speciesCode = obs.speciesCode,
                                scientificName = obs.sciName,
                                observationDate = obs.obsDt.split(" ").firstOrNull(), // Just date part
                                count = obs.howMany
                            )
                        }
                    _uiState.value = HotspotBirdListUiState.Success(distinctBirds)
                    Log.d("HotspotBirdListVM", "Fetched ${distinctBirds.size} distinct bird species for $locId.")
                } else {
                    val errorMsg = "Error fetching birds for hotspot $locId: ${response.code()} - ${response.message()}"
                    _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                    Log.e("HotspotBirdListVM", errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Exception fetching birds for hotspot $locId: ${e.localizedMessage}"
                _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                Log.e("HotspotBirdListVM", errorMsg, e)
            }
        }
    }
}