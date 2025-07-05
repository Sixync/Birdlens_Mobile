// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/accountinfo/AccountInfoScreen.kt
package com.android.birdlens.presentation.ui.screens.accountinfo

// Logic: The import for the obsolete Stripe CheckoutActivity is removed.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.ui.theme.BirdlensTheme
import com.android.birdlens.ui.theme.ButtonGreen
import com.android.birdlens.ui.theme.CardBackground
import com.android.birdlens.ui.theme.GreenWave2
import com.android.birdlens.ui.theme.GreenWave3
import com.android.birdlens.ui.theme.TextWhite
import com.android.birdlens.ui.theme.VeryDarkGreenBase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
        navController: NavController,
        accountInfoViewModel: AccountInfoViewModel = viewModel()
) {
    val uiState by accountInfoViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = Unit) { accountInfoViewModel.fetchCurrentUser() }

    AppScaffold(
            navController = navController,
            topBar = {
                CenterAlignedTopAppBar(
                        title = {
                            Text(
                                    stringResource(id = R.string.account_info_title),
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (navController.previousBackStackEntry != null) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = TextWhite
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                    onClick = { navController.navigate(Screen.Settings.route) }
                            ) {
                                Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = TextWhite
                                )
                            }
                        },
                        colors =
                                TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = Color.Transparent
                                )
                )
            },
            showBottomBar = true
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (val state = uiState) {
                is AccountInfoUiState.Idle, is AccountInfoUiState.Loading -> {
                    CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = TextWhite
                    )
                }
                is AccountInfoUiState.Success -> {
                    // Logic: The NavController is now passed to the AccountDetails composable
                    // so it can handle navigation events.
                    AccountDetails(user = state.user, navController = navController)
                }
                is AccountInfoUiState.Error -> {
                    Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                                stringResource(
                                        id = R.string.account_info_error_loading,
                                        state.message
                                ),
                                color = TextWhite.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                                onClick = { accountInfoViewModel.fetchCurrentUser() },
                                colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) { Text(stringResource(R.string.retry), color = TextWhite) }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDetails(user: UserResponse, navController: NavController) {
    val context = LocalContext.current

    Column(
            modifier =
                    Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        val avatarPainter =
                rememberAsyncImagePainter(
                        model =
                                ImageRequest.Builder(LocalContext.current)
                                        .data(
                                                user.avatarUrl
                                                        ?: "https://via.placeholder.com/150/CCCCCC/FFFFFF?Text=User"
                                        )
                                        .crossfade(true)
                                        .build()
                )
        Image(
                painter = avatarPainter,
                contentDescription = stringResource(id = R.string.user_avatar_description),
                modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.Gray),
                contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        UserInfoCard {
            ProfileItem(
                    label = stringResource(id = R.string.account_info_username),
                    value = user.username
            )
            CustomDivider()
            ProfileItem(
                    label = stringResource(id = R.string.account_info_email),
                    value = user.email
            )
            CustomDivider()
            ProfileItem(
                    label = stringResource(id = R.string.account_info_first_name),
                    value = user.firstName
            )
            CustomDivider()
            ProfileItem(
                    label = stringResource(id = R.string.account_info_last_name),
                    value = user.lastName
            )
            CustomDivider()
            ProfileItem(
                    label = stringResource(id = R.string.account_info_age),
                    value = user.age.toString()
            )
            CustomDivider()

            val subscriptionText =
                    if (!user.subscription.isNullOrBlank()) {
                        user.subscription
                    } else {
                        stringResource(id = R.string.subscription_tier_standard)
                    }
            ProfileItem(
                    label = stringResource(id = R.string.subscription_tier_label),
                    value = subscriptionText,
                    isHighlighted = !user.subscription.isNullOrBlank()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (user.subscription != "ExBird") {
            Button(
                    onClick = {
                        // Logic: The onClick action now navigates to the PremiumScreen,
                        // which contains the PayOS payment flow, instead of the old Stripe
                        // activity.
                        navController.navigate(Screen.Premium.route)
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
            ) {
                Text(stringResource(id = R.string.purchase_exbird_subscription), color = TextWhite)
            }
        } else {
            Button(
                    onClick = {
                        android.widget.Toast.makeText(
                                        context,
                                        "Subscription management not yet implemented.",
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenWave3)
            ) { Text(stringResource(id = R.string.manage_subscription), color = VeryDarkGreenBase) }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun UserInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    CardBackground.copy(
                                                                            alpha = 0.6f
                                                                    ),
                                                                    CardBackground.copy(
                                                                            alpha = 0.4f
                                                                    )
                                                            )
                                            )
                            )
                            .border(
                                    width = 1.dp,
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    Color.White.copy(alpha = 0.2f),
                                                                    Color.White.copy(alpha = 0.05f)
                                                            )
                                            ),
                                    shape = RoundedCornerShape(16.dp)
                            )
    ) { Column(modifier = Modifier.padding(horizontal = 16.dp)) { content() } }
}

@Composable
fun ProfileItem(label: String, value: String, isHighlighted: Boolean = false) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
                text = label,
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
        )

        val valueStyle =
                if (isHighlighted) {
                    TextStyle(
                            color = GreenWave2,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            shadow =
                                    Shadow(
                                            color = GreenWave2.copy(alpha = 0.7f),
                                            offset = Offset.Zero,
                                            blurRadius = 15f
                                    )
                    )
                } else {
                    TextStyle(color = TextWhite, fontWeight = FontWeight.Normal, fontSize = 16.sp)
                }

        Text(text = value, style = valueStyle)
    }
}

@Composable
fun CustomDivider() {
    HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.1f)
    )
}

// Logic: Renamed the preview function to reflect that it's testing the 'AccountDetails' composable,
// which is a stateless component that doesn't require a ViewModel.
@Preview
@Composable
fun AccountDetailsPreview() {
    val user =
            UserResponse(
                    id = 1,
                    username = "birdwatcher",
                    firstName = "John",
                    lastName = "Doe",
                    email = "john.doe@example.com",
                    age = 30,
                    avatarUrl = null,
                    subscription = "ExBird",
                    emailVerified = true
            )
    // Logic: The preview now correctly wraps the stateless `AccountDetails` component
    // inside the application's theme, allowing it to render successfully.
    BirdlensTheme { AccountDetails(user = user, navController = rememberNavController()) }
}
