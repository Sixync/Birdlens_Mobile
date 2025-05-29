// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/login/LoginScreen.kt
package com.android.birdlens.presentation.ui.screens.login

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.ui.screens.accountinfo.ApplicationProvider
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    onNavigateBack: () -> Unit,
    onForgotPassword: () -> Unit,
    // Removed onLoginWithFacebook, onLoginWithX, onLoginWithApple as per simplification
    // onLoginWithFacebook: () -> Unit,
    // onLoginWithX: () -> Unit,
    // onLoginWithApple: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Removed googleOneTapState as Google Sign-In flow is simplified
    // val googleOneTapState by googleAuthViewModel.googleSignInOneTapState.collectAsState()
    val backendAuthState by googleAuthViewModel.backendAuthState.collectAsState()
    val firebaseSignInState by googleAuthViewModel.firebaseSignInState.collectAsState()

    LaunchedEffect(firebaseSignInState) {
        when (val state = firebaseSignInState) {
            is GoogleAuthViewModel.FirebaseSignInState.Success -> {
                Toast.makeText(context, "Firebase Sign-In Success: ${state.firebaseUser.uid}", Toast.LENGTH_SHORT).show()
                Log.d("LoginScreen", "Firebase ID Token: ${state.firebaseIdToken.take(15)}...")
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
                Toast.makeText(context, "Auth Error (${state.operation}): ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetBackendAuthState()
            }
            else -> { /* Idle, Loading, or Success (which triggers Firebase sign-in) */ }
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LOG IN",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                CustomTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = AuthInputBackground,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = AuthInputBackground,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                )

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

                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            googleAuthViewModel.loginUser(LoginRequest(email, password))
                        } else {
                            Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                        }
                    },
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

                // Removed SocialLoginButton for Google
                // Removed SocialLoginButton for Facebook
                // Removed SocialLoginButton for X
                // Removed SocialLoginButton for Apple
                // Example:
                // SocialLoginButton(
                //     text = "Login with Google",
                //     onClick = {
                //         if (!isLoading) {
                //             // googleAuthViewModel.startGoogleSignIn() // Removed
                //             Toast.makeText(context, "Google login not available.", Toast.LENGTH_SHORT).show()
                //         }
                //     },
                //     iconPlaceholder = true,
                //     enabled = !isLoading,
                //     backgroundColor = SocialButtonBackgroundLight,
                //     contentColor = SocialButtonTextDark
                // )
                // Spacer(modifier = Modifier.height(12.dp))


                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(color = TextWhite)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
    }
}

// ... (CustomTextField and SocialLoginButton composables remain the same, though SocialLoginButton might be unused now or adapted)
// SocialLoginButton might be removed if no social logins are presented. For now, keeping its definition if other parts of app might use it.
// If truly unused, its definition can be removed as well.

// ...
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    backgroundColor: Color = AuthInputBackground
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(56.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
    enabled: Boolean = true,
    backgroundColor: Color = SocialButtonBackgroundLight,
    contentColor: Color = SocialButtonTextDark
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (iconPlaceholder) Arrangement.Start else Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (iconPlaceholder) {
                Box(modifier = Modifier.size(20.dp)) // Placeholder for an icon
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
        val dummyViewModel = GoogleAuthViewModel(ApplicationProvider.getApplicationContext()) // Pass context
        LoginScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {},
            onForgotPassword = {}
            // Removed Facebook, X, Apple click handlers from preview call
        )
    }
}