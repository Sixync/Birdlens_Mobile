// EXE201/app/src/main/java/com/android/birdlens/presentation/navigation/AppNavigation.kt
package com.android.birdlens.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.birdlens.presentation.ui.screens.accountinfo.AccountInfoScreen
import com.android.birdlens.presentation.ui.screens.allevents.AllEventsListScreen
import com.android.birdlens.presentation.ui.screens.alltours.AllToursListScreen
import com.android.birdlens.presentation.ui.screens.birdinfo.BirdInfoScreen // New Import
import com.android.birdlens.presentation.ui.screens.cart.CartScreen
import com.android.birdlens.presentation.ui.screens.community.CommunityScreen
import com.android.birdlens.presentation.ui.screens.login.LoginScreen
import com.android.birdlens.presentation.ui.screens.loginsuccess.LoginSuccessScreen
import com.android.birdlens.presentation.ui.screens.map.MapScreen
import com.android.birdlens.presentation.ui.screens.marketplace.MarketplaceScreen
import com.android.birdlens.presentation.ui.screens.pickdays.PickDaysScreen
import com.android.birdlens.presentation.ui.screens.register.RegisterScreen
import com.android.birdlens.presentation.ui.screens.settings.SettingsScreen
import com.android.birdlens.presentation.ui.screens.tour.TourScreen
import com.android.birdlens.presentation.ui.screens.tourdetail.TourDetailScreen
import com.android.birdlens.presentation.ui.screens.welcome.WelcomeScreen
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.presentation.viewmodel.BirdInfoViewModel // New Import
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.presentation.ui.screens.hotspotbirdlist.HotspotBirdListScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    googleAuthViewModel: GoogleAuthViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        modifier = modifier
    ) {
        // ... (Welcome, Login, Register, LoginSuccess composables remain the same)
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLoginClicked = { navController.navigate(Screen.Login.route) },
                onNewUserClicked = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                onNavigateBack = { navController.popBackStack() },
                onForgotPassword = { /* TODO */ }

            )
        }
        composable(
            route = Screen.HotspotBirdList.route,
            arguments = listOf(navArgument("hotspotId") {
                type = NavType.StringType
                // nullable = true // hotspotId can be non-nullable if you ensure it's always passed
            })
        ) { backStackEntry ->
            val hotspotId = backStackEntry.arguments?.getString("hotspotId")
            // Add a check for hotspotId != null if it's critical, or handle null in the screen
            HotspotBirdListScreen(
                navController = navController,
                hotspotId = hotspotId ?: "" // Pass an empty string or handle null more gracefully
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LoginSuccess.route) {
            LoginSuccessScreen(
                onContinue = {
                    navController.navigate(Screen.Tour.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }


        composable(Screen.Tour.route) {
            TourScreen(
                navController = navController,
                onNavigateToAllEvents = { navController.navigate(Screen.AllEventsList.route) },
                onNavigateToAllTours = { navController.navigate(Screen.AllToursList.route) },
                onNavigateToPopularTours = { navController.navigate(Screen.AllToursList.route) },
                onTourItemClick = { tourId ->
                    navController.navigate(Screen.TourDetail.createRoute(tourId))
                }
            )
        }
        composable(Screen.AllEventsList.route) {
            AllEventsListScreen(
                navController = navController,
                onEventItemClick = { eventId ->
                    navController.navigate(Screen.TourDetail.createRoute(eventId))
                }
            )
        }
        composable(Screen.AllToursList.route) {
            AllToursListScreen(
                navController = navController,
                onTourItemClick = { tourId ->
                    navController.navigate(Screen.TourDetail.createRoute(tourId))
                }
            )
        }
        composable(
            route = Screen.TourDetail.route,
            arguments = listOf(navArgument("tourId") { type = NavType.IntType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getInt("tourId") ?: -1
            TourDetailScreen(navController = navController, tourId = tourId)
        }
        composable(
            route = Screen.PickDays.route,
            arguments = listOf(navArgument("tourId") { type = NavType.IntType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getInt("tourId") ?: -1
            PickDaysScreen(navController = navController, tourId = tourId)
        }
        composable(Screen.Cart.route) {
            CartScreen(navController = navController)
        }
        composable(Screen.Marketplace.route) {
            MarketplaceScreen(navController = navController)
        }
        composable(Screen.Map.route) {
            MapScreen(navController = navController)
        }
        composable(Screen.Community.route) {
            CommunityScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel
            )
        }
        composable(Screen.AccountInfo.route) {
            val accountInfoViewModel: AccountInfoViewModel = viewModel()
            AccountInfoScreen(
                navController = navController,
                accountInfoViewModel = accountInfoViewModel
            )
        }
        composable( // New Route for BirdInfoScreen
            route = Screen.BirdInfo.route,
            arguments = listOf(navArgument("speciesCode") { type = NavType.StringType })
        ) { // NavBackStackEntry is implicitly available to viewModel()
            val birdInfoViewModel: BirdInfoViewModel = viewModel()
            BirdInfoScreen(
                navController = navController,
                viewModel = birdInfoViewModel
            )
        }
    }
}