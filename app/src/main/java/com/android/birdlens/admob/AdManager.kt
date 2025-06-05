// EXE201/app/src/main/java/com/android/birdlens/admob/AdManager.kt
package com.android.birdlens.admob

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val applicationContext: Context) {

    private var mInterstitialAd: InterstitialAd? = null
    private var isLoadingAd: Boolean = false
    private var isAdShowing: Boolean = false


    companion object {
        // This is a Test Ad Unit ID for Interstitial ads.
        // IMPORTANT: Replace with your actual Ad Unit ID from AdMob for production.
        private const val AD_UNIT_ID = "ca-app-pub-7643061698514707/2597978212"
        // Example of a real Ad Unit ID structure (replace with yours):
        // private const val AD_UNIT_ID = "ca-app-pub-YOUR_ADMOB_APP_ID/YOUR_INTERSTITIAL_AD_UNIT_ID"
        private const val TAG = "AdManager"
    }

    init {
        // Initialize the Mobile Ads SDK.
        // This should ideally be called only once, typically in an Application class's onCreate().
        MobileAds.initialize(applicationContext) { initializationStatus ->
            Log.d(TAG, "MobileAds initialization complete: $initializationStatus")
        }
        // Load the first ad.
        loadAd()
    }

    private fun loadAd() {
        if (isLoadingAd || mInterstitialAd != null) {
            Log.d(TAG, "Ad is already loaded or currently loading. Skipping loadAd().")
            return
        }
        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            applicationContext,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    isLoadingAd = false
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial ad failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    mInterstitialAd = null
                    isLoadingAd = false
                    // Optionally, retry loading after a delay here, or rely on external timer to call showAd again.
                }
            }
        )
    }

    /**
     * Shows the interstitial ad if it's loaded.
     * @param activity The current activity.
     * @param onAdFlowComplete A callback invoked when the ad is closed or fails to show.
     *                           This allows the caller (MainActivity) to decide if the timer should restart.
     */
    fun showInterstitialAd(activity: Activity, onAdFlowComplete: () -> Unit) { // Renamed callback
        if (isAdShowing) {
            Log.d(TAG, "An ad is already showing. Skipping new ad request.")
            onAdFlowComplete() // Signal completion so timer logic can be re-evaluated by caller
            return
        }

        if (mInterstitialAd != null) {
            isAdShowing = true
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed by the user.
                    Log.d(TAG, "Ad was dismissed.")
                    mInterstitialAd = null // The ad is used, clear its reference.
                    isAdShowing = false
                    loadAd() // Preload the next ad.
                    onAdFlowComplete() // Signal that the ad flow is complete for this instance.
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    isAdShowing = false
                    loadAd() // Attempt to load a new ad.
                    onAdFlowComplete() // Signal that the ad flow is complete for this instance.
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.")
                    // isAdShowing is true, mInterstitialAd will be set to null in onAdDismissedFullScreenContent.
                }

                override fun onAdImpression() {
                    super.onAdImpression()
                    Log.d(TAG, "Ad impression recorded.")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    Log.d(TAG, "Ad was clicked.")
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad wasn't ready. Attempting to load.")
            loadAd() // Try to load an ad if it's not available for the next cycle.
            onAdFlowComplete() // Still call this to allow the timer to be re-evaluated by caller.
        }
    }
}