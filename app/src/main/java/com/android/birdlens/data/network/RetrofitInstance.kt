// app/src/main/java/com/android/birdlens/data/network/RetrofitInstance.kt
package com.android.birdlens.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    // Replace with your actual backend URL.
    // For emulator accessing localhost of host machine, use 10.0.2.2
    private const val BASE_URL = "http://10.0.2.2/" // Or your deployed backend URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Logs request and response bodies
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // Add logging interceptor
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient) // Use the custom OkHttpClient
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}