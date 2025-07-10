// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/NotificationViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.Notification
import com.android.birdlens.data.repository.NotificationRepository
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Using the generic UI state for consistency
typealias NotificationListUiState = GenericUiState<List<Notification>>

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationRepository(application.applicationContext)

    private val _notificationsState = MutableStateFlow<NotificationListUiState>(GenericUiState.Idle)
    val notificationsState: StateFlow<NotificationListUiState> = _notificationsState.asStateFlow()

    // You can add pagination logic here if needed, similar to CommunityViewModel

    companion object {
        private const val TAG = "NotificationVM"
    }

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _notificationsState.value = GenericUiState.Loading
            try {
                // Fetching first page for now
                val response = repository.getNotifications(limit = 20, offset = 0)
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _notificationsState.value = GenericUiState.Success(genericResponse.data.items ?: emptyList())
                    } else {
                        _notificationsState.value = GenericUiState.Error(genericResponse.message ?: "Failed to load notifications")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _notificationsState.value = GenericUiState.Error(extractedMessage)
                    Log.e(TAG, "HTTP error fetching notifications: ${response.code()} - Full body: $errorBody")
                }
            } catch (e: Exception) {
                _notificationsState.value = GenericUiState.Error(e.localizedMessage ?: "An unexpected error occurred")
                Log.e(TAG, "Exception fetching notifications", e)
            }
        }
    }
}