// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/emailverification/EmailVerificationScreen.kt
package com.android.birdlens.presentation.ui.screens.emailverification // Create this package

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun EmailVerificationScreen(
    navController: NavController,
    token: String?,
    userId: String?,
    googleAuthViewModel: GoogleAuthViewModel = viewModel() // Assuming it's available globally or passed down
) {
    val verificationState by googleAuthViewModel.emailVerificationState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(token, userId) {
        if (token != null && userId != null) {
            googleAuthViewModel.verifyEmailToken(token, userId)
        } else {
            // Handle missing token or userId, perhaps navigate back or show an error immediately
            Toast.makeText(context, "Verification link is invalid.", Toast.LENGTH_LONG).show()
            navController.popBackStack(Screen.Welcome.route, inclusive = false)
        }
    }

    AuthScreenLayout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = verificationState) {
                is GoogleAuthViewModel.EmailVerificationState.Idle,
                is GoogleAuthViewModel.EmailVerificationState.Loading -> {
                    CircularProgressIndicator(color = TextWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Verifying your email...", color = TextWhite)
                }
                is GoogleAuthViewModel.EmailVerificationState.Success -> {
                    Text("Email Verified!", style = MaterialTheme.typography.headlineSmall, color = TextWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = TextWhite.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Text("Proceed to Login", color = TextWhite)
                    }
                }
                is GoogleAuthViewModel.EmailVerificationState.Error -> {
                    Text("Verification Failed", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            // Optionally allow retry or navigate to a help screen
                            // For now, navigate back to welcome or login
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                    ) {
                        Text("Back to Login", color = TextWhite)
                    }
                }
            }
        }
    }
    // Reset state when screen is left
    DisposableEffect(Unit) {
        onDispose {
            googleAuthViewModel.resetEmailVerificationState()
        }
    }
}