// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/settings/SettingsScreen.kt
package com.android.birdlens.presentation.ui.screens.settings

import android.annotation.SuppressLint
import android.app.Application // For ViewModel preview
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.android.birdlens.MainActivity
import com.android.birdlens.R
import com.android.birdlens.data.LanguageManager
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.components.SimpleTopAppBar
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*

data class SettingsItem(
    val titleResId: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isExternalLink: Boolean = false,
    val subText: String? = null
)

@Composable
fun SettingsScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    // This state will hold the currently selected language NAME for display.
    // It uses LocalContext.current, so when the Activity is recreated and
    // LocalContext.current provides the new locale-aware context, this will re-evaluate
    // and fetch the correct string resource.
    val currentLanguageName by remember {
        derivedStateOf {
            val currentLangCode = LanguageManager.getLanguagePreference(context)
            when (currentLangCode) {
                LanguageManager.LANGUAGE_VIETNAMESE -> context.getString(R.string.language_vietnamese)
                LanguageManager.LANGUAGE_ENGLISH -> context.getString(R.string.language_english)
                else -> context.getString(R.string.language_english) // Fallback
            }
        }
    }

    val settingsItemsSection1 = listOf(
        SettingsItem(R.string.settings_account, Icons.Outlined.AccountCircle, {
            navController.navigate(Screen.AccountInfo.route)
        }),
        SettingsItem(
            titleResId = R.string.settings_language,
            icon = Icons.Outlined.Language,
            onClick = { showLanguageDialog = true },
            subText = currentLanguageName // Pass the derived current language name
        ),
        SettingsItem(R.string.settings_notifications, Icons.Outlined.Notifications, { /* TODO: Navigate to Notifications settings */ }),
        SettingsItem(R.string.settings_privacy_security, Icons.Outlined.Lock, { /* TODO: Navigate to Privacy settings */ }),
        SettingsItem(R.string.settings_help_support, Icons.AutoMirrored.Filled.HelpOutline, { /* TODO: Navigate to Help/Support */ }, isExternalLink = true),
        SettingsItem(R.string.settings_about, Icons.Outlined.Info, { /* TODO: Navigate to About screen */ }, isExternalLink = true)
    )
    val settingsItemsSection2 = listOf(
        SettingsItem(R.string.settings_saved, Icons.Outlined.BookmarkBorder, { /* TODO: Navigate to Saved items */ }),
        SettingsItem(R.string.settings_liked, Icons.Outlined.FavoriteBorder, { /* TODO: Navigate to Liked items */ })
    )

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = stringResource(id = R.string.settings_title),
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = true // Settings screen is part of the main navigation flow with a bottom bar
    ) { innerPadding ->
        Surface(
            color = CardBackground.copy(alpha = 0.7f), // Using a themed background color
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .padding(innerPadding) // Apply padding from AppScaffold
                .padding(top = 8.dp) // Additional top padding if needed
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp) // Horizontal padding for the content inside the Surface
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f) // Makes the LazyColumn take available space, pushing logout button down
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Search Bar
                        Surface(
                            shape = RoundedCornerShape(50), // Circular ends
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Filled.Search,
                                            contentDescription = stringResource(id = R.string.icon_search_description),
                                            tint = SearchBarPlaceholderText
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f)) { // Ensure placeholder and text field align
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    stringResource(id = R.string.search_something),
                                                    color = SearchBarPlaceholderText,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Settings Items Section 1
                    items(settingsItemsSection1.size) { index ->
                        val item = settingsItemsSection1[index]
                        SettingsListItem(item = item)
                        if (index < settingsItemsSection1.size - 1) {
                            HorizontalDivider(
                                color = DividerColor,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) } // Spacer between sections

                    // Settings Items Section 2
                    items(settingsItemsSection2.size) { index ->
                        val item = settingsItemsSection2[index]
                        SettingsListItem(item = item)
                        if (index < settingsItemsSection2.size - 1) {
                            HorizontalDivider(
                                color = DividerColor,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                } // End of LazyColumn

                // Logout Button
                Button(
                    onClick = {
                        googleAuthViewModel.signOut(context)
                        // Navigate to Welcome and clear back stack
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true // Avoid multiple instances of WelcomeScreen
                        }
                    },
                    shape = RoundedCornerShape(50), // Pill shape
                    colors = ButtonDefaults.buttonColors(containerColor = LogoutButtonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp) // Padding around the button
                        .height(50.dp)
                ) {
                    Text(
                        stringResource(id = R.string.settings_logout),
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp)) // Ensure content doesn't stick to edge if no bottom bar padding
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = LanguageManager.getLanguagePreference(context), // Pass current for radio selection
            onLanguageSelected = { newLangCode ->
                val currentLangCode = LanguageManager.getLanguagePreference(context)
                if (newLangCode != currentLangCode) {
                    LanguageManager.changeLanguage(context, newLangCode) // Save preference
                    // Activity recreation will apply the change
                    val activity = context as? MainActivity
                    if (activity != null) {
                        activity.recreateActivity()
                    } else {
                        // This case should ideally not happen if context is from an Activity
                        Log.e("SettingsScreen", "Cannot recreate: Context is not MainActivity instance.")
                    }
                }
                showLanguageDialog = false // Dismiss dialog
            },
            onDismissRequest = { showLanguageDialog = false }
        )
    }
}

@Composable
fun SettingsListItem(item: SettingsItem) {
    // stringResource will fetch the string based on the current LocalContext's locale
    val title = stringResource(id = item.titleResId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(vertical = 12.dp), // Padding for each item for touch target and spacing
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = title, // Use localized title for accessibility
            tint = TextWhite,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = TextWhite,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f) // Text takes available space
        )
        // Display subtext (like current language) if available
        item.subText?.let {
            Text(
                text = it, // This is already a localized string (currentLanguageName)
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp)) // Space before chevron if subtext exists
        }
        // Show chevron for items that navigate or have subtext indicating selection
        if (item.isExternalLink || item.subText != null) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null, // Decorative
                tint = TextWhite.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    // Define languages with their codes and localized names
    val languages = listOf(
        LanguageManager.LANGUAGE_ENGLISH to stringResource(R.string.language_english),
        LanguageManager.LANGUAGE_VIETNAMESE to stringResource(R.string.language_vietnamese)
        // To add more languages:
        // LanguageManager.LANGUAGE_FRENCH to stringResource(R.string.language_french)
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AuthCardBackground // Use a consistent dialog background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.settings_select_language),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                languages.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(code) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (code == currentLanguageCode),
                            onClick = { onLanguageSelected(code) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = GreenWave2, // Themed color for selection
                                unselectedColor = TextWhite.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(name, color = TextWhite, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun SettingsScreenPreview() {
    BirdlensTheme {
        // For preview, create a dummy Application context if needed by ViewModel
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val dummyGoogleAuthViewModel: GoogleAuthViewModel = viewModel { GoogleAuthViewModel(application) }

        SettingsScreen(
            navController = rememberNavController(),
            googleAuthViewModel = dummyGoogleAuthViewModel
        )
    }
}