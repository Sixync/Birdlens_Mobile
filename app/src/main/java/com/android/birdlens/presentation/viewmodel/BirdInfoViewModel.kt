package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.android.birdlens.data.model.ebird.EbirdTaxonomy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BirdInfoUiState {
    object Idle : BirdInfoUiState()
    object Loading : BirdInfoUiState()
    data class Success(val birdData: EbirdTaxonomy) : BirdInfoUiState()
    data class Error(val message: String) : BirdInfoUiState()
}

class BirdInfoViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<BirdInfoUiState>(BirdInfoUiState.Idle)
    val uiState: StateFlow<BirdInfoUiState> = _uiState.asStateFlow()

    private val speciesCode: String? = savedStateHandle["speciesCode"]

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
                val response = EbirdRetrofitInstance.api.getSpeciesTaxonomy(speciesCode = code)
                if (response.isSuccessful && response.body() != null) {
                    val taxonomyList = response.body()!!
                    if (taxonomyList.isNotEmpty()) {
                        _uiState.value = BirdInfoUiState.Success(taxonomyList.first())
                    } else {
                        _uiState.value = BirdInfoUiState.Error("No taxonomy data found for species code: $code")
                        Log.w("BirdInfoVM", "Empty list returned for species code: $code")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                    _uiState.value = BirdInfoUiState.Error("API Error ${response.code()}: $errorBody")
                    Log.e("BirdInfoVM", "API Error ${response.code()}: $errorBody")
                }
            } catch (e: Exception) {
                _uiState.value = BirdInfoUiState.Error("Network request failed: ${e.localizedMessage}")
                Log.e("BirdInfoVM", "Exception fetching bird info", e)
            }
        }
    }
}