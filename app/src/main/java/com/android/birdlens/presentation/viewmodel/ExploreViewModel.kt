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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A new data class to hold the combined data for the UI
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
    data class Success(val results: List<BirdSpecies>) : ExploreUiState()
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
     * with images from Wikipedia.
     */
    fun fetchNotableBirds() {
        viewModelScope.launch {
            _uiState.value = ExploreUiState.Searching
            try {
                val homeCountryCode = UserSettingsManager.getHomeCountrySetting(context).code
                val notableObservations = hotspotRepository.getNotableBirds(homeCountryCode)

                if (notableObservations.isEmpty()) {
                    _uiState.value = ExploreUiState.Error("No notable birds found in your region recently.")
                    return@launch
                }

                // Concurrently fetch images for each notable bird
                val enrichedBirds = notableObservations.map { obs ->
                    async {
                        val imageUrl = getImageUrlForTitle(obs.comName)
                        BirdExploreInfo(
                            speciesCode = obs.speciesCode,
                            commonName = obs.comName,
                            sciName = obs.sciName,
                            imageUrl = imageUrl
                        )
                    }
                }.awaitAll()

                _uiState.value = ExploreUiState.ExploreFeedSuccess(enrichedBirds)
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error("Failed to load notable birds: ${e.localizedMessage}")
            }
        }
    }


    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.length < 2) { // Allow searching from 2 characters
            // When the query is too short, revert to the notable birds feed.
            fetchNotableBirds()
            return
        }

        _uiState.value = ExploreUiState.Searching
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            try {
                // Perform the search using the local BirdSpeciesRepository.
                val results = birdSpeciesRepository.searchBirds(query)
                // The Success state now holds a list of BirdSpecies.
                _uiState.value = ExploreUiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error("Search failed: ${e.localizedMessage}")
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _searchQuery.value = ""
        // After clearing, refetch the notable birds to restore the default explore view.
        fetchNotableBirds()
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