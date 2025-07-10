// EXE201/app/src/main/java/com/android/birdlens/data/repository/NotificationRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import com.android.birdlens.data.model.PaginatedNotificationsResponse
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import retrofit2.Response

class NotificationRepository(applicationContext: Context) {
    private val apiService: ApiService = RetrofitInstance.api(applicationContext)

    suspend fun getNotifications(limit: Int, offset: Int): Response<GenericApiResponse<PaginatedNotificationsResponse>> {
        return apiService.getNotifications(limit, offset)
    }
}