// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/allevents/AllEventsListScreen.kt
package com.android.birdlens.presentation.ui.screens.allevents

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.ui.screens.alltours.FullWidthTourItemCard // Reusing from AllTours
import com.android.birdlens.presentation.ui.screens.tour.TourItem // Reusing TourItem
import com.android.birdlens.ui.theme.*
import androidx.compose.foundation.clickable // Add this if not present

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllEventsListScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onEventItemClick: (Int) -> Unit,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    // Dummy data for "All Events"
    val allEventsList = List(8) { index ->
        TourItem(
            id = index + 200, // Unique IDs
            imageUrl = when (index % 3) {
                0 -> "https://plus.unsplash.com/premium_photo-1673283380436-ac702dc85c64?w=600&auto=format&fit=crop&q=60"
                1 -> "https://images.unsplash.com/photo-1559507984-555c777a7554?w=600&auto=format&fit=crop&q=60"
                else -> "https://images.unsplash.com/photo-1508007520041-4640673955de?w=600&auto=format&fit=crop&q=60"
            },
            title = "Special Event #${index + 1}"
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("All Events", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = GreenDeep
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val path1 = Path().apply { moveTo(0f, canvasHeight * 0.1f); cubicTo(canvasWidth * 0.2f, canvasHeight * 0.05f, canvasWidth * 0.3f, canvasHeight * 0.4f, canvasWidth * 0.6f, canvasHeight * 0.35f); cubicTo(canvasWidth * 0.9f, canvasHeight * 0.3f, canvasWidth * 1.1f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.7f); lineTo(0f, canvasHeight * 0.8f); close() }
                drawPath(path = path1, brush = Brush.radialGradient(listOf(GreenWave1.copy(alpha = 0.8f), GreenWave3.copy(alpha = 0.6f), GreenDeep.copy(alpha = 0.3f)), center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f), radius = canvasWidth * 0.8f))
                val path2 = Path().apply { moveTo(canvasWidth, canvasHeight * 0.5f); cubicTo(canvasWidth * 0.8f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.3f, canvasWidth * 0.4f, canvasHeight * 0.4f); cubicTo(canvasWidth * 0.1f, canvasHeight * 0.5f, canvasWidth * 0.0f, canvasHeight * 0.9f, canvasWidth * 0.3f, canvasHeight); lineTo(canvasWidth, canvasHeight); close() }
                drawPath(path = path2, brush = Brush.linearGradient(listOf(GreenWave4.copy(alpha = 0.4f), GreenWave1.copy(alpha = 0.3f), GreenDeep.copy(alpha = 0.1f)), start = Offset(canvasWidth * 0.8f, canvasHeight * 0.5f), end = Offset(canvasWidth * 0.3f, canvasHeight)))
            }

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
                    FullWidthTourItemCard( // Reusing the card from AllTours
                        item = eventItem,
                        onClick = { onEventItemClick(eventItem.id) },
                        isPreviewMode = isPreviewMode
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
        AllEventsListScreen(navController = rememberNavController(),
            onEventItemClick = {},
            isPreviewMode = true)
    }
}