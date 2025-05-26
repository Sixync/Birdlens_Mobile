// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/map/MapScreen.kt
package com.android.birdlens.presentation.ui.screens.map

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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold // Import
import com.android.birdlens.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

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
    var showMapError by remember { mutableStateOf(false) }

    val floatingActionItems = listOf(
        FloatingMapActionItem(icon = { Icon(Icons.Filled.Nature, contentDescription = "Bird Locations", tint = TextWhite) }, contentDescription = "Bird Locations", onClick = { Toast.makeText(context, "Show Bird Locations", Toast.LENGTH_SHORT).show() }, badgeCount = 0),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Bookmarks", tint = TextWhite) }, contentDescription = "Bookmarks", onClick = { Toast.makeText(context, "Show Bookmarks", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.WbSunny, contentDescription = "Weather", tint = TextWhite) }, contentDescription = "Weather", onClick = { Toast.makeText(context, "Show Weather", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Outlined.StarBorder, contentDescription = "Popular Hotspots", tint = TextWhite) }, contentDescription = "Popular Hotspots", onClick = { Toast.makeText(context, "Show Hotspots", Toast.LENGTH_SHORT).show() }),
        FloatingMapActionItem(icon = { Icon(Icons.Filled.Waves, contentDescription = "Migration Routes", tint = TextWhite.copy(alpha = 0.7f)) }, contentDescription = "Migration Routes", onClick = { Toast.makeText(context, "Show Migration (Not Implemented)", Toast.LENGTH_SHORT).show() })
    )

    val vietnamCenter = LatLng(16.047079, 108.220825)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vietnamCenter, 5f)
    }

    AppScaffold(
        navController = navController,
        topBar = {
            MapScreenHeader( // Specific header for MapScreen
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) }
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding) // Apply padding from AppScaffold
                .fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true),
                properties = MapProperties(mapType = MapType.NORMAL),
                onMapLoaded = { showMapError = false },
            ) {
                // Add Markers here
            }
            if (showMapError) {
                Box(modifier = Modifier.fillMaxSize().background(GreenDeep.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                    Text("Could not load map. Ensure Google Play Services is updated and API key is valid.", color = TextWhite, modifier = Modifier.padding(32.dp), textAlign = TextAlign.Center)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, top = 16.dp) // Added top padding
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
        modifier = modifier.padding(top=8.dp) // Added to give some space if status bar padding isn't enough by itself
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
            .background(ButtonGreen.copy(alpha = 0.8f))
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