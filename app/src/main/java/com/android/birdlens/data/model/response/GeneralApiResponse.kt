// app/src/main/java/com/android/birdlens/data/model/response/GeneralApiResponse.kt
package com.android.birdlens.data.model.response

import com.google.gson.annotations.SerializedName

// A generic wrapper for API responses, useful for consistent handling.
// Your backend seems to directly return the data or an error structure.
// For simplicity, we'll assume direct UserResponse for successful registration for now.
// If your backend has a standard wrapper like { "data": ..., "message": ..., "error": ...},
// you should model that here.
// For now, this file can be kept minimal or used if you decide to standardize responses.

// Example of a more generic response (if your backend used one)

data class GenericApiResponse<T>(
    @SerializedName("error")
    val error: Boolean,
    @SerializedName("data")
    val data: T?, // The actual data payload will be of type T
    @SerializedName("message") // Optional: if your API sometimes includes a message field
    val message: String? = null
)
