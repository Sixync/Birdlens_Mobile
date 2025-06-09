// EXE201/app/src/main/java/com/android/birdlens/data/model/ebird/EbirdRetrofitInstance.kt
package com.android.birdlens.data.model.ebird

import com.android.birdlens.data.network.ebird.EbirdApiKeyInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object EbirdRetrofitInstance {
    private const val BASE_URL = "https://api.ebird.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Increased connect timeout
            .readTimeout(30, TimeUnit.SECONDS)    // Increased read timeout
            .writeTimeout(30, TimeUnit.SECONDS)   // Increased write timeout
            .addInterceptor(loggingInterceptor)
            .addInterceptor(EbirdApiKeyInterceptor())
            .build()
    }

    val api: EbirdApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EbirdApiService::class.java)
    }
}