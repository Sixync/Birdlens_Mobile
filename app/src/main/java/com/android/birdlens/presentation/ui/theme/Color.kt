// app/src/main/java/com/android/birdlens/presentation/ui/theme/Color.kt
package com.android.birdlens.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// The darkest green, used as the primary background for screens.
val VeryDarkGreenBase = Color(0xFF0d1b06)

val GreenDeep = Color(0xFF0A1F0F)
// Intermediate green shades for UI elements and gradients.
val GreenWave1 = Color(0xFF466043)
val GreenWave2 = Color(0xFF7BA06A)
val GreenWave3 = Color(0xFF6DCFA8)
val GreenWave4 = Color(0xFF61FC7D)

// Specific UI element colors based on the new palette.
val ButtonGreen = Color(0xFF3cb056) // Vibrant accent green for primary actions.
val TextWhite = Color.White
val CardBackground = Color(0xFF1b4020) // Dark green for card surfaces.

// Auth screen specific colors.
val AuthCardBackground = Color.Black.copy(alpha = 0.35f)
val AuthInputBackground = Color.Black.copy(alpha = 0.25f)
val TextFieldPlaceholder = Color.White.copy(alpha = 0.7f)
val SocialButtonBackgroundLight = Color.White
val SocialButtonTextDark = Color.Black.copy(alpha = 0.85f)
val ClickableLinkText = Color(0xFF90EE90)

// Component-specific colors.
val BottomNavGreen = Color(0xFF00821B)
val SearchBarBackground = Color.White.copy(alpha = 0.15f)
val SearchBarPlaceholderText = TextWhite.copy(alpha = 0.6f)
val PageIndicatorInactive = Color.White.copy(alpha = 0.4f)
val PageIndicatorActive = TextWhite
val BirdlensGradientStart = Color(0xFFC0FF00)
val BirdlensGradientEnd = Color(0xFF69D84D)
val ActionButtonLightGray = Color(0xFFD3D3D3)
val ActionButtonTextDark = Color.Black.copy(alpha = 0.8f)
val StoreNamePlaceholderCircle = Color.LightGray
val LogoutButtonGreen = Color(0xFF50B062)
val DividerColor = TextWhite.copy(alpha = 0.2f)
val ImageScrimColor = Color.Black.copy(alpha = 0.4f)

// Colors for map data visualization.
val RangeResident = Color(0x994CAF50) // Green
val RangeBreeding = Color(0x99FFC107) // Amber/Yellow
val RangeNonBreeding = Color(0x992196F3) // Blue
val RangePassage = Color(0x999C27B0) // Purple