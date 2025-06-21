// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/birdrangemap/BirdRangeMapScreen.kt
package com.android.birdlens.presentation.ui.screens.birdrangemap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.BirdRangeMapViewModel
import com.android.birdlens.presentation.viewmodel.BirdRangeUiState
import com.android.birdlens.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun BirdRangeMapScreen(
    navController: NavController,
    viewModel: BirdRangeMapViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 3f) // Default to world view
    }

    AppScaffold(
        navController = navController,
        topBar = { SimpleTopAppBar(title = "Species Distribution", onNavigateBack = { navController.popBackStack() }) },
        showBottomBar = false
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                if (uiState is BirdRangeUiState.Success) {
                    val rangeData = (uiState as BirdRangeUiState.Success).rangeData
                    rangeData.residentPolygons.forEach {
                        Polygon(points = it, fillColor = RangeResident, strokeColor = RangeResident.copy(alpha = 0.8f), strokeWidth = 2f)
                    }
                    rangeData.breedingPolygons.forEach {
                        Polygon(points = it, fillColor = RangeBreeding, strokeColor = RangeBreeding.copy(alpha = 0.8f), strokeWidth = 2f)
                    }
                    rangeData.nonBreedingPolygons.forEach {
                        Polygon(points = it, fillColor = RangeNonBreeding, strokeColor = RangeNonBreeding.copy(alpha = 0.8f), strokeWidth = 2f)
                    }
                    rangeData.passagePolygons.forEach {
                        Polygon(points = it, fillColor = RangePassage, strokeColor = RangePassage.copy(alpha = 0.8f), strokeWidth = 2f)
                    }
                }
            }

            // Legend Overlay
            MapLegend()

            if (uiState is BirdRangeUiState.Loading) {
                CircularProgressIndicator(color = TextWhite)
            } else if (uiState is BirdRangeUiState.Error) {
                Text(text = (uiState as BirdRangeUiState.Error).message, color = Color.Red)
            }
        }
    }
}

@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Legend", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
                LegendItem(color = RangeResident, label = "Resident")
                LegendItem(color = RangeBreeding, label = "Breeding Range")
                LegendItem(color = RangeNonBreeding, label = "Non-breeding Range")
                LegendItem(color = RangePassage, label = "Passage")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = TextWhite, fontSize = 12.sp)
    }
}

// Add these new colors to your Color.kt file
// in ui.theme package
/*
val RangeResident = Color(0x994CAF50) // Green
val RangeBreeding = Color(0x99FFC107) // Amber/Yellow
val RangeNonBreeding = Color(0x992196F3) // Blue
val RangePassage = Color(0x999C27B0) // Purple
*/