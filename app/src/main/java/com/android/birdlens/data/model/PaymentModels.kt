// path: EXE201/app/src/main/java/com/android/birdlens/data/model/PaymentModels.kt
package com.android.birdlens.data.model

import com.google.gson.annotations.SerializedName

// A generic request body for initiating a payment. Can be used for Stripe and PayOS.
data class CreatePaymentRequest(
    @SerializedName("items") val items: List<PaymentItem>
)

data class PaymentItem(
    @SerializedName("id") val id: String
)

// Specific response from our backend for a PayOS link creation request.
data class CreatePayOSLinkResponse(
    @SerializedName("checkoutUrl") val checkoutUrl: String
)