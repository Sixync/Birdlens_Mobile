// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/tour/TourScreen.kt
package com.android.birdlens.presentation.ui.screens.tour

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.model.Event
import com.android.birdlens.data.model.Tour
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
import com.android.birdlens.presentation.viewmodel.*
import com.android.birdlens.ui.theme.*

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun TourScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onNavigateToAllEvents: () -> Unit,
    onNavigateToAllTours: () -> Unit,
    onNavigateToPopularTours: () -> Unit,
    onTourItemClick: (Long) -> Unit,
    onEventItemClick: (Long) -> Unit,
    eventViewModel: EventViewModel = viewModel(),
    tourViewModel: TourViewModel = viewModel(),
    exploreViewModel: ExploreViewModel = viewModel(), // Instantiate the new ViewModel
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val eventsState by eventViewModel.eventsState.collectAsState()
    val popularTourState by tourViewModel.popularTourState.collectAsState()
    val horizontalToursState by tourViewModel.horizontalToursState.collectAsState()

    // Collect states from the new ExploreViewModel
    val searchQuery by exploreViewModel.searchQuery.collectAsState()
    val exploreUiState by exploreViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (eventsState is EventUIState.Idle) eventViewModel.fetchEvents(limit = 5)
        if (popularTourState is TourUIState.Idle) tourViewModel.fetchPopularTour()
        if (horizontalToursState is TourUIState.Idle) tourViewModel.fetchHorizontalTours(limit = 5)
    }

    AppScaffold(
        navController = navController,
        topBar = {
            // The header now gets its search logic from the ExploreViewModel
            TourScreenHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { exploreViewModel.onSearchQueryChanged(it) },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                onClearSearch = { exploreViewModel.clearSearch() }
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Conditionally display the search results dropdown
            AnimatedVisibility(visible = exploreUiState is ExploreUiState.Searching || exploreUiState is ExploreUiState.Success) {
                SearchResults(
                    state = exploreUiState,
                    onResultClick = { speciesCode ->
                        exploreViewModel.clearSearch()
                        navController.navigate(Screen.BirdInfo.createRoute(speciesCode))
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                // The new "Explore Birds" section
                item {
                    SectionHeader(titleResId = R.string.explore_birds_title, onSeeAllClick = { /* TODO: Navigate to a full explore screen */ })
                    when (val state = exploreUiState) {
                        is ExploreUiState.Searching -> LoadingIndicator()
                        is ExploreUiState.ExploreFeedSuccess -> {
                            LazyRow(
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.notableBirds, key = { it.speciesCode }) { bird ->
                                    BirdExploreCard(
                                        birdInfo = bird,
                                        onClick = { navController.navigate(Screen.BirdInfo.createRoute(bird.speciesCode)) }
                                    )
                                }
                            }
                        }
                        is ExploreUiState.Error -> ErrorStateText(state.message)
                        else -> { // Idle or Success from live search, show the default feed
                            // You might want a default loading state here if notable birds haven't loaded yet
                            val notableBirdsState = exploreViewModel.uiState.value
                            if (notableBirdsState is ExploreUiState.ExploreFeedSuccess) {
                                LazyRow(
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(notableBirdsState.notableBirds, key = { it.speciesCode }) { bird ->
                                        BirdExploreCard(
                                            birdInfo = bird,
                                            onClick = { navController.navigate(Screen.BirdInfo.createRoute(bird.speciesCode)) }
                                        )
                                    }
                                }
                            } else {
                                LoadingIndicator() // Show loading while notable birds are fetched initially
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    SectionHeader(titleResId = R.string.tour_screen_events_of_year, onSeeAllClick = onNavigateToAllEvents)
                    when (val state = eventsState) {
                        is EventUIState.Loading -> LoadingIndicator()
                        is EventUIState.Success -> {
                            val currentEventItems = state.data.items ?: emptyList()
                            if (currentEventItems.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(currentEventItems, key = { "event_year_${it.id}" }) { event ->
                                        EventItemCardSmall(
                                            event = event,
                                            onClick = { onEventItemClick(event.id) },
                                            isPreviewMode = isPreviewMode
                                        )
                                    }
                                }
                            } else {
                                EmptyStateText("No events found for this year yet.")
                            }
                        }
                        is EventUIState.Error -> ErrorStateText(state.message)
                        is EventUIState.Idle -> LoadingPlaceholder()
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    SectionHeader(titleResId = R.string.tour_screen_popular_tour, onSeeAllClick = onNavigateToPopularTours)
                    when (val state = popularTourState) {
                        is TourUIState.Loading -> LoadingIndicator()
                        is TourUIState.Success -> {
                            state.data?.let { popularTour ->
                                PopularTourCard(
                                    tour = popularTour,
                                    onClick = { onTourItemClick(popularTour.id) },
                                    isPreviewMode = isPreviewMode
                                )
                                PageIndicator(count = 1, selectedIndex = 0)
                            } ?: EmptyStateText("No popular tour available.")
                        }
                        is TourUIState.Error -> ErrorStateText(state.message)
                        is TourUIState.Idle -> LoadingPlaceholder()
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    SectionHeader(titleResId = R.string.tour_screen_all_tours, onSeeAllClick = onNavigateToAllTours)
                    when (val state = horizontalToursState) {
                        is TourUIState.Loading -> LoadingIndicator()
                        is TourUIState.Success -> {
                            val tourItems = state.data.items ?: emptyList()
                            if (tourItems.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(tourItems, key = { "all_tours_${it.id}" }) { tour ->
                                        TourItemCardSmall(
                                            tour = tour,
                                            onClick = { onTourItemClick(tour.id) },
                                            isPreviewMode = isPreviewMode
                                        )
                                    }
                                }
                            } else {
                                EmptyStateText("No tours available at the moment.")
                            }
                        }
                        is TourUIState.Error -> ErrorStateText(state.message)
                        is TourUIState.Idle -> LoadingPlaceholder()
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SearchResults(
    state: ExploreUiState,
    onResultClick: (speciesCode: String) -> Unit
) {
    Surface(
        color = CardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        when (state) {
            is ExploreUiState.Searching -> {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextWhite)
                }
            }
            is ExploreUiState.Success -> {
                if (state.results.isEmpty()) {
                    Text(
                        "No birds found for your search.",
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(state.results, key = { it.speciesCode }) { bird ->
                            SearchResultItem(bird = bird, onClick = { onResultClick(bird.speciesCode) })
                        }
                    }
                }
            }
            is ExploreUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                // Do nothing for Idle or ExploreFeedSuccess states in this context
            }
        }
    }
}

@Composable
fun SearchResultItem(bird: BirdSpecies, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = bird.commonName, color = TextWhite, fontWeight = FontWeight.SemiBold)
            Text(text = bird.scientificName, color = TextWhite.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable
fun TourScreenHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    Column(Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)) {
        CustomTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Explore birds by name...",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            backgroundColor = SearchBarBackground,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = SearchBarPlaceholderText) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = SearchBarPlaceholderText)
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    stringResource(id = R.string.tour_screen_title_birdlens),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = GreenWave2, letterSpacing = 1.sp)
                )
                Text(
                    stringResource(id = R.string.tour_screen_subtitle_tours),
                    style = MaterialTheme.typography.titleLarge.copy(color = TextWhite, fontWeight = FontWeight.SemiBold)
                )
            }
            IconButton(onClick = onNavigateToCart) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = TextWhite, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun BirdExploreCard(birdInfo: BirdExploreInfo, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(width = 160.dp, height = 210.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = birdInfo.imageUrl,
                    error = painterResource(id = R.drawable.bg_placeholder_image) // Fallback placeholder
                ),
                contentDescription = birdInfo.commonName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.DarkGray)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = birdInfo.commonName,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = birdInfo.sciName,
                    color = TextWhite.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
private fun LoadingIndicator() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TextWhite)
    }
}

@Composable
private fun EmptyStateText(message: String) {
    Text(message, color = TextWhite.copy(alpha = 0.7f), modifier = Modifier
        .padding(vertical = 8.dp)
        .fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
private fun ErrorStateText(message: String) {
    Text("Error: $message", color = TextWhite.copy(alpha = 0.7f), modifier = Modifier
        .padding(vertical = 8.dp)
        .fillMaxWidth(), textAlign = TextAlign.Center)
}

@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp), contentAlignment = Alignment.Center) {
        Text("Loading...", color = TextWhite.copy(alpha = 0.7f))
    }
}

@Composable
fun EventItemCardSmall(
    event: Event,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(width = 150.dp, height = 200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val imageUrl = event.coverPhotoUrl
            if (isPreviewMode && imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(id = R.string.tour_screen_preview_image_small),
                        color = TextWhite.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = event.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 300f
                        )
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    event.title,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
fun SectionHeader(titleResId: Int, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = titleResId),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = TextWhite)
        )
        Text(
            text = stringResource(id = R.string.see_all),
            style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.8f)),
            modifier = Modifier.clickable(onClick = onSeeAllClick)
        )
    }
}

@Composable
fun TourItemCardSmall(
    tour: Tour,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(width = 150.dp, height = 200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.2f))

    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val imageUrl = tour.thumbnailUrl ?: tour.imagesUrl?.firstOrNull()
            if (isPreviewMode && imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(id = R.string.tour_screen_preview_image_small),
                        color = TextWhite.copy(alpha = 0.7f)
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = tour.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (tour.name.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 300f
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(tour.name, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun PopularTourCard(
    tour: Tour,
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
            val imageUrl = tour.thumbnailUrl ?: tour.imagesUrl?.firstOrNull()
            if (isPreviewMode && imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(id = R.string.tour_screen_preview_image_popular),
                        color = TextWhite.copy(alpha = 0.7f)
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = tour.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (tour.name.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.7f)),
                                startY = 300f
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(tour.name, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

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