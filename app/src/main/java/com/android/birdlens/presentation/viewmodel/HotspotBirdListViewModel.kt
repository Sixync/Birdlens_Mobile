// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/HotspotBirdListViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdObservation
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.android.birdlens.data.model.ebird.EbirdTaxonomy
import com.android.birdlens.data.model.wiki.WikiRetrofitInstance // Added for Wikipedia API
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException // Added for explicit cancellation handling
import kotlinx.coroutines.awaitAll

data class BirdSpeciesInfo(
    val commonName: String,
    val speciesCode: String,
    val scientificName: String,
    val observationDate: String?,
    val count: Int?,
    val isRecent: Boolean,
    val imageUrl: String? = null // New field for the bird's image
)

sealed class HotspotBirdListUiState {
    data object Idle : HotspotBirdListUiState() // Re-add or ensure Idle is present
    data object Loading : HotspotBirdListUiState() // Simplified from Idle to Loading for initial state
    data class Success(
        val birds: List<BirdSpeciesInfo>,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : HotspotBirdListUiState()
    data class Error(val message: String) : HotspotBirdListUiState()
}

open class HotspotBirdListViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    internal val initialLocId: String? // Made internal for preview access if needed

    // Initialize to Idle
    val _uiState = MutableStateFlow<HotspotBirdListUiState>(HotspotBirdListUiState.Idle)
    val uiState: StateFlow<HotspotBirdListUiState> = _uiState.asStateFlow()

    private val ebirdApiService = EbirdRetrofitInstance.api
    private val wikiApiService = WikiRetrofitInstance.api // Added Wiki API service

    companion object {
        private const val TAG = "HotspotBirdListVM"
        private const val RECENT_DAYS_BACK = 30
        private const val DETAILS_PAGE_SIZE = 15 // Number of species details (taxonomy + image) to fetch per page
    }

    private var allSpeciesCodesForHotspot: List<String> = emptyList()
    private var recentObsMapForHotspot: Map<String, EbirdObservation> = emptyMap()
    private var currentDetailsPage = 0
    private var isLoadingDetails = false
    private var allDetailsLoaded = false

    init {
        initialLocId = savedStateHandle.get<String>("hotspotId")
        Log.d(TAG, "ViewModel initialized. Attempting to retrieve hotspotId.")
        if (initialLocId.isNullOrBlank()) {
            Log.e(TAG, "INIT_ERROR: Hotspot ID (locId) is null or blank from SavedStateHandle.")
            _uiState.value =
                HotspotBirdListUiState.Error("Hotspot ID not provided. Cannot load bird list.")
        } else {
            Log.i(
                TAG,
                "INIT_SUCCESS: Valid Hotspot ID '$initialLocId' received. Triggering fetch."
            )
            // ViewModel is created, now trigger the first fetch if locId is valid.
            // The fetch function will set it to Loading.
            fetchAllSpeciesCodesAndInitialDetails(initialLocId)
        }
    }

    private fun fetchAllSpeciesCodesAndInitialDetails(locId: String) {
        if (_uiState.value is HotspotBirdListUiState.Error && initialLocId.isNullOrBlank()) {
            Log.w(
                TAG,
                "FETCH_SKIP: ViewModel is in an initial error state due to missing locId. Skipping fetch for '$locId'."
            )
            return
        }
        // Set to Loading when fetch actually starts
        _uiState.value = HotspotBirdListUiState.Loading
        Log.i(TAG, "INIT_FETCH_START: Attempting to fetch all species codes and recent observations for locId: '$locId'")
        viewModelScope.launch {
            try {
                coroutineScope {
                    val allSpeciesCodesDeferred = async {
                        ebirdApiService.getSpeciesListForHotspot(locId)
                    }
                    val recentObservationsDeferred = async {
                        ebirdApiService.getRecentObservationsForHotspot(
                            locId = locId,
                            back = RECENT_DAYS_BACK,
                            detail = "simple",
                            maxResults = 1000 // Fetch enough recent observations
                        )
                    }
                    val allSpeciesResponse = allSpeciesCodesDeferred.await()
                    val recentObsResponse = recentObservationsDeferred.await()

                    if (!allSpeciesResponse.isSuccessful || allSpeciesResponse.body() == null) {
                        val errorBody = allSpeciesResponse.errorBody()?.string()
                        val extractedMessage = ErrorUtils.extractMessage(errorBody, "Failed to fetch species list (HTTP ${allSpeciesResponse.code()})")
                        Log.e(TAG, "Failed to fetch species list for hotspot '$locId'. Code: ${allSpeciesResponse.code()}. Full error body: $errorBody")
                        _uiState.value = HotspotBirdListUiState.Error(extractedMessage)
                        return@coroutineScope
                    }
                    allSpeciesCodesForHotspot = allSpeciesResponse.body()!!

                    recentObsMapForHotspot = if (recentObsResponse.isSuccessful && recentObsResponse.body() != null) {
                        recentObsResponse.body()!!.associateBy { it.speciesCode }
                    } else {
                        Log.w(TAG, "Failed to fetch recent observations for '$locId'. Code: ${recentObsResponse.code()}. Proceeding without recency info for some species.")
                        emptyMap()
                    }

                    if (allSpeciesCodesForHotspot.isEmpty()) {
                        Log.i(TAG, "No species ever recorded at hotspot '$locId'.")
                        _uiState.value = HotspotBirdListUiState.Success(emptyList(), canLoadMore = false)
                        allDetailsLoaded = true
                        return@coroutineScope
                    }
                }

                currentDetailsPage = 0
                allDetailsLoaded = false
                loadPageOfBirdDetailsAndUpdateState(mutableListOf(), isInitialLoad = true)

            } catch (e: Exception) {
                val errorMsg = "Error fetching initial data for hotspot '$locId': ${e.javaClass.simpleName} - ${e.localizedMessage ?: "Unknown error"}"
                _uiState.value = HotspotBirdListUiState.Error(errorMsg)
                Log.e(TAG, "INIT_FETCH_EXCEPTION: $errorMsg", e)
            }
        }
    }

    fun loadMoreBirdDetails() {
        if (isLoadingDetails || allDetailsLoaded) {
            Log.d(TAG, "LoadMore SKIPPED: isLoadingDetails=$isLoadingDetails, allDetailsLoaded=$allDetailsLoaded")
            return
        }
        Log.d(TAG, "LoadMore TRIGGERED for page: $currentDetailsPage")

        viewModelScope.launch {
            isLoadingDetails = true
            val currentBirds = (_uiState.value as? HotspotBirdListUiState.Success)?.birds ?: emptyList()

            if (_uiState.value is HotspotBirdListUiState.Success) {
                _uiState.update {
                    (it as HotspotBirdListUiState.Success).copy(isLoadingMore = true)
                }
            }

            loadPageOfBirdDetailsAndUpdateState(currentBirds.toMutableList(), isInitialLoad = false)
            isLoadingDetails = false // Ensure this is reset even if an error occurs in loadPage
        }
    }

    private suspend fun loadPageOfBirdDetailsAndUpdateState(existingBirds: MutableList<BirdSpeciesInfo>, isInitialLoad: Boolean) {
        val startIndex = currentDetailsPage * DETAILS_PAGE_SIZE
        val endIndex = minOf(startIndex + DETAILS_PAGE_SIZE, allSpeciesCodesForHotspot.size)

        if (startIndex >= allSpeciesCodesForHotspot.size) {
            allDetailsLoaded = true
            _uiState.update {
                if (it is HotspotBirdListUiState.Success) {
                    it.copy(canLoadMore = false, isLoadingMore = false)
                } else {
                    // If state is not Success (e.g. Error, Loading), create a new Success state
                    HotspotBirdListUiState.Success(existingBirds, canLoadMore = false, isLoadingMore = false)
                }
            }
            Log.d(TAG, "All details loaded. Total: ${existingBirds.size}")
            return
        }

        val codesToFetchDetails = allSpeciesCodesForHotspot.subList(startIndex, endIndex)
        if (codesToFetchDetails.isEmpty()) {
            allDetailsLoaded = true
            _uiState.update {
                if (it is HotspotBirdListUiState.Success) {
                    it.copy(canLoadMore = false, isLoadingMore = false)
                } else {
                    HotspotBirdListUiState.Success(existingBirds, canLoadMore = false, isLoadingMore = false)
                }
            }
            Log.d(TAG, "No more codes to fetch details for this page.")
            return
        }

        Log.d(TAG, "Loading details for page $currentDetailsPage, codes: $codesToFetchDetails")

        try {
            val taxonomyResponse = ebirdApiService.getSpeciesTaxonomy(speciesCodes = codesToFetchDetails.joinToString(","))
            if (!taxonomyResponse.isSuccessful || taxonomyResponse.body() == null) {
                val errorMsg = "Failed to fetch taxonomy details for page $currentDetailsPage."
                Log.e(TAG, errorMsg)
                _uiState.update {
                    if (it is HotspotBirdListUiState.Success) it.copy(isLoadingMore = false) // Stop loading more indicator
                    else HotspotBirdListUiState.Error(errorMsg) // If not success, set to error
                }
                return
            }
            val taxonomyPageList = taxonomyResponse.body()!!

            val birdInfoDetailsForPage = coroutineScope {
                taxonomyPageList.map { taxon ->
                    async {
                        if (taxon.speciesCode.isBlank() || taxon.commonName.isBlank()) {
                            Log.w(TAG, "TRANSFORM_WARN: Skipping taxon due to blank data: $taxon")
                            return@async null
                        }
                        val imageUrl = fetchImageUrlForBird(taxon.commonName)
                        val recentObservation = recentObsMapForHotspot[taxon.speciesCode]
                        BirdSpeciesInfo(
                            commonName = taxon.commonName,
                            speciesCode = taxon.speciesCode,
                            scientificName = taxon.scientificName,
                            observationDate = recentObservation?.obsDt?.split(" ")?.firstOrNull(),
                            count = recentObservation?.howMany,
                            isRecent = recentObservation != null,
                            imageUrl = imageUrl
                        )
                    }
                }.awaitAll().filterNotNull()
            }

            existingBirds.addAll(birdInfoDetailsForPage)
            currentDetailsPage++
            allDetailsLoaded = endIndex >= allSpeciesCodesForHotspot.size || birdInfoDetailsForPage.isEmpty()

            val finalBirdList = existingBirds.distinctBy { it.speciesCode }.sortedWith(
                compareByDescending<BirdSpeciesInfo> { it.isRecent }
                    .thenBy { it.commonName }
            )
            _uiState.value = HotspotBirdListUiState.Success(finalBirdList, canLoadMore = !allDetailsLoaded, isLoadingMore = false)
            Log.d(TAG, "Page $currentDetailsPage loaded. Total birds: ${finalBirdList.size}. Can load more: ${!allDetailsLoaded}")

        } catch (e: Exception) {
            if (e is CancellationException) {
                Log.i(TAG, "Detail fetch for page $currentDetailsPage was cancelled.")
                _uiState.update { if (it is HotspotBirdListUiState.Success) it.copy(isLoadingMore = false) else it }
            } else {
                val errorMsg = "Error loading page $currentDetailsPage details: ${e.localizedMessage}"
                Log.e(TAG, errorMsg, e)
                _uiState.update {
                    if (it is HotspotBirdListUiState.Success) it.copy(isLoadingMore = false) // Stop loading more indicator
                    else HotspotBirdListUiState.Error(errorMsg)
                }
            }
        }
    }

    private suspend fun fetchImageUrlForBird(commonName: String): String? {
        if (commonName.isBlank()) return null
        return try {
            val wikiResponse = wikiApiService.getPageImage(titles = commonName)
            if (wikiResponse.isSuccessful && wikiResponse.body() != null) {
                val pageDetail = wikiResponse.body()!!.query?.pages?.values?.firstOrNull { it.thumbnail?.source != null }
                pageDetail?.thumbnail?.source.also {
                    if (it == null) Log.w(TAG, "No Wikipedia image source found for: $commonName")
                }
            } else {
                Log.w(TAG, "Wikipedia API error for $commonName: ${wikiResponse.code()} - ${wikiResponse.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching Wikipedia image for $commonName", e)
            null
        }
    }

    fun refreshData() {
        if (!initialLocId.isNullOrBlank()) {
            Log.i(TAG, "REFRESH_TRIGGERED: Refreshing all data for initialLocId: '$initialLocId'")
            fetchAllSpeciesCodesAndInitialDetails(initialLocId)
        } else {
            Log.e(TAG, "REFRESH_FAIL: Cannot refresh, initialLocId is missing or blank.")
            if (_uiState.value !is HotspotBirdListUiState.Error) {
                _uiState.value =
                    HotspotBirdListUiState.Error("Cannot refresh: Hotspot ID is invalid.")
            }
        }
    }
}