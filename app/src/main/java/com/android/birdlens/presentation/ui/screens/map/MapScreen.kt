package com.android.birdlens.presentation.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
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
import com.android.birdlens.data.model.ebird.EbirdNearbyHotspot // Import the new model
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.viewmodel.MapUiState
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }
    var uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = true))
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            mapProperties = mapProperties.copy(isMyLocationEnabled = true)
            uiSettings = uiSettings.copy(myLocationButtonEnabled = true)
            // You might want to fetch initial hotspots here or based on user location
        } else {
            mapProperties = mapProperties.copy(isMyLocationEnabled = false)
            uiSettings = uiSettings.copy(myLocationButtonEnabled = false)
        }
    }
    val vietnamCenter = LatLng(16.047079, 108.220825) // Default center
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vietnamCenter, 6f)
    }

    val floatingActionItems = listOf(
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Refresh, contentDescription = "Refresh Hotspots", tint = TextWhite) },
            contentDescription = "Refresh Hotspots",
            onClick = {
                // Fetch hotspots for the current camera view center
                val currentCameraPosition = cameraPositionState.position.target
                mapViewModel.fetchNearbyHotspots(currentCameraPosition)
                Toast.makeText(context, "Refreshing hotspots...", Toast.LENGTH_SHORT).show()

            }
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Info, contentDescription = "Bird Info (Test)", tint = TextWhite) },
            contentDescription = "Bird Info (Test)",
            onClick = {
                navController.navigate(Screen.BirdInfo.createRoute("houspa"))
            }
        ),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmarks", tint = TextWhite) }, contentDescription = "Bookmarks", onClick = { Toast.makeText(context, "Show Bookmarks", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.WbSunny, contentDescription = "Weather", tint = TextWhite) }, contentDescription = "Weather", onClick = { Toast.makeText(context, "Show Weather", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.StarBorder, contentDescription = "Popular Hotspots", tint = TextWhite) }, contentDescription = "Popular Hotspots", onClick = { Toast.makeText(context, "Show Hotspots", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Filled.Waves, contentDescription = "Migration Routes", tint = TextWhite.copy(alpha = 0.7f)) }, contentDescription = "Migration Routes", onClick = { Toast.makeText(context, "Show Migration (Not Implemented)", Toast.LENGTH_SHORT).show() })
    )



    // Fetch initial hotspots when map is ready (and permissions granted)
    LaunchedEffect(locationPermissionsState.allPermissionsGranted, cameraPositionState.isMoving) {
        if (locationPermissionsState.allPermissionsGranted && !cameraPositionState.isMoving) {
            // Fetch for the initial view or after camera settles
            // mapViewModel.fetchNearbyHotspots(cameraPositionState.position.target)
            // Consider debouncing or fetching on a button press to avoid too many API calls
        }
    }

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
                        showMapError = false
                        // Fetch initial hotspots for the default camera position
                        mapViewModel.fetchNearbyHotspots(cameraPositionState.position.target)
                    },
                    // onCameraMoveStarted = { reason ->
                    //     // Potentially fetch new hotspots when camera moves, with debouncing
                    //     // if (reason == CameraMoveStartedReason.GESTURE) {
                    //     //    mapViewModel.fetchNearbyHotspots(cameraPositionState.position.target)
                    //     // }
                    // }
                ) {
                    when (val state = mapUiState) {
                        is MapUiState.Success -> {
                            state.hotspots.forEach { ebirdHotspot ->
                                Marker(
                                    state = MarkerState(position = LatLng(ebirdHotspot.lat, ebirdHotspot.lng)),
                                    title = ebirdHotspot.locName,
                                    snippet = "Species: ${ebirdHotspot.numSpeciesAllTime ?: "N/A"}. Last obs: ${ebirdHotspot.latestObsDt ?: "N/A"}",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN), // Or a custom bird icon
                                    onInfoWindowClick = {
                                        navController.navigate(Screen.HotspotBirdList.createRoute(ebirdHotspot.locId))
                                    }
                                )
                            }
                        }
                        is MapUiState.Error -> {
                            // Optionally show error on map or as a toast
                            LaunchedEffect(state.message) { // Ensure toast shows once per message
                                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                        // Handle Idle, Loading states if needed (e.g. show a global loading indicator)
                        else -> {}
                    }
                }
                if (mapUiState is MapUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }

            } else {
                // Show UI to request permissions (existing code is fine)
                // ...
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Location permission is required to show your current location and enhance map features.",
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
                    if (locationPermissionsState.shouldShowRationale) {
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

            if (showMapError && locationPermissionsState.allPermissionsGranted) { // Only show map-specific error if permissions are granted
                Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Text("Could not load map. Ensure Google Play Services is updated and API key is valid.", color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                }
            }

            // Floating Action Buttons on the left
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
            showMapError = true // Set true if Play Services are an issue
        }
    }
}

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