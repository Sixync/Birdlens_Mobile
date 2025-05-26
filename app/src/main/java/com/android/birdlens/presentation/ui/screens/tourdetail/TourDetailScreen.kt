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
import com.android.birdlens.presentation.ui.components.AppScaffold // Import
import com.android.birdlens.presentation.ui.screens.tour.PageIndicator // Reusing
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
        TourDetailData(
            id = tourId, title = "Amazing Tour #${tourId}",
            images = listOf(
                "https://images.unsplash.com/photo-1547295026-2e935c753054?w=800&auto=format&fit=crop&q=60",
                "https://images.unsplash.com/photo-1589922026997-26049f03cf30?w=800&auto=format&fit=crop&q=60",
                "https://plus.unsplash.com/premium_photo-1673283380436-ac702dc85c64?w=800&auto=format&fit=crop&q=60"
            ),
            rating = 4.5f, reviewCount = 77, price = "xxx", storeName = "Official BirdLens Store",
            description = "This is a longer description to see how it wraps and fills the space available for the tour details. Enjoy this fantastic journey!"
        )
    }
    val birdlensGradientBrush = remember { Brush.linearGradient(colors = listOf(BirdlensGradientStart, BirdlensGradientEnd)) }

    AppScaffold(
        navController = navController,
        topBar = {
            DetailScreenHeader( // This header is specific, define or call here
                navController = navController,
                birdlensGradientBrush = birdlensGradientBrush
            )
        },
        // Assuming TourDetail usually doesn't have the main bottom bar, but can be enabled if needed
        showBottomBar = true // Set to true if it should have a bottom bar, false otherwise.
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding) // Apply padding from AppScaffold
                .fillMaxSize()
                .padding(horizontal = 16.dp) // Original horizontal padding for list content
        ) {
            // DetailScreenHeader is in topBar slot.
            // item { Spacer(modifier = Modifier.height(16.dp)) } // May not be needed if header is in topBar

            item {
                val pagerState = rememberPagerState(pageCount = { tourDetail.images.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(16.dp))
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
                PageIndicator(count = tourDetail.images.size, selectedIndex = pagerState.currentPage)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                            Box(modifier = Modifier.size(24.dp).background(StoreNamePlaceholderCircle, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tourDetail.storeName, color = TextWhite.copy(alpha = 0.9f), fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Description", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextWhite))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tourDetail.description, color = TextWhite.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { /* TODO */ }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = ActionButtonLightGray), modifier = Modifier.weight(1f).height(48.dp)) {
                                Text("JOIN NOW", color = ActionButtonTextDark, fontWeight = FontWeight.SemiBold)
                            }
                            Button(onClick = { /* TODO */ }, shape = RoundedCornerShape(50), colors = ButtonDefaults.buttonColors(containerColor = ActionButtonLightGray), modifier = Modifier.weight(1f).height(48.dp)) {
                                Text("ADD TO CART", color = ActionButtonTextDark, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
    birdlensGradientBrush: Brush,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = modifier.padding(top = 16.dp, bottom = 0.dp)) { // Adjusted padding
        Surface(
            shape = RoundedCornerShape(50),
            color = SearchBarBackground,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                cursorBrush = SolidColor(TextWhite),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = SearchBarPlaceholderText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) { Text("Search something", color = SearchBarPlaceholderText, fontSize = 16.sp) }
                            innerTextField()
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("BIRDLENS", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = GreenWave2, letterSpacing = 1.sp))
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = TextWhite, modifier = Modifier.size(28.dp))
            }
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