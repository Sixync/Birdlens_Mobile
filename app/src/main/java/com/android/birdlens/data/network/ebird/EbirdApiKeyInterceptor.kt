// Birdlens_Mobile\app\src\main\java\com\android\birdlens\data\network\ebird\EbirdApiKeyInterceptor.kt
package com.android.birdlens.data.network.ebird

import com.android.birdlens.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Protocol
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody

class EbirdApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = BuildConfig.EBIRD_API_KEY

        if (apiKey.isBlank() || apiKey == "YOUR_EBIRD_API_KEY_MISSING_IN_CONFIG") {
            // Log as an error for more visibility
            System.err.println("Error: eBird API key is missing or a placeholder. Request to ${originalRequest.url} will be blocked by interceptor.")
            // Return a synthetic error response instead of proceeding
            return Response.Builder()
                .request(originalRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(401) // Unauthorized
                .message("eBird API key is missing or invalid. Please configure it in local.properties.")
                .body("{\"error\":\"eBird API key not configured or is placeholder\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                .build()
        }

        val newRequest = originalRequest.newBuilder()
            .header("X-eBirdApiToken", apiKey)
            .build()
        return chain.proceed(newRequest)
    }
}