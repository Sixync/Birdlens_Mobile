// app/src/main/java/com/android/birdlens/presentation/ui/screens/hotspotbirdlist/HotspotBirdListScreen.kt
package com.android.birdlens.presentation.ui.screens.hotspotbirdlist

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.BirdSpeciesInfo
import com.android.birdlens.presentation.viewmodel.HotspotBirdListUiState
import com.android.birdlens.presentation.viewmodel.HotspotBirdListViewModel
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopAppBarWithInfoAction(
    title: String,
    onNavigateBack: () -> Unit,
    onInfoClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text(title, color = TextWhite, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
        },
        actions = {
            IconButton(onClick = onInfoClick) {
                Icon(Icons.Default.Info, contentDescription = "View Hotspot Info", tint = TextWhite)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun HotspotBirdListScreen(
    navController: NavController,
    viewModel: HotspotBirdListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val effectiveHotspotId = viewModel.initialLocId ?: ""

    if (effectiveHotspotId.isBlank()) {
        AppScaffold(
            navController = navController,
            topBar = { com.android.birdlens.presentation.ui.components.SimpleTopAppBar(title = stringResource(R.string.error_title), onNavigateBack = { navController.popBackStack() })},
            showBottomBar = false
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.error_hotspot_id_missing), color = TextWhite.copy(alpha = 0.8f))
            }
        }
        return
    }

    LaunchedEffect(listState, uiState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .debounce(200)
            .collect { visibleItems ->
                if (visibleItems.isNotEmpty()) {
                    val lastVisibleItemIndex = visibleItems.last().index
                    val currentState = uiState
                    if (currentState is HotspotBirdListUiState.Success &&
                        currentState.canLoadMore &&
                        !currentState.isLoadingMore &&
                        lastVisibleItemIndex >= currentState.birds.size - 5
                    ) {
                        Log.d("HotspotBirdListScreen", "Reached end of list, loading more birds.")
                        viewModel.loadMoreBirdDetails()
                    }
                }
            }
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBarWithInfoAction(
                title = stringResource(R.string.hotspot_birds_title),
                onNavigateBack = { navController.popBackStack() },
                onInfoClick = {
                    if (effectiveHotspotId.isNotBlank()) {
                        Log.d("HotspotBirdListScreen", "Navigating to HotspotDetail with locId: $effectiveHotspotId")
                        navController.navigate(Screen.HotspotDetail.createRoute(effectiveHotspotId))
                    } else {
                        Log.e("HotspotBirdListScreen", "Attempted to navigate to HotspotDetail, but effectiveHotspotId was blank!")
                        Toast.makeText(context, "Cannot show details, Hotspot ID is missing.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        },
        showBottomBar = false
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VeryDarkGreenBase)
        ) {
            when (val state = uiState) {
                is HotspotBirdListUiState.Idle, is HotspotBirdListUiState.Loading -> {
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
                    if (state.birds.isEmpty() && !state.isLoadingMore) {
                        Text(
                            text = stringResource(R.string.hotspot_no_birds_found, effectiveHotspotId),
                            color = TextWhite.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
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
                                        color = DividerColor.copy(alpha = 0.2f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = TextWhite.copy(alpha = 0.7f))
                                    }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = birdInfo.imageUrl,
                    placeholder = painterResource(id = R.drawable.bg_placeholder_image),
                    error = painterResource(id = R.drawable.bg_placeholder_image)
                ),
                contentDescription = birdInfo.commonName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = birdInfo.commonName,
                    color = TextWhite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
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
                            color = GreenWave3,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
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

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=740dp,dpi=480")
@Composable
fun HotspotBirdListScreenPreview_WithData() {
    val sampleBirds = listOf(
        BirdSpeciesInfo("House Sparrow", "houspa", "Passer domesticus", "2023-10-26", 5, true, imageUrl = "https://images.unsplash.com/photo-1518992028580-6d57bd80f2dd?w=100"),
        BirdSpeciesInfo("American Robin", "amerob", "Turdus migratorius", "2023-10-25", 2, true, imageUrl = "https://images.unsplash.com/photo-1506220926969-69611510758a?w=100"),
        BirdSpeciesInfo("Blue Jay", "blujay", "Cyanocitta cristata", null, null, false),
        BirdSpeciesInfo("Northern Cardinal", "norcar", "Cardinalis cardinalis", "2023-09-15", 1, true, imageUrl = "https://images.unsplash.com/photo-1604272058880-347a08c71049?w=100")
    )
    class MockHotspotBirdListViewModel : HotspotBirdListViewModel(SavedStateHandle(mapOf("hotspotId" to "L_PREVIEW"))) {
        init {
            (super._uiState as MutableStateFlow<HotspotBirdListUiState>).value = HotspotBirdListUiState.Success(sampleBirds, canLoadMore = true)
        }
    }

    BirdlensTheme {
        HotspotBirdListScreen(
            navController = rememberNavController(),
            viewModel = MockHotspotBirdListViewModel()
        )
    }
}