// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/settings/SettingsScreen.kt
package com.android.birdlens.presentation.ui.screens.settings

import android.annotation.SuppressLint
import android.app.Application // For ViewModel preview
import android.content.Intent
import android.net.Uri
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
import com.android.birdlens.presentation.viewmodel.AccountInfoUiState
import com.android.birdlens.presentation.viewmodel.AccountInfoViewModel
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
    accountInfoViewModel: AccountInfoViewModel, // Accept the ViewModel
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    val accountState by accountInfoViewModel.uiState.collectAsState()

    val currentLanguageName by remember {
        derivedStateOf {
            val currentLangCode = LanguageManager.getLanguagePreference(context)
            when (currentLangCode) {
                LanguageManager.LANGUAGE_VIETNAMESE -> context.getString(R.string.language_vietnamese)
                LanguageManager.LANGUAGE_ENGLISH -> context.getString(R.string.language_english)
                else -> context.getString(R.string.language_english)
            }
        }
    }

    val currentSubscriptionTier by remember(accountState) {
        derivedStateOf {
            when (val state = accountState) {
                is AccountInfoUiState.Success -> state.user.subscription ?: context.getString(R.string.subscription_tier_standard)
                is AccountInfoUiState.Error -> context.getString(R.string.subscription_tier_unknown) // Or "Error"
                else -> context.getString(R.string.loading_ellipsis) // "Loading..."
            }
        }
    }


    val openWebsiteIntent = remember {
        Intent(Intent.ACTION_VIEW, Uri.parse("https://birdlens.netlify.app/"))
    }


    val settingsItemsSection1 = listOf(
        SettingsItem(R.string.settings_account, Icons.Outlined.AccountCircle, {
            navController.navigate(Screen.Me.route)
        }),
        SettingsItem(
            titleResId = R.string.settings_my_subscription, // New String Resource
            icon = Icons.Outlined.WorkspacePremium,
            onClick = { navController.navigate(Screen.Premium.route) }, // Navigate to Premium screen
            subText = currentSubscriptionTier
        ),
        SettingsItem(
            titleResId = R.string.settings_language,
            icon = Icons.Outlined.Language,
            onClick = { showLanguageDialog = true },
            subText = currentLanguageName
        ),
        SettingsItem(R.string.settings_notifications, Icons.Outlined.Notifications, { /* TODO */ }),
        SettingsItem(R.string.settings_privacy_security, Icons.Outlined.Lock, { /* TODO */ }),
        SettingsItem(R.string.settings_help_support, Icons.AutoMirrored.Filled.HelpOutline, { context.startActivity(openWebsiteIntent) }, isExternalLink = true),
        SettingsItem(R.string.settings_about, Icons.Outlined.Info, { /* TODO */ }, isExternalLink = true)
    )
    val settingsItemsSection2 = listOf(
        SettingsItem(R.string.settings_saved, Icons.Outlined.BookmarkBorder, { /* TODO */ }),
        SettingsItem(R.string.settings_liked, Icons.Outlined.FavoriteBorder, { /* TODO */ })
    )

    // Logic: The adminSettingsItems list has been removed.
    // This removes the "Manage Subscriptions" option from the UI for regular users.

    AppScaffold(
        navController = navController,
        topBar = {
            SimpleTopAppBar(
                title = stringResource(id = R.string.settings_title),
                onNavigateBack = { navController.popBackStack() }
            )
        },
        showBottomBar = true
    ) { innerPadding ->
        Surface(
            color = CardBackground.copy(alpha = 0.7f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .padding(innerPadding)
                .padding(top = 8.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f)
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
                                        Box(modifier = Modifier.weight(1f)) {
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

                    item { Spacer(modifier = Modifier.height(24.dp)) }

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
                    // Logic: The "Admin Area" section and its items have been completely removed.
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                Button(
                    onClick = {
                        accountInfoViewModel.onUserLoggedOut()
                        googleAuthViewModel.signOut(context)
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = LogoutButtonGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .height(50.dp)
                ) {
                    Text(
                        stringResource(id = R.string.settings_logout),
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = LanguageManager.getLanguagePreference(context),
            onLanguageSelected = { newLangCode ->
                val currentLangCode = LanguageManager.getLanguagePreference(context)
                if (newLangCode != currentLangCode) {
                    LanguageManager.changeLanguage(context, newLangCode)
                    val activity = context as? MainActivity
                    activity?.recreateActivity()
                }
                showLanguageDialog = false
            },
            onDismissRequest = { showLanguageDialog = false }
        )
    }
}
@Composable
fun SettingsListItem(item: SettingsItem) {
    val title = stringResource(id = item.titleResId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = title,
            tint = TextWhite,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = TextWhite,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        item.subText?.let {
            Text(
                text = it,
                color = TextWhite.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (item.isExternalLink || item.subText != null) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
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
    val languages = listOf(
        LanguageManager.LANGUAGE_ENGLISH to stringResource(R.string.language_english),
        LanguageManager.LANGUAGE_VIETNAMESE to stringResource(R.string.language_vietnamese)
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AuthCardBackground
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
                                selectedColor = GreenWave2,
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
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val dummyGoogleAuthViewModel: GoogleAuthViewModel = viewModel { GoogleAuthViewModel(application) }
        val dummyAccountInfoViewModel: AccountInfoViewModel = viewModel { AccountInfoViewModel(application) }

        SettingsScreen(
            navController = rememberNavController(),
            googleAuthViewModel = dummyGoogleAuthViewModel,
            accountInfoViewModel = dummyAccountInfoViewModel
        )
    }
}