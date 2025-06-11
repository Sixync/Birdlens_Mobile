// EXE201/app/src/main/java/com/android/birdlens/data/model/response/UserResponse.kt
package com.android.birdlens.data.model.response

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: Long?,
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
    @SerializedName("subscription")
    val subscription: String?,
    @SerializedName("email_verified") // Added
    val emailVerified: Boolean
)