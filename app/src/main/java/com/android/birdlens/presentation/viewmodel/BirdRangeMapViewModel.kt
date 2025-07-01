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
// Import LatLngBounds and its builder.
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Processed data class ready for the UI. It now includes the calculated
 * geographic bounds of all its polygons.
 */
data class SpeciesRangeData(
    val residentPolygons: List<List<LatLng>> = emptyList(),
    val breedingPolygons: List<List<LatLng>> = emptyList(),
    val nonBreedingPolygons: List<List<LatLng>> = emptyList(),
    val passagePolygons: List<List<LatLng>> = emptyList(),
    val bounds: LatLngBounds? = null // This will hold the encompassing bounds.
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

    private val _uiState = MutableStateFlow<BirdRangeUiState>(BirdRangeUiState.Loading)
    val uiState: StateFlow<BirdRangeUiState> = _uiState.asStateFlow()

    private val scientificName: String? = savedStateHandle["scientificName"]

    companion object {
        private const val TAG = "BirdRangeMapVM"
    }

    init {
        if (!scientificName.isNullOrBlank()) {
            Log.d(TAG, "ViewModel initialized. Fetching range data for: $scientificName")
            fetchRangeData(scientificName)
        } else {
            Log.e(TAG, "Scientific name is null or blank in SavedStateHandle.")
            _uiState.value = BirdRangeUiState.Error("Scientific name not provided.")
        }
    }

    fun fetchRangeData(scientificName: String) {
        viewModelScope.launch {
            _uiState.value = BirdRangeUiState.Loading
            try {
                val response = speciesRepository.getSpeciesRange(scientificName)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val parsedData = parseApiResponse(apiResponse.data)
                        _uiState.value = BirdRangeUiState.Success(parsedData)
                    } else {
                        val errorMessage = apiResponse.message ?: "API returned an error with no message."
                        _uiState.value = BirdRangeUiState.Error(errorMessage)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = BirdRangeUiState.Error(ErrorUtils.extractMessage(errorBody, "Failed to load range data"))
                }
            } catch (e: Exception) {
                _uiState.value = BirdRangeUiState.Error(e.localizedMessage ?: "An unknown network error occurred.")
                Log.e(TAG, "Exception fetching species range for '$scientificName'", e)
            }
        }
    }

    private fun parseApiResponse(apiData: List<com.android.birdlens.data.model.ApiRangeData>): SpeciesRangeData {
        val allPolygons = mutableListOf<List<LatLng>>()
        val resident = mutableListOf<List<LatLng>>()
        val breeding = mutableListOf<List<LatLng>>()
        val nonBreeding = mutableListOf<List<LatLng>>()
        val passage = mutableListOf<List<LatLng>>()

        apiData.forEach { rangeData ->
            if (!rangeData.geoJson.isNullOrBlank()) {
                val polygons = parseGeoJsonToLatLngLists(rangeData.geoJson)
                resident.addAll(polygons)
                // Add all polygons to a master list for bounds calculation.
                allPolygons.addAll(polygons)
            }
        }

        // Create a bounds builder.
        val boundsBuilder = LatLngBounds.Builder()
        var hasPoints = false
        // Include every point from every polygon into the builder.
        allPolygons.forEach { polygon ->
            polygon.forEach { point ->
                boundsBuilder.include(point)
                hasPoints = true
            }
        }

        // Build the bounds if points were added, otherwise it remains null.
        val bounds = if (hasPoints) boundsBuilder.build() else null

        return SpeciesRangeData(resident, breeding, nonBreeding, passage, bounds)
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
                // For MultiPolygon, the structure is [[[[lng, lat],...]]]]
                for (i in 0 until coordinates.length()) {
                    val polygonArray = coordinates.getJSONArray(i)
                    // Each polygon has one outer ring and potentially inner rings (holes).
                    // We are only interested in the outer ring, which is the first element.
                    val exteriorRing = polygonArray.getJSONArray(0)
                    val polygonPoints = mutableListOf<LatLng>()
                    for (j in 0 until exteriorRing.length()) {
                        val point = exteriorRing.getJSONArray(j)
                        // GeoJSON is (longitude, latitude), but Google Maps LatLng is (latitude, longitude).
                        polygonPoints.add(LatLng(point.getDouble(1), point.getDouble(0)))
                    }
                    allPolygons.add(polygonPoints)
                }
            } else if (type == "Polygon") {
                // For Polygon, the structure is [[[lng, lat],...]]
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