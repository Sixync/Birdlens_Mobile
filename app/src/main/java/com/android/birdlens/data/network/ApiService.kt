// EXE201/app/src/main/java/com/android/birdlens/data/network/ApiService.kt
package com.android.birdlens.data.network

import com.android.birdlens.data.model.CreateSubscriptionRequest
import com.android.birdlens.data.model.Event
import com.android.birdlens.data.model.PaginatedEventData
import com.android.birdlens.data.model.PaginatedToursResponse
import com.android.birdlens.data.model.Subscription
import com.android.birdlens.data.model.Tour
import com.android.birdlens.data.model.TourCreateRequest
import com.android.birdlens.data.model.post.CommentResponse
import com.android.birdlens.data.model.post.CreateCommentRequest
import com.android.birdlens.data.model.post.PaginatedCommentsResponse
import com.android.birdlens.data.model.post.PaginatedPostsResponse
import com.android.birdlens.data.model.post.PostResponse
import com.android.birdlens.data.model.post.ReactionResponseData
import com.android.birdlens.data.model.request.ForgotPasswordRequest
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.model.request.ResetPasswordRequest
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.model.response.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH // Import PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<GenericApiResponse<String>>

    @POST("auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<GenericApiResponse<String>>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<GenericApiResponse<UserResponse>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body forgotPasswordRequest: ForgotPasswordRequest): Response<GenericApiResponse<Unit?>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body resetPasswordRequest: ResetPasswordRequest): Response<GenericApiResponse<Unit?>>


    // Tour Endpoints
    @GET("tours")
    suspend fun getTours(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedToursResponse>>

    @GET("tours/{tour_id}")
    suspend fun getTourById(
        @Path("tour_id") tourId: Long
    ): Response<GenericApiResponse<Tour>>

    @POST("tours")
    suspend fun createTour(
        @Body tourCreateRequest: TourCreateRequest
    ): Response<GenericApiResponse<Tour>>

    @Multipart
    @PUT("tours/{tour_id}/images")
    suspend fun addTourImages(
        @Path("tour_id") tourId: Long,
        @Part images: List<MultipartBody.Part>
    ): Response<GenericApiResponse<List<String>>>

    @Multipart
    @PUT("tours/{tour_id}/thumbnail")
    suspend fun addTourThumbnail(
        @Path("tour_id") tourId: Long,
        @Part thumbnail: MultipartBody.Part
    ): Response<GenericApiResponse<String>>

    // Event Endpoints
    @GET("events")
    suspend fun getEvents(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedEventData>>

    @GET("events/{event_id}")
    suspend fun getEventById(
        @Path("event_id") eventId: Long
    ): Response<GenericApiResponse<Event>>

    @GET("subscriptions")
    suspend fun getSubscriptions(): Response<GenericApiResponse<List<Subscription>>>

    @POST("subscriptions")
    suspend fun createSubscription(
        @Body createSubscriptionRequest: CreateSubscriptionRequest
    ): Response<GenericApiResponse<Subscription>>
    // Post Endpoints
    @GET("posts")
    suspend fun getPosts(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedPostsResponse>>

    @Multipart
    @POST("posts")
    suspend fun createPost(
        @Part("content") content: RequestBody,
        @Part("location_name") locationName: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part("privacy_level") privacyLevel: RequestBody,
        @Part("type") type: RequestBody?,
        @Part("is_featured") isFeatured: RequestBody,
        @Part mediaFiles: List<MultipartBody.Part>
    ): Response<GenericApiResponse<PostResponse>>

    @GET("posts/{post_id}/comments")
    suspend fun getComments(
        @Path("post_id") postId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<GenericApiResponse<PaginatedCommentsResponse>>

    @POST("posts/{post_id}/comments")
    suspend fun createComment(
        @Path("post_id") postId: String,
        @Body commentRequest: CreateCommentRequest
    ): Response<GenericApiResponse<CommentResponse>>

    @POST("posts/{post_id}/reactions")
    suspend fun addReaction(
        @Path("post_id") postId: String,
        @Query("reaction_type") reactionType: String
    ): Response<GenericApiResponse<ReactionResponseData?>>
}