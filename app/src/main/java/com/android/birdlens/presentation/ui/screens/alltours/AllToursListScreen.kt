// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/alltours/AllToursListScreen.kt
package com.android.birdlens.presentation.ui.screens.alltours

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.android.birdlens.presentation.ui.screens.tour.TourItem // Reusing TourItem
import com.android.birdlens.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllToursListScreen(
    navController: NavController,
    onTourItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    // Dummy data for "All Tours"
    val allToursList = List(10) { index ->
        TourItem(
            id = index + 100, // Unique IDs
            imageUrl = when (index % 4) {
                0 -> "https://images.unsplash.com/photo-1567020009789-972f00d0f1f0?w=600&auto=format&fit=crop&q=60" // Hoi An
                1 -> "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=600&auto=format&fit=crop&q=60" // Beach
                2 -> "https://images.unsplash.com/photo-1528181304800-259b08848526?w=600&auto=format&fit=crop&q=60" // Ha Long
                else -> "https://images.unsplash.com/photo-1483728642387-6c351bEC1d69?w=600&auto=format&fit=crop&q=60" // Mountain
            },
            title = "Amazing Tour Destination #${index + 1}"
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("All Tours", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent // Make TopAppBar transparent
                )
            )
        },
        containerColor = GreenDeep // Base background color for the screen
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) { // Box to hold background and content
            // Background Canvas (same as TourScreen, consider extracting)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                // drawRect(color = GreenDeep) // Already set by Scaffold containerColor
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
                    // You can add Sort By / Filter options here if needed, similar to design
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
}

@Composable
fun FullWidthTourItemCard(
    item: TourItem,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp), // More rounded
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp) // Taller card
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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

            // Overlay for text and icons
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.7f)),
                            startY = 400f // Adjust gradient start
                        )
                    )
            )

            // Top right icon (e.g., calendar or type)
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


            // "FREE RENTAL" badge (Simplified)
            // For the exact starburst shape, you'd need custom drawing or an SVG.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp)) // Simple badge
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
                    Text("Tour #${item.id % 100}", color = TextWhite.copy(alpha = 0.9f), fontSize = 14.sp) // Example
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