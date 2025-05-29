// EXE201/app/src/main/java/com/android/birdlens/MainActivity.kt
package com.android.birdlens

import android.os.Bundle
// import android.widget.Toast // No longer directly needed for Google Play Services check here
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Keep if GoogleAuthViewModel is still used for email/pass
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.navigation.AppNavigation
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel // Keep for email/pass
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.GreenDeep
// import com.google.android.gms.common.GoogleApiAvailability // No longer needed here
// import com.google.android.gms.common.ConnectionResult // No longer needed here

class MainActivity : ComponentActivity() {
    // ViewModel is still needed for email/password auth and Firebase custom token flow
    private val googleAuthViewModel: GoogleAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        // Removed googleAuthViewModel.initialize(this) as it was for Google One-Tap setup.
        // The ViewModel is now an AndroidViewModel and gets its context automatically.

        setContent {
            BirdlensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenDeep
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        googleAuthViewModel = googleAuthViewModel // Pass the ViewModel
                    )
                }
            }
        }
    }
}