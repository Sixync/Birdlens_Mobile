// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/BottomNavigationItems.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.android.birdlens.presentation.navigation.Screen

data class BottomNavItemData(
    val label: String,
    val icon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
    val route: String
)

val globalBottomNavItems = listOf(
    BottomNavItemData(
        label = "Settings", // Or "Filter" as in some designs
        icon = { Icon(Icons.Outlined.Tune, "Settings") },
        selectedIcon = { Icon(Icons.Filled.Tune, "Settings") },
        route = Screen.Settings.route
    ),
    BottomNavItemData(
        label = "Community",
        icon = { Icon(Icons.Outlined.Groups, "Community") },
        selectedIcon = { Icon(Icons.Filled.Groups, "Community") },
        route = Screen.Community.route
    ),
    BottomNavItemData(
        label = "Map",
        icon = { Icon(Icons.Outlined.Map, "Map") },
        selectedIcon = { Icon(Icons.Filled.Map, "Map") },
        route = Screen.Map.route
    ),
    BottomNavItemData(
        label = "Marketplace",
        icon = { Icon(Icons.Outlined.ShoppingCart, "Marketplace") },
        selectedIcon = { Icon(Icons.Filled.ShoppingCart, "Marketplace") },
        route = Screen.Marketplace.route // Could also link to Cart as a primary action
    ),
    BottomNavItemData(
        label = "Tours", // Or "Calendar" as in some designs
        icon = { Icon(Icons.Outlined.CalendarToday, "Tours") },
        selectedIcon = { Icon(Icons.Filled.CalendarToday, "Tours") },
        route = Screen.Tour.route // Main/home tab
    )
)