// EXE201/app/src/main/java/com/android/birdlens/MainActivity.kt
package com.android.birdlens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.android.birdlens.data.LanguageManager
import com.android.birdlens.presentation.navigation.AppNavigation
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.GreenDeep
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
        private const val TAG_DEEPLINK = "MainActivityDeepLink"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adManager = AdManager(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
            // Handle initial intent in onCreate, after NavController is set up by setContent
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) { // Correct non-nullable Intent
        super.onNewIntent(intent)
        Log.d(TAG_DEEPLINK, "onNewIntent called with: $intent")
        setIntent(intent) // Update the activity's current intent to this new one
        handleIntent(intent) // Process the new intent
    }

    @SuppressLint("RestrictedApi")
    private fun handleIntent(intent: Intent?) {
        val currentIntent = intent ?: run {
            Log.d(TAG_DEEPLINK, "handleIntent: intent is null, exiting.")
            return
        }

        val action = currentIntent.action
        val data = currentIntent.data
        Log.d(TAG_DEEPLINK, "handleIntent: action=$action, data=$data")
        Log.d(TAG_DEEPLINK, "handleIntent: RAW DATA URI AS STRING: ${data?.toString()}") // Log the raw URI

        if (Intent.ACTION_VIEW == action && data != null) {
            val scheme = data.scheme
            val host = data.host
            val path = data.path
            Log.d(TAG_DEEPLINK, "Parsed from deep link - Scheme: $scheme, Host: $host, Path: $path")

            val expectedScheme = "birdlens"
            val expectedHost = "deeplink"
            val expectedPath = "/auth/confirm-email"
            Log.d(TAG_DEEPLINK, "Comparing with - Expected Scheme: $expectedScheme, Expected Host: $expectedHost, Expected Path: $expectedPath")


            if (scheme == expectedScheme && host == expectedHost && path == expectedPath) {
                Log.d(TAG_DEEPLINK, "Deep link structure MATCHED.")
                val token = data.getQueryParameter("token")
                val userId = data.getQueryParameter("user_id") // This is the standard Android way
                Log.d(TAG_DEEPLINK, "Extracted token: $token, userId: $userId")

                if (token != null && userId != null) {
                    // Check if navController is initialized. It should be if setContent has run.
                    if (::navController.isInitialized) {
                        Log.d(TAG_DEEPLINK, "NavController is initialized. Attempting to navigate to EmailVerificationScreen.")
                        Log.d(TAG_DEEPLINK, "Current NavController graph: ${navController.graph.route}, Start Destination: ${navController.graph.startDestinationRoute}")
                        Log.d(TAG_DEEPLINK, "Current NavController backstack: ${navController.currentBackStack.value.joinToString { it.destination.route ?: "null" }}")

                        navController.navigate(Screen.EmailVerification.createRoute(token, userId)) {
                            // Optional: Consider popping up to a known screen if needed, or launching as single top
                            // popUpTo(Screen.Welcome.route)
                            // launchSingleTop = true
                        }
                        Log.d(TAG_DEEPLINK, "Navigation to EmailVerificationScreen attempted.")
                    } else {
                        Log.e(TAG_DEEPLINK, "NavController NOT initialized when trying to handle deep link. This is unexpected if setContent has completed.")
                    }
                } else {
                    Log.w(TAG_DEEPLINK, "Token or userId missing in parsed deep link data. URI was: ${data.toString()}")
                }
            } else {
                Log.w(TAG_DEEPLINK, "Deep link structure MISMATCHED. URI received: ${data.toString()}")
            }
        } else {
            Log.d(TAG_DEEPLINK, "Intent is not ACTION_VIEW or data is null. Not a deep link for email verification.")
        }
    }


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
        // If the activity was launched by a deep link that wasn't processed in onCreate (e.g. complex launch scenarios),
        // you might consider calling handleIntent(intent) here too, but usually onCreate/onNewIntent cover it.
        // handleIntent(intent) // Potentially redundant, but can be a safeguard if onCreate's call is missed.
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