// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/ExploreViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.UserSettingsManager
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.model.wiki.WikiRetrofitInstance
import com.android.birdlens.data.repository.BirdSpeciesRepository
import com.android.birdlens.data.repository.HotspotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// A data class to hold the combined data for the UI
data class BirdExploreInfo(
    val speciesCode: String,
    val commonName: String,
    val sciName: String,
    val imageUrl: String?
)

sealed class ExploreUiState {
    data object Idle : ExploreUiState()
    data object Searching : ExploreUiState()
    // The Success state now holds a list of BirdSpecies from the local database.
    data class Success(
        val birds: List<BirdExploreInfo>,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : ExploreUiState()
    data class ExploreFeedSuccess(val notableBirds: List<BirdExploreInfo>) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}

class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    // The ViewModel now uses both repositories for their respective tasks.
    private val hotspotRepository = HotspotRepository(application)
    private val birdSpeciesRepository = BirdSpeciesRepository(application)
    private var searchJob: Job? = null
    private val context = application.applicationContext
    private val wikiApiService = WikiRetrofitInstance.api

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Idle)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    // A simple in-memory cache to hold the notable birds feed.
    private var notableBirdsCache: List<BirdExploreInfo>? = null

    // State for managing search result pagination
    private var fullSearchResultList: List<BirdSpecies> = emptyList()
    private var currentSearchPage = 0
    private var isSearchLoading = false
    private val searchPageSize = 15

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 350L
        private const val TAG = "ExploreViewModel"
    }

    init {
        // Automatically fetch the notable birds for the user's home country on init.
        fetchNotableBirds()
    }

    /**
     * Fetches a list of notable birds for the user's home region and enriches them
     * with images from Wikipedia. This now uses an in-memory cache to prevent re-fetching.
     */
    fun fetchNotableBirds() {
        // If the cache already has data, use it immediately to prevent re-loading.
        notableBirdsCache?.let {
            _uiState.value = ExploreUiState.ExploreFeedSuccess(it)
            return
        }

        viewModelScope.launch {
            _uiState.value = ExploreUiState.Searching
            try {
                val homeCountryCode = UserSettingsManager.getHomeCountrySetting(context).code
                val notableObservations = hotspotRepository.getNotableBirds(homeCountryCode)

                if (notableObservations.isEmpty()) {
                    _uiState.value = ExploreUiState.ExploreFeedSuccess(emptyList()) // Show empty feed
                    return@launch
                }

                // Correctly use coroutineScope to call async.
                val enrichedBirds = coroutineScope {
                    notableObservations.map { obs ->
                        async {
                            BirdExploreInfo(
                                speciesCode = obs.speciesCode,
                                commonName = obs.comName,
                                sciName = obs.sciName,
                                imageUrl = getImageUrlForTitle(obs.comName)
                            )
                        }
                    }.awaitAll()
                }

                // Store the fetched data in the cache before updating the UI.
                notableBirdsCache = enrichedBirds
                _uiState.value = ExploreUiState.ExploreFeedSuccess(enrichedBirds)
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error("Failed to load notable birds: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Handles user input in the search bar. It uses a debounce to prevent excessive searching
     * and queries the local database for fast results.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.length < 2) { // Allow searching from 2 characters
            // When the query is short, restore the view from the cache instead of re-fetching.
            notableBirdsCache?.let {
                _uiState.value = ExploreUiState.ExploreFeedSuccess(it)
            } ?: fetchNotableBirds() // Fallback if cache is empty for some reason.
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.value = ExploreUiState.Searching
            try {
                // Step 1: Get the full list of matching species from the local DB.
                fullSearchResultList = birdSpeciesRepository.searchBirds(query)
                currentSearchPage = 0
                isSearchLoading = false
                // Step 2: Load the first page of enriched data (with images).
                loadSearchResultsPage(isInitialLoad = true)
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error("Search failed: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Called by the UI when the user scrolls to the end of the search results list.
     */
    fun loadMoreSearchResults() {
        if (isSearchLoading) return
        viewModelScope.launch {
            loadSearchResultsPage(isInitialLoad = false)
        }
    }

    /**
     * Fetches details (like images) for the next page of search results.
     */
    private suspend fun loadSearchResultsPage(isInitialLoad: Boolean) {
        if (isSearchLoading) return
        isSearchLoading = true

        if (!isInitialLoad) {
            (_uiState.value as? ExploreUiState.Success)?.let {
                _uiState.value = it.copy(isLoadingMore = true)
            }
        }

        val startIndex = currentSearchPage * searchPageSize
        if (startIndex >= fullSearchResultList.size) {
            (_uiState.value as? ExploreUiState.Success)?.let {
                _uiState.value = it.copy(isLoadingMore = false, canLoadMore = false)
            }
            isSearchLoading = false
            return
        }

        val endIndex = (startIndex + searchPageSize).coerceAtMost(fullSearchResultList.size)
        val speciesToFetch = fullSearchResultList.subList(startIndex, endIndex)

        try {
            // Correctly use coroutineScope to call async within the map function.
            val enrichedResults = coroutineScope {
                speciesToFetch.map { bird ->
                    async {
                        BirdExploreInfo(
                            speciesCode = bird.speciesCode,
                            commonName = bird.commonName,
                            sciName = bird.scientificName,
                            imageUrl = getImageUrlForTitle(bird.commonName)
                        )
                    }
                }.awaitAll()
            }


            val currentResults = if (isInitialLoad) emptyList() else (_uiState.value as? ExploreUiState.Success)?.birds ?: emptyList()
            val newTotalResults = currentResults + enrichedResults
            val canLoadMore = newTotalResults.size < fullSearchResultList.size

            _uiState.value = ExploreUiState.Success(newTotalResults, canLoadMore, isLoadingMore = false)
            currentSearchPage++
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load search results page: ${e.localizedMessage}")
            (_uiState.value as? ExploreUiState.Success)?.let {
                _uiState.value = it.copy(isLoadingMore = false)
            }
        } finally {
            isSearchLoading = false
        }
    }

    /**
     * Clears the search query and restores the default "notable birds" feed from the cache.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        fullSearchResultList = emptyList()
        // Instantly restore from cache to avoid flicker and network calls.
        notableBirdsCache?.let {
            _uiState.value = ExploreUiState.ExploreFeedSuccess(it)
        } ?: fetchNotableBirds() // Fallback if cache is somehow empty.
    }

    private suspend fun getImageUrlForTitle(title: String): String? {
        return try {
            val response = wikiApiService.getPageImage(titles = title)
            if (response.isSuccessful) {
                response.body()?.query?.pages?.values?.firstOrNull { it.thumbnail?.source != null }?.thumbnail?.source
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching Wikipedia image for title '$title'", e)
            null
        }
    }
}