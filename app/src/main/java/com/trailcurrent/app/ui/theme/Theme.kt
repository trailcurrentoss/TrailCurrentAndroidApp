package com.trailcurrent.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// TrailCurrent Brand Colors
object TrailCurrentPalette {
    // Primary & Secondary (The Brand Core)
    val primary = Color(0xFF52A441)           // Forest / Moss Green
    val primaryLight = Color(0xFF7BC96A)      // Lighter forest green
    val primaryDark = Color(0xFF3D7B31)       // Darker forest green
    val secondary = Color(0xFFD0E2C7)         // Pale Sage
    val secondaryDark = Color(0xFF9AB090)     // Darker sage for dark theme
    val link = Color(0xFF83A79C)              // Dusty teal/eucalyptus

    // Status Colors (High-visibility)
    val success = Color(0xFF74FE00)           // Electric Lime
    val info = Color(0xFF48E6FE)              // Bright Cyan
    val danger = Color(0xFFFF5453)            // Soft Red / Coral

    // Neutrals
    val light = Color(0xFFEBEBEB)             // Off-White / Gray
    val dark = Color(0xFF000000)              // True Black
    val white = Color(0xFFFFFFFF)

    // Subtle backgrounds
    val primaryBgSubtle = Color(0xFFDCEDD9)   // Very soft green tint
    val secondaryBgSubtle = Color(0xFFEDF3EA) // Very soft sage tint
    val surfaceLight = Color(0xFFFAFAFA)
    val surfaceDark = Color(0xFF121212)
    val surfaceVariantDark = Color(0xFF1E1E1E)
}

// Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = TrailCurrentPalette.primary,
    onPrimary = TrailCurrentPalette.dark,              // Black text on primary
    primaryContainer = TrailCurrentPalette.primaryBgSubtle,
    onPrimaryContainer = TrailCurrentPalette.primaryDark,
    secondary = TrailCurrentPalette.secondary,
    onSecondary = TrailCurrentPalette.dark,            // Black text on secondary
    secondaryContainer = TrailCurrentPalette.secondaryBgSubtle,
    onSecondaryContainer = TrailCurrentPalette.primaryDark,
    tertiary = TrailCurrentPalette.link,
    onTertiary = TrailCurrentPalette.dark,
    background = TrailCurrentPalette.surfaceLight,
    onBackground = TrailCurrentPalette.dark,
    surface = TrailCurrentPalette.white,
    onSurface = TrailCurrentPalette.dark,
    surfaceVariant = TrailCurrentPalette.light,
    onSurfaceVariant = Color(0xFF505050),
    error = TrailCurrentPalette.danger,
    onError = TrailCurrentPalette.dark                 // Black text on error
)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = TrailCurrentPalette.primaryLight,
    onPrimary = TrailCurrentPalette.dark,              // Black text on primary
    primaryContainer = TrailCurrentPalette.primaryDark,
    onPrimaryContainer = TrailCurrentPalette.primaryBgSubtle,
    secondary = TrailCurrentPalette.secondaryDark,
    onSecondary = TrailCurrentPalette.dark,            // Black text on secondary
    secondaryContainer = Color(0xFF3D5035),
    onSecondaryContainer = TrailCurrentPalette.secondary,
    tertiary = TrailCurrentPalette.link,
    onTertiary = TrailCurrentPalette.dark,
    background = TrailCurrentPalette.surfaceDark,
    onBackground = TrailCurrentPalette.light,
    surface = TrailCurrentPalette.surfaceVariantDark,
    onSurface = TrailCurrentPalette.light,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = TrailCurrentPalette.danger,
    onError = TrailCurrentPalette.dark                 // Black text on error
)

// Custom colors for specific use cases
object TrailCurrentColors {
    // Status colors (high-visibility neon accents)
    val statusGood = TrailCurrentPalette.success       // Electric Lime
    val statusWarning = Color(0xFFFFC107)              // Amber warning
    val statusCritical = TrailCurrentPalette.danger    // Coral red
    val statusInfo = TrailCurrentPalette.info          // Bright Cyan

    // Tank colors
    val freshWater = TrailCurrentPalette.info          // Bright Cyan
    val greyWater = Color(0xFF9E9E9E)
    val blackWater = Color(0xFF424242)

    // Energy colors
    val solar = Color(0xFFFFC107)
    val batteryGood = TrailCurrentPalette.success      // Electric Lime
    val batteryLow = TrailCurrentPalette.danger        // Coral red

    // Thermostat colors
    val heating = TrailCurrentPalette.danger           // Coral red for heat
    val cooling = TrailCurrentPalette.info             // Bright cyan for cool

    // Brand colors for direct access
    val primary = TrailCurrentPalette.primary
    val secondary = TrailCurrentPalette.secondary
    val link = TrailCurrentPalette.link
}

@Composable
fun TrailCurrentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) {
                TrailCurrentPalette.surfaceDark.toArgb()
            } else {
                TrailCurrentPalette.primary.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
