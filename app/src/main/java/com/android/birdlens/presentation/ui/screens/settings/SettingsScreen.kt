// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/settings/SettingsScreen.kt
package com.android.birdlens.presentation.ui.screens.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold // Import
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar // Import
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun SettingsScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val settingsItemsSection1 = listOf(
        SettingsItem("Account", Icons.Outlined.AccountCircle) { /* TODO */ },
        SettingsItem("Notifications", Icons.Outlined.Notifications) { /* TODO */ },
        SettingsItem("Privacy & Security", Icons.Outlined.Lock) { /* TODO */ },
        SettingsItem("Help & Support", Icons.AutoMirrored.Filled.HelpOutline) { /* TODO */ },
        SettingsItem("About", Icons.Outlined.Info) { /* TODO */ }
    )
    val settingsItemsSection2 = listOf(
        SettingsItem("Saved", Icons.Outlined.BookmarkBorder) { /* TODO */ },
        SettingsItem("Liked", Icons.Outlined.FavoriteBorder) { /* TODO */ }
    )

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = "Settings",
                onNavigateBack = { navController.popBackStack() } // Assuming back navigation is desired
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        Surface( // This Surface forms the card-like appearance for the settings list
            color = CardBackground.copy(alpha = 0.7f), // Use the card background
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // Rounded top corners
            modifier = Modifier
                .padding(innerPadding) // Apply padding from AppScaffold
                .padding(top = 8.dp) // Additional top padding if needed after TopAppBar
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp) // Horizontal padding for content inside the Surface
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f) // LazyColumn takes available space
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = SearchBarBackground,
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                                cursorBrush = SolidColor(TextWhite),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = SearchBarPlaceholderText)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (searchQuery.isEmpty()) {
                                                Text("Search something", color = SearchBarPlaceholderText, fontSize = 16.sp)
                                            }
                                            innerTextField()
                                        }
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    items(settingsItemsSection1.size) { index ->
                        val item = settingsItemsSection1[index]
                        SettingsListItem(item = item)
                        if (index < settingsItemsSection1.size - 1) {
                            HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                    items(settingsItemsSection2.size) { index ->
                        val item = settingsItemsSection2[index]
                        SettingsListItem(item = item)
                        if (index < settingsItemsSection2.size - 1) {
                            HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                } // End of LazyColumn

                Button(
                    onClick = {
                        googleAuthViewModel.signOut(context)
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = LogoutButtonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp) // Padding for the logout button
                        .height(50.dp)
                ) {
                    Text("LOG OUT", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp)) // Padding at the very bottom
            }
        }
    }
}

@Composable
fun SettingsListItem(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(vertical = 12.dp), // Padding for each item
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = item.icon, contentDescription = item.title, tint = TextWhite, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = item.title, color = TextWhite, fontSize = 16.sp)
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun SettingsScreenPreview() {
    BirdlensTheme {
        val dummyGoogleAuthViewModel = GoogleAuthViewModel()
        SettingsScreen(navController = rememberNavController(), googleAuthViewModel = dummyGoogleAuthViewModel)
    }
}