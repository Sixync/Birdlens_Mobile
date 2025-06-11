// app/src/main/java/com/android/birdlens/presentation/viewmodel/BirdInfoViewModel.kt
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
                        val birdData = taxonomyList.first()
                        Log.d(TAG, "eBird taxonomy fetched successfully for ${birdData.commonName}.")
                        // This now triggers the new image fetching logic
                        fetchBestWikipediaImage(birdData)
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

    /**
     * Fetches the best possible image from Wikipedia.
     * It first tries the common name. If that fails (e.g., it's a family page with no image),
     * it tries to find a representative species from the family and fetches its image.
     */
    private suspend fun fetchBestWikipediaImage(birdData: EbirdTaxonomy) {
        // 1. Primary attempt using the common name
        val primaryImageUrl = getImageUrlForTitle(birdData.commonName)

        if (primaryImageUrl != null) {
            // Success on first try
            Log.d(TAG, "Primary Wikipedia image found for ${birdData.commonName}: $primaryImageUrl")
            _uiState.value = BirdInfoUiState.Success(birdData, primaryImageUrl)
        } else {
            // 2. Fallback attempt for families/groups
            Log.w(TAG, "No direct image for '${birdData.commonName}'. Trying fallback using family name.")

            // The scientific name of the family is the most reliable for Wikipedia categories
            val familySciName = birdData.familyScientificName
            if (familySciName.isNullOrBlank()) {
                Log.w(TAG, "Fallback failed: No family scientific name available for ${birdData.commonName}.")
                _uiState.value = BirdInfoUiState.Success(birdData, null) // Final state: no image
                return
            }

            // Fetch a representative species from the family category on Wikipedia
            val representativeSpeciesTitle = getRepresentativeSpeciesFromFamily(familySciName)

            if (representativeSpeciesTitle != null) {
                Log.d(TAG, "Found representative species '$representativeSpeciesTitle' for family '$familySciName'. Fetching its image.")
                val fallbackImageUrl = getImageUrlForTitle(representativeSpeciesTitle)
                if (fallbackImageUrl != null) {
                    Log.d(TAG, "Fallback image found and being used: $fallbackImageUrl")
                    _uiState.value = BirdInfoUiState.Success(birdData, fallbackImageUrl)
                } else {
                    Log.w(TAG, "Fallback failed: Could not get image for representative species '$representativeSpeciesTitle'.")
                    _uiState.value = BirdInfoUiState.Success(birdData, null) // Final state: no image
                }
            } else {
                Log.w(TAG, "Fallback failed: Could not find any representative species for family '$familySciName'.")
                _uiState.value = BirdInfoUiState.Success(birdData, null) // Final state: no image
            }
        }
    }

    /**
     * Fetches the image URL for a given Wikipedia page title.
     * @return URL string or null if not found or on error.
     */
    private suspend fun getImageUrlForTitle(title: String): String? {
        try {
            val response = WikiRetrofitInstance.api.getPageImage(titles = title)
            if (response.isSuccessful) {
                val pages = response.body()?.query?.pages
                // Find the first valid page with a non-null thumbnail source
                val pageDetail = pages?.values?.firstOrNull { it.thumbnail?.source != null }
                return pageDetail?.thumbnail?.source
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching Wikipedia image for title '$title'", e)
        }
        return null
    }

    /**
     * Gets a title of a representative species from a Wikipedia category based on family name.
     * @return A species page title string, or null if not found.
     */
    private suspend fun getRepresentativeSpeciesFromFamily(familySciName: String): String? {
        try {
            val categoryTitle = "Category:$familySciName"
            Log.d(TAG, "Querying Wikipedia for members of '$categoryTitle'")
            val response = WikiRetrofitInstance.api.getCategoryMembers(cmTitle = categoryTitle)

            if (response.isSuccessful) {
                val members = response.body()?.query?.categoryMembers
                // Find the first member that doesn't seem like a list or another category
                return members?.firstOrNull { member ->
                    !member.title.contains("list of", ignoreCase = true) &&
                            !member.title.startsWith("Category:", ignoreCase = true) &&
                            !member.title.contains("template", ignoreCase = true)
                }?.title
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting representative species for '$familySciName'", e)
        }
        return null
    }
}