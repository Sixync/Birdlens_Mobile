// EXE201/app/src/main/java/com/android/birdlens/data/repository/TourRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import com.android.birdlens.data.model.PaginatedToursResponse
import com.android.birdlens.data.model.Tour
import com.android.birdlens.data.model.TourCreateRequest
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

class TourRepository(applicationContext: Context) {

    private val apiService: ApiService = RetrofitInstance.api(applicationContext)

    suspend fun getTours(limit: Int, offset: Int): Response<GenericApiResponse<PaginatedToursResponse>> {
        return apiService.getTours(limit, offset)
    }

    suspend fun getTourById(tourId: Long): Response<GenericApiResponse<Tour>> {
        return apiService.getTourById(tourId)
    }

    suspend fun createTour(tourCreateRequest: TourCreateRequest): Response<GenericApiResponse<Tour>> {
        // AuthInterceptor will add the token
        return apiService.createTour(tourCreateRequest)
    }

    suspend fun addTourImages(tourId: Long, imageFiles: List<File>): Response<GenericApiResponse<List<String>>> {
        val parts = imageFiles.map { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            // The backend iterates through all files, so the part name "images" should be fine
            // If specific part names were required for each file, this would need adjustment.
            MultipartBody.Part.createFormData("images", file.name, requestFile)
        }
        // AuthInterceptor will add the token
        return apiService.addTourImages(tourId, parts)
    }

    suspend fun addTourThumbnail(tourId: Long, thumbnailFile: File): Response<GenericApiResponse<String>> {
        val requestFile = thumbnailFile.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("thumbnail", thumbnailFile.name, requestFile)
        // AuthInterceptor will add the token
        return apiService.addTourThumbnail(tourId, part)
    }
}