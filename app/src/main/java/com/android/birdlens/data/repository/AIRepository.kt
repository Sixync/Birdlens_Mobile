// EXE201/app/src/main/java/com/android/birdlens/data/repository/AIRepository.kt
package com.android.birdlens.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.android.birdlens.data.model.AIIdentifyResponse
import com.android.birdlens.data.model.AIQuestionRequest
import com.android.birdlens.data.model.AIQuestionResponse
import com.android.birdlens.data.model.ChatMessage
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import com.android.birdlens.utils.FileUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

// Logic: This repository centralizes the logic for communicating with our backend's AI endpoints.
// It handles converting the bitmap to a file and constructing the necessary requests.
class AIRepository(private val applicationContext: Context) {
    private val apiService: ApiService = RetrofitInstance.api(applicationContext)
    private val fileUtil = FileUtil(applicationContext)

    suspend fun identifyBird(bitmap: Bitmap?, prompt: String): Response<GenericApiResponse<AIIdentifyResponse>> {
        val imagePart: MultipartBody.Part? = bitmap?.let {
            // Note: You need a method in FileUtil to convert a Bitmap to a File.
            val file = fileUtil.createFileFromBitmap(it, "identification_image.jpg")
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", file.name, requestFile)
        }

        val promptBody = prompt.toRequestBody("text/plain".toMediaTypeOrNull())

        return apiService.identifyBird(imagePart, promptBody)
    }

    suspend fun askQuestion(birdName: String, question: String, history: List<ChatMessage>): Response<GenericApiResponse<AIQuestionResponse>> {
        val request = AIQuestionRequest(birdName, question, history)
        return apiService.askAiQuestion(request)
    }
}