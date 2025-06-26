// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/map/MapScreen.kt
package com.android.birdlens.presentation.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward // Corrected import
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
// import androidx.compose.ui.res.colorResource // Not needed if using Color.kt
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R
import com.android.birdlens.data.CountrySetting
import com.android.birdlens.data.UserSettingsManager
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.BirdSpeciesInfo
import com.android.birdlens.presentation.viewmodel.HotspotSheetDetails
import com.android.birdlens.presentation.viewmodel.MapUiState
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.launch

fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    tintColor: Color? = null, // Changed to Compose Color for easier use
    sizeMultiplier: Float = 1.0f
): BitmapDescriptor? {
    return try {
        ContextCompat.getDrawable(context, vectorResId)?.let { vectorDrawable ->
            val mutatedDrawable = vectorDrawable.mutate()
            if (tintColor != null) {
                // Convert Compose Color to Android Color Int
                val androidColor = android.graphics.Color.argb(
                    (tintColor.alpha * 255).toInt(),
                    (tintColor.red * 255).toInt(),
                    (tintColor.green * 255).toInt(),
                    (tintColor.blue * 255).toInt()
                )
                DrawableCompat.setTint(mutatedDrawable, androidColor)
            }
            val originalWidth = mutatedDrawable.intrinsicWidth.takeIf { it > 0 } ?: 72
            val originalHeight = mutatedDrawable.intrinsicHeight.takeIf { it > 0 } ?: 72

            val width = (originalWidth * sizeMultiplier).toInt()
            val height = (originalHeight * sizeMultiplier).toInt()

            mutatedDrawable.setBounds(0, 0, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            mutatedDrawable.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    } catch (e: NullPointerException) {
        Log.e("MapScreen", "Error creating BitmapDescriptor: IBitmapDescriptorFactory not initialized or other NPE.", e)
        null
    } catch (e: Exception) {
        Log.e("MapScreen", "Generic error creating BitmapDescriptor.", e)
        null
    }
}


data class FloatingMapActionItem(
    val icon: @Composable () -> Unit,
    val contentDescriptionResId: Int,
    val onClick: () -> Unit,
    val badgeCount: Int? = null,
    val isSelected: Boolean = false,
    val key: String? = null
)

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
        // Logic: The ModalBottomSheet is re-introduced. Its visibility is controlled
        // by checking if the ViewModel's selectedHotspotDetails state is null or not.
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
                        // Logic: The main action button now navigates to the bird list screen.
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
                            // Logic: When a hotspot marker is clicked, the ViewModel fetches its details,
                            // and the coroutine expands the now-visible bottom sheet.
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

// Logic: This bottom sheet is now reinstated with one crucial change:
// the main action button now says "View Bird List" and its `onClick`
// lambda navigates to the HotspotBirdListScreen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotspotDetailsSheetContent(
    details: HotspotSheetDetails,
    onNavigateToFullDetails: () -> Unit,
    onBookmarkToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 200.dp)
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = details.hotspot.locName,
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Visibility, contentDescription = stringResource(R.string.map_sheet_recent_sightings), tint = TextWhite.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.map_sheet_recent_sightings) + ": ${details.recentSightingsCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite.copy(alpha = 0.8f)
            )
        }
        details.hotspot.numSpeciesAllTime?.let {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top=4.dp)) {
                Icon(Icons.Outlined.ListAlt, contentDescription = stringResource(R.string.map_sheet_total_species), tint = TextWhite.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.map_sheet_total_species) + ": $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite.copy(alpha = 0.8f)
                )
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        Text(
            stringResource(R.string.map_sheet_notable_bird),
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = details.notableBirdImageUrl,
                    placeholder = painterResource(id = R.drawable.ic_bird_placeholder),
                    error = painterResource(id = R.drawable.ic_bird_placeholder)
                ),
                contentDescription = "Notable bird",
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Example: Northern Cardinal",
                style = MaterialTheme.typography.bodyLarge,
                color = TextWhite
            )
        }

        if (details.speciesList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.map_sheet_species_list_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite
            )
            LazyRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(details.speciesList.take(5)) { species ->
                    AssistChip(
                        onClick = { /* TODO: Navigate to bird info for species.speciesCode */ },
                        label = { Text(species.commonName, color = TextWhite) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = ButtonGreen.copy(alpha = 0.7f)),
                        border = null
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = onBookmarkToggle,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenWave2),
                border = BorderStroke(1.dp, GreenWave2)
            ) {
                Icon(
                    if (details.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (details.isBookmarked) stringResource(R.string.map_action_unbookmark_hotspot) else stringResource(R.string.map_action_bookmark_hotspot)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (details.isBookmarked) stringResource(R.string.map_action_unbookmark_hotspot) else stringResource(R.string.map_action_bookmark_hotspot))
            }
            Button(
                onClick = onNavigateToFullDetails,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                // Logic: Changed the text of the button to reflect its new purpose.
                Text("View Bird List", color = TextWhite)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextWhite)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Composable
fun PermissionRationaleUI(onGrantPermissions: () -> Unit, showRationale: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.map_permission_required_message),
            color = TextWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onGrantPermissions,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
        ) {
            Text(stringResource(R.string.map_grant_permissions_button), color = TextWhite)
        }
        if (showRationale) {
            Text(
                stringResource(R.string.map_permission_denied_rationale),
                color = TextWhite.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun LoadingErrorUI(mapUiState: MapUiState) {
    val context = LocalContext.current
    if (mapUiState is MapUiState.Loading) {
        val loadingMessage = mapUiState.message
        if (!loadingMessage.isNullOrBlank()){
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = GreenWave2.copy(alpha = 0.7f), strokeWidth = 5.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = loadingMessage,
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextWhite)
            }
        }
    }

    (mapUiState as? MapUiState.Error)?.let { errorState ->
        LaunchedEffect(errorState.message) {
            Toast.makeText(context, errorState.message, Toast.LENGTH_LONG).show()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClearSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    onShowTutorial: () -> Unit,
    searchResults: List<BirdSpecies>,
    onSearchResultClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    stringResource(R.string.map_screen_title_birdlens).uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = GreenWave2, letterSpacing = 1.sp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextWhite)
                }
            },
            actions = {
                IconButton(onClick = onShowTutorial) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = stringResource(id = R.string.show_tutorial_content_description), tint = TextWhite)
                }
                IconButton(onClick = onNavigateToCart) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.icon_cart_description), tint = TextWhite, modifier = Modifier.size(28.dp))
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.map_search_placeholder), color = TextWhite.copy(alpha = 0.7f)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.map_search_icon_description), tint = TextWhite) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.map_search_clear_description), tint = TextWhite)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = SearchBarBackground,
                unfocusedContainerColor = SearchBarBackground,
                disabledContainerColor = SearchBarBackground,
                cursorColor = TextWhite,
                focusedBorderColor = GreenWave2,
                unfocusedBorderColor = Color.Transparent,
                focusedLeadingIconColor = TextWhite,
                unfocusedLeadingIconColor = TextWhite.copy(alpha = 0.7f),
                focusedTrailingIconColor = TextWhite,
                unfocusedTrailingIconColor = TextWhite.copy(alpha = 0.7f),
                focusedPlaceholderColor = TextWhite.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = TextWhite.copy(alpha = 0.7f)
            )
        )

        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(searchResults, key = { it.speciesCode }) { bird ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchResultClick(bird.commonName) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = TextWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(text = bird.commonName, color = TextWhite)
                                Text(
                                    text = bird.scientificName,
                                    color = TextWhite.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingMapButton(item: FloatingMapActionItem, modifier: Modifier = Modifier) {
    val contentDesc = stringResource(id = item.contentDescriptionResId)
    BadgedBox(
        badge = {
            if (item.badgeCount != null) {
                Badge(
                    containerColor = ActionButtonLightGray,
                    contentColor = ActionButtonTextDark,
                    modifier = Modifier.offset(x = (-6).dp, y = 6.dp)
                ) {
                    Text(item.badgeCount.toString())
                }
            }
        },
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (item.isSelected) GreenWave2.copy(alpha = 0.9f) else ButtonGreen.copy(alpha = 0.9f))
            .clickable(onClick = item.onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            item.icon()
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareModeBottomBar(
    selectedHotspots: List<EbirdNearbyHotspot>,
    onClearSelection: () -> Unit,
    onCompareClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground.copy(alpha = 0.95f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.map_compare_selected_info, selectedHotspots.size, MapViewModel.MAX_COMPARISON_ITEMS),
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onClearSelection) {
                    Text(stringResource(R.string.map_compare_clear_selection), color = GreenWave2)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedHotspots, key = { it.locId }) { hotspot ->
                    SuggestionChip(
                        onClick = { /* Optional: allow deselecting by tapping chip */ },
                        label = { Text(hotspot.locName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = ButtonGreen.copy(alpha = 0.7f),
                            labelColor = TextWhite
                        ),
                        border = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCompareClick,
                enabled = selectedHotspots.size >= 2,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text(stringResource(R.string.map_compare_button_text, selectedHotspots.size), color = TextWhite)
            }
        }
    }
}

@Composable
fun HomeCountrySelectionDialog(
    currentHomeCountryCode: String,
    onDismiss: () -> Unit,
    onCountrySelected: (CountrySetting) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = AuthCardBackground) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.dialog_select_home_country),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(modifier = Modifier.heightIn(max=300.dp)){
                    items(UserSettingsManager.PREDEFINED_COUNTRIES) { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country); onDismiss() }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (country.code == currentHomeCountryCode),
                                onClick = { onCountrySelected(country); onDismiss() },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = GreenWave2,
                                    unselectedColor = TextWhite.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(country.name, color = TextWhite, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(android.R.string.cancel).uppercase(), color = GreenWave2)
                }
            }
        }
    }
}