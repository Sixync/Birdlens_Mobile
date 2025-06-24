package com.android.birdlens.data.model.ebird

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.google.maps.android.clustering.ClusterItem

// For /v2/ref/hotspot/geo response
data class EbirdNearbyHotspot(
    @SerializedName("locId") val locId: String,
    @SerializedName("locName") val locName: String,
    @SerializedName("countryCode") val countryCode: String,
    @SerializedName("subnational1Code") val subnational1Code: String?, // e.g., state code
    @SerializedName("subnational2Code") val subnational2Code: String?, // e.g., county code
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("latestObsDt") val latestObsDt: String?, // Date of the latest observation
    @SerializedName("numSpeciesAllTime") val numSpeciesAllTime: Int?
): ClusterItem { // Implement ClusterItem

    // Implement ClusterItem properties
    override fun getPosition(): LatLng {
        return LatLng(lat, lng)
    }

    override fun getTitle(): String? {
        return locName
    }

    override fun getSnippet(): String? {
        return "Species: ${numSpeciesAllTime ?: "N/A"}"
    }

    // Optional: zIndex can be used to control marker stacking order
    override fun getZIndex(): Float? {
        return 0f // Default zIndex
    }
}