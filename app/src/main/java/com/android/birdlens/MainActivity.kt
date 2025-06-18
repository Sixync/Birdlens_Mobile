// EXE201/app/src/main/java/com/android/birdlens/MainActivity.kt
package com.android.birdlens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.admob.AdManager
import com.android.birdlens.data.AuthEvent
import com.android.birdlens.data.AuthEventBus
import com.android.birdlens.data.LanguageManager
import com.android.birdlens.data.repository.BirdSpeciesRepository
import com.android.birdlens.presentation.navigation.AppNavigation
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.GreenDeep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val googleAuthViewModel: GoogleAuthViewModel by viewModels()
    private val accountInfoViewModel: AccountInfoViewModel by viewModels()
    private lateinit var adManager: AdManager
    private val _shouldShowAds = MutableStateFlow(true)
    val shouldShowAds: StateFlow<Boolean> get() = _shouldShowAds.asStateFlow()

    private lateinit var navController: NavHostController

    companion object {
        private const val TAG_ADS = "MainActivityAds"
        // Logic: Removed the TAG_DEEPLINK as it's no longer used.
        private const val TAG_AUTH = "MainActivityAuth" // Tag for auth events
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Trigger bird species database population on a background thread
        lifecycleScope.launch(Dispatchers.IO) {
            BirdSpeciesRepository(applicationContext).populateDatabaseIfEmpty()
        }

        adManager = AdManager(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Ad logic collector
                launch {
                    Log.d(TAG_ADS, "Started collecting AccountInfoUiState for ad logic.")
                    accountInfoViewModel.uiState.collect { state ->
                        val previousShouldShowAds = _shouldShowAds.value
                        when (state) {
                            is AccountInfoUiState.Success -> {
                                val subscriptionName = state.user.subscription
                                _shouldShowAds.value = subscriptionName != "ExBird"
                                Log.d(TAG_ADS, "User subscription: '$subscriptionName'. Ads enabled: ${_shouldShowAds.value}")
                            }
                            is AccountInfoUiState.Error -> {
                                _shouldShowAds.value = true
                                Log.w(TAG_ADS, "Error fetching user profile: ${state.message}. Ads enabled by default.")
                            }
                            is AccountInfoUiState.Idle, is AccountInfoUiState.Loading -> {
                                Log.d(TAG_ADS, "User profile state: ${state::class.java.simpleName}. Current ads policy: ${_shouldShowAds.value}")
                                if (state is AccountInfoUiState.Idle && !previousShouldShowAds) {
                                    _shouldShowAds.value = true
                                    Log.d(TAG_ADS, "User logged out or session expired. Ads policy reset to true.")
                                }
                            }
                        }
                        if (previousShouldShowAds != _shouldShowAds.value) {
                            Log.d(TAG_ADS, "Ad policy changed. Ads enabled: ${_shouldShowAds.value}")
                        }
                    }
                }

                // Auth event collector for handling expired tokens
                launch {
                    AuthEventBus.events.collect { event ->
                        when (event) {
                            is AuthEvent.TokenExpiredOrInvalid -> {
                                Log.w(TAG_AUTH, "Token expired/invalid event received. Logging out user.")
                                Toast.makeText(this@MainActivity, "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show()
                                googleAuthViewModel.signOut(applicationContext)
                                navController.navigate(Screen.Welcome.route) {
                                    // Clear the entire back stack
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }
        }

        setContent {
            navController = rememberNavController() // NavController initialized here
            BirdlensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenDeep
                ) {
                    AppNavigation(
                        navController = navController,
                        googleAuthViewModel = googleAuthViewModel
                    )
                }
            }
            // Logic: Removed the call to handleIntent as it's no longer needed.
            // The NavController handles other deep links like password reset automatically.
        }
    }

    override fun onNewIntent(intent: Intent) { // Correct non-nullable Intent
        super.onNewIntent(intent)
        // Logic: The body of this method is now empty but the override is kept.
        // The Jetpack Navigation component handles deep links automatically when the activity is launched with a new intent.
        // No manual parsing is needed here anymore.
        Log.d(TAG_AUTH, "onNewIntent called with: $intent. Navigation Component will handle deep links.")
    }

    // Logic: The entire handleIntent method has been removed as it was only for email verification deep linking.

    fun triggerInterstitialAd() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (_shouldShowAds.value) {
                Log.d(TAG_ADS, "Interstitial ad triggered. Ads are currently enabled. Attempting to show.")
                adManager.showInterstitialAd(this@MainActivity) {
                    Log.d(TAG_ADS, "Ad flow finished for triggered ad.")
                }
            } else {
                Log.d(TAG_ADS, "Interstitial ad trigger ignored: Ads are disabled (e.g., ExBird subscription).")
            }
        } else {
            Log.d(TAG_ADS, "Interstitial ad trigger ignored: Activity not in resumed state.")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG_ADS, "onResume: Current shouldShowAds state: ${_shouldShowAds.value}. Ad display is event-driven.")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG_ADS, "onPause: Ad display is event-driven.")
    }

    fun recreateActivity() {
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}