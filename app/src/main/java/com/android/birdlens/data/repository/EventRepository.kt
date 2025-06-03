// EXE201/app/src/main/java/com/android/birdlens/data/repository/EventRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import com.android.birdlens.data.model.Event // Ensure correct Event model import
import com.android.birdlens.data.model.PaginatedEventData
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import retrofit2.Response

class EventRepository(applicationContext: Context) {

    private val apiService: ApiService = RetrofitInstance.api(applicationContext)

    suspend fun getEvents(limit: Int, offset: Int): Response<GenericApiResponse<PaginatedEventData>> {
        return apiService.getEvents(limit, offset)
    }

    suspend fun getEventById(eventId: Long): Response<GenericApiResponse<Event>> { // New method
        return apiService.getEventById(eventId)
    }
}