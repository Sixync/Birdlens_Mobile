// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/BirdRangeMapViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.repository.SpeciesRepository
import com.android.birdlens.utils.ErrorUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Processed data class ready for the UI. It contains lists of LatLng points
 * for each type of habitat, parsed from the raw GeoJSON.
 */
data class SpeciesRangeData(
    val residentPolygons: List<List<LatLng>> = emptyList(),
    val breedingPolygons: List<List<LatLng>> = emptyList(),
    val nonBreedingPolygons: List<List<LatLng>> = emptyList(),
    val passagePolygons: List<List<LatLng>> = emptyList()
    // Add other presence types as needed
)

/**
 * Represents the UI state for the BirdRangeMapScreen.
 */
sealed class BirdRangeUiState {
    data object Loading : BirdRangeUiState()
    data class Success(val rangeData: SpeciesRangeData) : BirdRangeUiState()
    data class Error(val message: String) : BirdRangeUiState()
}

class BirdRangeMapViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val speciesRepository = SpeciesRepository(application.applicationContext)
    // Logic: Changed the key to 'scientificName' to match the updated navigation route.
    val scientificName: String = savedStateHandle["scientificName"] ?: ""

    private val _uiState = MutableStateFlow<BirdRangeUiState>(BirdRangeUiState.Loading)
    val uiState: StateFlow<BirdRangeUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "BirdRangeMapVM"
    }

    init {
        if (scientificName.isNotBlank()) {
            fetchRangeData()
        } else {
            _uiState.value = BirdRangeUiState.Error("Scientific name not provided.")
        }
    }

    fun fetchRangeData() {
        viewModelScope.launch {
            _uiState.value = BirdRangeUiState.Loading
            try {
                // Logic: Call the repository with the scientificName retrieved from navigation.
                val response = speciesRepository.getSpeciesRange(scientificName)
                if (response.isSuccessful && response.body()?.data != null) {
                    val parsedData = parseApiResponse(response.body()!!.data!!)
                    _uiState.value = BirdRangeUiState.Success(parsedData)
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = BirdRangeUiState.Error(ErrorUtils.extractMessage(errorBody, "Failed to load range data"))
                }
            } catch (e: Exception) {
                _uiState.value = BirdRangeUiState.Error(e.localizedMessage ?: "An unknown network error occurred.")
                Log.e(TAG, "Exception fetching species range", e)
            }
        }
    }

    /**
     * Parses the raw API response into a structured [SpeciesRangeData] object
     * with lists of LatLng points for easy rendering on the map.
     */
    private fun parseApiResponse(apiData: List<com.android.birdlens.data.model.ApiRangeData>): SpeciesRangeData {
        val resident = mutableListOf<List<LatLng>>()
        val breeding = mutableListOf<List<LatLng>>()
        val nonBreeding = mutableListOf<List<LatLng>>()
        val passage = mutableListOf<List<LatLng>>()

        apiData.forEach { rangeData ->
            val polygons = parseGeoJsonToLatLngLists(rangeData.geoJsonString)
            when (rangeData.presence) {
                1 -> resident.addAll(polygons) // Resident
                2 -> breeding.addAll(polygons) // Breeding Season
                3 -> nonBreeding.addAll(polygons) // Non-breeding Season
                4 -> passage.addAll(polygons) // Passage
            }
        }
        return SpeciesRangeData(resident, breeding, nonBreeding, passage)
    }

    /**
     * Parses a GeoJSON string into a list of polygons, where each polygon is a list of LatLng points.
     * This handles both "Polygon" and "MultiPolygon" GeoJSON types.
     */
    private fun parseGeoJsonToLatLngLists(geoJsonString: String): List<List<LatLng>> {
        val allPolygons = mutableListOf<List<LatLng>>()
        try {
            val geoJson = JSONObject(geoJsonString)
            val coordinates = geoJson.getJSONArray("coordinates")
            val type = geoJson.getString("type")

            if (type == "MultiPolygon") {
                for (i in 0 until coordinates.length()) {
                    val polygonArray = coordinates.getJSONArray(i)
                    val exteriorRing = polygonArray.getJSONArray(0)
                    val polygonPoints = mutableListOf<LatLng>()
                    for (j in 0 until exteriorRing.length()) {
                        val point = exteriorRing.getJSONArray(j)
                        polygonPoints.add(LatLng(point.getDouble(1), point.getDouble(0))) // GeoJSON is (lng, lat), LatLng is (lat, lng)
                    }
                    allPolygons.add(polygonPoints)
                }
            } else if (type == "Polygon") {
                val exteriorRing = coordinates.getJSONArray(0)
                val polygonPoints = mutableListOf<LatLng>()
                for (j in 0 until exteriorRing.length()) {
                    val point = exteriorRing.getJSONArray(j)
                    polygonPoints.add(LatLng(point.getDouble(1), point.getDouble(0)))
                }
                allPolygons.add(polygonPoints)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse GeoJSON string: ${e.message}")
        }
        return allPolygons
    }
}