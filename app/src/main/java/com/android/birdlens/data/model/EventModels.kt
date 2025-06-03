// EXE201/app/src/main/java/com/android/birdlens/data/model/EventModels.kt
package com.android.birdlens.data.model // Or a new sub-package like com.android.birdlens.data.model.event

import com.google.gson.annotations.SerializedName

// Re-using com.android.birdlens.data.model.Event from TourModels.kt
// If you prefer to have it standalone, you can copy its definition here or move it.
// For now, we'll assume it's accessible via its existing path.

/**
 * Represents the "data" field in the paginated events API response.
 * This is the actual payload containing the list of events and pagination details.
 */
data class PaginatedEventData(
    @SerializedName("items") val items: List<Event>, // Using the existing Event model
    @SerializedName("total_count") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

// The full response will be GenericApiResponse<PaginatedEventData>
// No need for a PaginatedEventsResponse class if GenericApiResponse is used directly with PaginatedEventData