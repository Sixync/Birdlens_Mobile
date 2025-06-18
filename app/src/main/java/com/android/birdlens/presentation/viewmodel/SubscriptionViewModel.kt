// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/SubscriptionViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.Subscription
import com.android.birdlens.data.repository.SubscriptionRepository
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Using the generic UI state for consistency
typealias SubscriptionListUiState = GenericUiState<List<Subscription>>

class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository(application.applicationContext)

    private val _subscriptionsState = MutableStateFlow<SubscriptionListUiState>(GenericUiState.Idle)
    val subscriptionsState: StateFlow<SubscriptionListUiState> = _subscriptionsState.asStateFlow()

    companion object {
        private const val TAG = "SubscriptionVM"
    }

    init {
        // Fetch subscriptions as soon as the ViewModel is created.
        fetchSubscriptions()
    }

    fun fetchSubscriptions() {
        viewModelScope.launch {
            _subscriptionsState.value = GenericUiState.Loading
            try {
                val response = repository.getSubscriptions()
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _subscriptionsState.value = GenericUiState.Success(genericResponse.data)
                    } else {
                        _subscriptionsState.value = GenericUiState.Error(genericResponse.message ?: "Failed to load subscriptions")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _subscriptionsState.value = GenericUiState.Error(extractedMessage)
                    Log.e(TAG, "Fetch Subscriptions HTTP error: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _subscriptionsState.value = GenericUiState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Fetch Subscriptions exception", e)
            }
        }
    }
}