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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.R
// ...
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.MapUiState
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
// ...
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

// Helper function to convert vector drawable to BitmapDescriptor
fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    tintColor: Int? = null
): BitmapDescriptor? {
    return try { // Add try-catch here for safety during BitmapDescriptorFactory interaction
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
        // This specific catch is for the "IBitmapDescriptorFactory is not initialized" error
        Log.e("MapScreen", "Error creating BitmapDescriptor: IBitmapDescriptorFactory not initialized or other NPE.", e)
        null
    } catch (e: Exception) {
        Log.e("MapScreen", "Generic error creating BitmapDescriptor.", e)
        null
    }
}


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

    // State for the custom marker icon
    var customHotspotIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var mapsSdkInitialized by remember { mutableStateOf(false) }

    // Explicitly initialize Maps SDK and create the icon in the callback
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.initialize(context.applicationContext, MapsInitializer.Renderer.LATEST, object : OnMapsSdkInitializedCallback {
                override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
                    Log.d("MapScreen", "Maps SDK Initialized with renderer: $renderer")
                    // It's safer to create BitmapDescriptor here, after SDK initialization is confirmed.
                    customHotspotIcon = bitmapDescriptorFromVector(context, R.drawable.ic_map_pin)
                    if (customHotspotIcon == null) {
                        Log.e("MapScreen", "Failed to create customHotspotIcon even after SDK initialization.")
                        // Optionally set showMapError or use a default icon
                    }
                    mapsSdkInitialized = true // Set this true only after successful initialization and icon creation attempt
                }
            })
        } catch (e: Exception) {
            Log.e("MapScreen", "Error during MapsInitializer.initialize", e)
            showMapError = true
            mapsSdkInitialized = false // Ensure this is false if init fails
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
        6.0f
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
        if (mapsSdkInitialized) { // Only update map properties if SDK is ready
            mapProperties = mapProperties.copy(isMyLocationEnabled = locationPermissionsState.allPermissionsGranted)
            uiSettings = uiSettings.copy(myLocationButtonEnabled = locationPermissionsState.allPermissionsGranted)
            if (locationPermissionsState.allPermissionsGranted) {
                Log.d("MapScreen", "Permissions granted & SDK initialized. Map ready state will trigger initial fetch.")
                if (cameraPositionState.projection != null) {
                    mapViewModel.requestHotspotsForCurrentView(
                        cameraPositionState.position.target,
                        cameraPositionState.position.zoom
                    )
                }
            }
        }
    }

    var mapLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(cameraPositionState.isMoving, mapLoaded, locationPermissionsState.allPermissionsGranted, mapsSdkInitialized) {
        if (!cameraPositionState.isMoving && mapLoaded && locationPermissionsState.allPermissionsGranted && mapsSdkInitialized) {
            if (cameraPositionState.projection != null) {
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
                if (mapsSdkInitialized && cameraPositionState.projection != null && locationPermissionsState.allPermissionsGranted) {
                    val currentTarget = cameraPositionState.position.target
                    val currentZoom = cameraPositionState.position.zoom
                    Log.d("MapScreen", "Refresh button tapped. Re-triggering fetch for current view.")
                    mapViewModel.requestHotspotsForCurrentView(currentTarget, currentZoom)
                    Toast.makeText(context, "Refreshing hotspots...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Map not ready, SDK not initialized, or permissions not granted.", Toast.LENGTH_SHORT).show()
                }
            }
        ),
        // ...
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
                        if (customHotspotIcon == null && mapsSdkInitialized) { // Ensure SDK was init
                            Log.w("MapScreen", "customHotspotIcon is still null in onMapLoaded, attempting to create again.")
                            customHotspotIcon = bitmapDescriptorFromVector(context, R.drawable.ic_map_pin)
                            if(customHotspotIcon == null) {
                                Log.e("MapScreen", "Failed to create custom marker icon even in onMapLoaded.")
                                // Consider showing an error or using a default if this fails persistently
                            }
                        }
                        showMapError = false
                    },
                    onPOIClick = { poi ->
                        Toast.makeText(context, "POI Clicked: ${poi.name}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    if (customHotspotIcon != null) {
                        (mapUiState as? MapUiState.Success)?.let { successState ->
                            Log.d("MapScreen", "Rendering ${successState.hotspots.size} markers with custom icon. Fetched for context zoom: ${successState.zoomLevelContext}")
                            successState.hotspots.forEach { ebirdHotspot ->
                                Marker(
                                    state = MarkerState(position = LatLng(ebirdHotspot.lat, ebirdHotspot.lng)),
                                    title = ebirdHotspot.locName,
                                    snippet = "Species: ${ebirdHotspot.numSpeciesAllTime ?: "N/A"}. Last obs: ${ebirdHotspot.latestObsDt ?: "N/A"}",
                                    icon = customHotspotIcon,
                                    onInfoWindowClick = {
                                        navController.navigate(Screen.HotspotBirdList.createRoute(ebirdHotspot.locId))
                                    }
                                )
                            }
                        }
                    } else {
                        // This log helps diagnose if icon creation failed or is delayed
                        Log.w("MapScreen", "Custom hotspot icon is null; markers relying on it won't be rendered or will use default.")
                        // If you want to show default markers while custom icon is loading/failed:
                        // (mapUiState as? MapUiState.Success)?.hotspots?.forEach { ebirdHotspot ->
                        //     Marker(
                        //         state = MarkerState(position = LatLng(ebirdHotspot.lat, ebirdHotspot.lng)),
                        //         title = ebirdHotspot.locName,
                        //         // icon = BitmapDescriptorFactory.defaultMarker() // Fallback
                        //     )
                        // }
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

            } else if (!locationPermissionsState.allPermissionsGranted && mapsSdkInitialized) {
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

            if (showMapError) {
                Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Text("Could not load map. Ensure Google Play Services is updated, API key is valid, and Maps SDK initialized correctly.", color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                }
            }

            if(mapsSdkInitialized && locationPermissionsState.allPermissionsGranted) {
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
    }

    val googlePlayServicesAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (googlePlayServicesAvailable != com.google.android.gms.common.ConnectionResult.SUCCESS) {
        LaunchedEffect(googlePlayServicesAvailable) {
            Toast.makeText(context, "Google Play Services is not available or outdated. Map functionality may be limited.", Toast.LENGTH_LONG).show()
            showMapError = true
            mapsSdkInitialized = false // Crucial: if Play Services aren't OK, SDK won't init properly
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