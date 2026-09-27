package com.overdrive.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * Material 3 Dialog Frame with 100% parity with `ThemeOverlay.Overdrive.M3.MaterialAlertDialog`.
 *
 * Enforces:
 * - 8dp corner radius (`@dimen/card_radius_dialog`).
 * - Surface container fill (`colorSurfaceContainer`).
 * - Outlined subtle border (`outlineVariant`).
 * - Touch-safe action button bar.
 */
@Composable
fun OverdriveDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    icon: (@Composable () -> Unit)? = null,
    positiveButtonText: String? = null,
    onPositiveClick: (() -> Unit)? = null,
    negativeButtonText: String? = null,
    onNegativeClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val dimensions = OverdriveTheme.dimensions
    val colors = OverdriveTheme.colors
    val typography = androidx.compose.material3.MaterialTheme.typography

    val dialogShape = RoundedCornerShape(dimensions.cardRadiusDialog)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = modifier
                .widthIn(min = dimensions.dialogMinWidth, max = dimensions.dialogMaxWidth)
                .clip(dialogShape)
                .background(colors.surfaceContainer, dialogShape)
                .border(dimensions.borderHairline, colors.outlineVariant, dialogShape)
                .padding(dimensions.cardPaddingHero)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensions.cardGapVertical)
            ) {
                // Header (Icon + Title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (icon != null) {
                        icon()
                    }
                    Text(
                        text = title,
                        style = typography.titleLarge.copy(color = colors.onSurface)
                    )
                }

                // Body Content
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Button Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (negativeButtonText != null && onNegativeClick != null) {
                        OverdriveButton(
                            text = negativeButtonText,
                            onClick = onNegativeClick,
                            variant = OverdriveButtonVariant.TEXT
                        )
                        Spacer(modifier = Modifier.width(dimensions.dialogButtonGapInset * 2))
                    }

                    if (positiveButtonText != null && onPositiveClick != null) {
                        OverdriveButton(
                            text = positiveButtonText,
                            onClick = onPositiveClick,
                            variant = OverdriveButtonVariant.PRIMARY
                        )
                    }
                }
            }
        }
    }
}
