// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/accountinfo/AccountInfoScreen.kt
package com.android.birdlens.presentation.ui.screens.accountinfo

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.android.birdlens.data.model.response.UserResponse
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
import com.android.birdlens.ui.theme.*

@Composable
fun AccountInfoScreen(
    navController: NavController,
    accountInfoViewModel: AccountInfoViewModel = viewModel() // Default ViewModel instance
) {
    val uiState by accountInfoViewModel.uiState.collectAsState()

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "Account Information",
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = false // Typically, detail screens like this don't need the main bottom bar
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is AccountInfoUiState.Loading -> {
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
                        Text("Error: ${state.message}", color = TextWhite.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { accountInfoViewModel.fetchCurrentUser() },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                        ) {
                            Text("Retry", color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountDetails(user: UserResponse) {
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
                .data(user.avatarUrl ?: "https://via.placeholder.com/150/CCCCCC/FFFFFF?Text=User") // Placeholder if null
                .crossfade(true)
                .build()
            // You can add a placeholder/error drawable here if needed via .placeholder() .error()
        )
        Image(
            painter = avatarPainter,
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Gray), // Background for the circle if image loading fails or is transparent
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        UserInfoCard {
            UserInfoRow(label = "Username", value = user.username)
            UserInfoRow(label = "Email", value = user.email)
            UserInfoRow(label = "First Name", value = user.firstName)
            UserInfoRow(label = "Last Name", value = user.lastName)
            UserInfoRow(label = "Age", value = user.age.toString())
            // Add more fields as needed
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Add edit button or other actions if needed
        // Button(onClick = { /* TODO: Navigate to edit profile */ }) { Text("Edit Profile") }
    }
}

@Composable
fun UserInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackground.copy(alpha = 0.5f), // Slightly transparent card
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
fun UserInfoRow(label: String, value: String) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextWhite.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 18.sp,
            color = TextWhite,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AccountInfoScreenPreview_Success() {
    BirdlensTheme {
        val dummyUser = UserResponse(
            id = 1,
            username = "birdwatcher_pro",
            firstName = "Alex",
            lastName = "Smith",
            email = "alex.smith@example.com",
            age = 32,
            avatarUrl = null // "https://example.com/avatar.jpg"
        )
        // Simulate ViewModel and state for preview
        val mockViewModel = AccountInfoViewModel(ApplicationProvider.getApplicationContext())
        mockViewModel._uiState.value = AccountInfoUiState.Success(dummyUser)

        AppScaffold(navController = rememberNavController(), topBar = { SimpleTopAppBar("Account Info")}) {
            AccountDetails(user = dummyUser)
        }
    }
}

// Dummy Application class for preview context (if needed by ViewModel for preview)
// In a real app, you might have a proper Application class or use Hilt for previews.
class ApplicationProvider {
    companion object {
        @Composable
        fun getApplicationContext(): android.app.Application {
            return LocalContext.current.applicationContext as android.app.Application
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable", "StateFlowValueCalledInComposition")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AccountInfoScreenPreview_Loading() {
    BirdlensTheme {
        val mockViewModel = AccountInfoViewModel(ApplicationProvider.getApplicationContext())
        mockViewModel._uiState.value = AccountInfoUiState.Loading
        AccountInfoScreen(navController = rememberNavController(), accountInfoViewModel = mockViewModel)
    }
}

@SuppressLint("ViewModelConstructorInComposable", "StateFlowValueCalledInComposition")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun AccountInfoScreenPreview_Error() {
    BirdlensTheme {
        val mockViewModel = AccountInfoViewModel(ApplicationProvider.getApplicationContext())
        mockViewModel._uiState.value = AccountInfoUiState.Error("Failed to load user information. Please try again.")
        AccountInfoScreen(navController = rememberNavController(), accountInfoViewModel = mockViewModel)
    }
}