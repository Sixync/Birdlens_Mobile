// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/CommunityViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.post.CommentResponse
import com.android.birdlens.data.model.post.PaginatedCommentsResponse
import com.android.birdlens.data.model.post.PaginatedPostsResponse
import com.android.birdlens.data.model.post.PostResponse
import com.android.birdlens.data.repository.PostRepository
import com.android.birdlens.utils.ErrorUtils
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

    // Holds the currently selected feed type, defaulting to "trending".
    private val _feedType = MutableStateFlow("trending")
    val feedType: StateFlow<String> = _feedType.asStateFlow()

    private val _createPostState = MutableStateFlow<GenericUiState<PostResponse>>(GenericUiState.Idle)
    val createPostState: StateFlow<GenericUiState<PostResponse>> = _createPostState.asStateFlow()

    private val _commentsState = MutableStateFlow<GenericUiState<PaginatedCommentsResponse>>(GenericUiState.Idle)
    val commentsState: StateFlow<GenericUiState<PaginatedCommentsResponse>> = _commentsState.asStateFlow()

    companion object {
        private const val TAG = "CommunityVM"
    }

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
        // The automatic fetch in the init block is removed to prevent race conditions.
        // The UI will now be responsible for explicitly calling fetchPosts when it's ready.
    }

    fun onEnterScreen() {
        // This function is called by the UI. It checks if data is missing or if there was a previous error,
        // and if so, it triggers a fresh data load. This prevents re-fetching on simple recompositions.
        if (_postFeedState.value is PostFeedUiState.Idle || _postFeedState.value is PostFeedUiState.Error) {
            Log.d(TAG, "Community screen entered. State is Idle or Error, initiating fetch.")
            fetchPosts(initialLoad = true)
        }
    }

    /**
     * Sets the desired feed type and triggers a fresh data load.
     * @param type The type of feed to load ("trending", "all", etc.).
     */
    fun setFeedType(type: String) {
        if (_feedType.value == type) return // No change needed
        _feedType.value = type
        // Trigger a full refresh for the new feed type
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
                        PostFeedUiState.Loading
                    }
                }
            }

            try {
                // The current feed type is now passed to the repository.
                val response = postRepository.getPosts(limit = postPageSize, offset = offsetToFetch, type = _feedType.value)
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
                            currentPostPage++
                        }
                        allPostsLoaded = newPosts.size < postPageSize || paginatedData.totalPages <= currentPostPage
                    } else {
                        _postFeedState.value = PostFeedUiState.Error(genericResponse.message ?: "Failed to load posts")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _postFeedState.value = PostFeedUiState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching posts: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _postFeedState.value = PostFeedUiState.Error(e.localizedMessage ?: "Network error")
                Log.e(TAG, "Exception fetching posts", e)
            } finally {
                isPostLoadingPosts = false
                _postFeedState.update { currentState ->
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
        mediaUris: List<Uri>,
        sightingDate: String?,
        taggedSpeciesCode: String?
    ) {
        viewModelScope.launch {
            _createPostState.value = GenericUiState.Loading
            try {
                // Logic: Instead of directly using the file from the Uri, this now calls the
                // new conversion utility. Each image selected by the user is converted to a
                // standard JPEG format before being added to the upload list.
                val files: List<File> = mediaUris.mapNotNull { uri ->
                    // Convert every selected image to a JPEG file before uploading.
                    fileUtil.getConvertedImageFileFromUri(uri, Bitmap.CompressFormat.JPEG, 90)
                }
                Log.d(TAG, "Preparing to upload ${files.size} converted files for post.")

                val response = postRepository.createPost(
                    content, locationName, latitude, longitude, privacyLevel, type, isFeatured, files, sightingDate, taggedSpeciesCode
                )

                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _createPostState.value = GenericUiState.Success(genericResponse.data)
                        fetchPosts(initialLoad = true)
                    } else {
                        _createPostState.value = GenericUiState.Error(genericResponse.message ?: "Failed to create post")
                        Log.e(TAG, "API error creating post: ${genericResponse.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _createPostState.value = GenericUiState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error creating post: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _createPostState.value = GenericUiState.Error(e.localizedMessage ?: "An unexpected error occurred while creating the post")
                Log.e(TAG, "Exception creating post", e)
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
                    Log.e(TAG, "Invalid postId format for reaction: $postId, trying string comparison.")
                    originalState.posts.indexOfFirst { it.id.toString() == postId }
                }

                if (postIndex != -1) {
                    postToUpdate = originalState.posts[postIndex]
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
                        Log.d(TAG, "Reaction success: ${genericResponse.message}")
                    } else {
                        Log.e(TAG, "Failed to add reaction (API error): ${genericResponse.message}")
                        if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                            _postFeedState.value = originalState
                        }
                    }
                } else {
                    Log.e(TAG, "Error adding reaction (HTTP error): ${response.code()} - ${response.errorBody()?.string()}")
                    if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                        _postFeedState.value = originalState
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception adding reaction: ${e.localizedMessage}", e)
                if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                    _postFeedState.value = originalState
                }
            }
        }
    }

    fun createCommentForPost(postId: String, content: String) {
        viewModelScope.launch {
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
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        val newComment = genericResponse.data
                        Log.d(TAG, "Comment created successfully: $newComment")
                        _commentsState.update { currentCommentUiState ->
                            when (currentCommentUiState) {
                                is GenericUiState.Success -> {
                                    val existingItems = currentCommentUiState.data.items ?: emptyList()
                                    val updatedItems = listOf(newComment) + existingItems
                                    currentCommentUiState.copy(
                                        data = currentCommentUiState.data.copy(
                                            items = updatedItems,
                                            totalCount = currentCommentUiState.data.totalCount + 1
                                        )
                                    )
                                }
                                else -> {
                                    GenericUiState.Success(
                                        PaginatedCommentsResponse(
                                            items = listOf(newComment), totalCount = 1, page = 0,
                                            pageSize = commentPageSize, totalPages = 1
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        val errorMsg = genericResponse.message ?: "Failed to create comment"
                        Log.e(TAG, "Failed to create comment: $errorMsg")
                        // Revert optimistic update for comment count
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
                } else {
                    val errorBodyStr = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBodyStr, "Failed to create comment (HTTP ${response.code()})")
                    Log.e(TAG, "Failed to create comment (HTTP Error): $extractedMessage. Full Body: $errorBodyStr")
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
                Log.e(TAG, "Exception creating comment: ${e.localizedMessage}")
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
        if (currentPostIdForComments != postId || initialLoad) {
            resetCommentsStateInternal()
        }
        currentPostIdForComments = postId

        if (isCommentLoading && !initialLoad) return
        if (!initialLoad && allCommentsForPostLoaded) {
            Log.d(TAG, "All comments already loaded for post $postId.")
            return
        }

        viewModelScope.launch {
            isCommentLoading = true
            val offsetToFetch = if (initialLoad) 0 else currentCommentPage * commentPageSize

            if (initialLoad) {
                _commentsState.value = GenericUiState.Loading
            }

            try {
                val response = postRepository.getComments(postId, limit = commentPageSize, offset = offsetToFetch)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        val paginatedData = genericResponse.data
                        val newComments = paginatedData.items ?: emptyList()

                        _commentsState.update { currentState ->
                            val existingComments = if (initialLoad || currentState !is GenericUiState.Success) {
                                emptyList()
                            } else {
                                currentState.data.items ?: emptyList()
                            }
                            val updatedItems = existingComments + newComments
                            GenericUiState.Success(
                                paginatedData.copy(
                                    items = updatedItems,
                                    totalCount = paginatedData.totalCount,
                                    page = currentCommentPage,
                                    totalPages = paginatedData.totalPages
                                )
                            )
                        }
                        if (newComments.isNotEmpty()){
                            currentCommentPage++
                        }
                        allCommentsForPostLoaded = newComments.size < commentPageSize || paginatedData.totalPages <= currentCommentPage
                    } else {
                        val errorMsg = genericResponse.message ?: "Failed to load comments for post $postId"
                        if (initialLoad || _commentsState.value !is GenericUiState.Success) {
                            _commentsState.value = GenericUiState.Error(errorMsg)
                        }
                        Log.e(TAG, "API error fetching comments: $errorMsg")
                    }
                } else {
                    val errorBodyStr = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBodyStr, "Failed to load comments (HTTP ${response.code()}) for post $postId")
                    if (initialLoad || _commentsState.value !is GenericUiState.Success) {
                        _commentsState.value = GenericUiState.Error(extractedMessage)
                    }
                    Log.e(TAG, "HTTP error fetching comments (page $currentCommentPage): $extractedMessage. Full error body: $errorBodyStr")
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Network error fetching comments for post $postId"
                if (initialLoad || _commentsState.value !is GenericUiState.Success) {
                    _commentsState.value = GenericUiState.Error(errorMsg)
                }
                Log.e(TAG, "Exception fetching comments (page $currentCommentPage): $errorMsg", e)
            } finally {
                isCommentLoading = false
            }
        }
    }

    private fun resetCommentsStateInternal() {
        _commentsState.value = GenericUiState.Idle
        currentCommentPage = 0
        isCommentLoading = false
        allCommentsForPostLoaded = false
    }

    fun resetCommentsState() {
        resetCommentsStateInternal()
        currentPostIdForComments = null
    }
}