// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/tourdetail/TourDetailScreen.kt
package com.android.birdlens.presentation.ui.screens.tourdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.tour.PageIndicator
import com.android.birdlens.ui.theme.*

data class TourDetailData(
    val id: Int,
    val title: String,
    val images: List<String>,
    val rating: Float,
    val reviewCount: Int,
    val price: String,
    val storeName: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalTextApi::class)
@Composable
fun TourDetailScreen(
    navController: NavController,
    tourId: Int,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val tourDetail = remember(tourId) {
        // In a real app, fetch this data based on tourId from a ViewModel
        TourDetailData(
            id = tourId, title = "Tour #$tourId", // Changed to match design image more closely
            images = listOf(
                "https://images.unsplash.com/photo-1547295026-2e935c753054?w=800&auto=format&fit=crop&q=60",
                "https://images.unsplash.com/photo-1589922026997-26049f03cf30?w=800&auto=format&fit=crop&q=60",
                "https://plus.unsplash.com/premium_photo-1673283380436-ac702dc85c64?w=800&auto=format&fit=crop&q=60"
            ),
            rating = 4.5f, reviewCount = 77, price = "xxx", storeName = "Official BirdLens Store",
            description = "pu9ahscouyohspciuhspuichqp9ushcpuiquiwshcpuwihoxuiquhciuiqhbciuiqabnxchaqpihcbh" // Using placeholder from design
        )
    }
    val birdlensGradientBrush = remember { Brush.linearGradient(colors = listOf(BirdlensGradientStart, BirdlensGradientEnd)) }

    AppScaffold(
        navController = navController,
        topBar = {
            DetailScreenHeader(
                navController = navController,
                birdlensGradientBrush = birdlensGradientBrush // This gradient might not be used if header is simple
            )
        },
        showBottomBar = true // Design shows bottom bar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
            // .padding(horizontal = 16.dp) // Padding is applied to the content Surface now
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) } // Space below header

            item {
                val pagerState = rememberPagerState(pageCount = { tourDetail.images.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp) // Add horizontal padding for the pager
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) { pageIndex ->
                    if (isPreviewMode) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                            Text("Preview Pager Img ${pageIndex + 1}", color = TextWhite)
                        }
                    } else {
                        Image(painter = rememberAsyncImagePainter(model = tourDetail.images[pageIndex]), contentDescription = "Tour Image ${pageIndex + 1}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                PageIndicator(
                    count = tourDetail.images.size,
                    selectedIndex = pagerState.currentPage,
                    modifier = Modifier.padding(horizontal = 16.dp) // Align indicator with pager
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Surface(
                    color = AuthCardBackground, // Use the frosted background
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // Rounded only at the top
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp) // Space between pager/indicator and this card
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp)) { // Added more vertical padding
                        Text(tourDetail.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextWhite))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index -> Icon(Icons.Filled.Star, contentDescription = "Star", tint = if (index < tourDetail.rating.toInt()) Color.Yellow else TextWhite.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${tourDetail.reviewCount} Reviews", color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Price: ${tourDetail.price}\$/trip", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextWhite))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Placeholder for store icon if any
                            Box(modifier = Modifier.size(24.dp).background(StoreNamePlaceholderCircle, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tourDetail.storeName, color = TextWhite.copy(alpha = 0.9f), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Description", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextWhite))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tourDetail.description, color = TextWhite.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(32.dp)) // Increased space before button

                        // New "Pick your days" button
                        Button(
                            onClick = { navController.navigate(Screen.PickDays.createRoute(tourDetail.id)) },
                            shape = RoundedCornerShape(50), // Pill shape
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen), // Use ButtonGreen
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp) // Slightly taller button
                        ) {
                            Text("Pick your days", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextWhite)
                        }
                        Spacer(modifier = Modifier.height(16.dp)) // Space at the bottom of the card
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun DetailScreenHeader(
    navController: NavController,
    birdlensGradientBrush: Brush, // This might be unused if header is simple
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    // The design uses a simpler header for detail screen, just back arrow and title.
    // The complex search header is usually for main discovery screens.
    // For consistency with "Pick Your Days" screen, we'll use a similar header.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 4.dp, end = 16.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            // The title "BIRDLENS" is more prominent in the design than the actual tour title here.
            // If tour title is needed, it would be in the content usually.
            "BIRDLENS",
            style = MaterialTheme.typography.headlineSmall.copy( // Adjusted for size
                fontWeight = FontWeight.ExtraBold,
                color = GreenWave2, // Using the bright green from theme
                letterSpacing = 1.sp
            ),
            modifier = Modifier.weight(1f) // Pushes cart icon to the end
        )
        IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = TextWhite, modifier = Modifier.size(28.dp))
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun TourDetailScreenPreview() {
    BirdlensTheme {
        TourDetailScreen(navController = rememberNavController(), tourId = 1, isPreviewMode = true)
    }
}