// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/notification/NotificationScreen.kt
package com.android.birdlens.presentation.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.android.birdlens.data.model.Notification
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.GenericUiState
import com.android.birdlens.presentation.viewmodel.NotificationViewModel
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.CardBackground
import com.android.birdlens.ui.theme.DividerColor
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.TextWhite
import com.android.birdlens.ui.theme.VeryDarkGreenBase
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val uiState by notificationViewModel.notificationsState.collectAsState()

    AppScaffold(
        navController = navController,
        topBar = { SimpleTopAppBar(title = "Notifications", onNavigateBack = { navController.popBackStack() }) },
        showBottomBar = true // Assuming it's accessed from a main screen
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(VeryDarkGreenBase)
        ) {
            when (val state = uiState) {
                is GenericUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is GenericUiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = "You have no new notifications.",
                            color = TextWhite.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.data, key = { it.id }) { notification ->
                                NotificationItem(notification)
                            }
                        }
                    }
                }
                is GenericUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error loading notifications: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { notificationViewModel.fetchNotifications() }, colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)) {
                            Text("Retry")
                        }
                    }
                }
                is GenericUiState.Idle -> {}
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    val icon = when (notification.type) {
        "referral_success" -> Icons.Default.CardGiftcard
        "subscription_activated" -> Icons.Default.WorkspacePremium
        else -> Icons.Default.CheckCircle
    }
    val iconColor = when (notification.type) {
        "referral_success" -> GreenWave2
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(CardBackground.copy(alpha = 0.3f))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = notification.type,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.message,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatNotificationTimestamp(notification.createdAt),
                color = TextWhite.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(GreenWave2)
            )
        }
    }
}

private fun formatNotificationTimestamp(isoTimestamp: String): String {
    return try {
        val odt = OffsetDateTime.parse(isoTimestamp)
        val localDateTime = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        localDateTime.format(formatter)
    } catch (e: Exception) {
        "Just now" // Fallback
    }
}