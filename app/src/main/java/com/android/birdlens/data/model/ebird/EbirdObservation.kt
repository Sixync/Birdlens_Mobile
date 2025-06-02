package com.android.birdlens.data.model.ebird
import com.google.gson.annotations.SerializedName

data class EbirdObservation(
    @SerializedName("speciesCode") val speciesCode: String,
    @SerializedName("comName") val comName: String,
    @SerializedName("sciName") val sciName: String,
    @SerializedName("locId") val locId: String, // Hotspot ID where observed
    @SerializedName("locName") val locName: String, // Hotspot Name
    @SerializedName("obsDt") val obsDt: String, // Observation date and time "YYYY-MM-DD HH:mm"
    @SerializedName("howMany") val howMany: Int?, // Count observed
    @SerializedName("lat") val lat: Double, // Observation latitude (might differ slightly from hotspot center)
    @SerializedName("lng") val lng: Double, // Observation longitude
    @SerializedName("obsValid") val obsValid: Boolean,
    @SerializedName("obsReviewed") val obsReviewed: Boolean,
    @SerializedName("locationPrivate") val locationPrivate: Boolean,
    @SerializedName("subId") val subId: String // Checklist/submission ID
    // Add other fields if needed, like userDisplayName, obsId, etc.
)