package com.overdrive.app.ui.surveillance

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
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
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme
import java.util.Locale

enum class SurveillanceTab(val title: String, val icon: ImageVector) {
    GENERAL("General", Icons.Default.Security),
    DETECTION("Detection", Icons.Default.Visibility),
    RECORDING("Recording", Icons.Default.Videocam),
    OEM("Dashcam", Icons.Default.CameraAlt),
    STORAGE("Storage", Icons.Default.Storage)
}

data class SurveillanceUiState(
    val selectedTab: SurveillanceTab = SurveillanceTab.GENERAL,

    // General tab
    val isEnabled: Boolean = false,
    val isArmed: Boolean = false,
    val operatingMode: String = "onAndOff", // "onAndOff", "onOnly"
    val armMode: String = "lock", // "lock", "power"
    val accOffMode: String = "smart", // "smart", "continuous"
    val keepUsbPowerOnAccOff: Boolean = true,
    val mobileDataKeepAlive: Boolean = false,
    val di5CloudKeepAlive: Boolean = false,
    val diLink5KeepAlive: Boolean = false,
    val lowPowerMode: Boolean = true,
    val lowSocCutoff: Int = 20, // 0 - 30% (0 = Kapalı)
    val parkingIntelligenceEnabled: Boolean = false,
    val screenDeterrentEnabled: Boolean = false,
    val screenDeterrentDuration: Int = 10,
    val screenDeterrentMessage: String = "",
    val screenDeterrentTheme: String = "sentry1", // "sentry1", "sentry2"
    val geocodingEnabled: Boolean = false,
    val geocodingOnline: Boolean = false,
    val geocodingCustomUrl: String = "",

    // Detection tab
    val environmentPreset: String = "outdoor", // "outdoor", "garage", "street", "custom"
    val sensitivityLevel: Int = 3, // 1 - 5
    val distancePreset: Int = 3, // 1=Yakın, 3=Normal, 5=Genişletilmiş
    val loiteringTimeSeconds: Int = 3, // 1 - 10s
    val approachTriggerSeconds: Int = 0, // 0=Kapalı, 1..10s
    val shadowFilter: String = "off", // "off", "low", "medium", "high"
    val recordOnStrongMotion: Boolean = true,
    val discardEmptyMotion: Boolean = true,
    val discardEmptyNightMotion: Boolean = true,
    val cameraFront: Boolean = true,
    val cameraRight: Boolean = true,
    val cameraLeft: Boolean = true,
    val cameraRear: Boolean = true,
    val sideCamBoost: Boolean = false,
    val detectPerson: Boolean = true,
    val detectCar: Boolean = true,
    val detectBike: Boolean = true,
    val detectAnimal: Boolean = false,

    // Recording tab
    val preRecordSeconds: Int = 5, // 2 - 15s
    val postRecordSeconds: Int = 10, // 5 - 30s
    val surveillanceQuality: String = "STANDARD", // "ECONOMY", "STANDARD", "HIGH", "PREMIUM", "MAX"
    val recordingCodec: String = "H264",
    val surveillanceCameraFps: Int = 15,
    val segmentDurationMinutes: Int = 2,
    val recordingLayout: String = "standard",
    val telemetryOverlayEnabled: Boolean = false,
    val telemetryFields: Set<String> = setOf("speed", "timestamp", "location", "batteryPercent"),
    val rectifyStrength: Int = 0, // 0 - 100

    // Dashcam tab
    val oemRecordingMode: String = "off", // "off", "continuous", "smart"
    val oemTelemetryOverlay: Boolean = false,
    val oemTelemetryFields: Set<String> = setOf("speed", "timestamp", "location", "batteryPercent"),
    val oemPipelineStatus: String = "Boşta",
    val nativeDvrDisabled: Boolean = false,
    val cameraProbeUnset: Boolean = true,

    // Storage tab
    val storageType: String = "INTERNAL", // "INTERNAL", "SD_CARD", "USB"
    val storageLimitMb: Int = 500,
    val storageUsedText: String = "0 B kullanılır",
    val storageLimitText: String = "500 MB sınırı",
    val storageVolumeTotalText: String = "257.587 GB",
    val storageUsedPercent: Float = 0f,
    val sdCardAvailable: Boolean = false,
    val sdCardStatusText: String = "SD Kartı: tespit edilmedi",
    val sdCardSpaceInfo: String? = null,
    val usbAvailable: Boolean = true,
    val usbStatusText: String = "USB: Kullanılabilir",
    val usbSpaceInfo: String? = "136.9 GB ücretsiz / 137.4 GB toplam",
    val autoCleanupEvents: Boolean = true,
    val discardEmptyBrightEvents: Boolean = true,
    val cdrCleanupEnabled: Boolean = false,
    val cdrReservedSpaceMb: Int = 10000,
    val cdrProtectedHours: Int = 24,
    val cdrMinFilesKeep: Int = 10,

    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

private fun formatStorageLimitSurv(mb: Int): String = when {
    mb >= 1000 && mb % 1000 == 0 -> "${mb / 1000} GB"
    mb >= 1000 -> String.format(Locale.US, "%.1f GB", mb / 1000f)
    else -> "$mb MB"
}

@Composable
fun SurveillanceScreen(
    state: SurveillanceUiState,
    onTabSelected: (SurveillanceTab) -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onOperatingModeSelected: (String) -> Unit,
    onArmModeSelected: (String) -> Unit,
    onAccOffModeSelected: (String) -> Unit,
    onToggleKeepUsbPower: (Boolean) -> Unit,
    onToggleMobileDataKeepAlive: (Boolean) -> Unit,
    onToggleDi5CloudKeepAlive: (Boolean) -> Unit,
    onToggleDiLink5KeepAlive: (Boolean) -> Unit = {},
    onToggleLowPowerMode: (Boolean) -> Unit,
    onLowSocCutoffChange: (Int) -> Unit,
    onToggleParkingIntelligence: (Boolean) -> Unit = {},
    onToggleScreenDeterrent: (Boolean) -> Unit,
    onScreenDeterrentDurationChange: (Int) -> Unit,
    onScreenDeterrentMessageChange: (String) -> Unit,
    onScreenDeterrentThemeChange: (String) -> Unit = {},
    onToggleGeocodingEnabled: (Boolean) -> Unit,
    onToggleGeocodingOnline: (Boolean) -> Unit,
    onGeocodingCustomUrlChange: (String) -> Unit = {},
    onEnvironmentPresetSelected: (String) -> Unit,
    onToggleDetectPerson: (Boolean) -> Unit,
    onToggleDetectCar: (Boolean) -> Unit,
    onToggleDetectBike: (Boolean) -> Unit,
    onToggleDetectAnimal: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onDistanceChange: (Int) -> Unit,
    onLoiteringTimeChange: (Int) -> Unit,
    onApproachTriggerChange: (Int) -> Unit = {},
    onShadowFilterSelected: (String) -> Unit = {},
    onToggleRecordOnStrongMotion: (Boolean) -> Unit = {},
    onToggleDiscardEmptyMotion: (Boolean) -> Unit = {},
    onToggleDiscardNightMotion: (Boolean) -> Unit,
    onToggleCameraFront: (Boolean) -> Unit,
    onToggleCameraRight: (Boolean) -> Unit,
    onToggleCameraLeft: (Boolean) -> Unit,
    onToggleCameraRear: (Boolean) -> Unit,
    onToggleSideCamBoost: (Boolean) -> Unit,
    onPreRecordSecondsChange: (Int) -> Unit,
    onPostRecordSecondsChange: (Int) -> Unit,
    onQualitySelected: (String) -> Unit,
    onCodecSelected: (String) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onClipDurationSelected: (Int) -> Unit = {},
    onRecordingLayoutSelected: (String) -> Unit = {},
    onToggleTelemetryOverlay: (Boolean) -> Unit,
    onToggleTelemetryField: (String, Boolean) -> Unit = { _, _ -> },
    onRectifyStrengthChange: (Int) -> Unit = {},
    onOemRecordingModeSelected: (String) -> Unit = {},
    onToggleOemTelemetryOverlay: (Boolean) -> Unit = {},
    onToggleOemTelemetryField: (String, Boolean) -> Unit = { _, _ -> },
    onToggleNativeDvr: () -> Unit = {},
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanupEvents: (Boolean) -> Unit,
    onToggleDiscardBrightEvents: (Boolean) -> Unit,
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
                // Top Header for standalone mode
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
                                text = "Gözetim Ayarları",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OverdriveStatusPill(
                                label = if (state.isArmed) "DEVREDE (ARMED)" else if (state.isEnabled) "ETKİN" else "KAPALI",
                                status = if (state.isArmed) OverdrivePillStatus.SUCCESS else if (state.isEnabled) OverdrivePillStatus.INFO else OverdrivePillStatus.INFO
                            )
                        }
                        Text(
                            text = "Park halinde nöbetçi koruması, yapay zeka tehdit algılama ve kamera yönetimi.",
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

            // Scrollable Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (state.selectedTab) {
                    SurveillanceTab.GENERAL -> GeneralTabContent(
                        state = state,
                        onToggleMaster = onToggleMaster,
                        onOperatingModeSelected = onOperatingModeSelected,
                        onArmModeSelected = onArmModeSelected,
                        onAccOffModeSelected = onAccOffModeSelected,
                        onToggleKeepUsbPower = onToggleKeepUsbPower,
                        onToggleMobileDataKeepAlive = onToggleMobileDataKeepAlive,
                        onToggleDi5CloudKeepAlive = onToggleDi5CloudKeepAlive,
                        onToggleDiLink5KeepAlive = onToggleDiLink5KeepAlive,
                        onToggleLowPowerMode = onToggleLowPowerMode,
                        onLowSocCutoffChange = onLowSocCutoffChange,
                        onToggleParkingIntelligence = onToggleParkingIntelligence,
                        onToggleScreenDeterrent = onToggleScreenDeterrent,
                        onScreenDeterrentDurationChange = onScreenDeterrentDurationChange,
                        onScreenDeterrentMessageChange = onScreenDeterrentMessageChange,
                        onScreenDeterrentThemeChange = onScreenDeterrentThemeChange,
                        onToggleGeocodingEnabled = onToggleGeocodingEnabled,
                        onToggleGeocodingOnline = onToggleGeocodingOnline,
                        onGeocodingCustomUrlChange = onGeocodingCustomUrlChange
                    )
                    SurveillanceTab.DETECTION -> DetectionTabContent(
                        state = state,
                        onEnvironmentPresetSelected = onEnvironmentPresetSelected,
                        onToggleDetectPerson = onToggleDetectPerson,
                        onToggleDetectCar = onToggleDetectCar,
                        onToggleDetectBike = onToggleDetectBike,
                        onToggleDetectAnimal = onToggleDetectAnimal,
                        onSensitivityChange = onSensitivityChange,
                        onDistanceChange = onDistanceChange,
                        onLoiteringTimeChange = onLoiteringTimeChange,
                        onApproachTriggerChange = onApproachTriggerChange,
                        onShadowFilterSelected = onShadowFilterSelected,
                        onToggleRecordOnStrongMotion = onToggleRecordOnStrongMotion,
                        onToggleDiscardEmptyMotion = onToggleDiscardEmptyMotion,
                        onToggleDiscardNightMotion = onToggleDiscardNightMotion,
                        onToggleCameraFront = onToggleCameraFront,
                        onToggleCameraRight = onToggleCameraRight,
                        onToggleCameraLeft = onToggleCameraLeft,
                        onToggleCameraRear = onToggleCameraRear,
                        onToggleSideCamBoost = onToggleSideCamBoost
                    )
                    SurveillanceTab.RECORDING -> RecordingTabContent(
                        state = state,
                        onPreRecordSecondsChange = onPreRecordSecondsChange,
                        onPostRecordSecondsChange = onPostRecordSecondsChange,
                        onQualitySelected = onQualitySelected,
                        onCodecSelected = onCodecSelected,
                        onFpsSelected = onFpsSelected,
                        onClipDurationSelected = onClipDurationSelected,
                        onRecordingLayoutSelected = onRecordingLayoutSelected,
                        onToggleTelemetryOverlay = onToggleTelemetryOverlay,
                        onToggleTelemetryField = onToggleTelemetryField,
                        onRectifyStrengthChange = onRectifyStrengthChange
                    )
                    SurveillanceTab.OEM -> DashcamTabContent(
                        state = state,
                        onOemRecordingModeSelected = onOemRecordingModeSelected,
                        onToggleOemTelemetryOverlay = onToggleOemTelemetryOverlay,
                        onToggleOemTelemetryField = onToggleOemTelemetryField,
                        onToggleNativeDvr = onToggleNativeDvr
                    )
                    SurveillanceTab.STORAGE -> StorageTabContent(
                        state = state,
                        onStorageTypeSelected = onStorageTypeSelected,
                        onStorageLimitChange = onStorageLimitChange,
                        onToggleAutoCleanupEvents = onToggleAutoCleanupEvents,
                        onToggleDiscardBrightEvents = onToggleDiscardBrightEvents,
                        onToggleCdrCleanup = onToggleCdrCleanup,
                        onCdrReservedSpaceChange = onCdrReservedSpaceChange,
                        onCdrProtectedHoursChange = onCdrProtectedHoursChange,
                        onCdrMinFilesKeepChange = onCdrMinFilesKeepChange
                    )
                }
            }

            // Fixed Bottom Dock Bar with tabs and "Değişiklikleri uygula" button
            SurveillanceBottomDockBar(
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected,
                onApplyChanges = onApplyChanges
            )
        }
    }
}

@Composable
private fun SurveillanceBottomDockBar(
    selectedTab: SurveillanceTab,
    onTabSelected: (SurveillanceTab) -> Unit,
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
            // Left: Sub-tabs (General, Detection, Recording, Dashcam, Storage)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SurveillanceTab.values().forEach { tab ->
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

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE COLLAPSIBLE CARD & INFO NOTE BOX
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

@Composable
private fun InfoNoteBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: GENERAL (matching Screenshot_1790775212.png)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GeneralTabContent(
    state: SurveillanceUiState,
    onToggleMaster: (Boolean) -> Unit,
    onOperatingModeSelected: (String) -> Unit,
    onArmModeSelected: (String) -> Unit,
    onAccOffModeSelected: (String) -> Unit,
    onToggleKeepUsbPower: (Boolean) -> Unit,
    onToggleMobileDataKeepAlive: (Boolean) -> Unit,
    onToggleDi5CloudKeepAlive: (Boolean) -> Unit,
    onToggleDiLink5KeepAlive: (Boolean) -> Unit,
    onToggleLowPowerMode: (Boolean) -> Unit,
    onLowSocCutoffChange: (Int) -> Unit,
    onToggleParkingIntelligence: (Boolean) -> Unit,
    onToggleScreenDeterrent: (Boolean) -> Unit,
    onScreenDeterrentDurationChange: (Int) -> Unit,
    onScreenDeterrentMessageChange: (String) -> Unit,
    onScreenDeterrentThemeChange: (String) -> Unit,
    onToggleGeocodingEnabled: (Boolean) -> Unit,
    onToggleGeocodingOnline: (Boolean) -> Unit,
    onGeocodingCustomUrlChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Denetim Modu (matching Screenshot_1790775212.png)
        CollapsibleCard(
            title = "Denetim Modu",
            icon = Icons.Default.Security,
            statusBadge = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (state.isEnabled) "● ETKİN" else "○ KAPATILMIŞ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Çalışma Modu
                Text(
                    text = "Çalışma Modu",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Araç kapatıldıktan sonra Overdrive'ın çalışmaya devam edip etmeyeceği. Yalnızca Açık, park ettiğinizde Overdrive'ı tamamen kapatır; sürüş sırasındaki özellikler (dashcam, yolculuklar, şarj) bundan etkilenmez.",
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
                        listOf("onAndOff" to "Açık & Kapalı", "onOnly" to "Yalnızca Açık").forEach { (mode, label) ->
                            val isSelected = state.operatingMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onOperatingModeSelected(mode) },
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

                Spacer(modifier = Modifier.height(8.dp))
                InfoNoteBox(
                    text = "Overdrive park halindeyken izlemeye devam eder — gözetim ve arka plan izleme etkin kalır. Gece boyunca biraz batarya kullanır."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gözetimi Sağlayın
                SettingToggleRow(
                    title = "Gözetimi Sağlayın",
                    subtitle = "Hareket ve nesnelerin tespit edilmesi için izleme kameraları",
                    checked = state.isEnabled,
                    onCheckedChange = onToggleMaster
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Devreye Alma Modu
                Text(
                    text = "Devreye Alma Modu",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gözetimin araç kilitlendiğinde mi yoksa motor durduğunda mı devreye gireceğini belirleyin.",
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
                        listOf("lock" to "Kilitlenince", "power" to "Kapanınca").forEach { (arm, label) ->
                            val isSelected = state.armMode == arm
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onArmModeSelected(arm) },
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

                // ACC-OFF Modu
                Text(
                    text = "ACC-OFF Modu",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Park halinde sürekli kayıt mı yoksa hareket algılandığında akıllı kayıt mı yapılacağı.",
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
                        listOf("smart" to "Akıllı", "continuous" to "Sürekli").forEach { (accOff, label) ->
                            val isSelected = state.accOffMode == accOff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onAccOffModeSelected(accOff) },
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Switches
                SettingToggleRow(
                    title = "USB Güç Koruma (Keep USB Power)",
                    subtitle = "Park halindeyken USB portlarının enerjisini açık tutarak harici depolama ve kameraların kapanmasını engeller.",
                    checked = state.keepUsbPowerOnAccOff,
                    onCheckedChange = onToggleKeepUsbPower
                )

                SettingToggleRow(
                    title = "Mobil Veri Canlı Tutma",
                    subtitle = "Park halindeyken mobil ağ bağlantısını aktif tutarak uzaktan bildirim ve erişim sağlar.",
                    checked = state.mobileDataKeepAlive,
                    onCheckedChange = onToggleMobileDataKeepAlive
                )

                SettingToggleRow(
                    title = "Di5 Bulut Canlı Tutma",
                    subtitle = "DiLink 5 bulut servislerinin arka planda uyutulmasını önler.",
                    checked = state.di5CloudKeepAlive,
                    onCheckedChange = onToggleDi5CloudKeepAlive
                )

                SettingToggleRow(
                    title = "Düşük Güç Modu",
                    subtitle = "Park halindeyken CPU frekansını düşürerek enerji tüketimini optimize eder.",
                    checked = state.lowPowerMode,
                    onCheckedChange = onToggleLowPowerMode
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Düşük batarya kesme
                Text(
                    text = "Düşük batarya kesme",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ana batarya bu seviyeye düştüğünde gözetimi durdurarak aracın uykuya geçmesine izin verir ve aşırı deşarja karşı korur. Batarya seviyesinde hiç durmamak için Kapalı olarak ayarlayın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )

                Text(
                    text = if (state.lowSocCutoff > 0) "${state.lowSocCutoff}%" else "Kapalı",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Slider(
                    value = state.lowSocCutoff.toFloat(),
                    onValueChange = { onLowSocCutoffChange(it.toInt()) },
                    valueRange = 0f..30f,
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
                    Text(text = "30%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Card 2: Parking Intelligence
        CollapsibleCard(
            title = "Parking Intelligence",
            icon = Icons.Default.LocalParking
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Akıllı Park Analitiği",
                    subtitle = "Konum geçmişi ve güvenli park bölgelerini öğrenerek tanıdık ev/iş otoparklarında gözetim hassasiyetini otomatik optimize eder.",
                    checked = state.parkingIntelligenceEnabled,
                    onCheckedChange = onToggleParkingIntelligence
                )
            }
        }

        // Card 3: Ekran Deterjantı (Screen Deterrent)
        CollapsibleCard(
            title = "Ekran Deterjantı",
            icon = Icons.Default.Tv
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Nöbetçi Ekranı Caydırıcı Görünüm",
                    subtitle = "Yaklaşan bir şüpheli algılandığında araç içi orta ekranı uyandırarak Sentry gözü animasyonu ve uyarı metni gösterir.",
                    checked = state.screenDeterrentEnabled,
                    onCheckedChange = onToggleScreenDeterrent
                )

                if (state.screenDeterrentEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ekran Açık Kalma Süresi: ${state.screenDeterrentDuration}s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = state.screenDeterrentDuration.toFloat(),
                        onValueChange = { onScreenDeterrentDurationChange(it.toInt()) },
                        valueRange = 5f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Caydırıcı Tema:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("sentry1" to "Sentry 1 (Kırmızı)", "sentry2" to "Sentry 2 (Göz)").forEach { (th, label) ->
                            val isSelected = state.screenDeterrentTheme == th
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { onScreenDeterrentThemeChange(th) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    SettingTextFieldRow(
                        title = "Özel Uyarı Mesajı",
                        subtitle = "Ekranda görüntülenecek caydırıcı mesaj.",
                        value = state.screenDeterrentMessage,
                        onValueChange = onScreenDeterrentMessageChange,
                        placeholder = "Kayıt devrede. Güvenlik için lütfen araçtan uzaklaşın."
                    )
                }
            }
        }

        // Card 4: Yer etiketleme
        CollapsibleCard(
            title = "Yer etiketleme",
            icon = Icons.Default.Place
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Yer isimleriyle gözetim olaylarını etiketleyin",
                    subtitle = "Olay başlangıcında GPS koordinatlarını bölge/sokak adına dönüştürerek bildirimlerde ve dosya kayıtlarında gösterir.",
                    checked = state.geocodingEnabled,
                    onCheckedChange = onToggleGeocodingEnabled
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingToggleRow(
                    title = "Çevrimiçi çözücü kullan",
                    subtitle = "OpenStreetMap Nominatim'e geri döner. Sadece kaydı başlatma koordinatlarını gönderir.",
                    checked = state.geocodingOnline,
                    onCheckedChange = onToggleGeocodingOnline,
                    enabled = state.geocodingEnabled
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

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
// TAB 2: DETECTION (matching Screenshot_1790775384.png)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectionTabContent(
    state: SurveillanceUiState,
    onEnvironmentPresetSelected: (String) -> Unit,
    onToggleDetectPerson: (Boolean) -> Unit,
    onToggleDetectCar: (Boolean) -> Unit,
    onToggleDetectBike: (Boolean) -> Unit,
    onToggleDetectAnimal: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onDistanceChange: (Int) -> Unit,
    onLoiteringTimeChange: (Int) -> Unit,
    onApproachTriggerChange: (Int) -> Unit,
    onShadowFilterSelected: (String) -> Unit,
    onToggleRecordOnStrongMotion: (Boolean) -> Unit,
    onToggleDiscardEmptyMotion: (Boolean) -> Unit,
    onToggleDiscardNightMotion: (Boolean) -> Unit,
    onToggleCameraFront: (Boolean) -> Unit,
    onToggleCameraRight: (Boolean) -> Unit,
    onToggleCameraLeft: (Boolean) -> Unit,
    onToggleCameraRear: (Boolean) -> Unit,
    onToggleSideCamBoost: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Algılama Ayarları (matching Screenshot_1790775384.png)
        CollapsibleCard(
            title = "Algılama Ayarları",
            icon = Icons.Default.Visibility
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Çevre önceden ayarlandı
                Text(
                    text = "Çevre önceden ayarlandı",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Otopark durumunuz için hızlı bir ön ayar",
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
                        listOf(
                            "outdoor" to "Dışarıda",
                            "garage" to "Garaj",
                            "street" to "Sokak",
                            "custom" to "Geleneksel"
                        ).forEach { (env, label) ->
                            val isSelected = state.environmentPreset == env
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onEnvironmentPresetSelected(env) },
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

                Spacer(modifier = Modifier.height(8.dp))
                InfoNoteBox(
                    text = "Açık otoparklar ve giriş yolları için standart filtreleme. Güneş, bulutlar ve ışıklar ile ilgileniyor."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Duyarlılık
                Text(
                    text = "Duyarlılık",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hareketin ne kadar kolay algılanması",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )

                val sensLabel = when (state.sensitivityLevel) {
                    1 -> "1 Çok Düşük"
                    2 -> "2 Düşük"
                    3 -> "3 Öntanımlı"
                    4 -> "4 Yüksek"
                    else -> "5 Çok Yüksek"
                }

                Text(
                    text = sensLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Slider(
                    value = state.sensitivityLevel.toFloat(),
                    onValueChange = { onSensitivityChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                InfoNoteBox(
                    text = "Dengeli yaklaşık 3 metrelik bir mesafede yaklaşan bir kişiyi algılar."
                )

                Spacer(modifier = Modifier.height(14.dp))

                // İzleme Bölgesi
                Text(
                    text = "İzleme Bölgesi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Araba hareketi ne kadar uzakta uyarı tetikler",
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
                        listOf(1 to "Yakın", 3 to "Normal", 5 to "Genişletilmiş").forEach { (dist, label) ->
                            val isSelected = state.distancePreset == dist
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onDistanceChange(dist) },
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

                // Ulaşma Zamanı
                Text(
                    text = "Ulaşma Zamanı (Oyalanma)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Şüphelinin araç çevresinde kaç saniye duraklaması gerektiği.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Text(
                    text = "${state.loiteringTimeSeconds}s",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = state.loiteringTimeSeconds.toFloat(),
                    onValueChange = { onLoiteringTimeChange(it.toInt()) },
                    valueRange = 1f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Yaklaşma Tetikleyicisi
                Text(
                    text = "Yaklaşma Tetikleyicisi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kamera yapay zekâsı gerçekten yaklaşan bir kişiyi veya aracı doğruladığında daha erken kayda başla.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Text(
                    text = if (state.approachTriggerSeconds > 0) "${state.approachTriggerSeconds}s" else "Kapalı",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = state.approachTriggerSeconds.toFloat(),
                    onValueChange = { onApproachTriggerChange(it.toInt()) },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gölge Filtri
                Text(
                    text = "Gölge Filtri",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Rüzgarda sallanan ağaç gölgeleri ve güneş ışığı kaymalarını yapay zekayla eler.",
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
                        listOf("off" to "Kapalı", "low" to "Düşük", "medium" to "Orta", "high" to "Yüksek").forEach { (flt, label) ->
                            val isSelected = state.shadowFilter == flt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { onShadowFilterSelected(flt) },
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                SettingToggleRow(
                    title = "Güçlü Harekette Kaydet (Strong Motion)",
                    subtitle = "Yapay zeka nesnesi tanımasa dahi ani büyük hareketlerde acil kayıt başlatır.",
                    checked = state.recordOnStrongMotion,
                    onCheckedChange = onToggleRecordOnStrongMotion
                )

                SettingToggleRow(
                    title = "Gerçek Nesne İçermeyen Kayıtları Sil",
                    subtitle = "Olay sonunda incelenen videoda kişi/araç/hayvan tespit edilemezse depolamayı korumak için kaydı otomatik atar.",
                    checked = state.discardEmptyMotion,
                    onCheckedChange = onToggleDiscardEmptyMotion
                )

                SettingToggleRow(
                    title = "Geceleyin de Sil",
                    subtitle = "Gece kızılötesi ve düşük ışıkta boş hareket tetiklemelerini de filtreler.",
                    checked = state.discardEmptyNightMotion,
                    onCheckedChange = onToggleDiscardNightMotion
                )
            }
        }

        // Card 2: Kamera Kontrolleri
        CollapsibleCard(
            title = "Kamera Kontrolleri",
            icon = Icons.Default.CameraAlt
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Gözetim sırasında hangi kameraların aktif olarak izleme yapacağını seçin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 2x2 Camera Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CameraToggleTile(
                        title = "Ön Kamera",
                        checked = state.cameraFront,
                        onCheckedChange = onToggleCameraFront,
                        modifier = Modifier.weight(1f)
                    )
                    CameraToggleTile(
                        title = "Sağ Kamera",
                        checked = state.cameraRight,
                        onCheckedChange = onToggleCameraRight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CameraToggleTile(
                        title = "Sol Kamera",
                        checked = state.cameraLeft,
                        onCheckedChange = onToggleCameraLeft,
                        modifier = Modifier.weight(1f)
                    )
                    CameraToggleTile(
                        title = "Arka Kamera",
                        checked = state.cameraRear,
                        onCheckedChange = onToggleCameraRear,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleRow(
                    title = "Yan Kamera Güçlendirmesi",
                    subtitle = "Yan aynalardaki kameraların düşük ışık kazancını artırarak park halinde kör noktaları daha net aydınlatır.",
                    checked = state.sideCamBoost,
                    onCheckedChange = onToggleSideCamBoost
                )
            }
        }

        // Card 3: Nesneleri Tespit Etmek
        CollapsibleCard(
            title = "Nesneleri Tespit Etmek",
            icon = Icons.Default.Adjust
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Yapay zekanın nöbetçi kaydı tetiklemesini istediğiniz aktör türleri.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipButton(label = "Kişi", isSelected = state.detectPerson, onClick = { onToggleDetectPerson(!state.detectPerson) })
                    FilterChipButton(label = "Araba", isSelected = state.detectCar, onClick = { onToggleDetectCar(!state.detectCar) })
                    FilterChipButton(label = "Bisiklet", isSelected = state.detectBike, onClick = { onToggleDetectBike(!state.detectBike) })
                    FilterChipButton(label = "Hayvan", isSelected = state.detectAnimal, onClick = { onToggleDetectAnimal(!state.detectAnimal) })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: RECORDING & QUALITY (matching Screenshot_1790775449.png)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecordingTabContent(
    state: SurveillanceUiState,
    onPreRecordSecondsChange: (Int) -> Unit,
    onPostRecordSecondsChange: (Int) -> Unit,
    onQualitySelected: (String) -> Unit,
    onCodecSelected: (String) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onClipDurationSelected: (Int) -> Unit,
    onRecordingLayoutSelected: (String) -> Unit,
    onToggleTelemetryOverlay: (Boolean) -> Unit,
    onToggleTelemetryField: (String, Boolean) -> Unit,
    onRectifyStrengthChange: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Kayıt Ayarları (matching Screenshot_1790775449.png)
        CollapsibleCard(
            title = "Kayıt Ayarları",
            icon = Icons.Default.Videocam
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Etkinlik öncesi tampon
                Text(
                    text = "Etkinlik öncesi tampon",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hareket etkinliğinden önceki saniyeler",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Text(
                    text = "${state.preRecordSeconds}s",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = state.preRecordSeconds.toFloat(),
                    onValueChange = { onPreRecordSecondsChange(it.toInt()) },
                    valueRange = 2f..15f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Etkinlik sonrası tampon
                Text(
                    text = "Etkinlik sonrası tampon",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hareket durduktan sonra alınan saniyeler",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
                Text(
                    text = "${state.postRecordSeconds}s",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = state.postRecordSeconds.toFloat(),
                    onValueChange = { onPostRecordSecondsChange(it.toInt()) },
                    valueRange = 5f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gözetim Kalitesi
                Text(
                    text = "Gözetim Kalitesi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Park halinde (ACC kapalı) gözetim kaydı için Quality — sürüş sırasındaki araç kamerası kalitesinden bağımsızdır. Daha yüksek kademeler daha fazla ayrıntı korur ancak daha fazla depolama alanı kullanır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

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
                            val isSelected = state.surveillanceQuality == qual
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
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

                // Gözetim FPS
                Text(
                    text = "Gözetim FPS",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(10, 15, 20, 25, 30).forEach { fps ->
                            val isSelected = state.surveillanceCameraFps == fps
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
            }
        }

        // Card 2: Kayıt Düzeni
        CollapsibleCard(
            title = "Kayıt Düzeni",
            icon = Icons.Default.GridView
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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

        // Card 3: Telemetri Üstüleme
        CollapsibleCard(
            title = "Telemetri Üstüleme",
            icon = Icons.Default.Edit
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = "Olay Kliplerinde Telemetriyi Damgala",
                    subtitle = "Tarih, saat, batarya yüzdesi ve GPS konumunu doğrudan videoya yerleştirir.",
                    checked = state.telemetryOverlayEnabled,
                    onCheckedChange = onToggleTelemetryOverlay
                )
            }
        }

        // Card 4: Balıkgözü Düzeltme
        CollapsibleCard(
            title = "Balıkgözü Düzeltme",
            icon = Icons.Default.Public
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Düzeltme Gücü: ${state.rectifyStrength}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
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
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 4: OEM DASHCAM (matching Screenshot_1790775524.png)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashcamTabContent(
    state: SurveillanceUiState,
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
        // Card 1: OEM Dashcam
        CollapsibleCard(
            title = "OEM Dashcam",
            icon = Icons.Default.CameraAlt,
            statusBadge = {
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
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.cameraProbeUnset) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
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
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Text(
                    text = "Kayıt davranışı",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gözetim anında OEM ön dashcam sensörünün nasıl davranacağını belirleyin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioOptionCard(
                        title = "Kapalı",
                        subtitle = "Gözetim esnasında OEM sensöründen kayıt yapılmaz.",
                        icon = Icons.Default.Block,
                        isSelected = state.oemRecordingMode == "off",
                        onClick = { onOemRecordingModeSelected("off") }
                    )
                    RadioOptionCard(
                        title = "Sürekli",
                        subtitle = "Gözetim devredeyken ön sensörden kesintisiz kayıt yapılır.",
                        icon = Icons.Default.Videocam,
                        isSelected = state.oemRecordingMode == "continuous",
                        onClick = { onOemRecordingModeSelected("continuous") }
                    )
                    RadioOptionCard(
                        title = "Akıllı",
                        subtitle = "Yapay zeka veya hareket tetiklendiğinde OEM sensör kaydı devreye girer.",
                        icon = Icons.Default.Adjust,
                        isSelected = state.oemRecordingMode == "smart",
                        onClick = { onOemRecordingModeSelected("smart") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingToggleRow(
                    title = "OEM kliplerinde telemetriyi yerleştir",
                    subtitle = "Hızı, GPS'i ve zaman damgasını dvr_*.mp4 içine damgalar.",
                    checked = state.oemTelemetryOverlay,
                    onCheckedChange = onToggleOemTelemetryOverlay
                )
            }
        }

        // Card 2: Yerel DVR uygulaması
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
                            containerColor = if (state.nativeDvrDisabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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

// ─────────────────────────────────────────────────────────────────────────────
// TAB 5: STORAGE (matching Screenshot_1790775527.png & 1790775532.png)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StorageTabContent(
    state: SurveillanceUiState,
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanupEvents: (Boolean) -> Unit,
    onToggleDiscardBrightEvents: (Boolean) -> Unit,
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
        // Card 1: Denetim Kaynaklama (matching Screenshot_1790775527.png)
        CollapsibleCard(
            title = "Denetim Kaynaklama",
            icon = Icons.Default.Storage
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Depolama yeri",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gözlem olaylarını nerede kaydetmek için seçin",
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
                            "INTERNAL" to "İçsel",
                            "SD_CARD" to "SD Kartı",
                            "USB" to "USB"
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

                // SD Card & USB Status Box (matching Screenshot_1790775527.png)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    text = "Overdrive/surveillance'e kaydedilen olaylar",
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

                // Depolama Sınırı with Slider (matching Screenshot_1790775532.png)
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
                    text = formatStorageLimitSurv(state.storageLimitMb),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Slider(
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
                    title = "Eski Olayları Otomatik Sil",
                    subtitle = "Ayrılan depolama sınırına ulaşıldığında en eski gözetim kliplerini temizler.",
                    checked = state.autoCleanupEvents,
                    onCheckedChange = onToggleAutoCleanupEvents
                )

                SettingToggleRow(
                    title = "Flaş ve Parlama Olaylarını Kaydetme",
                    subtitle = "Ani ışık patlaması veya şimşek kaynaklı boş hareket tetiklemelerini kaydetmez.",
                    checked = state.discardEmptyBrightEvents,
                    onCheckedChange = onToggleDiscardBrightEvents
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
private fun CameraToggleTile(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
