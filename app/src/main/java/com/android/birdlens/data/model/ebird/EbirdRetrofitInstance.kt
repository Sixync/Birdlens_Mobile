package com.android.birdlens.data.model.ebird

import com.android.birdlens.data.network.ebird.EbirdApiKeyInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object EbirdRetrofitInstance {
    private const val BASE_URL = "https://api.ebird.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Log request and response bodies
    }

    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(EbirdApiKeyInterceptor()) // Adds the X-eBirdApiToken header
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