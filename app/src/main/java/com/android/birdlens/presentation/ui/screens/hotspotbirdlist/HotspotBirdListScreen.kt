package com.android.birdlens.presentation.ui.screens.hotspotbirdlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.ui.screens.map.BirdHotspot // Adjust import if BirdHotspot is moved
import com.android.birdlens.presentation.ui.screens.map.sampleBirdHotspots // Adjust import if sampleBirdHotspots is moved
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.TextWhite
import com.android.birdlens.ui.theme.DividerColor


@Composable
fun HotspotBirdListScreen(
    navController: NavController,
    hotspotId: String?
) {
    var hotspot by remember { mutableStateOf<BirdHotspot?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(hotspotId) {
        if (hotspotId == null) {
            errorMsg = "Hotspot ID is missing."
            return@LaunchedEffect
        }
        // In a real app, this data would be fetched from a ViewModel/Repository
        val foundHotspot = sampleBirdHotspots.find { it.id == hotspotId }
        if (foundHotspot == null) {
            errorMsg = "Hotspot not found."
        } else {
            hotspot = foundHotspot
        }
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = hotspot?.name ?: "Birds at Location",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false // Typically, a list derived from another screen might not show main nav
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                errorMsg != null -> {
                    Text(
                        text = errorMsg!!,
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                hotspot == null -> { // Still loading or ID was null initially
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                hotspot!!.birdCommonNames.isEmpty() -> {
                    Text(
                        text = "No birds listed for this hotspot.",
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp) // Let divider handle spacing
                    ) {
                        itemsIndexed(hotspot!!.birdCommonNames) { index, commonName ->
                            val speciesCode = hotspot!!.birdSpeciesCodes.getOrNull(index)
                            BirdListItem(
                                commonName = commonName,
                                onClick = {
                                    if (speciesCode != null && speciesCode.isNotBlank()) {
                                        navController.navigate(Screen.BirdInfo.createRoute(speciesCode))
                                    } else {
                                        Toast.makeText(context, "Species code not available for $commonName", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            if (index < hotspot!!.birdCommonNames.lastIndex) {
                                HorizontalDivider(color = DividerColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BirdListItem(commonName: String, onClick: () -> Unit) {
    Surface( // Using Surface for consistent click effect and potential background styling
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent // Background is handled by AppScaffold/SharedAppBackground
    ) {
        Text(
            text = commonName,
            color = TextWhite,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp) // Padding inside the clickable area
        )
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_WithData() {
    // For preview, we need to mock a hotspotId that exists in sampleBirdHotspots
    val previewHotspotId = sampleBirdHotspots.firstOrNull()?.id
    BirdlensTheme {
        HotspotBirdListScreen(
            navController = rememberNavController(),
            hotspotId = previewHotspotId
        )
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_NoBirds() {
    // To preview "No birds", we'd need a sample hotspot with empty bird lists
    // or modify one. For simplicity, let's just use a non-existent ID for error.
    BirdlensTheme {
        HotspotBirdListScreen(
            navController = rememberNavController(),
            hotspotId = "non_existent_id" // This will trigger "Hotspot not found"
        )
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_Loading() {
    BirdlensTheme {
        HotspotBirdListScreen(
            navController = rememberNavController(),
            hotspotId = null // Simulates loading or initial state before ID is processed
        )
    }
}