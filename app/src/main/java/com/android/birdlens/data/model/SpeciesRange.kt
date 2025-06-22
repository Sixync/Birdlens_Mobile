// EXE201/app/src/main/java/com/android/birdlens/data/model/SpeciesRange.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level API response from the /species/range endpoint.
 */
data class SpeciesRangeApiResponse(
    @SerializedName("error") val error: Boolean,
    @SerializedName("data") val data: List<ApiRangeData>?,
    @SerializedName("message") val message: String
)

/**
 * Represents a single range object from the backend API's data array.
 */
data class ApiRangeData(
    // Logic: Changed the type from a custom wrapper to a nullable String.
    // The backend sends the GeoJSON data as a double-encoded JSON string.
    // This model now correctly expects a String, which will be parsed into
    // a GeoJSON object in the ViewModel.
    @SerializedName("geo_json") val geoJson: String?
)