// EXE201/app/src/main/java/com/android/birdlens/data/model/request/PasswordResetRequest.kt
package com.android.birdlens.data.model.request

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val token: String,
    val new_password: String
)