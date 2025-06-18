// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/ForgotPasswordViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.request.ForgotPasswordRequest
import com.android.birdlens.data.model.request.ResetPasswordRequest
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import com.android.birdlens.utils.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PasswordResetState {
    data object Idle : PasswordResetState()
    data object Loading : PasswordResetState()
    data class Success(val message: String) : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService: ApiService = RetrofitInstance.api(application.applicationContext)

    private val _forgotPasswordState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val forgotPasswordState: StateFlow<PasswordResetState> = _forgotPasswordState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val resetPasswordState: StateFlow<PasswordResetState> = _resetPasswordState.asStateFlow()

    companion object {
        private const val TAG = "ForgotPasswordVM"
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = PasswordResetState.Loading
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error) {
                        _forgotPasswordState.value = PasswordResetState.Success(apiResponse.message ?: "Password reset link sent.")
                    } else {
                        _forgotPasswordState.value = PasswordResetState.Error(apiResponse.message ?: "Failed to send reset link.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _forgotPasswordState.value = PasswordResetState.Error(ErrorUtils.extractMessage(errorBody, "Server error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception requesting password reset", e)
                _forgotPasswordState.value = PasswordResetState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetPassword(token: String, newPassword: String) {
        viewModelScope.launch {
            _resetPasswordState.value = PasswordResetState.Loading
            try {
                val response = apiService.resetPassword(ResetPasswordRequest(token, newPassword))
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error) {
                        _resetPasswordState.value = PasswordResetState.Success(apiResponse.message ?: "Password has been reset successfully.")
                    } else {
                        _resetPasswordState.value = PasswordResetState.Error(apiResponse.message ?: "Failed to reset password.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _resetPasswordState.value = PasswordResetState.Error(ErrorUtils.extractMessage(errorBody, "Server error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception resetting password", e)
                _resetPasswordState.value = PasswordResetState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = PasswordResetState.Idle
    }

    fun resetResetPasswordState() {
        _resetPasswordState.value = PasswordResetState.Idle
    }
}