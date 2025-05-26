// EXE201/app/src/main/java/com/android/birdlens/presentation/navigation/AppNavigation.kt
package com.android.birdlens.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.birdlens.presentation.ui.screens.allevents.AllEventsListScreen
import com.android.birdlens.presentation.ui.screens.alltours.AllToursListScreen
import com.android.birdlens.presentation.ui.screens.cart.CartScreen
import com.android.birdlens.presentation.ui.screens.community.CommunityScreen
import com.android.birdlens.presentation.ui.screens.login.LoginScreen
import com.android.birdlens.presentation.ui.screens.loginsuccess.LoginSuccessScreen
import com.android.birdlens.presentation.ui.screens.map.MapScreen
import com.android.birdlens.presentation.ui.screens.marketplace.MarketplaceScreen
import com.android.birdlens.presentation.ui.screens.register.RegisterScreen
import com.android.birdlens.presentation.ui.screens.settings.SettingsScreen
import com.android.birdlens.presentation.ui.screens.tour.TourScreen
import com.android.birdlens.presentation.ui.screens.tourdetail.TourDetailScreen
import com.android.birdlens.presentation.ui.screens.welcome.WelcomeScreen
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
// No need to import AppScaffold here, individual screens will use it.

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
            // WelcomeScreen will use AuthScreenLayout internally
            WelcomeScreen(
                onLoginClicked = { navController.navigate(Screen.Login.route) },
                onNewUserClicked = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Login.route) {
            // LoginScreen will use AuthScreenLayout internally
            LoginScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                onNavigateBack = { navController.popBackStack() },
                onForgotPassword = { /* TODO */ },
                onLoginWithFacebook = { /* Placeholder */ },
                onLoginWithX = { /* Placeholder */ },
                onLoginWithApple = { /* Placeholder */ }
            )
        }
        composable(Screen.Register.route) {
            // RegisterScreen will use AuthScreenLayout internally
            RegisterScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLoginWithFacebook = { /* Placeholder */ },
                onLoginWithX = { /* Placeholder */ },
                onLoginWithApple = { /* Placeholder */ }
            )
        }
        composable(Screen.LoginSuccess.route) {
            // LoginSuccessScreen can use AuthScreenLayout or a simple Box with SharedAppBackground
            LoginSuccessScreen(
                onContinue = {
                    navController.navigate(Screen.Tour.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // Screens using AppScaffold will take navController and use it internally
        composable(Screen.Tour.route) {
            TourScreen(
                navController = navController, // Pass navController to TourScreen
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
                    navController.navigate(Screen.TourDetail.createRoute(eventId)) // Assuming events also lead to a "detail" screen
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
    }
}