// EXE201/app/src/main/java/com/android/birdlens/data/model/response/LoginData.kt
package com.android.birdlens.data.model.response

import com.google.gson.annotations.SerializedName

data class LoginData(
    @SerializedName("session_id")
    val sessionId: Long?, // Or String, depending on actual type
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    @SerializedName("access_token_exp")
    val accessTokenExp: String?,
    @SerializedName("refresh_token_exp")
    val refreshTokenExp: String?,

    // IF user details are also in this "data" object, add them here:
    @SerializedName("id")
    val id: Long?,
    @SerializedName("username")
    val username: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("age")
    val age: Int?,
    @SerializedName("avatar_url")
    val avatarUrl: String?
)