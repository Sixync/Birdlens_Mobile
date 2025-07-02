// path: EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/PaymentViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.CreatePaymentRequest
import com.android.birdlens.data.model.PaymentItem
import com.android.birdlens.data.repository.PaymentRepository
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PaymentUiState {
    data object Idle : PaymentUiState()
    data object Loading : PaymentUiState()
    data class LinkCreated(val checkoutUrl: String) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PaymentRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun createPayOSPaymentLink() {
        viewModelScope.launch {
            _uiState.value = PaymentUiState.Loading
            try {
                // For now, we hardcode the item to "sub_premium" as in the Stripe flow.
                val request = CreatePaymentRequest(items = listOf(PaymentItem(id = "sub_premium")))
                val response = repository.createPayOSLink(request)

                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _uiState.value = PaymentUiState.LinkCreated(genericResponse.data.checkoutUrl)
                    } else {
                        _uiState.value = PaymentUiState.Error(genericResponse.message ?: "Failed to create payment link.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _uiState.value = PaymentUiState.Error(
                        ErrorUtils.extractMessage(errorBody, "Server error: ${response.code()}")
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PaymentUiState.Error("An error occurred: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = PaymentUiState.Idle
    }
}