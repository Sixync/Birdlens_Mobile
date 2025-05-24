// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/register/RegisterScreen.kt
package com.android.birdlens.presentation.ui.screens.register

import android.annotation.SuppressLint
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
    onLoginWithFacebook: () -> Unit,
    onLoginWithX: () -> Unit,
    onLoginWithApple: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var ageString by remember { mutableStateOf("") } // Age as string for TextField

    val context = LocalContext.current
    val googleSignInState by googleAuthViewModel.googleAuthState.collectAsState()
    val registrationApiState by googleAuthViewModel.registrationState.collectAsState()

    // Handle Google Sign-In State
    LaunchedEffect(googleSignInState) {
        when (val state = googleSignInState) {
            is GoogleAuthViewModel.GoogleSignInState.Success -> {
                Toast.makeText(context, "Google Sign-Up/In Success: ${state.user.displayName}", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.LoginSuccess.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
                googleAuthViewModel.resetGoogleAuthState()
            }
            is GoogleAuthViewModel.GoogleSignInState.Error -> {
                Toast.makeText(context, "Google Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetGoogleAuthState()
            }
            else -> { /* Idle or Loading for Google Sign-In */ }
        }
    }

    // Handle Traditional Registration API State
    LaunchedEffect(registrationApiState) {
        when (val state = registrationApiState) {
            is GoogleAuthViewModel.RegistrationState.Success -> {
                Toast.makeText(context, "Registration Successful! Please login.", Toast.LENGTH_LONG).show()
                navController.navigate(Screen.Login.route) { // Navigate to Login after successful registration
                    popUpTo(Screen.Welcome.route) // Clear back stack up to Welcome
                }
                googleAuthViewModel.resetRegistrationState()
            }
            is GoogleAuthViewModel.RegistrationState.Error -> {
                Toast.makeText(context, "Registration API Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetRegistrationState()
            }
            else -> { /* Idle or Loading for API registration */ }
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ... background canvas drawing ...
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

            Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

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
                        .verticalScroll(rememberScrollState()) // Make content scrollable
                        .padding(horizontal = 24.dp, vertical = 24.dp), // Adjusted vertical padding
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CREATE YOUR ACCOUNT",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            letterSpacing = 1.sp // Adjusted letter spacing
                        ),
                        modifier = Modifier.padding(bottom = 20.dp) // Adjusted padding
                    )

                    CustomTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = "First Name",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        placeholder = "Last Name",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = ageString,
                        onValueChange = { ageString = it.filter { char -> char.isDigit() } }, // Allow only digits
                        placeholder = "Age",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Username",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(
                        value = retypePassword,
                        onValueChange = { retypePassword = it },
                        placeholder = "Retype password",
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val age = ageString.toIntOrNull()
                            if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "All fields except Avatar are required.", Toast.LENGTH_SHORT).show()
                            } else if (age == null || age <= 0) {
                                Toast.makeText(context, "Please enter a valid age.", Toast.LENGTH_SHORT).show()
                            }
                            else if (password != retypePassword) {
                                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            } else {
                                val request = RegisterRequest(
                                    username = username,
                                    password = password,
                                    email = email,
                                    firstName = firstName,
                                    lastName = lastName,
                                    age = age
                                    // avatarUrl can be added later if you have an input for it
                                )
                                googleAuthViewModel.registerUser(request)
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                        modifier = Modifier
                            .fillMaxWidth(0.7f) // Slightly wider button
                            .height(50.dp),
                        enabled = registrationApiState !is GoogleAuthViewModel.RegistrationState.Loading && googleSignInState !is GoogleAuthViewModel.GoogleSignInState.Loading
                    ) {
                        Text("Continue", color = TextWhite, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp)) // Space before social logins

                    SocialLoginButton(
                        text = "Sign up with Google",
                        onClick = {
                            if (googleSignInState !is GoogleAuthViewModel.GoogleSignInState.Loading) {
                                googleAuthViewModel.startGoogleSignIn(isSignUp = true)
                            }
                        },
                        iconPlaceholder = true,
                        enabled = registrationApiState !is GoogleAuthViewModel.RegistrationState.Loading && googleSignInState !is GoogleAuthViewModel.GoogleSignInState.Loading
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SocialLoginButton(text = "Sign up with Facebook", onClick = onLoginWithFacebook, iconPlaceholder = true, enabled = registrationApiState !is GoogleAuthViewModel.RegistrationState.Loading && googleSignInState !is GoogleAuthViewModel.GoogleSignInState.Loading)
                    Spacer(modifier = Modifier.height(10.dp))
                    SocialLoginButton(text = "Sign up with X", onClick = onLoginWithX, iconPlaceholder = true, enabled = registrationApiState !is GoogleAuthViewModel.RegistrationState.Loading && googleSignInState !is GoogleAuthViewModel.GoogleSignInState.Loading)
                    Spacer(modifier = Modifier.height(10.dp))
                    SocialLoginButton(text = "Sign up with Apple", onClick = onLoginWithApple, iconPlaceholder = true, enabled = registrationApiState !is GoogleAuthViewModel.RegistrationState.Loading && googleSignInState !is GoogleAuthViewModel.GoogleSignInState.Loading)

                    if (registrationApiState is GoogleAuthViewModel.RegistrationState.Loading || googleSignInState is GoogleAuthViewModel.GoogleSignInState.Loading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = TextWhite)
                    }
                    Spacer(modifier = Modifier.height(16.dp)) // Bottom padding
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
        val dummyViewModel = GoogleAuthViewModel()
        RegisterScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {},
            // onRegistrationSuccess = {}, // Removed, handled by ViewModel state
            onLoginWithFacebook = {},
            onLoginWithX = {},
            onLoginWithApple = {}
        )
    }
}