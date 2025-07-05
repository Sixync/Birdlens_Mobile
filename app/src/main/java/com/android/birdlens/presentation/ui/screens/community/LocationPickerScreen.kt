package com.android.birdlens.presentation.ui.screens.community

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.TextWhite
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun LocationPickerScreen(navController: NavController) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 3f) // Default world view
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )

        // Center Marker Icon
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = "Selected Location",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .padding(bottom = 24.dp) // Adjust so the pin tip is at the center
        )

        // Top App Bar for navigation
        SimpleTopAppBar(
            title = "Select Sighting Location",
            onNavigateBack = { navController.popBackStack() }
        )

        // Confirm Button
        ExtendedFloatingActionButton(
            text = { Text("Confirm Location", color = TextWhite) },
            icon = { Icon(Icons.Default.Check, contentDescription = "Confirm", tint = TextWhite) },
            onClick = {
                val selectedLocation = cameraPositionState.position.target
                // Use the SavedStateHandle to pass the result back to the previous screen
                navController.previousBackStackEntry?.savedStateHandle?.set("picked_location", selectedLocation)
                navController.popBackStack()
            },
            containerColor = ButtonGreen,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}