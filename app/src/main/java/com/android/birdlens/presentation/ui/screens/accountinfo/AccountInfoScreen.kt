// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/accountinfo/AccountInfoScreen.kt
package com.android.birdlens.presentation.ui.screens.accountinfo

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.payment.CheckoutActivity
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    navController: NavController,
    accountInfoViewModel: AccountInfoViewModel = viewModel()
) {
    val uiState by accountInfoViewModel.uiState.collectAsState()

    // Logic: Added a LaunchedEffect to trigger the data fetch when the screen is first composed.
    // This ensures user data is loaded whenever this screen becomes visible, resolving the infinite loading bug.
    // The ViewModel is now robust against re-fetching if data is already present.
    LaunchedEffect(key1 = Unit) {
        accountInfoViewModel.fetchCurrentUser()
    }

    AppScaffold(
        navController = navController,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.account_info_title), color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        showBottomBar = true
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
                    AccountDetails(user = state.user)
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
    val context = LocalContext.current

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

        if (user.subscription != "ExBird") {
            Button(
                onClick = {
                    val intent = Intent(context, CheckoutActivity::class.java)
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
            Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Subscription management not yet implemented.", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = GreenWave3)
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