// EXE201/app/src/main/java/com/android/birdlens/data/model/EventModels.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

data class PaginatedEventData(
    @SerializedName("items") val items: List<Event>?, // Made nullable
    @SerializedName("total_count") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)