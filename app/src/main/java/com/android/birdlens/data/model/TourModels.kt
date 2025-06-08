// EXE201/app/src/main/java/com/android/birdlens/data/model/TourModels.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

data class Event(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("cover_photo_url") val coverPhotoUrl: String?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)

data class Location(
    @SerializedName("id") val id: Long,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String
)

data class Tour(
    @SerializedName("id") val id: Long,
    @SerializedName("event_id") val eventId: Long,
    @SerializedName("event") val event: Event?,
    @SerializedName("price") val price: Double,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("duration") val duration: Int,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("location_id") val locationId: Long,
    @SerializedName("location") val location: Location?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("images_url") val imagesUrl: List<String>?
)

data class PaginatedToursResponse(
    @SerializedName("items") val items: List<Tour>?, // Made nullable
    @SerializedName("total_count") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class TourCreateRequest(
    @SerializedName("event_id") val eventId: Long,
    @SerializedName("tour_name") val tourName: String,
    @SerializedName("tour_description") val tourDescription: String,
    @SerializedName("price") val price: Double,
    @SerializedName("tour_capacity") val tourCapacity: Int,
    @SerializedName("duration_days") val durationDays: Int,
    @SerializedName("location_id") val locationId: Long
)