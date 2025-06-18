// EXE201/app/src/main/java/com/android/birdlens/data/network/AuthInterceptor.kt
package com.android.birdlens.data.network

import android.content.Context
import android.util.Log
import com.android.birdlens.data.AuthEvent
import com.android.birdlens.data.AuthEventBus
import com.android.birdlens.data.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(context: Context) : Interceptor {

    private val tokenManager = TokenManager.getInstance(context.applicationContext)

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        val token = tokenManager.getFirebaseIdToken()

        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        // Check for 401 Unauthorized response
        if (response.code == 401) {
            Log.w("AuthInterceptor", "Received 401 Unauthorized response from server. Signaling token expiration.")
            // Use runBlocking because intercept is not a suspend function.
            // This is a safe use case as it's just emitting to a SharedFlow, which is fast.
            runBlocking {
                AuthEventBus.postEvent(AuthEvent.TokenExpiredOrInvalid)
            }
        }

        return response
    }
}