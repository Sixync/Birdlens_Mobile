// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/alltours/AllToursListScreen.kt
package com.android.birdlens.presentation.ui.screens.alltours

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Ensure this is imported
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf // Added for rating example
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.data.model.Tour
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.TourUIState
import com.android.birdlens.presentation.viewmodel.TourViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun AllToursListScreen(
    navController: NavController,
    onTourItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    tourViewModel: TourViewModel = viewModel(),
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val toursState by tourViewModel.toursState.collectAsState()

    LaunchedEffect(Unit) {
        if (toursState is TourUIState.Idle || toursState is TourUIState.Error) {
            tourViewModel.fetchTours()
        }
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = stringResource(id = R.string.all_tours_title),
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = toursState) {
                is TourUIState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is TourUIState.Success -> {
                    val paginatedResponse = state.data
                    // FIX: Check for null items list OR empty list
                    if (paginatedResponse.items.isNullOrEmpty()) {
                        Text(
                            "No tours available at the moment.",
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp,
                                top = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = modifier.fillMaxSize()
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(id = R.string.results_found_count, paginatedResponse.totalCount.toInt()),
                                        color = TextWhite.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { /* TODO: Show sort options */ }
                                    ) {
                                        Text(
                                            stringResource(id = R.string.sort_by),
                                            color = TextWhite.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Filled.FilterList,
                                            contentDescription = stringResource(id = R.string.sort_icon_description),
                                            tint = TextWhite.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            items(paginatedResponse.items, key = { it.id }) { tour ->
                                FullWidthTourItemCardFromModel(
                                    tour = tour,
                                    onClick = { onTourItemClick(tour.id) },
                                    isPreviewMode = isPreviewMode
                                )
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
                            onClick = { tourViewModel.fetchTours() },
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
fun FullWidthTourItemCardFromModel(
    tour: Tour,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.1f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = tour.thumbnailUrl ?: tour.imagesUrl?.firstOrNull()
            val imagePainter = rememberAsyncImagePainter(model = imageUrl)

            if (isPreviewMode && imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.DarkGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(id = R.string.preview_image_placeholder),
                        color = TextWhite
                    )
                }
            } else {
                Image(
                    painter = imagePainter,
                    contentDescription = tour.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.7f)),
                            startY = 400f
                        )
                    )
            )

            IconButton(
                onClick = { /* TODO: Maybe show details or type */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = stringResource(id = R.string.tour_type_icon_description),
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    stringResource(id = R.string.free_rental_badge),
                    color = TextWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    tour.name,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(id = R.string.rating_star_description),
                            tint = Color.Yellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Icon(
                        Icons.Filled.StarHalf,
                        contentDescription = stringResource(id = R.string.rating_star_description),
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(id = R.string.tour_id_prefix, tour.id),
                        color = TextWhite.copy(alpha = 0.9f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AllToursListScreenPreview() {
    BirdlensTheme {
        AllToursListScreen(
            navController = rememberNavController(),
            onTourItemClick = {},
            isPreviewMode = true
        )
    }
}