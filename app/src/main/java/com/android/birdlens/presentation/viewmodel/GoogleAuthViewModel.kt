package com.android.birdlens.presentation.viewmodel

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.data.model.response.LoginData
import com.android.birdlens.data.model.response.LoginResponse
import com.android.birdlens.data.model.response.UserResponse
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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
class GoogleAuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    private val _googleAuthState = MutableStateFlow<GoogleSignInState>(GoogleSignInState.Idle)
    val googleAuthState: StateFlow<GoogleSignInState> = _googleAuthState.asStateFlow()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    private val _traditionalLoginState = MutableStateFlow<TraditionalLoginState>(TraditionalLoginState.Idle)
    val traditionalLoginState: StateFlow<TraditionalLoginState> = _traditionalLoginState.asStateFlow()

    private var oneTapClient: SignInClient? = null
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var signUpRequest: BeginSignInRequest

    private var googleSignInLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    // Ensure this is your WEB client ID from Google Cloud Console for Firebase Google Sign-In
    private val serverClientId = "154465275979-jjmn8mi9a47mjms952rcba7eph12kgo0.apps.googleusercontent.com"


    fun initialize(activity: ComponentActivity) {
        if (!checkGooglePlayServices(activity)) {
            _googleAuthState.value = GoogleSignInState.Error("Google Play Services unavailable or outdated")
            return
        }

        oneTapClient = Identity.getSignInClient(activity)
        Log.d("GoogleAuth", "Initializing with serverClientId: $serverClientId")


        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()

        signUpRequest = BeginSignInRequest.builder()
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
            handleGoogleSignInResult(result.resultCode, result.data)
        }
    }

    private fun handleGoogleSignInResult(resultCode: Int, data: android.content.Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient?.getSignInCredentialFromIntent(data)
                val idToken = credential?.googleIdToken

                if (idToken != null) {
                    Log.d("GoogleAuth", "Got ID token from Google: ${idToken.substring(0,15)}...")
                    firebaseAuthWithGoogle(idToken)
                } else {
                    _googleAuthState.value = GoogleSignInState.Error("No ID token found from Google Sign-In.")
                    Log.w("GoogleAuth", "No ID token found")
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val statusMessage = CommonStatusCodes.getStatusCodeString(statusCode)
                _googleAuthState.value = GoogleSignInState.Error("API Exception: $statusMessage (code $statusCode)")
                Log.e("GoogleAuth", "Google sign in failed - ApiException: $statusMessage ($statusCode)", e)

            } catch (e: Exception) {
                _googleAuthState.value = GoogleSignInState.Error("Error processing sign-in: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("GoogleAuth", "Google sign in failed - General Exception", e)
            }
        } else {
            _googleAuthState.value = GoogleSignInState.Error("Sign-in cancelled - Result code: $resultCode")
            Log.w("GoogleAuth", "Google Sign-In Result not OK. Code: $resultCode")
        }
    }

    private fun checkGooglePlayServices(activity: Activity): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(activity)

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("GoogleAuth", "Google Play Services unavailable, code: $resultCode")
            if (availability.isUserResolvableError(resultCode)) {
                availability.getErrorDialog(activity, resultCode, 2404 /*REQUEST_CODE_RECOVER_PLAY_SERVICES*/)?.show()
            } else {
                Toast.makeText(activity, "This device does not support Google Play Services", Toast.LENGTH_LONG).show()
            }
            return false
        }
        return true
    }


    fun startGoogleSignIn(isSignUp: Boolean = false) {
        if (googleSignInLauncher == null || oneTapClient == null) {
            _googleAuthState.value = GoogleSignInState.Error("Google Sign-In not initialized. Call initialize() first.")
            Log.e("GoogleAuth", "Launcher or OneTapClient not initialized")
            return
        }

        if (serverClientId == "YOUR_NEW_WEB_CLIENT_ID.apps.googleusercontent.com") {
            _googleAuthState.value = GoogleSignInState.Error("Web Client ID not configured in GoogleAuthViewModel.")
            Log.e("GoogleAuth", "FATAL: serverClientId is not configured. Please update it in GoogleAuthViewModel.kt")
            return
        }


        _googleAuthState.value = GoogleSignInState.Loading
        viewModelScope.launch {
            try {
                val request = if (isSignUp) signUpRequest else signInRequest
                Log.d("GoogleAuth", "Beginning sign-in/sign-up process with ${if (isSignUp) "sign-up" else "sign-in"} flow")
                val result = oneTapClient!!.beginSignIn(request).await()
                result.pendingIntent.intentSender?.let { intentSender ->
                    Log.d("GoogleAuth", "Got pendingIntent, launching sign-in activity")
                    googleSignInLauncher?.launch(IntentSenderRequest.Builder(intentSender).build())
                } ?: run {
                    _googleAuthState.value = GoogleSignInState.Error("Could not get pending intent for Google Sign-In.")
                    Log.e("GoogleAuth", "BeginSignInResult or PendingIntent is null")
                }
            } catch (e: Exception) {
                handleGoogleSignInException(e)
            }
        }
    }

    private fun handleGoogleSignInException(e: Exception) {
        when (e) {
            is ApiException -> {
                val statusCode = e.statusCode
                when (statusCode) {
                    CommonStatusCodes.NETWORK_ERROR ->
                        _googleAuthState.value = GoogleSignInState.Error("Network error during Google Sign-In.")
                    CommonStatusCodes.SIGN_IN_REQUIRED ->
                        _googleAuthState.value = GoogleSignInState.Error("Sign-in required by Google.")
                    else ->
                        _googleAuthState.value = GoogleSignInState.Error("Google Sign-In failed: ${CommonStatusCodes.getStatusCodeString(statusCode)} ($statusCode)")
                }
                Log.e("GoogleAuth", "Google Sign-In ApiException: ${CommonStatusCodes.getStatusCodeString(statusCode)}", e)
            }
            is IntentSender.SendIntentException -> {
                _googleAuthState.value = GoogleSignInState.Error("Failed to send Google Sign-In intent.")
                Log.e("GoogleAuth", "Google Sign-In SendIntentException", e)
            }
            else -> {
                _googleAuthState.value = GoogleSignInState.Error("An unexpected error occurred during Google Sign-In: ${e.localizedMessage ?: e.javaClass.simpleName}")
                Log.e("GoogleAuth", "Google Sign-In generic error", e)
            }
        }
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                Log.d("GoogleAuth", "Attempting Firebase auth with Google token")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                if (user != null) {
                    Log.d("GoogleAuth", "Firebase Auth with Google Success: ${user.displayName}")
                    _googleAuthState.value = GoogleSignInState.Success(user)
                } else {
                    _googleAuthState.value = GoogleSignInState.Error("Firebase authentication with Google failed (user is null).")
                    Log.w("GoogleAuth", "Firebase Auth with Google failed, user is null")
                }
            } catch (e: Exception) {
                _googleAuthState.value = GoogleSignInState.Error("Firebase authentication error: ${e.localizedMessage}")
                Log.e("GoogleAuth", "Firebase auth with Google error", e)
            }
        }
    }

    fun registerUser(registerRequest: RegisterRequest) {
        _registrationState.value = RegistrationState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.registerUser(registerRequest)
                if (response.isSuccessful && response.body() != null) {
                    _registrationState.value = RegistrationState.Success(response.body()!!)
                    Log.d("RegisterAPI", "Registration successful: ${response.body()}")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _registrationState.value = RegistrationState.Error("Registration failed: ${response.code()} - $errorBody")
                    Log.e("RegisterAPI", "Registration failed: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error("Registration error: ${e.localizedMessage ?: "Network error"}")
                Log.e("RegisterAPI", "Registration exception (Ask Gemini)", e) // Added your tag
            }
        }
    }

    fun loginUser(loginRequest: LoginRequest) {
        _traditionalLoginState.value = TraditionalLoginState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.loginUser(loginRequest)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (!apiResponse.error && apiResponse.data != null) {
                        val loginData = apiResponse.data
                        val tokenPrefix = loginData.accessToken?.takeIf { it.length >= 10 }?.substring(0, 10) ?: "N/A"
                        Log.d("LoginAPI", "Login successful. AccessToken: $tokenPrefix..., User: ${loginData.username ?: "N/A"}")
                        _traditionalLoginState.value = TraditionalLoginState.Success(loginData)
                    } else {
                        // API returned success (2xx) but with an error flag or missing data
                        val errorMessage = apiResponse.message ?: "Login failed: API indicated error."
                        _traditionalLoginState.value = TraditionalLoginState.Error(errorMessage)
                        Log.e("LoginAPI", "Login failed: $errorMessage (Error flag: ${apiResponse.error}, Data: ${apiResponse.data})")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _traditionalLoginState.value = TraditionalLoginState.Error("Login failed: HTTP ${response.code()} - $errorBody")
                    Log.e("LoginAPI", "Login failed: HTTP ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _traditionalLoginState.value = TraditionalLoginState.Error("Login error: ${e.localizedMessage ?: "Network error"}")
                Log.e("LoginAPI", "Login exception", e)
            }
        }
    }


    fun resetGoogleAuthState() {
        _googleAuthState.value = GoogleSignInState.Idle
    }

    fun resetRegistrationState() {
        _registrationState.value = RegistrationState.Idle
    }

    fun resetTraditionalLoginState() {
        _traditionalLoginState.value = TraditionalLoginState.Idle
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                auth.signOut() // Sign out from Firebase
                oneTapClient?.signOut()?.await() // Sign out from Google One Tap
                _googleAuthState.value = GoogleSignInState.Idle
                _registrationState.value = RegistrationState.Idle
                _traditionalLoginState.value = TraditionalLoginState.Idle // Reset traditional login state too
                Log.d("GoogleAuth", "User signed out from Firebase and Google One Tap")
                // Optionally clear any stored tokens here
            } catch (e: Exception) {
                Log.e("GoogleAuth", "Error during sign out: ${e.localizedMessage}", e)
                // Still reset states even if one part of sign-out fails
                _googleAuthState.value = GoogleSignInState.Idle
                _registrationState.value = RegistrationState.Idle
                _traditionalLoginState.value = TraditionalLoginState.Idle
            }
        }
    }


    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    sealed class GoogleSignInState {
        object Idle : GoogleSignInState()
        object Loading : GoogleSignInState()
        data class Success(val user: FirebaseUser) : GoogleSignInState()
        data class Error(val message: String) : GoogleSignInState()
    }

    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Loading : RegistrationState()
        data class Success(val user: UserResponse) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    sealed class TraditionalLoginState {
        object Idle : TraditionalLoginState()
        object Loading : TraditionalLoginState()
        data class Success(val loginData: LoginData) : TraditionalLoginState() // Changed to LoginData
        data class Error(val message: String) : TraditionalLoginState()
    }
}