// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/hotspotbirdlist/HotspotBirdListScreen.kt
package com.android.birdlens.presentation.ui.screens.hotspotbirdlist

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.R
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.BirdSpeciesInfo
import com.android.birdlens.presentation.viewmodel.HotspotBirdListUiState
import com.android.birdlens.presentation.viewmodel.HotspotBirdListViewModel
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun HotspotBirdListScreen(
    navController: NavController,
    hotspotId: String?, // This is the locId from eBird
    viewModel: HotspotBirdListViewModel // ViewModel will be automatically created with SavedStateHandle
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (hotspotId.isNullOrBlank() && viewModel.initialLocIdFromStateHandle.isNullOrBlank()) { // Check both direct param and state handle
        AppScaffold(
            navController = navController,
            topBar = { SimpleTopAppBar(title = stringResource(R.string.error_title), onNavigateBack = { navController.popBackStack() })},
            showBottomBar = false // Or as per your app's flow
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.error_hotspot_id_missing), color = TextWhite.copy(alpha = 0.8f))
            }
        }
        return
    }

    val effectiveHotspotId = hotspotId ?: viewModel.initialLocIdFromStateHandle ?: ""


    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = stringResource(R.string.hotspot_birds_title),
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false // Or true, depending on nav structure
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VeryDarkGreenBase) // Consistent background
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
                            text = stringResource(R.string.hotspot_no_birds_found, effectiveHotspotId),
                            color = TextWhite.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp) // Adjusted padding
                        ) {
                            itemsIndexed(state.birds, key = { _, bird -> bird.speciesCode }) { index, birdInfo ->
                                BirdListItem(
                                    birdInfo = birdInfo,
                                    onClick = {
                                        if (birdInfo.speciesCode.isNotBlank()) {
                                            navController.navigate(Screen.BirdInfo.createRoute(birdInfo.speciesCode))
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.error_species_code_unavailable, birdInfo.commonName), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                if (index < state.birds.lastIndex) {
                                    HorizontalDivider(
                                        color = DividerColor.copy(alpha = 0.2f), // Slightly more subtle divider
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp) // Match item padding
                                    )
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
    birdInfo: BirdSpeciesInfo,
    onClick: () -> Unit
) {
    val recencyColor = if (birdInfo.isRecent) GreenWave2 else TextWhite.copy(alpha = 0.6f)
    val recencyText = if (birdInfo.isRecent) stringResource(R.string.bird_status_recent) else stringResource(R.string.bird_status_historical)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp), // Added horizontal padding for consistency
        color = Color.Transparent // Items should be transparent to show LazyColumn background
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp), // Matched horizontal with above
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional: Recency Indicator (e.g., a colored dot)
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(recencyColor)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = birdInfo.commonName,
                    color = TextWhite, // Main text always white
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold // Slightly bolder for common name
                )
                Text(
                    text = birdInfo.scientificName,
                    color = TextWhite.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
                if (birdInfo.isRecent && (birdInfo.observationDate != null || birdInfo.count != null)) {
                    val observationDetails = mutableListOf<String>()
                    birdInfo.observationDate?.let { observationDetails.add(stringResource(R.string.bird_seen_on, it)) }
                    birdInfo.count?.let { if (it > 0) observationDetails.add(stringResource(R.string.bird_count, it)) }

                    if (observationDetails.isNotEmpty()) {
                        Text(
                            text = observationDetails.joinToString("  |  "),
                            color = GreenWave3, // Highlight recent details
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            // Display "Recent" / "Historical" tag
            Text(
                text = recencyText,
                color = recencyColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// Expose initialLocId for preview or other checks if needed by the screen directly
val HotspotBirdListViewModel.initialLocIdFromStateHandle: String?
    get() = this.javaClass.getDeclaredField("initialLocId").let {
        it.isAccessible = true
        it.get(this) as? String
    }


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=740dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_WithData() {
    val sampleBirds = listOf(
        BirdSpeciesInfo("House Sparrow", "houspa", "Passer domesticus", "2023-10-26", 5, true),
        BirdSpeciesInfo("American Robin", "amerob", "Turdus migratorius", "2023-10-25", 2, true),
        BirdSpeciesInfo("Blue Jay", "blujay", "Cyanocitta cristata", null, null, false),
        BirdSpeciesInfo("Northern Cardinal", "norcar", "Cardinalis cardinalis", "2023-09-15", 1, true)
    )
    // Mock ViewModel for preview
    class MockHotspotBirdListViewModel : HotspotBirdListViewModel(SavedStateHandle(mapOf("hotspotId" to "L_PREVIEW"))) {
        init {
            (super._uiState as MutableStateFlow<HotspotBirdListUiState>).value = HotspotBirdListUiState.Success(sampleBirds)
        }
    }

    BirdlensTheme {
        HotspotBirdListScreen(
            navController = rememberNavController(),
            hotspotId = "L_PREVIEW",
            viewModel = MockHotspotBirdListViewModel()
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=740dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_NoBirds() {
    class MockHotspotBirdListViewModel : HotspotBirdListViewModel(SavedStateHandle(mapOf("hotspotId" to "L_EMPTY"))) {
        init {
            (super._uiState as MutableStateFlow<HotspotBirdListUiState>).value = HotspotBirdListUiState.Success(emptyList())
        }
    }
    BirdlensTheme {
        HotspotBirdListScreen(
            navController = rememberNavController(),
            hotspotId = "L_EMPTY",
            viewModel = MockHotspotBirdListViewModel()
        )
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=740dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_Error() {
    class MockHotspotBirdListViewModel : HotspotBirdListViewModel(SavedStateHandle(mapOf("hotspotId" to "L_ERROR"))) {
        init {
            (super._uiState as MutableStateFlow<HotspotBirdListUiState>).value = HotspotBirdListUiState.Error("Failed to load bird data due to network issue.")
        }
    }
    BirdlensTheme {
        HotspotBirdListScreen(
            navController = rememberNavController(),
            hotspotId = "L_ERROR",
            viewModel = MockHotspotBirdListViewModel()
        )
    }
}