// EXE201/app/src/main/java/com/android/birdlens/data/network/RetrofitInstance.kt
package com.android.birdlens.data.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit // Import TimeUnit

object RetrofitInstance {
    private const val BASE_URL = "http://20.191.153.166/"
    //http://20.191.153.166/
    //http://10.0.2.2/ localhost
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Increased connect timeout to 30 seconds
            .readTimeout(30, TimeUnit.SECONDS)    // Increased read timeout to 30 seconds
            .writeTimeout(30, TimeUnit.SECONDS)   // Increased write timeout to 30 seconds
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .build()
    }

    private lateinit var apiService: ApiService

    fun getApiService(context: Context): ApiService {
        if (!::apiService.isInitialized) {
            synchronized(this) {
                if (!::apiService.isInitialized) {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(createOkHttpClient(context.applicationContext))
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    apiService = retrofit.create(ApiService::class.java)
                }
            }
        }
        return apiService
    }

    // This lazy val is problematic if context is needed before MainActivity (e.g. in Application class for init)
    // However, your current usage of api(context) should be fine.
    val api: ApiService by lazy {
        throw IllegalStateException("ApiService not initialized with Context. Use getApiService(context) instead.")
    }

    fun api(context: Context): ApiService = getApiService(context.applicationContext)
}