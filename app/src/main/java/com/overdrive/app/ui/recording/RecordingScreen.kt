package com.overdrive.app.ui.recording

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveSlider
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme
import java.util.Locale

enum class RecordingTab(val title: String, val icon: ImageVector) {
    CAPTURE("Capture", Icons.Default.Videocam),
    STATUS("Status", Icons.Default.Adjust),
    QUALITY("Quality", Icons.Default.Edit),
    OEM("Dashcam", Icons.Default.CameraAlt),
    STORAGE("Storage", Icons.Default.Storage)
}

data class RecordingUiState(
    val selectedTab: RecordingTab = RecordingTab.CAPTURE,

    // Capture tab
    val recordingMode: String = "NONE", // "NONE", "CONTINUOUS", "DRIVE_MODE", "PROXIMITY_GUARD"
    val recordingLayout: String = "standard", // "standard", "dashcam"
    val dashcamUseWindshield: Boolean = false,
    val proximityTriggerLevel: String = "RED", // "RED", "YELLOW_RED"
    val proximityPreSeconds: Int = 5,
    val proximityPostSeconds: Int = 10,
    val geocodingEnabled: Boolean = false,
    val geocodingOnline: Boolean = false,
    val geocodingCustomUrl: String = "",

    // Status tab
    val currentState: String = "Boşta",
    val isRecording: Boolean = false,
    val recordingsToday: Int = 0,
    val nativeResolution: String = "2560×1920 (4 × 1280×960)",
    val activeBitrateEstimate: String = "12 Mbps · ~5.4 GB / saat",

    // Quality tab
    val recordingQuality: String = "STANDARD", // "ECONOMY", "STANDARD", "HIGH", "PREMIUM", "MAX"
    val recordingCodec: String = "H264", // "H264", "H265"
    val targetFps: Int = 15, // 10, 15, 20, 25, 30
    val segmentDurationMinutes: Int = 2, // 2, 5, 10
    val rectifyStrength: Int = 0, // 0 - 100
    val telemetryOverlayEnabled: Boolean = true,
    val telemetryFields: Set<String> = setOf("speed", "gear", "gasPedal", "brake", "driverBelt", "passengerBelt", "turnSignals", "timestamp", "batteryPercent", "voltage12v", "lowBeam", "highBeam", "location", "vin"),
    val audioRecordingEnabled: Boolean = false,

    // OEM tab
    val oemRecordingMode: String = "off", // "off", "continuous", "smart"
    val oemTelemetryOverlay: Boolean = false,
    val oemTelemetryFields: Set<String> = setOf("speed", "timestamp", "location", "batteryPercent"),
    val oemPipelineStatus: String = "Boşta",
    val nativeDvrDisabled: Boolean = false,
    val nativeDvrInstalled: Boolean = false,
    val cameraProbeUnset: Boolean = true,

    // Storage tab
    val storageType: String = "INTERNAL", // "INTERNAL", "SD_CARD", "USB"
    val storageLimitMb: Int = 90000,
    val storageUsedText: String = "471.3 MB kullanılır",
    val storageLimitText: String = "90000 MB sınırı",
    val storageVolumeTotalText: String = "257.587 GB",
    val storageUsedPercent: Float = 0.05f,
    val sdCardAvailable: Boolean = false,
    val sdCardStatusText: String = "SD Kartı: tespit edilmedi",
    val sdCardSpaceInfo: String? = null,
    val usbAvailable: Boolean = true,
    val usbStatusText: String = "USB: Kullanılabilir",
    val usbSpaceInfo: String? = "136.9 GB ücretsiz / 137.4 GB toplam",
    val autoCleanup: Boolean = true,
    val cdrCleanupEnabled: Boolean = false,
    val cdrReservedSpaceMb: Int = 10000,
    val cdrProtectedHours: Int = 24,
    val cdrMinFilesKeep: Int = 10,

    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

fun formatStorageLimit(mb: Int): String = when {
    mb >= 1000 && mb % 1000 == 0 -> "${mb / 1000} GB"
    mb >= 1000 -> String.format(Locale.US, "%.1f GB", mb / 1000f)
    else -> "$mb MB"
}

@Composable
fun RecordingScreen(
    state: RecordingUiState,
    onTabSelected: (RecordingTab) -> Unit,
    onRecordingModeSelected: (String) -> Unit,
    onRecordingLayoutSelected: (String) -> Unit,
    onToggleDashcamWindshield: (Boolean) -> Unit,
    onProximityTriggerLevelSelected: (String) -> Unit,
    onProximityPreSecondsChange: (Int) -> Unit,
    onProximityPostSecondsChange: (Int) -> Unit,
    onToggleGeocodingEnabled: (Boolean) -> Unit,
    onToggleGeocodingOnline: (Boolean) -> Unit,
    onGeocodingCustomUrlChange: (String) -> Unit = {},
    onQualitySelected: (String) -> Unit,
    onCodecSelected: (String) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onClipDurationSelected: (Int) -> Unit,
    onRectifyStrengthChange: (Int) -> Unit,
    onToggleTelemetryOverlay: (Boolean) -> Unit,
    onToggleTelemetryField: (String, Boolean) -> Unit = { _, _ -> },
    onToggleAudioRecording: (Boolean) -> Unit = {},
    onOemRecordingModeSelected: (String) -> Unit,
    onToggleOemTelemetryOverlay: (Boolean) -> Unit,
    onToggleOemTelemetryField: (String, Boolean) -> Unit = { _, _ -> },
    onToggleNativeDvr: () -> Unit,
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanup: (Boolean) -> Unit,
    onToggleCdrCleanup: (Boolean) -> Unit = {},
    onCdrReservedSpaceChange: (Int) -> Unit = {},
    onCdrProtectedHoursChange: (Int) -> Unit = {},
    onCdrMinFilesKeepChange: (Int) -> Unit = {},
    onRefresh: () -> Unit,
    onApplyChanges: () -> Unit = {},
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showHeader) {
                // Top Header for standalone screen mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kayıt Ayarları",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OverdriveStatusPill(
                                label = if (state.isRecording) "KAYDEDİYOR" else "BOŞTA (IDLE)",
                                status = if (state.isRecording) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO
                            )
                        }
                        Text(
                            text = "Sürüş ve park video kayıt modları, çözünürlük kalitesi ve telemetri bindirme.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yenile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Scrollable Tab Content taking remaining space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (state.selectedTab) {
                    RecordingTab.CAPTURE -> CaptureTabContent(
                        state = state,
                        onRecordingModeSelected = onRecordingModeSelected,
                        onRecordingLayoutSelected = onRecordingLayoutSelected,
                        onToggleDashcamWindshield = onToggleDashcamWindshield,
                        onProximityTriggerLevelSelected = onProximityTriggerLevelSelected,
                        onProximityPreSecondsChange = onProximityPreSecondsChange,
                        onProximityPostSecondsChange = onProximityPostSecondsChange,
                        onToggleGeocodingEnabled = onToggleGeocodingEnabled,
                        onToggleGeocodingOnline = onToggleGeocodingOnline,
                        onGeocodingCustomUrlChange = onGeocodingCustomUrlChange
                    )
                    RecordingTab.STATUS -> StatusTabContent(
                        state = state,
                        onRefresh = onRefresh
                    )
                    RecordingTab.QUALITY -> QualityTabContent(
                        state = state,
                        onQualitySelected = onQualitySelected,
                        onCodecSelected = onCodecSelected,
                        onFpsSelected = onFpsSelected,
                        onClipDurationSelected = onClipDurationSelected,
                        onRectifyStrengthChange = onRectifyStrengthChange,
                        onToggleTelemetryOverlay = onToggleTelemetryOverlay,
                        onToggleTelemetryField = onToggleTelemetryField,
                        onToggleAudioRecording = onToggleAudioRecording
                    )
                    RecordingTab.OEM -> OemDashcamTabContent(
                        state = state,
                        onOemRecordingModeSelected = onOemRecordingModeSelected,
                        onToggleOemTelemetryOverlay = onToggleOemTelemetryOverlay,
                        onToggleOemTelemetryField = onToggleOemTelemetryField,
                        onToggleNativeDvr = onToggleNativeDvr
                    )
                    RecordingTab.STORAGE -> StorageTabContent(
                        state = state,
                        onStorageTypeSelected = onStorageTypeSelected,
                        onStorageLimitChange = onStorageLimitChange,
                        onToggleAutoCleanup = onToggleAutoCleanup,
                        onToggleCdrCleanup = onToggleCdrCleanup,
                        onCdrReservedSpaceChange = onCdrReservedSpaceChange,
                        onCdrProtectedHoursChange = onCdrProtectedHoursChange,
                        onCdrMinFilesKeepChange = onCdrMinFilesKeepChange
                    )
                }
            }

            // Fixed Bottom Dock Bar with tabs and "Değişiklikleri uygula" button
            RecordingBottomDockBar(
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected,
                showApplyButton = state.selectedTab != RecordingTab.STATUS,
                onApplyChanges = onApplyChanges
            )
        }
    }
}

@Composable
private fun RecordingBottomDockBar(
    selectedTab: RecordingTab,
    onTabSelected: (RecordingTab) -> Unit,
    showApplyButton: Boolean,
    onApplyChanges: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Sub-tabs (matching webview / screenshot dock layout)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecordingTab.values().forEach { tab ->
                    val isSelected = tab == selectedTab
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .width(76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else Color.Transparent
                            )
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Right: Apply Changes Button
            if (showApplyButton) {
                Button(
                    onClick = onApplyChanges,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Değişiklikleri uygula",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COLLAPSIBLE CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CollapsibleCard(
    title: String,
    icon: ImageVector,
    statusBadge: @Composable (() -> Unit)? = null,
    initiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (statusBadge != null) {
                        statusBadge()
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Daralt" else "Genişlet",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: CAPTURE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CaptureTabContent(
    state: RecordingUiState,
    onRecordingModeSelected: (String) -> Unit,
    onRecordingLayoutSelected: (String) -> Unit,
    onToggleDashcamWindshield: (Boolean) -> Unit,
    onProximityTriggerLevelSelected: (String) -> Unit,
    onProximityPreSecondsChange: (Int) -> Unit,
    onProximityPostSecondsChange: (Int) -> Unit,
    onToggleGeocodingEnabled: (Boolean) -> Unit,
    onToggleGeocodingOnline: (Boolean) -> Unit,
    onGeocodingCustomUrlChange: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Kayıt Mode (ACC ON)
        CollapsibleCard(
            title = "Kayıt Mode (ACC ON)",
            icon = Icons.Default.Videocam
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Modu Seç",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Araç kullanılırken ne zaman kayda geçeceğinizi seçin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // 2x2 Grid of Radio Cards (matching Screenshot_1790775083.png)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioOptionCard(
                        title = "Hiç (Özel olarak)",
                        subtitle = "Kayıt yok - pil ve kaynak tasarruf eder",
                        icon = Icons.Default.Block,
                        isSelected = state.recordingMode == "NONE",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("NONE") }
                    )
                    RadioOptionCard(
                        title = "Sürekli",
                        subtitle = "Kameraların aktif olduğu tüm zamanları kaydet .",
                        icon = Icons.Default.Videocam,
                        isSelected = state.recordingMode == "CONTINUOUS",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("CONTINUOUS") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioOptionCard(
                        title = "Sürücü Modu",
                        subtitle = "Sadece sürüş sırasında kaydedilmelidir (D/R/S/M ekipmanları)",
                        icon = Icons.Default.DirectionsCar,
                        isSelected = state.recordingMode == "DRIVE_MODE",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("DRIVE_MODE") }
                    )
                    RadioOptionCard(
                        title = "Yakınlık Muhafızları",
                        subtitle = "Nesnelerin arabaya yaklaştığını kaydet",
                        icon = Icons.Default.Security,
                        isSelected = state.recordingMode == "PROXIMITY_GUARD",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("PROXIMITY_GUARD") }
                    )
                }
            }
        }

        // Card 2: Kayıt Düzeni
        CollapsibleCard(
            title = "Kayıt Düzeni",
            icon = Icons.Default.GridView
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kamera Düzeni",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CANLI OLARAK UYGULANIR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Standard, dört 360 kamerayı 2×2 ızgara olarak kaydeder. Dashcam, öndeki yol görünümünü üste, 360 sol, arka ve sağ kameraları alta yerleştirir. Telemetri bindirmesi her iki durumda da korunur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // 2 Large vertical stacked buttons inside container (matching Screenshot_1790775088.png)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isStandard = state.recordingLayout == "standard"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isStandard) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onRecordingLayoutSelected("standard") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Standart (360 ızgara)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isStandard) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isStandard) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val isDashcam = state.recordingLayout == "dashcam"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDashcam) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onRecordingLayoutSelected("dashcam") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Dashcam (ön + yanlar)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isDashcam) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isDashcam) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Card 3: Yer etiketleme (matching Screenshot_1790775095.png & 1790775101.png)
        CollapsibleCard(
            title = "Yer etiketleme",
            icon = Icons.Default.Place
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Yer isimleriyle dashcam kayıtlarını etiketleyin
                SettingToggleRow(
                    title = "Yer isimleriyle dashcam kayıtlarını etiketleyin",
                    subtitle = "GPS'i geriye çevirin, kayıt başlatın, bölge/şehir etiketine koyun ve her klipin yan arabasının yanında kaydetin.",
                    checked = state.geocodingEnabled,
                    onCheckedChange = onToggleGeocodingEnabled
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Row 2: Çevrimiçi çözücü kullan
                SettingToggleRow(
                    title = "Çevrimiçi çözücü kullan",
                    subtitle = "OpenStreetMap Nominatim'e geri döner. Cihazın geokodlaması bir yerin adını veremiyor. Sadece kaydı başlatma koordinatlarını ve cihaz dilini gönderir.",
                    checked = state.geocodingOnline,
                    onCheckedChange = onToggleGeocodingOnline,
                    enabled = state.geocodingEnabled
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Row 3: Özel Nominatim URL
                SettingTextFieldRow(
                    title = "Özel Nominatim URL",
                    subtitle = "Önemli, kendi kendine konutlanmış bir örnekte bulunup, kamuoyu OSM son noktasını tamamen atlatmak için.",
                    value = state.geocodingCustomUrl,
                    onValueChange = onGeocodingCustomUrlChange,
                    placeholder = "https://nominatim.example.com",
                    enabled = state.geocodingEnabled
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: STATUS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatusTabContent(
    state: RecordingUiState,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Matching Screenshot_1790775106.png
        CollapsibleCard(
            title = "Kayıt Durumu",
            icon = Icons.Default.Adjust,
            statusBadge = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (state.isRecording) "REC" else "İDL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mevcut Durum",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.currentState,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Günümüzde Kayıtlar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.recordingsToday} →",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kamera Çözünürlüğü",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.nativeResolution,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Aktif Bit Hızı (Tahmini)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.activeBitrateEstimate,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: QUALITY & OVERLAY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QualityTabContent(
    state: RecordingUiState,
    onQualitySelected: (String) -> Unit,
    onCodecSelected: (String) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onClipDurationSelected: (Int) -> Unit,
    onRectifyStrengthChange: (Int) -> Unit,
    onToggleTelemetryOverlay: (Boolean) -> Unit,
    onToggleTelemetryField: (String, Boolean) -> Unit,
    onToggleAudioRecording: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Video Kaliteli (matching Screenshot_1790775115.png & 1790775123.png)
        CollapsibleCard(
            title = "Video Kaliteli",
            icon = Icons.Default.Videocam
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Kayıt kalitesi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Daha yüksek seviyeler daha fazla depolama maliyeti karşılığında daha fazla ayrıntı korur.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // 5 Vertical Pill Buttons in gray container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        listOf(
                            "ECONOMY" to "Ekonomik",
                            "STANDARD" to "Standart",
                            "HIGH" to "Yüksek",
                            "PREMIUM" to "Ödül",
                            "MAX" to "Max ."
                        ).forEach { (qual, label) ->
                            val isSelected = state.recordingQuality == qual
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onQualitySelected(qual) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Video Kodek
                Text(
                    text = "Video Kodek",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "H.265 % 50 daha küçük dosyalar üretir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("H264" to "H.264", "H265" to "H.265").forEach { (codec, label) ->
                            val isSelected = state.recordingCodec == codec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onCodecSelected(codec) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Kamera FPS
                Text(
                    text = "Kamera FPS",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kayıt ve izleme için çerçeve hızı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(10, 15, 20, 25, 30).forEach { fps ->
                            val isSelected = state.targetFps == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onFpsSelected(fps) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fps.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Klip Süresi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Klip Süresi",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SONRAKİ KLİP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "Kaydedilen her video segmentinin uzunluğu. Hem sürüş (ACC açık) hem de park halindeki gözetim (ACC kapalı) kayıtlarına, bir sonraki segment değişiminde uygulanır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(2 to "2 dk", 5 to "5 dk", 10 to "10 dk").forEach { (duration, label) ->
                            val isSelected = state.segmentDurationMinutes == duration
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onClipDurationSelected(duration) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Uyarı Kutusu (clip_duration_warning)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE65100).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFFE65100).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = "Daha uzun klipler, daha az ve daha büyük dosya demektir — ancak devam eden bir klip yalnızca değişim sırasında sonlandırılır. Ani güç kaybında (ör. çarpma sırasında batarya kesilmesi) mevcut klip kaybolabilir; bu nedenle 10 dakikalık ayar, 2 dakikaya kıyasla 10 dakikaya kadar kaydedilmemiş görüntü kaybı riski taşır. Aracınız aniden güç kaybedebiliyorsa 2 dakikada tutun.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bilgi Paneli: Sen ayarladıkça & Yerli (active_estimate & native_resolution)
                val bitrateMbps = when (state.recordingQuality) {
                    "ECONOMY" -> 2
                    "STANDARD" -> 4
                    "HIGH" -> 8
                    "PREMIUM" -> 10
                    "MAX" -> 12
                    else -> 4
                }
                val mbPerClip = ((bitrateMbps * 1_000_000L / 8L) * (state.segmentDurationMinutes * 60L) / (1024L * 1024L)).toInt()
                val gbPerHour = String.format(Locale.US, "%.1f", (bitrateMbps * 1_000_000.0 / 8.0 * 3600.0) / (1024.0 * 1024.0 * 1024.0))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sen ayarladıkça",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$bitrateMbps Mbps · ~$gbPerHour GB / saat · ~$mbPerClip MB / ${state.segmentDurationMinutes} dk",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.5.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Yerli",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "2560×1920 mozaik · 4 × 1280×960 kamera",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Balıkgözü Düzeltme (matching Screenshot_1790775156.png)
        CollapsibleCard(
            title = "Balıkgözü Düzeltme",
            icon = Icons.Default.Public
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Düzeltme Gücü",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kayıtlardaki ve gözetim kliplerindeki artık balıkgözü fıçı eğrisini düzeltir. 0 = kapalı (ham HAL çıkışı). Yüksek değerler çevresel pikselleri içe doğru çeker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Text(
                    text = if (state.rectifyStrength > 0) "${state.rectifyStrength}%" else "Kapalı",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OverdriveSlider(
                    value = state.rectifyStrength.toFloat(),
                    onValueChange = { onRectifyStrengthChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Kapalı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Maks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Card 3: Telemetri Üstüleme (matching Screenshot_1790775164.png)
        CollapsibleCard(
            title = "Telemetri Üstüleme",
            icon = Icons.Default.Edit
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Kayıtlı Videolar Göster",
                    subtitle = "Kaydedilen videoda örtüşme hızı, eşya, pedaller ve zaman damgası",
                    checked = state.telemetryOverlayEnabled,
                    onCheckedChange = onToggleTelemetryOverlay
                )

                if (state.telemetryOverlayEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Fields to burn in",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose what the overlay draws on trip and manual recordings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    TelemetryFieldPicker(
                        selectedFields = state.telemetryFields,
                        onToggleField = onToggleTelemetryField
                    )
                }
            }
        }

        // Card 4: Kabin Sesleri (matching Screenshot_1790775172.png)
        CollapsibleCard(
            title = "Kabin Sesleri",
            icon = Icons.Default.Mic
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Kabin Sesini Kaydet",
                    subtitle = "Kabin mikrofonunu video ile birlikte yakalar. Sadece ACC-on kayıt sırasında aktif (Sıradan, Sürücülük, Yakınlık Koruması); asla Gözlem sırasında.",
                    checked = state.audioRecordingEnabled,
                    onCheckedChange = onToggleAudioRecording
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 4: OEM DASHCAM
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OemDashcamTabContent(
    state: RecordingUiState,
    onOemRecordingModeSelected: (String) -> Unit,
    onToggleOemTelemetryOverlay: (Boolean) -> Unit,
    onToggleOemTelemetryField: (String, Boolean) -> Unit,
    onToggleNativeDvr: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: OEM Dashcam (matching Screenshot_1790775179.png)
        CollapsibleCard(
            title = "OEM Dashcam",
            icon = Icons.Default.CameraAlt,
            statusBadge = if (!state.cameraProbeUnset) {
                {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = state.oemPipelineStatus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else null
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.cameraProbeUnset) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Kamera kimliği yapılandırılmadı",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hangi AVMCamera kimliğinin ön dashcam sensörü olduğunu seçmek için Diagnostics → Camera Probe'u açın. Auto = pano XOR 1; Seal/Han'da pano id 1'dir, bu yüzden OEM dashcam varsayılan olarak id 0'dır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Kayıt davranışı",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ön dashcam sensörünün pano cam_*.mp4 dashcam klipleriyle birlikte dvr_*.mp4'yi ne zaman yazacağını kontrol eder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    // 3 Radio cards (Kapalı, Sürekli, Akıllı)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadioOptionCard(
                            title = "Kapalı",
                            subtitle = "OEM ön sensöründen kayıt yapma. Pano dashcam normal şekilde cam_*.mp4 yazmaya devam eder.",
                            icon = Icons.Default.Block,
                            isSelected = state.oemRecordingMode == "off",
                            onClick = { onOemRecordingModeSelected("off") }
                        )
                        RadioOptionCard(
                            title = "Sürekli",
                            subtitle = "Pano dashcam ne yaparsa yapsın, OverDrive çalışırken dvr_*.mp4 kaydeder.",
                            icon = Icons.Default.Videocam,
                            isSelected = state.oemRecordingMode == "continuous",
                            onClick = { onOemRecordingModeSelected("continuous") }
                        )
                        RadioOptionCard(
                            title = "Akıllı",
                            subtitle = "Pano dashcam'i yansıt: pano kayıt yaparken (Drive Mode / Continuous / Proximity Guard tetiklenir), dvr_*.mp4'yi de kaydet. Pano durunca OEM durur.",
                            icon = Icons.Default.Adjust,
                            isSelected = state.oemRecordingMode == "smart",
                            onClick = { onOemRecordingModeSelected("smart") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "OEM klipler üzerine telemetriyi göm",
                        subtitle = "Hızı, GPS'i ve zaman damgasını dvr_*.mp4 içine damgalar. Pano bindirme ayarından bağımsızdır.",
                        checked = state.oemTelemetryOverlay,
                        onCheckedChange = onToggleOemTelemetryOverlay
                    )

                    if (state.oemTelemetryOverlay) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Gömülecek alanlar",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Katmanın OEM araç kamerası kliplerinde ne çizdiğini seçin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        TelemetryFieldPicker(
                            selectedFields = state.oemTelemetryFields,
                            onToggleField = onToggleOemTelemetryField
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pipeline Status Row
                    Column {
                        Text(
                            text = state.oemPipelineStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "OEM ön sensör kaydını etkinleştirmek için yukarıdan Sürekli veya Akıllı seçin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Card 2: Native DVR app (com.byd.cdr)
        if (!state.cameraProbeUnset && state.nativeDvrInstalled) {
            CollapsibleCard(
                title = "Yerel DVR uygulaması (com.byd.cdr)",
                icon = Icons.Default.CameraAlt
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Fabrika dashcam uygulaması /sdcard/DCIM/BYDCam'e kaydeder ve her ACC ON'da AVMCamera'yı açar. Devre dışı bırakmak, OverDrive'ın kamerayı çekişme olmadan kullanmasını sağlar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.nativeDvrDisabled) "Durum: OverDrive tarafından devre dışı" else "Durum: Etkin",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.nativeDvrDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = onToggleNativeDvr,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (state.nativeDvrDisabled) "Yerel DVR'ı yeniden etkinleştir" else "Yerel DVR'yi devre dışı bırak",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 5: STORAGE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StorageTabContent(
    state: RecordingUiState,
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanup: (Boolean) -> Unit,
    onToggleCdrCleanup: (Boolean) -> Unit = {},
    onCdrReservedSpaceChange: (Int) -> Unit = {},
    onCdrProtectedHoursChange: (Int) -> Unit = {},
    onCdrMinFilesKeepChange: (Int) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Kayıtlama Depolama (matching Screenshot_1790775183.png & 1790775190.png)
        CollapsibleCard(
            title = "Kayıtlama Depolama",
            icon = Icons.Default.Storage
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Depolama yeri
                Text(
                    text = "Depolama yeri",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kayıtları nerede kaydetmek için seçin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // 3-Way Segmented Control: İçsel, SD Kartı, USB
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "INTERNAL" to "Dahili",
                            "SD_CARD" to "SD Kart",
                            "USB" to "USB Bellek"
                        ).forEach { (type, label) ->
                            val isSelected = state.storageType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { onStorageTypeSelected(type) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SD Card & USB Status Box (matching Screenshot_1790775183.png)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // SD Card status row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.sdCardAvailable) Color(0xFF2E7D32) else Color(0xFF9E9E9E))
                            )
                            Column {
                                Text(
                                    text = state.sdCardStatusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (state.sdCardAvailable && state.sdCardSpaceInfo != null) {
                                    Text(
                                        text = state.sdCardSpaceInfo,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // USB status row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.usbAvailable) Color(0xFF2E7D32) else Color(0xFF9E9E9E))
                            )
                            Column {
                                Text(
                                    text = state.usbStatusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (state.usbAvailable && state.usbSpaceInfo != null) {
                                    Text(
                                        text = state.usbSpaceInfo,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Depolama Kullanımı
                Text(
                    text = "Depolama Kullanımı",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Overdrive/recordings'e kaydedilen kayıtlar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                LinearProgressIndicator(
                    progress = { state.storageUsedPercent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.storageUsedText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${state.storageLimitMb} MB sınırı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Depolama Sınırı with Slider (matching Screenshot_1790775190.png)
                Text(
                    text = "Depolama Sınırı",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sınırın ulaştığı zaman en eskiyi otomatik olarak sil",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )

                Text(
                    text = formatStorageLimit(state.storageLimitMb),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OverdriveSlider(
                    value = state.storageLimitMb.toFloat().coerceIn(100f, 100000f),
                    onValueChange = { onStorageLimitChange(it.toInt()) },
                    valueRange = 100f..100000f,
                    steps = 0,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "100 MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.storageVolumeTotalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingToggleRow(
                    title = "Eski Klipleri Otomatik Sil (Döngüsel Kayıt)",
                    subtitle = "Depolama limiti dolduğunda kilitlenmemiş en eski klipleri silerek yer açar.",
                    checked = state.autoCleanup,
                    onCheckedChange = onToggleAutoCleanup
                )
            }
        }

        // Card 2: BYD Dashcam Auto-Cleanup Card
        CollapsibleCard(
            title = "BYD Dashcam Otomatik Temizleme",
            icon = Icons.Default.CleaningServices
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SD kart dolduğunda Overdrive için yer açmak üzere en eski BYD fabrika dashcam video dosyalarını otomatik temizler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                SettingToggleRow(
                    title = "Eski Dashcam Dosyalarını Otomatik Sil",
                    subtitle = "BYD dashcam (com.byd.cdr) klasörünü izler ve Overdrive için ayrılan alan sınırına yaklaşıldığında eski kayıtları siler.",
                    checked = state.cdrCleanupEnabled,
                    onCheckedChange = onToggleCdrCleanup
                )

                if (state.cdrCleanupEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Overdrive için Ayrılan Alan:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5000, 10000, 15000, 20000).forEach { mb ->
                            PresetBadgeButton(
                                label = "${mb / 1000} GB",
                                isSelected = state.cdrReservedSpaceMb == mb,
                                modifier = Modifier.weight(1f),
                                onClick = { onCdrReservedSpaceChange(mb) }
                            )
                        }
                    }

                    Text(
                        text = "Son Dosyaları Koru:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(12 to "12 saat", 24 to "24 saat", 48 to "48 saat", 72 to "72 saat").forEach { (h, label) ->
                            PresetBadgeButton(
                                label = label,
                                isSelected = state.cdrProtectedHours == h,
                                modifier = Modifier.weight(1f),
                                onClick = { onCdrProtectedHoursChange(h) }
                            )
                        }
                    }

                    Text(
                        text = "En Az Dosya Sayısı:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 20, 50).forEach { count ->
                            PresetBadgeButton(
                                label = count.toString(),
                                isSelected = state.cdrMinFilesKeep == count,
                                modifier = Modifier.weight(1f),
                                onClick = { onCdrMinFilesKeepChange(count) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RadioOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            )
            .border(
                1.5.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Radio Indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetBadgeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TelemetryFieldPicker(
    selectedFields: Set<String>,
    onToggleField: (String, Boolean) -> Unit
) {
    val catalog = listOf(
        "speed" to "Hız",
        "gear" to "Vites",
        "gasPedal" to "Gaz pedalı",
        "brake" to "Fren",
        "driverBelt" to "Sürücü emniyet kemeri",
        "passengerBelt" to "Yolcu emniyet kemeri",
        "turnSignals" to "Sinyaller",
        "timestamp" to "Tarih ve saat",
        "batteryPercent" to "Batarya %",
        "voltage12v" to "12V gerilim",
        "lowBeam" to "Kısa far",
        "highBeam" to "Uzun far",
        "location" to "GPS konumu",
        "vin" to "Şasi no (VIN)"
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        catalog.forEach { (id, name) ->
            val isChecked = selectedFields.contains(id)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .border(
                        1.dp,
                        if (isChecked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleField(id, !isChecked) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else 0.45f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else { _ -> },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        )
    }
}

@Composable
private fun SettingTextFieldRow(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
}
