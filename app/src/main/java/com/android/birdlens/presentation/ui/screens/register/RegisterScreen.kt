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
import com.android.birdlens.presentation.ui.screens.accountinfo.ApplicationProvider
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
// import com.android.birdlens.presentation.ui.screens.login.SocialLoginButton // If used
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun RegisterScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    onNavigateBack: () -> Unit,
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
                Toast.makeText(context, "$operationType Error: ${state.message}", Toast.LENGTH_LONG).show()
                googleAuthViewModel.resetBackendAuthState()
            }
            is GoogleAuthViewModel.BackendAuthState.RegistrationSuccess -> {
                Toast.makeText(context, "Registration with backend successful. Signing into Firebase...", Toast.LENGTH_SHORT).show()
            }
            is GoogleAuthViewModel.BackendAuthState.CustomTokenReceived -> {
                Toast.makeText(context, "Auth with backend successful. Signing into Firebase...", Toast.LENGTH_SHORT).show()
            }
            else -> { /* Idle or Loading */ }
        }
    }

    val isLoading = backendAuthState is GoogleAuthViewModel.BackendAuthState.Loading ||
            firebaseSignInState is GoogleAuthViewModel.FirebaseSignInState.Loading

    // SOLUTION: Get string resources here
    val backText = stringResource(id = R.string.back)
    val createAccountTitleText = stringResource(id = R.string.create_account_title)
    val firstNamePlaceholderText = stringResource(id = R.string.first_name_placeholder)
    val lastNamePlaceholderText = stringResource(id = R.string.last_name_placeholder)
    val agePlaceholderText = stringResource(id = R.string.age_placeholder)
    val emailPlaceholderText = stringResource(id = R.string.email_placeholder)
    val usernamePlaceholderText = stringResource(id = R.string.username_placeholder)
    val passwordPlaceholderText = stringResource(id = R.string.password_placeholder)
    val retypePasswordPlaceholderText = stringResource(id = R.string.retype_password_placeholder)
    val continueButtonText = stringResource(id = R.string.continue_button)
    // val signUpWithGoogleText = stringResource(id = R.string.signup_with_google) // If social buttons used

    val errorAllFieldsText = stringResource(id = R.string.register_error_all_fields)
    val errorValidAgeText = stringResource(id = R.string.register_error_valid_age)
    val errorPasswordLengthText = stringResource(id = R.string.register_error_password_length)
    val errorPasswordsMismatchText = stringResource(id = R.string.register_error_passwords_mismatch)

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

                CustomTextField(value = firstName, onValueChange = { firstName = it }, placeholder = firstNamePlaceholderText, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = lastName, onValueChange = { lastName = it }, placeholder = lastNamePlaceholderText, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = ageString, onValueChange = { ageString = it.filter { char -> char.isDigit() } }, placeholder = agePlaceholderText, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = email, onValueChange = { email = it }, placeholder = emailPlaceholderText, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = username, onValueChange = { username = it }, placeholder = usernamePlaceholderText, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = password, onValueChange = { password = it }, placeholder = passwordPlaceholderText, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next), backgroundColor = AuthInputBackground)
                Spacer(modifier = Modifier.height(12.dp))
                CustomTextField(value = retypePassword, onValueChange = { retypePassword = it }, placeholder = retypePasswordPlaceholderText, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done), backgroundColor = AuthInputBackground)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val age = ageString.toIntOrNull()
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, errorAllFieldsText, Toast.LENGTH_SHORT).show()
                        } else if (age == null || age <= 0) {
                            Toast.makeText(context, errorValidAgeText, Toast.LENGTH_SHORT).show()
                        } else if (password.length < 6) {
                            Toast.makeText(context, errorPasswordLengthText, Toast.LENGTH_SHORT).show()
                        } else if (password != retypePassword) {
                            Toast.makeText(context, errorPasswordsMismatchText, Toast.LENGTH_SHORT).show()
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
                    Text(continueButtonText, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Example if you re-add social login buttons:
                // SocialLoginButton(
                //    text = signUpWithGoogleText,
                //    onClick = { /* ... */ },
                //    // ...
                // )
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
        val dummyViewModel = GoogleAuthViewModel(ApplicationProvider.getApplicationContext())
        RegisterScreen(
            navController = navController,
            googleAuthViewModel = dummyViewModel,
            onNavigateBack = {}
        )
    }
}