package com.overdrive.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test suite verifying that Jetpack Compose Design System tokens
 * maintain 100% parity with XML tokens in:
 * - `res/values/colors_m3.xml`
 * - `res/values-night/colors_m3.xml`
 * - `res/values/dimens_overdrive.xml`
 * - `res/values/themes_overdrive.xml`
 */
class OverdriveDesignSystemParityTest {

    @Test
    fun testDarkColorPaletteMatchesXmlTokens() {
        val dark = DarkOverdriveColors

        // Primary brand teal (seed #00D4AA -> #5DDBB6 in dark mode)
        assertEquals(Color(0xFF5DDBB6), dark.primary)
        assertEquals(Color(0xFF003828), dark.onPrimary)
        assertEquals(Color(0xFF00513B), dark.primaryContainer)
        assertEquals(Color(0xFF78F3CC), dark.onPrimaryContainer)

        // OLED green-black backgrounds and surface tiers
        assertEquals(Color(0xFF010A07), dark.background)
        assertEquals(Color(0xFF010A07), dark.surface)
        assertEquals(Color(0xFF04120F), dark.surfaceContainerLow)
        assertEquals(Color(0xFF081915), dark.surfaceContainer)
        assertEquals(Color(0xFF0E241E), dark.surfaceContainerHigh)
        assertEquals(Color(0xFF143029), dark.surfaceContainerHighest)

        // Automotive status colors
        assertEquals(Color(0xFF5BD382), dark.statusSuccess)
        assertEquals(Color(0xFFFFB870), dark.statusWarning)
        assertEquals(Color(0xFFFFB4AB), dark.statusDanger)
        assertEquals(Color(0xFF85CFFF), dark.statusInfo)
    }

    @Test
    fun testLightColorPaletteMatchesXmlTokens() {
        val light = LightOverdriveColors

        // Primary brand teal in light mode (#007A62)
        assertEquals(Color(0xFF007A62), light.primary)
        assertEquals(Color(0xFFFFFFFF), light.onPrimary)
        assertEquals(Color(0xFF78F3CC), light.primaryContainer)

        // Warm-neutral surfaces
        assertEquals(Color(0xFFF7FAF7), light.background)
        assertEquals(Color(0xFFECEFEC), light.surfaceContainer)
        assertEquals(Color(0xFFF1F4F0), light.surfaceContainerLow)

        // Light status colors
        assertEquals(Color(0xFF1F7A3F), light.statusSuccess)
        assertEquals(Color(0xFFA6601C), light.statusWarning)
        assertEquals(Color(0xFFBA1A1A), light.statusDanger)
    }

    @Test
    fun testDimensionTokensMatchOverdriveStandards() {
        val d = DefaultOverdriveDimensions

        // Strict 8dp corner radius across all components as defined in dimens_overdrive.xml
        assertEquals(8.dp, d.cardRadiusStandard)
        assertEquals(8.dp, d.cardRadiusHero)
        assertEquals(8.dp, d.cardRadiusDialog)
        assertEquals(8.dp, d.cardRadiusAccent)

        // Card and page padding
        assertEquals(12.dp, d.cardPaddingStandard)
        assertEquals(16.dp, d.cardPaddingHero)
        assertEquals(12.dp, d.pagePaddingHorizontal)
        assertEquals(8.dp, d.pagePaddingTop)
        assertEquals(12.dp, d.pagePaddingBottom)

        // Inter-card rhythm
        assertEquals(10.dp, d.cardGapVertical)
        assertEquals(10.dp, d.cardGapHorizontal)
        assertEquals(5.dp, d.cardGapHorizontalHalf)

        // Navigation rail widths
        assertEquals(80.dp, d.railCompactWidth)
        assertEquals(216.dp, d.railExpandedWidth)
    }

    @Test
    fun testTelemetryTypographyUsesMonospaceForStability() {
        val typo = OverdriveTelemetryTypography()
        assertEquals(FontFamily.Monospace, typo.displaySpeed.fontFamily)
        assertEquals(FontFamily.Monospace, typo.metricLarge.fontFamily)
        assertEquals(FontFamily.Monospace, typo.metricMedium.fontFamily)
        assertEquals(FontFamily.Monospace, typo.metricSmall.fontFamily)

        // Speed text must be massive for arms-length glanceability
        assertTrue(typo.displaySpeed.fontSize.value >= 40f)
    }

    @Test
    fun testThemeModeSelectionLogic() {
        // Dark theme must always have dark background
        assertEquals(Color(0xFF010A07), DarkOverdriveColors.background)
        // Light theme must have light background
        assertEquals(Color(0xFFF7FAF7), LightOverdriveColors.background)

        assertFalse(DarkOverdriveColors.background == LightOverdriveColors.background)
    }
}
