// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/allevents/AllEventsListScreen.kt
package com.android.birdlens.presentation.ui.screens.allevents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Ensure this is imported
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.R
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.ui.screens.alltours.FullWidthTourItemCardFromModel
import com.android.birdlens.presentation.viewmodel.TourUIState
import com.android.birdlens.presentation.viewmodel.TourViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun AllEventsListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onEventItemClick: (Long) -> Unit,
    tourViewModel: TourViewModel = viewModel(),
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val toursAsEventsState by tourViewModel.toursState.collectAsState()

    LaunchedEffect(Unit) {
        if (toursAsEventsState is TourUIState.Idle || toursAsEventsState is TourUIState.Error) {
            tourViewModel.fetchTours()
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
            when (val state = toursAsEventsState) {
                is TourUIState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is TourUIState.Success -> {
                    val paginatedResponse = state.data
                    // FIX: Check for null items list OR empty list
                    if (paginatedResponse.items.isNullOrEmpty()) {
                        Text(
                            "No events (via tours) available at the moment.",
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
                            items(paginatedResponse.items, key = { "event_tour_${it.id}" }) { tourItem ->
                                FullWidthTourItemCardFromModel(
                                    tour = tourItem,
                                    onClick = { onEventItemClick(tourItem.id) },
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
                        Text("Error fetching events: ${state.message}", color = TextWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
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

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AllEventsListScreenPreview() {
    BirdlensTheme {
        AllEventsListScreen(
            navController = rememberNavController(),
            onEventItemClick = {},
            isPreviewMode = true
        )
    }
}