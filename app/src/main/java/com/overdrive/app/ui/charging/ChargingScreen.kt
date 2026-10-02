package com.overdrive.app.ui.charging

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.charging.station.EvStation
import com.overdrive.app.charging.station.EvStationRepository
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% Jetpack Compose Native Charging Screen.
 * Complete 1:1 replacement for the legacy WebView /charging page.
 */
@Composable
fun ChargingScreen(
    state: ChargingUiState,
    onTabSelected: (ChargingTab) -> Unit = {},
    onTargetSocChange: (Int) -> Unit = {},
    onCurrentLimitChange: (Int) -> Unit = {},
    onTogglePortLock: () -> Unit = {},
    onToggleBatteryPreHeat: () -> Unit = {},
    onAddSession: (ChargingSession) -> Unit = {},
    onDeleteSession: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var showAddDialog by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<ChargingSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<ChargingSession?>(null) }
    var showStationGuide by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var preselectedStation by remember { mutableStateOf<EvStation?>(null) }

    if (showStationGuide) {
        EvStationPickerDialog(
            onDismissRequest = { showStationGuide = false },
            onStationSelected = { station ->
                preselectedStation = station
                showStationGuide = false
                showAddDialog = true
            }
        )
    }

    if (showMapPicker) {
        EvStationMapPickerDialog(
            onDismissRequest = { showMapPicker = false },
            onStationSelected = { station ->
                preselectedStation = station
                showMapPicker = false
                showAddDialog = true
            }
        )
    }

    if (showAddDialog) {
        ChargeCostEditDialog(
            batteryCapacityKwh = state.batteryCapacityKwh,
            sessionToEdit = null,
            initialStation = preselectedStation,
            onDismiss = {
                showAddDialog = false
                preselectedStation = null
            },
            onSave = { session ->
                onAddSession(session)
                showAddDialog = false
                preselectedStation = null
            }
        )
    }

    sessionToEdit?.let { session ->
        ChargeCostEditDialog(
            batteryCapacityKwh = state.batteryCapacityKwh,
            sessionToEdit = session,
            onDismiss = {
                sessionToEdit = null
            },
            onSave = { updatedSession ->
                onAddSession(updatedSession)
                sessionToEdit = null
            }
        )
    }

    sessionToDelete?.let { session ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("tr-TR"))
        val dateString = dateFormat.format(Date(session.timestamp))
        OverdriveDialog(
            onDismissRequest = { sessionToDelete = null },
            title = "Şarj Seansını Sil",
            positiveButtonText = "Sil",
            onPositiveClick = {
                onDeleteSession(session.id)
                sessionToDelete = null
            },
            negativeButtonText = "Vazgeç",
            onNegativeClick = { sessionToDelete = null }
        ) {
            Text(
                text = "${session.location} (${dateString}) kaydını silmek istediğinizden emin misiniz?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    start = 10.dp,
                    end = 10.dp,
                    top = 6.dp,
                    bottom = 10.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header Bar
            ChargingHeader(
                status = state.status,
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected,
            )

            when (state.selectedTab) {
                ChargingTab.LIVE -> {
                    // Hero Card: Live Battery SoC & Charging Power
                    LiveChargingHeroCard(state = state)

                    // Target SoC Limit & AC Current Selector Card
                    ChargingLimitsCard(
                        targetSoc = state.targetSocLimit,
                        currentLimit = state.targetCurrentLimitA,
                        onTargetSocChange = onTargetSocChange,
                        onCurrentLimitChange = onCurrentLimitChange,
                    )

                    // Port Lock & Battery Thermal Health Card
                    HardwareActionsCard(
                        isPortUnlocked = state.isPortUnlocked,
                        batteryTemp = state.batteryTempCelsius,
                        isPreHeating = state.isBatteryPreHeating,
                        onTogglePortLock = onTogglePortLock,
                        onTogglePreHeat = onToggleBatteryPreHeat,
                    )
                }
                ChargingTab.SESSIONS -> {
                    // Aggregates Card with "+ Seans Ekle", "İstasyon Rehberi" and "Harita" buttons
                    SessionsSummaryCard(
                        totalSessions = state.totalSessionsCount,
                        totalEnergy = state.totalEnergyDeliveredKwh,
                        onAddClick = { showAddDialog = true },
                        onBrowseStationsClick = { showStationGuide = true },
                        onBrowseMapClick = { showMapPicker = true }
                    )

                    if (state.sessions.isEmpty()) {
                        EmptySessionsCard(onAddClick = { showAddDialog = true })
                    } else {
                        // List of Historical Sessions
                        state.sessions.forEach { session ->
                            SessionItemCard(
                                session = session,
                                onEditClick = { sessionToEdit = session },
                                onDeleteClick = { sessionToDelete = session }
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// HEADER & TAB SELECTOR
// -----------------------------------------------------------------------------
@Composable
private fun ChargingHeader(
    status: ChargingStatus,
    selectedTab: ChargingTab,
    onTabSelected: (ChargingTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (pillText, pillStatus) = when (status) {
        ChargingStatus.CHARGING -> stringResource(R.string.charging_status_charging) to OverdrivePillStatus.SUCCESS
        ChargingStatus.PLUGGED_IN -> stringResource(R.string.charging_status_plugged) to OverdrivePillStatus.INFO
        ChargingStatus.COMPLETE -> stringResource(R.string.charging_status_complete) to OverdrivePillStatus.SUCCESS
        ChargingStatus.DISCONNECTED -> stringResource(R.string.charging_status_disconnected) to OverdrivePillStatus.INFO
        ChargingStatus.FAULT -> stringResource(R.string.charging_status_fault) to OverdrivePillStatus.DANGER
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Status pill on the start side (eliminates the duplicate "Şarj" header)
        OverdriveStatusPill(label = pillText, status = pillStatus)

        // Tab Toggle Pills on the end side
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabButton(
                text = stringResource(R.string.charging_tab_live),
                isSelected = selectedTab == ChargingTab.LIVE,
                onClick = { onTabSelected(ChargingTab.LIVE) }
            )
            TabButton(
                text = stringResource(R.string.charging_tab_sessions),
                isSelected = selectedTab == ChargingTab.SESSIONS,
                onClick = { onTabSelected(ChargingTab.SESSIONS) }
            )
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "TabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "TabText"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}

// -----------------------------------------------------------------------------
// LIVE CHARGING HERO CARD
// -----------------------------------------------------------------------------
@Composable
private fun LiveChargingHeroCard(
    state: ChargingUiState,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 10.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: Battery SoC & Range
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "%${state.socPercent}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.charging_remaining_range_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${state.estimatedRangeKm} km",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.charging_capacity_fmt, state.batteryCapacityKwh),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Right: Charging Power & ETA
                Column(horizontalAlignment = Alignment.End) {
                    if (state.status == ChargingStatus.CHARGING) {
                        Text(
                            text = "${state.livePowerKw} kW",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.charging_target_eta_fmt, state.targetSocLimit, state.remainingMinutesToTarget),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.charging_energy_added_fmt, state.sessionEnergyAddedKwh),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = "0.0 kW",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.charging_no_current),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Electrical metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricTile(
                    title = stringResource(R.string.charging_metric_voltage),
                    value = "${state.liveVoltageV.toInt()} V",
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = stringResource(R.string.charging_metric_current),
                    value = String.format(Locale.US, "%.1f A", state.liveCurrentA),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = stringResource(R.string.charging_metric_current_limit),
                    value = "${state.targetCurrentLimitA} A",
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = stringResource(R.string.charging_metric_target_soc),
                    value = "%${state.targetSocLimit}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// -----------------------------------------------------------------------------
// CHARGING LIMITS & CURRENT CARD
// -----------------------------------------------------------------------------
@Composable
private fun ChargingLimitsCard(
    targetSoc: Int,
    currentLimit: Int,
    onTargetSocChange: (Int) -> Unit,
    onCurrentLimitChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 10.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.charging_limits_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.charging_recommended_80, targetSoc),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(70, 80, 90, 100).forEach { preset ->
                    val isSelected = targetSoc == preset
                    val label = if (preset == 80) "%80 (Önerilen)" else "%$preset"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { onTargetSocChange(preset) }
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Modern EV Charging Slider
            @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
            Slider(
                value = targetSoc.toFloat(),
                onValueChange = { onTargetSocChange(it.toInt()) },
                valueRange = 50f..100f,
                steps = 9,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary)
                        )
                    }
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(6.dp),
                        thumbTrackGapSize = 0.dp,
                        trackInsideCornerSize = 3.dp,
                        drawStopIndicator = null,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // AC Current Limit Chips
            Text(
                text = stringResource(R.string.charging_max_ac_current),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val limits = listOf(6, 8, 10, 13, 16, 32)
                limits.forEach { amp ->
                    val isSelected = amp == currentLimit
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        label = "AmpBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        label = "AmpText"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .clickable { onCurrentLimitChange(amp) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${amp}A",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// HARDWARE ACTIONS & THERMAL CARD
// -----------------------------------------------------------------------------
@Composable
private fun HardwareActionsCard(
    isPortUnlocked: Boolean,
    batteryTemp: Float,
    isPreHeating: Boolean,
    onTogglePortLock: () -> Unit,
    onTogglePreHeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 10.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.charging_battery_health_temp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.charging_battery_temp_status_fmt, batteryTemp.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverdriveButton(
                    text = if (isPreHeating) stringResource(R.string.charging_btn_preheat_on) else stringResource(R.string.charging_btn_preheat),
                    variant = if (isPreHeating) OverdriveButtonVariant.PRIMARY else OverdriveButtonVariant.OUTLINED,
                    onClick = onTogglePreHeat,
                )

                OverdriveButton(
                    text = if (isPortUnlocked) stringResource(R.string.charging_btn_port_unlocked) else stringResource(R.string.charging_btn_unlock_port),
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = onTogglePortLock,
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// SESSIONS SUMMARY & LIST
// -----------------------------------------------------------------------------
@Composable
private fun SessionsSummaryCard(
    totalSessions: Int,
    totalEnergy: Float,
    onAddClick: () -> Unit = {},
    onBrowseStationsClick: () -> Unit = {},
    onBrowseMapClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 10.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.charging_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.charging_history_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.charging_sessions_count_fmt, totalSessions),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.charging_total_energy_fmt, totalEnergy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OverdriveButton(
                    text = "İstasyon Rehberi",
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = onBrowseStationsClick,
                )

                OverdriveButton(
                    text = "🗺️ Harita",
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = onBrowseMapClick,
                )

                OverdriveButton(
                    text = "+ Seans Ekle",
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = onAddClick,
                )
            }
        }
    }
}

@Composable
private fun SessionItemCard(
    session: ChargingSession,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("tr-TR"))
    val dateString = dateFormat.format(Date(session.timestamp))

    val isV2L = session.chargeType.equals("V2L", ignoreCase = true)
    val isAc = session.chargeType.equals("AC", ignoreCase = true) || (!session.isDc && !isV2L)

    val badgeText = when {
        isV2L -> "V2L DEŞARJ"
        isAc -> "AC TİP-2"
        else -> "DC HIZLI"
    }
    val badgeColor = when {
        isV2L -> MaterialTheme.colorScheme.tertiary
        isAc -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 10.dp,
        onClick = onEditClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Badge, Location, Date, Edit & Delete Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    Text(
                        text = session.location,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = "Düzenle",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.8.dp
            )

            // Middle & Bottom Row: SoC, Duration, Odometer, Energy, Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "%${session.startSoc} ➔ %${session.endSoc}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${session.durationMinutes} dk",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val metaParts = mutableListOf<String>()
                    if (session.odometerKm != null && session.odometerKm > 0) {
                        metaParts.add("🚗 ${String.format(Locale.getDefault(), "%,d", session.odometerKm)} km")
                    }
                    if (session.unitPrice != null && session.unitPrice > 0f) {
                        metaParts.add("₺${String.format(Locale.getDefault(), "%.2f", session.unitPrice)}/kWh")
                    }
                    if (metaParts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = metaParts.joinToString("  •  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.charging_session_energy_fmt, session.energyKwh),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (session.totalCost != null && session.totalCost > 0) {
                        Text(
                            text = String.format(Locale.getDefault(), "₺%.2f", session.totalCost),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    } else if (session.costEstimate != null) {
                        Text(
                            text = session.costEstimate,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (session.peakPowerKw > 0) {
                        Text(
                            text = stringResource(R.string.charging_session_peak_fmt, session.peakPowerKw),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySessionsCard(
    onAddClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    OverdriveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 16.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_charging),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = "Henüz kayıtlı şarj seansı bulunmuyor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Araç şarj edildiğinde veya manuel seans eklendiğinde enerji, süre, maliyet ve kilometre kayıtları burada listelenir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OverdriveButton(
                text = "+ İlk Seansı Ekle",
                variant = OverdriveButtonVariant.PRIMARY,
                onClick = onAddClick,
            )
        }
    }
}

