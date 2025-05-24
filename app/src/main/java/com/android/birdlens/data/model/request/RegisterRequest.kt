// app/src/main/java/com/android/birdlens/data/model/request/RegisterRequest.kt
package com.android.birdlens.data.model.request

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    @SerializedName("first_name") // Matches the backend Go struct field tag
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val age: Int,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null // Optional
)