// EXE201/app/src/main/java/com/android/birdlens/presentation/ui/screens/cart/CartScreen.kt
package com.android.birdlens.presentation.ui.screens.cart

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource // Import this
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.R // Import this
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.ui.theme.*

// CartItemData data class remains the same
data class CartItemData(
    val id: Int,
    val name: String, // Dynamic
    val price: String, // Dynamic
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

    AppScaffold(
        navController = navController,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.cart_title), // Localized
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back), // Localized
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        showBottomBar = true
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

@Composable
fun CartItemCard(
    item: CartItemData,
    onQuantityChange: (Int) -> Unit,
    onItemSelectedChange: (Boolean) -> Unit,
    isPreviewMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ButtonGreen.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Ensure card wraps content height
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
                    contentDescription = item.name, // Dynamic item name for accessibility
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
                        .background(Color.DarkGray.copy(alpha = if (isPreviewMode && item.imageUrl != null) 0.5f else 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPreviewMode && item.imageUrl == null) {
                        Text(
                            stringResource(id = R.string.cart_item_image_preview_placeholder), // Localized
                            color = TextWhite.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    } else if (isPreviewMode && item.imageUrl != null) {
                        Text(
                            stringResource(id = R.string.cart_item_image_preview_placeholder_detail), // Localized
                            color = TextWhite.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) // Dynamic
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(id = R.string.cart_item_price_format, item.price), // Localized format string
                    color = TextWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
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
            .padding(horizontal = 0.dp, vertical = 0.dp)
    ) {
        IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp).clip(CircleShape)) {
            Icon(
                Icons.Filled.Remove,
                contentDescription = stringResource(id = R.string.cart_decrement_quantity), // Localized
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = quantity.toString(), // Dynamic number
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp).clip(CircleShape)) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.cart_increment_quantity), // Localized
                tint = TextWhite,
                modifier = Modifier.size(18.dp)
            )
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
        color = CardBackground.copy(alpha = 0.5f),
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
                Text(
                    stringResource(id = R.string.all_selected_count, selectedItemCount), // Localized
                    color = TextWhite,
                    fontSize = 14.sp
                )
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
                Text(
                    stringResource(id = R.string.buy_now), // Localized
                    fontWeight = FontWeight.Bold
                )
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