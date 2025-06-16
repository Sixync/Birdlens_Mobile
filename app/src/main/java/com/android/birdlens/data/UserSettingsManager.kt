package com.android.birdlens.data

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder // Import Geocoder
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.io.IOException // Import IOException
import java.util.Locale

data class CountrySetting(
    val code: String,       // ISO 3166-1 alpha-2 country code (e.g., "VN", "US")
    val name: String,       // User-friendly name
    val center: LatLng,     // Default center for this country
    val zoom: Float         // Default zoom level for this country
)

object UserSettingsManager {
    private const val PREFS_NAME = "birdlens_user_settings_prefs"
    private const val KEY_HOME_COUNTRY_CODE = "home_country_code"
    private const val TAG = "UserSettingsManager"

    val VIETNAM_SETTINGS = CountrySetting("VN", "Vietnam", LatLng(16.047079, 108.220825), 5.5f)
    val USA_SETTINGS = CountrySetting("US", "United States", LatLng(39.8283, -98.5795), 3.5f)
    val UK_SETTINGS = CountrySetting("GB", "United Kingdom", LatLng(54.0, -2.0), 5.0f)
    val JAPAN_SETTINGS = CountrySetting("JP", "Japan", LatLng(36.2048, 138.2529), 5.0f)
    val AUSTRALIA_SETTINGS = CountrySetting("AU", "Australia", LatLng(-25.2744, 133.7751), 3.5f)


    val PREDEFINED_COUNTRIES: List<CountrySetting> = listOf(
        VIETNAM_SETTINGS,
        USA_SETTINGS,
        UK_SETTINGS,
        JAPAN_SETTINGS,
        AUSTRALIA_SETTINGS
    ).sortedBy { it.name }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveHomeCountryCode(context: Context, countryCode: String) {
        getPreferences(context).edit().putString(KEY_HOME_COUNTRY_CODE, countryCode).apply()
        Log.d(TAG, "Saved home country code: $countryCode")
    }

    fun getHomeCountrySetting(context: Context): CountrySetting {
        val savedCode = getPreferences(context).getString(KEY_HOME_COUNTRY_CODE, VIETNAM_SETTINGS.code)
        Log.d(TAG, "Retrieved home country code: $savedCode")
        return PREDEFINED_COUNTRIES.find { it.code == savedCode } ?: VIETNAM_SETTINGS
    }

    /**
     * Gets a country code from LatLng using Android's Geocoder.
     * This can be network-dependent and might fail if geocoding services are unavailable.
     * It falls back to the user's home country setting or a default if geocoding fails.
     */
    fun getCountryCodeFromLatLng(context: Context, latLng: LatLng): String {
        if (!Geocoder.isPresent()) {
            Log.w(TAG, "Geocoder not present on this device. Falling back for country code.")
            return getHomeCountrySetting(context).code // Fallback
        }
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            // Modern Geocoder.getFromLocation takes a listener for Android S+
            // For simplicity and broader compatibility, using the older synchronous version here.
            // Consider using the asynchronous version for better performance on newer APIs if this becomes a bottleneck.
            @Suppress("DEPRECATION") // Using deprecated getFromLocation for wider compatibility for now
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val countryCode = addresses[0].countryCode // ISO 3166-1 alpha-2
                if (!countryCode.isNullOrBlank()) {
                    Log.d(TAG, "Geocoder successfully returned country code: $countryCode for $latLng")
                    return countryCode
                } else {
                    Log.w(TAG, "Geocoder returned null or blank country code for $latLng.")
                }
            } else {
                Log.w(TAG, "Geocoder returned no addresses for $latLng.")
            }
        } catch (e: IOException) {
            // This can happen due to network issues or if the geocoder service is unavailable
            Log.e(TAG, "Geocoder failed to get country code for $latLng due to IOException: ${e.message}")
        } catch (e: IllegalArgumentException) {
            // Invalid latitude or longitude
            Log.e(TAG, "Geocoder failed due to invalid coordinates $latLng: ${e.message}")
        }  catch (e: Exception) {
            // Catch any other unexpected exceptions from geocoder
            Log.e(TAG, "Unexpected error during geocoding for $latLng: ${e.message}", e)
        }

        Log.w(TAG, "Falling back to home country setting or default for country code from LatLng.")
        return getHomeCountrySetting(context).code // Fallback to user's home country or default
    }
}