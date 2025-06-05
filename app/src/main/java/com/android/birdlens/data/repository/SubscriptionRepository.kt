// EXE201/app/src/main/java/com/android/birdlens/data/repository/SubscriptionRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import com.android.birdlens.data.model.CreateSubscriptionRequest
import com.android.birdlens.data.model.Subscription
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import retrofit2.Response

class SubscriptionRepository(applicationContext: Context) {

    private val apiService: ApiService = RetrofitInstance.api(applicationContext)

    suspend fun getSubscriptions(): Response<GenericApiResponse<List<Subscription>>> {
        return apiService.getSubscriptions()
    }

    suspend fun createSubscription(request: CreateSubscriptionRequest): Response<GenericApiResponse<Subscription>> {
        return apiService.createSubscription(request)
    }
}