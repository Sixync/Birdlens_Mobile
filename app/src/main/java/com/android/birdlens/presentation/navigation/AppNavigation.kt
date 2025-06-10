// EXE201/app/src/main/java/com/android/birdlens/presentation/navigation/AppNavigation.kt
package com.android.birdlens.presentation.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.android.birdlens.presentation.ui.screens.accountinfo.AccountInfoScreen
import com.android.birdlens.presentation.ui.screens.admin.subscriptions.AdminSubscriptionListScreen
import com.android.birdlens.presentation.ui.screens.admin.subscriptions.CreateSubscriptionScreen
import com.android.birdlens.presentation.ui.screens.allevents.AllEventsListScreen
import com.android.birdlens.presentation.ui.screens.alltours.AllToursListScreen
import com.android.birdlens.presentation.ui.screens.birdidentifier.BirdIdentifierScreen
import com.android.birdlens.presentation.ui.screens.birdinfo.BirdInfoScreen
import com.android.birdlens.presentation.ui.screens.cart.CartScreen
import com.android.birdlens.presentation.ui.screens.community.CommunityScreen
import com.android.birdlens.presentation.ui.screens.emailverification.EmailVerificationScreen
import com.android.birdlens.presentation.ui.screens.eventdetail.EventDetailScreen
import com.android.birdlens.presentation.ui.screens.login.LoginScreen
import com.android.birdlens.presentation.ui.screens.loginsuccess.LoginSuccessScreen
import com.android.birdlens.presentation.ui.screens.map.MapScreen
import com.android.birdlens.presentation.ui.screens.marketplace.MarketplaceScreen
import com.android.birdlens.presentation.ui.screens.pickdays.PickDaysScreen
import com.android.birdlens.presentation.ui.screens.pleaseverify.PleaseVerifyEmailScreen // Import new screen
import com.android.birdlens.presentation.ui.screens.register.RegisterScreen
import com.android.birdlens.presentation.ui.screens.settings.SettingsScreen
import com.android.birdlens.presentation.ui.screens.tour.TourScreen
import com.android.birdlens.presentation.ui.screens.tourdetail.TourDetailScreen
import com.android.birdlens.presentation.ui.screens.welcome.WelcomeScreen
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.presentation.viewmodel.BirdInfoViewModel
import com.android.birdlens.presentation.viewmodel.EventViewModel
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.presentation.ui.screens.hotspotbirdlist.HotspotBirdListScreen
import com.android.birdlens.presentation.viewmodel.AdminSubscriptionViewModel
import com.android.birdlens.presentation.viewmodel.BirdIdentifierViewModel
import com.android.birdlens.presentation.viewmodel.EventDetailViewModel
import com.android.birdlens.presentation.viewmodel.EventDetailViewModelFactory
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.presentation.viewmodel.HotspotBirdListViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    googleAuthViewModel: GoogleAuthViewModel,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application

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
        composable(Screen.Register.route) {
            // Pass the AccountInfoViewModel here if you decide to fetch user profile immediately after reg
            val accountInfoViewModel: AccountInfoViewModel = viewModel()
            RegisterScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.LoginSuccess.route) {
            // The AccountInfoViewModel is needed here to check verification status
            val accountInfoViewModel: AccountInfoViewModel = viewModel()
            LoginSuccessScreen(
                navController = navController, // Pass NavController
                accountInfoViewModel = accountInfoViewModel // Pass AccountInfoViewModel
            )
        }
        composable(
            route = Screen.PleaseVerifyEmail.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType; nullable = true })
        ) { backStackEntry ->
            PleaseVerifyEmailScreen(
                navController = navController,
                email = backStackEntry.arguments?.getString("email"),
                googleAuthViewModel = googleAuthViewModel
            )
        }

        composable(
            route = Screen.EmailVerification.route,
            arguments = listOf(
                navArgument("token") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("user_id") {
                    type = NavType.StringType
                    nullable = true
                }
            ),
            deepLinks = listOf(navDeepLink {
                uriPattern = "birdlens://deeplink/auth/confirm-email?token={token}&user_id={user_id}"
            })
        ) { backStackEntry ->
            EmailVerificationScreen(
                navController = navController,
                token = backStackEntry.arguments?.getString("token"),
                userId = backStackEntry.arguments?.getString("user_id"),
                googleAuthViewModel = googleAuthViewModel
            )
        }


        composable(Screen.Tour.route) {
            val eventViewModel: EventViewModel = viewModel()
            TourScreen(
                navController = navController,
                onNavigateToAllEvents = { navController.navigate(Screen.AllEventsList.route) },
                onNavigateToAllTours = { navController.navigate(Screen.AllToursList.route) },
                onNavigateToPopularTours = { navController.navigate(Screen.AllToursList.route) },
                onTourItemClick = { tourIdAsInt ->
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsInt.toLong()))
                },
                onEventItemClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                eventViewModel = eventViewModel
            )
        }
        composable(Screen.AllEventsList.route) {
            val eventViewModel: EventViewModel = viewModel()
            AllEventsListScreen(
                navController = navController,
                onEventItemClick = { eventIdAsLong ->
                    navController.navigate(Screen.EventDetail.createRoute(eventIdAsLong))
                },
                eventViewModel = eventViewModel
            )
        }
        composable(Screen.AllToursList.route) {
            AllToursListScreen(
                navController = navController,
                onTourItemClick = { tourIdAsLong ->
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsLong))
                }
            )
        }
        composable(
            route = Screen.TourDetail.route,
            arguments = listOf(navArgument("tourId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getLong("tourId") ?: -1L
            TourDetailScreen(navController = navController, tourId = tourId, tourViewModel = viewModel())
        }
        composable(
            route = Screen.PickDays.route,
            arguments = listOf(navArgument("tourId") { type = NavType.LongType })
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
            val mapViewModel: MapViewModel = viewModel()
            MapScreen(navController = navController, mapViewModel = mapViewModel)
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
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L
            val eventDetailViewModel: EventDetailViewModel = viewModel(
                factory = EventDetailViewModelFactory(
                    application,
                    backStackEntry,
                    backStackEntry.arguments
                )
            )
            EventDetailScreen(
                navController = navController,
                eventId = eventId,
                eventDetailViewModel = eventDetailViewModel
            )
        }

        composable(
            route = Screen.BirdInfo.route,
            arguments = listOf(navArgument("speciesCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val birdInfoViewModel: BirdInfoViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { BirdInfoViewModel(createSavedStateHandle()) }
                }
            )
            BirdInfoScreen(
                navController = navController,
                viewModel = birdInfoViewModel
            )
        }

        composable(
            route = Screen.HotspotBirdList.route,
            arguments = listOf(navArgument("hotspotId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hotspotId = backStackEntry.arguments?.getString("hotspotId")
            val hotspotBirdListViewModel: HotspotBirdListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { HotspotBirdListViewModel(createSavedStateHandle()) }
                }
            )
            HotspotBirdListScreen(
                navController = navController,
                hotspotId = hotspotId,
                viewModel = hotspotBirdListViewModel
            )
        }
        composable(Screen.BirdIdentifier.route) {
            val birdIdentifierViewModel: BirdIdentifierViewModel = viewModel()
            BirdIdentifierScreen(navController = navController, viewModel = birdIdentifierViewModel)
        }
        composable(Screen.AdminSubscriptionList.route) {
            val adminSubscriptionViewModel: AdminSubscriptionViewModel = viewModel()
            AdminSubscriptionListScreen(
                navController = navController,
                adminSubscriptionViewModel = adminSubscriptionViewModel
            )
        }
        composable(Screen.AdminCreateSubscription.route) {
            val adminSubscriptionViewModel: AdminSubscriptionViewModel = viewModel()
            CreateSubscriptionScreen(
                navController = navController,
                adminSubscriptionViewModel = adminSubscriptionViewModel
            )
        }
    }
}