package com.android.birdlens.data.model.request

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("referral_code")
    val referralCode: String? = null
)