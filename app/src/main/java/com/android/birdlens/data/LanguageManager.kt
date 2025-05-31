// EXE201/app/src/main/java/com/android/birdlens/data/LanguageManager.kt
package com.android.birdlens.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

object LanguageManager {

    private const val PREFS_NAME = "birdlens_lang_prefs"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_VIETNAMESE = "vi"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLanguagePreference(context: Context, languageCode: String) {
        getPreferences(context).edit().putString(KEY_SELECTED_LANGUAGE, languageCode).apply()
    }

    fun getLanguagePreference(context: Context): String {
        // Default to English if no preference is found
        return getPreferences(context).getString(KEY_SELECTED_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    /**
     * Sets the app's locale and returns a new context with this locale.
     * This should be used in Activity's attachBaseContext.
     * This method is crucial for ensuring the Activity and its Composables use the correct resources.
     */
    fun wrapContext(context: Context): Context {
        val savedLanguage = getLanguagePreference(context) // Reads saved lang using the original context
        val locale = Locale(savedLanguage)
        Locale.setDefault(locale) // Sets default locale for the entire application process

        val resources: Resources = context.resources
        val config: Configuration = Configuration(resources.configuration) // Create a mutable copy of the current configuration

        // Apply the new locale to the configuration
        // For API level N (24) and above (minSdk is 33, so this path is always taken)
        config.setLocale(locale)
        val localeList = android.os.LocaleList(locale)
        android.os.LocaleList.setDefault(localeList) // Sets default locale list for the app process
        config.setLocales(localeList)

        // Create and return a new context with the updated configuration.
        // This new context will provide resources (like strings) based on the new locale.
        return context.createConfigurationContext(config)
    }

    /**
     * Call this when the user actively changes the language in settings.
     * It saves the preference and then expects the calling Activity to recreate itself.
     */
    fun changeLanguage(context: Context, languageCode: String) {
        saveLanguagePreference(context, languageCode)
        // The activity needs to be recreated for the change to take full effect.
        // This is typically handled in the UI layer (e.g., SettingsScreen calls activity.recreate()).
        // No need to update locale here directly, as attachBaseContext will handle it on recreation.
    }
}