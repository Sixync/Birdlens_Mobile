// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/alltours/AllToursListScreen.kt
package com.android.birdlens.presentation.ui.screens.alltours

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.ui.components.AppScaffold // Import
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar // Import
import com.android.birdlens.presentation.ui.screens.tour.TourItem // Reusing TourItem
import com.android.birdlens.ui.theme.*

@Composable
fun AllToursListScreen(
    navController: NavController,
    onTourItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    val allToursList = List(10) { index ->
        TourItem(
            id = index + 100,
            imageUrl = when (index % 4) {
                0 -> "https://images.unsplash.com/photo-1567020009789-972f00d0f1f0?w=600&auto=format&fit=crop&q=60"
                1 -> "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=600&auto=format&fit=crop&q=60"
                2 -> "https://images.unsplash.com/photo-1528181304800-259b08848526?w=600&auto=format&fit=crop&q=60"
                else -> "https://images.unsplash.com/photo-1483728642387-6c351bEC1d69?w=600&auto=format&fit=crop&q=60"
            },
            title = "Amazing Tour Destination #${index + 1}"
        )
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "All Tours",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false // Typically, list detail screens don't show main bottom nav
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp, // Use innerPadding for bottom
                top = innerPadding.calculateTopPadding() + 8.dp // Use innerPadding for top
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier.fillMaxSize() // modifier from parameter if specific needed
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Result found (${allToursList.size})", color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { /* TODO: Show sort options */ }) {
                        Text("Sort By", color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.FilterList, contentDescription = "Sort", tint = TextWhite.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(allToursList, key = { it.id }) { tourItem ->
                FullWidthTourItemCard(
                    item = tourItem,
                    onClick = { onTourItemClick(tourItem.id) },
                    isPreviewMode = isPreviewMode
                )
            }
        }
    }
}

@Composable
fun FullWidthTourItemCard(
    item: TourItem,
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
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.1f)) // Ensure cards have a background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPreviewMode) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.DarkGray.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Preview Image", color = TextWhite)
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageUrl),
                    contentDescription = item.title,
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
                Icon(Icons.Filled.CalendarToday, contentDescription = "Tour Type", tint = TextWhite, modifier = Modifier.size(20.dp))
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("FREE RENTAL", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    item.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) {
                        Icon(Icons.Filled.Star, contentDescription = "Rating Star", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tour #${item.id % 100}", color = TextWhite.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AllToursListScreenPreview() {
    BirdlensTheme {
        AllToursListScreen(navController = rememberNavController(),
            onTourItemClick = {},
            isPreviewMode = true)
    }
}