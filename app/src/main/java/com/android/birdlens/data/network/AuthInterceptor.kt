// EXE201/app/src/main/java/com/android/birdlens/data/network/AuthInterceptor.kt
package com.android.birdlens.data.network

import android.content.Context
import com.android.birdlens.data.TokenManager
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

        return chain.proceed(requestBuilder.build())
    }
}