// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/components/AppScaffold.kt
package com.android.birdlens.presentation.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState // Import this
import com.android.birdlens.presentation.navigation.Screen

@Composable
fun AppScaffold(
    navController: NavController, // Pass NavController directly
    topBar: @Composable () -> Unit = {},
    showBottomBar: Boolean = true,
    floatingActionButton: @Composable () -> Unit = {},
    // currentRoute is now derived internally
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
        SharedAppBackground() // Apply the background to the whole area

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            topBar = topBar,
            bottomBar = {
                if (showBottomBar) {
                    AppBottomNavigationBar(
                        items = globalBottomNavItems,
                        selectedItemRoute = currentRoute, // Use derived currentRoute
                        onItemSelected = { item ->
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Tour.route) { // Assuming Tour is the "home" of bottom nav graph
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            },
            floatingActionButton = floatingActionButton,
            containerColor = Color.Transparent // Make Scaffold transparent to see SharedAppBackground
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}