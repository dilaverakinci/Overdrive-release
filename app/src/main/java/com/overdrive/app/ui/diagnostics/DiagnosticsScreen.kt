package com.overdrive.app.ui.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.overdrive.app.domain.diagnostics.BmsTelemetry
import com.overdrive.app.domain.diagnostics.EcuHealthStatus
import com.overdrive.app.domain.diagnostics.EcuTelemetrySnapshot
import com.overdrive.app.domain.diagnostics.EcuType
import com.overdrive.app.domain.diagnostics.dtc.DtcCategory
import com.overdrive.app.domain.diagnostics.dtc.DtcCode
import com.overdrive.app.domain.diagnostics.dtc.DtcSeverity
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveDimensions
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * Diagnostics Screen tabs.
 */
enum class DiagnosticsTab(val titleTr: String, val titleEn: String) {
    SYSTEM_TOOLS("Sistem ve Araçlar", "System & Tools"),
    VEHICLE_ECU("Araç ECU & Arıza Teşhisi", "Vehicle ECU & DTC")
}

/**
 * State representing system health metrics, 9-ECU live telemetry, and DTC trouble codes.
 */
data class DiagnosticsUiState(
    // System & Tools tab
    val selectedTab: DiagnosticsTab = DiagnosticsTab.SYSTEM_TOOLS,
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
    val isBydAdbActive: Boolean = false,
    val isBydAdbActivating: Boolean = false,
    val bydAdbPort: Int = 5555,

    // Vehicle ECU & DTC tab
    val overallHealth: EcuHealthStatus = EcuHealthStatus.OFFLINE,
    val ecuSnapshots: Map<EcuType, EcuTelemetrySnapshot> = emptyMap(),
    val bmsTelemetry: BmsTelemetry = BmsTelemetry(),
    val dtcCodes: List<DtcCode> = emptyList(),
    val isScanningDtc: Boolean = false,
    val isClearingDtc: Boolean = false,
    val showClearDtcConfirmDialog: Boolean = false,
    val actionFeedbackMessage: String? = null,
    val selectedEcuForDetail: EcuType? = null
)

/**
 * 100% Jetpack Compose Native Diagnostics Screen with 9-ECU Telemetry and OBD-II DTC Scanner.
 */
@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onTabSelected: (DiagnosticsTab) -> Unit = {},
    onScanDtcClick: () -> Unit = {},
    onRequestClearDtcClick: () -> Unit = {},
    onConfirmClearDtc: () -> Unit = {},
    onDismissClearDtcDialog: () -> Unit = {},
    onDismissFeedback: () -> Unit = {},
    onToggleEcuDetail: (EcuType) -> Unit = {},
    onAdbClick: () -> Unit = {},
    onEnableBydAdbClick: () -> Unit = {},
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
                    start = 8.dp,
                    end = 8.dp,
                    top = 4.dp,
                    bottom = 8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OverdriveButton(
                    text = stringResource(R.string.rail_settings),
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = onSettingsShortcutClick,
                )
            }

            // Tab Selector Pill Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DiagnosticsTab.values().forEach { tab ->
                    val isSelected = state.selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.titleTr,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Feedback Notification Banner
            AnimatedVisibility(visible = state.actionFeedbackMessage != null) {
                state.actionFeedbackMessage?.let { msg ->
                    OverdriveCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Kapat",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { onDismissFeedback() }
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            when (state.selectedTab) {
                DiagnosticsTab.SYSTEM_TOOLS -> {
                    // System Health Section
                    Text(
                        text = stringResource(R.string.diagnostics_health_section),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                    Spacer(modifier = Modifier.height(2.dp))

                    // Developer Tools Section
                    Text(
                        text = stringResource(R.string.diagnostics_tools_section),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolCard(
                            title = if (state.isBydAdbActive) "BYD ADB (Port ${state.bydAdbPort})" else "⚡ BYD ADB Aç",
                            description = if (state.isBydAdbActivating) "Açılıyor (ts-framework)..." else if (state.isBydAdbActive) "127.0.0.1:${state.bydAdbPort} Aktif ✓" else "Kablosuz ADB'yi araç içinden başlat",
                            iconRes = R.drawable.ic_services,
                            onClick = onEnableBydAdbClick,
                            modifier = Modifier.weight(1f),
                        )
                        ToolCard(
                            title = stringResource(R.string.diagnostics_section_adb_console),
                            description = stringResource(R.string.diagnostics_adb_subtitle),
                            iconRes = R.drawable.ic_console,
                            onClick = onAdbClick,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolCard(
                            title = stringResource(R.string.diagnostics_section_traffic),
                            description = stringResource(R.string.diagnostics_traffic_subtitle),
                            iconRes = R.drawable.ic_traffic_monitor,
                            onClick = onTrafficClick,
                            modifier = Modifier.weight(1f),
                        )
                        ToolCard(
                            title = stringResource(R.string.diagnostics_section_camera_probe),
                            description = stringResource(R.string.diagnostics_camera_probe_subtitle),
                            iconRes = R.drawable.ic_camera_probe,
                            onClick = onCameraProbeClick,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolCard(
                            title = stringResource(R.string.diagnostics_section_battery),
                            description = stringResource(R.string.diagnostics_battery_subtitle),
                            iconRes = R.drawable.ic_battery_health,
                            onClick = onBatteryHealthClick,
                            onLongClick = onBatteryLongClick,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                DiagnosticsTab.VEHICLE_ECU -> {
                    // Vehicle Health & BMS Battery Monitor
                    BmsBalanceSummaryCard(bms = state.bmsTelemetry, overall = state.overallHealth)

                    Spacer(modifier = Modifier.height(2.dp))

                    // 9-ECU Telemetry Status Grid
                    Text(
                        text = "CAN-Bus Elektronik Kontrol Üniteleri (9 ECU)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    EcuStatusGrid(
                        snapshots = state.ecuSnapshots,
                        selectedEcu = state.selectedEcuForDetail,
                        onEcuClick = onToggleEcuDetail
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // OBD-II DTC Scanner Section
                    DtcScannerSection(
                        dtcCodes = state.dtcCodes,
                        isScanning = state.isScanningDtc,
                        isClearing = state.isClearingDtc,
                        onScanClick = onScanDtcClick,
                        onClearClick = onRequestClearDtcClick
                    )
                }
            }
        }

        // Clear DTC Confirmation Dialog
        if (state.showClearDtcConfirmDialog) {
            OverdriveDialog(
                onDismissRequest = onDismissClearDtcDialog,
                title = "Arıza Kodları (DTC) Temizlensin mi?",
                positiveButtonText = "Evet, Temizle",
                onPositiveClick = onConfirmClearDtc,
                negativeButtonText = "İptal",
                onNegativeClick = onDismissClearDtcDialog
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tüm ECU modüllerindeki kayıtlı arıza kodları CAN-Bus üzerinden sıfırlanacaktır.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "⚠️ Güvenlik Uyarısı: Bu işlem sırasında aracın kesinlikle PARK (P) konumunda ve durur vaziyette olması zorunludur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BmsBalanceSummaryCard(
    bms: BmsTelemetry,
    overall: EcuHealthStatus,
    modifier: Modifier = Modifier
) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Genel Araç Sağlık Durumu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = when (overall) {
                            EcuHealthStatus.NORMAL -> "Tüm Sistemler Normal"
                            EcuHealthStatus.WARNING -> "Sistem Uyarısı Mevcut"
                            EcuHealthStatus.FAULT -> "Kritik Sistem Arızası"
                            EcuHealthStatus.OFFLINE -> "ECU Veri Akışı Bekleniyor"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (overall) {
                            EcuHealthStatus.NORMAL -> MaterialTheme.colorScheme.primary
                            EcuHealthStatus.WARNING -> Color(0xFFFFA000)
                            EcuHealthStatus.FAULT -> MaterialTheme.colorScheme.error
                            EcuHealthStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                OverdriveStatusPill(
                    status = when (overall) {
                        EcuHealthStatus.NORMAL -> OverdrivePillStatus.SUCCESS
                        EcuHealthStatus.WARNING -> OverdrivePillStatus.WARNING
                        EcuHealthStatus.FAULT -> OverdrivePillStatus.DANGER
                        EcuHealthStatus.OFFLINE -> OverdrivePillStatus.INFO
                    },
                    label = overall.labelTr
                )
            }

            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))

            // Battery Cell Balance Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BMS Hücre Voltaj Dengesizliği (Δ)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Δ ${bms.cellVoltageDeltaMv} mV",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            bms.cellVoltageDeltaMv < 30 -> MaterialTheme.colorScheme.primary
                            bms.cellVoltageDeltaMv < 50 -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (!bms.cellVoltageMinV.isNaN() && !bms.cellVoltageMaxV.isNaN()) {
                            "Min: ${String.format("%.2f", bms.cellVoltageMinV)}V | Max: ${String.format("%.2f", bms.cellVoltageMaxV)}V"
                        } else "Hücreler: --",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (!bms.lowVoltage12v.isNaN()) "12V Akü: ${String.format("%.1f", bms.lowVoltage12v)}V" else "12V: --",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EcuStatusGrid(
    snapshots: Map<EcuType, EcuTelemetrySnapshot>,
    selectedEcu: EcuType?,
    onEcuClick: (EcuType) -> Unit,
    modifier: Modifier = Modifier
) {
    val ecus = EcuType.values().toList()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ecus.chunked(3).forEach { rowEcus ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowEcus.forEach { ecu ->
                    val snapshot = snapshots[ecu]
                    val isExpanded = selectedEcu == ecu
                    EcuCard(
                        ecu = ecu,
                        snapshot = snapshot,
                        isExpanded = isExpanded,
                        onClick = { onEcuClick(ecu) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EcuCard(
    ecu: EcuType,
    snapshot: EcuTelemetrySnapshot?,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = snapshot?.status ?: EcuHealthStatus.OFFLINE
    OverdriveCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ecu.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OverdriveStatusPill(
                    status = when (status) {
                        EcuHealthStatus.NORMAL -> OverdrivePillStatus.SUCCESS
                        EcuHealthStatus.WARNING -> OverdrivePillStatus.WARNING
                        EcuHealthStatus.FAULT -> OverdrivePillStatus.DANGER
                        EcuHealthStatus.OFFLINE -> OverdrivePillStatus.INFO
                    },
                    label = status.labelTr
                )
            }

            Text(
                text = snapshot?.primaryMetric?.ifBlank { ecu.titleTr } ?: ecu.titleTr,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )

            Text(
                text = snapshot?.secondaryMetric?.ifBlank { "CAN Çevrimiçi" } ?: "CAN Bekliyor",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            // Expanded Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.2f))
                    snapshot?.details?.forEach { (k, v) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = v, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DtcScannerSection(
    dtcCodes: List<DtcCode>,
    isScanning: Boolean,
    isClearing: Boolean,
    onScanClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "OBD-II Arıza Kodları (DTC)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (dtcCodes.isEmpty()) "0 Aktif Arıza Kodu" else "${dtcCodes.size} Arıza Kaydı",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OverdriveButton(
                    text = if (isScanning) "Taranıyor..." else "Hataları Tara",
                    variant = OverdriveButtonVariant.OUTLINED,
                    onClick = onScanClick,
                    enabled = !isScanning && !isClearing
                )
                OverdriveButton(
                    text = if (isClearing) "Siliniyor..." else "Kodları Sil",
                    variant = OverdriveButtonVariant.PRIMARY,
                    onClick = onClearClick,
                    enabled = !isScanning && !isClearing
                )
            }
        }

        if (dtcCodes.isEmpty()) {
            OverdriveCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OverdriveTheme.colors.statusSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Aktif Arıza Kodu Bulunamadı",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tüm ECU kontrol modülleri ve aktüatör hatları hatasız çalışıyor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dtcCodes.forEach { code ->
                    DtcCodeCard(code = code)
                }
            }
        }
    }
}

@Composable
private fun DtcCodeCard(code: DtcCode, modifier: Modifier = Modifier) {
    OverdriveCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = code.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "[${code.ecu.name}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                OverdriveStatusPill(
                    status = when (code.severity) {
                        DtcSeverity.CRITICAL -> OverdrivePillStatus.DANGER
                        DtcSeverity.WARNING -> OverdrivePillStatus.WARNING
                        DtcSeverity.INFO -> OverdrivePillStatus.INFO
                    },
                    label = code.severity.labelTr
                )
            }

            Text(
                text = code.titleTr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = code.descriptionTr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    modifier = Modifier.size(20.dp),
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
