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
// import androidx.compose.foundation.text.BasicTextField // Not used in this version
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
// import androidx.compose.ui.graphics.SolidColor // Not used in this version
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource // Import this
// import androidx.compose.ui.text.ExperimentalTextApi // Not strictly needed if not using advanced text features here
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R // Import this
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.tour.PageIndicator
import com.android.birdlens.ui.theme.*

// TourDetailData data class remains the same
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

@OptIn(ExperimentalFoundationApi::class) // ExperimentalTextApi might not be needed now
@Composable
fun TourDetailScreen(
    navController: NavController,
    tourId: Int,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val tourDetail = remember(tourId) {
        // Dummy data for tour detail
        TourDetailData(
            id = tourId, title = "Tour #$tourId",
            images = listOf(
                "https://images.unsplash.com/photo-1547295026-2e935c753054?w=800&auto=format&fit=crop&q=60",
                "https://images.unsplash.com/photo-1589922026997-26049f03cf30?w=800&auto=format&fit=crop&q=60",
                "https://plus.unsplash.com/premium_photo-1673283380436-ac702dc85c64?w=800&auto=format&fit=crop&q=60"
            ),
            rating = 4.5f, reviewCount = 77, price = "150", // Example price value
            storeName = "Official BirdLens Store",
            description = "Explore the wonders of nature with our guided bird watching tour. This tour takes you through serene landscapes, offering opportunities to spot various bird species in their natural habitat. Suitable for all ages and experience levels."
        )
    }
    // birdlensGradientBrush is not used in the simplified DetailScreenHeader, can be removed if not used elsewhere.
    // val birdlensGradientBrush = remember { Brush.linearGradient(colors = listOf(BirdlensGradientStart, BirdlensGradientEnd)) }


    AppScaffold(
        navController = navController,
        topBar = {
            DetailScreenHeader(navController = navController)
        },
        showBottomBar = true
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                val pagerState = rememberPagerState(pageCount = { tourDetail.images.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) { pageIndex ->
                    if (isPreviewMode) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(id = R.string.tour_detail_preview_pager_image, pageIndex + 1), // Localized
                                color = TextWhite
                            )
                        }
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(model = tourDetail.images[pageIndex]),
                            contentDescription = stringResource(id = R.string.tour_detail_image_description, pageIndex + 1), // Localized
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                PageIndicator(
                    count = tourDetail.images.size,
                    selectedIndex = pagerState.currentPage,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Surface(
                    color = AuthCardBackground,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp)) {
                        Text( // Dynamic title
                            tourDetail.title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { index ->
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = stringResource(id = R.string.rating_star_description), // Localized
                                    tint = if (index < tourDetail.rating.toInt()) Color.Yellow else TextWhite.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text( // Dynamic review count
                                stringResource(id = R.string.tour_detail_reviews_count, tourDetail.reviewCount),
                                color = TextWhite.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text( // Dynamic price
                            stringResource(id = R.string.tour_detail_price_format, tourDetail.price),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(24.dp).background(StoreNamePlaceholderCircle, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tourDetail.storeName, color = TextWhite.copy(alpha = 0.9f), fontSize = 14.sp) // Dynamic store name
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            stringResource(id = R.string.tour_detail_description_label), // Localized
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text( // Dynamic description
                            tourDetail.description,
                            color = TextWhite.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { navController.navigate(Screen.PickDays.createRoute(tourDetail.id)) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                stringResource(id = R.string.tour_detail_pick_days_button), // Localized
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreenHeader(
    navController: NavController,
    // birdlensGradientBrush: Brush, // Removed as it's not used in this simplified header
    modifier: Modifier = Modifier
) {
    // Simplified header for detail screens
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back), // Localized
                tint = TextWhite
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            stringResource(id = R.string.tour_screen_title_birdlens), // Using the app title/brand for consistency
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = GreenWave2,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
            Icon(
                Icons.Filled.ShoppingCart,
                contentDescription = stringResource(id = R.string.icon_cart_description), // Localized
                tint = TextWhite,
                modifier = Modifier.size(28.dp)
            )
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