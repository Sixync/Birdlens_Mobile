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
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
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
    // Logic: A ViewModel should fetch its own data. We get a direct reference to ApiService.
    private val apiService: ApiService = RetrofitInstance.api(application.applicationContext)

    val locId: String? = savedStateHandle["locId"]

    private val _uiState = MutableStateFlow<HotspotDetailUiState>(HotspotDetailUiState.Loading)
    val uiState: StateFlow<HotspotDetailUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "HotspotDetailVM"
    }

    init {
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
                // Logic: Fetch all independent network calls concurrently for performance.
                val basicInfoDeferred = async { hotspotRepository.getHotspotDetails(currentLocId) }
                val recentSightingsDeferred = async { hotspotRepository.getEbirdApiService().getRecentObservationsForHotspot(currentLocId, back = 7, maxResults = 20) }
                val userInfoDeferred = async { apiService.getCurrentUser() }

                // Logic: First, await the user info to determine subscription status.
                val userInfoResponse = userInfoDeferred.await()
                val isSubscribed = if (userInfoResponse.isSuccessful) {
                    val isExBird = userInfoResponse.body()?.data?.subscription == "ExBird"
                    Log.d(TAG, "User info fetched successfully. Subscription: ${userInfoResponse.body()?.data?.subscription}, IsExBird: $isExBird")
                    isExBird
                } else {
                    Log.w(TAG, "Failed to get user info, assuming not subscribed. Error: ${userInfoResponse.code()}")
                    false
                }

                // Logic: Based on the subscription status, conditionally create the analysis task.
                val analysisDeferred = if (isSubscribed) {
                    async { hotspotRepository.getVisitingTimesAnalysis(currentLocId, null) }
                } else {
                    null // If not subscribed, this task will be null.
                }

                // Logic: Now await the rest of the deferred tasks.
                val basicInfo = basicInfoDeferred.await()
                if (basicInfo == null) {
                    _uiState.value = HotspotDetailUiState.Error("Could not load hotspot details for $currentLocId")
                    return@launch
                }

                val recentSightingsResponse = recentSightingsDeferred.await()
                val recentSightings = if (recentSightingsResponse.isSuccessful) recentSightingsResponse.body() ?: emptyList() else emptyList()

                // Logic: Only try to get analysis data if the analysis task was created.
                var analysis: VisitingTimesAnalysis? = null
                if (analysisDeferred != null) {
                    // Logic: Correctly await the Deferred<Response> object.
                    val analysisResponse = analysisDeferred.await()
                    if (analysisResponse.isSuccessful && analysisResponse.body()?.error == false) {
                        analysis = analysisResponse.body()?.data
                        Log.d(TAG, "Successfully fetched premium analysis data for subscribed user.")
                    } else {
                        Log.w(TAG, "User is subscribed but failed to get analysis data. Code: ${analysisResponse.code()}.")
                    }
                }

                // Logic: Update the UI state with all the fetched data.
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