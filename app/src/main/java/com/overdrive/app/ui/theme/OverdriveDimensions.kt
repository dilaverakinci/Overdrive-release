package com.overdrive.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Overdrive Design System Dimension Tokens.
 *
 * Mapped 1:1 with `res/values/dimens_overdrive.xml` and `res/values/dimens.xml`.
 * Calibrated for BYD Seal 15.6" rotatable infotainment (1920x1080 landscape, 1080x1920 portrait).
 */
@Immutable
data class OverdriveDimensions(
    // App bar
    val appBarHeight: Dp = 60.dp,

    // Page padding / outer rhythm
    val pagePaddingHorizontal: Dp = 24.dp,
    val pagePaddingTop: Dp = 20.dp,
    val pagePaddingBottom: Dp = 24.dp,

    // Inter-card gaps
    val cardGapVertical: Dp = 12.dp,
    val cardGapHorizontal: Dp = 12.dp,
    val cardGapHorizontalHalf: Dp = 6.dp,

    // Card radii (Strictly 8dp as defined in dimens_overdrive.xml)
    val cardRadiusStandard: Dp = 8.dp,
    val cardRadiusHero: Dp = 8.dp,
    val cardRadiusDialog: Dp = 8.dp,
    val cardRadiusAccent: Dp = 8.dp,

    // Card padding
    val cardPaddingStandard: Dp = 20.dp,
    val cardPaddingHero: Dp = 24.dp,

    // Grid tile sizing
    val gridTileMinHeight: Dp = 128.dp,

    // Card icon sizes
    val cardIconStandard: Dp = 24.dp,
    val cardIconService: Dp = 32.dp,
    val cardIconHero: Dp = 56.dp,

    // Section overlines
    val sectionOverlineTop: Dp = 12.dp,
    val sectionOverlineBottom: Dp = 8.dp,

    // Navigation rail widths
    val railCompactWidth: Dp = 80.dp,
    val railExpandedWidth: Dp = 216.dp,
    val railRowHeight: Dp = 56.dp,
    val railIconSize: Dp = 24.dp,

    // Dialog & Modals
    val dialogButtonGapInset: Dp = 4.dp,
    val dialogMinWidth: Dp = 320.dp,
    val dialogMaxWidth: Dp = 560.dp,

    // Navigation rail pill metrics
    val railItemPillInsetHorizontal: Dp = 12.dp,
    val railItemPillInsetVertical: Dp = 4.dp,
    val railItemPillRadius: Dp = 8.dp,

    // Hairline borders
    val borderHairline: Dp = 1.dp
) {
    companion object {
        val Default = OverdriveDimensions()
        val appBarHeight: Dp get() = Default.appBarHeight
        val pagePaddingHorizontal: Dp get() = Default.pagePaddingHorizontal
        val pagePaddingTop: Dp get() = Default.pagePaddingTop
        val pagePaddingBottom: Dp get() = Default.pagePaddingBottom
        val cardGapVertical: Dp get() = Default.cardGapVertical
        val cardGapHorizontal: Dp get() = Default.cardGapHorizontal
        val cardRadiusStandard: Dp get() = Default.cardRadiusStandard
        val cardRadiusHero: Dp get() = Default.cardRadiusHero
        val cardPaddingStandard: Dp get() = Default.cardPaddingStandard
        val cardPaddingHero: Dp get() = Default.cardPaddingHero
        val railCompactWidth: Dp get() = Default.railCompactWidth
        val railExpandedWidth: Dp get() = Default.railExpandedWidth
        val railRowHeight: Dp get() = Default.railRowHeight
        val railItemPillInsetHorizontal: Dp get() = Default.railItemPillInsetHorizontal
        val railItemPillInsetVertical: Dp get() = Default.railItemPillInsetVertical
        val railItemPillRadius: Dp get() = Default.railItemPillRadius
    }
}

val DefaultOverdriveDimensions = OverdriveDimensions()
