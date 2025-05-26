// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/register/RegisterScreen.kt
package com.android.birdlens.presentation.ui.screens.register

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.presentation.ui.screens.login.CustomTextField // Reusing
import com.android.birdlens.presentation.ui.screens.login.SocialLoginButton // Reusing
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun RegisterScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    onNavigateBack: () -> Unit,
    onLoginWithFacebook: () -> Unit, // Placeholder
    onLoginWithX: () -> Unit,      // Placeholder
    onLoginWithApple: () -> Unit,     // Placeholder
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

    val googleOneTapState by googleAuthViewModel.googleSignInOneTapState.collectAsState()
    val backendAuthState by googleAuthViewModel.backendAuthState.collectAsState()
    val firebaseSignInState by googleAuthViewModel.firebaseSignInState.collectAsState()

    // Handle Firebase Sign-In State (final step for all auth flows)
    LaunchedEffect(firebaseSignInState) {
        when (val state = firebaseSignInState) {
            is GoogleAuthViewModel.FirebaseSignInState.Success -> {
                Toast.makeText(context, "Firebase Sign-In Success (after registration/Google): ${state.firebaseUser.uid}", Toast.LENGTH_SHORT).show()
                Log.d("RegisterScreen", "Firebase ID Token: ${state.firebaseIdToken.take(15)}...")
                navController.navigate(Screen.LoginSuccess.route) { // Or directly to Tour screen
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
                googleAuthViewModel.resetFirebaseSignInState()
                googleAuthViewModel.resetBackendAuthState()
                googleAuthViewModel.resetGoogleOneTapState()
            }
            is GoogleAuthViewModel.FirebaseSignInState.Error -> {
                Toast.makeText(context, "Firebase Sign-In Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetFirebaseSignInState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    // Handle Backend Auth State (intermediate step for both traditional and Google)
    LaunchedEffect(backendAuthState) {
        when (val state = backendAuthState) {
            is GoogleAuthViewModel.BackendAuthState.Error -> {
                val operationType = when (state.operation) {
                    GoogleAuthViewModel.AuthOperation.REGISTER -> "Registration"
                    GoogleAuthViewModel.AuthOperation.GOOGLE_SIGN_IN -> "Google Sign-Up"
                    else -> "Authentication"
                }
                Toast.makeText(context, "$operationType Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetBackendAuthState()
            }
            is GoogleAuthViewModel.BackendAuthState.RegistrationSuccess -> {
                // ViewModel automatically calls signInToFirebaseWithCustomToken
                Toast.makeText(context, "Registration with backend successful. Signing into Firebase...", Toast.LENGTH_SHORT).show()
            }
            is GoogleAuthViewModel.BackendAuthState.CustomTokenReceived -> {
                // This is for Google flow, ViewModel automatically calls signInToFirebaseWithCustomToken
                Toast.makeText(context, "Google auth with backend successful. Signing into Firebase...", Toast.LENGTH_SHORT).show()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    // Handle Google OneTap State (initial step for Google Sign-Up)
    LaunchedEffect(googleOneTapState) {
        when (val state = googleOneTapState) {
            is GoogleAuthViewModel.GoogleSignInOneTapState.Error -> {
                Toast.makeText(context, "Google Sign-Up Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetGoogleOneTapState()
            }
            else -> { /* Idle, UILaunching, GoogleIdTokenRetrieved (handled by VM) */ }
        }
    }

    val isLoading = backendAuthState is GoogleAuthViewModel.BackendAuthState.Loading ||
            firebaseSignInState is GoogleAuthViewModel.FirebaseSignInState.Loading ||
            googleOneTapState is GoogleAuthViewModel.GoogleSignInOneTapState.UILaunching

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ... background canvas drawing (no changes needed here) ...
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(color = GreenDeep)
            val path1 = Path().apply {
                moveTo(0f, canvasHeight * 0.1f)
                cubicTo(canvasWidth * 0.2f, canvasHeight * 0.05f, canvasWidth * 0.3f, canvasHeight * 0.4f, canvasWidth * 0.6f, canvasHeight * 0.35f)
                cubicTo(canvasWidth * 0.9f, canvasHeight * 0.3f, canvasWidth * 1.1f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.7f)
                lineTo(0f, canvasHeight * 0.8f); close()
            }
            drawPath(path = path1, brush = Brush.radialGradient(listOf(GreenWave1.copy(alpha = 0.8f), GreenWave3.copy(alpha = 0.6f), GreenDeep.copy(alpha = 0.3f)), center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f), radius = canvasWidth * 0.8f))
            val path2 = Path().apply {
                moveTo(canvasWidth, canvasHeight * 0.5f)
                cubicTo(canvasWidth * 0.8f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.3f, canvasWidth * 0.4f, canvasHeight * 0.4f)
                cubicTo(canvasWidth * 0.1f, canvasHeight * 0.5f, canvasWidth * 0.0f, canvasHeight * 0.9f, canvasWidth * 0.3f, canvasHeight); lineTo(canvasWidth, canvasHeight); close()
            }
            drawPath(path = path2, brush = Brush.linearGradient(listOf(GreenWave4.copy(alpha = 0.4f), GreenWave1.copy(alpha = 0.3f), GreenDeep.copy(alpha = 0.1f)), start = Offset(canvasWidth * 0.8f, canvasHeight * 0.5f), end = Offset(canvasWidth * 0.3f, canvasHeight)))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
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

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    CustomTextField(value = firstName, onValueChange = { firstName = it }, placeholder = "First Name", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(value = lastName, onValueChange = { lastName = it }, placeholder = "Last Name", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(value = ageString, onValueChange = { ageString = it.filter { char -> char.isDigit() } }, placeholder = "Age", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(value = email, onValueChange = { email = it }, placeholder = "Email", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(value = username, onValueChange = { username = it }, placeholder = "Username", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(value = password, onValueChange = { password = it }, placeholder = "Password", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next))
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(value = retypePassword, onValueChange = { retypePassword = it }, placeholder = "Retype password", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done))

                    Spacer(modifier = Modifier.height(20.dp))

                    Button( // Traditional Registration Button
                        onClick = {
                            val age = ageString.toIntOrNull()
                            if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "All fields are required.", Toast.LENGTH_SHORT).show()
                            } else if (age == null || age <= 0) {
                                Toast.makeText(context, "Please enter a valid age.", Toast.LENGTH_SHORT).show()
                            } else if (password.length < 6) {
                                Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                            } else if (password != retypePassword) {
                                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            } else {
                                val request = RegisterRequest(
                                    username = username, password = password, email = email,
                                    firstName = firstName, lastName = lastName, age = age
                                )
                                googleAuthViewModel.registerUser(request)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                        modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                        enabled = !isLoading
                    ) {
                        Text("Continue", color = TextWhite, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    SocialLoginButton( // Google Sign-Up Button
                        text = "Sign up with Google",
                        onClick = {
                            if (!isLoading) {
                                googleAuthViewModel.startGoogleSignIn()
                            }
                        },
                        iconPlaceholder = true, // Replace with actual Google icon
                        enabled = !isLoading
                    )
                    // ... Other social sign-up buttons ...
                    Spacer(modifier = Modifier.height(10.dp))
                    SocialLoginButton(text = "Sign up with Facebook", onClick = onLoginWithFacebook, iconPlaceholder = true, enabled = !isLoading)
                    Spacer(modifier = Modifier.height(10.dp))
                    SocialLoginButton(text = "Sign up with X", onClick = onLoginWithX, iconPlaceholder = true, enabled = !isLoading)
                    Spacer(modifier = Modifier.height(10.dp))
                    SocialLoginButton(text = "Sign up with Apple", onClick = onLoginWithApple, iconPlaceholder = true, enabled = !isLoading)


                    if (isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = TextWhite)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=780dp,dpi=480")
@Composable
fun RegisterScreenPreview() {
    BirdlensTheme {
        val navController = rememberNavController()
        val dummyViewModel = GoogleAuthViewModel() // For preview purposes
        RegisterScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {},
            onLoginWithFacebook = {},
            onLoginWithX = {},
            onLoginWithApple = {}
        )
    }
}