// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/cart/CartScreen.kt
package com.android.birdlens.presentation.ui.screens.cart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.screens.tour.BottomNavItem // Reusing
import com.android.birdlens.presentation.ui.screens.tour.BottomNavigationBar // Reusing
import com.android.birdlens.ui.theme.*

data class CartItemData(
    val id: Int,
    val name: String,
    val price: String,
    val imageUrl: String?,
    var quantity: Int,
    var isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    isPreviewMode: Boolean = LocalInspectionMode.current
) {
    var cartItems by remember {
        mutableStateOf(
            listOf(
                CartItemData(1, "Tour #5", "$$$", "https://images.unsplash.com/photo-1567020009789-972f00d0f1f0?w=300&auto=format&fit=crop&q=60", 1, false),
                CartItemData(2, "Another Tour", "$$", null, 2, true),
                CartItemData(3, "Event Ticket", "$", null, 1, false),
                CartItemData(4, "Souvenir Pack", "$$$$", null, 1, true)
            )
        )
    }
    var selectAllChecked by remember { mutableStateOf(cartItems.all { it.isSelected } && cartItems.isNotEmpty()) }

    val bottomNavItems = listOf( // Same as TourScreen
        BottomNavItem("Filter", { Icon(Icons.Outlined.Tune, "Filter") }, { Icon(Icons.Filled.Tune, "Filter") },  Screen.Settings.route),
        BottomNavItem("People", { Icon(Icons.Outlined.Groups, "People") }, { Icon(Icons.Filled.Groups, "People") }, Screen.Community.route),
        BottomNavItem("Map", { Icon(Icons.Outlined.Map, "Map") }, { Icon(Icons.Filled.Map, "Map") }, "map_route"),
        BottomNavItem("Cart", { Icon(Icons.Outlined.ShoppingCart, "Marketplace") }, { Icon(Icons.Filled.ShoppingCart, "Marketplace") }, Screen.Marketplace.route), // Updated to Marketplace
        BottomNavItem("Calendar", { Icon(Icons.Outlined.CalendarToday, "Calendar") }, { Icon(Icons.Filled.CalendarToday, "Calendar") }, Screen.Tour.route) // Navigates to Tour
    )
    // In CartScreen, "Cart" tab should always be selected.
    val selectedBottomNavItem by remember { mutableStateOf(bottomNavItems.indexOfFirst { it.route == Screen.Cart.route }) }


    Box(modifier = modifier.fillMaxSize()) {
        // Background Canvas (reused from other screens)
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
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("YOUR CART", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) { // This is the key back action
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
                    selectedItemIndex = selectedBottomNavItem, // Cart is always selected here
                    onItemSelected = { index ->
                        val newDestinationRoute = bottomNavItems[index].route
                        // Only navigate if it's a different route than current (CartScreen)
                        if (navController.currentDestination?.route != newDestinationRoute) {
                            navController.navigate(newDestinationRoute) {
                                if (newDestinationRoute == Screen.Tour.route) {
                                    // Navigating from Cart back to Tour: Pop Cart.
                                    // Tour becomes the top, and launchSingleTop ensures no new instance.
                                    popUpTo(Screen.Tour.route) { inclusive = false }
                                } else {
                                    // For other potential top-level sections accessible from Cart's bottom bar
                                    // (currently only placeholder routes or Cart itself)
                                    // This assumes Tour is the "home base" of the bottom nav graph.
                                    popUpTo(Screen.Tour.route) { saveState = true }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems, key = { it.id }) { item ->
                        CartItemCard(
                            item = item,
                            onQuantityChange = { newQuantity ->
                                cartItems = cartItems.map {
                                    if (it.id == item.id) it.copy(quantity = newQuantity.coerceAtLeast(1)) else it
                                }
                            },
                            onItemSelectedChange = { isSelected ->
                                cartItems = cartItems.map {
                                    if (it.id == item.id) it.copy(isSelected = isSelected) else it
                                }.also { updatedItems ->
                                    selectAllChecked = updatedItems.all { it.isSelected } && updatedItems.isNotEmpty()
                                }
                            },
                            isPreviewMode = isPreviewMode
                        )
                    }
                }

                CartFooter(
                    selectedItemCount = cartItems.count { it.isSelected },
                    onSelectAllClicked = { checked ->
                        selectAllChecked = checked
                        cartItems = cartItems.map { it.copy(isSelected = checked) }
                    },
                    selectAllChecked = selectAllChecked,
                    onBuyNowClicked = { /* TODO: Handle Buy Now */ }
                )
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItemData,
    onQuantityChange: (Int) -> Unit,
    onItemSelectedChange: (Boolean) -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonGreen.copy(alpha = 0.8f)), // Using ButtonGreen as requested
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Adjust height based on content
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = onItemSelectedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = GreenWave2,
                    uncheckedColor = TextWhite.copy(alpha = 0.7f),
                    checkmarkColor = GreenDeep
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (item.imageUrl != null && !isPreviewMode) {
                Image(
                    painter = rememberAsyncImagePainter(model = item.imageUrl),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray.copy(alpha = if (isPreviewMode && item.imageUrl != null) 0.5f else 0.2f)), // Show placeholder if preview and has image URL
                    contentAlignment = Alignment.Center
                ) {
                    if (isPreviewMode && item.imageUrl == null) { // Only show "Img" if no image URL in preview
                        Text("Img", color = TextWhite.copy(alpha = 0.7f), fontSize = 10.sp)
                    } else if (isPreviewMode && item.imageUrl != null){
                        Text("Preview Img", color = TextWhite.copy(alpha = 0.7f), fontSize = 8.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Price ${item.price}", color = TextWhite.copy(alpha = 0.8f), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))
            QuantitySelector(
                quantity = item.quantity,
                onDecrement = { onQuantityChange(item.quantity - 1) },
                onIncrement = { onQuantityChange(item.quantity + 1) }
            )
        }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 0.dp, vertical = 0.dp) // Reduced padding to make buttons more effective
    ) {
        IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp).clip(CircleShape)) {
            Icon(Icons.Filled.Remove, contentDescription = "Decrement", tint = TextWhite, modifier = Modifier.size(18.dp))
        }
        Text(
            text = quantity.toString(),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp) // Keep padding for text
        )
        IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp).clip(CircleShape)) {
            Icon(Icons.Filled.Add, contentDescription = "Increment", tint = TextWhite, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun CartFooter(
    selectedItemCount: Int,
    selectAllChecked: Boolean,
    onSelectAllClicked: (Boolean) -> Unit,
    onBuyNowClicked: () -> Unit
) {
    Surface(
        color = CardBackground.copy(alpha = 0.5f), // Slightly more opaque than typical CardBackground
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectAllChecked,
                    onCheckedChange = onSelectAllClicked,
                    colors = CheckboxDefaults.colors(
                        checkedColor = GreenWave2,
                        uncheckedColor = TextWhite.copy(alpha = 0.7f),
                        checkmarkColor = GreenDeep
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("ALL ($selectedItemCount)", color = TextWhite, fontSize = 14.sp)
            }

            Button(
                onClick = onBuyNowClicked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionButtonLightGray,
                    contentColor = ActionButtonTextDark
                ),
                modifier = Modifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                Text("BUY NOW", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun CartScreenPreview() {
    BirdlensTheme {
        CartScreen(navController = rememberNavController(), isPreviewMode = true)
    }
}