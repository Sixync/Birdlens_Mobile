// path: EXE201/app/src/main/java/com/android/birdlens/ui/theme/Theme.kt
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
import androidx.compose.ui.platform.LocalContext

// Logic: Define a custom DarkColorScheme using our specific brand colors.
// This ensures that Material components like Button, Surface, etc., use OUR colors
// by default, instead of the generic purple/pink theme. The `primary` color
// is what `Button` uses by default for its container.
private val AppDarkColorScheme = darkColorScheme(
    primary = ButtonGreen, // Buttons will now use our defined ButtonGreen
    secondary = GreenWave2,
    tertiary = GreenWave3,
    background = VeryDarkGreenBase,
    surface = CardBackground,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

// The light color scheme is not used, but we keep it for completeness.
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun BirdlensTheme(
    darkTheme: Boolean = true, // Force dark theme to always use our custom scheme
    // Logic: Default dynamicColor to false to disable Material You and enforce our brand colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // This path is now disabled by default.
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Logic: The app will now always use our custom AppDarkColorScheme.
        darkTheme -> AppDarkColorScheme
        else -> AppDarkColorScheme // Also use our dark scheme in light mode for consistency.
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}