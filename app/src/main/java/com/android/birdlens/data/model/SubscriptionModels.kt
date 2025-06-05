// EXE201/app/src/main/java/com/android/birdlens/data/model/SubscriptionModels.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

// Corresponds to Go's store.Subscription
data class Subscription(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: Double,
    @SerializedName("duration_days") val durationDays: Int,
    @SerializedName("created_at") val createdAt: String, // Assuming ISO 8601 String
    @SerializedName("updated_at") val updatedAt: String? // Assuming ISO 8601 String
)

// Corresponds to Go's CreateSubscriptionRequest
data class CreateSubscriptionRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("price") val price: Double,
    @SerializedName("duration_days") val durationDays: Int
)