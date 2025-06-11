// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/CommunityViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.post.CommentResponse
import com.android.birdlens.data.model.post.PaginatedCommentsResponse
import com.android.birdlens.data.model.post.PaginatedPostsResponse
import com.android.birdlens.data.model.post.PostResponse
import com.android.birdlens.data.repository.PostRepository
import com.android.birdlens.utils.FileUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed class PostFeedUiState {
    data object Idle : PostFeedUiState()
    data object Loading : PostFeedUiState()
    data class Success(val posts: List<PostResponse>, val canLoadMore: Boolean, val isLoadingMore: Boolean = false) : PostFeedUiState()
    data class Error(val message: String) : PostFeedUiState()
}

// Generic UI state for single operations like create, update, delete
sealed class GenericUiState<out T> {
    data object Idle : GenericUiState<Nothing>()
    data object Loading : GenericUiState<Nothing>()
    data class Success<T>(val data: T) : GenericUiState<T>()
    data class Error(val message: String) : GenericUiState<Nothing>()
}


class CommunityViewModel(application: Application) : AndroidViewModel(application) {

    private val postRepository = PostRepository(application.applicationContext)
    private val fileUtil = FileUtil(application.applicationContext)

    private val _postFeedState = MutableStateFlow<PostFeedUiState>(PostFeedUiState.Idle)
    val postFeedState: StateFlow<PostFeedUiState> = _postFeedState.asStateFlow()

    private val _createPostState = MutableStateFlow<GenericUiState<PostResponse>>(GenericUiState.Idle)
    val createPostState: StateFlow<GenericUiState<PostResponse>> = _createPostState.asStateFlow()

    private val _commentsState = MutableStateFlow<GenericUiState<PaginatedCommentsResponse>>(GenericUiState.Idle)
    val commentsState: StateFlow<GenericUiState<PaginatedCommentsResponse>> = _commentsState.asStateFlow()


    private var currentPostPage = 0
    private val postPageSize = 10
    private var isPostLoadingPosts = false
    private var allPostsLoaded = false

    private var currentCommentPage = 0
    private val commentPageSize = 10
    private var isCommentLoading = false
    private var allCommentsForPostLoaded = false
    private var currentPostIdForComments: String? = null


    init {
        fetchPosts(initialLoad = true)
    }

    fun fetchPosts(initialLoad: Boolean = false) {
        if (isPostLoadingPosts || (!initialLoad && allPostsLoaded)) return

        viewModelScope.launch {
            isPostLoadingPosts = true
            val offsetToFetch = if (initialLoad) 0 else currentPostPage * postPageSize

            if (initialLoad) {
                currentPostPage = 0
                allPostsLoaded = false
                _postFeedState.value = PostFeedUiState.Loading
            } else {
                _postFeedState.update { currentState ->
                    if (currentState is PostFeedUiState.Success) {
                        currentState.copy(isLoadingMore = true)
                    } else {
                        PostFeedUiState.Loading // Fallback if trying to paginate from non-success state
                    }
                }
            }

            try {
                val response = postRepository.getPosts(limit = postPageSize, offset = offsetToFetch)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        val paginatedData = genericResponse.data
                        val newPosts = paginatedData.items ?: emptyList()

                        _postFeedState.update { currentState ->
                            val existingPosts = if (initialLoad || currentState !is PostFeedUiState.Success) emptyList() else currentState.posts
                            PostFeedUiState.Success(
                                posts = existingPosts + newPosts,
                                canLoadMore = newPosts.size >= postPageSize && paginatedData.totalPages > (currentPostPage +1),
                                isLoadingMore = false
                            )
                        }
                        if (newPosts.isNotEmpty()) {
                            currentPostPage++ // Increment page only if new items were fetched
                        }
                        allPostsLoaded = newPosts.size < postPageSize || paginatedData.totalPages <= currentPostPage
                    } else {
                        _postFeedState.value = PostFeedUiState.Error(genericResponse.message ?: "Failed to load posts")
                    }
                } else {
                    _postFeedState.value = PostFeedUiState.Error("Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _postFeedState.value = PostFeedUiState.Error(e.localizedMessage ?: "Network error")
            } finally {
                isPostLoadingPosts = false
                _postFeedState.update { currentState -> // Ensure isLoadingMore is reset
                    if (currentState is PostFeedUiState.Success) {
                        currentState.copy(isLoadingMore = false)
                    } else currentState
                }
            }
        }
    }

    fun createPost(
        content: String,
        locationName: String?,
        latitude: Double?,
        longitude: Double?,
        privacyLevel: String,
        type: String?,
        isFeatured: Boolean,
        mediaUris: List<Uri>
    ) {
        viewModelScope.launch {
            _createPostState.value = GenericUiState.Loading
            try {
                // Convert Uris to Files using FileUtil
                val files: List<File> = mediaUris.mapNotNull { uri ->
                    fileUtil.getFileFromUri(uri) // getFileFromUri should handle potential nulls gracefully
                }
                Log.d("CommunityVM", "Preparing to upload ${files.size} files for post.")

                val response = postRepository.createPost(
                    content, locationName, latitude, longitude, privacyLevel, type, isFeatured, files
                )

                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _createPostState.value = GenericUiState.Success(genericResponse.data)
                        fetchPosts(initialLoad = true) // Refresh post list after successful creation
                    } else {
                        _createPostState.value = GenericUiState.Error(genericResponse.message ?: "Failed to create post")
                        Log.e("CommunityVM", "API error creating post: ${genericResponse.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error during post creation"
                    _createPostState.value = GenericUiState.Error("Error: ${response.code()} - $errorBody")
                    Log.e("CommunityVM", "HTTP error creating post: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _createPostState.value = GenericUiState.Error(e.localizedMessage ?: "An unexpected error occurred while creating the post")
                Log.e("CommunityVM", "Exception creating post", e)
            }
        }
    }

    fun resetCreatePostState() {
        _createPostState.value = GenericUiState.Idle
    }


    fun addReactionToPost(postId: String, reactionType: String) {
        viewModelScope.launch {
            val originalState = _postFeedState.value
            var postToUpdate: PostResponse? = null
            var postIndex = -1

            if (originalState is PostFeedUiState.Success) {
                val postIdAsLong = postId.toLongOrNull()
                postIndex = if (postIdAsLong != null) {
                    originalState.posts.indexOfFirst { it.id == postIdAsLong }
                } else {
                    Log.e("CommunityVM", "Invalid postId format for reaction: $postId, trying string comparison.")
                    originalState.posts.indexOfFirst { it.id.toString() == postId }
                }


                if (postIndex != -1) {
                    postToUpdate = originalState.posts[postIndex]
                    // Optimistic update
                    val newLikedStatus = !postToUpdate.isLiked
                    val newLikesCount = if (newLikedStatus) postToUpdate.likesCount + 1 else (postToUpdate.likesCount - 1).coerceAtLeast(0)

                    val updatedPost = postToUpdate.copy(isLiked = newLikedStatus, likesCount = newLikesCount)
                    val updatedPostsList = originalState.posts.toMutableList()
                    updatedPostsList[postIndex] = updatedPost
                    _postFeedState.value = originalState.copy(posts = updatedPostsList)
                }
            }

            try {
                val response = postRepository.addReaction(postId, reactionType)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error) {
                        // Backend might return updated like count and isLiked status in genericResponse.data
                        // If so, update the specific post again with confirmed data
                        // For now, optimistic update is considered final on success
                        Log.d("CommunityVM", "Reaction success: ${genericResponse.message}")
                    } else {
                        Log.e("CommunityVM", "Failed to add reaction (API error): ${genericResponse.message}")
                        // Revert optimistic update if API call fails
                        if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                            _postFeedState.value = originalState
                        }
                    }
                } else {
                    Log.e("CommunityVM", "Error adding reaction (HTTP error): ${response.code()} - ${response.errorBody()?.string()}")
                    if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                        _postFeedState.value = originalState // Revert optimistic update
                    }
                }
            } catch (e: Exception) {
                Log.e("CommunityVM", "Exception adding reaction: ${e.localizedMessage}", e)
                if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                    _postFeedState.value = originalState // Revert optimistic update
                }
            }
        }
    }

    fun createCommentForPost(postId: String, content: String) {
        viewModelScope.launch {
            // Optimistically update the comment count on the main post card
            _postFeedState.update { currentPostState ->
                if (currentPostState is PostFeedUiState.Success) {
                    val postIdAsLong = postId.toLongOrNull()
                    currentPostState.copy(posts = currentPostState.posts.map { post ->
                        if ((postIdAsLong != null && post.id == postIdAsLong) || post.id.toString() == postId) {
                            post.copy(commentsCount = post.commentsCount + 1)
                        } else post
                    })
                } else currentPostState
            }

            try {
                val response = postRepository.createComment(postId, content)
                if (response.isSuccessful && response.body()?.error == false && response.body()?.data != null) {
                    val newComment = response.body()!!.data!!
                    Log.d("CommunityVM", "Comment created successfully: $newComment")

                    // Update the comments list in the bottom sheet
                    _commentsState.update { currentCommentUiState ->
                        when (currentCommentUiState) {
                            is GenericUiState.Success -> {
                                val existingItems = currentCommentUiState.data.items ?: emptyList()
                                // Prepend new comment to show it at the top
                                val updatedItems = listOf(newComment) + existingItems

                                currentCommentUiState.copy(
                                    data = currentCommentUiState.data.copy(
                                        items = updatedItems,
                                        totalCount = currentCommentUiState.data.totalCount + 1
                                    )
                                )
                            }
                            else -> {
                                // If this is the first comment, initialize the success state
                                GenericUiState.Success(
                                    PaginatedCommentsResponse(
                                        items = listOf(newComment),
                                        totalCount = 1,
                                        page = 0, // Assuming first page
                                        pageSize = commentPageSize,
                                        totalPages = 1
                                    )
                                )
                            }
                        }
                    }
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Failed to create comment"
                    Log.e("CommunityVM", "Failed to create comment: $errorMsg")
                    // Revert optimistic update for comment count if API call fails
                    _postFeedState.update { currentPostState ->
                        if (currentPostState is PostFeedUiState.Success) {
                            val postIdAsLong = postId.toLongOrNull()
                            currentPostState.copy(posts = currentPostState.posts.map { post ->
                                if ((postIdAsLong != null && post.id == postIdAsLong) || post.id.toString() == postId) {
                                    post.copy(commentsCount = (post.commentsCount - 1).coerceAtLeast(0))
                                } else post
                            })
                        } else currentPostState
                    }
                }
            } catch (e: Exception) {
                Log.e("CommunityVM", "Exception creating comment: ${e.localizedMessage}")
                // Revert optimistic update for comment count on exception
                _postFeedState.update { currentPostState ->
                    if (currentPostState is PostFeedUiState.Success) {
                        val postIdAsLong = postId.toLongOrNull()
                        currentPostState.copy(posts = currentPostState.posts.map { post ->
                            if ((postIdAsLong != null && post.id == postIdAsLong) || post.id.toString() == postId) {
                                post.copy(commentsCount = (post.commentsCount - 1).coerceAtLeast(0))
                            } else post
                        })
                    } else currentPostState
                }
            }
        }
    }

    fun fetchCommentsForPost(postId: String, initialLoad: Boolean = false) {
        // If fetching for a new post or it's an initial load for the current post, reset pagination
        if (currentPostIdForComments != postId || initialLoad) {
            resetCommentsStateInternal() // Resets page, loading flags, etc.
        }
        currentPostIdForComments = postId // Set current post ID for subsequent pagination


        if (isCommentLoading && !initialLoad) return // Prevent multiple simultaneous pagination calls
        if (!initialLoad && allCommentsForPostLoaded) {
            Log.d("CommunityVM", "All comments already loaded for post $postId.")
            return
        }

        viewModelScope.launch {
            isCommentLoading = true
            val offsetToFetch = if (initialLoad) 0 else currentCommentPage * commentPageSize

            if (initialLoad) {
                _commentsState.value = GenericUiState.Loading // Show loading for initial fetch
            }
            // For pagination, we don't set to Loading to keep existing comments visible

            try {
                val response = postRepository.getComments(postId, limit = commentPageSize, offset = offsetToFetch)
                if (response.isSuccessful && response.body()?.error == false && response.body()?.data != null) {
                    val paginatedData = response.body()!!.data!!
                    val newComments = paginatedData.items ?: emptyList()

                    _commentsState.update { currentState ->
                        val existingComments = if (initialLoad || currentState !is GenericUiState.Success) {
                            emptyList()
                        } else {
                            currentState.data.items ?: emptyList()
                        }
                        // Comments are usually sorted newest first by backend; append for pagination
                        val updatedItems = existingComments + newComments

                        GenericUiState.Success(
                            paginatedData.copy( // Use API's pagination info
                                items = updatedItems,
                                totalCount = paginatedData.totalCount,
                                page = currentCommentPage, // This reflects the "page" of data we just fetched
                                totalPages = paginatedData.totalPages
                            )
                        )
                    }
                    if (newComments.isNotEmpty()){
                        currentCommentPage++ // Increment page for next fetch
                    }
                    // Update allCommentsLoaded based on the response
                    allCommentsForPostLoaded = newComments.size < commentPageSize || paginatedData.totalPages <= currentCommentPage
                } else {
                    val errorMsg = response.body()?.message ?: "Failed to load comments for post $postId"
                    // Only set to error if it's an initial load or if the current state isn't already Success (to avoid wiping data on pagination error)
                    if (initialLoad || _commentsState.value !is GenericUiState.Success) {
                        _commentsState.value = GenericUiState.Error(errorMsg)
                    } else {
                        Log.e("CommunityVM", "Error fetching comments (page $currentCommentPage): $errorMsg")
                        // Optionally, emit a transient error state or log, but keep existing data for pagination errors
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Network error fetching comments for post $postId"
                if (initialLoad || _commentsState.value !is GenericUiState.Success) {
                    _commentsState.value = GenericUiState.Error(errorMsg)
                } else {
                    Log.e("CommunityVM", "Exception fetching comments (page $currentCommentPage): $errorMsg", e)
                    // Keep existing data on pagination error
                }
            } finally {
                isCommentLoading = false
            }
        }
    }

    // Renamed to avoid confusion with public reset that also nullifies postId
    private fun resetCommentsStateInternal() {
        _commentsState.value = GenericUiState.Idle
        currentCommentPage = 0
        isCommentLoading = false
        allCommentsForPostLoaded = false
        // currentPostIdForComments is NOT reset here, it's managed by the caller (fetchCommentsForPost)
    }

    // Public function to reset comments state, e.g., when bottom sheet is dismissed
    fun resetCommentsState() {
        resetCommentsStateInternal()
        currentPostIdForComments = null // Explicitly nullify when dismissing sheet
    }
}