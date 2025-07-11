package com.android.birdlens.presentation.ui.screens.register

import android.annotation.SuppressLint
import android.app.Application
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
import androidx.compose.ui.res.stringResource
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
import com.android.birdlens.R
import com.android.birdlens.data.model.request.RegisterRequest
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*
import java.util.regex.Pattern

@Composable
fun RegisterScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var retypePassword by remember { mutableStateOf("") }
    var referralCodeInput by remember { mutableStateOf(googleAuthViewModel.referralCodeFromLink ?: "") }

    val context = LocalContext.current

    val backendAuthState by googleAuthViewModel.backendAuthState.collectAsState()
    val firebaseSignInState by googleAuthViewModel.firebaseSignInState.collectAsState()

    val emailPattern = remember { Pattern.compile("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+") }

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
                    else -> "Authentication"
                }
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                Log.e("RegisterScreen", "$operationType Backend Error: ${state.message}")
                googleAuthViewModel.resetBackendAuthState()
            }
            is GoogleAuthViewModel.BackendAuthState.RegistrationSuccess -> {
                Toast.makeText(context, "Registration initiated. Signing into Firebase...", Toast.LENGTH_SHORT).show()
                Log.d("RegisterScreen", "Registration with backend successful. Got custom token.")
            }
            is GoogleAuthViewModel.BackendAuthState.CustomTokenReceived -> {
                Toast.makeText(context, "Authentication progressing...", Toast.LENGTH_SHORT).show()
                Log.d("RegisterScreen", "Auth with backend successful (custom token received). Signing into Firebase...")
            }
            else -> { /* Idle or Loading */ }
        }
    }

    val isLoading = backendAuthState is GoogleAuthViewModel.BackendAuthState.Loading ||
            firebaseSignInState is GoogleAuthViewModel.FirebaseSignInState.Loading

    val backText = stringResource(id = R.string.back)
    val createAccountTitleText = stringResource(id = R.string.create_account_title)
    val emailPlaceholderText = stringResource(id = R.string.email_placeholder)
    val passwordPlaceholderText = stringResource(id = R.string.password_placeholder)
    val retypePasswordPlaceholderText = stringResource(id = R.string.retype_password_placeholder)
    val continueButtonText = stringResource(id = R.string.continue_button)

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
                    text = createAccountTitleText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                CustomTextField(value = email, onValueChange = { email = it }, placeholder = emailPlaceholderText, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = password, onValueChange = { password = it }, placeholder = passwordPlaceholderText, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = retypePassword, onValueChange = { retypePassword = it }, placeholder = retypePasswordPlaceholderText, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = referralCodeInput, onValueChange = { referralCodeInput = it }, placeholder = "Referral Code (Optional)", modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done), backgroundColor = AuthInputBackground)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        val trimmedPassword = password.trim()
                        val trimmedRetypePassword = retypePassword.trim()
                        val trimmedReferralCode = referralCodeInput.trim()

                        var errorMessage = ""

                        if (trimmedEmail.isBlank() || trimmedPassword.isBlank() || trimmedRetypePassword.isBlank()) {
                            errorMessage = context.getString(R.string.register_error_all_fields)
                        } else if (trimmedPassword.length < 3) {
                            errorMessage = context.getString(R.string.register_error_password_min_length, 3)
                        } else if (trimmedPassword != trimmedRetypePassword) {
                            errorMessage = context.getString(R.string.register_error_passwords_mismatch)
                        } else if (!emailPattern.matcher(trimmedEmail).matches()) {
                            errorMessage = context.getString(R.string.register_error_invalid_email)
                        }

                        if (errorMessage.isNotEmpty()) {
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                        } else {
                            val request = RegisterRequest(
                                email = trimmedEmail,
                                password = trimmedPassword,
                                referralCode = if(trimmedReferralCode.isBlank()) null else trimmedReferralCode
                            )
                            googleAuthViewModel.registerUser(request)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                    modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                    enabled = !isLoading
                ) {
                    Text(continueButtonText, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(20.dp))

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