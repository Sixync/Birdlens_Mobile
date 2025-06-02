package com.android.birdlens.data.model.ebird

import com.google.gson.annotations.SerializedName

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
)