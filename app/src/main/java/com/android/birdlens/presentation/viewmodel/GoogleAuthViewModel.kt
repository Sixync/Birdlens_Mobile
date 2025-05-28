// EXE201/app/src/main/java/com/android/birdlens/presentation/viewmodel/GoogleAuthViewModel.kt
package com.android.birdlens.presentation.viewmodel

import android.app.Activity
import android.app.Application // New import
import android.content.Context
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.AndroidViewModel // Changed from ViewModel to AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.request.GoogleIdTokenRequest
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.network.ApiService // For type
import com.android.birdlens.data.network.RetrofitInstance
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoogleAuthViewModel(application: Application) : AndroidViewModel(application) { // Changed
    private val auth: FirebaseAuth = Firebase.auth
    private val appApplication: Application = application // Store application context

    // ApiService instance initialized with context
    private val apiService: ApiService = RetrofitInstance.api(application.applicationContext)

    // ... (rest of the _googleSignInOneTapState, _backendAuthState, _firebaseSignInState declarations remain the same)
    private val _googleSignInOneTapState = MutableStateFlow<GoogleSignInOneTapState>(GoogleSignInOneTapState.Idle)
    val googleSignInOneTapState: StateFlow<GoogleSignInOneTapState> = _googleSignInOneTapState.asStateFlow()

    private val _backendAuthState = MutableStateFlow<BackendAuthState>(BackendAuthState.Idle)
    val backendAuthState: StateFlow<BackendAuthState> = _backendAuthState.asStateFlow()

    private val _firebaseSignInState = MutableStateFlow<FirebaseSignInState>(FirebaseSignInState.Idle)
    val firebaseSignInState: StateFlow<FirebaseSignInState> = _firebaseSignInState.asStateFlow()

    private var oneTapClient: SignInClient? = null
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var googleIdTokenSignInRequest: BeginSignInRequest

    private var googleSignInLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    private val serverClientId = "154465275979-jjmn8mi9a47mjms952rcba7eph12kgo0.apps.googleusercontent.com"

    // initialize, checkGooglePlayServices, startGoogleSignIn, handleGoogleOneTapResult, handleGoogleOneTapException methods remain the same
    // ...
    fun initialize(activity: ComponentActivity) {
        if (!checkGooglePlayServices(activity)) {
            _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("Google Play Services unavailable or outdated.")
            return
        }

        oneTapClient = Identity.getSignInClient(activity)
        Log.d("GoogleAuthVM", "Initializing with serverClientId: $serverClientId")

        googleIdTokenSignInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()

        googleSignInLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            handleGoogleOneTapResult(result.resultCode, result.data)
        }
    }

    private fun checkGooglePlayServices(activity: Activity): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(activity)

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("GoogleAuthVM", "Google Play Services unavailable, code: $resultCode")
            if (availability.isUserResolvableError(resultCode)) {
                availability.getErrorDialog(activity, resultCode, 2404 /*REQUEST_CODE_RECOVER_PLAY_SERVICES*/)?.show()
            } else {
                Toast.makeText(activity, "This device does not support Google Play Services", Toast.LENGTH_LONG).show()
            }
            return false
        }
        return true
    }

    fun startGoogleSignIn() {
        if (googleSignInLauncher == null || oneTapClient == null) {
            _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("Google Sign-In not initialized.")
            Log.e("GoogleAuthVM", "Launcher or OneTapClient not initialized for Google Sign-In")
            return
        }
        if (serverClientId.startsWith("YOUR_") || serverClientId.isEmpty()) {
            _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("Web Client ID not configured in GoogleAuthViewModel.")
            Log.e("GoogleAuthVM", "FATAL: serverClientId for Google Sign-In is not configured.")
            return
        }

        _googleSignInOneTapState.value = GoogleSignInOneTapState.UILaunching
        viewModelScope.launch {
            try {
                Log.d("GoogleAuthVM", "Beginning Google One Tap sign-in process")
                val result = oneTapClient!!.beginSignIn(googleIdTokenSignInRequest).await()
                result.pendingIntent.intentSender?.let { intentSender ->
                    Log.d("GoogleAuthVM", "Got pendingIntent from Google, launching One Tap UI")
                    googleSignInLauncher?.launch(IntentSenderRequest.Builder(intentSender).build())
                } ?: run {
                    _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("Could not get pending intent for Google One Tap.")
                    Log.e("GoogleAuthVM", "Google BeginSignInResult or PendingIntent is null")
                }
            } catch (e: Exception) {
                handleGoogleOneTapException(e)
            }
        }
    }

    private fun handleGoogleOneTapResult(resultCode: Int, data: android.content.Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient?.getSignInCredentialFromIntent(data)
                val googleIdToken = credential?.googleIdToken

                if (googleIdToken != null) {
                    Log.d("GoogleAuthVM", "Got Google ID token from One Tap: ${googleIdToken.take(15)}...")
                    _googleSignInOneTapState.value = GoogleSignInOneTapState.GoogleIdTokenRetrieved(googleIdToken)
                    authenticateWithBackendUsingGoogleToken(googleIdToken)
                } else {
                    _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("No Google ID token found from One Tap.")
                    Log.w("GoogleAuthVM", "No Google ID token found from One Tap result")
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val statusMessage = CommonStatusCodes.getStatusCodeString(statusCode)
                _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("Google One Tap API Exception: $statusMessage (code $statusCode)")
                Log.e("GoogleAuthVM", "Google One Tap failed - ApiException: $statusMessage ($statusCode)", e)
            } catch (e: Exception) {
                _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("Error processing Google One Tap: ${e.localizedMessage}")
                Log.e("GoogleAuthVM", "Google One Tap failed - General Exception", e)
            }
        } else {
            _googleSignInOneTapState.value = GoogleSignInOneTapState.Error("Google One Tap cancelled or failed. Result code: $resultCode")
            Log.w("GoogleAuthVM", "Google One Tap Result not OK. Code: $resultCode")
        }
    }

    private fun handleGoogleOneTapException(e: Exception) {
        val errorMessage = when (e) {
            is ApiException -> "Google API Error: ${CommonStatusCodes.getStatusCodeString(e.statusCode)} (${e.statusCode})"
            is IntentSender.SendIntentException -> "Failed to send Google Sign-In intent."
            else -> "Google Sign-In unexpected error: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
        _googleSignInOneTapState.value = GoogleSignInOneTapState.Error(errorMessage)
        Log.e("GoogleAuthVM", "Google One Tap Exception: $errorMessage", e)
    }


    private fun authenticateWithBackendUsingGoogleToken(googleIdToken: String) {
        _backendAuthState.value = BackendAuthState.Loading
        viewModelScope.launch {
            try {
                // Use the member apiService
                val response = apiService.signInWithGoogleToken(GoogleIdTokenRequest(googleIdToken))
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val customToken = apiResponse.data
                        Log.d("GoogleAuthVM", "Backend Google Auth Success, got custom token: ${customToken.take(15)}...")
                        _backendAuthState.value = BackendAuthState.CustomTokenReceived(customToken)
                        signInToFirebaseWithCustomToken(customToken)
                    } else {
                        val errorMsg = apiResponse.message ?: "Backend Google auth failed: API error."
                        _backendAuthState.value = BackendAuthState.Error(errorMsg, AuthOperation.GOOGLE_SIGN_IN)
                        Log.e("GoogleAuthVM", "Backend Google Auth failed: $errorMsg")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _backendAuthState.value = BackendAuthState.Error("Backend Google auth HTTP Error: ${response.code()} - $errorBody", AuthOperation.GOOGLE_SIGN_IN)
                    Log.e("GoogleAuthVM", "Backend Google Auth HTTP Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _backendAuthState.value = BackendAuthState.Error("Backend Google auth Exception: ${e.localizedMessage}", AuthOperation.GOOGLE_SIGN_IN)
                Log.e("GoogleAuthVM", "Backend Google auth Exception", e)
            }
        }
    }

    fun registerUser(registerRequest: RegisterRequest) {
        _backendAuthState.value = BackendAuthState.Loading
        viewModelScope.launch {
            try {
                // Use the member apiService
                val response = apiService.registerUser(registerRequest)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val customToken = apiResponse.data
                        Log.d("GoogleAuthVM", "Backend Registration Success, got custom token: ${customToken.take(15)}...")
                        _backendAuthState.value = BackendAuthState.RegistrationSuccess(customToken)
                        signInToFirebaseWithCustomToken(customToken)
                    } else {
                        val errorMsg = apiResponse.message ?: "Registration failed: API error."
                        _backendAuthState.value = BackendAuthState.Error(errorMsg, AuthOperation.REGISTER)
                        Log.e("GoogleAuthVM", "Registration API failed: $errorMsg")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _backendAuthState.value = BackendAuthState.Error("Registration HTTP Error: ${response.code()} - $errorBody", AuthOperation.REGISTER)
                    Log.e("GoogleAuthVM", "Registration HTTP Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _backendAuthState.value = BackendAuthState.Error("Registration Exception: ${e.localizedMessage}", AuthOperation.REGISTER)
                Log.e("GoogleAuthVM", "Registration Exception", e)
            }
        }
    }

    fun loginUser(loginRequest: LoginRequest) {
        _backendAuthState.value = BackendAuthState.Loading
        viewModelScope.launch {
            try {
                // Use the member apiService
                val response = apiService.loginUser(loginRequest)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val customToken = apiResponse.data
                        Log.d("GoogleAuthVM", "Backend Login Success, got custom token: ${customToken.take(15)}...")
                        _backendAuthState.value = BackendAuthState.CustomTokenReceived(customToken)
                        signInToFirebaseWithCustomToken(customToken)
                    } else {
                        val errorMsg = apiResponse.message ?: "Login failed: API error."
                        _backendAuthState.value = BackendAuthState.Error(errorMsg, AuthOperation.LOGIN)
                        Log.e("GoogleAuthVM", "Login API failed: $errorMsg")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _backendAuthState.value = BackendAuthState.Error("Login HTTP Error: ${response.code()} - $errorBody", AuthOperation.LOGIN)
                    Log.e("GoogleAuthVM", "Login HTTP Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _backendAuthState.value = BackendAuthState.Error("Login Exception: ${e.localizedMessage}", AuthOperation.LOGIN)
                Log.e("GoogleAuthVM", "Login Exception", e)
            }
        }
    }

    // signInToFirebaseWithCustomToken, signOut, reset methods, getCurrentFirebaseUser, and Sealed classes remain the same
    // ...
    private fun signInToFirebaseWithCustomToken(customToken: String) {
        _firebaseSignInState.value = FirebaseSignInState.Loading
        viewModelScope.launch {
            try {
                Log.d("GoogleAuthVM", "Attempting Firebase sign-in with custom token: ${customToken.take(15)}...")
                val authResult = auth.signInWithCustomToken(customToken).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    Log.d("GoogleAuthVM", "Firebase signInWithCustomToken SUCCESS. User UID: ${firebaseUser.uid}")
                    val idTokenResult = firebaseUser.getIdToken(true).await()
                    val firebaseIdToken = idTokenResult.token
                    if (firebaseIdToken != null) {
                        Log.d("GoogleAuthVM", "Firebase ID Token obtained: ${firebaseIdToken.take(15)}...")
                        Log.d("GoogleAuthVM_POSTMAN", "Firebase ID Token for Postman: $firebaseIdToken") // LOG THE TOKEN
                        // Store the token using TokenManager
                        com.android.birdlens.data.TokenManager.getInstance(appApplication.applicationContext).saveFirebaseIdToken(firebaseIdToken)
                        _firebaseSignInState.value = FirebaseSignInState.Success(firebaseUser, firebaseIdToken)
                    } else {
                        _firebaseSignInState.value = FirebaseSignInState.Error("Failed to get Firebase ID Token after custom sign-in.")
                        Log.e("GoogleAuthVM", "Firebase ID Token was null after custom sign-in.")
                    }
                } else {
                    _firebaseSignInState.value = FirebaseSignInState.Error("Firebase custom sign-in failed (user is null).")
                    Log.w("GoogleAuthVM", "Firebase signInWithCustomToken failed, user is null")
                }
            } catch (e: Exception) {
                _firebaseSignInState.value = FirebaseSignInState.Error("Firebase custom sign-in error: ${e.localizedMessage}")
                Log.e("GoogleAuthVM", "Firebase signInWithCustomToken error", e)
            }
        }
    }

    fun signOut(context: Context) { // context here is fine for TokenManager
        viewModelScope.launch {
            try {
                auth.signOut()
                oneTapClient?.signOut()?.await()
                com.android.birdlens.data.TokenManager.getInstance(context.applicationContext).clearTokens() // Clear stored token
                resetAllAuthStates()
                Log.d("GoogleAuthVM", "User signed out from Firebase and Google One Tap")
            } catch (e: Exception) {
                Log.e("GoogleAuthVM", "Error during sign out: ${e.localizedMessage}", e)
                com.android.birdlens.data.TokenManager.getInstance(context.applicationContext).clearTokens() // Still clear token
                resetAllAuthStates()
            }
        }
    }

    fun resetGoogleOneTapState() { _googleSignInOneTapState.value = GoogleSignInOneTapState.Idle }
    fun resetBackendAuthState() { _backendAuthState.value = BackendAuthState.Idle }
    fun resetFirebaseSignInState() { _firebaseSignInState.value = FirebaseSignInState.Idle }

    private fun resetAllAuthStates() {
        resetGoogleOneTapState()
        resetBackendAuthState()
        resetFirebaseSignInState()
    }

    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    sealed class GoogleSignInOneTapState {
        object Idle : GoogleSignInOneTapState()
        object UILaunching : GoogleSignInOneTapState()
        data class GoogleIdTokenRetrieved(val idToken: String) : GoogleSignInOneTapState()
        data class Error(val message: String) : GoogleSignInOneTapState()
    }

    enum class AuthOperation { LOGIN, REGISTER, GOOGLE_SIGN_IN }

    sealed class BackendAuthState {
        object Idle : BackendAuthState()
        object Loading : BackendAuthState()
        data class CustomTokenReceived(val customToken: String) : BackendAuthState()
        data class RegistrationSuccess(val customToken: String) : BackendAuthState()
        data class Error(val message: String, val operation: AuthOperation) : BackendAuthState()
    }

    sealed class FirebaseSignInState {
        object Idle : FirebaseSignInState()
        object Loading : FirebaseSignInState()
        data class Success(val firebaseUser: FirebaseUser, val firebaseIdToken: String) : FirebaseSignInState()
        data class Error(val message: String) : FirebaseSignInState()
    }
}