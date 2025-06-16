// app/src/main/java/com/android/birdlens/presentation/ui/screens/map/MapScreen.kt
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
import androidx.compose.foundation.BorderStroke // Import for BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.R
import com.android.birdlens.data.local.BirdSpecies
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.MapUiState
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.maps.android.compose.Marker // Ensure this is the compose Marker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// bitmapDescriptorFromVector function remains the same
fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    tintColor: Int? = null
): BitmapDescriptor? {
    return try {
        ContextCompat.getDrawable(context, vectorResId)?.let { vectorDrawable ->
            val mutatedDrawable = vectorDrawable.mutate()
            if (tintColor != null) {
                DrawableCompat.setTint(mutatedDrawable, tintColor)
            }
            val width = mutatedDrawable.intrinsicWidth.takeIf { it > 0 } ?: 72
            val height = mutatedDrawable.intrinsicHeight.takeIf { it > 0 } ?: 72
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
    val isSelected: Boolean = false
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

    var customHotspotIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var selectedHotspotIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var mapsSdkInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context.applicationContext, MapsInitializer.Renderer.LATEST) { renderer ->
                Log.d("MapScreen", "Maps SDK Initialized with renderer: $renderer")
                customHotspotIcon = bitmapDescriptorFromVector(context, R.drawable.ic_map_pin)
                selectedHotspotIcon = bitmapDescriptorFromVector(
                    context,
                    R.drawable.ic_map_pin,
                    ContextCompat.getColor(context, R.color.purple_500) // Make sure R.color.purple_500 exists
                )
                if (customHotspotIcon == null) Log.e("MapScreen", "Failed to create customHotspotIcon.")
                if (selectedHotspotIcon == null) Log.e("MapScreen", "Failed to create selectedHotspotIcon.")
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

    var mapProperties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL, latLngBoundsForCameraTarget = null))
    }
    var uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = true, compassEnabled = true))
    }

    val initialCameraPosition = CameraPosition.fromLatLngZoom(
        MapViewModel.VIETNAM_INITIAL_CENTER,
        MapViewModel.HOME_BUTTON_ZOOM_LEVEL
    )
    val cameraPositionState = rememberCameraPositionState {
        position = initialCameraPosition
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted, mapsSdkInitialized) {
        if (mapsSdkInitialized) {
            mapProperties = mapProperties.copy(isMyLocationEnabled = locationPermissionsState.allPermissionsGranted)
            uiSettings = uiSettings.copy(myLocationButtonEnabled = locationPermissionsState.allPermissionsGranted)
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

    val floatingActionItems = listOf(
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.map_action_home), tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_home,
            onClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(MapViewModel.VIETNAM_INITIAL_CENTER, MapViewModel.HOME_BUTTON_ZOOM_LEVEL), 1000)
                }
            }
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.map_action_refresh_hotspots), tint = TextWhite) },
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
            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.map_action_identify_bird), tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_identify_bird,
            onClick = { navController.navigate(Screen.BirdIdentifier.route) }
        ),
        FloatingMapActionItem(
            icon = { Icon(if (isCompareModeActive) Icons.Filled.Compare else Icons.Outlined.Compare, contentDescription = stringResource(if (isCompareModeActive) R.string.map_action_exit_compare else R.string.map_action_start_compare), tint = if (isCompareModeActive) GreenWave2 else TextWhite) },
            contentDescriptionResId = if (isCompareModeActive) R.string.map_action_exit_compare else R.string.map_action_start_compare,
            onClick = { mapViewModel.toggleCompareMode() },
            isSelected = isCompareModeActive
        )
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    AppScaffold(
        navController = navController,
        topBar = {
            MapScreenHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = { mapViewModel.onSearchQueryChanged(it) },
                onSearchSubmit = {
                    keyboardController?.hide()
                    mapViewModel.onBirdSearchSubmitted()
                    coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(MapViewModel.VIETNAM_INITIAL_CENTER, MapViewModel.HOME_BUTTON_ZOOM_LEVEL), 1500) }
                },
                onClearSearch = {
                    keyboardController?.hide()
                    mapViewModel.onSearchQueryCleared(cameraPositionState.position.target)
                },
                onNavigateBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() else { /* Handle no backstack */ } },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                searchResults = searchResults,
                onSearchResultClick = { birdName ->
                    keyboardController?.hide()
                    mapViewModel.onSearchQueryChanged(birdName)
                    mapViewModel.onBirdSearchSubmitted()
                }
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
                    // Removed onMarkerClick from GoogleMap directly
                ) {
                    if (customHotspotIcon != null) {
                        (mapUiState as? MapUiState.Success)?.hotspots?.forEach { ebirdHotspot ->
                            val isSelectedForCompare = selectedHotspotsForComparison.any { it.locId == ebirdHotspot.locId }
                            val currentIcon = if (isCompareModeActive && isSelectedForCompare && selectedHotspotIcon != null) {
                                selectedHotspotIcon
                            } else {
                                customHotspotIcon
                            }

                            Marker(
                                state = MarkerState(position = LatLng(ebirdHotspot.lat, ebirdHotspot.lng)),
                                title = ebirdHotspot.locName,
                                snippet = stringResource(R.string.map_marker_snippet_species, ebirdHotspot.numSpeciesAllTime ?: "N/A"),
                                icon = currentIcon,
                                zIndex = if (isCompareModeActive && isSelectedForCompare) 1.0f else 0.0f,
                                onClick = { marker -> // Handle click on individual Marker
                                    if (isCompareModeActive) {
                                        mapViewModel.onHotspotSelectedForComparison(ebirdHotspot)
                                        true // Consume click in compare mode
                                    } else {
                                        // Allow default info window behavior or custom navigation
                                        // To navigate immediately:
                                        // navController.navigate(Screen.HotspotBirdList.createRoute(ebirdHotspot.locId))
                                        // true // if you handle it and don't want info window
                                        false // to allow default info window
                                    }
                                },
                                onInfoWindowClick = {
                                    if (!isCompareModeActive) {
                                        navController.navigate(Screen.HotspotBirdList.createRoute(ebirdHotspot.locId))
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (!locationPermissionsState.allPermissionsGranted && mapsSdkInitialized) {
                // ... (Permission rationale UI remains the same) ...
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
                        onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Text(stringResource(R.string.map_grant_permissions_button), color = TextWhite)
                    }
                    if (locationPermissionsState.shouldShowRationale && !locationPermissionsState.allPermissionsGranted) {
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

            if(mapsSdkInitialized && locationPermissionsState.allPermissionsGranted) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp, top = 16.dp, bottom = if (isCompareModeActive && selectedHotspotsForComparison.isNotEmpty()) 100.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    floatingActionItems.forEach { item ->
                        FloatingMapButton(item = item)
                    }
                }
            }

            if (mapUiState is MapUiState.Loading) {
                val loadingMessage = (mapUiState as MapUiState.Loading).message
                if (!loadingMessage.isNullOrBlank()){
                    Column(
                        modifier = Modifier.align(Alignment.Center),
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
            }

            (mapUiState as? MapUiState.Error)?.let { errorState ->
                LaunchedEffect(errorState.message) {
                    Toast.makeText(context, errorState.message, Toast.LENGTH_LONG).show()
                }
            }

            if (showMapError) {
                Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.map_error_loading_detailed), color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                }
            }
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
                        border = null // No border
                    )
                        // Alternative for truly no border: border = null

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClearSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
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
                IconButton(onClick = onNavigateToCart) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.icon_cart_description), tint = TextWhite, modifier = Modifier.size(28.dp))
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
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
            colors = OutlinedTextFieldDefaults.colors( // Changed from .colors to .outlinedTextFieldColors
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = SearchBarBackground, // M3 uses containerColor
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
fun FloatingMapButton(item: FloatingMapActionItem) {
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
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (item.isSelected) GreenWave2.copy(alpha = 0.9f) else ButtonGreen.copy(alpha = 0.9f))
            .clickable(onClick = item.onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            item.icon()
        }
    }
}


@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun MapScreenPreviewWithCompareModeActive() {
    val mockViewModel: MapViewModel = viewModel()
    LaunchedEffect(Unit) {
        (mockViewModel.isCompareModeActive as MutableStateFlow).value = true
        (mockViewModel.selectedHotspotsForComparison as MutableStateFlow).value = listOf(
            EbirdNearbyHotspot("L1", "Test Hotspot 1", "VN", null, null, 10.0, 106.0, "2023-10-10", 50),
            EbirdNearbyHotspot("L2", "Test Hotspot 2 with a very long name to test ellipsis", "VN", null, null, 10.1, 106.1, "2023-10-11", 70)
        )
    }
    BirdlensTheme {
        MapScreen(navController = rememberNavController(), mapViewModel = mockViewModel)
    }
}