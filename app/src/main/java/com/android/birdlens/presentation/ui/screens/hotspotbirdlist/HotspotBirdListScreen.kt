// Birdlens_Mobile/app/src/main/java/com/android/birdlens/presentation/ui/screens/hotspotbirdlist/HotspotBirdListScreen.kt
package com.android.birdlens.presentation.ui.screens.hotspotbirdlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// ... other imports
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
// Remove BirdHotspot and sampleBirdHotspots imports if they were from MapScreen previously
import com.android.birdlens.presentation.viewmodel.BirdSpeciesInfo
import com.android.birdlens.presentation.viewmodel.HotspotBirdListUiState
import com.android.birdlens.presentation.viewmodel.HotspotBirdListViewModel
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.TextWhite
import com.android.birdlens.ui.theme.DividerColor

@Composable
fun HotspotBirdListScreen(
    navController: NavController,
    hotspotId: String?, // This is the locId from eBird
    viewModel: HotspotBirdListViewModel = viewModel() // ViewModel will be automatically created with SavedStateHandle
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Check if hotspotId was passed correctly (ViewModel constructor handles null/blank from SavedStateHandle)
    // However, if navigation itself fails to pass it, viewModel creation might fail.
    // This check is more for graceful UI if something went wrong with nav args before VM init.
    if (hotspotId.isNullOrBlank()) {
        AppScaffold(
            navController = navController,
            topBar = { SimpleTopAppBar(title = "Error", onNavigateBack = { navController.popBackStack() })},
            showBottomBar = false
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Hotspot ID is missing.", color = TextWhite.copy(alpha = 0.8f))
            }
        }
        return
    }

    AppScaffold(
        navController = navController,
        topBar = {
            // Title could be dynamic if fetched, or generic like "Birds at Hotspot"
            SimpleTopAppBar(
                title = "Birds at Hotspot", // You might want to pass hotspotName via nav args too for a better title
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
            when (val state = uiState) {
                is HotspotBirdListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is HotspotBirdListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is HotspotBirdListUiState.Success -> {
                    if (state.birds.isEmpty()) {
                        Text(
                            text = "No recent bird observations found for this hotspot.",
                            color = TextWhite.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)
                        ) {
                            itemsIndexed(state.birds, key = { _, bird -> bird.speciesCode }) { index, birdInfo ->
                                BirdListItem(
                                    commonName = birdInfo.commonName,
                                    scientificName = birdInfo.scientificName,
                                    observationDetail = formatObservationDetail(birdInfo),
                                    onClick = {
                                        if (birdInfo.speciesCode.isNotBlank()) {
                                            navController.navigate(Screen.BirdInfo.createRoute(birdInfo.speciesCode))
                                        } else {
                                            Toast.makeText(context, "Species code not available for ${birdInfo.commonName}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                if (index < state.birds.lastIndex) {
                                    HorizontalDivider(color = DividerColor.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BirdListItem(
    commonName: String,
    scientificName: String,
    observationDetail: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
            Text(
                text = commonName,
                color = TextWhite,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = scientificName,
                color = TextWhite.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            if (observationDetail.isNotBlank()) {
                Text(
                    text = observationDetail,
                    color = TextWhite.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatObservationDetail(birdInfo: BirdSpeciesInfo): String {
    val details = mutableListOf<String>()
    birdInfo.observationDate?.let { details.add("Seen: $it") }
    birdInfo.count?.let { if (it > 0) details.add("Count: $it") }
    return details.joinToString("  |  ")
}


@Preview(showBackground = true, device = "spec:width=360dp,height=640dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_WithData() {
    // Previewing with actual ViewModel is complex due to SavedStateHandle and API calls.
    // For a simple UI preview, you can mock the state.
    val sampleBirds = listOf(
        BirdSpeciesInfo("House Sparrow", "houspa", "Passer domesticus", "2023-10-26", 5),
        BirdSpeciesInfo("American Robin", "amerob", "Turdus migratorius", "2023-10-25", 2)
    )
    BirdlensTheme {
        // Mocking a successful state for preview
        val mockUiState = HotspotBirdListUiState.Success(sampleBirds)
        // This screen normally gets its ViewModel via hiltViewModel or viewModel delegate.
        // For preview, we're showing the content part.

        AppScaffold(
            navController = rememberNavController(),
            topBar = { SimpleTopAppBar(title = "Birds at Hotspot", onNavigateBack = {}) },
            showBottomBar = false
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)
                ) {
                    itemsIndexed(sampleBirds, key = { _, bird -> bird.speciesCode }) { index, birdInfo ->
                        BirdListItem(
                            commonName = birdInfo.commonName,
                            scientificName = birdInfo.scientificName,
                            observationDetail = formatObservationDetail(birdInfo),
                            onClick = {}
                        )
                        if (index < sampleBirds.lastIndex) {
                            HorizontalDivider(color = DividerColor.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                }
            }
        }
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