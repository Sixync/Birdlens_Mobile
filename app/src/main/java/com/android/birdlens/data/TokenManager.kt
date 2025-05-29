// EXE201/app/src/main/java/com/android/birdlens/data/TokenManager.kt
package com.android.birdlens.data

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "birdlens_auth_prefs"
        private const val KEY_FIREBASE_ID_TOKEN = "firebase_id_token"

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
    }

    fun saveFirebaseIdToken(token: String?) {
        prefs.edit().putString(KEY_FIREBASE_ID_TOKEN, token).apply()
    }

    fun getFirebaseIdToken(): String? {
        return prefs.getString(KEY_FIREBASE_ID_TOKEN, null)
    }

    fun clearTokens() {
        prefs.edit().remove(KEY_FIREBASE_ID_TOKEN).apply()
    }
}