package com.overdrive.app.ui.diagnostics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions

/**
 * State representing system health metrics and diagnostic statuses.
 */
data class DiagnosticsUiState(
    val networkSsid: String = "Wi-Fi: Bağlı Değil",
    val tunnelState: String = "Tünel: Çevrimdışı",
    val isTunnelOnline: Boolean = false,
    val storageUsed: String = "--",
    val storageFree: String = "--",
    val cameraStatus: String = "Kamera Hazır",
    val isCameraOnline: Boolean = true,
    val batterySoh: String = "%100 Sağlık",
    val isBatteryGood: Boolean = true,
    val isBatteryReviewNeeded: Boolean = false,
)

/**
 * 100% Jetpack Compose Native Diagnostics Screen.
 * Complete replacement for legacy XML fragment_diagnostics.
 */
@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onAdbClick: () -> Unit = {},
    onTrafficClick: () -> Unit = {},
    onCameraProbeClick: () -> Unit = {},
    onBatteryHealthClick: () -> Unit = {},
    onBatteryLongClick: () -> Unit = {},
    onSettingsShortcutClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = OverdriveDimensions.pagePaddingHorizontal,
                    end = OverdriveDimensions.pagePaddingHorizontal,
                    top = OverdriveDimensions.pagePaddingTop,
                    bottom = OverdriveDimensions.pagePaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.rail_diagnostics),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.diagnostics_hero_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OverdriveButton(
                    text = stringResource(R.string.rail_settings),
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = onSettingsShortcutClick,
                )
            }

            // Section 1: System Health Section Title
            Text(
                text = stringResource(R.string.diagnostics_health_section),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Health 2x2 Grid (Row 1: Network & Storage)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HealthTile(
                    title = stringResource(R.string.diagnostics_health_network),
                    primaryText = state.networkSsid,
                    secondaryText = state.tunnelState,
                    isHealthy = state.isTunnelOnline,
                    modifier = Modifier.weight(1f),
                )
                HealthTile(
                    title = stringResource(R.string.diagnostics_health_storage),
                    primaryText = state.storageUsed,
                    secondaryText = state.storageFree,
                    isHealthy = true,
                    modifier = Modifier.weight(1f),
                )
            }

            // Health 2x2 Grid (Row 2: Camera & Battery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HealthTile(
                    title = stringResource(R.string.diagnostics_health_camera),
                    primaryText = state.cameraStatus,
                    secondaryText = if (state.isCameraOnline) "4x AVM Akışı Aktif" else "Kamera Servisi Bekliyor",
                    isHealthy = state.isCameraOnline,
                    onClick = onCameraProbeClick,
                    modifier = Modifier.weight(1f),
                )
                HealthTile(
                    title = stringResource(R.string.diagnostics_health_battery),
                    primaryText = state.batterySoh,
                    secondaryText = if (state.isBatteryReviewNeeded) "İnceleme Gerekli" else "BMS Dengeli",
                    isHealthy = state.isBatteryGood,
                    onClick = onBatteryHealthClick,
                    onLongClick = onBatteryLongClick,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Section 2: Tools Section Title
            Text(
                text = stringResource(R.string.diagnostics_tools_section),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            // Tools 2x2 Grid (Row 1: ADB Console & Traffic Monitor)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToolCard(
                    title = stringResource(R.string.diagnostics_section_adb_console),
                    description = stringResource(R.string.diagnostics_adb_subtitle),
                    iconRes = R.drawable.ic_console,
                    onClick = onAdbClick,
                    modifier = Modifier.weight(1f),
                )
                ToolCard(
                    title = stringResource(R.string.diagnostics_section_traffic),
                    description = stringResource(R.string.diagnostics_traffic_subtitle),
                    iconRes = R.drawable.ic_traffic_monitor,
                    onClick = onTrafficClick,
                    modifier = Modifier.weight(1f),
                )
            }

            // Tools 2x2 Grid (Row 2: Camera Probe & Battery Health)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToolCard(
                    title = stringResource(R.string.diagnostics_section_camera_probe),
                    description = stringResource(R.string.diagnostics_camera_probe_subtitle),
                    iconRes = R.drawable.ic_camera_probe,
                    onClick = onCameraProbeClick,
                    modifier = Modifier.weight(1f),
                )
                ToolCard(
                    title = stringResource(R.string.diagnostics_section_battery),
                    description = stringResource(R.string.diagnostics_battery_subtitle),
                    iconRes = R.drawable.ic_battery_health,
                    onClick = onBatteryHealthClick,
                    onLongClick = onBatteryLongClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HealthTile(
    title: String,
    primaryText: String,
    secondaryText: String,
    isHealthy: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null || onLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = { onClick?.invoke() },
                            onLongClick = { onLongClick?.invoke() }
                        )
                    } else Modifier
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OverdriveStatusPill(
                    status = if (isHealthy) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.WARNING,
                    label = if (isHealthy) "Tamam" else "Uyarı",
                )
            }

            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
            )

            Text(
                text = secondaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolCard(
    title: String,
    description: String,
    iconRes: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { onLongClick?.invoke() }
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
