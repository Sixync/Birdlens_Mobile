// EXE201/app/src/main/java/com/android/birdlens/MainActivity.kt
package com.android.birdlens

import android.content.Context
import android.os.Bundle
// import android.os.Handler // No longer needed for fixed timer
// import android.os.Looper // No longer needed for fixed timer
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
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.admob.AdManager
import com.android.birdlens.data.LanguageManager
import com.android.birdlens.presentation.navigation.AppNavigation
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
    // private val adHandler = Handler(Looper.getMainLooper()) // Removed: Timer-based handler
    // private lateinit var adDisplayRunnable: Runnable // Removed: Timer-based runnable

    // State to control ad visibility based on subscription
    // Default to true (show ads) until subscription status is confirmed.
    // If confirmed as "ExBird", this will be set to false.
    private val _shouldShowAds = MutableStateFlow(true)
    val shouldShowAds: StateFlow<Boolean> get() = _shouldShowAds.asStateFlow()


    companion object {
        // Removed: AD_DISPLAY_INTERVAL_MS as fixed timer is gone
        // private const val AD_DISPLAY_INTERVAL_MS = 5 * 60 * 1000L
        private const val TAG_ADS = "MainActivityAds"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adManager = AdManager(applicationContext) // AdManager loads an ad on init

        // Removed: adDisplayRunnable initialization and timer logic
        // adDisplayRunnable = Runnable { ... }

        // Observe user's subscription status
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG_ADS, "Started collecting AccountInfoUiState for ad logic.")
                accountInfoViewModel.uiState.collect { state ->
                    val previousShouldShowAds = _shouldShowAds.value
                    when (state) {
                        is AccountInfoUiState.Success -> {
                            val subscriptionName = state.user.subscription
                            // Key logic: Disable ads if user has "ExBird" subscription
                            _shouldShowAds.value = subscriptionName != "ExBird"
                            Log.d(TAG_ADS, "User subscription: '$subscriptionName'. Ads enabled: ${_shouldShowAds.value}")
                        }
                        is AccountInfoUiState.Error -> {
                            _shouldShowAds.value = true // Default to showing ads if profile fetch fails
                            Log.w(TAG_ADS, "Error fetching user profile: ${state.message}. Ads enabled by default.")
                        }
                        is AccountInfoUiState.Idle, is AccountInfoUiState.Loading -> {
                            // While loading or idle (before first fetch), maintain current ad policy.
                            // If default is 'true', ads might be prepared.
                            // If fetching user status takes time, an ad *could* be shown if triggered *before* status is confirmed.
                            // This is why event-driven triggers are better than an immediate startup timer.
                            // For initial load, consider _shouldShowAds = false until status is confirmed,
                            // or ensure no ad trigger happens before confirmation.
                            // For now, we stick to the default of true, and rely on event-driven triggers not firing too early.
                            Log.d(TAG_ADS, "User profile state: ${state::class.java.simpleName}. Current ads policy: ${_shouldShowAds.value}")
                            if (state is AccountInfoUiState.Idle && !previousShouldShowAds) {
                                // This case: If user was ExBird (ads off), then logs out (Idle state from ViewModel), turn ads back on.
                                _shouldShowAds.value = true
                                Log.d(TAG_ADS, "User logged out or session expired. Ads policy reset to true.")
                            }
                        }
                    }

                    // This section originally managed the timer.
                    // Now, it just logs the policy change. The actual ad showing is event-driven.
                    if (previousShouldShowAds != _shouldShowAds.value) {
                        Log.d(TAG_ADS, "Ad policy changed. Ads enabled: ${_shouldShowAds.value}")
                        // If ads were just disabled, any currently showing ad will complete. Future ads won't show.
                        // If ads were just enabled, they will only show when triggered by an event.
                    }
                }
            }
        }

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
                        // Pass 'this' (MainActivity instance) or a callback to trigger ads if needed by deeper screens
                        // e.g., mainActivity = this
                    )
                }
            }
        }
    }

    /**
     * Call this method from various points in your application (e.g., after specific user actions,
     * navigating from certain screens) to attempt showing an interstitial ad.
     * The ad will only be shown if the user is not subscribed ("ExBird") and an ad is loaded.
     */
    fun triggerInterstitialAd() {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (_shouldShowAds.value) {
                Log.d(TAG_ADS, "Interstitial ad triggered. Ads are currently enabled. Attempting to show.")
                adManager.showInterstitialAd(this@MainActivity) {
                    // This callback is invoked when the ad is closed or failed to show.
                    // AdManager's internal logic already calls loadAd() to preload the next one.
                    Log.d(TAG_ADS, "Ad flow finished for triggered ad.")
                }
            } else {
                Log.d(TAG_ADS, "Interstitial ad trigger ignored: Ads are disabled (e.g., ExBird subscription).")
            }
        } else {
            Log.d(TAG_ADS, "Interstitial ad trigger ignored: Activity not in resumed state.")
        }
    }


    // Removed: startAdTimer() and stopAdTimer() as they were tied to the fixed interval timer.
    // The AdManager handles preloading ads. Showing ads is now event-driven via triggerInterstitialAd().

    override fun onResume() {
        super.onResume()
        // No timer to restart here. AdManager handles its own preloading.
        // You might want to log or check ad readiness if specific onResume ad display is ever needed.
        Log.d(TAG_ADS, "onResume: Current shouldShowAds state: ${_shouldShowAds.value}. Ad display is event-driven.")
    }

    override fun onPause() {
        super.onPause()
        // No timer to stop here.
        Log.d(TAG_ADS, "onPause: Ad display is event-driven.")
    }

    fun recreateActivity() {
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}