// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/allevents/AllEventsListScreen.kt
package com.android.birdlens.presentation.ui.screens.allevents

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
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
import com.android.birdlens.data.model.Event // Using the existing Event model
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.EventViewModel // New ViewModel
import com.android.birdlens.presentation.viewmodel.EventUIState // New UI State
import com.android.birdlens.ui.theme.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AllEventsListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onEventItemClick: (Long) -> Unit, // Keeps the same signature, though destination might change
    eventViewModel: EventViewModel = viewModel(), // Use EventViewModel
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val eventsDataState by eventViewModel.eventsState.collectAsState()

    LaunchedEffect(Unit) {
        // Fetch events if state is Idle or Error, or if you want to refresh
        if (eventsDataState is EventUIState.Idle || eventsDataState is EventUIState.Error) {
            eventViewModel.fetchEvents()
        }
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = stringResource(id = R.string.all_events_title),
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
            when (val state = eventsDataState) {
                is EventUIState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is EventUIState.Success -> {
                    val paginatedResponse = state.data
                    if (paginatedResponse.items.isNullOrEmpty()) {
                        Text(
                            "No events available at the moment.",
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
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
                            items(paginatedResponse.items, key = { "event_${it.id}" }) { eventItem ->
                                FullWidthEventItemCard( // New Composable for Event items
                                    event = eventItem,
                                    onClick = { onEventItemClick(eventItem.id) },
                                    isPreviewMode = isPreviewMode
                                )
                            }
                            // TODO: Add pagination loading indicator and logic if needed
                        }
                    }
                }
                is EventUIState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error fetching events: ${state.message}", color = TextWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { eventViewModel.fetchEvents() },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) {
                            Text(stringResource(R.string.retry), color = TextWhite)
                        }
                    }
                }
                is EventUIState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading), color = TextWhite.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

// Helper function to format date strings
fun formatEventDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Date N/A"
    return try {
        val odt = OffsetDateTime.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        odt.format(formatter)
    } catch (e: Exception) {
        dateString // fallback to original if parsing fails
    }
}


@Composable
fun FullWidthEventItemCard(
    event: Event,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            // .height(220.dp) // Let height be dynamic or set a fixed one
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.1f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // Height for the image part
            ) {
                val imageUrl = event.coverPhotoUrl
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
                        contentDescription = event.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box( // Gradient overlay for text readability
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.7f)),
                                startY = 300f // Adjust gradient start
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    event.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    event.description,
                    color = TextWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = "Start Date",
                        tint = TextWhite.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Starts: ${formatEventDate(event.startDate)}",
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CalendarToday, // Could use a different icon for end date
                        contentDescription = "End Date",
                        tint = TextWhite.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Ends: ${formatEventDate(event.endDate)}",
                        color = TextWhite.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AllEventsListScreenPreview() {
    BirdlensTheme {
        // Mock EventViewModel for preview
        val mockEventViewModel: EventViewModel = viewModel()
        // You could set up mockEventViewModel._eventsState.value here if needed for previewing different states
        AllEventsListScreen(
            navController = rememberNavController(),
            onEventItemClick = {},
            eventViewModel = mockEventViewModel,
            isPreviewMode = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FullWidthEventItemCardPreview() {
    BirdlensTheme {
        val sampleEvent = Event(
            id = 1,
            title = "Spring Migration Festival",
            description = "Celebrate the return of migratory birds with guided walks, workshops, and family activities. A great event for all ages!",
            coverPhotoUrl = "https://example.com/events/spring_migration.jpg",
            startDate = "2025-04-15T09:00:00Z",
            endDate = "2025-04-17T17:00:00Z",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = null
        )
        Box(modifier = Modifier.background(VeryDarkGreenBase).padding(16.dp)) {
            FullWidthEventItemCard(event = sampleEvent, onClick = { }, isPreviewMode = false)
        }
    }
}