// Birdlens_Mobile/app/src/main/java/com/android/birdlens/presentation/viewmodel/MapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.data.model.ebird.EbirdRetrofitInstance
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MapUiState {
    object Idle : MapUiState()
    object Loading : MapUiState()
    data class Success(val hotspots: List<EbirdNearbyHotspot>) : MapUiState()
    data class Error(val message: String) : MapUiState()
}

class MapViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MapUiState>(MapUiState.Idle)
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val ebirdApiService = EbirdRetrofitInstance.api

    fun fetchNearbyHotspots(center: LatLng, radiusKm: Int = 25) {
        _uiState.value = MapUiState.Loading
        viewModelScope.launch {
            try {
                val response = ebirdApiService.getNearbyHotspots(
                    lat = center.latitude,
                    lng = center.longitude,
                    dist = radiusKm
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = MapUiState.Success(response.body()!!)
                    Log.d("MapViewModel", "Fetched ${response.body()!!.size} hotspots.")
                } else {
                    val errorMsg = "Error fetching hotspots: ${response.code()} - ${response.message()}"
                    _uiState.value = MapUiState.Error(errorMsg)
                    Log.e("MapViewModel", errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Exception fetching hotspots: ${e.localizedMessage}"
                _uiState.value = MapUiState.Error(errorMsg)
                Log.e("MapViewModel", errorMsg, e)
            }
        }
    }
}