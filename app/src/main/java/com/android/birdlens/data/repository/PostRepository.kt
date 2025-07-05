// EXE201/app/src/main/java/com/android/birdlens/data/repository/PostRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import com.android.birdlens.data.model.post.CommentResponse
import com.android.birdlens.data.model.post.CreateCommentRequest
import com.android.birdlens.data.model.post.PaginatedCommentsResponse
import com.android.birdlens.data.model.post.PaginatedPostsResponse
import com.android.birdlens.data.model.post.PostResponse
import com.android.birdlens.data.model.post.ReactionResponseData // Changed
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File

class PostRepository(applicationContext: Context) {

    private val apiService: ApiService = RetrofitInstance.api(applicationContext)

    suspend fun getPosts(limit: Int, offset: Int, type: String?): Response<GenericApiResponse<PaginatedPostsResponse>> {
        return apiService.getPosts(limit, offset, type)
    }

    suspend fun createPost(
        content: String,
        locationName: String?,
        latitude: Double?,
        longitude: Double?,
        privacyLevel: String,
        type: String?,
        isFeatured: Boolean,
        mediaFiles: List<File>,
        sightingDate: String?,
        taggedSpeciesCode: String?
    ): Response<GenericApiResponse<PostResponse>> {
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val locationNameBody = locationName?.toRequestBody("text/plain".toMediaTypeOrNull())
        val latitudeBody = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val longitudeBody = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val privacyLevelBody = privacyLevel.toRequestBody("text/plain".toMediaTypeOrNull())
        val typeBody = type?.toRequestBody("text/plain".toMediaTypeOrNull())
        val isFeaturedBody = isFeatured.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val sightingDateBody = sightingDate?.toRequestBody("text/plain".toMediaTypeOrNull())
        val taggedSpeciesCodeBody = taggedSpeciesCode?.toRequestBody("text/plain".toMediaTypeOrNull())


        val mediaParts = mediaFiles.mapNotNull { file ->
            if (file.exists()) {
                // Logic: Specify the exact MIME type 'image/jpeg' since we know how the file was created.
                // This prevents the backend from defaulting to 'application/octet-stream'.
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("media_files", file.name, requestFile)
            } else {
                null
            }
        }

        return apiService.createPost(
            content = contentBody,
            locationName = locationNameBody,
            latitude = latitudeBody,
            longitude = longitudeBody,
            privacyLevel = privacyLevelBody,
            type = typeBody,
            isFeatured = isFeaturedBody,
            mediaFiles = mediaParts,
            sightingDate = sightingDateBody,
            taggedSpeciesCode = taggedSpeciesCodeBody
        )
    }

    suspend fun getComments(postId: String, limit: Int, offset: Int): Response<GenericApiResponse<PaginatedCommentsResponse>> {
        return apiService.getComments(postId, limit, offset)
    }

    suspend fun createComment(postId: String, content: String): Response<GenericApiResponse<CommentResponse>> {
        val request = CreateCommentRequest(content)
        return apiService.createComment(postId, request)
    }

    suspend fun addReaction(postId: String, reactionType: String): Response<GenericApiResponse<ReactionResponseData?>> {
        return apiService.addReaction(postId, reactionType)
    }
}