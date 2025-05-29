// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/AccountInfoViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.response.UserResponse
import com.android.birdlens.data.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountInfoViewModel(application: Application) : AndroidViewModel(application) {

    val _uiState = MutableStateFlow<AccountInfoUiState>(AccountInfoUiState.Loading)
    val uiState: StateFlow<AccountInfoUiState> = _uiState.asStateFlow()

    private val apiService = RetrofitInstance.api(application.applicationContext)

    init {
        fetchCurrentUser()
    }

    fun fetchCurrentUser() {
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
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _uiState.value = AccountInfoUiState.Error("Error: ${response.code()} - $errorBody")
                    Log.e("AccountInfoVM", "HTTP error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _uiState.value = AccountInfoUiState.Error("Exception: ${e.localizedMessage ?: "Network request failed"}")
                Log.e("AccountInfoVM", "Exception fetching user", e)
            }
        }
    }
}

sealed class AccountInfoUiState {
    object Loading : AccountInfoUiState()
    data class Success(val user: UserResponse) : AccountInfoUiState()
    data class Error(val message: String) : AccountInfoUiState()
}