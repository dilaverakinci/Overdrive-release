package com.overdrive.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.overdrive.app.ui.theme.OverdriveTheme

enum class OverdrivePillStatus {
    SUCCESS,
    WARNING,
    DANGER,
    INFO
}

/**
 * Status Pill with 100% visual parity with Overdrive Dashboard status indicators.
 *
 * Semi-transparent container tone with active LED-style status dot and high-contrast text.
 */
@Composable
fun OverdriveStatusPill(
    label: String,
    modifier: Modifier = Modifier,
    status: OverdrivePillStatus = OverdrivePillStatus.SUCCESS,
    showDot: Boolean = true,
    customColor: Color? = null,
    customContainerColor: Color? = null
) {
    val colors = OverdriveTheme.colors
    val dimensions = OverdriveTheme.dimensions

    val statusColor = customColor ?: when (status) {
        OverdrivePillStatus.SUCCESS -> colors.statusSuccess
        OverdrivePillStatus.WARNING -> colors.statusWarning
        OverdrivePillStatus.DANGER -> colors.statusDanger
        OverdrivePillStatus.INFO -> colors.statusInfo
    }

    val containerColor = customContainerColor ?: when (status) {
        OverdrivePillStatus.SUCCESS -> colors.statusSuccessContainer
        OverdrivePillStatus.WARNING -> colors.statusWarningContainer
        OverdrivePillStatus.DANGER -> colors.statusDangerContainer
        OverdrivePillStatus.INFO -> colors.statusInfoContainer
    }

    val pillShape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .clip(pillShape)
            .background(containerColor, pillShape)
            .border(dimensions.borderHairline, statusColor.copy(alpha = 0.4f), pillShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                color = colors.onSurface
            )
        )
    }
}
