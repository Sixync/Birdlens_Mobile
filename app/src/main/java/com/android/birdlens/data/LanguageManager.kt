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
    // Logic: Add a new key to track if the user has ever set a language.
    // This will determine if the initial language selection screen should be shown.
    private const val KEY_LANGUAGE_HAS_BEEN_SET = "language_has_been_set"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_VIETNAMESE = "vi"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Logic: A new function to check if the language has been set at least once.
    fun hasLanguageBeenSet(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_LANGUAGE_HAS_BEEN_SET, false)
    }

    fun saveLanguagePreference(context: Context, languageCode: String) {
        getPreferences(context).edit()
            .putString(KEY_SELECTED_LANGUAGE, languageCode)
            .putBoolean(KEY_LANGUAGE_HAS_BEEN_SET, true) // Also mark that the language has been set.
            .apply()
    }

    fun getLanguagePreference(context: Context): String {
        return getPreferences(context).getString(KEY_SELECTED_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }

    fun wrapContext(context: Context): Context {
        val savedLanguage = getLanguagePreference(context)
        val locale = Locale(savedLanguage)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = Configuration(resources.configuration)

        config.setLocale(locale)
        val localeList = android.os.LocaleList(locale)
        android.os.LocaleList.setDefault(localeList)
        config.setLocales(localeList)

        return context.createConfigurationContext(config)
    }

    fun changeLanguage(context: Context, languageCode: String) {
        saveLanguagePreference(context, languageCode)
    }
}