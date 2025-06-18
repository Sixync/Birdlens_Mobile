// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/forgotpassword/ForgotPasswordScreen.kt
package com.android.birdlens.presentation.ui.screens.forgotpassword

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.ui.screens.login.CustomTextField
import com.android.birdlens.presentation.viewmodel.ForgotPasswordViewModel
import com.android.birdlens.presentation.viewmodel.PasswordResetState
import com.android.birdlens.ui.theme.AuthCardBackground
import com.android.birdlens.ui.theme.AuthInputBackground
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    val state by viewModel.forgotPasswordState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        when (val currentState = state) {
            is PasswordResetState.Success -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                viewModel.resetForgotPasswordState()
                navController.popBackStack()
            }
            is PasswordResetState.Error -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                viewModel.resetForgotPasswordState()
            }
            else -> {}
        }
    }

    AuthScreenLayout(
        topContent = {
            Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp)) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                }
            }
        }
    ) {
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
                    Text("Forgot Password", style = MaterialTheme.typography.headlineSmall, color = TextWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        textAlign = TextAlign.Center,
                        color = TextWhite.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Your email address",
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = AuthInputBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.requestPasswordReset(email) },
                        enabled = state !is PasswordResetState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        if (state is PasswordResetState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextWhite)
                        } else {
                            Text("Send Reset Link", color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}