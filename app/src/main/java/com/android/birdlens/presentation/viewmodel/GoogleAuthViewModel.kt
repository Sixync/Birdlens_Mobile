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

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var oneTapClient: SignInClient? = null
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var signUpRequest: BeginSignInRequest

    private var googleSignInLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    // Web Client ID from the Firebase console / google-services.json
    private val serverClientId = "154465275979-jjmn8mi9a47mjms952rcba7eph12kgo0.apps.googleusercontent.com"

    fun initialize(activity: ComponentActivity) {
        // First, check if Google Play Services is available
        if (!checkGooglePlayServices(activity)) {
            _authState.value = AuthState.Error("Google Play Services unavailable or outdated")
            return
        }

        oneTapClient = Identity.getSignInClient(activity)
        Log.d("GoogleAuth", "Initializing with serverClientId: $serverClientId")

        // General Sign-In Request
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false) // Show account picker
                    .build()
            )
            .setAutoSelectEnabled(false) // Don't auto-select, show account picker
            .build()

        // Specific Sign-Up Request
        signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false) // Ensures it shows account chooser
                    .build()
            )
            .setAutoSelectEnabled(false) // For sign-up, always show account chooser
            .build()

        googleSignInLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            handleSignInResult(result.resultCode, result.data)
        }
    }

    private fun handleSignInResult(resultCode: Int, data: android.content.Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                val credential = oneTapClient?.getSignInCredentialFromIntent(data)
                val idToken = credential?.googleIdToken

                if (idToken != null) {
                    Log.d("GoogleAuth", "Got ID token from Google: ${idToken.substring(0, 15)}...")
                    firebaseAuthWithGoogle(idToken)
                } else {
                    _authState.value = AuthState.Error("No ID token found from Google Sign-In.")
                    Log.w("GoogleAuth", "No ID token found")
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val statusMessage = CommonStatusCodes.getStatusCodeString(statusCode)
                _authState.value = AuthState.Error("API Exception: $statusMessage (code $statusCode)")
                Log.e("GoogleAuth", "Google sign in failed - ApiException: $statusMessage ($statusCode)", e)
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error processing sign-in: ${e.javaClass.simpleName} - ${e.message}")
                Log.e("GoogleAuth", "Google sign in failed - General Exception", e)
            }
        } else {
            _authState.value = AuthState.Error("Sign-in cancelled - Result code: $resultCode")
            Log.w("GoogleAuth", "Google Sign-In Result not OK. Code: $resultCode")
        }
    }

    // Check if Google Play Services is available and up-to-date
    private fun checkGooglePlayServices(activity: Activity): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(activity)

        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("GoogleAuth", "Google Play Services unavailable, code: $resultCode")
            if (availability.isUserResolvableError(resultCode)) {
                availability.getErrorDialog(activity, resultCode, 2404)?.show()
            } else {
                Toast.makeText(activity, "This device does not support Google Play Services", Toast.LENGTH_LONG).show()
            }
            return false
        }
        return true
    }

    fun startGoogleSignIn(isSignUp: Boolean = false) {
        if (googleSignInLauncher == null || oneTapClient == null) {
            _authState.value = AuthState.Error("Google Sign-In not initialized. Call initialize() first.")
            Log.e("GoogleAuth", "Launcher or OneTapClient not initialized")
            return
        }

        if (serverClientId == "YOUR_NEW_WEB_CLIENT_ID.apps.googleusercontent.com") {
            _authState.value = AuthState.Error("Web Client ID not configured in GoogleAuthViewModel.")
            Log.e("GoogleAuth", "FATAL: serverClientId is not configured. Please update it in GoogleAuthViewModel.kt")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val request = if (isSignUp) signUpRequest else signInRequest
                Log.d("GoogleAuth", "Beginning sign-in/sign-up process with ${if (isSignUp) "sign-up" else "sign-in"} flow")
                val result = oneTapClient!!.beginSignIn(request).await()
                result.pendingIntent.intentSender?.let { intentSender ->
                    Log.d("GoogleAuth", "Got pendingIntent, launching sign-in activity")
                    googleSignInLauncher?.launch(IntentSenderRequest.Builder(intentSender).build())
                } ?: run {
                    _authState.value = AuthState.Error("Could not get pending intent for Google Sign-In.")
                    Log.e("GoogleAuth", "BeginSignInResult or PendingIntent is null")
                }
            } catch (e: Exception) {
                handleSignInException(e)
            }
        }
    }

    private fun handleSignInException(e: Exception) {
        when (e) {
            is ApiException -> {
                val statusCode = e.statusCode
                val statusMessage = CommonStatusCodes.getStatusCodeString(statusCode)
                Log.e("GoogleAuth", "API Exception during beginSignIn: Code $statusCode - $statusMessage", e)

                val errorMessage = when (statusCode) {
                    CommonStatusCodes.NETWORK_ERROR ->
                        "Network error. Check your internet connection."
                    CommonStatusCodes.DEVELOPER_ERROR ->
                        "Developer configuration error. Verify SHA-1 fingerprint and Client ID in Firebase."
                    CommonStatusCodes.INTERNAL_ERROR ->
                        "Google Play Services internal error. Try updating Google Play Services."
                    CommonStatusCodes.SIGN_IN_REQUIRED ->
                        "Sign-in required. No Google accounts found on device."
                    CommonStatusCodes.RESOLUTION_REQUIRED ->
                        "Resolution required. Additional steps needed to complete sign-in."
                    else ->
                        "Google Sign-In error: $statusMessage (code $statusCode)"
                }
                _authState.value = AuthState.Error(errorMessage)
            }
            else -> {
                Log.e("GoogleAuth", "Non-API Exception during sign-in: ${e.javaClass.simpleName}", e)
                _authState.value = AuthState.Error("Sign-in error: ${e.localizedMessage ?: e.javaClass.simpleName}")
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
                    _authState.value = AuthState.Success(user)
                } else {
                    _authState.value = AuthState.Error("Firebase authentication with Google failed (user is null).")
                    Log.w("GoogleAuth", "Firebase Auth with Google failed, user is null")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Firebase authentication error: ${e.localizedMessage}")
                Log.e("GoogleAuth", "Firebase auth with Google error", e)
            }
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                auth.signOut()
                oneTapClient?.signOut()?.await() // Sign out from Google One Tap
                _authState.value = AuthState.Idle
                Log.d("GoogleAuth", "User signed out from Firebase and Google One Tap")
            } catch (e: Exception) {
                Log.e("GoogleAuth", "Error during sign out: ${e.localizedMessage}", e)
                // Still set to Idle as the intention is to sign out
                _authState.value = AuthState.Idle
            }
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}