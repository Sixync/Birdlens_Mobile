package com.android.birdlens.data.model.ebird

import com.google.gson.annotations.SerializedName

data class EbirdTaxonomy(
    @SerializedName("sciName")
    val scientificName: String,
    @SerializedName("comName")
    val commonName: String,
    @SerializedName("speciesCode")
    val speciesCode: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("taxonOrder")
    val taxonOrder: Double?,
    @SerializedName("order")
    val birdOrder: String?, // API field is "order", renamed to avoid conflict with Kotlin keyword
    @SerializedName("familyComName")
    val familyCommonName: String?,
    @SerializedName("familySciName")
    val familyScientificName: String?
    // Add other fields as needed from the eBird API documentation
    // For example, image URL if available directly, or links to Macaulay Library.
    // For this example, we'll assume no direct image URL from this specific endpoint.
)