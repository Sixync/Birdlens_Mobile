// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/BirdInfoViewModel.kt
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
    data object Idle : BirdInfoUiState()
    data object Loading : BirdInfoUiState()
    data class Success(val birdData: EbirdTaxonomy, val imageUrl: String?) : BirdInfoUiState()
    data class Error(val message: String) : BirdInfoUiState()
}

open class BirdInfoViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val _uiState = MutableStateFlow<BirdInfoUiState>(BirdInfoUiState.Idle)
    val uiState: StateFlow<BirdInfoUiState> = _uiState.asStateFlow()

    // Hold the speciesCode from SavedStateHandle
    private val speciesCode: String? = savedStateHandle["speciesCode"]
    private var currentBirdData: EbirdTaxonomy? = null // Cache fetched bird data

    companion object {
        private const val TAG = "BirdInfoVM"
    }

    init {
        if (!speciesCode.isNullOrBlank()) {
            Log.d(TAG, "ViewModel initialized with speciesCode: $speciesCode. Fetching bird info.")
            fetchBirdInformation(speciesCode)
        } else {
            val errorMsg = "Species code not provided to BirdInfoViewModel."
            _uiState.value = BirdInfoUiState.Error(errorMsg)
            Log.e(TAG, errorMsg)
        }
    }

    // Public function to allow retry from UI
    fun fetchBirdInformation() {
        if (!speciesCode.isNullOrBlank()) {
            fetchBirdInformation(speciesCode)
        } else {
            Log.e(TAG, "Retry fetch called, but speciesCode is still missing.")
            // State should already be Error if speciesCode was initially null/blank
        }
    }

    private fun fetchBirdInformation(code: String) {
        if (code.isBlank()) {
            _uiState.value = BirdInfoUiState.Error("Cannot fetch info: Species code is blank.")
            return
        }

        _uiState.value = BirdInfoUiState.Loading
        viewModelScope.launch {
            try {
                Log.d(TAG, "Fetching eBird taxonomy for species code: $code")
                val ebirdResponse = EbirdRetrofitInstance.api.getSpeciesTaxonomy(
                    speciesCodes = code,
                )
                if (ebirdResponse.isSuccessful && ebirdResponse.body() != null) {
                    val taxonomyList = ebirdResponse.body()!!
                    if (taxonomyList.isNotEmpty()) {
                        currentBirdData = taxonomyList.first()
                        Log.d(TAG, "eBird taxonomy fetched successfully for ${currentBirdData!!.commonName}.")
                        fetchWikipediaImage(currentBirdData!!)
                    } else {
                        val errorMsg = "No taxonomy data found for species code: $code"
                        _uiState.value = BirdInfoUiState.Error(errorMsg)
                        Log.w(TAG, errorMsg)
                    }
                } else {
                    val errorBody = ebirdResponse.errorBody()?.string() ?: "Unknown eBird API error"
                    val errorMsg = "eBird API Error ${ebirdResponse.code()}: $errorBody"
                    _uiState.value = BirdInfoUiState.Error(errorMsg)
                    Log.e(TAG, errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Network request failed while fetching eBird data: ${e.localizedMessage}"
                _uiState.value = BirdInfoUiState.Error(errorMsg)
                Log.e(TAG, "Exception fetching bird info from eBird", e)
            }
        }
    }

    private suspend fun fetchWikipediaImage(birdData: EbirdTaxonomy) {
        try {
            Log.d(TAG, "Fetching Wikipedia image for: ${birdData.commonName}")
            val wikiResponse = WikiRetrofitInstance.api.getPageImage(titles = birdData.commonName)
            if (wikiResponse.isSuccessful && wikiResponse.body() != null) {
                val wikiQueryResponse = wikiResponse.body()!!
                val pages = wikiQueryResponse.query?.pages
                if (pages != null && pages.isNotEmpty()) {
                    val pageDetail = pages.values.firstOrNull { it.pageId != null && it.pageId > 0 }
                    val imageUrl = pageDetail?.thumbnail?.source
                    if (imageUrl != null) {
                        Log.d(TAG, "Wikipedia image URL found: $imageUrl")
                        _uiState.value = BirdInfoUiState.Success(birdData, imageUrl)
                    } else {
                        Log.w(TAG, "No image URL in Wikipedia response for ${birdData.commonName}. Thumbnail: ${pageDetail?.thumbnail}, PageImage: ${pageDetail?.pageImage}")
                        _uiState.value = BirdInfoUiState.Success(birdData, null)
                    }
                } else {
                    Log.w(TAG, "No pages found in Wikipedia response for ${birdData.commonName}")
                    _uiState.value = BirdInfoUiState.Success(birdData, null)
                }
            } else {
                val errorBody = wikiResponse.errorBody()?.string() ?: "Unknown Wikipedia API error"
                Log.e(TAG, "Wikipedia API Error ${wikiResponse.code()}: $errorBody")
                _uiState.value = BirdInfoUiState.Success(birdData, null) // Still success for eBird part
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching Wikipedia image for ${birdData.commonName}", e)
            _uiState.value = BirdInfoUiState.Success(birdData, null) // Still success for eBird part
        }
    }
}