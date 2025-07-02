// path: EXE201/app/src/main/java/com/android/birdlens/MainActivity.kt
// (complete file content here - full imports, package names, all code)
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdPolicyState {
    data object UNDETERMINED : AdPolicyState()
    data object ADS_ENABLED : AdPolicyState()
    data object ADS_DISABLED : AdPolicyState()
}


class MainActivity : ComponentActivity() {
    private val googleAuthViewModel: GoogleAuthViewModel by viewModels()
    private val accountInfoViewModel: AccountInfoViewModel by viewModels()
    private lateinit var adManager: AdManager

    private val _adPolicyState = MutableStateFlow<AdPolicyState>(AdPolicyState.UNDETERMINED)
    val adPolicyState: StateFlow<AdPolicyState> = _adPolicyState.asStateFlow()

    private lateinit var navController: NavHostController

    private var adTimerJob: Job? = null
    private var lastAdShownTimestamp: Long = 0

    companion object {
        private const val TAG_ADS = "MainActivityAds"
        private const val TAG_AUTH = "MainActivityAuth"
        private const val AD_TIMER_INTERVAL_MS = 3 * 60 * 1000L // 3 minutes
        private const val MIN_TIME_BETWEEN_ADS_MS = 60 * 1000L // 1 minute
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            BirdSpeciesRepository(applicationContext).populateDatabaseIfEmpty()
        }

        adManager = AdManager(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    Log.d(TAG_ADS, "Started collecting AccountInfoUiState for ad logic.")
                    accountInfoViewModel.uiState.collect { state ->
                        val newAdPolicy = when (state) {
                            is AccountInfoUiState.Success -> {
                                if (state.user.subscription == "ExBird") AdPolicyState.ADS_DISABLED else AdPolicyState.ADS_ENABLED
                            }
                            is AccountInfoUiState.Error -> AdPolicyState.ADS_ENABLED
                            is AccountInfoUiState.Idle, is AccountInfoUiState.Loading -> AdPolicyState.UNDETERMINED
                        }

                        if (_adPolicyState.value != newAdPolicy) {
                            Log.i(TAG_ADS, "Ad policy changing from ${_adPolicyState.value::class.simpleName} to ${newAdPolicy::class.simpleName}")
                            _adPolicyState.value = newAdPolicy
                        }

                        if (newAdPolicy == AdPolicyState.ADS_ENABLED) {
                            startAdTimer()
                        } else {
                            stopAdTimer()
                        }
                    }
                }

                launch {
                    AuthEventBus.events.collect { event ->
                        when (event) {
                            is AuthEvent.TokenExpiredOrInvalid -> {
                                if (navController.currentDestination?.route != Screen.Welcome.route) {
                                    Log.w(TAG_AUTH, "Token expired/invalid event received. Logging out user.")
                                    Toast.makeText(this@MainActivity, "Your session has expired. Please log in again.", Toast.LENGTH_LONG).show()
                                    accountInfoViewModel.onUserLoggedOut()
                                    googleAuthViewModel.signOut(applicationContext)
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = true
                                        }
                                        launchSingleTop = true
                                    }
                                } else {
                                    Log.d(TAG_AUTH, "Token expired event received, but already on or navigating to Welcome screen. Ignoring.")
                                }
                            }
                        }
                    }
                }
            }
        }

        setContent {
            navController = rememberNavController()
            BirdlensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenDeep
                ) {
                    AppNavigation(
                        navController = navController,
                        googleAuthViewModel = googleAuthViewModel,
                        accountInfoViewModel = accountInfoViewModel,
                        triggerAd = { triggerInterstitialAd() }
                    )
                }
            }
        }

        // Logic: Process the initial intent when the app is created.
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Logic: Process subsequent intents (e.g., if the app is already open).
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "app" && uri.host == "birdlens") {
            Log.d(TAG_AUTH, "Handling deep link: $uri")
            when (uri.path) {
                "/payment-success" -> {
                    // Navigate to a success screen. Pop up everything back to home first.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                    navController.navigate(Screen.PaymentResult.createRoute(true))
                }
                "/payment-cancel" -> {
                    // Navigate to a failure/cancelled screen.
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                    navController.navigate(Screen.PaymentResult.createRoute(false))
                }
                else -> {
                    // Handle other deep links like password reset if needed
                    Log.d(TAG_AUTH, "onNewIntent called with unhandled deep link path: ${uri.path}")
                }
            }
            // Clear the intent data so it's not processed again on configuration change.
            setIntent(null)
        }
    }


    private fun startAdTimer() {
        if (adTimerJob?.isActive == true) {
            return
        }
        Log.i(TAG_ADS, "Starting ad timer with ${AD_TIMER_INTERVAL_MS / 1000}s interval.")
        adTimerJob = lifecycleScope.launch {
            while (true) {
                delay(AD_TIMER_INTERVAL_MS)
                Log.d(TAG_ADS, "Ad timer fired. Triggering ad.")
                triggerInterstitialAd()
            }
        }
    }

    private fun stopAdTimer() {
        if (adTimerJob?.isActive == true) {
            Log.i(TAG_ADS, "Stopping ad timer.")
            adTimerJob?.cancel()
            adTimerJob = null
        }
    }

    fun triggerInterstitialAd() {
        if (System.currentTimeMillis() - lastAdShownTimestamp < MIN_TIME_BETWEEN_ADS_MS) {
            Log.d(TAG_ADS, "Ad trigger skipped: Not enough time has passed since the last ad.")
            return
        }

        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (_adPolicyState.value == AdPolicyState.ADS_ENABLED) {
                Log.d(TAG_ADS, "Interstitial ad triggered. Ads are currently enabled. Attempting to show.")
                stopAdTimer()
                adManager.showInterstitialAd(this@MainActivity) {
                    Log.d(TAG_ADS, "Ad flow finished. Restarting timer.")
                    lastAdShownTimestamp = System.currentTimeMillis()
                    if (_adPolicyState.value == AdPolicyState.ADS_ENABLED) {
                        startAdTimer()
                    }
                }
            } else {
                Log.d(TAG_ADS, "Interstitial ad trigger ignored: Ad policy is ${_adPolicyState.value::class.simpleName}.")
            }
        } else {
            Log.d(TAG_ADS, "Interstitial ad trigger ignored: Activity not in resumed state.")
        }
    }

    override fun onResume() {
        super.onResume()
        if (_adPolicyState.value == AdPolicyState.ADS_ENABLED) {
            startAdTimer()
        }
        Log.d(TAG_ADS, "onResume: Current ad policy state: ${_adPolicyState.value::class.simpleName}.")
    }

    override fun onPause() {
        super.onPause()
        stopAdTimer()
        Log.d(TAG_ADS, "onPause: Ad timer stopped.")
    }

    fun recreateActivity() {
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}