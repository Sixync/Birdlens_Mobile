// EXE201/app/src/main/java/com/android/birdlens/data/model/request/LoginRequest.kt
package com.android.birdlens.data.model.request

// Changed 'username' to 'email' to match your login request JSON example
data class LoginRequest(
    val email: String,
    val password: String
)