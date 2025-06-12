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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch

fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    tintColor: Int? = null
): BitmapDescriptor? {
    return try {
        ContextCompat.getDrawable(context, vectorResId)?.let { vectorDrawable ->
            if (tintColor != null) {
                DrawableCompat.setTint(vectorDrawable, tintColor)
            }
            val width = vectorDrawable.intrinsicWidth.takeIf { it > 0 } ?: 72
            val height = vectorDrawable.intrinsicHeight.takeIf { it > 0 } ?: 72
            vectorDrawable.setBounds(0, 0, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            vectorDrawable.draw(canvas)
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
    val contentDescriptionResId: Int, // Changed to resource ID
    val onClick: () -> Unit,
    val badgeCount: Int? = null
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

    var customHotspotIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var mapsSdkInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context.applicationContext, MapsInitializer.Renderer.LATEST, object : OnMapsSdkInitializedCallback {
                override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
                    Log.d("MapScreen", "Maps SDK Initialized with renderer: $renderer")
                    customHotspotIcon = bitmapDescriptorFromVector(context, R.drawable.ic_map_pin)
                    if (customHotspotIcon == null) {
                        Log.e("MapScreen", "Failed to create customHotspotIcon even after SDK initialization.")
                    }
                    mapsSdkInitialized = true
                }
            })
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
            Log.d("MapScreen", "Camera idle. Target: $currentTarget, Zoom: $currentZoom. Notifying ViewModel.")
            mapViewModel.requestHotspotsForCurrentView(currentTarget, currentZoom)
        }
    }

    val floatingActionItems = listOf(
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.map_action_home), tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_home,
            onClick = {
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            MapViewModel.VIETNAM_INITIAL_CENTER,
                            MapViewModel.HOME_BUTTON_ZOOM_LEVEL
                        ),
                        durationMs = 1000
                    )
                }
            }
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.map_action_refresh_hotspots), tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_refresh_hotspots,
            onClick = {
                if (mapsSdkInitialized && mapLoaded) {
                    val currentTarget = cameraPositionState.position.target
                    val currentZoom = cameraPositionState.position.zoom
                    Log.d("MapScreen", "Refresh button tapped. Re-triggering fetch for current view.")
                    mapViewModel.requestHotspotsForCurrentView(currentTarget, currentZoom)
                    Toast.makeText(context, context.getString(R.string.map_toast_refreshing_hotspots), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.map_toast_map_not_ready), Toast.LENGTH_SHORT).show()
                }
            }
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.map_action_identify_bird), tint = TextWhite) },
            contentDescriptionResId = R.string.map_action_identify_bird,
            onClick = {
                navController.navigate(Screen.BirdIdentifier.route)
            }
        ),
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
                    // Animate camera to show the whole country after search
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                MapViewModel.VIETNAM_INITIAL_CENTER,
                                MapViewModel.HOME_BUTTON_ZOOM_LEVEL
                            ),
                            1500
                        )
                    }
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
                    // This sequence updates the text field and immediately submits
                    mapViewModel.onSearchQueryChanged(birdName)
                    mapViewModel.onBirdSearchSubmitted()
                }
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!mapsSdkInitialized && !showMapError) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
            } else if (locationPermissionsState.allPermissionsGranted && mapsSdkInitialized) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    onMapLoaded = {
                        Log.d("MapScreen", "GoogleMap onMapLoaded callback.")
                        mapLoaded = true
                        showMapError = false
                        mapViewModel.requestHotspotsForCurrentView(
                            cameraPositionState.position.target,
                            cameraPositionState.position.zoom
                        )
                    },
                    onPOIClick = { poi ->
                        Toast.makeText(context, context.getString(R.string.map_toast_poi_clicked, poi.name), Toast.LENGTH_SHORT).show()
                    }
                ) {
                    if (customHotspotIcon != null) {
                        (mapUiState as? MapUiState.Success)?.let { successState ->
                            Log.d("MapScreen", "Rendering ${successState.hotspots.size} markers with custom icon.")
                            successState.hotspots.forEach { ebirdHotspot ->
                                Marker(
                                    state = MarkerState(position = LatLng(ebirdHotspot.lat, ebirdHotspot.lng)),
                                    title = ebirdHotspot.locName,
                                    snippet = stringResource(R.string.map_marker_snippet_species, ebirdHotspot.numSpeciesAllTime ?: "N/A"),
                                    icon = customHotspotIcon,
                                    onInfoWindowClick = {
                                        navController.navigate(Screen.HotspotBirdList.createRoute(ebirdHotspot.locId))
                                    }
                                )
                            }
                        }
                    } else {
                        Log.w("MapScreen", "Custom hotspot icon is null; markers relying on it won't be rendered or will use default.")
                    }
                }

                if (mapUiState is MapUiState.Loading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = GreenWave2.copy(alpha = 0.7f), strokeWidth = 5.dp)
                        val message = (mapUiState as MapUiState.Loading).message
                        if (!message.isNullOrBlank()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = message,
                                color = TextWhite,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                (mapUiState as? MapUiState.Error)?.let { errorState ->
                    LaunchedEffect(errorState, errorState.message) {
                        Toast.makeText(context, errorState.message, Toast.LENGTH_LONG).show()
                    }
                }

            } else if (!locationPermissionsState.allPermissionsGranted && mapsSdkInitialized) {
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

            if (showMapError) {
                Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.map_error_loading_detailed), color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                }
            }

            if(mapsSdkInitialized && locationPermissionsState.allPermissionsGranted) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    floatingActionItems.forEach { item ->
                        FloatingMapButton(item = item)
                    }
                }
            }
        }
    }

    val googlePlayServicesAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (googlePlayServicesAvailable != com.google.android.gms.common.ConnectionResult.SUCCESS) {
        LaunchedEffect(googlePlayServicesAvailable) {
            Toast.makeText(context, context.getString(R.string.map_toast_play_services_unavailable), Toast.LENGTH_LONG).show()
            showMapError = true
            mapsSdkInitialized = false
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
            modifier = modifier.padding(top = 8.dp)
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
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedContainerColor = SearchBarBackground,
                unfocusedContainerColor = SearchBarBackground,
                cursorColor = TextWhite,
                focusedIndicatorColor = GreenWave2,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // Search recommendations dropdown
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
                                Icons.Outlined.Search, // Or a bird icon
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
                Badge(containerColor = ActionButtonLightGray, contentColor = ActionButtonTextDark, modifier = Modifier.offset(x = (-6).dp, y = 6.dp)) {
                    Text(item.badgeCount.toString())
                }
            }
        },
        modifier = Modifier
            .size(52.dp) // Slightly smaller for a list of buttons
            .clip(CircleShape)
            .background(ButtonGreen.copy(alpha = 0.9f)) // Slightly more opaque
            .clickable(onClick = item.onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            item.icon() // Icon composable is called here
        }
    }
}


@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun MapScreenPreview() {
    BirdlensTheme {
        MapScreen(navController = rememberNavController())
    }
}