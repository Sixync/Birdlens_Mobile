// path: EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/payment/PaymentResultScreen.kt
package com.android.birdlens.presentation.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.GreenDeep
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.Pink80
import com.android.birdlens.ui.theme.TextWhite

@Composable
fun PaymentResultScreen(
    navController: NavController,
    isSuccess: Boolean,
    accountInfoViewModel: AccountInfoViewModel // Accept the ViewModel as a parameter
) {
    // Logic: If the payment was successful, this LaunchedEffect triggers a single time
    // to call `fetchCurrentUser` on the AccountInfoViewModel. This ensures the app's
    // user profile state is updated immediately with the new subscription information.
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            accountInfoViewModel.fetchCurrentUser()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(GreenDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (isSuccess) "Success" else "Failed",
                tint = if (isSuccess) GreenWave2 else Pink80,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isSuccess) "Payment Successful!" else "Payment Cancelled",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = TextWhite),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSuccess) "Your ExBird subscription is now active." else "Your payment was not completed. Please try again.",
                style = MaterialTheme.typography.bodyLarge.copy(color = TextWhite.copy(alpha = 0.8f)),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp)
            ) {
                Text(if (isSuccess) "Explore Premium" else "Back to Home", color = TextWhite, fontSize = 16.sp)
            }
        }
    }
}