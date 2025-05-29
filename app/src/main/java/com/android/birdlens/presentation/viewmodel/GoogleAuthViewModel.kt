// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/GoogleAuthViewModel.kt
package com.android.birdlens.presentation.viewmodel

// import android.app.Activity // No longer needed for Google One-Tap
import android.app.Application
import android.content.Context // Keep for TokenManager in signOut
import android.util.Log
// import android.widget.Toast // Can be removed if not used directly in VM, typically handle UI feedback in UI layer
// import androidx.activity.ComponentActivity // No longer needed for Google One-Tap
// import androidx.activity.result.ActivityResultLauncher // No longer needed
// import androidx.activity.result.IntentSenderRequest // No longer needed
// import androidx.activity.result.contract.ActivityResultContracts // No longer needed
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// import com.android.birdlens.data.model.request.GoogleIdTokenRequest // No longer needed
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.network.ApiService
import com.android.birdlens.data.network.RetrofitInstance
// import com.google.android.gms.auth.api.identity.BeginSignInRequest // No longer needed
// import com.google.android.gms.auth.api.identity.Identity // No longer needed
// import com.google.android.gms.auth.api.identity.SignInClient // No longer needed
// import com.google.android.gms.common.api.ApiException // No longer needed
// import com.google.android.gms.common.api.CommonStatusCodes // No longer needed
// import com.google.android.gms.common.GoogleApiAvailability // No longer needed
// import com.google.android.gms.common.ConnectionResult // No longer needed
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

    // Removed _googleSignInOneTapState and its public StateFlow
    // private val _googleSignInOneTapState = MutableStateFlow<GoogleSignInOneTapState>(GoogleSignInOneTapState.Idle)
    // val googleSignInOneTapState: StateFlow<GoogleSignInOneTapState> = _googleSignInOneTapState.asStateFlow()

    private val _backendAuthState = MutableStateFlow<BackendAuthState>(BackendAuthState.Idle)
    val backendAuthState: StateFlow<BackendAuthState> = _backendAuthState.asStateFlow()

    private val _firebaseSignInState = MutableStateFlow<FirebaseSignInState>(FirebaseSignInState.Idle)
    val firebaseSignInState: StateFlow<FirebaseSignInState> = _firebaseSignInState.asStateFlow()

    // Removed properties related to Google One-Tap
    // private var oneTapClient: SignInClient? = null
    // private lateinit var signInRequest: BeginSignInRequest // This was unused, maybe a typo for googleIdTokenSignInRequest
    // private lateinit var googleIdTokenSignInRequest: BeginSignInRequest
    // private var googleSignInLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    // private val serverClientId = "154465275979-jjmn8mi9a47mjms952rcba7eph12kgo0.apps.googleusercontent.com" // No longer needed

    // Removed initialize, checkGooglePlayServices, startGoogleSignIn,
    // handleGoogleOneTapResult, handleGoogleOneTapException, authenticateWithBackendUsingGoogleToken methods
    // as they were specific to Google One-Tap and direct backend /auth/google calls.

    fun registerUser(registerRequest: RegisterRequest) {
        _backendAuthState.value = BackendAuthState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.registerUser(registerRequest)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val customToken = apiResponse.data
                        Log.d("AuthVM", "Backend Registration Success, got custom token: ${customToken.take(15)}...")
                        _backendAuthState.value = BackendAuthState.RegistrationSuccess(customToken)
                        signInToFirebaseWithCustomToken(customToken)
                    } else {
                        val errorMsg = apiResponse.message ?: "Registration failed: API error."
                        _backendAuthState.value = BackendAuthState.Error(errorMsg, AuthOperation.REGISTER)
                        Log.e("AuthVM", "Registration API failed: $errorMsg")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _backendAuthState.value = BackendAuthState.Error("Registration HTTP Error: ${response.code()} - $errorBody", AuthOperation.REGISTER)
                    Log.e("AuthVM", "Registration HTTP Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _backendAuthState.value = BackendAuthState.Error("Registration Exception: ${e.localizedMessage}", AuthOperation.REGISTER)
                Log.e("AuthVM", "Registration Exception", e)
            }
        }
    }

    fun loginUser(loginRequest: LoginRequest) {
        _backendAuthState.value = BackendAuthState.Loading
        viewModelScope.launch {
            try {
                val response = apiService.loginUser(loginRequest)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val customToken = apiResponse.data
                        Log.d("AuthVM", "Backend Login Success, got custom token: ${customToken.take(15)}...")
                        _backendAuthState.value = BackendAuthState.CustomTokenReceived(customToken) // Generic for any custom token
                        signInToFirebaseWithCustomToken(customToken)
                    } else {
                        val errorMsg = apiResponse.message ?: "Login failed: API error."
                        _backendAuthState.value = BackendAuthState.Error(errorMsg, AuthOperation.LOGIN)
                        Log.e("AuthVM", "Login API failed: $errorMsg")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _backendAuthState.value = BackendAuthState.Error("Login HTTP Error: ${response.code()} - $errorBody", AuthOperation.LOGIN)
                    Log.e("AuthVM", "Login HTTP Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _backendAuthState.value = BackendAuthState.Error("Login Exception: ${e.localizedMessage}", AuthOperation.LOGIN)
                Log.e("AuthVM", "Login Exception", e)
            }
        }
    }

    private fun signInToFirebaseWithCustomToken(customToken: String) {
        _firebaseSignInState.value = FirebaseSignInState.Loading
        viewModelScope.launch {
            try {
                Log.d("AuthVM", "Attempting Firebase sign-in with custom token: ${customToken.take(15)}...")
                val authResult = auth.signInWithCustomToken(customToken).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    Log.d("AuthVM", "Firebase signInWithCustomToken SUCCESS. User UID: ${firebaseUser.uid}")
                    val idTokenResult = firebaseUser.getIdToken(true).await()
                    val firebaseIdToken = idTokenResult.token
                    if (firebaseIdToken != null) {
                        Log.d("AuthVM", "Firebase ID Token obtained: ${firebaseIdToken.take(15)}...")
                        Log.d("AuthVM_TOKEN", "Firebase ID Token: $firebaseIdToken")
                        com.android.birdlens.data.TokenManager.getInstance(appApplication.applicationContext).saveFirebaseIdToken(firebaseIdToken)
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
                _firebaseSignInState.value = FirebaseSignInState.Error("Firebase custom sign-in error: ${e.localizedMessage}")
                Log.e("AuthVM", "Firebase signInWithCustomToken error", e)
            }
        }
    }

    fun signOut(context: Context) { // Pass context for TokenManager
        viewModelScope.launch {
            try {
                auth.signOut()
                // Removed oneTapClient?.signOut() as it's no longer used
                com.android.birdlens.data.TokenManager.getInstance(context.applicationContext).clearTokens()
                resetAllAuthStates()
                Log.d("AuthVM", "User signed out from Firebase.")
            } catch (e: Exception) {
                Log.e("AuthVM", "Error during sign out: ${e.localizedMessage}", e)
                com.android.birdlens.data.TokenManager.getInstance(context.applicationContext).clearTokens()
                resetAllAuthStates() // Ensure states are reset even on error
            }
        }
    }

    // Removed resetGoogleOneTapState
    // fun resetGoogleOneTapState() { _googleSignInOneTapState.value = GoogleSignInOneTapState.Idle }
    fun resetBackendAuthState() { _backendAuthState.value = BackendAuthState.Idle }
    fun resetFirebaseSignInState() { _firebaseSignInState.value = FirebaseSignInState.Idle }

    private fun resetAllAuthStates() {
        // resetGoogleOneTapState() // Removed
        resetBackendAuthState()
        resetFirebaseSignInState()
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    // Removed GoogleSignInOneTapState sealed class

    // Simplified AuthOperation
    enum class AuthOperation { LOGIN, REGISTER } // Removed GOOGLE_SIGN_IN

    sealed class BackendAuthState {
        object Idle : BackendAuthState()
        object Loading : BackendAuthState()
        data class CustomTokenReceived(val customToken: String) : BackendAuthState() // Used for login
        data class RegistrationSuccess(val customToken: String) : BackendAuthState() // Specific for registration response
        data class Error(val message: String, val operation: AuthOperation) : BackendAuthState()
    }

    sealed class FirebaseSignInState {
        object Idle : FirebaseSignInState()
        object Loading : FirebaseSignInState()
        data class Success(val firebaseUser: FirebaseUser, val firebaseIdToken: String) : FirebaseSignInState()
        data class Error(val message: String) : FirebaseSignInState()
    }
}