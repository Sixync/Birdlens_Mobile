// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/register/RegisterScreen.kt
package com.android.birdlens.presentation.ui.screens.register

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.ui.screens.accountinfo.ApplicationProvider
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
// import com.android.birdlens.presentation.ui.screens.login.SocialLoginButton // Keep if planning to use for other social, remove if not
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun RegisterScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    onNavigateBack: () -> Unit,
    // Removed onLoginWithFacebook, onLoginWithX, onLoginWithApple as per simplification
    // onLoginWithFacebook: () -> Unit,
    // onLoginWithX: () -> Unit,
    // onLoginWithApple: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var ageString by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Removed googleOneTapState
    // val googleOneTapState by googleAuthViewModel.googleSignInOneTapState.collectAsState()
    val backendAuthState by googleAuthViewModel.backendAuthState.collectAsState()
    val firebaseSignInState by googleAuthViewModel.firebaseSignInState.collectAsState()

    LaunchedEffect(firebaseSignInState) {
        when (val state = firebaseSignInState) {
            is GoogleAuthViewModel.FirebaseSignInState.Success -> {
                Toast.makeText(context, "Firebase Sign-In Success (after registration): ${state.firebaseUser.uid}", Toast.LENGTH_SHORT).show()
                Log.d("RegisterScreen", "Firebase ID Token: ${state.firebaseIdToken.take(15)}...")
                navController.navigate(Screen.LoginSuccess.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
                googleAuthViewModel.resetFirebaseSignInState()
                googleAuthViewModel.resetBackendAuthState()
                // googleAuthViewModel.resetGoogleOneTapState() // No longer needed
            }
            is GoogleAuthViewModel.FirebaseSignInState.Error -> {
                Toast.makeText(context, "Firebase Sign-In Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetFirebaseSignInState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    LaunchedEffect(backendAuthState) {
        when (val state = backendAuthState) {
            is GoogleAuthViewModel.BackendAuthState.Error -> {
                val operationType = when (state.operation) {
                    GoogleAuthViewModel.AuthOperation.REGISTER -> "Registration"
                    // GoogleAuthViewModel.AuthOperation.GOOGLE_SIGN_IN -> "Google Sign-Up" // Removed
                    else -> "Authentication" // Could be LOGIN if called from a different context, but here it's REGISTER
                }
                Toast.makeText(context, "$operationType Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetBackendAuthState()
            }
            is GoogleAuthViewModel.BackendAuthState.RegistrationSuccess -> {
                Toast.makeText(context, "Registration with backend successful. Signing into Firebase...", Toast.LENGTH_SHORT).show()
                // Firebase sign-in is triggered automatically by the ViewModel
            }
            // CustomTokenReceived is typically for login or Google Sign-In, not direct registration response.
            // Keeping it in case the VM's logic handles it generically.
            is GoogleAuthViewModel.BackendAuthState.CustomTokenReceived -> {
                Toast.makeText(context, "Auth with backend successful. Signing into Firebase...", Toast.LENGTH_SHORT).show()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    // Removed LaunchedEffect for googleOneTapState

    val isLoading = backendAuthState is GoogleAuthViewModel.BackendAuthState.Loading ||
            firebaseSignInState is GoogleAuthViewModel.FirebaseSignInState.Loading
    // Removed googleOneTapState check from isLoading

    AuthScreenLayout(
        modifier = modifier,
        topContent = {
            // ... (topContent remains the same)
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp)) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = TextWhite
                    )
                }
            }
        }
    ) {
        Surface(
            color = AuthCardBackground,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CREATE YOUR ACCOUNT",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                CustomTextField(value = firstName, onValueChange = { firstName = it }, placeholder = "First Name", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = lastName, onValueChange = { lastName = it }, placeholder = "Last Name", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = ageString, onValueChange = { ageString = it.filter { char -> char.isDigit() } }, placeholder = "Age", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = email, onValueChange = { email = it }, placeholder = "Email", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = username, onValueChange = { username = it }, placeholder = "Username", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = password, onValueChange = { password = it }, placeholder = "Password", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = retypePassword, onValueChange = { retypePassword = it }, placeholder = "Retype password", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), backgroundColor = AuthInputBackground)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val age = ageString.toIntOrNull()
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "All fields are required.", Toast.LENGTH_SHORT).show()
                        } else if (age == null || age <= 0) {
                            Toast.makeText(context, "Please enter a valid age.", Toast.LENGTH_SHORT).show()
                        } else if (password.length < 6) { // Basic password validation
                            Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                        } else if (password != retypePassword) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                        } else {
                            val request = RegisterRequest(
                                username = username, password = password, email = email,
                                firstName = firstName, lastName = lastName, age = age
                                // avatarUrl is optional and defaults to null
                            )
                            googleAuthViewModel.registerUser(request)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                    enabled = !isLoading
                ) {
                    Text("Continue", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Removed SocialLoginButton for Google and other social platforms
                // Example:
                // SocialLoginButton(text = "Sign up with Google", onClick = { if (!isLoading) { /* googleAuthViewModel.startGoogleSignIn() */ Toast.makeText(context, "Google Sign-up not available.", Toast.LENGTH_SHORT).show() } }, iconPlaceholder = true, enabled = !isLoading, backgroundColor = SocialButtonBackgroundLight, contentColor = SocialButtonTextDark)
                // Spacer(modifier = Modifier.height(10.dp))


                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = TextWhite)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp))
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=780dp,dpi=480")
@Composable
fun RegisterScreenPreview() {
    BirdlensTheme {
        val navController = rememberNavController()
        val dummyViewModel = GoogleAuthViewModel(ApplicationProvider.getApplicationContext()) // Pass context
        RegisterScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {}
            // Removed Facebook, X, Apple click handlers from preview call
        )
    }
}