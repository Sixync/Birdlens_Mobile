// EXE201/app/src/main/java/com/android/birdlens/data/model/response/LoginResponse.kt
package com.android.birdlens.data.model.response

import com.google.gson.annotations.SerializedName

// Assuming the login response includes user details and tokens
data class LoginResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("age")
    val age: Int,
    @SerializedName("avatar_url")
    val avatarUrl: String?,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
) {
    // Optional: A helper function to extract UserResponse data if needed elsewhere
    fun toUserResponse(): UserResponse {
        return UserResponse(
            id = id,
            username = username,
            firstName = firstName,
            lastName = lastName,
            email = email,
            age = age,
            avatarUrl = avatarUrl,
            subscription = null
        )
    }
}