// EXE201/app/src/main/java/com/android/birdlens/data/model/response/UserResponse.kt
package com.android.birdlens.data.model.response

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: Long?, // Assuming id might not always be in every UserResponse context
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
    @SerializedName("subscription") // This field is key
    val subscription: String?    // Nullable, as it's not present if user hasn't subscribed
)