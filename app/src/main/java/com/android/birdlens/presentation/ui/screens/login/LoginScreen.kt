// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/login/LoginScreen.kt
package com.android.birdlens.presentation.ui.screens.login

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    onNavigateBack: () -> Unit,
    // onLoginSuccess is now handled by observing FirebaseSignInState
    onForgotPassword: () -> Unit,
    onLoginWithFacebook: () -> Unit, // Assuming these are placeholders for now
    onLoginWithX: () -> Unit,
    onLoginWithApple: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") } // Changed from username
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    val googleOneTapState by googleAuthViewModel.googleSignInOneTapState.collectAsState()
    val backendAuthState by googleAuthViewModel.backendAuthState.collectAsState()
    val firebaseSignInState by googleAuthViewModel.firebaseSignInState.collectAsState()

    // Handle Firebase Sign-In State (final step for all auth flows)
    LaunchedEffect(firebaseSignInState) {
        when (val state = firebaseSignInState) {
            is GoogleAuthViewModel.FirebaseSignInState.Success -> {
                Toast.makeText(context, "Firebase Sign-In Success: ${state.firebaseUser.uid}", Toast.LENGTH_SHORT).show()
                // You have state.firebaseIdToken to use for your backend API calls
                Log.d("LoginScreen", "Firebase ID Token: ${state.firebaseIdToken.take(15)}...")
                navController.navigate(Screen.LoginSuccess.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
                googleAuthViewModel.resetFirebaseSignInState() // Reset after handling
                googleAuthViewModel.resetBackendAuthState() // Also reset previous step's state
                googleAuthViewModel.resetGoogleOneTapState() // Reset if it was Google flow
            }
            is GoogleAuthViewModel.FirebaseSignInState.Error -> {
                Toast.makeText(context, "Firebase Sign-In Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetFirebaseSignInState()
            }
            else -> { /* Idle or Loading for Firebase Sign-In */ }
        }
    }

    // Handle Backend Auth State (intermediate step)
    LaunchedEffect(backendAuthState) {
        when (val state = backendAuthState) {
            is GoogleAuthViewModel.BackendAuthState.Error -> {
                Toast.makeText(context, "Auth Error (${state.operation}): ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetBackendAuthState()
            }
            // CustomTokenReceived is handled by ViewModel calling signInToFirebaseWithCustomToken
            // RegistrationSuccess also leads to signInToFirebaseWithCustomToken
            else -> { /* Idle, Loading, or Success (which triggers Firebase sign-in) */ }
        }
    }

    // Handle Google OneTap State (initial step for Google Sign-In)
    LaunchedEffect(googleOneTapState) {
        when (val state = googleOneTapState) {
            is GoogleAuthViewModel.GoogleSignInOneTapState.Error -> {
                Toast.makeText(context, "Google Sign-In Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetGoogleOneTapState()
            }
            // UILaunching and GoogleIdTokenRetrieved are intermediate states handled by VM
            else -> { /* Idle, UILaunching, GoogleIdTokenRetrieved */ }
        }
    }


    val isLoading = backendAuthState is GoogleAuthViewModel.BackendAuthState.Loading ||
            firebaseSignInState is GoogleAuthViewModel.FirebaseSignInState.Loading ||
            googleOneTapState is GoogleAuthViewModel.GoogleSignInOneTapState.UILaunching


    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ... background drawing (no changes needed here) ...
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(color = GreenDeep)
            val path1 = Path().apply {
                moveTo(0f, canvasHeight * 0.1f)
                cubicTo(
                    canvasWidth * 0.2f, canvasHeight * 0.05f,
                    canvasWidth * 0.3f, canvasHeight * 0.4f,
                    canvasWidth * 0.6f, canvasHeight * 0.35f
                )
                cubicTo(
                    canvasWidth * 0.9f, canvasHeight * 0.3f,
                    canvasWidth * 1.1f, canvasHeight * 0.6f,
                    canvasWidth * 0.7f, canvasHeight * 0.7f
                )
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
            // ... Back button ... (no changes)
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

            Spacer(modifier = Modifier.height(16.dp))

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
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LOG IN",
                        // ... style ...
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    CustomTextField(
                        value = email, // Changed from username
                        onValueChange = { email = it },
                        placeholder = "Email", // Changed from Username
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ... Forgot password text ... (no changes)
                    Spacer(modifier = Modifier.height(12.dp))

                    ClickableText(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = TextWhite.copy(alpha = 0.8f), fontSize = 13.sp)) {
                                append("Forget your password? ")
                            }
                            pushStringAnnotation(tag = "CLICK_HERE", annotation = "forgot_password")
                            withStyle(style = SpanStyle(color = ClickableLinkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                                append("Click here")
                            }
                            pop()
                        },
                        onClick = { offset ->
                            buildAnnotatedString {}.getStringAnnotations(tag = "CLICK_HERE", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    onForgotPassword()
                                }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )


                    Spacer(modifier = Modifier.height(24.dp))

                    Button( // Login Button
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                googleAuthViewModel.loginUser(LoginRequest(email, password))
                            } else {
                                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            }
                        },
                        // ... style, enabled state ...
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                        modifier = Modifier.size(60.dp),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Login",
                            tint = TextWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    SocialLoginButton( // Google Login Button
                        text = "Login with Google",
                        onClick = {
                            if (!isLoading) {
                                googleAuthViewModel.startGoogleSignIn()
                            }
                        },
                        // ... style, enabled state ...
                        iconPlaceholder = true, // Replace with actual Google icon later
                        enabled = !isLoading
                    )
                    // ... Other social login buttons ...
                    Spacer(modifier = Modifier.height(12.dp))
                    SocialLoginButton(text = "Login with Facebook", onClick = onLoginWithFacebook, iconPlaceholder = true, enabled = !isLoading)
                    Spacer(modifier = Modifier.height(12.dp))
                    SocialLoginButton(text = "Login with X", onClick = onLoginWithX, iconPlaceholder = true, enabled = !isLoading)
                    Spacer(modifier = Modifier.height(12.dp))
                    SocialLoginButton(text = "Login with Apple", onClick = onLoginWithApple, iconPlaceholder = true, enabled = !isLoading)


                    if (isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = TextWhite)
                    }
                }
            }
        }
    }
}

// ... CustomTextField and SocialLoginButton composables remain the same ...
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Surface(
        color = ButtonGreen,
        shape = RoundedCornerShape(50),
        modifier = modifier.height(50.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
            cursorBrush = SolidColor(TextWhite),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = TextFieldPlaceholder, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconPlaceholder: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = SocialButtonBackground,
            contentColor = SocialButtonText,
            disabledContainerColor = SocialButtonBackground.copy(alpha = 0.5f),
            disabledContentColor = SocialButtonText.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconPlaceholder) {
                // TODO: Replace with actual icon if available
                // For example: Icon(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = "Google")
                Box(modifier = Modifier.size(24.dp)) // Placeholder for icon
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}


@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=780dp,dpi=480")
@Composable
fun LoginScreenPreview() {
    BirdlensTheme {
        val navController = rememberNavController()
        // In a real app, use a ViewModel factory or Hilt for ViewModel injection
        val dummyViewModel = GoogleAuthViewModel() // For preview only
        LoginScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {},
            onForgotPassword = {},
            onLoginWithFacebook = {},
            onLoginWithX = {},
            onLoginWithApple = {}
        )
    }
}