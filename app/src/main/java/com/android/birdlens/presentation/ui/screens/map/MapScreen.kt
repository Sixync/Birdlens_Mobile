// app/src/main/java/com/android/birdlens/presentation/ui/screens/map/MapScreen.kt
package com.android.birdlens.presentation.ui.screens.map

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.screens.tour.BottomNavItem
import com.android.birdlens.presentation.ui.screens.tour.BottomNavigationBar
import com.android.birdlens.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collect

data class FloatingMapActionItem(
    val icon: @Composable () -> Unit,
    val contentDescription: String,
    val onClick: () -> Unit,
    val badgeCount: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bottomNavItems = listOf(
        BottomNavItem("Filter", { Icon(Icons.Outlined.Tune, "Filter") }, { Icon(Icons.Filled.Tune, "Filter") }, "filter_route_placeholder_map"),
        BottomNavItem("People", { Icon(Icons.Outlined.Groups, "People") }, { Icon(Icons.Filled.Groups, "People") }, "people_route_placeholder_map"),
        BottomNavItem("Map", { Icon(Icons.Outlined.Map, "Map") }, { Icon(Icons.Filled.Map, "Map") }, Screen.Map.route),
        BottomNavItem("Cart", { Icon(Icons.Outlined.ShoppingCart, "Marketplace") }, { Icon(Icons.Filled.ShoppingCart, "Marketplace") }, Screen.Marketplace.route),
        BottomNavItem("Calendar", { Icon(Icons.Outlined.CalendarToday, "Calendar") }, { Icon(Icons.Filled.CalendarToday, "Calendar") }, Screen.Tour.route)
    )

    var selectedBottomNavItem by remember {
        mutableStateOf(bottomNavItems.indexOfFirst { it.route == Screen.Map.route }.coerceAtLeast(0))
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            val newIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
            if (newIndex != -1) {
                selectedBottomNavItem = newIndex
            }
        }
    }

    val floatingActionItems = listOf(
        FloatingMapActionItem(
            icon = { Icon(Icons.Filled.Nature, contentDescription = "Bird Locations", tint = TextWhite) }, // Using Nature as a bird proxy
            contentDescription = "Bird Locations",
            onClick = { Toast.makeText(context, "Show Bird Locations", Toast.LENGTH_SHORT).show() },
            badgeCount = 0 // Example badge
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmarks", tint = TextWhite) },
            contentDescription = "Bookmarks",
            onClick = { Toast.makeText(context, "Show Bookmarks", Toast.LENGTH_SHORT).show() }
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Outlined.WbSunny, contentDescription = "Weather", tint = TextWhite) }, // Sun icon for weather
            contentDescription = "Weather",
            onClick = { Toast.makeText(context, "Show Weather", Toast.LENGTH_SHORT).show() }
        ),
        FloatingMapActionItem(
            icon = { Icon(Icons.Outlined.StarBorder, contentDescription = "Popular Hotspots", tint = TextWhite) },
            contentDescription = "Popular Hotspots",
            onClick = { Toast.makeText(context, "Show Hotspots", Toast.LENGTH_SHORT).show() }
        ),
        FloatingMapActionItem( // Migration - placeholder, less prominent
            icon = { Icon(Icons.Filled.Waves, contentDescription = "Migration Routes", tint = TextWhite.copy(alpha = 0.7f)) },
            contentDescription = "Migration Routes",
            onClick = { Toast.makeText(context, "Show Migration (Not Implemented)", Toast.LENGTH_SHORT).show() }
        )
    )

    // Initial camera position for Vietnam
    val vietnamCenter = LatLng(16.047079, 108.220825) // Da Nang as a rough center
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vietnamCenter, 5f) // Zoom level 5 shows most of Vietnam
    }
    var showMapError by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Background Canvas - will be mostly covered by the map
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = GreenDeep)
            // ... (optional: add subtle wave paths if needed, but map will dominate)
        }

        Scaffold(
            topBar = {
                MapScreenHeader(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToCart = { navController.navigate(Screen.Cart.route) }
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    items = bottomNavItems,
                    selectedItemIndex = selectedBottomNavItem,
                    onItemSelected = { index ->
                        val destinationRoute = bottomNavItems[index].route
                        if (navController.currentDestination?.route != destinationRoute) {
                            // Prevent navigating to placeholder routes for now
                            if (!destinationRoute.contains("placeholder")) {
                                navController.navigate(destinationRoute) {
                                    popUpTo(Screen.Tour.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                Toast.makeText(context, "Feature not implemented", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent, // Map will provide its own background
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false, // Controls can be added manually if needed
                        myLocationButtonEnabled = true // Requires location permission handling
                    ),
                    properties = MapProperties(
                        mapType = MapType.NORMAL // Or SATELLITE, HYBRID, TERRAIN
                    ),
                    onMapLoaded = {
                        // Map has loaded, perhaps move camera again if needed
                        // cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(vietnamCenter, 5f))
                        showMapError = false
                    },
                    // In a real app, you'd handle API key errors more gracefully
                    // For now, we'll just show a text overlay if it fails to load.
                    // This simple error check won't catch API key issues directly, but it's a start.
                    // Proper API key error handling is more involved.
                ) {
                    // TODO: Add Markers here for bird locations
                    // Example Marker:
                    // Marker(
                    //     state = MarkerState(position = LatLng(10.7769, 106.7009)), // Ho Chi Minh City
                    //     title = "Ho Chi Minh City",
                    //     snippet = "Bird Spotting Area 1"
                    // )
                }
                if (showMapError) {
                    Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                        Text("Could not load map. Ensure Google Play Services is updated and API key is valid.", color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }


                // Floating Action Buttons on the left
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp) // Padding from the left edge
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
    // Check for Google Play Services (simplified check for map screen)
    // Consider moving this to MainActivity or a ViewModel if more robust checking is needed
    val googlePlayServicesAvailable = com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
    if (googlePlayServicesAvailable != com.google.android.gms.common.ConnectionResult.SUCCESS) {
        // This is a side effect, should ideally be handled where it can show a dialog
        LaunchedEffect(googlePlayServicesAvailable) {
            showMapError = true // Set error state if play services are not available
            // Toast.makeText(context, "Google Play Services is required for maps.", Toast.LENGTH_LONG).show()
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
    // Similar to DetailScreenHeader but simpler for map
    CenterAlignedTopAppBar(
        title = {
            Text(
                "BIRDLENS",
                style = MaterialTheme.typography.headlineSmall.copy( // Slightly smaller than TourScreen
                    fontWeight = FontWeight.ExtraBold,
                    color = GreenWave2, // Bright green title
                    letterSpacing = 1.sp
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextWhite
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToCart) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "Cart",
                    tint = TextWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent // Keep it transparent to see map/background
        ),
        modifier = modifier
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingMapButton(item: FloatingMapActionItem) {
    BadgedBox(
        badge = {
            if (item.badgeCount != null) {
                Badge(
                    containerColor = ActionButtonLightGray, // Or a more contrasting color like Red
                    contentColor = ActionButtonTextDark,
                    modifier = Modifier.offset(x = (-6).dp, y = 6.dp)
                ) {
                    Text(item.badgeCount.toString())
                }
            }
        },
        modifier = Modifier
            .size(56.dp) // Standard FAB size
            .clip(CircleShape)
            .background(ButtonGreen.copy(alpha = 0.8f)) // Semi-transparent background
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
        // For preview, GoogleMap might not render, or show a placeholder.
        // We simulate the structure.
        val navController = rememberNavController()
        MapScreen(navController = navController)
    }
}