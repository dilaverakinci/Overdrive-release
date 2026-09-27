package com.overdrive.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

import androidx.appcompat.app.AppCompatDelegate
import com.overdrive.app.ui.util.PreferencesManager

val LocalOverdriveColors = staticCompositionLocalOf { DarkOverdriveColors }
val LocalOverdriveDimensions = staticCompositionLocalOf { DefaultOverdriveDimensions }
val LocalOverdriveTelemetryTypography = staticCompositionLocalOf { OverdriveTelemetryTypography() }

val OverdriveShapes = Shapes(
    extraSmall = RoundedCornerShape(DefaultOverdriveDimensions.cardRadiusAccent),
    small = RoundedCornerShape(DefaultOverdriveDimensions.cardRadiusStandard),
    medium = RoundedCornerShape(DefaultOverdriveDimensions.cardRadiusStandard),
    large = RoundedCornerShape(DefaultOverdriveDimensions.cardRadiusHero),
    extraLarge = RoundedCornerShape(DefaultOverdriveDimensions.cardRadiusHero)
)

enum class OverdriveThemeMode {
    DARK,
    LIGHT,
    AUTO
}

/**
 * Overdrive Material 3 Theme for BYD Smart Cockpit displays.
 *
 * Guarantees 100% visual and ergonomic parity with existing native XML styling
 * (colors_m3.xml, themes_overdrive.xml, dimens_overdrive.xml).
 */
@Composable
fun OverdriveTheme(
    themeMode: OverdriveThemeMode = OverdriveThemeMode.AUTO,
    isHeadlightOn: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        OverdriveThemeMode.DARK -> true
        OverdriveThemeMode.LIGHT -> false
        OverdriveThemeMode.AUTO -> {
            try {
                when (PreferencesManager.getThemeMode()) {
                    AppCompatDelegate.MODE_NIGHT_YES -> true
                    AppCompatDelegate.MODE_NIGHT_NO -> false
                    else -> isHeadlightOn || isSystemDark
                }
            } catch (_: Throwable) {
                isHeadlightOn || isSystemDark
            }
        }
    }

    val colors = if (isDark) DarkOverdriveColors else LightOverdriveColors

    val materialColorScheme = ColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        inversePrimary = if (isDark) Color(0xFF00876C) else Color(0xFF5DDBB6),
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        secondaryContainer = colors.secondaryContainer,
        onSecondaryContainer = colors.onSecondaryContainer,
        tertiary = colors.tertiary,
        onTertiary = colors.onTertiary,
        tertiaryContainer = colors.tertiaryContainer,
        onTertiaryContainer = colors.onTertiaryContainer,
        background = colors.background,
        onBackground = colors.onBackground,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.onSurfaceVariant,
        surfaceTint = colors.primary,
        inverseSurface = if (isDark) Color(0xFFDEE4E0) else Color(0xFF2D3130),
        inverseOnSurface = if (isDark) Color(0xFF2D3130) else Color(0xFFEEF1ED),
        error = colors.error,
        onError = colors.onError,
        errorContainer = colors.errorContainer,
        onErrorContainer = colors.onErrorContainer,
        outline = colors.outline,
        outlineVariant = colors.outlineVariant,
        scrim = Color(0xFF000000),
        surfaceBright = if (isDark) Color(0xFF1B3B32) else Color(0xFFF7FAF7),
        surfaceDim = if (isDark) Color(0xFF010A07) else Color(0xFFD8DBD7),
        surfaceContainer = colors.surfaceContainer,
        surfaceContainerHigh = colors.surfaceContainerHigh,
        surfaceContainerHighest = colors.surfaceContainerHighest,
        surfaceContainerLow = colors.surfaceContainerLow,
        surfaceContainerLowest = colors.surfaceContainerLowest
    )

    CompositionLocalProvider(
        LocalOverdriveColors provides colors,
        LocalOverdriveDimensions provides DefaultOverdriveDimensions,
        LocalOverdriveTelemetryTypography provides OverdriveTelemetryTypography()
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = DefaultOverdriveTypography,
            shapes = OverdriveShapes,
            content = content
        )
    }
}

object OverdriveTheme {
    val colors: OverdriveColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOverdriveColors.current

    val dimensions: OverdriveDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalOverdriveDimensions.current

    val telemetryTypography: OverdriveTelemetryTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalOverdriveTelemetryTypography.current
}
