// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotDetailViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.VisitingTimesAnalysis
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdObservation
import com.android.birdlens.data.repository.HotspotRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HotspotDetailData(
    val basicInfo: EbirdNearbyHotspot,
    val recentSightings: List<EbirdObservation>,
    val analysis: VisitingTimesAnalysis?,
    val isSubscribed: Boolean
)

sealed class HotspotDetailUiState {
    data object Loading : HotspotDetailUiState()
    data class Success(val data: HotspotDetailData) : HotspotDetailUiState()
    data class Error(val message: String) : HotspotDetailUiState()
}

class HotspotDetailViewModelFactory(
    private val application: Application,
    owner: androidx.savedstate.SavedStateRegistryOwner,
    defaultArgs: android.os.Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        if (modelClass.isAssignableFrom(HotspotDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HotspotDetailViewModel(application, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class HotspotDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val hotspotRepository = HotspotRepository(application.applicationContext)
    private val accountInfoViewModel = AccountInfoViewModel(application)

    // Logic: Make the locId public for easier debugging if needed, though it's still safely initialized.
    val locId: String? = savedStateHandle["locId"]

    private val _uiState = MutableStateFlow<HotspotDetailUiState>(HotspotDetailUiState.Loading)
    val uiState: StateFlow<HotspotDetailUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "HotspotDetailVM"
    }

    init {
        // Logic: Added more detailed logging for initialization.
        Log.d(TAG, "ViewModel initialized. Attempting to get 'locId' from SavedStateHandle.")
        if (!locId.isNullOrBlank()) {
            Log.i(TAG, "ViewModel successfully initialized with locId: '$locId'. Fetching data.")
            fetchData()
        } else {
            val errorMsg = "Hotspot ID was not provided to the ViewModel."
            _uiState.value = HotspotDetailUiState.Error(errorMsg)
            Log.e(TAG, "ViewModel initialized, but 'locId' from SavedStateHandle is null or blank.")
        }
    }

    fun fetchData() {
        val currentLocId = locId
        if (currentLocId.isNullOrBlank()) {
            _uiState.value = HotspotDetailUiState.Error("Cannot fetch data without a valid Hotspot ID.")
            return
        }

        viewModelScope.launch {
            _uiState.value = HotspotDetailUiState.Loading
            try {
                val basicInfoDeferred = async { hotspotRepository.getHotspotDetails(currentLocId) }
                val recentSightingsDeferred = async { hotspotRepository.getEbirdApiService().getRecentObservationsForHotspot(currentLocId, back = 7, maxResults = 20) }
                val analysisDeferred = async { hotspotRepository.getVisitingTimesAnalysis(currentLocId, null) }

                accountInfoViewModel.fetchCurrentUser()
                val isSubscribed = (accountInfoViewModel.uiState.value as? AccountInfoUiState.Success)?.user?.subscription == "ExBird"

                val basicInfo = basicInfoDeferred.await()
                if (basicInfo == null) {
                    _uiState.value = HotspotDetailUiState.Error("Could not load hotspot details for $currentLocId")
                    return@launch
                }

                val recentSightingsResponse = recentSightingsDeferred.await()
                val recentSightings = if (recentSightingsResponse.isSuccessful) recentSightingsResponse.body() ?: emptyList() else emptyList()

                val analysisResponse = analysisDeferred.await()
                var analysis: VisitingTimesAnalysis? = null
                if(analysisResponse.isSuccessful && analysisResponse.body()?.error == false) {
                    analysis = analysisResponse.body()?.data
                } else {
                    Log.w(TAG, "Failed to get analysis data. Code: ${analysisResponse.code()}. User might not be subscribed or data unavailable.")
                }

                _uiState.value = HotspotDetailUiState.Success(
                    HotspotDetailData(
                        basicInfo = basicInfo,
                        recentSightings = recentSightings,
                        analysis = analysis,
                        isSubscribed = isSubscribed
                    )
                )

            } catch (e: Exception) {
                _uiState.value = HotspotDetailUiState.Error(e.localizedMessage ?: "An unknown error occurred while loading hotspot data.")
                Log.e(TAG, "Exception in fetchData for $currentLocId", e)
            }
        }
    }
}