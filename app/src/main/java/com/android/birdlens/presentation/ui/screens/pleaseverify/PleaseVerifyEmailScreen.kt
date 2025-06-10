// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/pleaseverify/PleaseVerifyEmailScreen.kt
package com.android.birdlens.presentation.ui.screens.pleaseverify // Create this package

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.android.birdlens.R
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel // Assuming for sign out
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun PleaseVerifyEmailScreen(
    navController: NavController,
    email: String?,
    googleAuthViewModel: GoogleAuthViewModel // Or a different ViewModel if resend logic is complex
) {
    val context = LocalContext.current

    AuthScreenLayout {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Verify Your Email",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "We've sent a confirmation link to ${email ?: "your email address"}.",
                textAlign = TextAlign.Center,
                color = TextWhite.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Please check your inbox (and spam folder) and click the link to activate your account.",
                textAlign = TextAlign.Center,
                color = TextWhite.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    // TODO: Implement Resend Verification Email Logic
                    // This would typically involve:
                    // 1. A new backend endpoint like /auth/resend-verification
                    // 2. ViewModel function to call this endpoint.
                    // Toast.makeText(context, "Resend email (not implemented yet)", Toast.LENGTH_SHORT).show()

                    // For now, just log out or go to login
                    googleAuthViewModel.signOut(context)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text("Resend Email / Go to Login", color = TextWhite)
            }
        }
    }
}