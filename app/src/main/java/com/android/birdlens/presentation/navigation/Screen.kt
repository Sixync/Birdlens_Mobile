// EXE201/app/src/main/java/com/android/birdlens/presentation/navigation/Screen.kt
package com.android.birdlens.presentation.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome_screen")
    data object Login : Screen("login_screen")
    data object Register : Screen("register_screen")
    data object LoginSuccess : Screen("login_success_screen")
    data object Tour : Screen("tour_screen")
    data object AllEventsList : Screen("all_events_list_screen")
    data object AllToursList : Screen("all_tours_list_screen")
    data object TourDetail : Screen("tour_detail_screen/{tourId}") { // New with argument
        fun createRoute(tourId: Long) = "tour_detail_screen/$tourId"
    }
    data object PickDays : Screen("pick_days_screen/{tourId}") { // New Screen
        fun createRoute(tourId: Long) = "pick_days_screen/$tourId"
    }
    data object Cart : Screen("cart_screen")
    data object Marketplace : Screen("marketplace_screen")
    data object Map : Screen("map_screen") // Added Map Screen
    data object Community : Screen("community_screen")
    data object Settings : Screen("settings_screen")
    data object AccountInfo : Screen("account_info_screen") // New Account Info Screen
    data object BirdInfo : Screen("bird_info_screen/{speciesCode}") { // New Bird Info Screen
        fun createRoute(speciesCode: String) = "bird_info_screen/$speciesCode"
    }
    data object HotspotBirdList : Screen("hotspot_bird_list_screen/{hotspotId}") {
        fun createRoute(hotspotId: String) = "hotspot_bird_list_screen/$hotspotId"
    }
    // Add other screens here as your app grows
    // data object MainApp : Screen("main_app_screen")
}