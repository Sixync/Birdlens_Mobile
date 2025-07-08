// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/community/CommunityScreen.kt
package com.android.birdlens.presentation.ui.screens.community

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

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
    val feedType by communityViewModel.feedType.collectAsState()

    var showCommentSheetForPostId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(communityViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            communityViewModel.onEnterScreen()
        }
    }

    LaunchedEffect(listState, postFeedState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
            if (visibleItems.isNotEmpty()) {
                val lastVisibleItemIndex = visibleItems.last().index
                val currentSuccessState = postFeedState
                if (currentSuccessState is PostFeedUiState.Success &&
                    currentSuccessState.canLoadMore &&
                    !currentSuccessState.isLoadingMore
                ) {
                    if (lastVisibleItemIndex >= currentSuccessState.posts.size - 5) {
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
                onClick = { navController.navigate(Screen.CreatePost.route) },
                containerColor = ButtonGreen,
                contentColor = TextWhite
            ) { Icon(Icons.Filled.Add, contentDescription = "Create Post") }
        },
        showBottomBar = true
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            val tabTitles = listOf("For You", "Following")
            val selectedTabIndex = if (feedType == "trending") 0 else 1
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = TextWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = GreenWave2
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            val newType = if (index == 0) "trending" else "all"
                            communityViewModel.setFeedType(newType)
                        },
                        text = { Text(text = title) },
                        selectedContentColor = GreenWave2,
                        unselectedContentColor = TextWhite.copy(alpha = 0.7f)
                    )
                }
            }

            Box(
                modifier =
                Modifier
                    .weight(1f)
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
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp)
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding =
                                PaddingValues(
                                    top = 8.dp,
                                    bottom = 80.dp
                                )
                            ) {
                                items(state.posts, key = { it.id }) { post ->
                                    // Logic: Conditionally render the correct card based on the post's `type`.
                                    // This is the core of the differentiation logic.
                                    when (post.type) {
                                        "sighting" -> SightingPostCard(
                                            post = post,
                                            onLikeClick = { communityViewModel.addReactionToPost(post.id.toString(), "like") },
                                            onCommentClick = {
                                                showCommentSheetForPostId = post.id.toString()
                                                communityViewModel.fetchCommentsForPost(post.id.toString(), initialLoad = true)
                                                coroutineScope.launch { sheetState.show() }
                                            },
                                            onShareClick = { /* TODO */ },
                                            onViewOnMapClick = { /* TODO: Navigate to map with coordinates */ }
                                        )
                                        else -> StandardPostCard( // Renamed from CommunityPostCard
                                            post = post,
                                            onLikeClick = { communityViewModel.addReactionToPost(post.id.toString(), "like") },
                                            onCommentClick = {
                                                showCommentSheetForPostId = post.id.toString()
                                                communityViewModel.fetchCommentsForPost(post.id.toString(), initialLoad = true)
                                                coroutineScope.launch { sheetState.show() }
                                            },
                                            onShareClick = { /* TODO */}
                                        )
                                    }
                                }
                                if (state.isLoadingMore) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.Center
                                        ) { CircularProgressIndicator(color = TextWhite) }
                                    }
                                }
                            }
                        }
                    }
                    is PostFeedUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Error: ${state.message}",
                                color = TextWhite.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { communityViewModel.fetchPosts(initialLoad = true) }
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                    is PostFeedUiState.Idle -> {
                    }
                }
            }
        }

        if (showCommentSheetForPostId != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showCommentSheetForPostId = null
                    communityViewModel
                        .resetCommentsState()
                    coroutineScope.launch { sheetState.hide() }
                },
                sheetState = sheetState,
                containerColor =
                CardBackground.copy(alpha = 0.95f)
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

// Logic: Renamed CommunityPostCard to StandardPostCard to better reflect its purpose.
@Composable
fun StandardPostCard(
    post: PostResponse,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PostHeader(post = post) // Use a common header

            if (!post.imagesUrls.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = post.imagesUrls.first(), error = painterResource(id = R.drawable.bg_placeholder_image), placeholder = painterResource(id = R.drawable.bg_placeholder_image)),
                    contentDescription = stringResource(id = R.string.community_post_image_description),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 350.dp).aspectRatio(16f / 9f, matchHeightConstraintsFirst = false).background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = post.content,
                color = TextWhite,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
            )

            PostActions(post, onLikeClick, onCommentClick, onShareClick)
        }
    }
}

// Logic: Created a new, distinct SightingPostCard composable.
@Composable
fun SightingPostCard(
    post: PostResponse,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewOnMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border( // A gradient border to make it stand out as special.
                width = 2.dp,
                brush = Brush.linearGradient(colors = listOf(GreenWave2.copy(alpha = 0.8f), GreenWave3.copy(alpha = 0.6f))),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PostHeader(post = post, isSighting = true) // Pass flag to show sighting badge

            if (!post.imagesUrls.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(model = post.imagesUrls.first(), error = painterResource(id = R.drawable.bg_placeholder_image), placeholder = painterResource(id = R.drawable.bg_placeholder_image)),
                    contentDescription = stringResource(id = R.string.community_post_image_description),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 350.dp).aspectRatio(16f / 9f).background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )
            }

            // Data Strip: A structured section for key sighting info.
            SightingDataStrip(
                speciesCode = post.taggedSpeciesCode,
                sightingDate = post.sightingDate,
                locationName = post.locationName
            )

            if (post.content.isNotBlank()) {
                Text(
                    text = post.content, // User content is now more like a caption.
                    color = TextWhite.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                )
            }

            PostActions(post, onLikeClick, onCommentClick, onShareClick, onViewOnMapClick)
        }
    }
}

// Logic: A common header composable to reduce code duplication between cards.
// It includes logic to display a "Sighting" badge.
@Composable
fun PostHeader(post: PostResponse, isSighting: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = post.posterAvatarUrl, error = painterResource(id = R.drawable.ic_launcher_foreground), placeholder = painterResource(id = R.drawable.ic_launcher_background)),
            contentDescription = stringResource(id = R.string.community_user_avatar_description),
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = post.posterName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(text = formatTimestamp(post.createdAt), color = TextWhite.copy(alpha = 0.7f), fontSize = 14.sp)
        }
        if (isSighting) {
            Icon(Icons.Default.Verified, contentDescription = stringResource(id = R.string.sighting_post_badge), tint = GreenWave2, modifier = Modifier.size(24.dp))
        }
    }
}

// Logic: Common action bar to reduce duplication. It conditionally shows "View on Map" button.
@Composable
fun PostActions(
    post: PostResponse,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onViewOnMapClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ActionButton(icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, text = "${post.likesCount}", accessibilityTextResId = R.string.community_action_like, onClick = onLikeClick, tint = if (post.isLiked) Color.Red else TextWhite)
        ActionButton(icon = Icons.Outlined.ChatBubbleOutline, text = "${post.commentsCount}", accessibilityTextResId = R.string.community_action_comment, onClick = onCommentClick)
        ActionButton(icon = Icons.Outlined.Share, text = "${post.sharesCount}", accessibilityTextResId = R.string.community_action_share, onClick = onShareClick)
        onViewOnMapClick?.let {
            ActionButton(icon = Icons.Outlined.Map, text = stringResource(id = R.string.sighting_card_view_on_map), accessibilityTextResId = R.string.sighting_card_view_on_map, onClick = it)
        }
    }
}

// Logic: A new composable specifically for the structured data in a Sighting.
@Composable
fun SightingDataStrip(speciesCode: String?, sightingDate: String?, locationName: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        speciesCode?.let {
            SightingInfoRow(icon = painterResource(id = R.drawable.ic_bird_placeholder), label = stringResource(id = R.string.sighting_card_species_label), value = it)
        }
        sightingDate?.let {
            SightingInfoRow(icon = Icons.Default.CalendarToday, label = stringResource(id = R.string.sighting_card_date_label), value = formatSightingDate(it))
        }
        locationName?.let {
            SightingInfoRow(icon = Icons.Default.LocationOn, label = stringResource(id = R.string.sighting_card_location_label), value = it)
        }
    }
}

@Composable
fun SightingInfoRow(icon: Any, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (icon) {
            is ImageVector -> Icon(imageVector = icon, contentDescription = label, tint = GreenWave3, modifier = Modifier.size(18.dp))
            is androidx.compose.ui.graphics.painter.Painter -> Icon(painter = icon, contentDescription = label, tint = GreenWave3, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = TextWhite.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = value, color = TextWhite, fontWeight = FontWeight.Normal)
    }
}

// ... (Other composables like CommunityHeader, ActionButton, formatSightingDate, formatTimestamp remain the same) ...
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
        com.android.birdlens.presentation.ui.screens.login.CustomTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(id = R.string.community_search_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            backgroundColor = SearchBarBackground,
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription =
                    stringResource(id = R.string.icon_search_description),
                    tint = SearchBarPlaceholderText
                )
            }
        )
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
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
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

fun formatSightingDate(isoTimestamp: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoTimestamp)
        val localDate = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        localDate.format(formatter)
    } catch (e: Exception) {
        isoTimestamp.substringBefore("T") // Fallback
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun CommunityScreenPreview_Success_WithData() {
    BirdlensTheme {
        val samplePost =
            PostResponse(
                id = 1L,
                posterAvatarUrl = null,
                posterName = "BirdLover123",
                createdAt = OffsetDateTime.now().toString(),
                imagesUrls =
                listOf(
                    "https://images.unsplash.com/photo-1518992028580-6d57bd80f2dd?w=800&auto=format&fit=crop&q=60"
                ),
                content =
                "Look at this beautiful Eastern Bluebird I spotted today! #birdwatching #nature",
                likesCount = 15,
                commentsCount = 3,
                sharesCount = 2,
                isLiked = false,
                type = "general",
                sightingDate = null,
                taggedSpeciesCode = null,
                locationName = null,
                latitude = null,
                longitude = null
            )
        val sampleSighting =
            samplePost.copy(
                id = 2L,
                isLiked = true,
                likesCount = 1,
                type = "sighting",
                sightingDate = "2023-10-27T10:00:00Z",
                taggedSpeciesCode = "easblu",
                locationName = "Central Park"
            )
        val mockViewModel: CommunityViewModel = viewModel()
        LaunchedEffect(Unit) {
            val internalStateField =
                CommunityViewModel::class.java.getDeclaredField("_postFeedState")
            internalStateField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val mutableFlow =
                internalStateField.get(mockViewModel) as MutableStateFlow<PostFeedUiState>
            mutableFlow.value =
                PostFeedUiState.Success(
                    posts = listOf(sampleSighting, samplePost),
                    canLoadMore = true
                )
        }

        CommunityScreen(navController = rememberNavController(), communityViewModel = mockViewModel)
    }
}