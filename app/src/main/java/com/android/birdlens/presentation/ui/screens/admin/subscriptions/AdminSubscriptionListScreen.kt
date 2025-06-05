// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/admin/subscriptions/AdminSubscriptionListScreen.kt
package com.android.birdlens.presentation.ui.screens.admin.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.R
import com.android.birdlens.data.model.Subscription
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.AdminSubscriptionViewModel
import com.android.birdlens.presentation.viewmodel.SubscriptionUIState
import com.android.birdlens.ui.theme.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AdminSubscriptionListScreen(
    navController: NavController,
    adminSubscriptionViewModel: AdminSubscriptionViewModel = viewModel()
) {
    val subscriptionsState by adminSubscriptionViewModel.subscriptionsState.collectAsState()

    LaunchedEffect(Unit) {
        adminSubscriptionViewModel.fetchSubscriptions()
    }

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "Admin: Subscriptions",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AdminCreateSubscription.route) },
                containerColor = ButtonGreen,
                contentColor = TextWhite
            ) {
                Icon(Icons.Filled.Add, "Create Subscription")
            }
        },
        showBottomBar = false // Or true if it's part of a main admin flow
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = subscriptionsState) {
                is SubscriptionUIState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is SubscriptionUIState.Success -> {
                    val subscriptions = state.data
                    if (subscriptions.isEmpty()) {
                        Text(
                            "No subscriptions found.",
                            color = TextWhite.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(subscriptions, key = { it.id }) { subscription ->
                                SubscriptionItemCard(subscription = subscription)
                            }
                        }
                    }
                }
                is SubscriptionUIState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Error: ${state.message}", color = TextWhite.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { adminSubscriptionViewModel.fetchSubscriptions() },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) {
                            Text(stringResource(R.string.retry), color = TextWhite)
                        }
                    }
                }
                is SubscriptionUIState.Idle -> {
                    Text("Idle", color = TextWhite.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun SubscriptionItemCard(subscription: Subscription) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = subscription.name,
                style = MaterialTheme.typography.titleLarge.copy(color = TextWhite, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subscription.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.8f)),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoChip(
                    icon = Icons.Filled.AttachMoney,
                    text = "%.2f".format(subscription.price),
                    iconColor = GreenWave2
                )
                InfoChip(
                    icon = Icons.Filled.CalendarToday,
                    text = "${subscription.durationDays} days",
                    iconColor = GreenWave3
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Created: ${formatSubscriptionDate(subscription.createdAt)}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextWhite.copy(alpha = 0.6f))
            )
            subscription.updatedAt?.let {
                Text(
                    text = "Updated: ${formatSubscriptionDate(it)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextWhite.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(color = TextWhite.copy(alpha = 0.9f)))
    }
}

fun formatSubscriptionDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "N/A"
    return try {
        // Assuming backend format "2024-05-21T10:00:00Z" or similar OffsetDateTime compatible
        val odt = OffsetDateTime.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
        odt.format(formatter)
    } catch (e: Exception) {
        dateString // fallback
    }
}

@Preview(showBackground = true)
@Composable
fun AdminSubscriptionListScreenPreview() {
    BirdlensTheme {
        val navController = rememberNavController()
        // Mock ViewModel or use a simple instance for preview
        AdminSubscriptionListScreen(navController = navController)
    }
}

@Preview
@Composable
fun SubscriptionItemCardPreview() {
    BirdlensTheme {
        SubscriptionItemCard(
            subscription = Subscription(
                id = 1, name = "Premium Plan", description = "Access all features and unlimited bird identification.",
                price = 9.99, durationDays = 30,
                createdAt = "2024-01-01T10:00:00Z", updatedAt = "2024-01-15T12:30:00Z"
            )
        )
    }
}