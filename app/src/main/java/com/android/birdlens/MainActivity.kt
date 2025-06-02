package com.android.birdlens

import android.content.Context // Crucial import
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
import com.android.birdlens.admob.AdManager // Import the AdManager
import com.android.birdlens.data.LanguageManager // Crucial import
import com.android.birdlens.presentation.navigation.AppNavigation
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.GreenDeep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val googleAuthViewModel: GoogleAuthViewModel by viewModels()
    private lateinit var adManager: AdManager
    private val adHandler = Handler(Looper.getMainLooper())
    private lateinit var adDisplayRunnable: Runnable

    companion object {
        // Interval for displaying ads: 5 minutes
        private const val AD_DISPLAY_INTERVAL_MS = 5 * 60 * 1000L
        // private const val AD_DISPLAY_INTERVAL_MS = 15 * 1000L // For testing: 15 seconds
        private const val TAG_ADS = "MainActivityAds"
    }

    override fun attachBaseContext(newBase: Context) {
        // This line applies the language preference BEFORE anything else in the Activity is set up.
        super.attachBaseContext(LanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdManager
        adManager = AdManager(applicationContext)

        // Define the runnable that will attempt to show an ad
        adDisplayRunnable = Runnable {
            Log.d(TAG_ADS, "Ad timer fired. Attempting to show ad.")
            // Ensure the activity is still in a state where ads can be shown (e.g., resumed)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                adManager.showInterstitialAd(this@MainActivity) {
                    // This callback is invoked when the ad is closed, failed to show, or was already showing.
                    // It's important to restart the timer for the next interval.
                    Log.d(TAG_ADS, "Ad flow finished (closed, failed, or skipped). Restarting timer.")
                    // Ensure the timer only restarts if the activity is still in a resumed state.
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        startAdTimer()
                    } else {
                        Log.d(TAG_ADS, "Activity not resumed. Not restarting ad timer from callback.")
                    }
                }
            } else {
                 Log.d(TAG_ADS, "Activity not resumed when ad timer fired. Not showing ad or restarting timer immediately.")
                 // The timer will be managed by onResume/onPause. If it was supposed to fire but activity isn't resumed,
                 // it means onPause likely already stopped it, or onResume will restart it.
            }
        }

        setContent {
            BirdlensTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GreenDeep // This is the base color of your app content area
                ) {
                    val navController = rememberNavController()
                    // AppNavigation, and thus WelcomeScreen, will use the context established by attachBaseContext.
                    AppNavigation(
                        navController = navController,
                        googleAuthViewModel = googleAuthViewModel
                    )
                }
            }
        }
    }

    private fun startAdTimer() {
        // Remove any existing callbacks to prevent multiple timers
        adHandler.removeCallbacks(adDisplayRunnable)
        // Post the new runnable with the defined interval
        adHandler.postDelayed(adDisplayRunnable, AD_DISPLAY_INTERVAL_MS)
        Log.d(TAG_ADS, "Ad timer started for ${AD_DISPLAY_INTERVAL_MS / 1000} seconds.")
    }

    private fun stopAdTimer() {
        adHandler.removeCallbacks(adDisplayRunnable)
        Log.d(TAG_ADS, "Ad timer stopped.")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG_ADS, "onResume: Starting ad display timer.")
        startAdTimer() // Start/restart the timer when the activity resumes
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG_ADS, "onPause: Stopping ad display timer.")
        stopAdTimer() // Stop the timer when the activity pauses
    }

    // This method is used for language changes
    fun recreateActivity() {
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}