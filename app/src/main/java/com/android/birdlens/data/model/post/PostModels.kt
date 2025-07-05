// EXE201/app/src/main/java/com/android/birdlens/data/model/post/PostModels.kt
package com.android.birdlens.data.model.post

import com.google.gson.annotations.SerializedName

// Corrected PostResponse to match the backend's JSON structure for GET /posts
data class PostResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("poster_avatar_url") val posterAvatarUrl: String?,
    @SerializedName("poster_name") val posterName: String,
    @SerializedName("created_at") val createdAt: String, // ISO 8601 timestamp
    @SerializedName("images_urls") val imagesUrls: List<String>?,
    @SerializedName("content") val content: String,
    @SerializedName("likes_count") var likesCount: Int, // var to allow local update
    @SerializedName("comments_count") val commentsCount: Int,
    @SerializedName("shares_count") val sharesCount: Int, // Assuming it's present as per Go struct & example
    @SerializedName("is_liked") var isLiked: Boolean, // var to allow local update, matches 'is_liked' from example
    @SerializedName("type") val type: String,
    @SerializedName("sighting_date") val sightingDate: String?,
    @SerializedName("tagged_species_code") val taggedSpeciesCode: String?
)

data class CommentResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("post_id") val postId: Long,
    @SerializedName("user_full_name") val userFullName: String,
    @SerializedName("user_avatar_url") val userAvatarUrl: String?,
    @SerializedName("content") val content: String,
    @SerializedName("created_at") val createdAt: String
)

data class CreateCommentRequest(
    @SerializedName("content") val content: String
)

data class CreatePostRequestFields(
    val content: String,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val privacyLevel: String,
    val type: String?,
    val isFeatured: Boolean,
    val sightingDate: String?, // New field
    val taggedSpeciesCode: String? // New field
)

data class PaginatedPostsResponse(
    @SerializedName("items") val items: List<PostResponse>?, // Made nullable
    @SerializedName("total_count") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class PaginatedCommentsResponse(
    @SerializedName("items") val items: List<CommentResponse>?, // Made nullable
    @SerializedName("total_count") val totalCount: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class ReactionResponseData(
    @SerializedName("likes_count") val likesCount: Int?,
    @SerializedName("is_liked") val isLiked: Boolean?
)