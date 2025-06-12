// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/accountinfo/AccountInfoScreen.kt
package com.android.birdlens.presentation.ui.screens.accountinfo

import android.annotation.SuppressLint
import android.content.Intent // Import Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.birdlens.R
import com.android.birdlens.data.model.response.UserResponse
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
// Import CheckoutActivity to launch it
import com.android.birdlens.presentation.ui.screens.payment.CheckoutActivity
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun AccountInfoScreen(
    navController: NavController,
    accountInfoViewModel: AccountInfoViewModel = viewModel()
) {
    val uiState by accountInfoViewModel.uiState.collectAsState()

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = stringResource(id = R.string.account_info_title),
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is AccountInfoUiState.Idle, is AccountInfoUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = TextWhite)
                }
                is AccountInfoUiState.Success -> {
                    AccountDetails(user = state.user) // Pass navController if needed for other nav
                }
                is AccountInfoUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(id = R.string.account_info_error_loading, state.message),
                            color = TextWhite.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { accountInfoViewModel.fetchCurrentUser() },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) {
                            Text(stringResource(R.string.retry), color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDetails(user: UserResponse) {
    val context = LocalContext.current // Get context to launch activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        val avatarPainter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.avatarUrl ?: "https://via.placeholder.com/150/CCCCCC/FFFFFF?Text=User")
                .crossfade(true)
                .build()
        )
        Image(
            painter = avatarPainter,
            contentDescription = stringResource(id = R.string.user_avatar_description),
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        UserInfoCard {
            UserInfoRow(label = stringResource(id = R.string.account_info_username), value = user.username)
            UserInfoRow(
                label = stringResource(id = R.string.account_info_email),
                value = user.email,
                icon = if (user.emailVerified) Icons.Filled.VerifiedUser else null,
                iconColor = if (user.emailVerified) GreenWave2 else TextWhite.copy(alpha = 0.5f)
            )
            UserInfoRow(label = stringResource(id = R.string.account_info_first_name), value = user.firstName)
            UserInfoRow(label = stringResource(id = R.string.account_info_last_name), value = user.lastName)
            UserInfoRow(label = stringResource(id = R.string.account_info_age), value = user.age.toString())

            val subscriptionText = if (!user.subscription.isNullOrBlank()) {
                user.subscription
            } else {
                stringResource(id = R.string.subscription_tier_standard)
            }
            UserInfoRow(
                label = stringResource(id = R.string.subscription_tier_label),
                value = subscriptionText,
                icon = if (!user.subscription.isNullOrBlank()) Icons.Filled.WorkspacePremium else null,
                iconColor = if (!user.subscription.isNullOrBlank()) GreenWave2 else TextWhite.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Button to Purchase/Manage Subscription
        if (user.subscription != "ExBird") { // Assuming "ExBird" is your premium subscription name
            Button(
                onClick = {
                    val intent = Intent(context, CheckoutActivity::class.java)
                    // You can pass data to CheckoutActivity if needed, e.g., which subscription to buy.
                    // For now, CheckoutActivity uses a hardcoded item.
                    // intent.putExtra("SUBSCRIPTION_ID", "exbird_premium_monthly")
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text(stringResource(id = R.string.purchase_exbird_subscription), color = TextWhite)
            }
        } else {
            // Optionally, show a "Manage Subscription" button or info if already subscribed
            Button(
                onClick = {
                    // TODO: Navigate to a subscription management portal or show info
                    // For now, could also launch CheckoutActivity if it can handle "managing"
                    android.widget.Toast.makeText(context, "Subscription management not yet implemented.", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = GreenWave3) // Different color for manage
            ) {
                Text(stringResource(id = R.string.manage_subscription), color = VeryDarkGreenBase)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UserInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun UserInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconColor: Color = TextWhite
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextWhite.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 18.sp,
            color = TextWhite,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = if (icon != null && label == stringResource(id = R.string.account_info_email)) 26.dp else 0.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

@SuppressLint("ViewModelConstructorInComposable", "StateFlowValueCalledInComposition")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AccountInfoScreenPreview_Success_WithSubscription() {
    BirdlensTheme {
        val dummyUser = UserResponse(
            id = 1, username = "birdwatcher_pro", firstName = "Alex", lastName = "Smith",
            email = "alex.smith@example.com", age = 32, avatarUrl = null,
            subscription = "ExBird",
            emailVerified = true
        )
        val context = LocalContext.current
        val mockViewModel = AccountInfoViewModel(context.applicationContext as android.app.Application)
        mockViewModel._uiState.value = AccountInfoUiState.Success(dummyUser)

        AppScaffold(navController = rememberNavController(), topBar = { SimpleTopAppBar(stringResource(id = R.string.account_info_title))}) {
            Box(Modifier.padding(it)) { AccountDetails(user = dummyUser) }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable", "StateFlowValueCalledInComposition")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AccountInfoScreenPreview_Success_NoSubscription_NotVerified() {
    BirdlensTheme {
        val dummyUser = UserResponse(
            id = 1, username = "free_user", firstName = "Jane", lastName = "Doe",
            email = "jane.doe@example.com", age = 28, avatarUrl = null,
            subscription = null,
            emailVerified = false
        )
        val context = LocalContext.current
        val mockViewModel = AccountInfoViewModel(context.applicationContext as android.app.Application)
        mockViewModel._uiState.value = AccountInfoUiState.Success(dummyUser)

        AppScaffold(navController = rememberNavController(), topBar = { SimpleTopAppBar(stringResource(id = R.string.account_info_title))}) {
            Box(Modifier.padding(it)) { AccountDetails(user = dummyUser) }
        }
    }
}