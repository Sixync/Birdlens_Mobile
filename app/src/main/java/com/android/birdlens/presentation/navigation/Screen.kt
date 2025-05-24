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
        fun createRoute(tourId: Int) = "tour_detail_screen/$tourId"
    }
    data object Cart : Screen("cart_screen")
    data object Marketplace : Screen("marketplace_screen")
    data object Map : Screen("map_screen") // Added Map Screen
    data object Community : Screen("community_screen")
    data object Settings : Screen("settings_screen")
    // Add other screens here as your app grows
    // data object MainApp : Screen("main_app_screen")
}
