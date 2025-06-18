// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/resetpassword/ResetPasswordScreen.kt
package com.android.birdlens.presentation.ui.screens.resetpassword

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
import com.android.birdlens.presentation.viewmodel.ForgotPasswordViewModel
import com.android.birdlens.presentation.viewmodel.PasswordResetState
import com.android.birdlens.ui.theme.AuthCardBackground
import com.android.birdlens.ui.theme.AuthInputBackground
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun ResetPasswordScreen(
    navController: NavController,
    token: String,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val state by viewModel.resetPasswordState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        when (val currentState = state) {
            is PasswordResetState.Success -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                viewModel.resetResetPasswordState()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            }
            is PasswordResetState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                viewModel.resetResetPasswordState()
            }
            else -> {}
        }
    }

    AuthScreenLayout {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = AuthCardBackground,
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Reset Your Password", style = MaterialTheme.typography.headlineSmall, color = TextWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Please enter your new password below.",
                        textAlign = TextAlign.Center,
                        color = TextWhite.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CustomTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = "New Password",
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        backgroundColor = AuthInputBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CustomTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm New Password",
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        backgroundColor = AuthInputBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (newPassword.length < 3) {
                                Toast.makeText(context, "Password must be at least 3 characters.", Toast.LENGTH_SHORT).show()
                            } else if (newPassword != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.resetPassword(token, newPassword)
                            }
                        },
                        enabled = state !is PasswordResetState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        if (state is PasswordResetState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextWhite)
                        } else {
                            Text("Reset Password", color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}