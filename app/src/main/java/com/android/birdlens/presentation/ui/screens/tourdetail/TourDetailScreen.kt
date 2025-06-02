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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
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
import com.android.birdlens.data.model.Tour
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.tour.PageIndicator
import com.android.birdlens.presentation.viewmodel.TourUIState
import com.android.birdlens.presentation.viewmodel.TourViewModel
import com.android.birdlens.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TourDetailScreen(
    navController: NavController,
    tourId: Long,
    modifier: Modifier = Modifier,
    tourViewModel: TourViewModel = viewModel(),
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val tourDetailState by tourViewModel.tourDetailState.collectAsState()

    LaunchedEffect(tourId) {
        if (tourId != -1L) {
            tourViewModel.fetchTourById(tourId)
        }
    }

    AppScaffold(
        navController = navController,
        topBar = {
            DetailScreenHeader(navController = navController)
        },
        showBottomBar = true
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = tourDetailState) {
                is TourUIState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is TourUIState.Success -> {
                    val tourDetail = state.data
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item { Spacer(modifier = Modifier.height(16.dp)) }

                        item {
                            val imagesToDisplay = tourDetail.imagesUrl ?: tourDetail.thumbnailUrl?.let { listOf(it) } ?: emptyList()
                            if (imagesToDisplay.isNotEmpty()) {
                                val pagerState = rememberPagerState(pageCount = { imagesToDisplay.size })
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(250.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) { pageIndex ->
                                    Image(
                                        painter = rememberAsyncImagePainter(model = imagesToDisplay[pageIndex]),
                                        contentDescription = stringResource(id = R.string.tour_detail_image_description, pageIndex + 1),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                PageIndicator(
                                    count = imagesToDisplay.size,
                                    selectedIndex = pagerState.currentPage,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(250.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No images available", color = TextWhite)
                                }
                            }
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
                                    Text(
                                        tourDetail.name,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Placeholder for Rating and Reviews
                                        repeat(4) { Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp)) }
                                        Icon(Icons.Filled.StarHalf, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(id = R.string.tour_detail_reviews_count, 77), // Dummy review count
                                            color = TextWhite.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        stringResource(id = R.string.tour_detail_price_format, "%.2f".format(tourDetail.price)),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextWhite)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(24.dp).background(StoreNamePlaceholderCircle, CircleShape))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            tourDetail.event?.title ?: tourDetail.location?.name ?: "Venue N/A",
                                            color = TextWhite.copy(alpha = 0.9f),
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        stringResource(id = R.string.tour_detail_description_label),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = TextWhite)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
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
                                            stringResource(id = R.string.tour_detail_pick_days_button),
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
                is TourUIState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}", color = TextWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { if(tourId != -1L) tourViewModel.fetchTourById(tourId) },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) {
                            Text(stringResource(R.string.retry), color = TextWhite)
                        }
                    }
                }
                is TourUIState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading), color = TextWhite.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreenHeader(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 4.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.back),
                tint = TextWhite
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            stringResource(id = R.string.tour_screen_title_birdlens),
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
                contentDescription = stringResource(id = R.string.icon_cart_description),
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
        val dummyTour = Tour(
            id = 1L, eventId = 1L, event = null, price = 99.99, capacity = 20,
            name = "Preview Amazing Bird Tour", description = "This is a detailed description of the amazing bird tour. You will see many colorful birds and learn about their habitats. Bring your binoculars!",
            thumbnailUrl = "https://images.unsplash.com/photo-1547295026-2e935c753054?w=800&auto=format&fit=crop&q=60",
            duration = 3, startDate = "2024-07-01T10:00:00Z", endDate = "2024-07-03T17:00:00Z",
            locationId = 1L, location = null, createdAt = "2024-01-01T10:00:00Z", updatedAt = null,
            imagesUrl = listOf(
                "https://images.unsplash.com/photo-1547295026-2e935c753054?w=800&auto=format&fit=crop&q=60",
                "https://images.unsplash.com/photo-1589922026997-26049f03cf30?w=800&auto=format&fit=crop&q=60"
            )
        )
        val previewTourViewModel: TourViewModel = viewModel()
        LaunchedEffect(Unit) {
            previewTourViewModel._tourDetailState.value = TourUIState.Success(dummyTour)
        }

        TourDetailScreen(
            navController = rememberNavController(),
            tourId = 1L,
            tourViewModel = previewTourViewModel,
            isPreviewMode = true
        )
    }
}