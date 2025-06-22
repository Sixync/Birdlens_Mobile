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
     * Logic: The function signature is simplified to reflect the hardcoded test.
     * It no longer takes parameters and just calls the parameter-less API function.
     */
    suspend fun getSpeciesRange(): Response<SpeciesRangeApiResponse> {
        return apiService.getSpeciesRange()
    }
}