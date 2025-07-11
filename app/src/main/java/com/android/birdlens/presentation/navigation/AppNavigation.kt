// EXE201/app/src/main/java/com/android/birdlens/presentation/navigation/AppNavigation.kt
package com.android.birdlens.presentation.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.android.birdlens.presentation.ui.screens.birdrangemap.BirdRangeMapScreen
import com.android.birdlens.presentation.ui.screens.cart.CartScreen
import com.android.birdlens.presentation.ui.screens.comparison.HotspotComparisonScreen
import com.android.birdlens.presentation.ui.screens.community.CreatePostScreen
import com.android.birdlens.presentation.ui.screens.community.CommunityScreen
import com.android.birdlens.presentation.ui.screens.community.LocationPickerScreen
import com.android.birdlens.presentation.ui.screens.eventdetail.EventDetailScreen
import com.android.birdlens.presentation.ui.screens.forgotpassword.ForgotPasswordScreen
import com.android.birdlens.presentation.ui.screens.hotspotbirdlist.HotspotBirdListScreen
import com.android.birdlens.presentation.ui.screens.hotspotdetail.HotspotDetailScreen
import com.android.birdlens.presentation.ui.screens.login.LoginScreen
import com.android.birdlens.presentation.ui.screens.loginsuccess.LoginSuccessScreen
import com.android.birdlens.presentation.ui.screens.map.MapScreen
import com.android.birdlens.presentation.ui.screens.marketplace.MarketplaceScreen
// Logic: Import the new NotificationScreen.
import com.android.birdlens.presentation.ui.screens.notification.NotificationScreen
import com.android.birdlens.presentation.ui.screens.payment.PayOSCheckoutScreen
import com.android.birdlens.presentation.ui.screens.payment.PaymentResultScreen
import com.android.birdlens.presentation.ui.screens.pickdays.PickDaysScreen
import com.android.birdlens.presentation.ui.screens.pleaseverify.PleaseVerifyEmailScreen
import com.android.birdlens.presentation.ui.screens.premium.PremiumScreen
import com.android.birdlens.presentation.ui.screens.register.RegisterScreen
import com.android.birdlens.presentation.ui.screens.resetpassword.ResetPasswordScreen
import com.android.birdlens.presentation.ui.screens.settings.SettingsScreen
import com.android.birdlens.presentation.ui.screens.tour.TourScreen
import com.android.birdlens.presentation.ui.screens.tourdetail.TourDetailScreen
import com.android.birdlens.presentation.ui.screens.welcome.WelcomeScreen
import com.android.birdlens.presentation.viewmodel.*
import com.google.android.gms.maps.model.LatLng

@Composable
fun AppNavigation(
    navController: NavHostController,
    googleAuthViewModel: GoogleAuthViewModel,
    modifier: Modifier = Modifier,
    accountInfoViewModel: AccountInfoViewModel,
    triggerAd: () -> Unit
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
                onNewUserClicked = { navController.navigate(Screen.Register.route) },
                navController = navController
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
            route = Screen.ResetPassword.route,
            arguments = listOf(navArgument("token") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = "app://birdlens/reset-password/{token}" })
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: ""
            val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()
            ResetPasswordScreen(navController = navController, token = token, viewModel = forgotPasswordViewModel)
        }
        composable(Screen.LoginSuccess.route) {
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

        composable(Screen.Home.route) {
            val eventViewModel: EventViewModel = viewModel()
            val tourViewModel: TourViewModel = viewModel()
            val exploreViewModel: ExploreViewModel = viewModel() // Add this
            TourScreen(
                navController = navController,
                onNavigateToAllEvents = { navController.navigate(Screen.AllEventsList.route) },
                onNavigateToAllTours = { navController.navigate(Screen.AllToursList.route) },
                onNavigateToPopularTours = { navController.navigate(Screen.AllToursList.route) },
                onTourItemClick = { tourIdAsLong ->
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsLong))
                },
                onEventItemClick = { eventId ->
                    navController.navigate(Screen.EventDetail.createRoute(eventId))
                },
                eventViewModel = eventViewModel,
                tourViewModel = tourViewModel,
                exploreViewModel = exploreViewModel // Pass it here
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
            val tourViewModel: TourViewModel = viewModel()
            AllToursListScreen(
                navController = navController,
                onTourItemClick = { tourIdAsLong ->
                    navController.navigate(Screen.TourDetail.createRoute(tourIdAsLong))
                },
                tourViewModel = tourViewModel
            )
        }
        composable(
            route = Screen.TourDetail.route,
            arguments = listOf(navArgument("tourId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tourId = backStackEntry.arguments?.getLong("tourId") ?: -1L
            val tourViewModel: TourViewModel = viewModel()
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
        composable(
            route = Screen.CreatePost.route,
            arguments = listOf(
                navArgument("speciesCode") { type = NavType.StringType; nullable = true },
                navArgument("speciesName") { type = NavType.StringType; nullable = true },
                navArgument("imageUri") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            // Logic: Create a dedicated ViewModel for this screen.
            val createPostViewModel: CreatePostViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        CreatePostViewModel(
                            application = application,
                            savedStateHandle = createSavedStateHandle()
                        )
                    }
                }
            )
            // Logic: Observe the SavedStateHandle for results from the location picker.
            val locationResult = navController.currentBackStackEntry?.savedStateHandle?.get<LatLng>("picked_location")
            LaunchedEffect(locationResult) {
                locationResult?.let {
                    createPostViewModel.setLocation(it)
                    // Clear the result from the SavedStateHandle so it's not processed again
                    navController.currentBackStackEntry?.savedStateHandle?.remove<LatLng>("picked_location")
                }
            }

            CreatePostScreen(
                navController = navController,
                viewModel = createPostViewModel
            )
        }
        // Logic: Add the new LocationPickerScreen to the navigation graph.
        composable(Screen.LocationPicker.route) {
            LocationPickerScreen(navController = navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                googleAuthViewModel = googleAuthViewModel,
                accountInfoViewModel = accountInfoViewModel
            )
        }
        composable(Screen.Me.route) {
            AccountInfoScreen(
                navController = navController,
                accountInfoViewModel = accountInfoViewModel
            )
        }

        composable(
            route = Screen.EventDetail.route,
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventDetailViewModel: EventDetailViewModel = viewModel(
                factory = EventDetailViewModelFactory(
                    application,
                    backStackEntry,
                    backStackEntry.arguments
                )
            )
            EventDetailScreen(
                navController = navController,
                eventId = backStackEntry.arguments?.getLong("eventId") ?: -1L,
                eventDetailViewModel = eventDetailViewModel
            )
        }

        composable(
            route = Screen.BirdInfo.route,
            arguments = listOf(navArgument("speciesCode") { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                triggerAd()
            }
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
            val hotspotBirdListViewModel: HotspotBirdListViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { HotspotBirdListViewModel(createSavedStateHandle()) }
                }
            )
            HotspotBirdListScreen(
                navController = navController,
                viewModel = hotspotBirdListViewModel
            )
        }
        composable(
            route = Screen.HotspotDetail.route,
            arguments = listOf(navArgument("locId") { type = NavType.StringType })
        ) { backStackEntry ->
            val hotspotDetailViewModel: HotspotDetailViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { HotspotDetailViewModel(application, createSavedStateHandle()) }
                }
            )
            HotspotDetailScreen(
                navController = navController,
                viewModel = hotspotDetailViewModel
            )
        }

        composable(Screen.BirdIdentifier.route) {
            val birdIdentifierViewModel: BirdIdentifierViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        BirdIdentifierViewModel(application, createSavedStateHandle())
                    }
                }
            )
            BirdIdentifierScreen(
                navController = navController,
                viewModel = birdIdentifierViewModel,
                triggerAd = triggerAd
            )
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
        composable(
            route = Screen.HotspotComparison.route,
            arguments = listOf(navArgument("locIds") { type = NavType.StringType })
        ) { backStackEntry ->
            val locIdsString = backStackEntry.arguments?.getString("locIds")
            val locIdsList = locIdsString?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val comparisonViewModel: HotspotComparisonViewModel = viewModel(
                factory = HotspotComparisonViewModelFactory(application, locIdsList)
            )
            HotspotComparisonScreen(
                navController = navController,
                viewModel = comparisonViewModel
            )
        }
        composable(Screen.Premium.route) {
            val subscriptionViewModel: SubscriptionViewModel = viewModel()
            PremiumScreen(
                navController = navController,
                subscriptionViewModel = subscriptionViewModel,
                accountInfoViewModel = accountInfoViewModel
            )
        }

        // Logic: Add the new NotificationScreen to the navigation graph.
        composable(Screen.Notifications.route) {
            val notificationViewModel: NotificationViewModel = viewModel()
            NotificationScreen(
                navController = navController,
                notificationViewModel = notificationViewModel
            )
        }

        composable(
            route = Screen.PayOSCheckout.route,
            arguments = listOf(navArgument("checkoutUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val checkoutUrl = backStackEntry.arguments?.getString("checkoutUrl") ?: ""
            PayOSCheckoutScreen(navController = navController, checkoutUrl = checkoutUrl)
        }

        composable(
            route = Screen.PaymentResult.route,
            arguments = listOf(navArgument("isSuccess") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isSuccess = backStackEntry.arguments?.getBoolean("isSuccess") ?: false
            PaymentResultScreen(
                navController = navController,
                isSuccess = isSuccess,
                accountInfoViewModel = accountInfoViewModel
            )
        }
    }
}