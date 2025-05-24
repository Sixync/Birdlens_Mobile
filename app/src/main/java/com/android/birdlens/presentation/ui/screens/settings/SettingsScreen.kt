// app/src/main/java/com/android/birdlens/presentation/ui/screens/settings/SettingsScreen.kt
package com.android.birdlens.presentation.ui.screens.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
// import androidx.compose.material.icons.automirrored.filled.Logout // Not used directly here
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.android.birdlens.presentation.ui.screens.tour.BottomNavItem
import com.android.birdlens.presentation.ui.screens.tour.BottomNavigationBar
import com.android.birdlens.presentation.viewmodel.GoogleAuthViewModel
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.flow.collect

data class SettingsItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    googleAuthViewModel: GoogleAuthViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val settingsItemsSection1 = listOf(
        SettingsItem("Account", Icons.Outlined.AccountCircle) { /* TODO: Navigate to Account */ },
        SettingsItem("Notifications", Icons.Outlined.Notifications) { /* TODO: Navigate to Notifications */ },
        SettingsItem("Privacy & Security", Icons.Outlined.Lock) { /* TODO: Navigate to Privacy & Security */ },
        SettingsItem("Help & Support", Icons.AutoMirrored.Filled.HelpOutline) { /* TODO: Navigate to Help & Support */ },
        SettingsItem("About", Icons.Outlined.Info) { /* TODO: Navigate to About */ }
    )

    val settingsItemsSection2 = listOf(
        SettingsItem("Saved", Icons.Outlined.BookmarkBorder) { /* TODO: Navigate to Saved */ },
        SettingsItem("Liked", Icons.Outlined.FavoriteBorder) { /* TODO: Navigate to Liked */ }
    )

    val bottomNavItems = listOf(
        BottomNavItem("Settings", { Icon(Icons.Outlined.Tune, "Settings") }, { Icon(Icons.Filled.Tune, "Settings") }, Screen.Settings.route),
        BottomNavItem("Community", { Icon(Icons.Outlined.Groups, "Community") }, { Icon(Icons.Filled.Groups, "Community") }, Screen.Community.route),
        BottomNavItem("Map", { Icon(Icons.Outlined.Map, "Map") }, { Icon(Icons.Filled.Map, "Map") }, Screen.Map.route),
        BottomNavItem("Marketplace", { Icon(Icons.Outlined.ShoppingCart, "Marketplace") }, { Icon(Icons.Filled.ShoppingCart, "Marketplace") }, Screen.Marketplace.route),
        BottomNavItem("Calendar", { Icon(Icons.Outlined.CalendarToday, "Calendar") }, { Icon(Icons.Filled.CalendarToday, "Calendar") }, Screen.Tour.route)
    )

    var selectedBottomNavItem by remember {
        mutableStateOf(bottomNavItems.indexOfFirst { it.route == Screen.Settings.route }.coerceAtLeast(0))
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            val newIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
            if (newIndex != -1) {
                selectedBottomNavItem = newIndex
            } else {
                selectedBottomNavItem = bottomNavItems.indexOfFirst { it.route == Screen.Settings.route }.coerceAtLeast(0)
            }
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(color = GreenDeep)
            // ... background wave paths ...
            val path1 = Path().apply { moveTo(0f, canvasHeight * 0.1f); cubicTo(canvasWidth * 0.2f, canvasHeight * 0.05f, canvasWidth * 0.3f, canvasHeight * 0.4f, canvasWidth * 0.6f, canvasHeight * 0.35f); cubicTo(canvasWidth * 0.9f, canvasHeight * 0.3f, canvasWidth * 1.1f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.7f); lineTo(0f, canvasHeight * 0.8f); close() }
            drawPath(path = path1, brush = Brush.radialGradient(listOf(GreenWave1.copy(alpha = 0.8f), GreenWave3.copy(alpha = 0.6f), GreenDeep.copy(alpha = 0.3f)), center = Offset(canvasWidth * 0.2f, canvasHeight * 0.2f), radius = canvasWidth * 0.8f))
            val path2 = Path().apply { moveTo(canvasWidth, canvasHeight * 0.5f); cubicTo(canvasWidth * 0.8f, canvasHeight * 0.6f, canvasWidth * 0.7f, canvasHeight * 0.3f, canvasWidth * 0.4f, canvasHeight * 0.4f); cubicTo(canvasWidth * 0.1f, canvasHeight * 0.5f, canvasWidth * 0.0f, canvasHeight * 0.9f, canvasWidth * 0.3f, canvasHeight); lineTo(canvasWidth, canvasHeight); close() }
            drawPath(path = path2, brush = Brush.linearGradient(listOf(GreenWave4.copy(alpha = 0.4f), GreenWave1.copy(alpha = 0.3f), GreenDeep.copy(alpha = 0.1f)), start = Offset(canvasWidth * 0.8f, canvasHeight * 0.5f), end = Offset(canvasWidth * 0.3f, canvasHeight)))
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Settings", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    items = bottomNavItems,
                    selectedItemIndex = selectedBottomNavItem,
                    onItemSelected = { index ->
                        val destinationRoute = bottomNavItems[index].route
                        if (navController.currentDestination?.route != destinationRoute) {
                            navController.navigate(destinationRoute) {
                                popUpTo(Screen.Tour.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Surface(
                color = CardBackground.copy(alpha = 0.7f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(top = 8.dp)
                    .fillMaxSize()
            ) {
                Column( // Parent Column for LazyColumn and Logout Button
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp) // Horizontal padding for the content within the card
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f) // LazyColumn takes available space
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Search Bar
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = SearchBarBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
                                    cursorBrush = SolidColor(TextWhite),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
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

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        items(settingsItemsSection2.size) { index ->
                            val item = settingsItemsSection2[index]
                            SettingsListItem(item = item)
                            if (index < settingsItemsSection2.size - 1) {
                                HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                        // Spacer removed from here, as LazyColumn now handles the weight
                    }

                    // Logout Button is now a direct child of the parent Column
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
                            .padding(vertical = 24.dp) // Padding around the button
                            .height(50.dp)
                    ) {
                        Text("LOG OUT", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Some padding at the very bottom of the card
                }
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = TextWhite,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.title,
            color = TextWhite,
            fontSize = 16.sp
        )
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