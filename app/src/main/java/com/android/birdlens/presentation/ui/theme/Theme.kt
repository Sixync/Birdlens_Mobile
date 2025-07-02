// app/src/main/java/com/android/birdlens/presentation/ui/theme/Theme.kt
package com.android.birdlens.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Custom dark color scheme using the defined brand colors.
private val BirdlensDarkColorScheme = darkColorScheme(
    primary = ButtonGreen,
    onPrimary = VeryDarkGreenBase,
    background = VeryDarkGreenBase,
    surface = CardBackground,
    onSurface = TextWhite,
    surfaceVariant = CardBackground.copy(alpha = 0.6f), // Used for secondary surfaces like assistant chat bubbles
    onSurfaceVariant = TextWhite,
    error = Color(0xFFFFB4AB), // Standard Material 3 error color for dark themes
    onError = Color(0xFF690005)
)

@Composable
fun BirdlensTheme(
    // The app now consistently uses the dark theme to match the background.
    darkTheme: Boolean = true,
    // Dynamic color is disabled to enforce the brand's color palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> BirdlensDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}