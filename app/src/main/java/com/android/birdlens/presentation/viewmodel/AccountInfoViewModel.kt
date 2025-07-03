// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/AccountInfoViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.response.UserResponse
import com.android.birdlens.data.network.RetrofitInstance
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountInfoViewModel(application: Application) : AndroidViewModel(application) {

    internal val _uiState = MutableStateFlow<AccountInfoUiState>(AccountInfoUiState.Idle)
    val uiState: StateFlow<AccountInfoUiState> = _uiState.asStateFlow()

    private val apiService = RetrofitInstance.api(application.applicationContext)

    init {
    }

    fun fetchCurrentUser() {
        // Logic: Removed the check that prevented re-fetching if the state was already `Success`.
        // This allows the payment result screen to force a refresh of the user's profile,
        // ensuring their new subscription status is correctly loaded.
        if (_uiState.value is AccountInfoUiState.Loading) {
            Log.d("AccountInfoVM", "FetchCurrentUser skipped. State is already Loading.")
            return
        }
        _uiState.value = AccountInfoUiState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful && response.body() != null) {
                    val genericResponse = response.body()!!
                    if (!genericResponse.error && genericResponse.data != null) {
                        _uiState.value = AccountInfoUiState.Success(genericResponse.data)
                    } else {
                        _uiState.value = AccountInfoUiState.Error(genericResponse.message ?: "Failed to load user data")
                        Log.e("AccountInfoVM", "API error: ${genericResponse.message}")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Error ${response.code()}")
                    _uiState.value = AccountInfoUiState.Error(extractedMessage)
                    Log.e("AccountInfoVM", "HTTP error: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _uiState.value = AccountInfoUiState.Error("Exception: ${e.localizedMessage ?: "Network request failed"}")
                Log.e("AccountInfoVM", "Exception fetching user", e)
            }
        }
    }

    fun onUserLoggedOut() {
        _uiState.value = AccountInfoUiState.Idle
        Log.d("AccountInfoVM", "User state has been reset to Idle due to logout.")
    }

    fun resetUiStateToIdle() {
        Log.d("AccountInfoVM", "Resetting UI state to Idle post-event.")
        _uiState.value = AccountInfoUiState.Idle
    }
}

sealed class AccountInfoUiState {
    data object Idle : AccountInfoUiState()
    data object Loading : AccountInfoUiState()
    data class Success(val user: UserResponse) : AccountInfoUiState()
    data class Error(val message: String) : AccountInfoUiState()
}