package com.overdrive.app.ui.charging

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    // Aggregates Card
                    SessionsSummaryCard(
                        totalSessions = state.totalSessionsCount,
                        totalEnergy = state.totalEnergyDeliveredKwh,
                    )

                    // List of Historical Sessions
                    state.sessions.forEach { session ->
                        SessionItemCard(session = session)
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = stringResource(R.string.rail_charging),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Yüksek Voltaj Batarya ve Şarj Yönetimi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val (pillText, pillStatus) = when (status) {
                ChargingStatus.CHARGING -> "ŞARJ EDİLİYOR" to OverdrivePillStatus.SUCCESS
                ChargingStatus.PLUGGED_IN -> "FİŞ TAKILI" to OverdrivePillStatus.INFO
                ChargingStatus.COMPLETE -> "TAM DOLU" to OverdrivePillStatus.SUCCESS
                ChargingStatus.DISCONNECTED -> "BOŞTA" to OverdrivePillStatus.WARNING
                ChargingStatus.FAULT -> "HATA" to OverdrivePillStatus.DANGER
            }

            OverdriveStatusPill(label = pillText, status = pillStatus)

            // Tab Toggle Pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    text = "Canlı Şarj",
                    isSelected = selectedTab == ChargingTab.LIVE,
                    onClick = { onTabSelected(ChargingTab.LIVE) }
                )
                TabButton(
                    text = "Oturumlar",
                    isSelected = selectedTab == ChargingTab.SESSIONS,
                    onClick = { onTabSelected(ChargingTab.SESSIONS) }
                )
            }
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
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: Battery SoC & Range
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "%${state.socPercent}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Column {
                        Text(
                            text = "Kalan Tahmini Menzil",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${state.estimatedRangeKm} km",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Kapasite: ${state.batteryCapacityKwh} kWh",
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
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "%${state.targetSocLimit} hedefe ${state.remainingMinutesToTarget} dk kaldı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Eklenen: +${state.sessionEnergyAddedKwh} kWh",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = "0.0 kW",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Şarj Akımı Yok",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Electrical metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricTile(
                    title = "Voltaj",
                    value = "${state.liveVoltageV.toInt()} V",
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = "Akım",
                    value = String.format("%.1f A", state.liveCurrentA),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = "Akım Limiti",
                    value = "${state.targetCurrentLimitA} A",
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    title = "Hedef SoC",
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(10.dp),
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
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard)
        ) {
            Text(
                text = "Şarj ve Akım Sınırları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Slider: Target SoC Limit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Hedef Şarj Sınırı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "%$targetSoc (Önerilen: %80)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Slider(
                value = targetSoc.toFloat(),
                onValueChange = { onTargetSocChange(it.toInt()) },
                valueRange = 50f..100f,
                steps = 9, // 50, 55, 60, ..., 100
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AC Current Limit Chips
            Text(
                text = "Maksimum AC Şarj Akımı",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .clickable { onCurrentLimitChange(amp) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${amp}A",
                            style = MaterialTheme.typography.titleMedium,
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
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Batarya Sağlığı & Sıcaklığı",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Hücre Sıcaklığı: $batteryTemp°C · Termal Durum Normal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OverdriveButton(
                    text = if (isPreHeating) "Ön Isıtma Açık" else "Ön Isıtma",
                    variant = if (isPreHeating) OverdriveButtonVariant.PRIMARY else OverdriveButtonVariant.OUTLINED,
                    onClick = onTogglePreHeat,
                )

                OverdriveButton(
                    text = if (isPortUnlocked) "Port Açık" else "Port Kilidini Çöz",
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
    modifier: Modifier = Modifier,
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Toplam Şarj Geçmişi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Kaydedilen tüm AC ve DC şarj oturumları",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$totalSessions Oturum",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = String.format("%.1f kWh Toplam", totalEnergy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionItemCard(
    session: ChargingSession,
    modifier: Modifier = Modifier,
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.timestamp))

    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OverdriveDimensions.cardPaddingStandard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = session.location,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%${session.startSoc} ➔ %${session.endSoc} · ${session.durationMinutes} dakika",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${session.energyKwh} kWh",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Maks ${session.peakPowerKw} kW",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (session.costEstimate != null) {
                    Text(
                        text = session.costEstimate,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
