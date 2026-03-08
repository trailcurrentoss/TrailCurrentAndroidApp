package com.trailcurrentoutbound.app.ui.theme

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
object TrailCurrentOutboundPalette {
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
    primary = TrailCurrentOutboundPalette.primary,
    onPrimary = TrailCurrentOutboundPalette.dark,              // Black text on primary
    primaryContainer = TrailCurrentOutboundPalette.primaryBgSubtle,
    onPrimaryContainer = TrailCurrentOutboundPalette.primaryDark,
    secondary = TrailCurrentOutboundPalette.secondary,
    onSecondary = TrailCurrentOutboundPalette.dark,            // Black text on secondary
    secondaryContainer = TrailCurrentOutboundPalette.secondaryBgSubtle,
    onSecondaryContainer = TrailCurrentOutboundPalette.primaryDark,
    tertiary = TrailCurrentOutboundPalette.link,
    onTertiary = TrailCurrentOutboundPalette.dark,
    background = TrailCurrentOutboundPalette.surfaceLight,
    onBackground = TrailCurrentOutboundPalette.dark,
    surface = TrailCurrentOutboundPalette.white,
    onSurface = TrailCurrentOutboundPalette.dark,
    surfaceVariant = TrailCurrentOutboundPalette.light,
    onSurfaceVariant = Color(0xFF505050),
    error = TrailCurrentOutboundPalette.danger,
    onError = TrailCurrentOutboundPalette.dark                 // Black text on error
)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = TrailCurrentOutboundPalette.primaryLight,
    onPrimary = TrailCurrentOutboundPalette.dark,              // Black text on primary
    primaryContainer = TrailCurrentOutboundPalette.primaryDark,
    onPrimaryContainer = TrailCurrentOutboundPalette.primaryBgSubtle,
    secondary = TrailCurrentOutboundPalette.secondaryDark,
    onSecondary = TrailCurrentOutboundPalette.dark,            // Black text on secondary
    secondaryContainer = Color(0xFF3D5035),
    onSecondaryContainer = TrailCurrentOutboundPalette.secondary,
    tertiary = TrailCurrentOutboundPalette.link,
    onTertiary = TrailCurrentOutboundPalette.dark,
    background = TrailCurrentOutboundPalette.surfaceDark,
    onBackground = TrailCurrentOutboundPalette.light,
    surface = TrailCurrentOutboundPalette.surfaceVariantDark,
    onSurface = TrailCurrentOutboundPalette.light,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = TrailCurrentOutboundPalette.danger,
    onError = TrailCurrentOutboundPalette.dark                 // Black text on error
)

// Custom colors for specific use cases
object TrailCurrentOutboundColors {
    // Status colors (high-visibility neon accents)
    val statusGood = TrailCurrentOutboundPalette.success       // Electric Lime
    val statusWarning = Color(0xFFFFC107)              // Amber warning
    val statusCritical = TrailCurrentOutboundPalette.danger    // Coral red
    val statusInfo = TrailCurrentOutboundPalette.info          // Bright Cyan

    // Tank colors
    val freshWater = TrailCurrentOutboundPalette.info          // Bright Cyan
    val greyWater = Color(0xFF9E9E9E)
    val blackWater = Color(0xFF424242)

    // Energy colors
    val solar = Color(0xFFFFC107)
    val batteryGood = TrailCurrentOutboundPalette.success      // Electric Lime
    val batteryLow = TrailCurrentOutboundPalette.danger        // Coral red

    // Thermostat colors
    val heating = TrailCurrentOutboundPalette.danger           // Coral red for heat
    val cooling = TrailCurrentOutboundPalette.info             // Bright cyan for cool

    // Brand colors for direct access
    val primary = TrailCurrentOutboundPalette.primary
    val secondary = TrailCurrentOutboundPalette.secondary
    val link = TrailCurrentOutboundPalette.link
}

@Composable
fun TrailCurrentOutboundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) {
                TrailCurrentOutboundPalette.surfaceDark.toArgb()
            } else {
                TrailCurrentOutboundPalette.primary.toArgb()
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
