package com.overdrive.app.ui.dashboard

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions

/**
 * State container for Dashboard Remote / Web portal card.
 */
data class DashboardRemoteState(
    val isOnline: Boolean = false,
    val statusText: String = "",
    val deviceId: String = "",
    val activeUrl: String? = null,
    val deviceToken: String = "",
    val isTokenMasked: Boolean = true,
    val qrBitmap: Bitmap? = null,
    val isExpanded: Boolean = false,
)

/**
 * State container for Quick Status and Daemons.
 */
data class DashboardHeroState(
    val greeting: String = "",
    val subtitle: String = "",
    val vehicleModel: String? = null,
    val tunnelChipText: String = "",
    val isTunnelOnline: Boolean = false,
    val daemonsChipText: String = "",
    val areDaemonsRunning: Boolean = false,
    val recordingChipText: String = "",
    val isRecordingActive: Boolean = false,
)

/**
 * Complete 100% Jetpack Compose Native Dashboard Screen.
 * Fully replaces fragment_dashboard.xml preserving 1:1 layout, tokens, card padding and behavior.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    heroState: DashboardHeroState,
    remoteState: DashboardRemoteState,
    onVehicleCardClick: () -> Unit = {},
    onRecordingsClick: () -> Unit = {},
    onLiveClick: () -> Unit = {},
    onDaemonsClick: () -> Unit = {},
    onTripsClick: () -> Unit = {},
    onVehicleControlClick: () -> Unit = {},
    onToggleRemoteExpanded: () -> Unit = {},
    onToggleTokenMask: () -> Unit = {},
    onCopyToken: () -> Unit = {},
    onCopyUrl: (String) -> Unit = {},
    onRegenerateToken: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = OverdriveDimensions.pagePaddingHorizontal,
                    end = OverdriveDimensions.pagePaddingHorizontal,
                    top = OverdriveDimensions.pagePaddingTop,
                    bottom = OverdriveDimensions.pagePaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. HERO CARD
            HeroStatusCard(
                uiState = uiState,
                heroState = heroState,
                onClick = onVehicleCardClick,
            )

            // 2. CONDITIONAL CHARGING CARD
            val snapshot = (uiState.vehicle as? DashboardUiState.VehicleState.Ready)?.snapshot
            val charging = snapshot?.charging
            if (charging != null && (charging.charging || charging.plugged)) {
                ChargingStatusCard(charging = charging)
            }

            // 3. RECORDINGS & STORAGE CARD
            RecordingsAndStorageCard(
                recordingState = uiState.recordings,
                onClick = onRecordingsClick,
            )

            // 4. RECENT ACTIVITY CARD
            RecentActivityCard(activityState = uiState.activity)

            // 5. QUICK ACTIONS TILES (2x2)
            QuickActionsGrid(
                daemonsRunningText = heroState.daemonsChipText,
                onLiveClick = onLiveClick,
                onDaemonsClick = onDaemonsClick,
                onTripsClick = onTripsClick,
                onVehicleControlClick = onVehicleControlClick,
            )

            // 6. REMOTE WEB ACCESS PORTAL CARD (:8080)
            RemoteAccessCard(
                remoteState = remoteState,
                onToggleExpand = onToggleRemoteExpanded,
                onToggleTokenMask = onToggleTokenMask,
                onCopyToken = onCopyToken,
                onCopyUrl = onCopyUrl,
                onRegenerateToken = onRegenerateToken,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// HERO CARD
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroStatusCard(
    uiState: DashboardUiState,
    heroState: DashboardHeroState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vehicleSnapshot = (uiState.vehicle as? DashboardUiState.VehicleState.Ready)?.snapshot

    OverdriveCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            // Top Header: Greeting, Subtitle, Model Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = heroState.greeting.ifEmpty { stringResource(R.string.dashboard_modern_vehicle_status) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = heroState.subtitle.ifEmpty { stringResource(R.string.dashboard_modern_updating) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text(
                        text = heroState.vehicleModel ?: stringResource(R.string.dashboard_vehicle_tap_to_set),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Battery (SOC) & Range Metric Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Battery Box
                val socValue = vehicleSnapshot?.socPercent
                MetricBox(
                    label = stringResource(R.string.dashboard_modern_battery),
                    value = if (socValue != null) "${socValue.toInt()}%" else stringResource(R.string.dashboard_metric_value_pending),
                    isLoading = uiState.vehicle is DashboardUiState.VehicleState.Loading,
                    modifier = Modifier.weight(1f),
                )

                // Range Box
                val rangeValue = vehicleSnapshot?.range?.value
                val rangeUnit = vehicleSnapshot?.range?.unit?.label ?: "km"
                MetricBox(
                    label = stringResource(R.string.dashboard_modern_range),
                    value = if (rangeValue != null) "$rangeValue $rangeUnit" else stringResource(R.string.dashboard_metric_value_pending),
                    isLoading = uiState.vehicle is DashboardUiState.VehicleState.Loading,
                    modifier = Modifier.weight(1f),
                )
            }

            // SOC Progress Bar
            val socPercent = vehicleSnapshot?.socPercent?.toFloat() ?: 0f
            val progressColor = if (socPercent <= 20f && socPercent > 0f) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (socPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            // Status Chips
            Spacer(modifier = Modifier.height(14.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Tunnel Chip
                OverdriveStatusPill(
                    label = heroState.tunnelChipText.ifEmpty { stringResource(R.string.dashboard_tunnel_offline) },
                    status = if (heroState.isTunnelOnline) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO,
                )

                // Daemons Chip
                OverdriveStatusPill(
                    label = heroState.daemonsChipText.ifEmpty { stringResource(R.string.dashboard_daemons_running_default) },
                    status = if (heroState.areDaemonsRunning) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.DANGER,
                )

                // Recording Chip
                OverdriveStatusPill(
                    label = heroState.recordingChipText.ifEmpty { stringResource(R.string.dashboard_chip_recording_idle) },
                    status = if (heroState.isRecordingActive) OverdrivePillStatus.DANGER else OverdrivePillStatus.INFO,
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CHARGING CARD
// -----------------------------------------------------------------------------
@Composable
private fun ChargingStatusCard(
    charging: DashboardChargingSnapshot,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OverdriveDimensions.cardRadiusStandard),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_charging),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = charging.stateName ?: stringResource(R.string.dashboard_modern_charging),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Power (kW)
                if (charging.powerKw != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${charging.powerKw} kW",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_modern_charge_power_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Time to full (ETA)
                if (charging.timeToFullMinutes != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${charging.timeToFullMinutes} dk",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_modern_charge_eta_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                // Session kWh
                if (charging.sessionKwh != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${charging.sessionKwh} kWh",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.dashboard_modern_charge_session_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// RECORDINGS & STORAGE CARD
// -----------------------------------------------------------------------------
@Composable
private fun RecordingsAndStorageCard(
    recordingState: DashboardUiState.RecordingState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_recording),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))

                val clipCount = (recordingState as? DashboardUiState.RecordingState.Ready)?.todayClipCount
                val clipText = if (clipCount != null) {
                    "$clipCount klip kaydedildi"
                } else {
                    stringResource(R.string.dashboard_metric_value_pending)
                }

                Text(
                    text = clipText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                Icon(
                    painter = painterResource(R.drawable.ic_arrow_outward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            val storage = (recordingState as? DashboardUiState.RecordingState.Ready)?.storage
            if (storage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val usedGb = String.format("%.1f", storage.usedBytes / (1024.0 * 1024 * 1024))
                val totalGb = String.format("%.1f", storage.totalBytes / (1024.0 * 1024 * 1024))
                Text(
                    text = "$usedGb GB / $totalGb GB depolama alanı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (storage.usagePercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// RECENT ACTIVITY CARD
// -----------------------------------------------------------------------------
@Composable
private fun RecentActivityCard(
    activityState: DashboardUiState.ActivityState,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = stringResource(R.string.dashboard_modern_recent_activity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (activityState) {
                is DashboardUiState.ActivityState.Loading -> {
                    Text(
                        text = stringResource(R.string.dashboard_modern_activity_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is DashboardUiState.ActivityState.Ready -> {
                    if (activityState.rows.isEmpty()) {
                        Text(
                            text = "Yakın zamanda kayıtlı etkinlik bulunmuyor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        activityState.rows.forEachIndexed { index, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (row.icon != 0) {
                                    Icon(
                                        painter = painterResource(row.icon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Text(
                                    text = row.text.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                is DashboardUiState.ActivityState.Unavailable -> {
                    Text(
                        text = "Etkinlik verisi alınamadı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// QUICK ACTIONS GRID
// -----------------------------------------------------------------------------
@Composable
private fun QuickActionsGrid(
    daemonsRunningText: String,
    onLiveClick: () -> Unit,
    onDaemonsClick: () -> Unit,
    onTripsClick: () -> Unit,
    onVehicleControlClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = stringResource(R.string.dashboard_modern_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Live & Daemons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionTile(
                    title = stringResource(R.string.dashboard_action_live),
                    iconRes = R.drawable.ic_live,
                    onClick = onLiveClick,
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    title = stringResource(R.string.dashboard_metric_services),
                    subtitle = daemonsRunningText,
                    iconRes = R.drawable.ic_services,
                    onClick = onDaemonsClick,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Trips & Vehicle Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickActionTile(
                    title = stringResource(R.string.dashboard_action_trips),
                    iconRes = R.drawable.ic_trips,
                    onClick = onTripsClick,
                    modifier = Modifier.weight(1f),
                )
                QuickActionTile(
                    title = stringResource(R.string.dashboard_action_vehicle_control),
                    iconRes = R.drawable.ic_vehicle_control,
                    onClick = onVehicleControlClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String? = null,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(OverdriveDimensions.cardRadiusStandard))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(12.dp)
            .height(52.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// REMOTE WEB ACCESS PORTAL CARD (:8080)
// -----------------------------------------------------------------------------
@Composable
private fun RemoteAccessCard(
    remoteState: DashboardRemoteState,
    onToggleExpand: () -> Unit,
    onToggleTokenMask: () -> Unit,
    onCopyToken: () -> Unit,
    onCopyUrl: (String) -> Unit,
    onRegenerateToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        onClick = onToggleExpand,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            // Header Row (Always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(OverdriveDimensions.cardPaddingStandard),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Online Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (remoteState.isOnline) Color(0xFF00D4AA) else MaterialTheme.colorScheme.outline
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dashboard_metric_tunnel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = remoteState.statusText.ifEmpty { stringResource(R.string.dashboard_tunnel_offline) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ic_expand),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (remoteState.isExpanded) 180f else 0f),
                )
            }

            // Expanded Details
            if (remoteState.isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = OverdriveDimensions.cardPaddingStandard,
                            end = OverdriveDimensions.cardPaddingStandard,
                            bottom = OverdriveDimensions.cardPaddingStandard,
                        )
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )

                    // Device ID Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_modern_remote_details),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = remoteState.deviceId,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // QR Code Box
                    Spacer(modifier = Modifier.height(12.dp))
                    if (remoteState.qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        ) {
                            Image(
                                bitmap = remoteState.qrBitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.cd_qr_code),
                                modifier = Modifier.size(160.dp),
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.dashboard_qr_waiting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // URL Preview
                    if (!remoteState.activeUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = remoteState.activeUrl,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCopyUrl(remoteState.activeUrl) },
                        )
                    }

                    // Security Token Section
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.dashboard_access_code),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val displayToken = if (remoteState.isTokenMasked) {
                            stringResource(R.string.dashboard_token_masked)
                        } else {
                            remoteState.deviceToken
                        }

                        Text(
                            text = displayToken,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )

                        IconButton(onClick = onToggleTokenMask) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_view),
                                contentDescription = stringResource(R.string.cd_show_hide_token),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(onClick = onCopyToken) {
                            Icon(
                                painter = painterResource(R.drawable.ic_copy),
                                contentDescription = stringResource(R.string.cd_copy_token),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Regenerate Token Button
                    TextButton(
                        onClick = onRegenerateToken,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_regenerate_token),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
