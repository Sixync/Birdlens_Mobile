package com.android.birdlens.data.network.ebird

import com.android.birdlens.BuildConfig// Import BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class EbirdApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = BuildConfig.EBIRD_API_KEY

        if (apiKey.isBlank() || apiKey == "YOUR_EBIRD_API_KEY_MISSING_IN_CONFIG") {
            // Optionally, you could throw an exception or log a more severe warning
            // For now, proceed without the header if the key is missing,
            // which will likely result in an API error from eBird.
            println("Warning: eBird API key is missing or a placeholder. Requests to eBird API may fail.")
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("X-eBirdApiToken", apiKey)
            .build()
        return chain.proceed(newRequest)
    }
}