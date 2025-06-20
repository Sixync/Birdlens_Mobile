// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/payment/CheckoutActivity.kt
package com.android.birdlens.presentation.ui.screens.payment

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.birdlens.BuildConfig
import com.android.birdlens.data.network.RetrofitInstance
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.GreenDeep
import com.android.birdlens.ui.theme.TextWhite
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CheckoutActivity : ComponentActivity() {
    companion object {
        // Logic: Use the centralized URL from BuildConfig instead of a hardcoded constant.
        private val BACKEND_URL = BuildConfig.BACKEND_BASE_URL
        private const val TAG = "CheckoutActivity"
    }

    private lateinit var okHttpClient: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        okHttpClient = RetrofitInstance.createOkHttpClient(applicationContext)

        setContent {
            BirdlensTheme {
                CheckoutScreenWrapper(onNavigateBack = { finish() })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CheckoutScreenWrapper(onNavigateBack: () -> Unit) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Complete Payment", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = GreenDeep
                    )
                )
            },
            containerColor = GreenDeep
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CheckoutScreenContent()
            }
        }
    }


    @Composable
    private fun CheckoutScreenContent() {
        var paymentIntentClientSecret by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        val paymentSheet = rememberPaymentSheet { paymentResult ->
            isLoading = false
            when (paymentResult) {
                is PaymentSheetResult.Completed -> showToast("Payment complete!")
                is PaymentSheetResult.Canceled -> showToast("Payment canceled!")
                is PaymentSheetResult.Failed -> {
                    error = paymentResult.error.localizedMessage ?: "Payment failed: Unknown error"
                    Log.e(TAG, "PaymentSheetResult.Failed: ${paymentResult.error.message}", paymentResult.error)
                }
            }
        }

        error?.let { errorMessage ->
            ErrorAlert(
                errorMessage = errorMessage,
                onDismiss = { error = null }
            )
        }

        LaunchedEffect(Unit) {
            isLoading = true
            fetchPaymentIntent().onSuccess { clientSecret ->
                paymentIntentClientSecret = clientSecret
                isLoading = false
            }.onFailure { paymentIntentError ->
                error = paymentIntentError.localizedMessage ?: "Could not retrieve payment details."
                Log.e(TAG, "fetchPaymentIntent error: ${paymentIntentError.message}", paymentIntentError)
                isLoading = false
            }
        }

        if (isLoading) {
            CircularProgressIndicator(color = TextWhite)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Preparing your payment...", color = TextWhite)
        } else {
            PayButton(
                enabled = paymentIntentClientSecret != null && !isLoading,
                onClick = {
                    paymentIntentClientSecret?.let {
                        isLoading = true
                        onPayClicked(
                            paymentSheet = paymentSheet,
                            paymentIntentClientSecret = it,
                        )
                    } ?: run {
                        error = "Payment details not available. Please try again."
                    }
                }
            )
        }
    }

    @Composable
    private fun PayButton(
        enabled: Boolean,
        onClick: () -> Unit
    ) {
        Button(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = enabled,
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
        ) {
            Text("Pay Now", color = TextWhite)
        }
    }

    @Composable
    private fun ErrorAlert(
        errorMessage: String,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Error") },
            text = { Text(text = errorMessage) },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(text = "Ok")
                }
            }
        )
    }

    private suspend fun fetchPaymentIntent(): Result<String> = suspendCoroutine { continuation ->
        // Logic: Correctly form the full URL using the centralized base URL.
        val url = "$BACKEND_URL/create-payment-intent".replace("//create", "/create")

        val shoppingCartContent = """
         {
             "items": [
                 {"id":"sub_premium"}
             ]
         }
     """

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = shoppingCartContent.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        Log.d(TAG, "Fetching PaymentIntent from: $url with body: $shoppingCartContent")

        okHttpClient.newCall(request)
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "OkHttp onFailure: ${e.message}", e)
                    continuation.resume(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "OkHttp onResponse not successful: ${response.code} - $errorBody")
                        continuation.resume(Result.failure(Exception("Server error: ${response.code} - $errorBody")))
                    } else {
                        val clientSecret = extractClientSecretFromResponse(response)
                        clientSecret?.let { secret ->
                            Log.d(TAG, "OkHttp onResponse success, clientSecret: ${secret.take(10)}...")
                            continuation.resume(Result.success(secret))
                        } ?: run {
                            val errorMsg = "Could not find payment intent client secret in response!"
                            Log.e(TAG, errorMsg)
                            continuation.resume(Result.failure(Exception(errorMsg)))
                        }
                    }
                }
            })
    }

    private fun extractClientSecretFromResponse(response: Response): String? {
        return try {
            val responseData = response.body?.string()
            Log.d(TAG, "Raw response for clientSecret extraction: $responseData")
            if (responseData.isNullOrBlank()) {
                Log.e(TAG, "Response data for clientSecret extraction is null or blank.")
                return null
            }
            val responseJson = JSONObject(responseData)
            responseJson.getString("clientSecret")
        } catch (e: JSONException) {
            Log.e(TAG, "JSONException during clientSecret extraction: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Generic exception during clientSecret extraction: ${e.message}", e)
            null
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this,  message, Toast.LENGTH_LONG).show()
        }
    }

    private fun onPayClicked(
        paymentSheet: PaymentSheet,
        paymentIntentClientSecret: String,
    ) {
        val configuration = PaymentSheet.Configuration.Builder("Birdlens, Inc.")
            .allowsDelayedPaymentMethods(true)
            .googlePay(PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "DE",
                currencyCode = "EUR"
            ))
            .build()

        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }
}