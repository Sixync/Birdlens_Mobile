// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/community/CommunityScreen.kt
package com.android.birdlens.presentation.ui.screens.community

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold // Import
import com.android.birdlens.ui.theme.*

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

@Composable
fun CommunityScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = LocalInspectionMode.current // Keep for previews
) {
    var searchQuery by remember { mutableStateOf("") }

    val posts = remember {
        mutableStateListOf(
            CommunityPost(id = 1, userImageUrl = "https://images.unsplash.com/profile-1446404465118-3a53b909cc82?ixlib=rb-0.3.5&q=80&fm=jpg&crop=faces&cs=tinysrgb&fit=crop&h=128&w=128&s=27a346c2362207494baa7b76f5d606e5", username = "Bird Enthusiast", timePosted = "2h ago", imageUrl = "https://images.unsplash.com/photo-1444464666168-49d633b86797?w=800&auto=format&fit=crop&q=60", description = "Just spotted this beautiful Eastern Bluebird in my backyard! 🐦 #BirdWatching #Nature", likes = 128, comments = 24, shares = 12),
            CommunityPost(id = 2, userImageUrl = "https://images.unsplash.com/profile-1604758536753-68fd6f23aaf7image?ixlib=rb-4.0.3&crop=faces&fit=crop&w=128&h=128", username = "Nature Explorer", timePosted = "5h ago", imageUrl = "https://images.unsplash.com/photo-1452570053594-1b985d6ea890?w=800&auto=format&fit=crop&q=60", description = "Early morning catch! This hummingbird was enjoying the morning nectar 🌸 #Hummingbird #Wildlife", likes = 256, comments = 42, shares = 18),
            CommunityPost(id = 3, userImageUrl = "https://images.unsplash.com/profile-1604758536753-68fd6f23aaf7image?ixlib=rb-4.0.3&crop=faces&fit=crop&w=128&h=128", username = "Wildlife Photographer", timePosted = "1d ago", imageUrl = "https://images.unsplash.com/photo-1472561300420-3eac65ab572a?w=800&auto=format&fit=crop&q=60", description = "Captured this majestic owl in its natural habitat. Such a magnificent creature! 🦉 #WildlifePhotography", likes = 512, comments = 78, shares = 45)
        )
    }

    AppScaffold(
        navController = navController,
        topBar = {
            CommunityHeader( // This header is specific enough to be defined/called here
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) }
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding) // Apply padding from AppScaffold
                .fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
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
                    onCommentClick = { /* TODO */ },
                    onShareClick = { /* TODO */ }
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
    // Surface for header is removed as AppScaffold provides the background.
    // Padding is applied by the topBar slot arrangement in AppScaffold.
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) { // Adjust padding as needed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Community", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNavigateToCart) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = TextWhite)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = SearchBarBackground,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextWhite)
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text("Search community posts...", color = SearchBarPlaceholderText, fontSize = 16.sp)
                            }
                            innerTextField()
                        }
                    }
                )
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
            .padding(horizontal = 16.dp, vertical = 8.dp), // This padding is for between cards
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = post.userImageUrl, error = rememberAsyncImagePainter(model = "https://via.placeholder.com/40")),
                    contentDescription = "User Avatar",
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = post.username, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(text = post.timePosted, color = TextWhite.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }
            Image(
                painter = rememberAsyncImagePainter(model = post.imageUrl, error = rememberAsyncImagePainter(model = "https://via.placeholder.com/400x300")),
                contentDescription = "Post Image",
                modifier = Modifier.fillMaxWidth().height(300.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = post.description,
                color = TextWhite,
                fontSize = 16.sp,
                modifier = Modifier.padding(16.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ActionButton(icon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, text = "${post.likes}", onClick = onLikeClick, tint = if (post.isLiked) Color.Red else TextWhite)
                ActionButton(icon = Icons.Outlined.ChatBubbleOutline, text = "${post.comments}", onClick = onCommentClick)
                ActionButton(icon = Icons.Outlined.Share, text = "${post.shares}", onClick = onShareClick)
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = TextWhite
) {
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
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