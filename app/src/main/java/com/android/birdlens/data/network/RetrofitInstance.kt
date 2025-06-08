// EXE201/app/src/main/java/com/android/birdlens/data/network/RetrofitInstance.kt
package com.android.birdlens.data.network

import android.content.Context // Required for AuthInterceptor instantiation
import com.android.birdlens.MainActivity // Or any other context provider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "http://10.0.2.2/"  // Or your deployed backend URL
        //http://20.191.153.166/
        //http://10.0.2.2/ localhost
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Function to create OkHttpClient, requires Context for AuthInterceptor
    fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(context.applicationContext)) // Add AuthInterceptor
            .build()
    }

    // ApiService instance needs to be initialized with context
    // This lazy initialization needs context. One way is to initialize it from Application class
    // or pass context when first accessed. For simplicity, assuming a context is available.
    // A better approach would be to use dependency injection (e.g., Hilt).

    // Lateinit var for ApiService, to be initialized with context
    private lateinit var apiService: ApiService

    fun getApiService(context: Context): ApiService {
        if (!::apiService.isInitialized) {
            synchronized(this) {
                if (!::apiService.isInitialized) {
                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(createOkHttpClient(context.applicationContext)) // Pass context here
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    apiService = retrofit.create(ApiService::class.java)
                }
            }
        }
        return apiService
    }

    // Old lazy val - this will cause issues if context is needed before MainActivity.
    // Keeping the structure but showing how to adapt if context is needed earlier.
    // For now, we will adapt how it's called in ViewModels.
    val api: ApiService by lazy {
        // This will crash if called before context is available for AuthInterceptor.
        // This is a placeholder, use getApiService(context) instead.
        // For a quick fix without major DI restructure, you'd pass context to viewmodels
        // that then pass to this.
        // However, for a robust solution, DI (Hilt) is recommended.
        // For this exercise, we'll modify ViewModels to call getApiService(context).
        throw IllegalStateException("ApiService not initialized with Context. Use getApiService(context) instead.")
    }

    // Simplified access for ViewModels that can get context
    // This is a common pattern if not using Hilt.
    // Make sure context passed is applicationContext to avoid leaks.
    fun api(context: Context): ApiService = getApiService(context.applicationContext)
}