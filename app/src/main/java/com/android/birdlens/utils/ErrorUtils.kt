// EXE201/app/src/main/java/com/android/birdlens/utils/ErrorUtils.kt
package com.android.birdlens.utils

import android.util.Log
import org.json.JSONObject
import org.json.JSONException

object ErrorUtils {
    fun extractMessage(errorBody: String?, defaultMessagePrefix: String): String {
        if (errorBody.isNullOrBlank()) {
            return "$defaultMessagePrefix: Response body was empty."
        }
        try {
            val jsonObject = JSONObject(errorBody)
            if (jsonObject.has("message")) {
                return jsonObject.getString("message")
            }
            // Fallback for some APIs that might use "error" as the message string
            if (jsonObject.has("error") && jsonObject.get("error") is String) {
                return jsonObject.getString("error")
            }
            Log.w("ErrorUtils", "Error JSON found but no 'message' or string 'error' field. Defaulting. Body: $errorBody")
            return "$defaultMessagePrefix: An unspecified error occurred."
        } catch (e: JSONException) {
            Log.w("ErrorUtils", "Failed to parse error body as JSON. Assuming plain text. Body: $errorBody. Error: ${e.message}")
            return errorBody
        }
    }
}