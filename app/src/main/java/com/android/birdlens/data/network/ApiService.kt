// EXE201/app/src/main/java/com/android/birdlens/data/network/ApiService.kt
package com.android.birdlens.data.network

import com.android.birdlens.data.model.request.GoogleIdTokenRequest // New import
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.model.response.GenericApiResponse
// No longer need LoginData if data field is just the custom token string for login/register
// No longer need UserResponse directly from register if custom token is the primary output
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<GenericApiResponse<String>> // Assuming data is custom token string

    @POST("auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<GenericApiResponse<String>> // Assuming data is custom token string

    @POST("auth/google") // New endpoint for Google Sign-In via your backend
    suspend fun signInWithGoogleToken(@Body googleTokenRequest: GoogleIdTokenRequest): Response<GenericApiResponse<String>> // Assuming data is custom token string
}