// EXE201/app/src/main/java/com/android/birdlens/data/network/ApiService.kt
package com.android.birdlens.data.network

// import com.android.birdlens.data.model.request.GoogleIdTokenRequest // No longer needed
import com.android.birdlens.data.model.CreateSubscriptionRequest
import com.android.birdlens.data.model.Event
import com.android.birdlens.data.model.PaginatedEventData // New import
import com.android.birdlens.data.model.PaginatedToursResponse
import com.android.birdlens.data.model.Subscription
import com.android.birdlens.data.model.Tour
import com.android.birdlens.data.model.TourCreateRequest
import com.android.birdlens.data.model.post.CommentResponse
import com.android.birdlens.data.model.post.CreateCommentRequest
import com.android.birdlens.data.model.post.PaginatedCommentsResponse
import com.android.birdlens.data.model.post.PaginatedPostsResponse
import com.android.birdlens.data.model.post.PostResponse
import com.android.birdlens.data.model.post.ReactionResponseData // Changed
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.model.response.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    @GET("subscriptions") // Requires auth (handled by AuthInterceptor)
    suspend fun getSubscriptions(): Response<GenericApiResponse<List<Subscription>>>

    @POST("subscriptions") // Requires auth (handled by AuthInterceptor)
    suspend fun createSubscription(
        @Body createSubscriptionRequest: CreateSubscriptionRequest
    ): Response<GenericApiResponse<Subscription>> // Backend returns the created subscription
    // Post Endpoints
    @GET("posts")
    suspend fun getPosts(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedPostsResponse>> // Corrected

    @Multipart
    @POST("posts")
    suspend fun createPost(
        @Part("content") content: RequestBody,
        @Part("location_name") locationName: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?, // Corrected from logitude
        @Part("privacy_level") privacyLevel: RequestBody,
        @Part("type") type: RequestBody?,
        @Part("is_featured") isFeatured: RequestBody,
        @Part mediaFiles: List<MultipartBody.Part>
    ): Response<GenericApiResponse<PostResponse>> // Expecting the created PostResponse structure

    @GET("posts/{post_id}/comments")
    suspend fun getComments(
        @Path("post_id") postId: String, // Assuming post_id is String, adjust if Long
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedCommentsResponse>> // Corrected

    @POST("posts/{post_id}/comments")
    suspend fun createComment(
        @Path("post_id") postId: String, // Assuming post_id is String
        @Body commentRequest: CreateCommentRequest
    ): Response<GenericApiResponse<CommentResponse>> // Corrected

    @POST("posts/{post_id}/reactions")
    suspend fun addReaction(
        @Path("post_id") postId: String, // Assuming post_id is String
        @Query("reaction_type") reactionType: String
    ): Response<GenericApiResponse<ReactionResponseData?>> // Backend returns nil in data, message in outer layer
}