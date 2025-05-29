// EXE201/app/src/main/java/com/android/birdlens/data/network/ApiService.kt
package com.android.birdlens.data.network

// import com.android.birdlens.data.model.request.GoogleIdTokenRequest // No longer needed
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.model.response.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
// Removed @Header("Authorization") as interceptor will handle it for relevant endpoints

interface ApiService {
    @POST("auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<GenericApiResponse<String>>

    @POST("auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<GenericApiResponse<String>>

    // Removed: signInWithGoogleToken as the backend /auth/google endpoint is not used.
    // @POST("auth/google")
    // suspend fun signInWithGoogleToken(@Body googleTokenRequest: GoogleIdTokenRequest): Response<GenericApiResponse<String>>

    @GET("users/me") // New endpoint to get current user
    suspend fun getCurrentUser(): Response<GenericApiResponse<UserResponse>>
}