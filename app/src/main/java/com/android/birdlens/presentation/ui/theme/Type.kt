// app/src/main/java/com/android/birdlens/presentation/ui/theme/Type.kt
package com.android.birdlens.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define base text styles for consistent typography throughout the app.
val Typography = Typography(
    // Style for large titles, e.g., screen headers.
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        color = TextWhite // Titles are consistently white.
    ),
    // Style for primary body text.
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = TextWhite.copy(alpha = 0.8f) // Body text is slightly transparent for hierarchy.
    ),
    // Style for smaller body text, often used for captions or subtitles.
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        color = TextWhite.copy(alpha = 0.8f)
    ),
    // Style for small labels, like on buttons or for annotations.
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
        color = TextWhite.copy(alpha = 0.7f)
    )
)