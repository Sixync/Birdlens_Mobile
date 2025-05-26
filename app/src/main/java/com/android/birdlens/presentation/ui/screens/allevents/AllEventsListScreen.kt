// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/allevents/AllEventsListScreen.kt
package com.android.birdlens.presentation.ui.screens.allevents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.ui.components.AppScaffold // Import
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar // Import
import com.android.birdlens.presentation.ui.screens.alltours.FullWidthTourItemCard
import com.android.birdlens.presentation.ui.screens.tour.TourItem
import com.android.birdlens.ui.theme.*

@Composable
fun AllEventsListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onEventItemClick: (Int) -> Unit,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val allEventsList = List(8) { index ->
        TourItem(
            id = index + 200,
            imageUrl = when (index % 3) {
                0 -> "https://plus.unsplash.com/premium_photo-1673283380436-ac702dc85c64?w=600&auto=format&fit=crop&q=60"
                1 -> "https://images.unsplash.com/photo-1559507984-555c777a7554?w=600&auto=format&fit=crop&q=60"
                else -> "https://images.unsplash.com/photo-1508007520041-4640673955de?w=600&auto=format&fit=crop&q=60"
            },
            title = "Special Event #${index + 1}"
        )
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar( // Use the simple top app bar
                title = "All Events",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false // Decide if this screen needs a bottom bar
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = innerPadding.calculateTopPadding() + 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Result found (${allEventsList.size})", color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { /* TODO: Show sort options */ }) {
                        Text("Sort By", color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.FilterList, contentDescription = "Sort", tint = TextWhite.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(allEventsList, key = { it.id }) { eventItem ->
                FullWidthTourItemCard(
                    item = eventItem,
                    onClick = { onEventItemClick(eventItem.id) },
                    isPreviewMode = isPreviewMode
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AllEventsListScreenPreview() {
    BirdlensTheme {
        AllEventsListScreen(navController = rememberNavController(),
            onEventItemClick = {},
            isPreviewMode = true)
    }
}