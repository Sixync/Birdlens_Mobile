// EXE201/app/src/main/java/com/android/birdlens/data/network/ApiService.kt
package com.android.birdlens.data.network

import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.model.response.GenericApiResponse // Import new wrapper
import com.android.birdlens.data.model.response.LoginData // Import new inner data class
import com.android.birdlens.data.model.response.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<UserResponse> // Assuming register is not wrapped or has a different wrapper

    @POST("auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<GenericApiResponse<LoginData>> // Updated
}