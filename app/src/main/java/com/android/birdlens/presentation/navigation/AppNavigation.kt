// app/src/main/java/com/android/birdlens/presentation/navigation/AppNavigation.kt
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
import com.android.birdlens.presentation.ui.screens.login.LoginScreen
import com.android.birdlens.presentation.ui.screens.loginsuccess.LoginSuccessScreen
import com.android.birdlens.presentation.ui.screens.map.MapScreen // Added import
import com.android.birdlens.presentation.ui.screens.marketplace.MarketplaceScreen
import com.android.birdlens.presentation.ui.screens.register.RegisterScreen
import com.android.birdlens.presentation.ui.screens.tour.TourScreen
import com.android.birdlens.presentation.ui.screens.tourdetail.TourDetailScreen
import com.android.birdlens.presentation.ui.screens.welcome.WelcomeScreen
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    googleAuthViewModel: GoogleAuthViewModel, // Added parameter
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
                onLoginSuccess = { // For traditional email/password login
                    navController.navigate(Screen.LoginSuccess.route) {
                        popUpTo(Screen.Welcome.route) // Keep Welcome on backstack
                    }
                },
                onForgotPassword = { /* TODO */ },
                // ... other login methods
                onLoginWithFacebook = {
                    navController.navigate(Screen.LoginSuccess.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                },
                onLoginWithX = {
                    navController.navigate(Screen.LoginSuccess.route) { popUpTo(Screen.Welcome.route) }
                },
                onLoginWithApple = {
                    navController.navigate(Screen.LoginSuccess.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegistrationSuccess = { // For traditional email/password registration
                    navController.navigate(Screen.Login.route) { // Navigate to Login after registration
                        popUpTo(Screen.Welcome.route)
                    }
                },
                // ... other register methods
                onLoginWithFacebook = {
                    navController.navigate(Screen.LoginSuccess.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                },
                onLoginWithX = {
                    navController.navigate(Screen.LoginSuccess.route) { popUpTo(Screen.Welcome.route) }
                },
                onLoginWithApple = {
                    navController.navigate(Screen.LoginSuccess.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                },
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
                    navController.navigate(Screen.TourDetail.createRoute(eventId)) // Can navigate to tour detail
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
            TourDetailScreen(
                navController = navController,
                tourId = tourId
            )
        }
        composable(Screen.Cart.route) {
            CartScreen(navController = navController)
        }
        composable(Screen.Marketplace.route) {
            MarketplaceScreen(navController = navController)
        }
        composable(Screen.Map.route) { // Added MapScreen route
            MapScreen(navController = navController)
        }
    }
}