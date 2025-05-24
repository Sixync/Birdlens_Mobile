// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/tour/TourScreen.kt
package com.android.birdlens.presentation.ui.screens.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
// import androidx.navigation.compose.currentBackStackEntryAsState // Replaced by currentBackStackEntryFlow
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.ui.theme.*
import kotlinx.coroutines.flow.collect // Ensure this import for flow collection

// --- Data Models (Dummy) ---
data class TourItem(val id: Int, val imageUrl: String, val title: String = "")
data class BottomNavItem(val label: String, val icon: @Composable () -> Unit, val selectedIcon: @Composable () -> Unit, val route: String)


// --- Main Screen Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    onNavigateToAllEvents: () -> Unit,
    onNavigateToAllTours: () -> Unit,
    onNavigateToPopularTours: () -> Unit,
    onTourItemClick: (Int) -> Unit,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    var searchQuery by remember { mutableStateOf("") }


    // Dummy data
    val eventsOfTheYear = listOf(
        TourItem(1, "https://plus.unsplash.com/premium_photo-1673283380436-ac702dc85c64?w=300&auto=format&fit=crop&q=60", "Hoi An Lanterns"),
        TourItem(2, "https://images.unsplash.com/photo-1528181304800-259b08848526?w=300&auto=format&fit=crop&q=60", "Ha Long Bay"),
        TourItem(3, "https://images.unsplash.com/photo-1559507984-555c777a7554?w=300&auto=format&fit=crop&q=60", "Sapa Rice Terraces")
    )
    val popularTour = TourItem(4, "https://images.unsplash.com/photo-1567020009789-972f00d0f1f0?w=800&auto=format&fit=crop&q=60", "Night Market")
    val allToursData = listOf(
        TourItem(5, "https://images.unsplash.com/photo-1567020009789-972f00d0f1f0?w=300&auto=format&fit=crop&q=60", "Ancient Town"),
        TourItem(6, "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=300&auto=format&fit=crop&q=60", "Beach Sunset"),
        TourItem(7, "https://images.unsplash.com/photo-1483728642387-6c351bEC1d69?w=300&auto=format&fit=crop&q=60", "Mountain Peak")
    )

    val bottomNavItems = listOf(
        BottomNavItem("Filter", { Icon(Icons.Outlined.Tune, "Filter") }, { Icon(Icons.Filled.Tune, "Filter") }, "filter_route"), // Placeholder route
        BottomNavItem("People", { Icon(Icons.Outlined.Groups, "People") }, { Icon(Icons.Filled.Groups, "People") }, "people_route"), // Placeholder route
        BottomNavItem("Map", { Icon(Icons.Outlined.Map, "Map") }, { Icon(Icons.Filled.Map, "Map") }, Screen.Map.route),
        BottomNavItem("Cart", { Icon(Icons.Outlined.ShoppingCart, "Marketplace") }, { Icon(Icons.Filled.ShoppingCart, "Marketplace") }, Screen.Marketplace.route), // Updated to Marketplace
        BottomNavItem("Calendar", { Icon(Icons.Outlined.CalendarToday, "Calendar") }, { Icon(Icons.Filled.CalendarToday, "Calendar") }, Screen.Tour.route) // Main/home tab
    )

    var selectedBottomNavItem by remember {
        mutableStateOf(bottomNavItems.indexOfFirst { it.route == Screen.Tour.route }.coerceAtLeast(0))
    }

    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val currentRoute = backStackEntry.destination.route
            val newIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
            if (newIndex != -1) {
                selectedBottomNavItem = newIndex
            } else {
                // If on a screen not in bottomNav (e.g., TourDetail, or Cart if Cart had its own different bottom bar),
                // keep the "Calendar" (Tour) tab selected as the conceptual parent.
                selectedBottomNavItem = bottomNavItems.indexOfFirst { it.route == Screen.Tour.route }.coerceAtLeast(0)
            }
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        // Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            drawRect(color = GreenDeep)
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
            bottomBar = {
                BottomNavigationBar(
                    items = bottomNavItems,
                    selectedItemIndex = selectedBottomNavItem,
                    onItemSelected = { index ->
                        val destinationRoute = bottomNavItems[index].route
                        if (navController.currentDestination?.route != destinationRoute) {
                            if (destinationRoute == Screen.Cart.route) {
                                // Simple navigation to Cart, keeps TourScreen on the back stack
                                navController.navigate(Screen.Cart.route)
                            } else {
                                // Standard bottom nav behavior for other tabs
                                navController.navigate(destinationRoute) {
                                    popUpTo(Screen.Tour.route) { // Pop back to TourScreen (our main tab)
                                        saveState = true
                                    }
                                    launchSingleTop = true // Avoid multiple copies of the same destination
                                    restoreState = true  // Restore state when reselecting
                                }
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    TourScreenHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onNavigateToCart = { navController.navigate(Screen.Cart.route) } // Header cart icon
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    SectionWithHorizontalList(
                        title = "Events of the year",
                        items = eventsOfTheYear,
                        onItemClick = { tourItem -> onTourItemClick(tourItem.id) },
                        onSeeAllClick = onNavigateToAllEvents,
                        isPreviewMode = isPreviewMode
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    SectionHeader(title = "Popular Tour", onSeeAllClick = onNavigateToPopularTours)
                    PopularTourCard(item = popularTour, onClick = { onTourItemClick(popularTour.id) }, isPreviewMode = isPreviewMode)
                    PageIndicator(count = 3, selectedIndex = 0) // This indicator is static, might need logic if pager is used
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    SectionWithHorizontalList(
                        title = "All Tours",
                        items = allToursData,
                        onItemClick = { tourItem -> onTourItemClick(tourItem.id) },
                        onSeeAllClick = onNavigateToAllTours,
                        isPreviewMode = isPreviewMode
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}


// --- Reusable Sub-Components ---
@Composable
fun TourScreenHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToCart: () -> Unit
) {
    Column {
        Surface(
            shape = RoundedCornerShape(50),
            color = SearchBarBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
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
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "BIRDLENS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenWave2,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    "Tours",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            IconButton(onClick = onNavigateToCart) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = TextWhite, modifier = Modifier.size(28.dp))
            }
        }
    }
}
@Composable
fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = TextWhite)
        )
        Text(
            text = "See all ->",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite.copy(alpha = 0.8f)),
            modifier = Modifier.clickable(onClick = onSeeAllClick)
        )
    }
}

@Composable
fun SectionWithHorizontalList(
    title: String,
    items: List<TourItem>,
    onItemClick: (TourItem) -> Unit,
    onSeeAllClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Column {
        SectionHeader(title = title, onSeeAllClick = onSeeAllClick)
        LazyRow(
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.id }) { item ->
                TourItemCardSmall(item = item, onClick = { onItemClick(item) }, isPreviewMode = isPreviewMode)
            }
        }
    }
}

@Composable
fun TourItemCardSmall(
    item: TourItem,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(width = 150.dp, height = 200.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isPreviewMode) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Preview Img", color = TextWhite.copy(alpha = 0.7f))
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageUrl),
                    contentDescription = item.title.ifEmpty { "Tour image" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // You can add a title overlay here if needed
            if (item.title.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 300f
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(item.title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun PopularTourCard(
    item: TourItem,
    onClick: () -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isPreviewMode) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Preview Popular Img", color = TextWhite.copy(alpha = 0.7f))
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageUrl),
                    contentDescription = item.title.ifEmpty { "Popular tour image" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (item.title.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.7f)),
                                startY = 300f
                            )
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(item.title, color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PageIndicator(count: Int, selectedIndex: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (index == selectedIndex) PageIndicatorActive else PageIndicatorInactive)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    selectedItemIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = BottomNavGreen,
        contentColor = TextWhite,
        modifier = Modifier.height(70.dp) // Standard height
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = { onItemSelected(index) },
                icon = {
                    if (selectedItemIndex == index) item.selectedIcon() else item.icon()
                },
                label = { Text(item.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextWhite,
                    unselectedIconColor = TextWhite.copy(alpha = 0.7f),
                    selectedTextColor = TextWhite,
                    unselectedTextColor = TextWhite.copy(alpha = 0.7f),
                    indicatorColor = BottomNavGreen // Or a slightly different shade for selection
                )
            )
        }
    }
}


// --- Preview ---
@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun TourScreenPreview() {
    BirdlensTheme {
        TourScreen(
            navController = rememberNavController(),
            onNavigateToAllEvents = {},
            onNavigateToAllTours = {},
            onNavigateToPopularTours = {},
            onTourItemClick = {},
            isPreviewMode = true
        )
    }
}