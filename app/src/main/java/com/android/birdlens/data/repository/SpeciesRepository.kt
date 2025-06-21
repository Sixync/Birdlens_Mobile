// EXE201/app/src/main/java/com/android/birdlens/data/repository/SpeciesRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import com.android.birdlens.data.model.SpeciesRangeApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import retrofit2.Response

/**
 * Repository for handling species-related data, such as distribution ranges.
 */
class SpeciesRepository(context: Context) {
    private val apiService: ApiService = RetrofitInstance.api(context)

    /**
     *
     * renamed the parameter for clarity. The function now takes a scientific name.
     */
    suspend fun getSpeciesRange(scientificName: String): Response<SpeciesRangeApiResponse> {
        return apiService.getSpeciesRange(scientificName)
    }
}