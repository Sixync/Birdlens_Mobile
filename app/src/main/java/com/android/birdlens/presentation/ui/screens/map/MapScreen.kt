// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/map/MapScreen.kt
package com.android.birdlens.presentation.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.R
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.map.components.CompareModeBottomBar
import com.android.birdlens.presentation.ui.screens.map.components.FloatingMapActionItem
import com.android.birdlens.presentation.ui.screens.map.components.FloatingMapButton
import com.android.birdlens.presentation.ui.screens.map.components.HomeCountrySelectionDialog
import com.android.birdlens.presentation.ui.screens.map.components.HotspotDetailsSheetContent
import com.android.birdlens.presentation.ui.screens.map.components.LoadingErrorUI
import com.android.birdlens.presentation.ui.screens.map.components.MapScreenHeader
import com.android.birdlens.presentation.ui.screens.map.components.PermissionRationaleUI
import com.android.birdlens.presentation.ui.screens.map.components.TutorialOverlay
import com.android.birdlens.presentation.ui.screens.map.components.TutorialStepInfo
import com.android.birdlens.presentation.viewmodel.MapUiState
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    var showMapError by remember { mutableStateOf(false) }
    val mapUiState by mapViewModel.uiState.collectAsState()
    val searchQuery by mapViewModel.searchQuery.collectAsState()
    val searchResults by mapViewModel.searchResults.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isCompareModeActive by mapViewModel.isCompareModeActive.collectAsState()
    val selectedHotspotsForComparison by mapViewModel.selectedHotspotsForComparison.collectAsState()

    var mapsSdkInitialized by remember { mutableStateOf(false) }

    val currentHomeCountrySetting by mapViewModel.currentHomeCountrySetting.collectAsState()
    val currentRadiusKm by mapViewModel.currentRadiusKm.collectAsState()
    var showHomeCountryDialog by remember { mutableStateOf(false) }

    // Logic: The bottom sheet state and its visibility controller are restored.
    val selectedHotspotDetails by mapViewModel.selectedHotspotDetails.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    val mapProperties by mapViewModel.mapProperties
    val uiSettings by mapViewModel.mapUiSettings
    val isHeatmapVisible by mapViewModel.isHeatmapVisible.collectAsState()
    val heatmapData by mapViewModel.heatmapData.collectAsState()
    val isMapLocked by mapViewModel.isMapLocked.collectAsState()
    val bookmarkedHotspotIds by mapViewModel.bookmarkedHotspotIds.collectAsState()

    // Tutorial states
    val showTutorial by mapViewModel.showTutorial.collectAsState()
    val tutorialStepIndex by mapViewModel.tutorialStepIndex.collectAsState()
    val elementCoordinates by mapViewModel.elementCoordinates.collectAsState()


    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context.applicationContext, MapsInitializer.Renderer.LATEST) { renderer ->
                Log.d("MapScreen", "Maps SDK Initialized with renderer: $renderer")
                mapsSdkInitialized = true
            }
        } catch (e: Exception) {
            Log.e("MapScreen", "Error during MapsInitializer.initialize", e)
            showMapError = true
            mapsSdkInitialized = false
        }
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapViewModel.initialMapCenter, mapViewModel.initialMapZoom)
    }

    LaunchedEffect(currentHomeCountrySetting) {
        coroutineScope.launch {
            Log.d("MapScreen", "Home country changed to ${currentHomeCountrySetting.name}. Animating map.")
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(currentHomeCountrySetting.center, currentHomeCountrySetting.zoom),
                1000
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted, mapsSdkInitialized, isMapLocked) {
        if (mapsSdkInitialized) {
            mapViewModel._mapProperties.value = mapViewModel._mapProperties.value.copy(isMyLocationEnabled = locationPermissionsState.allPermissionsGranted)
            mapViewModel._mapUiSettings.value = mapViewModel._mapUiSettings.value.copy(
                myLocationButtonEnabled = locationPermissionsState.allPermissionsGranted,
                scrollGesturesEnabled = !isMapLocked,
                zoomGesturesEnabled = !isMapLocked
            )
        }
    }

    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(cameraPositionState.isMoving, mapLoaded) {
        if (!cameraPositionState.isMoving && mapLoaded) {
            val currentTarget = cameraPositionState.position.target
            val currentZoom = cameraPositionState.position.zoom
            mapViewModel.requestHotspotsForCurrentView(currentTarget, currentZoom)
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    val radiusFilterKey = "radius_filter"

    val floatingActionItems = listOf(
        FloatingMapActionItem(
            icon = { Icon(if (isHeatmapVisible) Icons.Filled.Thermostat else Icons.Outlined.Thermostat, contentDescription = null, tint = TextWhite ) },
            contentDescriptionResId = if(isHeatmapVisible) R.string.map_action_hide_heatmap else R.string.map_action_show_heatmap,
            onClick = { mapViewModel.toggleHeatmap() },
            isSelected = isHeatmapVisible,
            key = "heatmap_toggle"
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Layers, contentDescription = null, tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_change_map_type,
            onClick = { mapViewModel.toggleMapType() }
        ),
        FloatingMapActionItem(
            icon = { Icon(if (isMapLocked) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = null, tint = TextWhite) },
            contentDescriptionResId = if(isMapLocked) R.string.map_action_unlock_map else R.string.map_action_lock_map,
            onClick = { mapViewModel.toggleMapLock() },
            isSelected = isMapLocked
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Public, contentDescription = null, tint = TextWhite) },
            contentDescriptionResId = R.string.settings_change_home_country,
            onClick = { showHomeCountryDialog = true },
            key = "country_selector"
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Refresh, contentDescription = null, tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_refresh_hotspots,
            onClick = {
                if (mapsSdkInitialized && mapLoaded) {
                    mapViewModel.requestHotspotsForCurrentView(cameraPositionState.position.target, cameraPositionState.position.zoom, forceRefresh = true)
                    Toast.makeText(context, context.getString(R.string.map_toast_refreshing_hotspots), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.map_toast_map_not_ready), Toast.LENGTH_SHORT).show()
                }
            }
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Compare, contentDescription = null, tint = if (isCompareModeActive) GreenWave2 else TextWhite) },
            contentDescriptionResId = if (isCompareModeActive) R.string.map_action_exit_compare else R.string.map_action_start_compare,
            onClick = { mapViewModel.toggleCompareMode() },
            isSelected = isCompareModeActive,
            badgeCount = if (isCompareModeActive && selectedHotspotsForComparison.isNotEmpty()) selectedHotspotsForComparison.size else null
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_identify_bird,
            onClick = { navController.navigate(Screen.BirdIdentifier.route) }
        )
    )

    AppScaffold(
        navController = navController,
        topBar = {
            MapScreenHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { mapViewModel.onSearchQueryChanged(it) },
                onSearchSubmit = {
                    keyboardController?.hide()
                    mapViewModel.onBirdSearchSubmitted()
                },
                onClearSearch = {
                    keyboardController?.hide()
                    mapViewModel.onSearchQueryCleared(cameraPositionState.position.target, cameraPositionState.position.zoom)
                },
                onNavigateBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() else { /* Handle no backstack */ } },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                onShowTutorial = { mapViewModel.startTutorial() },
                searchResults = searchResults,
                onSearchResultClick = { birdName ->
                    keyboardController?.hide()
                    mapViewModel.onSearchQueryChanged(birdName)
                    mapViewModel.onBirdSearchSubmitted()
                }
            )
        },
        showBottomBar = true,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 70.dp)
            ) {
                for (item in floatingActionItems) {
                    FloatingMapButton(
                        item = item,
                        modifier = item.key?.let { key ->
                            Modifier.onGloballyPositioned { coordinates ->
                                val rect = Rect(
                                    offset = coordinates.positionInWindow(),
                                    size = coordinates.size.toSize()
                                )
                                mapViewModel.registerUiElement(key, rect)
                            }
                        } ?: Modifier
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedHotspotDetails != null) {
            ModalBottomSheet(
                onDismissRequest = { mapViewModel.clearSelectedHotspot() },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                containerColor = CardBackground.copy(alpha = 0.95f),
            ) {
                selectedHotspotDetails?.let { details ->
                    HotspotDetailsSheetContent(
                        details = details,
                        onNavigateToFullDetails = {
                            navController.navigate(Screen.HotspotBirdList.createRoute(details.hotspot.locId))
                            coroutineScope.launch { sheetState.hide() }
                                .invokeOnCompletion { if (it == null) mapViewModel.clearSelectedHotspot() }
                        },
                        onBookmarkToggle = { mapViewModel.toggleBookmarkCurrentHotspot() }
                    )
                }
            }
        }

        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            if (!mapsSdkInitialized && !showMapError) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
            } else if (locationPermissionsState.allPermissionsGranted && mapsSdkInitialized) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    onMapLoaded = {
                        mapLoaded = true
                        showMapError = false
                        mapViewModel.requestHotspotsForCurrentView(cameraPositionState.position.target, cameraPositionState.position.zoom)
                    },
                    onPOIClick = { poi ->
                        Toast.makeText(context, context.getString(R.string.map_toast_poi_clicked, poi.name), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    val currentHotspots = (mapUiState as? MapUiState.Success)?.hotspots ?: emptyList()

                    if (isHeatmapVisible && heatmapData.isNotEmpty()) {
                        val heatmapTileProvider = remember(heatmapData) {
                            HeatmapTileProvider.Builder().weightedData(heatmapData).radius(50).opacity(0.7).build()
                        }
                        TileOverlay(tileProvider = heatmapTileProvider, visible = true, fadeIn = true)
                    } else if (!isHeatmapVisible && currentHotspots.isNotEmpty()) {
                        Clustering(
                            items = currentHotspots,
                            onClusterClick = { cluster ->
                                if (cluster != null) {
                                    val newZoom = cameraPositionState.position.zoom + 2
                                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(cluster.position, newZoom))
                                }
                                false
                            },
                            onClusterItemClick = { hotspot ->
                                mapViewModel.onHotspotMarkerClick(hotspot)
                                coroutineScope.launch { sheetState.expand() }
                                true
                            },
                            clusterContent = { cluster ->
                                if (cluster != null) {
                                    Surface(
                                        shape = CircleShape,
                                        color = ButtonGreen.copy(alpha = 0.8f),
                                        contentColor = TextWhite,
                                        border = BorderStroke(2.dp, TextWhite.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = cluster.size.toString(),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            clusterItemContent = { hotspot ->
                                val isSelectedForCompare = selectedHotspotsForComparison.any { it.locId == hotspot.locId }
                                val isBookmarked = bookmarkedHotspotIds.contains(hotspot.locId)

                                val pinDrawableRes = when {
                                    isCompareModeActive && isSelectedForCompare -> R.drawable.ic_custom_pin_selected
                                    isBookmarked -> R.drawable.ic_custom_pin_selected
                                    else -> R.drawable.ic_custom_pin
                                }
                                val pinTint = when {
                                    isCompareModeActive && isSelectedForCompare -> GreenWave2
                                    isBookmarked -> PurpleGrey80
                                    else -> Color.Unspecified
                                }
                                Icon(
                                    painter = painterResource(id = pinDrawableRes),
                                    contentDescription = hotspot.title,
                                    tint = pinTint,
                                    modifier = Modifier.onGloballyPositioned {
                                        // Register the first hotspot marker for the tutorial
                                        if (elementCoordinates["map_hotspot"] == null) {
                                            val rect = Rect(
                                                offset = it.positionInWindow(),
                                                size = it.size.toSize()
                                            )
                                            mapViewModel.registerUiElement("map_hotspot", rect)
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            } else if (!locationPermissionsState.allPermissionsGranted && mapsSdkInitialized) {
                PermissionRationaleUI(onGrantPermissions = { locationPermissionsState.launchMultiplePermissionRequest() }, showRationale = locationPermissionsState.shouldShowRationale)
            }

            // Map Status Overlay
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = CardBackground.copy(alpha = 0.8f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.map_status_mode, if (isHeatmapVisible) stringResource(R.string.map_mode_heatmap) else stringResource(R.string.map_mode_clusters)),
                        color = TextWhite, fontSize = 12.sp
                    )
                    Text("|", color = TextWhite.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(
                        stringResource(R.string.map_status_country, currentHomeCountrySetting.name),
                        color = TextWhite, fontSize = 12.sp
                    )
                    Text("|", color = TextWhite.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text(
                        stringResource(R.string.map_status_radius, currentRadiusKm),
                        color = TextWhite, fontSize = 12.sp
                    )
                }
            }

            // Radius Filter Chips
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isCompareModeActive && selectedHotspotsForComparison.isNotEmpty()) 100.dp else 16.dp)
                    .onGloballyPositioned { coordinates ->
                        val rect = Rect(
                            offset = coordinates.positionInWindow(),
                            size = coordinates.size.toSize()
                        )
                        mapViewModel.registerUiElement(radiusFilterKey, rect)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    mapViewModel.availableRadii.forEach { radius ->
                        val isSelected = radius == currentRadiusKm
                        FilterChip(
                            selected = isSelected,
                            enabled = true,
                            onClick = {
                                mapViewModel.setRadius(
                                    radius,
                                    cameraPositionState.position.target,
                                    cameraPositionState.position.zoom
                                )
                            },
                            label = { Text("${radius}km", color = if (isSelected) VeryDarkGreenBase else TextWhite) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CardBackground.copy(alpha = 0.7f),
                                selectedContainerColor = GreenWave2,
                                labelColor = TextWhite,
                                selectedLabelColor = VeryDarkGreenBase
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = GreenWave2.copy(alpha = 0.5f),
                                selectedBorderColor = Color.Transparent,
                                disabledBorderColor = GreenWave2.copy(alpha = 0.2f),
                                disabledSelectedBorderColor = Color.Transparent,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.5.dp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }


            AnimatedVisibility(
                visible = isCompareModeActive && selectedHotspotsForComparison.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CompareModeBottomBar(
                    selectedHotspots = selectedHotspotsForComparison,
                    onClearSelection = { mapViewModel.clearComparisonSelection() },
                    onCompareClick = {
                        val locIds = selectedHotspotsForComparison.map { it.locId }
                        if (locIds.size >= 2) {
                            navController.navigate(Screen.HotspotComparison.createRoute(locIds))
                            mapViewModel.toggleCompareMode()
                        } else {
                            Toast.makeText(context, context.getString(R.string.map_compare_toast_min_selection), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            LoadingErrorUI(mapUiState = mapUiState)

            if (showMapError) {
                Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.map_error_loading_detailed), color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                }
            }

            // --- TUTORIAL OVERLAY ---
            val currentTutorialKey = if (tutorialStepIndex < mapViewModel.tutorialSteps.size) mapViewModel.tutorialSteps[tutorialStepIndex].first else null
            val currentTutorialText = if (tutorialStepIndex < mapViewModel.tutorialSteps.size) mapViewModel.tutorialSteps[tutorialStepIndex].second else ""
            var currentRect by remember { mutableStateOf<Rect?>(null) } // Use remember for the Rect

            if (currentTutorialKey == "map_hotspot" && elementCoordinates[currentTutorialKey] == null) {
                val centerPoint = cameraPositionState.projection?.toScreenLocation(cameraPositionState.position.target)
                if (centerPoint != null) {
                    val density = LocalDensity.current
                    val markerHeightPx = with(density) { 48.dp.toPx() }
                    val markerWidthPx = with(density) { 36.dp.toPx() }
                    currentRect = Rect(
                        left = centerPoint.x - markerWidthPx / 2,
                        top = centerPoint.y - markerHeightPx,
                        right = centerPoint.x + markerWidthPx / 2,
                        bottom = centerPoint.y.toFloat()
                    )
                }
            } else {
                currentRect = elementCoordinates[currentTutorialKey]
            }

            val currentStep = if (currentTutorialKey != null) {
                TutorialStepInfo(
                    key = currentTutorialKey,
                    text = currentTutorialText,
                    targetRect = currentRect,
                    isCircleSpotlight = currentTutorialKey != radiusFilterKey
                )
            } else null

            TutorialOverlay(
                isVisible = showTutorial,
                currentStep = currentStep,
                totalSteps = mapViewModel.tutorialSteps.size,
                currentStepIndex = tutorialStepIndex,
                contentPadding = innerPadding,
                onNext = { mapViewModel.nextTutorialStep() },
                onPrevious = { mapViewModel.previousTutorialStep() },
                onSkip = { mapViewModel.skipTutorial() }
            )
        }


        if (showHomeCountryDialog) {
            HomeCountrySelectionDialog(
                currentHomeCountryCode = currentHomeCountrySetting.code,
                onDismiss = { showHomeCountryDialog = false },
                onCountrySelected = { countrySetting ->
                    mapViewModel.setHomeCountry(countrySetting)
                    showHomeCountryDialog = false
                }
            )
        }
    }
}