// app/src/main/java/com/android/birdlens/data/model/request/RegisterRequest.kt
package com.android.birdlens.data.model.request

import com.google.gson.annotations.SerializedName

// Logic: The RegisterRequest class is simplified to only include email and password.
// The other fields (username, firstName, lastName, age) are no longer sent from the client
// as they are now auto-generated on the backend.
data class RegisterRequest(
    val email: String,
    val password: String
)