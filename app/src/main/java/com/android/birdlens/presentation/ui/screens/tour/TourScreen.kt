// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/tour/TourScreen.kt
package com.android.birdlens.presentation.ui.screens.tour

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource // Make sure this is imported
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R // Make sure this is imported
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.ui.theme.*

// Data Model TourItem remains the same
data class TourItem(val id: Int, val imageUrl: String, val title: String = "")

@Composable
fun TourScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToAllEvents: () -> Unit,
    onNavigateToAllTours: () -> Unit,
    onNavigateToPopularTours: () -> Unit,
    onTourItemClick: (Int) -> Unit,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    var searchQuery by remember { mutableStateOf("") }

    // Dummy data (remains the same)
    val eventsOfTheYear = listOf(
        TourItem(1, "https://plus.unsplash.com/premium_photo-1673283380436-ac702dc85c64?w=300&auto=format&fit=crop&q=60", "Hoi An Lanterns"),
        TourItem(2, "https://images.unsplash.com/photo-1528181304800-259b08848526?w=300&auto=format&fit=crop&q=60", "Ha Long Bay"),
        TourItem(3, "https://images.unsplash.com/photo-1559507984-555c777a7554?w=300&auto=format&fit=crop&q=60", "Sapa Rice Terraces")
    )
    val popularTour = TourItem(4, "https://images.unsplash.com/photo-1567020009789-972f00d0f1f0?w=800&auto=format&fit=crop&q=60", "Night Market")
    val allToursData = listOf(
        TourItem(5, "https://images.unsplash.com/photo-1567020009789-972f00d0f1f0?w=300&auto=format&fit=crop&q=60", "Ancient Town"),
        TourItem(6, "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=300&auto=format&fit=crop&q=60", "Beach Sunset"),
        TourItem(7, "https://images.unsplash.com/photo-1483728642387-6c351bEC1d69?w=300&auto=format&fit=crop&q=60", "Mountain Peak")
    )

    AppScaffold(
        navController = navController,
        topBar = {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                TourScreenHeader(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) }
                )
            }
        },
        showBottomBar = true,
        floatingActionButton = { }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            item {
                SectionWithHorizontalList(
                    titleResId = R.string.tour_screen_events_of_year, // Use ResId
                    items = eventsOfTheYear,
                    onItemClick = { tourItem -> onTourItemClick(tourItem.id) },
                    onSeeAllClick = onNavigateToAllEvents,
                    isPreviewMode = isPreviewMode
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                SectionHeader(titleResId = R.string.tour_screen_popular_tour, onSeeAllClick = onNavigateToPopularTours) // Use ResId
                PopularTourCard(item = popularTour, onClick = { onTourItemClick(popularTour.id) }, isPreviewMode = isPreviewMode)
                PageIndicator(count = 3, selectedIndex = 0)
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                SectionWithHorizontalList(
                    titleResId = R.string.tour_screen_all_tours, // Use ResId
                    items = allToursData,
                    onItemClick = { tourItem -> onTourItemClick(tourItem.id) },
                    onSeeAllClick = onNavigateToAllTours,
                    isPreviewMode = isPreviewMode
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TourScreenHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToCart: () -> Unit
) {
    Column {
        Surface(
            shape = RoundedCornerShape(50),
            color = SearchBarBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                cursorBrush = SolidColor(TextWhite),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(id = R.string.icon_search_description),
                            tint = SearchBarPlaceholderText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    stringResource(id = R.string.search_something),
                                    color = SearchBarPlaceholderText,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    stringResource(id = R.string.tour_screen_title_birdlens), // Localized
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenWave2,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    stringResource(id = R.string.tour_screen_subtitle_tours), // Localized
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            IconButton(onClick = onNavigateToCart) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = stringResource(id = R.string.icon_cart_description),
                    tint = TextWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(titleResId: Int, onSeeAllClick: () -> Unit) { // Changed to take titleResId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = titleResId), // Use stringResource
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = TextWhite)
        )
        Text(
            text = stringResource(id = R.string.see_all), // Use stringResource
            style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.8f)),
            modifier = Modifier.clickable(onClick = onSeeAllClick)
        )
    }
}

@Composable
fun SectionWithHorizontalList(
    titleResId: Int, // Changed to take titleResId
    items: List<TourItem>,
    onItemClick: (TourItem) -> Unit,
    onSeeAllClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Column {
        SectionHeader(titleResId = titleResId, onSeeAllClick = onSeeAllClick) // Pass ResId
        LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                TourItemCardSmall(item = item, onClick = { onItemClick(item) }, isPreviewMode = isPreviewMode)
            }
        }
    }
}

@Composable
fun TourItemCardSmall(
    item: TourItem,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(width = 150.dp, height = 200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isPreviewMode) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(id = R.string.tour_screen_preview_image_small), // Localized
                        color = TextWhite.copy(alpha = 0.7f)
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageUrl),
                    contentDescription = item.title.ifEmpty { stringResource(id = R.string.tour_image_description) }, // Localized fallback
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (item.title.isNotEmpty()) { // Dynamic title, not from string resources
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 300f // Adjust this value for the gradient's start position
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(item.title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun PopularTourCard(
    item: TourItem,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isPreviewMode) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(id = R.string.tour_screen_preview_image_popular), // Localized
                        color = TextWhite.copy(alpha = 0.7f)
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageUrl),
                    contentDescription = item.title.ifEmpty { stringResource(id = R.string.popular_tour_image_description) }, // Localized fallback
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (item.title.isNotEmpty()) { // Dynamic title
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.7f)),
                                startY = 300f // Adjust for gradient start
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(item.title, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// PageIndicator remains the same

@Composable
fun PageIndicator(count: Int, selectedIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == selectedIndex) PageIndicatorActive else PageIndicatorInactive)
            )
        }
    }
}


@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun TourScreenPreview() {
    BirdlensTheme {
        TourScreen(
            navController = rememberNavController(),
            onNavigateToAllEvents = {},
            onNavigateToAllTours = {},
            onNavigateToPopularTours = {},
            onTourItemClick = {},
            isPreviewMode = true
        )
    }
}