// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/premium/PremiumScreen.kt
package com.android.birdlens.presentation.ui.screens.premium

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.data.model.Subscription
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.ui.screens.payment.CheckoutActivity
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.presentation.viewmodel.GenericUiState
import com.android.birdlens.presentation.viewmodel.SubscriptionViewModel
import com.android.birdlens.ui.theme.*
import java.util.Locale

@Composable
fun PremiumScreen(
    navController: NavController,
    subscriptionViewModel: SubscriptionViewModel = viewModel(),
    accountInfoViewModel: AccountInfoViewModel = viewModel()
) {
    val subscriptionState by subscriptionViewModel.subscriptionsState.collectAsState()
    val accountState by accountInfoViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val isAlreadySubscribed = (accountState as? AccountInfoUiState.Success)?.user?.subscription == "ExBird"

    // Logic: This launcher starts the CheckoutActivity and waits for a result.
    val checkoutLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Payment was successful. Show feedback and refresh the user's profile data.
            Toast.makeText(context, "Subscription successful! Refreshing profile...", Toast.LENGTH_SHORT).show()
            accountInfoViewModel.fetchCurrentUser()
        } else {
            // Payment was cancelled or failed.
            Toast.makeText(context, "Payment process was not completed.", Toast.LENGTH_SHORT).show()
        }
    }

    // Logic: This ensures the latest user data is fetched every time this screen is entered.
    // This prevents the user from seeing the "Subscribe" button if they already have a subscription.
    LaunchedEffect(key1 = Unit) {
        accountInfoViewModel.fetchCurrentUser()
    }

    AppScaffold(
        navController = navController,
        topBar = { SimpleTopAppBar(title = "Birdlens Premium") }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GreenDeep, VeryDarkGreenBase)
                    )
                )
        ) {
            when (val state = subscriptionState) {
                is GenericUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is GenericUiState.Error -> {
                    Text(
                        "Error: ${state.message}",
                        color = TextWhite.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                is GenericUiState.Success -> {
                    val exBirdSubscription = state.data.find { it.name == "ExBird" }
                    if (exBirdSubscription != null) {
                        PremiumContent(
                            subscription = exBirdSubscription,
                            isSubscribed = isAlreadySubscribed,
                            onSubscribeClick = {
                                // Logic: Launch the CheckoutActivity using the new launcher.
                                val intent = Intent(context, CheckoutActivity::class.java)
                                checkoutLauncher.launch(intent)
                            }
                        )
                    } else {
                        Text(
                            "The ExBird subscription plan is currently unavailable.",
                            color = TextWhite.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is GenericUiState.Idle -> {
                    // Handled by loading state
                }
            }
        }
    }
}

@Composable
fun PremiumContent(
    subscription: Subscription,
    isSubscribed: Boolean,
    onSubscribeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = "Premium",
            tint = GreenWave2,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = subscription.name,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = TextWhite)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subscription.description,
            style = MaterialTheme.typography.bodyLarge.copy(color = TextWhite.copy(alpha = 0.8f)),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        FeatureList()

        Spacer(modifier = Modifier.weight(1f))

        if (isSubscribed) {
            Text(
                "You are a Premium Member!",
                style = MaterialTheme.typography.titleLarge.copy(color = GreenWave2, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Button(
                onClick = onSubscribeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text(
                    text = String.format(Locale.US, "Subscribe Now for $%.2f", subscription.price),
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Billed once. Renews automatically every ${subscription.durationDays} days. Cancel anytime.",
            style = MaterialTheme.typography.bodySmall.copy(color = TextWhite.copy(alpha = 0.6f)),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FeatureList() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FeatureItem(icon = Icons.Default.Check, text = "Unlimited AI Bird Identifications")
        FeatureItem(icon = Icons.Default.Check, text = "Ad-Free Experience")
        FeatureItem(icon = Icons.Default.Check, text = "Exclusive Community Badges")
        FeatureItem(icon = Icons.Default.Check, text = "Access to Advanced Map Layers")
    }
}

@Composable
fun FeatureItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GreenWave2,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(GreenWave2.copy(alpha = 0.15f))
                .padding(4.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = TextWhite, fontSize = 16.sp)
    }
}