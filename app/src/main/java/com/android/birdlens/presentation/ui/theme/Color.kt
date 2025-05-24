package com.android.birdlens.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val GreenDeep = Color(0xFF0A1F0F) // A guess for the darkest background area
val GreenWave1 = Color(0xFF318F49)
val GreenWave2 = Color(0xFFD0FF01) // Very bright lime, use with care for large areas
val GreenWave3 = Color(0xFF2C8B4C)
val GreenWave4 = Color(0xFF11E758)

val ButtonGreen = Color(0xFF466043)
val TextWhite = Color.White
val CardBackground = Color.Black.copy(alpha = 0.25f) // For the semi-transparent card

// Colors for Login Screen (TextFieldBackground was removed as it's same as ButtonGreen)
val TextFieldPlaceholder = Color.White.copy(alpha = 0.7f)
val SocialButtonBackground = Color(0xFFE8E8E8)
val SocialButtonText = Color.Black.copy(alpha = 0.8f)
val ClickableLinkText = Color(0xFF90EE90)

// New color for Tour Screen
val BottomNavGreen = Color(0xFF00821B)
val SearchBarBackground = Color.White.copy(alpha = 0.15f) // Semi-transparent white for search
val SearchBarPlaceholderText = TextWhite.copy(alpha = 0.6f)
val PageIndicatorInactive = Color.White.copy(alpha = 0.4f)
val PageIndicatorActive = TextWhite

// New Colors for Tour Detail Screen
val BirdlensGradientStart = Color(0xFFC0FF00) // New, or adjust GreenWave2
val BirdlensGradientEnd = Color(0xFF69D84D)   // New
val ActionButtonLightGray = Color(0xFFD3D3D3) // For "Join Now", "Add to Cart"
val ActionButtonTextDark = Color.Black.copy(alpha = 0.8f)
val StoreNamePlaceholderCircle = Color.LightGray

// New Colors for Settings Screen
val LogoutButtonGreen = Color(0xFF50B062) // A distinct green for logout button
val DividerColor = TextWhite.copy(alpha = 0.2f)