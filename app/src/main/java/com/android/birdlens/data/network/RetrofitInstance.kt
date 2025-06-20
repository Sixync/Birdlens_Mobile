// EXE201/app/src/main/java/com/android/birdlens/data/network/RetrofitInstance.kt
package com.android.birdlens.data.network

import android.content.Context
import com.android.birdlens.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit // Import TimeUnit

object RetrofitInstance {
    // Logic: Remove the hardcoded BASE_URL. It will now come from BuildConfig.
    // private const val BASE_URL = "http://10.0.2.2/"

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
                        // Logic: Use the centralized URL from BuildConfig.
                        .baseUrl(BuildConfig.BACKEND_BASE_URL)
                        .client(createOkHttpClient(context.applicationContext))
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    apiService = retrofit.create(ApiService::class.java)
                }
            }
        }
        return apiService
    }

    val api: ApiService by lazy {
        throw IllegalStateException("ApiService not initialized with Context. Use getApiService(context) instead.")
    }

    fun api(context: Context): ApiService = getApiService(context.applicationContext)
}