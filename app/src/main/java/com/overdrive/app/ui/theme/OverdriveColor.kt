package com.overdrive.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Overdrive Design System Color Tokens.
 *
 * Mapped 1:1 with Material 3 tokens from:
 * - `res/values/colors_m3.xml` (Light palette)
 * - `res/values-night/colors_m3.xml` (Dark palette)
 *
 * Generated from seed #00D4AA (brand teal) with BYD automotive OLED calibrations:
 * - Deep green-black OLED surfaces (#010A07) for minimal battery drain and glare.
 * - Restrained, accessible status indicators matching SAE/ISO automotive standards.
 */
@Immutable
data class OverdriveColors(
    // Primary Brand
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    // Secondary
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    // Tertiary (Sky-blue accent for vehicle & data viz)
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    // Error / Alert
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    // Background & Surfaces (M3 Stepped Elevation Containers)
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    // Outline & Dividers
    val outline: Color,
    val outlineVariant: Color,

    // Semantic Automotive Status (Paired with Container fills)
    val statusSuccess: Color,
    val statusSuccessContainer: Color,
    val statusWarning: Color,
    val statusWarningContainer: Color,
    val statusDanger: Color,
    val statusDangerContainer: Color,
    val statusInfo: Color,
    val statusInfoContainer: Color,

    // Shifter / Gear positions (P, R, N, D)
    val gearPark: Color = Color(0xFFFF5252),
    val gearReverse: Color = Color(0xFFFFAB00),
    val gearNeutral: Color = Color(0xFF94A3B8),
    val gearDrive: Color = Color(0xFF5BD382)
)

val DarkOverdriveColors = OverdriveColors(
    primary = Color(0xFF5DDBB6),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFF78F3CC),

    secondary = Color(0xFFB1CCC0),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF334B43),
    onSecondaryContainer = Color(0xFFCDE8DC),

    tertiary = Color(0xFF85CFFF),
    onTertiary = Color(0xFF00344C),
    tertiaryContainer = Color(0xFF004C6C),
    onTertiaryContainer = Color(0xFFC5E7FF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF010A07),
    onBackground = Color(0xFFE6EDE9),
    surface = Color(0xFF010A07),
    onSurface = Color(0xFFE6EDE9),
    surfaceVariant = Color(0xFF2C3F39),
    onSurfaceVariant = Color(0xFFB2C4BC),
    surfaceContainerLowest = Color(0xFF000503),
    surfaceContainerLow = Color(0xFF04120F),
    surfaceContainer = Color(0xFF081915),
    surfaceContainerHigh = Color(0xFF0E241E),
    surfaceContainerHighest = Color(0xFF143029),

    outline = Color(0xFF7D918B),
    outlineVariant = Color(0xFF24332D),

    statusSuccess = Color(0xFF5BD382),
    statusSuccessContainer = Color(0xCC06331F),
    statusWarning = Color(0xFFFFB870),
    statusWarningContainer = Color(0xCC33240C),
    statusDanger = Color(0xFFFFB4AB),
    statusDangerContainer = Color(0xCC3A1512),
    statusInfo = Color(0xFF85CFFF),
    statusInfoContainer = Color(0xCC062F42)
)

val LightOverdriveColors = OverdriveColors(
    primary = Color(0xFF007A62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF78F3CC),
    onPrimaryContainer = Color(0xFF002115),

    secondary = Color(0xFF4B635A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8DC),
    onSecondaryContainer = Color(0xFF072019),

    tertiary = Color(0xFF00658F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC5E7FF),
    onTertiaryContainer = Color(0xFF001E2E),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF7FAF7),
    onBackground = Color(0xFF181C1A),
    surface = Color(0xFFF7FAF7),
    onSurface = Color(0xFF181C1A),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF3F4944),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F0),
    surfaceContainer = Color(0xFFECEFEC),
    surfaceContainerHigh = Color(0xFFE6E9E6),
    surfaceContainerHighest = Color(0xFFE0E4E1),

    outline = Color(0xFF6F7975),
    outlineVariant = Color(0xFFBFC9C3),

    statusSuccess = Color(0xFF1F7A3F),
    statusSuccessContainer = Color(0xCCB5EFCB),
    statusWarning = Color(0xFFA6601C),
    statusWarningContainer = Color(0xCCFFDDB8),
    statusDanger = Color(0xFFBA1A1A),
    statusDangerContainer = Color(0xCCFFDAD6),
    statusInfo = Color(0xFF00658F),
    statusInfoContainer = Color(0xCCCBE6FF)
)
