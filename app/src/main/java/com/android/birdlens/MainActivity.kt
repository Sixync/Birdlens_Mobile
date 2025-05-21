package com.android.birdlens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.navigation.AppNavigation
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.GreenDeep
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult

class MainActivity : ComponentActivity() {
    private val googleAuthViewModel: GoogleAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check Google Play Services availability
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Toast.makeText(this, "This device doesn't support Google Play Services, which is required for Google Sign-In",
                    Toast.LENGTH_LONG).show()
            }
        }

        // Initialize GoogleAuthViewModel. This is where the ActivityResultLauncher is registered.
        googleAuthViewModel.initialize(this)

        setContent {
            BirdlensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenDeep
                ) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        googleAuthViewModel = googleAuthViewModel
                    )
                }
            }
        }
    }
}