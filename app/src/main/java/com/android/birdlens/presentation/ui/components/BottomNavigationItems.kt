// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/BottomNavigationItems.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.birdlens.R
import com.android.birdlens.presentation.navigation.Screen

data class BottomNavItemData(
    val labelResId: Int,
    val icon: @Composable () -> Unit,
    val selectedIcon: @Composable () -> Unit,
    val route: String
)

// Logic: The bottom navigation items are now reordered and updated to match the new requirements.
// 'Home' is the new main screen, 'Premium' is added to showcase the subscription,
// and 'Me' replaces 'Settings' as the user profile entry point.
val globalBottomNavItems
    @Composable
    get() = listOf(
        BottomNavItemData(
            labelResId = R.string.bottom_nav_home,
            icon = { Icon(Icons.Outlined.Home, stringResource(id = R.string.bottom_nav_home)) },
            selectedIcon = { Icon(Icons.Filled.Home, stringResource(id = R.string.bottom_nav_home)) },
            route = Screen.Home.route // Main/home tab
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
            labelResId = R.string.bottom_nav_premium,
            icon = { Icon(Icons.Outlined.WorkspacePremium, stringResource(id = R.string.bottom_nav_premium)) },
            selectedIcon = { Icon(Icons.Filled.WorkspacePremium, stringResource(id = R.string.bottom_nav_premium)) },
            route = Screen.Premium.route
        ),
        BottomNavItemData(
            labelResId = R.string.bottom_nav_me,
            icon = { Icon(Icons.Outlined.AccountCircle, stringResource(id = R.string.bottom_nav_me)) },
            selectedIcon = { Icon(Icons.Filled.AccountCircle, stringResource(id = R.string.bottom_nav_me)) },
            route = Screen.Me.route
        )
    )