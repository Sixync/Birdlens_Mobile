// app/src/main/java/com/android/birdlens/data/model/AIModels.kt
package com.android.birdlens.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

data class AIIdentifyResponse(
    @SerializedName("possibilities") val possibilities: List<String>?,
    @SerializedName("identified_bird") val identifiedBird: String?,
    @SerializedName("chat_response") val chatResponse: String?,
    @SerializedName("image_url") val imageUrl: String?
)

@Parcelize
data class ChatMessage(
    @SerializedName("role") val role: String, // "user" or "assistant"
    @SerializedName("text") val text: String,
    // The following fields are for UI state. When serialized for an API call,
    // the backend is expected to ignore them.
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString()
) : Parcelable


data class AIQuestionRequest(
    @SerializedName("bird_name") val birdName: String,
    @SerializedName("question") val question: String,
    @SerializedName("history") val history: List<ChatMessage>
)

data class AIQuestionResponse(
    @SerializedName("chat_response") val chatResponse: String
)