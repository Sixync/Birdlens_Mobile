// EXE201/app/src/main/java/com/android/birdlens/data/model/Notification.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

data class Notification(
    @SerializedName("id") val id: Long,
    @SerializedName("user_id") val userId: Long,
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String, // e.g., "referral_success", "new_follower"
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String // ISO 8601 timestamp
)

data class PaginatedNotificationsResponse(
    @SerializedName("items") val items: List<Notification>?,
    @SerializedName("total_count") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)