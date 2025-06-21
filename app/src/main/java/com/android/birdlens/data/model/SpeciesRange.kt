// EXE201/app/src/main/java/com/android/birdlens/data/model/SpeciesRange.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

/**
 * Represents the top-level API response from the /species/{id}/range endpoint.
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
    @SerializedName("presence") val presence: Int,
    @SerializedName("origin") val origin: Int,
    // The GeoJSON is received as a raw string from the backend.
    @SerializedName("geo_json") val geoJsonString: String
)