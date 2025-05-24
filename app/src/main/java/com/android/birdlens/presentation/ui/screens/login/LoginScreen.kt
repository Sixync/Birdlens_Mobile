// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/login/LoginScreen.kt
package com.android.birdlens.presentation.ui.screens.login

import android.annotation.SuppressLint
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
    onLoginSuccess: () -> Unit, // This will be triggered by ViewModel state now
    onForgotPassword: () -> Unit,
    onLoginWithFacebook: () -> Unit,
    onLoginWithX: () -> Unit,
    onLoginWithApple: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val googleSignInState by googleAuthViewModel.googleAuthState.collectAsState()
    val traditionalLoginApiState by googleAuthViewModel.traditionalLoginState.collectAsState()

    // Handle Google Sign-In State
    LaunchedEffect(googleSignInState) {
        when (val state = googleSignInState) {
            is GoogleAuthViewModel.GoogleSignInState.Success -> {
                Toast.makeText(context, "Google Sign-In Success: ${state.user.displayName}", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.LoginSuccess.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true}
                }
                googleAuthViewModel.resetGoogleAuthState()
            }
            is GoogleAuthViewModel.GoogleSignInState.Error -> {
                Toast.makeText(context, "Google Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetGoogleAuthState()
            }
            else -> { /* Do nothing for Loading or Idle in LaunchedEffect */ }
        }
    }

    // Handle Traditional Login API State
    LaunchedEffect(traditionalLoginApiState) {
        when (val state = traditionalLoginApiState) {
            is GoogleAuthViewModel.TraditionalLoginState.Success -> {
                Toast.makeText(context, "Login Successful: Welcome ${state.loginData.username ?: "User"}", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.LoginSuccess.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true}
                }
                googleAuthViewModel.resetTraditionalLoginState()
            }
            is GoogleAuthViewModel.TraditionalLoginState.Error -> {
                Toast.makeText(context, "Login Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetTraditionalLoginState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    val isLoading = googleSignInState is GoogleAuthViewModel.GoogleSignInState.Loading ||
            traditionalLoginApiState is GoogleAuthViewModel.TraditionalLoginState.Loading


    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // ... background drawing ...
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
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    CustomTextField(
                        value = username,
                        onValueChange = { username = it },
                        placeholder = "Username",
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
                            if (username.isNotBlank() && password.isNotBlank()) {
                                googleAuthViewModel.loginUser(LoginRequest(username, password))
                            } else {
                                Toast.makeText(context, "Please enter username and password", Toast.LENGTH_SHORT).show()
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

                    SocialLoginButton(
                        text = "Login with Google",
                        onClick = {
                            if (!isLoading) {
                                googleAuthViewModel.startGoogleSignIn(isSignUp = false)
                            }
                        },
                        iconPlaceholder = true,
                        enabled = !isLoading
                    )
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

// ... (CustomTextField and SocialLoginButton composables remain the same) ...
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
                Box(modifier = Modifier.size(24.dp))
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
        val dummyViewModel = GoogleAuthViewModel()
        LoginScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {},
            onLoginSuccess = {},
            onForgotPassword = {},
            onLoginWithFacebook = {},
            onLoginWithX = {},
            onLoginWithApple = {}
        )
    }
}