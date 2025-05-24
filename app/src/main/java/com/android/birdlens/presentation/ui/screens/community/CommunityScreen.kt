// app/src/main/java/com/android/birdlens/presentation/ui/screens/community/CommunityScreen.kt
package com.android.birdlens.presentation.ui.screens.community

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.screens.tour.BottomNavItem // Import
import com.android.birdlens.presentation.ui.screens.tour.BottomNavigationBar
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.flow.collect // Import

// Data models for Community Posts
data class CommunityPost(
    val id: Int,
    val userImageUrl: String,
    val username: String,
    val timePosted: String,
    val imageUrl: String,
    val description: String,
    val likes: Int,
    val comments: Int,
    val shares: Int,
    var isLiked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    var searchQuery by remember { mutableStateOf("") }

    val posts = remember {
        mutableStateListOf(
            CommunityPost(
                id = 1,
                userImageUrl = "https://images.unsplash.com/profile-1446404465118-3a53b909cc82?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=128&w=128&s=27a346c2362207494baa7b76f5d606e5",
                username = "Bird Enthusiast",
                timePosted = "2h ago",
                imageUrl = "https://images.unsplash.com/photo-1444464666168-49d633b86797?w=800&auto=format&fit=crop&q=60",
                description = "Just spotted this beautiful Eastern Bluebird in my backyard! 🐦 #BirdWatching #Nature",
                likes = 128,
                comments = 24,
                shares = 12
            ),
            CommunityPost(
                id = 2,
                userImageUrl = "https://images.unsplash.com/profile-1604758536753-68fd6f23aaf7image?ixlib=rb-4.0.3&crop=faces&fit=crop&w=128&h=128",
                username = "Nature Explorer",
                timePosted = "5h ago",
                imageUrl = "https://images.unsplash.com/photo-1452570053594-1b985d6ea890?w=800&auto=format&fit=crop&q=60",
                description = "Early morning catch! This hummingbird was enjoying the morning nectar 🌸 #Hummingbird #Wildlife",
                likes = 256,
                comments = 42,
                shares = 18
            ),
            CommunityPost(
                id = 3,
                userImageUrl = "https://images.unsplash.com/profile-1604758536753-68fd6f23aaf7image?ixlib=rb-4.0.3&crop=faces&fit=crop&w=128&h=128", // Example: same user, different post
                username = "Wildlife Photographer", // Can be same or different
                timePosted = "1d ago",
                imageUrl = "https://images.unsplash.com/photo-1472561300420-3eac65ab572a?w=800&auto=format&fit=crop&q=60", // Different image
                description = "Captured this majestic owl in its natural habitat. Such a magnificent creature! 🦉 #WildlifePhotography",
                likes = 512,
                comments = 78,
                shares = 45
            )
        )
    }

    val bottomNavItems = listOf(
        BottomNavItem("Filter", { Icon(Icons.Outlined.Tune, "Filter", tint = TextWhite.copy(alpha = 0.7f)) }, { Icon(Icons.Filled.Tune, "Filter", tint = TextWhite) }, Screen.Settings.route),
        BottomNavItem("Community", { Icon(Icons.Outlined.Groups, "Community", tint = TextWhite.copy(alpha = 0.7f)) }, { Icon(Icons.Filled.Groups, "Community", tint = TextWhite) }, Screen.Community.route),
        BottomNavItem("Map", { Icon(Icons.Outlined.Map, "Map", tint = TextWhite.copy(alpha = 0.7f)) }, { Icon(Icons.Filled.Map, "Map", tint = TextWhite) }, Screen.Map.route),
        BottomNavItem("Marketplace", { Icon(Icons.Outlined.ShoppingCart, "Marketplace", tint = TextWhite.copy(alpha = 0.7f)) }, { Icon(Icons.Filled.ShoppingCart, "Marketplace", tint = TextWhite) }, Screen.Marketplace.route),
        BottomNavItem("Calendar", { Icon(Icons.Outlined.CalendarToday, "Calendar", tint = TextWhite.copy(alpha = 0.7f)) }, { Icon(Icons.Filled.CalendarToday, "Calendar", tint = TextWhite) }, Screen.Tour.route)
    )

    var selectedBottomNavItem by remember {
        mutableStateOf(bottomNavItems.indexOfFirst { it.route == Screen.Community.route }.coerceAtLeast(0))
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            val newIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
            if (newIndex != -1) {
                selectedBottomNavItem = newIndex
            } else {
                // If on a sub-screen not in bottom nav, keep "Community" tab selected.
                selectedBottomNavItem = bottomNavItems.indexOfFirst { it.route == Screen.Community.route }.coerceAtLeast(0)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background with gradient waves (consistent with WelcomeScreen/TourScreen)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(color = GreenDeep)
            val path1 = Path().apply { moveTo(0f, canvasHeight * 0.1f); cubicTo(canvasWidth * 0.2f, canvasHeight * 0.05f, canvasWidth * 0.3f, canvasHeight * 0.4f, canvasWidth * 0.6f, canvasHeight * 0.35f); cubicTo(canvasWidth * 0.9f, canvasHeight * 0.3f, canvasWidth * 1.1f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.7f); lineTo(0f, canvasHeight * 0.8f); close() }
            drawPath(path = path1, brush = Brush.radialGradient(listOf(GreenWave1.copy(alpha = 0.8f), GreenWave3.copy(alpha = 0.6f), GreenDeep.copy(alpha = 0.3f)), center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f), radius = canvasWidth * 0.8f))
            val path2 = Path().apply { moveTo(canvasWidth, canvasHeight * 0.5f); cubicTo(canvasWidth * 0.8f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.3f, canvasWidth * 0.4f, canvasHeight * 0.4f); cubicTo(canvasWidth * 0.1f, canvasHeight * 0.5f, canvasWidth * 0.0f, canvasHeight * 0.9f, canvasWidth * 0.3f, canvasHeight); lineTo(canvasWidth, canvasHeight); close() }
            drawPath(path = path2, brush = Brush.linearGradient(listOf(GreenWave4.copy(alpha = 0.4f), GreenWave1.copy(alpha = 0.3f), GreenDeep.copy(alpha = 0.1f)), start = Offset(canvasWidth * 0.8f, canvasHeight * 0.5f), end = Offset(canvasWidth * 0.3f, canvasHeight)))
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(), // Apply padding for system bars
            containerColor = Color.Transparent, // Make Scaffold transparent to see canvas
            bottomBar = {
                BottomNavigationBar(
                    items = bottomNavItems,
                    selectedItemIndex = selectedBottomNavItem,
                    onItemSelected = { index ->
                        val destinationRoute = bottomNavItems[index].route
                        if (navController.currentDestination?.route != destinationRoute &&
                            !destinationRoute.contains("placeholder")) { // Avoid navigating to placeholders
                            navController.navigate(destinationRoute) {
                                popUpTo(Screen.Tour.route) { // Assuming Tour is main/home for this nav graph
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding) // Apply padding from Scaffold
                    .fillMaxSize()
            ) {
                // Header
                CommunityHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) }
                )

                // Posts List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), // Takes remaining space in the Column
                    contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp) // Padding for list items
                ) {
                    items(posts, key = { it.id }) { post ->
                        CommunityPostCard(
                            post = post,
                            onLikeClick = {
                                val postIndex = posts.indexOfFirst { p -> p.id == post.id }
                                if (postIndex != -1) {
                                    val currentPost = posts[postIndex]
                                    posts[postIndex] = currentPost.copy(
                                        isLiked = !currentPost.isLiked,
                                        likes = if (!currentPost.isLiked) currentPost.likes + 1 else currentPost.likes - 1
                                    )
                                }
                            },
                            onCommentClick = { /* TODO: Implement comment functionality */ },
                            onShareClick = { /* TODO: Implement share functionality */ }
                        )
                    }
                }
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
    Surface(modifier = modifier.fillMaxWidth(), color = Color.Transparent) { // Header itself is transparent
        Column(modifier = Modifier.padding(16.dp)) { // Padding for header content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Community", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                IconButton(onClick = onNavigateToCart) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        tint = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            Surface(
                shape = RoundedCornerShape(50),
                color = SearchBarBackground, // Defined in theme
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextWhite // Changed from SearchBarPlaceholderText for better visibility
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        textStyle =
                        androidx.compose.ui.text.TextStyle( // Explicit TextStyle
                            color = TextWhite,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp), // Ensure text field itself has some padding
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) { // Align placeholder to start
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search community posts...",
                                        color = SearchBarPlaceholderText, // Use placeholder color
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityPostCard(
    post: CommunityPost,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Padding around each card
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground) // Defined in theme
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // User Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                Image(
                    painter =
                    rememberAsyncImagePainter(
                        model = post.userImageUrl,
                        error = // Placeholder if image fails to load
                        rememberAsyncImagePainter(
                            model = "https://via.placeholder.com/40" // Generic placeholder
                        )
                    ),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = post.username,
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = post.timePosted,
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            // Post Image
            Image(
                painter =
                rememberAsyncImagePainter(
                    model = post.imageUrl,
                    error =
                    rememberAsyncImagePainter(
                        model = "https://via.placeholder.com/400x300"
                    )
                ),
                contentDescription = "Post Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop
            )

            // Description
            Text(
                text = post.description,
                color = TextWhite,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround // Changed to SpaceAround for better distribution
            ) {
                ActionButton(
                    icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    text = "${post.likes}",
                    onClick = onLikeClick,
                    tint = if (post.isLiked) Color.Red else TextWhite // Red when liked
                )
                ActionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    text = "${post.comments}",
                    onClick = onCommentClick
                )
                ActionButton(
                    icon = Icons.Outlined.Share, // Using Outlined.Share, Filled.Share is also an option
                    text = "${post.shares}",
                    onClick = onShareClick
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, // Explicit import
    text: String,
    onClick: () -> Unit,
    tint: Color = TextWhite
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp), // Padding for touch target
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Decorative icon
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, color = tint, fontSize = 14.sp)
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun CommunityScreenPreview() {
    BirdlensTheme {
        CommunityScreen(navController = rememberNavController(), isPreviewMode = true)
    }
}