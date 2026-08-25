package com.elwataniatv.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

/**
 * Parses a "#RRGGBB" hex string (as saved by the admin panel in
 * config/app → primaryColor / secondaryColor) into a Compose Color.
 * Returns null for blank or malformed values so the caller can fall back.
 */
fun layoutDirectionForLanguage(language: String): LayoutDirection {
    val baseLanguage = language.trim().lowercase()
        .substringBefore('-')
        .substringBefore('_')
    return if (baseLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
}

fun parseHexColor(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6) return null
    return runCatching {
        Color(clean.toLong(16) or 0xFF000000L)
    }.getOrNull()
}

@Composable
fun ElwataniaTVTheme(
    darkTheme: Boolean = true, // Default to dark TV experience theme
    primaryColor: Color? = null,   // remote config/app → primaryColor
    secondaryColor: Color? = null, // remote config/app → secondaryColor
    accentColor: Color? = null,
    dynamicColorEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val primary = primaryColor ?: BrandPrimary
    val secondary = secondaryColor ?: BrandSecondary
    val accent = accentColor ?: BrandAccent
    // Firebase controls whether Android 12+ system colors should be used.
    // When disabled, the channel palette remains deterministic and editable from the dashboard.
    val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useDynamicColors = dynamicColorEnabled && supportsDynamicColors
    val colorScheme = if (useDynamicColors) {
        if (darkTheme) dynamicDarkColorScheme(LocalView.current.context) else dynamicLightColorScheme(LocalView.current.context)
    } else if (darkTheme) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            tertiary = accent,
            background = BrandBg,
            surface = BrandPanel,
            surfaceVariant = BrandBorder,
            onPrimary = TextPrimary,
            onSecondary = TextPrimary,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = accent,
            background = Color(0xFFF6F8FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFE8EDF5),
            outline = Color(0xFFCBD5E1),
            onBackground = Color(0xFF111827),
            onSurface = Color(0xFF111827)
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val languageTag = LocalConfiguration.current.locales[0].toLanguageTag()
    val layoutDirection = layoutDirectionForLanguage(languageTag)
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
