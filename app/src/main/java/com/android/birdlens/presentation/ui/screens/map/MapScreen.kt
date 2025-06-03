// app/src/main/java/com/android/birdlens/presentation/ui/screens/map/MapScreen.kt
package com.android.birdlens.presentation.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
// ...
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.MapUiState
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
// ...
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

data class FloatingMapActionItem(
    val icon: @Composable () -> Unit,
    val contentDescription: String,
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
        6.0f // Start with a zoom level that VM considers "overview" or "initial"
    )
    val cameraPositionState = rememberCameraPositionState {
        position = initialCameraPosition
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        mapProperties = mapProperties.copy(isMyLocationEnabled = locationPermissionsState.allPermissionsGranted)
        uiSettings = uiSettings.copy(myLocationButtonEnabled = locationPermissionsState.allPermissionsGranted)
        if (locationPermissionsState.allPermissionsGranted) {
            Log.d("MapScreen", "Permissions granted. Map ready state will trigger initial fetch.")
            // Trigger initial fetch if map is already loaded and permissions just got granted
            if (cameraPositionState.projection != null) {
                mapViewModel.requestHotspotsForCurrentView(
                    cameraPositionState.position.target,
                    cameraPositionState.position.zoom
                )
            }
        }
    }

    // This effect listens for camera idle and notifies the ViewModel
    var mapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(cameraPositionState.isMoving, mapLoaded, locationPermissionsState.allPermissionsGranted) {
        if (!cameraPositionState.isMoving && mapLoaded && locationPermissionsState.allPermissionsGranted) {
            if (cameraPositionState.projection != null) { // Ensure map is ready
                val currentTarget = cameraPositionState.position.target
                val currentZoom = cameraPositionState.position.zoom
                Log.d("MapScreen", "Camera idle. Target: $currentTarget, Zoom: $currentZoom. Notifying ViewModel.")
                mapViewModel.requestHotspotsForCurrentView(currentTarget, currentZoom)
            } else {
                Log.d("MapScreen", "Camera idle, but map projection is null. Map might not be fully loaded.")
            }
        }
    }

    val floatingActionItems = listOf(
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Refresh, contentDescription = "Refresh Hotspots", tint = TextWhite) },
            contentDescription = "Refresh Hotspots",
            onClick = {
                if (cameraPositionState.projection != null && locationPermissionsState.allPermissionsGranted) {
                    val currentTarget = cameraPositionState.position.target
                    val currentZoom = cameraPositionState.position.zoom
                    Log.d("MapScreen", "Refresh button tapped. Re-triggering fetch for current view.")
                    mapViewModel.requestHotspotsForCurrentView(currentTarget, currentZoom)
                    Toast.makeText(context, "Refreshing hotspots...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Map not ready or permissions not granted.", Toast.LENGTH_SHORT).show()
                }
            }
        ),
        // ... other items
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Info, contentDescription = "Bird Info (Test)", tint = TextWhite) },
            contentDescription = "Bird Info (Test)",
            onClick = {
                navController.navigate(Screen.BirdInfo.createRoute("houspa")) // Example species
            }
        ),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmarks", tint = TextWhite) }, contentDescription = "Bookmarks", onClick = { Toast.makeText(context, "Show Bookmarks", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.WbSunny, contentDescription = "Weather", tint = TextWhite) }, contentDescription = "Weather", onClick = { Toast.makeText(context, "Show Weather", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.StarBorder, contentDescription = "Popular Hotspots", tint = TextWhite) }, contentDescription = "Popular Hotspots", onClick = { Toast.makeText(context, "Show Hotspots", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Filled.Waves, contentDescription = "Migration Routes", tint = TextWhite.copy(alpha = 0.7f)) }, contentDescription = "Migration Routes", onClick = { Toast.makeText(context, "Show Migration (Not Implemented)", Toast.LENGTH_SHORT).show() })
    )

    AppScaffold(
        navController = navController,
        topBar = {
            MapScreenHeader(
                onNavigateBack = { if (navController.previousBackStackEntry != null) navController.popBackStack() else { /* Handle */ } },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) }
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (locationPermissionsState.allPermissionsGranted) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings,
                    onMapLoaded = {
                        Log.d("MapScreen", "GoogleMap onMapLoaded callback. Current map zoom: ${cameraPositionState.position.zoom}")
                        mapLoaded = true // Set map as loaded
                        showMapError = false
                        // Initial request will be triggered by the LaunchedEffect observing isMoving, mapLoaded, and permissions.
                    },
                    onPOIClick = { poi ->
                        Toast.makeText(context, "POI Clicked: ${poi.name}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    (mapUiState as? MapUiState.Success)?.let { successState ->
                        val currentMapZoom = cameraPositionState.position.zoom
                        Log.d("MapScreen", "Rendering ${successState.hotspots.size} markers. Current map zoom: $currentMapZoom. Fetched for context zoom: ${successState.zoomLevelContext}")

                        successState.hotspots.forEach { ebirdHotspot ->
                            // Determine marker color based on current map zoom vs. defined thresholds
                            val markerHue = if (currentMapZoom <= MapViewModel.OVERVIEW_MAX_ZOOM) {
                                BitmapDescriptorFactory.HUE_RED // More prominent for overview
                            } else {
                                BitmapDescriptorFactory.HUE_AZURE // Standard for detailed
                            }

                            Marker(
                                state = MarkerState(position = LatLng(ebirdHotspot.lat, ebirdHotspot.lng)),
                                title = ebirdHotspot.locName,
                                snippet = "Species: ${ebirdHotspot.numSpeciesAllTime ?: "N/A"}. Last obs: ${ebirdHotspot.latestObsDt ?: "N/A"}",
                                icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                                // Visibility is now implicitly handled by the ViewModel providing the correct list
                                // No complex alpha logic needed here for showing/hiding
                                onInfoWindowClick = {
                                    navController.navigate(Screen.HotspotBirdList.createRoute(ebirdHotspot.locId))
                                }
                            )
                        }
                    }
                }

                if (mapUiState is MapUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                (mapUiState as? MapUiState.Error)?.let { errorState ->
                    LaunchedEffect(errorState.message) {
                        Toast.makeText(context, errorState.message, Toast.LENGTH_LONG).show()
                    }
                }

            } else { // Permissions not granted UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Location permission is required to show your current location and relevant map features.",
                        color = TextWhite,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Text("Grant Permissions", color = TextWhite)
                    }
                    if (locationPermissionsState.shouldShowRationale && !locationPermissionsState.allPermissionsGranted) {
                        Text(
                            "If you permanently denied permission, you'll need to enable it in app settings.",
                            color = TextWhite.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (showMapError && locationPermissionsState.allPermissionsGranted) {
                Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Text("Could not load map. Ensure Google Play Services is updated and API key is valid.", color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                }
            }

            // Floating Action Buttons
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, top = 16.dp)
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                floatingActionItems.forEach { item ->
                    FloatingMapButton(item = item)
                }
            }
        }
    }

    val googlePlayServicesAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (googlePlayServicesAvailable != com.google.android.gms.common.ConnectionResult.SUCCESS) {
        LaunchedEffect(googlePlayServicesAvailable) {
            showMapError = true
        }
    }
}

// MapScreenHeader and FloatingMapButton remain the same
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenHeader(
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {
            Text("BIRDLENS", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = GreenWave2, letterSpacing = 1.sp))
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
        },
        actions = {
            IconButton(onClick = onNavigateToCart) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = TextWhite, modifier = Modifier.size(28.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        modifier = modifier.padding(top=8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingMapButton(item: FloatingMapActionItem) {
    BadgedBox(
        badge = {
            if (item.badgeCount != null) {
                Badge(containerColor = ActionButtonLightGray, contentColor = ActionButtonTextDark, modifier = Modifier.offset(x = (-6).dp, y = 6.dp)) {
                    Text(item.badgeCount.toString())
                }
            }
        },
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(ButtonGreen.copy(alpha = 0.85f))
            .clickable(onClick = item.onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            item.icon()
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