// EXE201/app/src/main/java/com/android/birdlens/data/network/ApiService.kt
package com.android.birdlens.data.network

// import com.android.birdlens.data.model.request.GoogleIdTokenRequest // No longer needed
import com.android.birdlens.data.model.Event
import com.android.birdlens.data.model.PaginatedEventData // New import
import com.android.birdlens.data.model.PaginatedToursResponse
import com.android.birdlens.data.model.Tour
import com.android.birdlens.data.model.TourCreateRequest
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.model.response.UserResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
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

    // Tour Endpoints
    @GET("tours")
    suspend fun getTours(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedToursResponse>> // Backend wraps paginated list in its JsonResponse

    @GET("tours/{tour_id}")
    suspend fun getTourById(
        @Path("tour_id") tourId: Long
    ): Response<GenericApiResponse<Tour>> // Backend wraps single tour in its JsonResponse

    @POST("tours") // Assuming this will require auth
    suspend fun createTour(
        @Body tourCreateRequest: TourCreateRequest
    ): Response<GenericApiResponse<Tour>> // Backend returns created tour, wrapped

    @Multipart
    @PUT("tours/{tour_id}/images") // Assuming this will require auth
    suspend fun addTourImages(
        @Path("tour_id") tourId: Long,
        @Part images: List<MultipartBody.Part> // Use List for multiple files with the same part name key
    ): Response<GenericApiResponse<List<String>>> // Backend returns list of URLs, wrapped

    @Multipart
    @PUT("tours/{tour_id}/thumbnail") // Assuming this will require auth
    suspend fun addTourThumbnail(
        @Path("tour_id") tourId: Long,
        @Part thumbnail: MultipartBody.Part // Single file
    ): Response<GenericApiResponse<String>> // Backend returns single URL, wrapped

    // Event Endpoints
    @GET("events") // New endpoint for fetching events
    suspend fun getEvents(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedEventData>> // Using the new PaginatedEventData

    @GET("events/{event_id}") // Endpoint to get a single event
    suspend fun getEventById(
        @Path("event_id") eventId: Long
    ): Response<GenericApiResponse<Event>> // Ensure Event model matches response
}