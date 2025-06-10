// EXE201/app/src/main/java/com/android/birdlens/presentation/navigation/Screen.kt
package com.android.birdlens.presentation.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome_screen")
    data object Login : Screen("login_screen")
    data object Register : Screen("register_screen")
    data object LoginSuccess : Screen("login_success_screen")
    data object PleaseVerifyEmail : Screen("please_verify_email_screen/{email}") {
        fun createRoute(email: String) = "please_verify_email_screen/$email"
    }
    data object Tour : Screen("tour_screen")
    data object AllEventsList : Screen("all_events_list_screen")

    data object EmailVerification : Screen("email_verification_screen?token={token}&user_id={user_id}") {
        fun createRoute(token: String, userId: String) = "email_verification_screen?token=$token&user_id=$userId"
    }
    data object AllToursList : Screen("all_tours_list_screen")
    data object TourDetail : Screen("tour_detail_screen/{tourId}") {
        fun createRoute(tourId: Long) = "tour_detail_screen/$tourId"
    }
    data object EventDetail : Screen("event_detail_screen/{eventId}") {
        fun createRoute(eventId: Long) = "event_detail_screen/$eventId"
    }
    data object PickDays : Screen("pick_days_screen/{tourId}") {
        fun createRoute(tourId: Long) = "pick_days_screen/$tourId"
    }
    data object Cart : Screen("cart_screen")
    data object Marketplace : Screen("marketplace_screen")
    data object Map : Screen("map_screen")
    data object Community : Screen("community_screen")
    data object Settings : Screen("settings_screen")
    data object AccountInfo : Screen("account_info_screen")
    data object BirdInfo : Screen("bird_info_screen/{speciesCode}") {
        fun createRoute(speciesCode: String) = "bird_info_screen/$speciesCode"
    }
    data object HotspotBirdList : Screen("hotspot_bird_list_screen/{hotspotId}") {
        fun createRoute(hotspotId: String) = "hotspot_bird_list_screen/$hotspotId"
    }

    data object BirdIdentifier : Screen("bird_identifier_screen")
    data object AdminSubscriptionList : Screen("admin_subscription_list_screen")
    data object AdminCreateSubscription : Screen("admin_create_subscription_screen")
}