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
import androidx.compose.ui.res.stringResource
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
import androidx.test.core.app.ApplicationProvider
import com.android.birdlens.R
import com.android.birdlens.data.model.request.LoginRequest
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    onNavigateBack: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

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
            }
            is GoogleAuthViewModel.FirebaseSignInState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show() // Using state.message directly
                googleAuthViewModel.resetFirebaseSignInState()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    LaunchedEffect(backendAuthState) {
        when (val state = backendAuthState) {
            is GoogleAuthViewModel.BackendAuthState.Error -> {
                // Display only the message from the backend error state
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetBackendAuthState()
            }
            else -> { /* Idle, Loading, or Success (which triggers Firebase sign-in) */ }
        }
    }

    val isLoading = backendAuthState is GoogleAuthViewModel.BackendAuthState.Loading ||
            firebaseSignInState is GoogleAuthViewModel.FirebaseSignInState.Loading

    // SOLUTION: Get the string resource here, in the Composable scope
    val loginErrorCredentialsText = stringResource(id = R.string.login_error_credentials)
    val backText = stringResource(id = R.string.back)
    val loginText = stringResource(id = R.string.login)
    val loginTitleText = stringResource(id = R.string.log_in_title)
    val emailPlaceholderText = stringResource(id = R.string.email_placeholder)
    val passwordPlaceholderText = stringResource(id = R.string.password_placeholder)
    val forgotPasswordPromptText = stringResource(id = R.string.forgot_password_prompt)
    val clickHereText = stringResource(id = R.string.click_here)
    // val loginWithGoogleText = stringResource(id = R.string.login_with_google) // If social buttons are used

    AuthScreenLayout(
        modifier = modifier,
        topContent = {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp)) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = backText,
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
                    text = loginTitleText,
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
                    placeholder = emailPlaceholderText,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = AuthInputBackground,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = androidx.compose.ui.text.input.ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(16.dp))

                CustomTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = passwordPlaceholderText,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = AuthInputBackground,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fix: Define the annotatedString once to be used by both text and onClick
                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = TextWhite.copy(alpha = 0.8f), fontSize = 13.sp)) {
                        append(forgotPasswordPromptText + " ")
                    }
                    pushStringAnnotation(tag = "CLICK_HERE", annotation = "forgot_password")
                    withStyle(style = SpanStyle(color = ClickableLinkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                        append(clickHereText)
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        // Correctly check for the annotation on the annotatedString that was clicked
                        annotatedString.getStringAnnotations(tag = "CLICK_HERE", start = offset, end = offset)
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
                            Toast.makeText(context, loginErrorCredentialsText, Toast.LENGTH_SHORT).show()
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
                        contentDescription = loginText,
                        tint = TextWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Example if you re-add social login buttons:
                // SocialLoginButton(
                //     text = loginWithGoogleText,
                //     onClick = { /* ... */ },
                //     // ...
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
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    backgroundColor: Color = AuthInputBackground,
    leadingIcon: (@Composable () -> Unit)? = null // Added leadingIcon
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp), // Or 50 for more rounded like search
        modifier = modifier.height(56.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp), // Adjusted padding if icon is present
            textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
            cursorBrush = SolidColor(TextWhite),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    leadingIcon?.invoke() // Render leading icon if provided
                    if (leadingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = TextFieldPlaceholder, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
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
        val dummyViewModel = GoogleAuthViewModel(ApplicationProvider.getApplicationContext())
        LoginScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {},
            onForgotPassword = {}
        )
    }
}