// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/loginsuccess/LoginSuccessScreen.kt
package com.android.birdlens.presentation.ui.screens.loginsuccess

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.R
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AuthScreenLayout
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun LoginSuccessScreen(
    navController: NavController,
    accountInfoViewModel: AccountInfoViewModel,
    modifier: Modifier = Modifier
) {
    val accountState by accountInfoViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        accountInfoViewModel.fetchCurrentUser()
    }

    LaunchedEffect(accountState) {
        when (val state = accountState) {
            is AccountInfoUiState.Success -> {
                val welcomeMessage = context.getString(R.string.welcome_user, state.user.firstName)
                Toast.makeText(context, welcomeMessage, Toast.LENGTH_SHORT).show()
                delay(500)

                if (state.user.emailVerified) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.PleaseVerifyEmail.createRoute(state.user.email)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
                // After the navigation is triggered, reset the state to prevent this
                // LaunchedEffect from re-triggering the toast on recomposition.
                accountInfoViewModel.resetUiStateToIdle()
            }
            is AccountInfoUiState.Error -> {
                Toast.makeText(context, "Error fetching profile: ${state.message}. Please log in again.", Toast.LENGTH_LONG).show()
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            }
            AccountInfoUiState.Loading -> { /* Show loading indicator, already handled below */ }
            AccountInfoUiState.Idle -> { /* Initial state, waiting for fetch */ }
        }
    }


    AuthScreenLayout(modifier = modifier) {
        Spacer(modifier = Modifier.weight(1f)) // Pushes the card to center/bottom

        Surface(
            color = AuthCardBackground,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (accountState is AccountInfoUiState.Loading || accountState is AccountInfoUiState.Idle) {
                    CircularProgressIndicator(color = TextWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.login_successful_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        "Finalizing setup...", // Or a more generic loading message
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextWhite.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    )
                } else {
                    // This part might not be seen for long due to navigation logic above
                    // but acts as a fallback visual.
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.purchase_success_icon_description),
                        tint = GreenWave2,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.login_successful_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.welcome_to_app),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextWhite.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(0.5f)) // Some space at the bottom
        Spacer(modifier = Modifier.height(WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp))
    }
}