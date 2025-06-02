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
import com.android.birdlens.presentation.ui.screens.birdinfo.BirdInfoScreen
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
import com.android.birdlens.presentation.viewmodel.BirdInfoViewModel
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.presentation.ui.screens.hotspotbirdlist.HotspotBirdListScreen
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.presentation.viewmodel.HotspotBirdListViewModel

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
            arguments = listOf(navArgument("hotspotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hotspotId = backStackEntry.arguments?.getString("hotspotId")
            HotspotBirdListScreen(
                navController = navController,
                hotspotId = hotspotId ?: ""
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
            // TourScreen still uses its internal dummy TourItem which has Int ID
            // So, its onTourItemClick lambda provides an Int. We convert it to Long here.
            TourScreen(
                navController = navController,
                onNavigateToAllEvents = { navController.navigate(Screen.AllEventsList.route) },
                onNavigateToAllTours = { navController.navigate(Screen.AllToursList.route) },
                onNavigateToPopularTours = { navController.navigate(Screen.AllToursList.route) },
                onTourItemClick = { tourIdAsInt -> // tourIdAsInt is Int from TourScreen's dummy data
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsInt.toLong()))
                }
            )
        }
        composable(Screen.AllEventsList.route) {
            // AllEventsListScreen's onEventItemClick lambda provides a Long (from fetched Tour data)
            AllEventsListScreen(
                navController = navController,
                onEventItemClick = { eventIdAsLong -> // eventIdAsLong is Long
                    navController.navigate(Screen.TourDetail.createRoute(eventIdAsLong))
                }
            )
        }
        composable(Screen.AllToursList.route) {
            // AllToursListScreen's onTourItemClick lambda provides a Long (from fetched Tour data)
            AllToursListScreen(
                navController = navController,
                onTourItemClick = { tourIdAsLong -> // tourIdAsLong is Long
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsLong))
                }
            )
        }
        composable(
            route = Screen.TourDetail.route,
            arguments = listOf(navArgument("tourId") { type = NavType.LongType }) // Expect Long
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getLong("tourId") ?: -1L
            TourDetailScreen(navController = navController, tourId = tourId)
        }
        composable(
            route = Screen.PickDays.route,
            arguments = listOf(navArgument("tourId") { type = NavType.LongType }) // Expect Long
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getLong("tourId") ?: -1L
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
        composable(
            route = Screen.BirdInfo.route,
            arguments = listOf(navArgument("speciesCode") { type = NavType.StringType })
        ) {
            val birdInfoViewModel: BirdInfoViewModel = viewModel()
            BirdInfoScreen(
                navController = navController,
                viewModel = birdInfoViewModel
            )
        }
        composable(Screen.Map.route) {
            val mapViewModel: MapViewModel = viewModel() // Instance per MapScreen
            MapScreen(navController = navController, mapViewModel = mapViewModel)
        }

        composable(
            route = Screen.HotspotBirdList.route,
            arguments = listOf(navArgument("hotspotId") {
                type = NavType.StringType
                // nullable = true // If it can be null, handle in screen and VM
            })
        ) { backStackEntry ->
            // ViewModel will get hotspotId from SavedStateHandle
            val hotspotBirdListViewModel: HotspotBirdListViewModel = viewModel()
            HotspotBirdListScreen(
                navController = navController,
                hotspotId = backStackEntry.arguments?.getString("hotspotId"), // Pass for initial check, VM uses SavedStateHandle
                viewModel = hotspotBirdListViewModel
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