// EXE201/app/src/main/java/com/android/birdlens/presentation/navigation/AppNavigation.kt
package com.android.birdlens.presentation.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.android.birdlens.presentation.ui.screens.comparison.HotspotComparisonScreen // Import the screen
import com.android.birdlens.presentation.ui.screens.community.CreatePostScreen
import com.android.birdlens.presentation.ui.screens.community.CommunityScreen
import com.android.birdlens.presentation.ui.screens.emailverification.EmailVerificationScreen
import com.android.birdlens.presentation.ui.screens.eventdetail.EventDetailScreen
import com.android.birdlens.presentation.ui.screens.forgotpassword.ForgotPasswordScreen
import com.android.birdlens.presentation.ui.screens.login.LoginScreen
import com.android.birdlens.presentation.ui.screens.loginsuccess.LoginSuccessScreen
import com.android.birdlens.presentation.ui.screens.map.MapScreen
import com.android.birdlens.presentation.ui.screens.marketplace.MarketplaceScreen
import com.android.birdlens.presentation.ui.screens.pickdays.PickDaysScreen
import com.android.birdlens.presentation.ui.screens.pleaseverify.PleaseVerifyEmailScreen
import com.android.birdlens.presentation.ui.screens.register.RegisterScreen
import com.android.birdlens.presentation.ui.screens.resetpassword.ResetPasswordScreen
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
import com.android.birdlens.presentation.viewmodel.CommunityViewModel
import com.android.birdlens.presentation.viewmodel.EventDetailViewModel
import com.android.birdlens.presentation.viewmodel.EventDetailViewModelFactory
import com.android.birdlens.presentation.viewmodel.ForgotPasswordViewModel
import com.android.birdlens.presentation.viewmodel.HotspotComparisonViewModel
import com.android.birdlens.presentation.viewmodel.HotspotComparisonViewModelFactory // Import the factory
import com.android.birdlens.presentation.viewmodel.MapViewModel
import com.android.birdlens.presentation.viewmodel.HotspotBirdListViewModel
import com.android.birdlens.presentation.viewmodel.TourViewModel


@Composable
fun AppNavigation(
    navController: NavHostController,
    googleAuthViewModel: GoogleAuthViewModel,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as Application
    val communityViewModel: CommunityViewModel = viewModel()

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
                onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ForgotPassword.route) {
            val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()
            ForgotPasswordScreen(navController = navController, viewModel = forgotPasswordViewModel)
        }
        composable(
            route = Screen.ResetPassword.route, // "reset_password_screen/{token}"
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
            // This deep link will capture the URL from the email and map the path segment to the 'token' argument.
            deepLinks = listOf(navDeepLink { uriPattern = "app://birdlens/reset-password/{token}" })
        ) { backStackEntry ->
            // The token is now automatically extracted from the deep link's path.
            val token = backStackEntry.arguments?.getString("token") ?: ""
            val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()
            ResetPasswordScreen(navController = navController, token = token, viewModel = forgotPasswordViewModel)
        }
        composable(Screen.LoginSuccess.route) {
            val accountInfoViewModel: AccountInfoViewModel = viewModel()
            LoginSuccessScreen(
                navController = navController,
                accountInfoViewModel = accountInfoViewModel
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
            val tourViewModel: TourViewModel = viewModel() // Added TourViewModel for this screen
            TourScreen(
                navController = navController,
                onNavigateToAllEvents = { navController.navigate(Screen.AllEventsList.route) },
                onNavigateToAllTours = { navController.navigate(Screen.AllToursList.route) },
                onNavigateToPopularTours = { navController.navigate(Screen.AllToursList.route) }, // Can be same as AllTours or a filtered view
                onTourItemClick = { tourIdAsLong -> // Changed from tourIdAsInt
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsLong))
                },
                onEventItemClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                eventViewModel = eventViewModel,
                tourViewModel = tourViewModel // Pass it here
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
            val tourViewModel: TourViewModel = viewModel() // Added TourViewModel
            AllToursListScreen(
                navController = navController,
                onTourItemClick = { tourIdAsLong ->
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsLong))
                },
                tourViewModel = tourViewModel // Pass it
            )
        }
        composable(
            route = Screen.TourDetail.route,
            arguments = listOf(navArgument("tourId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getLong("tourId") ?: -1L
            val tourViewModel: TourViewModel = viewModel() // Instantiate TourViewModel
            TourDetailScreen(navController = navController, tourId = tourId, tourViewModel = tourViewModel)
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
            CommunityScreen(navController = navController, communityViewModel = communityViewModel)
        }
        composable(Screen.CreatePost.route) {
            CreatePostScreen(navController = navController, communityViewModel = communityViewModel)
        }
        composable(Screen.Settings.route) {
            val accountInfoViewModel: AccountInfoViewModel = viewModel() // For subscription status in settings
            SettingsScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                accountInfoViewModel = accountInfoViewModel // Pass it
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
            // val eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L // Already handled by factory
            val eventDetailViewModel: EventDetailViewModel = viewModel(
                factory = EventDetailViewModelFactory(
                    application,
                    backStackEntry,
                    backStackEntry.arguments
                )
            )
            EventDetailScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L, // Still need to pass it for direct use if any
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
            // val hotspotId = backStackEntry.arguments?.getString("hotspotId") // Already handled by factory
            val hotspotBirdListViewModel: HotspotBirdListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { HotspotBirdListViewModel(createSavedStateHandle()) }
                }
            )
            HotspotBirdListScreen(
                navController = navController,
                hotspotId = backStackEntry.arguments?.getString("hotspotId"),
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
        // Corrected Composable for HotspotComparisonScreen
        composable(
            route = Screen.HotspotComparison.route, // Use the object directly
            arguments = listOf(navArgument("locIds") { type = NavType.StringType })
        ) { backStackEntry ->
            val locIdsString = backStackEntry.arguments?.getString("locIds")
            val locIdsList = locIdsString?.split(",")?.filter { it.isNotBlank() } ?: emptyList() // Ensure no blank IDs
            val comparisonViewModel: HotspotComparisonViewModel = viewModel(
                factory = HotspotComparisonViewModelFactory(application, locIdsList)
            )
            HotspotComparisonScreen( // Make sure this screen is imported
                navController = navController,
                viewModel = comparisonViewModel
            )
        }
    }
}