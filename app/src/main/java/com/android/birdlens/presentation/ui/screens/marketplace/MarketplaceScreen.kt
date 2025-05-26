// app/src/main/java/com/android/birdlens/presentation/ui/screens/marketplace/MarketplaceScreen.kt
package com.android.birdlens.presentation.ui.screens.marketplace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.birdlens.presentation.navigation.Screen
import com.android.birdlens.presentation.ui.components.AppScaffold
import com.android.birdlens.presentation.ui.screens.tour.SectionHeader
import com.android.birdlens.ui.theme.*

// --- Data Models (Dummy for Marketplace) ---
data class MarketplaceItem(
        val id: Int,
        val imageUrl: String,
        val title: String = "",
        val subtitle: String? = null, // e.g., for product number or rating
        val type: ItemType
)

enum class ItemType {
    BIRD_PICTURE,
    CATEGORY,
    DEAL,
    RENTAL_SERVICE
}

@Composable
fun MarketplaceScreen(
        navController: NavController,
        modifier: Modifier = Modifier,
        isPreviewMode: Boolean = LocalInspectionMode.current
) {
    var searchQuery by remember { mutableStateOf("") }

    // Dummy data
    val birdPicture =
            MarketplaceItem(
                    1,
                    "https://images.unsplash.com/photo-1518992028580-6d57bd80f2dd?w=800&auto=format&fit=crop&q=60",
                    "Eastern Bluebird",
                    type = ItemType.BIRD_PICTURE
            )
    val categories =
            listOf(
                    MarketplaceItem(
                            10,
                            "https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=300&auto=format&fit=crop&q=60",
                            "Binoculars",
                            type = ItemType.CATEGORY
                    ),
                    MarketplaceItem(
                            11,
                            "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=300&auto=format&fit=crop&q=60",
                            "Cameras",
                            type = ItemType.CATEGORY
                    ),
                    MarketplaceItem(
                            12,
                            "https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=300&auto=format&fit=crop&q=60",
                            "Tents",
                            type = ItemType.CATEGORY
                    ),
                    MarketplaceItem(
                            13,
                            "https://images.unsplash.com/photo-1523626752472-b55a628f1acc?w=300&auto=format&fit=crop&q=60",
                            "Apparel",
                            type = ItemType.CATEGORY
                    )
            )
    val deals =
            listOf(
                    MarketplaceItem(
                            20,
                            "https://images.unsplash.com/photo-1572361493773-5091190ae8ab?w=400&auto=format&fit=crop&q=60",
                            "Camo Netting",
                            "Product #1",
                            type = ItemType.DEAL
                    ),
                    MarketplaceItem(
                            21,
                            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400&auto=format&fit=crop&q=60",
                            "Pro Binoculars",
                            "Product #2",
                            type = ItemType.DEAL
                    ),
                    MarketplaceItem(
                            22,
                            "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=400&auto=format&fit=crop&q=60",
                            "Vintage Camera",
                            "Product #3",
                            type = ItemType.DEAL
                    )
            )
    val rentalServices =
            listOf(
                    MarketplaceItem(
                            30,
                            "https://images.unsplash.com/photo-1531891437562-4301cf35b7e4?w=300&auto=format&fit=crop&q=60",
                            "Assistant",
                            type = ItemType.RENTAL_SERVICE
                    ),
                    MarketplaceItem(
                            31,
                            "https://images.unsplash.com/photo-1604272058880-347a08c71049?w=300&auto=format&fit=crop&q=60",
                            "Camera & Bino",
                            type = ItemType.RENTAL_SERVICE
                    ),
                    MarketplaceItem(
                            32,
                            "https://images.unsplash.com/photo-1519638831568-d9897f54ed69?w=300&auto=format&fit=crop&q=60",
                            "Pro Camera",
                            type = ItemType.RENTAL_SERVICE
                    ),
                    MarketplaceItem(
                            33,
                            "https://images.unsplash.com/photo-1580894742574-biky916073ac?w=300&auto=format&fit=crop&q=60",
                            "Gloves",
                            type = ItemType.RENTAL_SERVICE
                    )
            )

    AppScaffold(
            navController = navController,
            topBar = {
                MarketplaceHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onNavigateToCart = { navController.navigate(Screen.Cart.route) }
                )
            },
            showBottomBar = true
    ) { innerPadding ->
        LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp)
        ) {
            item {
                SectionHeader(title = "Bird pictures", onSeeAllClick = { /* TODO */})
                BirdPictureCard(
                        item = birdPicture,
                        onClick = { /* TODO */},
                        isPreviewMode = isPreviewMode
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                MarketplaceHorizontalListSection(
                        title = "Categories",
                        items = categories,
                        onItemClick = { /* TODO */},
                        onSeeAllClick = { /* TODO */},
                        itemCard = { itemData, onClickAction, isPreview ->
                            SmallItemCard(
                                    item = itemData,
                                    onClick = onClickAction,
                                    isPreviewMode = isPreview
                            )
                        },
                        isPreviewMode = isPreviewMode
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                MarketplaceHorizontalListSection(
                        title = "Deals",
                        items = deals,
                        onItemClick = { /* TODO */},
                        onSeeAllClick = { /* TODO */},
                        itemCard = { itemData, onClickAction, isPreview ->
                            DealItemCard(
                                    item = itemData,
                                    onClick = onClickAction,
                                    isPreviewMode = isPreview
                            )
                        },
                        isPreviewMode = isPreviewMode
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                MarketplaceHorizontalListSection(
                        title = "Rental Services",
                        items = rentalServices,
                        onItemClick = { /* TODO */},
                        onSeeAllClick = { /* TODO */},
                        itemCard = { itemData, onClickAction, isPreview ->
                            SmallItemCard(
                                    item = itemData,
                                    onClick = onClickAction,
                                    isPreviewMode = isPreview,
                                    showSubtitleAsRating = true
                            )
                        },
                        isPreviewMode = isPreviewMode
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MarketplaceHeader(
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        onNavigateToCart: () -> Unit
) {
    Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
        Surface(
                shape = RoundedCornerShape(50),
                color = SearchBarBackground,
                modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
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
                                    contentDescription = "Search",
                                    tint = SearchBarPlaceholderText
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                            "Search something",
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
        Spacer(modifier = Modifier.height(20.dp))
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                        "BIRDLENS",
                        style =
                                MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GreenWave2,
                                        letterSpacing = 1.sp
                                )
                )
                Text(
                        "Marketplace",
                        style =
                                MaterialTheme.typography.titleLarge.copy(
                                        color = TextWhite,
                                        fontWeight = FontWeight.SemiBold
                                )
                )
            }
            IconButton(onClick = onNavigateToCart) {
                Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = "Go to Cart",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun BirdPictureCard(item: MarketplaceItem, onClick: () -> Unit, isPreviewMode: Boolean) {
    Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(200.dp).clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPreviewMode) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                ) { Text("Preview: ${item.title}", color = TextWhite) }
            } else {
                Image(
                        painter = rememberAsyncImagePainter(model = item.imageUrl),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                    modifier =
                            Modifier.fillMaxSize()
                                    .background(
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    Color.Transparent,
                                                                    Color.Black.copy(alpha = 0.0f),
                                                                    Color.Black.copy(alpha = 0.5f)
                                                            ),
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

@Composable
fun MarketplaceHorizontalListSection(
        title: String,
        items: List<MarketplaceItem>,
        onItemClick: (MarketplaceItem) -> Unit,
        onSeeAllClick: () -> Unit,
        itemCard:
                @Composable
                (item: MarketplaceItem, onClick: () -> Unit, isPreviewMode: Boolean) -> Unit,
        isPreviewMode: Boolean
) {
    Column {
        SectionHeader(title = title, onSeeAllClick = onSeeAllClick)
        LazyRow(
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = items, key = { it.id }) { currentItem ->
                itemCard(currentItem, { onItemClick(currentItem) }, isPreviewMode)
            }
        }
    }
}

@Composable
fun SmallItemCard(
        item: MarketplaceItem,
        onClick: () -> Unit,
        isPreviewMode: Boolean,
        showSubtitleAsRating: Boolean = false
) {
    Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(120.dp).height(150.dp).clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.65f).fillMaxWidth()) {
                if (isPreviewMode) {
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                    ) { Text("Img", color = TextWhite.copy(alpha = 0.7f), fontSize = 10.sp) }
                } else {
                    Image(
                            painter = rememberAsyncImagePainter(model = item.imageUrl),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                    modifier = Modifier.weight(0.35f).padding(8.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
            ) {
                Text(
                        text = item.title,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                if (showSubtitleAsRating && item.subtitle == null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        repeat(5) {
                            Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = PageIndicatorInactive,
                                    modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                } else if (item.subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                            text = item.subtitle,
                            color = TextWhite.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun DealItemCard(item: MarketplaceItem, onClick: () -> Unit, isPreviewMode: Boolean) {
    Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(180.dp).height(220.dp).clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.7f).fillMaxWidth()) {
                if (isPreviewMode) {
                    Box(
                            modifier =
                                    Modifier.fillMaxSize()
                                            .background(Color.DarkGray.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                    ) { Text("Preview Deal", color = TextWhite, fontSize = 12.sp) }
                } else {
                    Image(
                            painter = rememberAsyncImagePainter(model = item.imageUrl),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                    modifier = Modifier.weight(0.3f).padding(10.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                        item.title,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                                Icons.Filled.Star,
                                contentDescription = "Rating Star",
                                tint = if (index < 3) Color.Yellow else PageIndicatorInactive,
                                modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if (item.subtitle != null) {
                        Text(item.subtitle, color = TextWhite.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=360dp,height=800dp,dpi=480")
@Composable
fun MarketplaceScreenPreview() {
    BirdlensTheme {
        MarketplaceScreen(navController = rememberNavController(), isPreviewMode = true)
    }
}
