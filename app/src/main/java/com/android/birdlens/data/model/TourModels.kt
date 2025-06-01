// EXE201/app/src/main/java/com/android/birdlens/data/model/TourModels.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

// Corresponds to Go's store.Event
data class Event(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("cover_photo_url") val coverPhotoUrl: String?,
    @SerializedName("start_date") val startDate: String, // Assuming ISO 8601 String from backend
    @SerializedName("end_date") val endDate: String,   // Assuming ISO 8601 String
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String?
)

// Corresponds to Go's store.Location
data class Location(
    @SerializedName("id") val id: Long,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String
)

// Corresponds to Go's store.Tour
data class Tour(
    @SerializedName("id") val id: Long,
    @SerializedName("event_id") val eventId: Long,
    @SerializedName("event") val event: Event?, // Nested object
    @SerializedName("price") val price: Double,
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?,
    @SerializedName("duration") val duration: Int, // Duration in days
    @SerializedName("start_date") val startDate: String, // Assuming ISO 8601 String
    @SerializedName("end_date") val endDate: String,     // Assuming ISO 8601 String
    @SerializedName("location_id") val locationId: Long,
    @SerializedName("location") val location: Location?, // Nested object
    @SerializedName("created_at") val createdAt: String, // Assuming ISO 8601 String
    @SerializedName("updated_at") val updatedAt: String?, // Assuming ISO 8601 String
    @SerializedName("images_url") val imagesUrl: List<String>?
)

// For GET /tours response which is paginated
// Corresponds to Go's store.PaginatedList[store.Tour]
data class PaginatedToursResponse(
    @SerializedName("items") val items: List<Tour>,
    @SerializedName("total_count") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

// For POST /tours request
// Corresponds to Go's TourCreateRequest
data class TourCreateRequest(
    @SerializedName("event_id") val eventId: Long,
    @SerializedName("tour_name") val tourName: String,
    @SerializedName("tour_description") val tourDescription: String,
    @SerializedName("price") val price: Double,
    @SerializedName("tour_capacity") val tourCapacity: Int,
    @SerializedName("duration_days") val durationDays: Int,
    @SerializedName("location_id") val locationId: Long
    // Thumbnail is handled by a separate endpoint
)

// For the response of POST /tours, which is a single Tour object
// This will be wrapped in GenericApiResponse<Tour>

// For the response of PUT /tours/{tour_id}/images
// This will be wrapped in GenericApiResponse<List<String>>

// For the response of PUT /tours/{tour_id}/thumbnail
// This will be wrapped in GenericApiResponse<String>