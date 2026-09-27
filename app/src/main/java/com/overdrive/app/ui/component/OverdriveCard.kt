package com.overdrive.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.overdrive.app.ui.theme.OverdriveTheme

enum class OverdriveCardVariant {
    FILLED,
    OUTLINED,
    HERO
}

/**
 * Material 3 Card Container with 100% visual parity with `Widget.Overdrive.M3.CardView`.
 *
 * Enforces:
 * - 8dp corner radius (`@dimen/card_radius_standard`).
 * - Surface container fill (`colorSurfaceContainer`).
 * - Zero elevation shadow (restrained tonal stepping for automotive OLED screens).
 * - Optional hairline border (`outlineVariant`) for outlined cards.
 */
@Composable
fun OverdriveCard(
    modifier: Modifier = Modifier,
    variant: OverdriveCardVariant = OverdriveCardVariant.FILLED,
    backgroundColor: Color = when (variant) {
        OverdriveCardVariant.FILLED -> OverdriveTheme.colors.surfaceContainer
        OverdriveCardVariant.OUTLINED -> OverdriveTheme.colors.surface
        OverdriveCardVariant.HERO -> OverdriveTheme.colors.surfaceContainer
    },
    borderColor: Color = when (variant) {
        OverdriveCardVariant.OUTLINED -> OverdriveTheme.colors.outlineVariant
        else -> Color.Transparent
    },
    borderWidth: Dp = if (variant == OverdriveCardVariant.OUTLINED) OverdriveTheme.dimensions.borderHairline else 0.dp,
    shape: Shape = RoundedCornerShape(
        if (variant == OverdriveCardVariant.HERO) OverdriveTheme.dimensions.cardRadiusHero else OverdriveTheme.dimensions.cardRadiusStandard
    ),
    contentPadding: Dp = if (variant == OverdriveCardVariant.HERO) OverdriveTheme.dimensions.cardPaddingHero else OverdriveTheme.dimensions.cardPaddingStandard,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = OverdriveTheme.colors.primary),
            onClick = onClick
        )
    } else {
        Modifier
    }

    val borderModifier = if (borderWidth > 0.dp && borderColor != Color.Transparent) {
        Modifier.border(BorderStroke(borderWidth, borderColor), shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .then(borderModifier)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}
