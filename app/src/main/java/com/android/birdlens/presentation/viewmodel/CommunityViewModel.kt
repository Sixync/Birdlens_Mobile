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

// CommentUiState is not used directly in this file anymore, GenericUiState<PaginatedCommentsResponse> is used.
// sealed class CommentUiState { ... }

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


    init {
        fetchPosts(initialLoad = true)
    }

    fun fetchPosts(initialLoad: Boolean = false) {
        if (isPostLoadingPosts || (!initialLoad && allPostsLoaded)) return

        viewModelScope.launch {
            isPostLoadingPosts = true
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
                val response = postRepository.getPosts(limit = postPageSize, offset = currentPostPage * postPageSize)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        val paginatedData = genericResponse.data
                        val newPosts = paginatedData.items ?: emptyList() // Handle null items

                        _postFeedState.update { currentState ->
                            val existingPosts = if (initialLoad || currentState !is PostFeedUiState.Success) emptyList() else currentState.posts
                            PostFeedUiState.Success(
                                posts = existingPosts + newPosts,
                                canLoadMore = newPosts.size >= postPageSize,
                                isLoadingMore = false
                            )
                        }
                        currentPostPage++
                        allPostsLoaded = newPosts.size < postPageSize
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
                _postFeedState.update { currentState ->
                    if (currentState is PostFeedUiState.Success) {
                        currentState.copy(isLoadingMore = false)
                    } else {
                        currentState
                    }
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
                val files = mediaUris.mapNotNull { uri ->
                    fileUtil.getFileFromUri(uri)
                }

                val response = postRepository.createPost(
                    content, locationName, latitude, longitude, privacyLevel, type, isFeatured, files
                )
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _createPostState.value = GenericUiState.Success(genericResponse.data)
                        fetchPosts(initialLoad = true)
                    } else {
                        _createPostState.value = GenericUiState.Error(genericResponse.message ?: "Failed to create post")
                    }
                } else {
                    _createPostState.value = GenericUiState.Error("Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                _createPostState.value = GenericUiState.Error(e.localizedMessage ?: "An unexpected error occurred")
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
                if (postIdAsLong != null) {
                    postIndex = originalState.posts.indexOfFirst { it.id == postIdAsLong }
                } else {
                    Log.e("CommunityVM", "Invalid postId format for reaction: $postId")
                    postIndex = originalState.posts.indexOfFirst { it.id.toString() == postId }
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
                        Log.d("CommunityVM", "Reaction success: ${genericResponse.message}")
                    } else {
                        Log.e("CommunityVM", "Failed to add reaction (API error): ${genericResponse.message}")
                        if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                            _postFeedState.value = originalState
                        }
                    }
                } else {
                    Log.e("CommunityVM", "Error adding reaction (HTTP error): ${response.code()} - ${response.errorBody()?.string()}")
                    if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                        _postFeedState.value = originalState
                    }
                }
            } catch (e: Exception) {
                Log.e("CommunityVM", "Exception adding reaction: ${e.localizedMessage}", e)
                if (originalState is PostFeedUiState.Success && postToUpdate != null && postIndex != -1) {
                    _postFeedState.value = originalState
                }
            }
        }
    }

    fun createCommentForPost(postId: String, content: String) {
        viewModelScope.launch {
            _commentsState.value = GenericUiState.Loading
            try {
                val response = postRepository.createComment(postId, content)
                if (response.isSuccessful && response.body()?.error == false && response.body()?.data != null) {
                    val newComment = response.body()!!.data!!
                    Log.d("CommunityVM", "Comment created successfully: $newComment")

                    _postFeedState.update { currentState ->
                        if (currentState is PostFeedUiState.Success) {
                            val postIdAsLong = postId.toLongOrNull()
                            currentState.copy(posts = currentState.posts.map {
                                if ((postIdAsLong != null && it.id == postIdAsLong) || it.id.toString() == postId) {
                                    it.copy(commentsCount = it.commentsCount + 1)
                                } else it
                            })
                        } else currentState
                    }
                    // Add the new comment to the beginning of the current list in _commentsState
                    _commentsState.update { currentCommentUiState ->
                        if (currentCommentUiState is GenericUiState.Success) {
                            val updatedItems = listOf(newComment) + (currentCommentUiState.data.items ?: emptyList())
                            currentCommentUiState.copy(
                                data = currentCommentUiState.data.copy(
                                    items = updatedItems,
                                    totalCount = currentCommentUiState.data.totalCount + 1
                                    // page, pageSize, totalPages might need adjustment if you want perfect pagination after local add
                                )
                            )
                        } else {
                            // If previous state wasn't success (e.g., loading/error/idle), set it to success with the new comment
                            GenericUiState.Success(PaginatedCommentsResponse(listOf(newComment),1,1,1,1))
                        }
                    }
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: "Failed to create comment"
                    Log.e("CommunityVM", "Failed to create comment: $errorMsg")
                    _commentsState.value = GenericUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("CommunityVM", "Exception creating comment: ${e.localizedMessage}")
                _commentsState.value = GenericUiState.Error(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun fetchCommentsForPost(postId: String, initialLoad: Boolean = false) {
        if (isCommentLoading || (!initialLoad && allCommentsForPostLoaded)) return

        viewModelScope.launch {
            isCommentLoading = true
            if (initialLoad) {
                currentCommentPage = 0
                allCommentsForPostLoaded = false
                _commentsState.value = GenericUiState.Loading
            }

            try {
                val response = postRepository.getComments(postId, limit = commentPageSize, offset = currentCommentPage * commentPageSize)
                if (response.isSuccessful && response.body()?.error == false && response.body()?.data != null) {
                    val paginatedData = response.body()!!.data!!
                    val newComments = paginatedData.items ?: emptyList() // Handle null items

                    _commentsState.update { currentState ->
                        val existingComments = if(initialLoad || currentState !is GenericUiState.Success) emptyList() else currentState.data.items ?: emptyList()
                        GenericUiState.Success(
                            paginatedData.copy(items = existingComments + newComments) // Concatenate, ensuring items is non-null
                        )
                    }
                    currentCommentPage++
                    allCommentsForPostLoaded = newComments.size < commentPageSize
                } else {
                    _commentsState.value = GenericUiState.Error(response.body()?.message ?: "Failed to load comments")
                }
            } catch (e: Exception) {
                _commentsState.value = GenericUiState.Error(e.localizedMessage ?: "Network error")
            } finally {
                isCommentLoading = false
            }
        }
    }

    fun resetCommentsState() {
        _commentsState.value = GenericUiState.Idle
        currentCommentPage = 0
        isCommentLoading = false
        allCommentsForPostLoaded = false
    }
}

sealed class GenericUiState<out T> {
    data object Idle : GenericUiState<Nothing>()
    data object Loading : GenericUiState<Nothing>()
    data class Success<T>(val data: T) : GenericUiState<T>()
    data class Error(val message: String) : GenericUiState<Nothing>()
}