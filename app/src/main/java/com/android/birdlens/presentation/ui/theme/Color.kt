// EXE201/app/src/main/java/com/android/birdlens/ui/theme/Color.kt
package com.android.birdlens.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val GreenDeep = Color(0xFF0A1F0F) // Base for some older designs, can be reviewed
val VeryDarkGreenBase = Color(0xFF050F07) // Current darker base for SharedAppBackground

// Updated GreenWave colors for the new linear gradient effect
val GreenWave1 = Color(0xFF466043) // Was 0xFF318F49
val GreenWave2 = Color(0xFF7BA06A) // Was 0xFFD0FF01 (Lime Green)
val GreenWave3 = Color(0xFF6DCFA8) // Was 0xFF2C8B4C
val GreenWave4 = Color(0xFF61FC7D) // Was 0xFF11E758

val ButtonGreen = Color(0xFF466043) // This is now the same as GreenWave1, might be intentional or review later
val TextWhite = Color.White

// Background for the cards in Auth screens (frosted glass effect)
val AuthCardBackground = Color.Black.copy(alpha = 0.35f)
// Background for text input fields in Auth screens
val AuthInputBackground = Color.Black.copy(alpha = 0.25f)


val TextFieldPlaceholder = Color.White.copy(alpha = 0.7f)

// Social Login Buttons - new style
val SocialButtonBackgroundLight = Color.White
val SocialButtonTextDark = Color.Black.copy(alpha = 0.85f)

val ClickableLinkText = Color(0xFF90EE90)

// Colors for Tour Screen
val BottomNavGreen = Color(0xFF00821B)
val SearchBarBackground = Color.White.copy(alpha = 0.15f)
val SearchBarPlaceholderText = TextWhite.copy(alpha = 0.6f)
val PageIndicatorInactive = Color.White.copy(alpha = 0.4f)
val PageIndicatorActive = TextWhite

// Colors for Tour Detail Screen
val BirdlensGradientStart = Color(0xFFC0FF00)
val BirdlensGradientEnd = Color(0xFF69D84D)
val ActionButtonLightGray = Color(0xFFD3D3D3)
val ActionButtonTextDark = Color.Black.copy(alpha = 0.8f)
val StoreNamePlaceholderCircle = Color.LightGray

// CardBackground - can be used for non-auth screens or specific cases
val CardBackground = Color.Black.copy(alpha = 0.25f)

// Colors for Settings Screen
val LogoutButtonGreen = Color(0xFF50B062)
val DividerColor = TextWhite.copy(alpha = 0.2f)