package com.android.birdlens.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.TokenManager
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
import com.android.birdlens.utils.ErrorUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleAuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = Firebase.auth
    private val appApplication: Application = application

    private val apiService: ApiService = RetrofitInstance.api(application.applicationContext)
    private val tokenManager = TokenManager.getInstance(application.applicationContext)


    private val _backendAuthState = MutableStateFlow<BackendAuthState>(BackendAuthState.Idle)
    val backendAuthState: StateFlow<BackendAuthState> = _backendAuthState.asStateFlow()

    private val _firebaseSignInState = MutableStateFlow<FirebaseSignInState>(FirebaseSignInState.Idle)
    val firebaseSignInState: StateFlow<FirebaseSignInState> = _firebaseSignInState.asStateFlow()

    var referralCodeFromLink: String? = null

    fun registerUser(registerRequest: RegisterRequest) {
        resetBackendAuthState()
        _backendAuthState.value = BackendAuthState.Loading
        Log.d("AuthVM", "Registering user: ${registerRequest.email} with referral: ${registerRequest.referralCode}")
        viewModelScope.launch {
            try {
                val response = apiService.registerUser(registerRequest)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val customToken = apiResponse.data
                        Log.d("AuthVM", "Backend Registration Success, got custom token (first 15 chars): ${customToken.take(15)}...")
                        _backendAuthState.value = BackendAuthState.RegistrationSuccess(customToken)
                        signInToFirebaseWithCustomToken(customToken)
                    } else {
                        val errorMsg = apiResponse.message ?: "Registration failed: API error."
                        _backendAuthState.value = BackendAuthState.Error(errorMsg, AuthOperation.REGISTER)
                        Log.e("AuthVM", "Registration API failed: $errorMsg")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Registration HTTP Error ${response.code()}")
                    _backendAuthState.value = BackendAuthState.Error(extractedMessage, AuthOperation.REGISTER)
                    Log.e("AuthVM", "Registration HTTP Error: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _backendAuthState.value = BackendAuthState.Error("Registration Exception: ${e.localizedMessage ?: "An unexpected error occurred."}", AuthOperation.REGISTER)
                Log.e("AuthVM", "Registration Exception", e)
            }
        }
    }

    fun loginUser(loginRequest: LoginRequest) {
        resetBackendAuthState()
        _backendAuthState.value = BackendAuthState.Loading
        Log.d("AuthVM", "Logging in user: ${loginRequest.email}")
        viewModelScope.launch {
            try {
                val response = apiService.loginUser(loginRequest)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val customToken = apiResponse.data
                        Log.d("AuthVM", "Backend Login Success, got custom token (first 15 chars): ${customToken.take(15)}...")
                        _backendAuthState.value = BackendAuthState.CustomTokenReceived(customToken)
                        signInToFirebaseWithCustomToken(customToken)
                    } else {
                        val errorMsg = apiResponse.message ?: "Login failed: API error."
                        _backendAuthState.value = BackendAuthState.Error(errorMsg, AuthOperation.LOGIN)
                        Log.e("AuthVM", "Login API failed: $errorMsg")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val extractedMessage = ErrorUtils.extractMessage(errorBody, "Login HTTP Error ${response.code()}")
                    _backendAuthState.value = BackendAuthState.Error(extractedMessage, AuthOperation.LOGIN)
                    Log.e("AuthVM", "Login HTTP Error: ${response.code()} - Full error body: $errorBody")
                }
            } catch (e: Exception) {
                _backendAuthState.value = BackendAuthState.Error("Login Exception: ${e.localizedMessage ?: "An unexpected error occurred."}", AuthOperation.LOGIN)
                Log.e("AuthVM", "Login Exception", e)
            }
        }
    }

    private fun signInToFirebaseWithCustomToken(customToken: String) {
        _firebaseSignInState.value = FirebaseSignInState.Loading
        Log.d("AuthVM", "Attempting Firebase sign-in with custom token (first 15 chars): ${customToken.take(15)}...")
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithCustomToken(customToken).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    Log.d("AuthVM", "Firebase signInWithCustomToken SUCCESS. User UID: ${firebaseUser.uid}")
                    val idTokenResult = firebaseUser.getIdToken(true).await()
                    val firebaseIdToken = idTokenResult.token
                    if (firebaseIdToken != null) {
                        Log.d("AuthVM", "Firebase ID Token obtained : ${firebaseIdToken}")
                        tokenManager.saveFirebaseIdToken(firebaseIdToken)
                        _firebaseSignInState.value = FirebaseSignInState.Success(firebaseUser, firebaseIdToken)
                    } else {
                        _firebaseSignInState.value = FirebaseSignInState.Error("Failed to get Firebase ID Token after custom sign-in.")
                        Log.e("AuthVM", "Firebase ID Token was null after custom sign-in.")
                    }
                } else {
                    _firebaseSignInState.value = FirebaseSignInState.Error("Firebase custom sign-in failed (user is null).")
                    Log.w("AuthVM", "Firebase signInWithCustomToken failed, user is null")
                }
            } catch (e: Exception) {
                _firebaseSignInState.value = FirebaseSignInState.Error("Firebase custom sign-in error: ${e.localizedMessage ?: "An unexpected error occurred."}")
                Log.e("AuthVM", "Firebase signInWithCustomToken error", e)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                auth.signOut()
                tokenManager.clearTokens()
                resetAllAuthStates()
                Log.d("AuthVM", "User signed out from Firebase.")
            } catch (e: Exception) {
                Log.e("AuthVM", "Error during sign out: ${e.localizedMessage}", e)
                tokenManager.clearTokens()
                resetAllAuthStates()
            }
        }
    }

    fun resetBackendAuthState() { _backendAuthState.value = BackendAuthState.Idle }
    fun resetFirebaseSignInState() { _firebaseSignInState.value = FirebaseSignInState.Idle }

    private fun resetAllAuthStates() {
        resetBackendAuthState()
        resetFirebaseSignInState()
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    enum class AuthOperation { LOGIN, REGISTER }

    sealed class BackendAuthState {
        data object Idle : BackendAuthState()
        data object Loading : BackendAuthState()
        data class CustomTokenReceived(val customToken: String) : BackendAuthState()
        data class RegistrationSuccess(val customToken: String) : BackendAuthState()
        data class Error(val message: String, val operation: AuthOperation) : BackendAuthState()
    }

    sealed class FirebaseSignInState {
        data object Idle : FirebaseSignInState()
        data object Loading : FirebaseSignInState()
        data class Success(val firebaseUser: FirebaseUser, val firebaseIdToken: String) : FirebaseSignInState()
        data class Error(val message: String) : FirebaseSignInState()
    }
}