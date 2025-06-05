// EXE201/app/src/main/java/com/android/birdlens/MainActivity.kt
package com.android.birdlens

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val accountInfoViewModel: AccountInfoViewModel by viewModels() // Added
    private lateinit var adManager: AdManager
    private val adHandler = Handler(Looper.getMainLooper())
    private lateinit var adDisplayRunnable: Runnable

    // State to control ad visibility based on subscription
    // Default to true (show ads) until subscription status is confirmed.
    private val _shouldShowAds = MutableStateFlow(true)
    private val shouldShowAds: StateFlow<Boolean> get() = _shouldShowAds.asStateFlow()


    companion object {
        private const val AD_DISPLAY_INTERVAL_MS = 5 * 60 * 1000L
        // private const val AD_DISPLAY_INTERVAL_MS = 15 * 1000L // For testing
        private const val TAG_ADS = "MainActivityAds"
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adManager = AdManager(applicationContext)

        adDisplayRunnable = Runnable {
            Log.d(TAG_ADS, "Ad timer fired. Current shouldShowAds state: ${_shouldShowAds.value}")
            if (_shouldShowAds.value && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                Log.d(TAG_ADS, "Attempting to show ad as per subscription status.")
                adManager.showInterstitialAd(this@MainActivity) {
                    // This callback is invoked when the ad is closed or failed to show.
                    Log.d(TAG_ADS, "Ad flow finished. Re-evaluating timer based on current ad policy.")
                    if (_shouldShowAds.value && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        startAdTimer() // Restart timer only if ads are still allowed
                    } else {
                        Log.d(TAG_ADS, "Ads are now disabled or activity not resumed. Not restarting timer.")
                    }
                }
            } else {
                Log.d(TAG_ADS, "Ad timer fired, but ads are currently disabled by subscription or activity state. Not showing ad.")
                // If ads are disabled, and the timer fired, it means _shouldShowAds was true when timer was last set.
                // No need to explicitly stop timer here, as startAdTimer checks _shouldShowAds.
            }
        }

        // Observe user's subscription status
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
                            _shouldShowAds.value = true // Show ads if profile fetch fails
                            Log.w(TAG_ADS, "Error fetching user profile: ${state.message}. Ads enabled by default.")
                        }
                        is AccountInfoUiState.Idle, is AccountInfoUiState.Loading -> {
                            // While loading or idle (before first fetch), keep current ad policy.
                            // Default is true, so ads will be prepared. If fetch determines ExBird, they'll be stopped.
                            Log.d(TAG_ADS, "User profile state: ${state::class.java.simpleName}. Current ads policy: ${_shouldShowAds.value}")
                            // If it's the very first load and state becomes Idle/Loading after being Error, ensure ads are on.
                            if (state is AccountInfoUiState.Idle && previousShouldShowAds == false) {
                                _shouldShowAds.value = true
                            }
                        }
                    }

                    // If the ad policy changed OR if the activity is resumed and ads should be shown
                    if (previousShouldShowAds != _shouldShowAds.value ||
                        (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && _shouldShowAds.value && previousShouldShowAds) // handles onResume when ads were already on
                    ) {
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (_shouldShowAds.value) {
                                Log.d(TAG_ADS, "Ad policy allows ads. Ensuring timer is (re)started.")
                                startAdTimer()
                            } else {
                                Log.d(TAG_ADS, "Ad policy disallows ads. Ensuring timer is stopped.")
                                stopAdTimer()
                            }
                        } else {
                            Log.d(TAG_ADS, "Ad policy updated, but activity not resumed. Timer will be handled by onResume/onPause.")
                        }
                    }
                }
            }
        }
        // Note: AccountInfoViewModel's init block calls fetchCurrentUser(), so no need to call it explicitly here
        // unless you want to force a refresh at a specific point.

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

    private fun startAdTimer() {
        // Always check the latest shouldShowAds state before actually starting
        if (!_shouldShowAds.value) {
            Log.d(TAG_ADS, "startAdTimer called, but shouldShowAds is currently false. Stopping timer instead.")
            stopAdTimer() // Ensure it's stopped if policy changed
            return
        }
        adHandler.removeCallbacks(adDisplayRunnable) // Remove existing to prevent duplicates
        adHandler.postDelayed(adDisplayRunnable, AD_DISPLAY_INTERVAL_MS)
        Log.d(TAG_ADS, "Ad timer (re)started for ${AD_DISPLAY_INTERVAL_MS / 1000} seconds.")
    }

    private fun stopAdTimer() {
        adHandler.removeCallbacks(adDisplayRunnable)
        Log.d(TAG_ADS, "Ad timer stopped.")
    }

    override fun onResume() {
        super.onResume()
        // When activity resumes, re-evaluate ad timer based on current known policy
        Log.d(TAG_ADS, "onResume: Current shouldShowAds state: ${_shouldShowAds.value}")
        if (_shouldShowAds.value) {
            startAdTimer()
        } else {
            stopAdTimer() // Ensure timer remains stopped if ads are disabled
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG_ADS, "onPause: Stopping ad display timer.")
        stopAdTimer() // Always stop timer when activity is paused
    }

    fun recreateActivity() {
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}