package com.overdrive.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.overdrive.app.ui.theme.OverdriveTheme

enum class OverdriveButtonVariant {
    PRIMARY,
    TONAL,
    OUTLINED,
    TEXT,
    DANGER
}

/**
 * Material 3 Automotive Button with 100% parity with `Widget.Overdrive.M3.Button`.
 *
 * Enforces:
 * - 8dp corner radius (`@dimen/card_radius_accent`).
 * - Minimum 48dp touch target for safe, vibration-resistant in-car interaction.
 * - Non-all-caps sentence case typography for high glanceability.
 */
@Composable
fun OverdriveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OverdriveButtonVariant = OverdriveButtonVariant.PRIMARY,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val dimensions = OverdriveTheme.dimensions
    val colors = OverdriveTheme.colors
    val typography = androidx.compose.material3.MaterialTheme.typography

    val shape = RoundedCornerShape(dimensions.cardRadiusAccent)
    val buttonMinModifier = modifier.defaultMinSize(minHeight = 48.dp)

    when (variant) {
        OverdriveButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonMinModifier,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.surfaceContainerHigh,
                    disabledContentColor = colors.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                Text(
                    text = text,
                    style = typography.labelLarge
                )
            }
        }

        OverdriveButtonVariant.TONAL -> {
            Button(
                onClick = onClick,
                modifier = buttonMinModifier,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                Text(
                    text = text,
                    style = typography.labelLarge
                )
            }
        }

        OverdriveButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonMinModifier,
                enabled = enabled,
                shape = shape,
                border = BorderStroke(dimensions.borderHairline, colors.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                Text(
                    text = text,
                    style = typography.labelLarge
                )
            }
        }

        OverdriveButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = buttonMinModifier,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colors.primary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                Text(
                    text = text,
                    style = typography.labelLarge
                )
            }
        }

        OverdriveButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                modifier = buttonMinModifier,
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = colors.onError
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                }
                Text(
                    text = text,
                    style = typography.labelLarge
                )
            }
        }
    }
}
