// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/AdminSubscriptionViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.CreateSubscriptionRequest
import com.android.birdlens.data.model.Subscription
import com.android.birdlens.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SubscriptionUIState<out T> {
    data object Idle : SubscriptionUIState<Nothing>()
    data object Loading : SubscriptionUIState<Nothing>()
    data class Success<T>(val data: T) : SubscriptionUIState<T>()
    data class Error(val message: String) : SubscriptionUIState<Nothing>()
}

class AdminSubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository(application.applicationContext)

    private val _subscriptionsState = MutableStateFlow<SubscriptionUIState<List<Subscription>>>(SubscriptionUIState.Idle)
    val subscriptionsState: StateFlow<SubscriptionUIState<List<Subscription>>> = _subscriptionsState.asStateFlow()

    private val _createSubscriptionState = MutableStateFlow<SubscriptionUIState<Subscription>>(SubscriptionUIState.Idle)
    val createSubscriptionState: StateFlow<SubscriptionUIState<Subscription>> = _createSubscriptionState.asStateFlow()

    fun fetchSubscriptions() {
        viewModelScope.launch {
            _subscriptionsState.value = SubscriptionUIState.Loading
            try {
                val response = repository.getSubscriptions()
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _subscriptionsState.value = SubscriptionUIState.Success(genericResponse.data)
                    } else {
                        _subscriptionsState.value = SubscriptionUIState.Error(genericResponse.message ?: "Failed to load subscriptions")
                    }
                } else {
                    _subscriptionsState.value = SubscriptionUIState.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _subscriptionsState.value = SubscriptionUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
            }
        }
    }

    fun createSubscription(name: String, description: String, priceStr: String, durationDaysStr: String) {
        val price = priceStr.toDoubleOrNull()
        val durationDays = durationDaysStr.toIntOrNull()

        if (name.isBlank() || description.isBlank() || price == null || durationDays == null) {
            _createSubscriptionState.value = SubscriptionUIState.Error("All fields are required and must be valid.")
            return
        }
        if (price <= 0 || durationDays <= 0) {
            _createSubscriptionState.value = SubscriptionUIState.Error("Price and duration must be positive values.")
            return
        }

        val request = CreateSubscriptionRequest(name, description, price, durationDays)
        viewModelScope.launch {
            _createSubscriptionState.value = SubscriptionUIState.Loading
            try {
                val response = repository.createSubscription(request)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _createSubscriptionState.value = SubscriptionUIState.Success(genericResponse.data)
                        fetchSubscriptions() // Refresh list after creation
                    } else {
                        _createSubscriptionState.value = SubscriptionUIState.Error(genericResponse.message ?: "Failed to create subscription")
                    }
                } else {
                    _createSubscriptionState.value = SubscriptionUIState.Error("Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _createSubscriptionState.value = SubscriptionUIState.Error(e.localizedMessage ?: "An unexpected error occurred")
            }
        }
    }

    fun resetCreateSubscriptionState() {
        _createSubscriptionState.value = SubscriptionUIState.Idle
    }
}