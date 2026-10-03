package com.overdrive.app.ui.dashboard

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.theme.OverdriveDimensions
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.vehicle.VehicleArt

/**
 * State container for Dashboard Remote / Web portal card.
 */
data class DashboardRemoteState(
    val isOnline: Boolean = false,
    val statusText: String = "",
    val deviceId: String = "",
    val activeUrl: String? = null,
    val localLanUrl: String? = null,
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
    val modelId: String? = null,
    val tunnelChipText: String = "",
    val isTunnelOnline: Boolean = false,
    val daemonsChipText: String = "",
    val areDaemonsRunning: Boolean = false,
    val recordingChipText: String = "",
    val isRecordingActive: Boolean = false,
)

/**
 * Complete 100% Jetpack Compose Native Dashboard Screen.
 * Fully matches the perfected native XML landscape layout (fragment_dashboard.xml) with:
 * - Full-bleed Hero Card on top (Ribbon backdrop + 3D Vehicle render + SOC/Range boxes + Gauge + 3 Status Chips)
 * - 2-Column landscape grid below Hero Card:
 *   Left column: Quick Actions (2x2) + Remote Access card
 *   Right column: Recordings & Storage + Recent Activity card
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

    // Smoothly scroll down when remote access card is expanded so its content is fully visible
    LaunchedEffect(remoteState.isExpanded) {
        if (remoteState.isExpanded) {
            delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
            delay(150)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 80.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. HERO STATUS CARD (Top Full-Width)
            HeroStatusCard(
                uiState = uiState,
                heroState = heroState,
                onClick = onVehicleCardClick,
            )

            // 2. TWO-COLUMN LANDSCAPE CONTENT GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // LEFT COLUMN (Quick Actions, Remote Access, Charging)
                Column(
                    modifier = Modifier.weight(1.08f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // CONDITIONAL CHARGING CARD
                    val snapshot = (uiState.vehicle as? DashboardUiState.VehicleState.Ready)?.snapshot
                    val charging = snapshot?.charging
                    if (charging != null && (charging.charging || charging.plugged)) {
                        ChargingStatusCard(charging = charging)
                    }

                    // QUICK ACTIONS TILES (2x2)
                    QuickActionsGrid(
                        daemonsRunningText = heroState.daemonsChipText,
                        onLiveClick = onLiveClick,
                        onDaemonsClick = onDaemonsClick,
                        onTripsClick = onTripsClick,
                        onVehicleControlClick = onVehicleControlClick,
                    )

                    // REMOTE WEB ACCESS PORTAL CARD (:8080)
                    RemoteAccessCard(
                        remoteState = remoteState,
                        onToggleExpand = onToggleRemoteExpanded,
                        onToggleTokenMask = onToggleTokenMask,
                        onCopyToken = onCopyToken,
                        onCopyUrl = onCopyUrl,
                        onRegenerateToken = onRegenerateToken,
                    )
                }

                // RIGHT COLUMN (Recordings & Storage, Recent Activity)
                Column(
                    modifier = Modifier.weight(0.92f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // RECORDINGS & STORAGE CARD
                    RecordingsAndStorageCard(
                        recordingState = uiState.recordings,
                        onClick = onRecordingsClick,
                    )

                    // RECENT ACTIVITY CARD
                    RecentActivityCard(activityState = uiState.activity)
                }
            }

            // Bottom clearance spacer for automotive dock & insets
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// HERO STATUS CARD
// -----------------------------------------------------------------------------
@Composable
private fun HeroStatusCard(
    uiState: DashboardUiState,
    heroState: DashboardHeroState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vehicleSnapshot = (uiState.vehicle as? DashboardUiState.VehicleState.Ready)?.snapshot

    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Layer 1: Ribbon backdrop (aligned to BottomEnd)
            Image(
                painter = painterResource(R.drawable.dashboard_hero_ribbon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(520.dp)
                    .height(160.dp)
                    .alpha(0.3f),
                contentScale = ContentScale.Crop,
            )

            // Layer 2: Vehicle 3D Render Art (aligned to BottomEnd with inset)
            Image(
                painter = painterResource(VehicleArt.drawableFor(heroState.modelId)),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 48.dp)
                    .heightIn(max = 145.dp)
                    .wrapContentWidth(),
                contentScale = ContentScale.Fit,
            )

            // Layer 3: Foreground Interactive Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header Row: Greeting & Subtitle on left, Model / "Tap to set" on right
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

                    // Clickable touch target to open Vehicle Model & Capacity dialog
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onClick,
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = heroState.vehicleModel ?: stringResource(R.string.dashboard_vehicle_tap_to_set),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Battery (SOC) & Range Metric Boxes (240dp fixed width to match landscape XML)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Battery Box
                    val socValue = vehicleSnapshot?.socPercent
                    MetricBox(
                        label = stringResource(R.string.dashboard_modern_battery).uppercase(),
                        value = if (socValue != null) "${socValue.toInt()}%" else stringResource(R.string.dashboard_metric_value_pending),
                        isLoading = uiState.vehicle is DashboardUiState.VehicleState.Loading,
                        modifier = Modifier.width(240.dp),
                    )

                    // Range Box
                    val rangeValue = vehicleSnapshot?.range?.value
                    val rangeUnit = vehicleSnapshot?.range?.unit?.label ?: "km"
                    MetricBox(
                        label = stringResource(R.string.dashboard_modern_range).uppercase(),
                        value = if (rangeValue != null) "$rangeValue $rangeUnit" else stringResource(R.string.dashboard_metric_value_pending),
                        isLoading = uiState.vehicle is DashboardUiState.VehicleState.Loading,
                        modifier = Modifier.width(240.dp),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Battery Gauge (LinearProgressIndicator, 492dp width to match metric boxes)
                val socPercent = vehicleSnapshot?.socPercent?.toFloat() ?: 0f
                val progressColor = if (socPercent <= 20f && socPercent > 0f) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }

                LinearProgressIndicator(
                    progress = { (socPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(492.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Chips Row (150dp width, 34dp min height each)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val colors = OverdriveTheme.colors
                    // Tunnel Chip
                    HeroStatusChip(
                        text = heroState.tunnelChipText.ifEmpty { stringResource(R.string.dashboard_tunnel_offline) },
                        containerColor = if (heroState.isTunnelOnline) colors.statusSuccessContainer else colors.statusDangerContainer,
                        textColor = if (heroState.isTunnelOnline) colors.statusSuccess else colors.statusDanger,
                    )

                    // Services Chip
                    HeroStatusChip(
                        text = heroState.daemonsChipText.ifEmpty { stringResource(R.string.dashboard_daemons_running_default) },
                        containerColor = if (heroState.areDaemonsRunning) colors.statusSuccessContainer else colors.statusDangerContainer,
                        textColor = if (heroState.areDaemonsRunning) colors.statusSuccess else colors.statusDanger,
                    )

                    // Recording Chip
                    HeroStatusChip(
                        text = heroState.recordingChipText.ifEmpty { stringResource(R.string.dashboard_chip_recording_idle) },
                        containerColor = if (heroState.isRecordingActive) colors.statusDangerContainer else colors.statusWarningContainer,
                        textColor = if (heroState.isRecordingActive) colors.statusDanger else colors.statusWarning,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatusChip(
    text: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(150.dp)
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            .heightIn(min = 78.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
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
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Power (kW)
                if (charging.powerKw != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${charging.powerKw} kW",
                            style = MaterialTheme.typography.titleMedium,
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
                            text = stringResource(R.string.format_minutes, charging.timeToFullMinutes),
                            style = MaterialTheme.typography.titleMedium,
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
                            style = MaterialTheme.typography.titleMedium,
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
        contentPadding = 12.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
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
                    stringResource(R.string.dashboard_modern_clips_today, clipCount)
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
                Spacer(modifier = Modifier.height(6.dp))
                val usedGb = String.format("%.1f", storage.usedBytes / (1024.0 * 1024 * 1024))
                val totalGb = String.format("%.1f", storage.totalBytes / (1024.0 * 1024 * 1024))
                Text(
                    text = stringResource(R.string.dashboard_modern_storage_used_total, usedGb, totalGb),
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
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.dashboard_modern_storage_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        contentPadding = 12.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.dashboard_modern_recent_activity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                            text = stringResource(R.string.dashboard_modern_no_activity),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        activityState.rows.forEach { row ->
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
                        text = stringResource(R.string.dashboard_modern_activity_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// QUICK ACTIONS GRID (2x2)
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
        contentPadding = 12.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.dashboard_modern_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(10.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
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
        modifier = modifier.fillMaxWidth(),
        contentPadding = 0.dp,
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
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        onClick = onToggleExpand,
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 10.dp),
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
                    Spacer(modifier = Modifier.height(10.dp))
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

                    if (!remoteState.localLanUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dashboard_local_lan_url, remoteState.localLanUrl),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCopyUrl(remoteState.localLanUrl) },
                        )
                    }

                    // PWA Info Note
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_pwa_install_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Security Token Section
                    Spacer(modifier = Modifier.height(12.dp))
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
                            .padding(horizontal = 12.dp, vertical = 4.dp),
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
