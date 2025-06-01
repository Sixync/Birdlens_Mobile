package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.android.birdlens.data.model.ebird.EbirdTaxonomy
import com.android.birdlens.data.model.wiki.WikiRetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BirdInfoUiState {
    object Idle : BirdInfoUiState()
    object Loading : BirdInfoUiState()
    data class Success(val birdData: EbirdTaxonomy, val imageUrl: String?) : BirdInfoUiState() // Added imageUrl
    data class Error(val message: String) : BirdInfoUiState()
}

class BirdInfoViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<BirdInfoUiState>(BirdInfoUiState.Idle)
    val uiState: StateFlow<BirdInfoUiState> = _uiState.asStateFlow()

    private val speciesCode: String? = savedStateHandle["speciesCode"]
    private var currentBirdData: EbirdTaxonomy? = null


    init {
        if (!speciesCode.isNullOrBlank()) {
            fetchBirdInformation(speciesCode)
        } else {
            _uiState.value = BirdInfoUiState.Error("Species code not provided.")
            Log.e("BirdInfoVM", "Species code is null or blank from SavedStateHandle.")
        }
    }

    fun fetchBirdInformation(code: String = speciesCode ?: "") {
        if (code.isBlank()) {
            _uiState.value = BirdInfoUiState.Error("Cannot fetch info: Species code is blank.")
            return
        }

        _uiState.value = BirdInfoUiState.Loading
        viewModelScope.launch {
            try {
                // Step 1: Fetch eBird Taxonomy
                val ebirdResponse = EbirdRetrofitInstance.api.getSpeciesTaxonomy(speciesCode = code)
                if (ebirdResponse.isSuccessful && ebirdResponse.body() != null) {
                    val taxonomyList = ebirdResponse.body()!!
                    if (taxonomyList.isNotEmpty()) {
                        currentBirdData = taxonomyList.first()
                        // Step 2: Fetch Wikipedia Image URL
                        fetchWikipediaImage(currentBirdData!!)
                    } else {
                        _uiState.value = BirdInfoUiState.Error("No taxonomy data found for species code: $code")
                        Log.w("BirdInfoVM", "Empty list returned for species code: $code")
                    }
                } else {
                    val errorBody = ebirdResponse.errorBody()?.string() ?: "Unknown eBird API error"
                    _uiState.value = BirdInfoUiState.Error("eBird API Error ${ebirdResponse.code()}: $errorBody")
                    Log.e("BirdInfoVM", "eBird API Error ${ebirdResponse.code()}: $errorBody")
                }
            } catch (e: Exception) {
                _uiState.value = BirdInfoUiState.Error("Network request failed: ${e.localizedMessage}")
                Log.e("BirdInfoVM", "Exception fetching bird info", e)
            }
        }
    }

    private suspend fun fetchWikipediaImage(birdData: EbirdTaxonomy) {
        try {
            Log.d("BirdInfoVM", "Fetching Wikipedia image for: ${birdData.commonName}")
            val wikiResponse = WikiRetrofitInstance.api.getPageImage(titles = birdData.commonName)
            if (wikiResponse.isSuccessful && wikiResponse.body() != null) {
                val wikiQueryResponse = wikiResponse.body()!!
                val pages = wikiQueryResponse.query?.pages
                if (pages != null && pages.isNotEmpty()) {
                    // The "pages" object has a dynamic key (page ID). Get the first page entry.
                    // Exclude negative page IDs which indicate missing pages.
                    val pageDetail = pages.values.firstOrNull { it.pageId != null && it.pageId > 0 }
                    val imageUrl = pageDetail?.thumbnail?.source
                    if (imageUrl != null) {
                        Log.d("BirdInfoVM", "Wikipedia image URL found: $imageUrl")
                        _uiState.value = BirdInfoUiState.Success(birdData, imageUrl)
                    } else {
                        Log.w("BirdInfoVM", "No image URL in Wikipedia response for ${birdData.commonName}. Thumbnail: ${pageDetail?.thumbnail}, PageImage: ${pageDetail?.pageImage}")
                        _uiState.value = BirdInfoUiState.Success(birdData, null) // Success with eBird, but no image
                    }
                } else {
                    Log.w("BirdInfoVM", "No pages found in Wikipedia response for ${birdData.commonName}")
                    _uiState.value = BirdInfoUiState.Success(birdData, null)
                }
            } else {
                val errorBody = wikiResponse.errorBody()?.string() ?: "Unknown Wikipedia API error"
                Log.e("BirdInfoVM", "Wikipedia API Error ${wikiResponse.code()}: $errorBody")
                _uiState.value = BirdInfoUiState.Success(birdData, null) // Still success for eBird part
            }
        } catch (e: Exception) {
            Log.e("BirdInfoVM", "Exception fetching Wikipedia image", e)
            _uiState.value = BirdInfoUiState.Success(birdData, null) // Still success for eBird part
        }
    }
}