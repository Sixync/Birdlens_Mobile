// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/BottomNavigationItems.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource // Added
import com.android.birdlens.R // Added
import com.android.birdlens.presentation.navigation.Screen

data class BottomNavItemData(
    val labelResId: Int, // Changed to resource ID
    val icon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
    val route: String
)

val globalBottomNavItems
    @Composable // Needs to be composable to access stringResource
    get() = listOf(
        BottomNavItemData(
            labelResId = R.string.bottom_nav_settings,
            icon = { Icon(Icons.Outlined.Tune, stringResource(id = R.string.bottom_nav_settings)) },
            selectedIcon = { Icon(Icons.Filled.Tune, stringResource(id = R.string.bottom_nav_settings)) },
            route = Screen.Settings.route
        ),
        BottomNavItemData(
            labelResId = R.string.bottom_nav_community,
            icon = { Icon(Icons.Outlined.Groups, stringResource(id = R.string.bottom_nav_community)) },
            selectedIcon = { Icon(Icons.Filled.Groups, stringResource(id = R.string.bottom_nav_community)) },
            route = Screen.Community.route
        ),
        BottomNavItemData(
            labelResId = R.string.bottom_nav_map,
            icon = { Icon(Icons.Outlined.Map, stringResource(id = R.string.bottom_nav_map)) },
            selectedIcon = { Icon(Icons.Filled.Map, stringResource(id = R.string.bottom_nav_map)) },
            route = Screen.Map.route
        ),
        BottomNavItemData(
            labelResId = R.string.bottom_nav_marketplace,
            icon = { Icon(Icons.Outlined.ShoppingCart, stringResource(id = R.string.bottom_nav_marketplace)) },
            selectedIcon = { Icon(Icons.Filled.ShoppingCart, stringResource(id = R.string.bottom_nav_marketplace)) },
            route = Screen.Marketplace.route // Could also link to Cart as a primary action
        ),
        BottomNavItemData(
            labelResId = R.string.bottom_nav_tours,
            icon = { Icon(Icons.Outlined.CalendarToday, stringResource(id = R.string.bottom_nav_tours)) },
            selectedIcon = { Icon(Icons.Filled.CalendarToday, stringResource(id = R.string.bottom_nav_tours)) },
            route = Screen.Tour.route // Main/home tab
        )
    )