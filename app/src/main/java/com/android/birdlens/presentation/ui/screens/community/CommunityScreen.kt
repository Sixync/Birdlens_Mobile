// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/community/CommunityScreen.kt
package com.android.birdlens.presentation.ui.screens.community

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.data.model.post.PostResponse
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.CommunityViewModel
import com.android.birdlens.presentation.viewmodel.PostFeedUiState
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    communityViewModel: CommunityViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val postFeedState by communityViewModel.postFeedState.collectAsState()
    val listState = rememberLazyListState()

    var showCommentSheetForPostId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Effect for pagination
    LaunchedEffect(listState, postFeedState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty()) {
                    val lastVisibleItemIndex = visibleItems.last().index
                    val currentSuccessState = postFeedState
                    if (currentSuccessState is PostFeedUiState.Success && currentSuccessState.canLoadMore && !currentSuccessState.isLoadingMore) {
                        if (lastVisibleItemIndex >= currentSuccessState.posts.size - 5) { // Trigger load more when 5 items from end
                            communityViewModel.fetchPosts()
                        }
                    }
                }
            }
    }

    AppScaffold(
        navController = navController,
        topBar = {
            CommunityHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                // Navigate to CreatePostScreen
                onClick = { navController.navigate(Screen.CreatePost.route) },
                containerColor = ButtonGreen,
                contentColor = TextWhite
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Post")
            }
        },
        showBottomBar = true
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = postFeedState) {
                is PostFeedUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = TextWhite
                    )
                }
                is PostFeedUiState.Success -> {
                    if (state.posts.isEmpty() && !state.isLoadingMore) {
                        Text(
                            "No posts yet. Be the first to share!",
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp) // Added bottom padding for FAB
                        ) {
                            items(state.posts, key = { it.id }) { post ->
                                CommunityPostCard(
                                    post = post,
                                    onLikeClick = {
                                        communityViewModel.addReactionToPost(post.id.toString(), "like")
                                    },
                                    onCommentClick = {
                                        showCommentSheetForPostId = post.id.toString()
                                        communityViewModel.fetchCommentsForPost(post.id.toString(), initialLoad = true) // Fetch comments for this post
                                        coroutineScope.launch { sheetState.show() }
                                    },
                                    onShareClick = { /* TODO */ }
                                )
                            }
                            // Loading indicator for pagination
                            if (state.isLoadingMore) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = TextWhite)
                                    }
                                }
                            }
                        }
                    }
                }
                is PostFeedUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Error: ${state.message}",
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { communityViewModel.fetchPosts(initialLoad = true) }) { // Pass initialLoad = true for retry
                            Text("Retry")
                        }
                    }
                }
                is PostFeedUiState.Idle -> {
                    // This state should ideally transition to Loading quickly
                }
            }
        }

        // Modal Bottom Sheet for Comments
        if (showCommentSheetForPostId != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showCommentSheetForPostId = null
                    communityViewModel.resetCommentsState() // Reset comment state when sheet dismissed
                    coroutineScope.launch { sheetState.hide() }
                },
                sheetState = sheetState,
                containerColor = CardBackground.copy(alpha = 0.95f) // Semi-transparent background
            ) {
                CommentSection(
                    postId = showCommentSheetForPostId!!,
                    communityViewModel = communityViewModel,
                    onDismiss = {
                        showCommentSheetForPostId = null
                        communityViewModel.resetCommentsState()
                        coroutineScope.launch { sheetState.hide() }
                    }
                )
            }
        }
    }
}

@Composable
fun CommunityHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(id = R.string.community_title),
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNavigateToCart) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = stringResource(id = R.string.icon_cart_description),
                    tint = TextWhite
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Using the CustomTextField from login screen for consistency, or define a similar one
        com.android.birdlens.presentation.ui.screens.login.CustomTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(id = R.string.community_search_placeholder),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            backgroundColor = SearchBarBackground,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.icon_search_description), tint = SearchBarPlaceholderText) }
        )
    }
}

@Composable
fun CommunityPostCard(
    post: PostResponse,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Ensure cards have padding if LazyColumn doesn't
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // User Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = post.posterAvatarUrl,
                        error = painterResource(id = R.drawable.ic_launcher_foreground), // Generic placeholder
                        placeholder = painterResource(id = R.drawable.ic_launcher_background)
                    ),
                    contentDescription = stringResource(id = R.string.community_user_avatar_description),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray), // Placeholder background
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = post.posterName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = formatTimestamp(post.createdAt), color = TextWhite.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }

            // Post Image (if any)
            if (!post.imagesUrls.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = post.imagesUrls.first(), // Show first image
                        error = painterResource(id = R.drawable.bg_placeholder_image), // More fitting placeholder
                        placeholder = painterResource(id = R.drawable.bg_placeholder_image)
                    ),
                    contentDescription = stringResource(id = R.string.community_post_image_description),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 350.dp) // Constrain height
                        .aspectRatio(16f / 9f, matchHeightConstraintsFirst = false) // Maintain aspect ratio
                        .background(Color.DarkGray), // Placeholder background for image area
                    contentScale = ContentScale.Crop
                )
            }

            // Post Content
            Text(
                text = post.content,
                color = TextWhite,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround // Distribute buttons evenly
            ) {
                ActionButton(
                    icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    text = "${post.likesCount}", // Dynamic count
                    accessibilityTextResId = R.string.community_action_like,
                    onClick = onLikeClick,
                    tint = if (post.isLiked) Color.Red else TextWhite // Change tint if liked
                )
                ActionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = "${post.commentsCount}", // Dynamic count
                    accessibilityTextResId = R.string.community_action_comment,
                    onClick = onCommentClick
                )
                ActionButton(
                    icon = Icons.Outlined.Share,
                    text = "${post.sharesCount}", // Dynamic count
                    accessibilityTextResId = R.string.community_action_share,
                    onClick = onShareClick
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    accessibilityTextResId: Int, // For accessibility
    onClick: () -> Unit,
    tint: Color = TextWhite // Default tint
) {
    val actionName = stringResource(id = accessibilityTextResId)
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = actionName, // Use localized string for accessibility
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = tint, fontSize = 14.sp)
    }
}

// Preview for CommunityScreen - assumes ViewModel is handled by compose viewModel()
@SuppressLint("StateFlowValueCalledInComposition")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun CommunityScreenPreview_Success_WithData() {
    BirdlensTheme {
        val samplePost = PostResponse(
            id = 1L,
            posterAvatarUrl = null,
            posterName = "BirdLover123",
            createdAt = OffsetDateTime.now().toString(),
            imagesUrls = listOf("https://images.unsplash.com/photo-1518992028580-6d57bd80f2dd?w=800&auto=format&fit=crop&q=60"),
            content = "Look at this beautiful Eastern Bluebird I spotted today! #birdwatching #nature",
            likesCount = 15,
            commentsCount = 3,
            sharesCount = 2,
            isLiked = false
        )
        val mockViewModel: CommunityViewModel = viewModel()
        // For preview, directly set the state if possible (requires making _postFeedState internal or having a setter)
        // This is a simplified way; in complex scenarios, use Hilt for preview or mock repository.
        LaunchedEffect(Unit) {
            val internalStateField = CommunityViewModel::class.java.getDeclaredField("_postFeedState")
            internalStateField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val mutableFlow = internalStateField.get(mockViewModel) as MutableStateFlow<PostFeedUiState>
            mutableFlow.value = PostFeedUiState.Success(posts = listOf(samplePost, samplePost.copy(id = 2L, isLiked = true, likesCount = 1)), canLoadMore = true)
        }

        CommunityScreen(navController = rememberNavController(), communityViewModel = mockViewModel)
    }
}