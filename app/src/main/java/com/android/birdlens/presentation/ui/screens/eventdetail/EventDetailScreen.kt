// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/eventdetail/EventDetailScreen.kt
package com.android.birdlens.presentation.ui.screens.eventdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.lifecycle.ViewModelProvider // No longer needed here
// import androidx.lifecycle.viewmodel.compose.viewModel // No longer needed here
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.birdlens.R
import com.android.birdlens.data.model.Event
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.allevents.formatEventDate
import com.android.birdlens.presentation.ui.screens.tourdetail.DetailScreenHeader
import com.android.birdlens.presentation.viewmodel.EventDetailViewModel
// import com.android.birdlens.presentation.viewmodel.EventDetailViewModelFactory // No longer needed here
import com.android.birdlens.presentation.viewmodel.EventUIState
import com.android.birdlens.ui.theme.*
import androidx.compose.ui.graphics.vector.ImageVector
// import androidx.lifecycle.SavedStateHandle // No longer needed here


@Composable
fun EventDetailScreen(
    navController: NavController,
    eventId: Long,
    eventDetailViewModel: EventDetailViewModel // Accept ViewModel as a parameter
) {
    val eventState by eventDetailViewModel.eventDetailState.collectAsState()

    LaunchedEffect(eventId) {
        if (eventId != -1L) {
            // The ViewModel's init block should handle fetching if eventId is available from SavedStateHandle.
            // This call ensures fetching if eventId is passed directly and VM might have been created earlier
            // or if you want to explicitly re-fetch when eventId argument changes.
            eventDetailViewModel.fetchEventDetails(eventId)
        }
    }

    AppScaffold(
        navController = navController,
        topBar = { DetailScreenHeader(navController = navController) },
        showBottomBar = true // Assuming event details can be a main navigation item or accessed from one
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = eventState) {
                is EventUIState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is EventUIState.Success -> {
                    EventContent(event = state.data)
                }
                is EventUIState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}", color = TextWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { if(eventId != -1L) eventDetailViewModel.fetchEventDetails(eventId) },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) {
                            Text(stringResource(R.string.retry), color = TextWhite)
                        }
                    }
                }
                is EventUIState.Idle -> {
                    // Show a loading indicator or placeholder if in Idle state and eventId is valid
                    if (eventId != -1L) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                    } else {
                        Text("Event ID not available.", color = TextWhite.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

// ... EventContent and DetailInfoRow Composables remain the same ...
@Composable
fun EventContent(event: Event) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        item {
            val imagePainter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(event.coverPhotoUrl ?: R.drawable.ic_launcher_background) // Placeholder if URL is null
                    .crossfade(true)
                    .build()
            )
            Image(
                painter = imagePainter,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                DetailInfoRow(icon = Icons.Filled.CalendarToday, label = "Starts:", value = formatEventDate(event.startDate))
                DetailInfoRow(icon = Icons.Filled.CalendarToday, label = "Ends:", value = formatEventDate(event.endDate))

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "About this Event",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextWhite.copy(alpha = 0.85f),
                        lineHeight = 22.sp
                    )
                )
                // You can add more details or actions related to the event here
                // e.g., a button to find associated tours, share event, etc.
            }
        }
    }
}

@Composable
fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = GreenWave2, // Or another appropriate color
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.9f))
        )
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun EventDetailScreenPreview() {
    val dummyEvent = Event(
        id = 1L,
        title = "Annual Bird Watching Festival",
        description = "Join us for the annual bird watching festival! A great opportunity to see rare birds and learn from experts. Activities include guided tours, workshops, and photography contests.",
        coverPhotoUrl = "https://images.unsplash.com/photo-1506220926969-69611510758a?w=800&auto=format&fit=crop&q=60",
        startDate = "2024-08-15T09:00:00Z",
        endDate = "2024-08-17T17:00:00Z",
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = null
    )
    // For preview, we'd ideally mock the ViewModel or pass a dummy one.
    // Since EventDetailViewModel requires Application and SavedStateHandle,
    // direct preview is a bit complex. This preview shows the content part.
    BirdlensTheme {
        AppScaffold(navController = rememberNavController(), topBar = { DetailScreenHeader(rememberNavController()) }) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                EventContent(event = dummyEvent)
            }
        }
    }
}