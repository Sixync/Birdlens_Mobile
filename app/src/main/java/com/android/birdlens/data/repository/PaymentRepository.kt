package com.android.birdlens.data.repository

import android.content.Context
import com.android.birdlens.data.model.CreatePayOSLinkResponse
import com.android.birdlens.data.model.CreatePaymentRequest
import com.android.birdlens.data.model.response.GenericApiResponse
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import retrofit2.Response

class PaymentRepository(applicationContext: Context) {
    private val apiService: ApiService = RetrofitInstance.api(applicationContext)

    suspend fun createPayOSLink(request: CreatePaymentRequest): Response<GenericApiResponse<CreatePayOSLinkResponse>> {
        return apiService.createPayOSPaymentLink(request)
    }
}