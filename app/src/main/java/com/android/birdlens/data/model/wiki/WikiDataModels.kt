package com.android.birdlens.data.model.wiki

import com.google.gson.annotations.SerializedName

data class WikiQueryResponse(
    @SerializedName("batchcomplete") val batchComplete: String?,
    @SerializedName("query") val query: WikiQuery?
)

data class WikiQuery(
    @SerializedName("pages") val pages: Map<String, WikiPageDetail>? // Key is page ID
)

data class WikiPageDetail(
    @SerializedName("pageid") val pageId: Int?,
    @SerializedName("ns") val ns: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("thumbnail") val thumbnail: WikiThumbnail?,
    @SerializedName("pageimage") val pageImage: String? // Filename of the page image
)

data class WikiThumbnail(
    @SerializedName("source") val source: String?,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?
)