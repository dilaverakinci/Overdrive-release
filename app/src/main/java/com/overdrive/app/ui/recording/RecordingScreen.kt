package com.overdrive.app.ui.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme

enum class RecordingTab(val title: String, val icon: ImageVector) {
    CAPTURE("Kayıt Modu", Icons.Default.Videocam),
    STATUS("Durum", Icons.Default.Info),
    QUALITY("Kalite & Bindirme", Icons.Default.HighQuality),
    OEM("OEM Dashcam", Icons.Default.CameraAlt),
    STORAGE("Depolama", Icons.Default.Settings)
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

    // Status tab
    val currentState: String = "Boşta (Idle)",
    val isRecording: Boolean = false,
    val recordingsToday: Int = 0,
    val nativeResolution: String = "2560×1920 (4 × 1280×960)",
    val activeBitrateEstimate: String = "12 Mbps · ~5.4 GB / saat",

    // Quality tab
    val recordingQuality: String = "HIGH", // "ECONOMY", "STANDARD", "HIGH", "PREMIUM", "MAX"
    val recordingCodec: String = "H264", // "H264", "H265"
    val targetFps: Int = 15, // 10, 15, 20, 25, 30
    val segmentDurationMinutes: Int = 2, // 2, 5, 10
    val rectifyStrength: Int = 0, // 0 - 100
    val telemetryOverlayEnabled: Boolean = true,

    // OEM tab
    val oemRecordingMode: String = "off", // "off", "continuous", "smart"
    val oemTelemetryOverlay: Boolean = false,
    val nativeDvrDisabled: Boolean = false,

    // Storage tab
    val storageType: String = "INTERNAL", // "INTERNAL", "SDCARD"
    val storageLimitMb: Int = 20000,
    val autoCleanup: Boolean = true,

    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

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
    onQualitySelected: (String) -> Unit,
    onCodecSelected: (String) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onClipDurationSelected: (Int) -> Unit,
    onRectifyStrengthChange: (Int) -> Unit,
    onToggleTelemetryOverlay: (Boolean) -> Unit,
    onOemRecordingModeSelected: (String) -> Unit,
    onToggleOemTelemetryOverlay: (Boolean) -> Unit,
    onToggleNativeDvr: () -> Unit,
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanup: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    showHeader: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (showHeader) OverdriveTheme.dimensions.pagePaddingHorizontal else 0.dp,
                    vertical = if (showHeader) OverdriveTheme.dimensions.pagePaddingTop else 0.dp
                )
        ) {
            if (showHeader) {
                // Top Header
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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

            // Sub-tabs row
            RecordingSubTabRow(
                selectedTab = state.selectedTab,
                onTabSelected = onTabSelected
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
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
                        onToggleGeocodingOnline = onToggleGeocodingOnline
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
                        onToggleTelemetryOverlay = onToggleTelemetryOverlay
                    )
                    RecordingTab.OEM -> OemDashcamTabContent(
                        state = state,
                        onOemRecordingModeSelected = onOemRecordingModeSelected,
                        onToggleOemTelemetryOverlay = onToggleOemTelemetryOverlay,
                        onToggleNativeDvr = onToggleNativeDvr
                    )
                    RecordingTab.STORAGE -> StorageTabContent(
                        state = state,
                        onStorageTypeSelected = onStorageTypeSelected,
                        onStorageLimitChange = onStorageLimitChange,
                        onToggleAutoCleanup = onToggleAutoCleanup
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingSubTabRow(
    selectedTab: RecordingTab,
    onTabSelected: (RecordingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RecordingTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    onToggleGeocodingOnline: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sürüş Kayıt Modu (ACC ON)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Araç çalışırken veya sürüş sırasında kameranın ne zaman kayıt yapacağını seçin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Kapalı (None)",
                        subtitle = "Kayıt yapmaz, kaynak tasarrufu",
                        isSelected = state.recordingMode == "NONE",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("NONE") }
                    )
                    SegmentOptionButton(
                        title = "Sürekli (Continuous)",
                        subtitle = "Kontak açıkken kesintisiz kayıt",
                        isSelected = state.recordingMode == "CONTINUOUS",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("CONTINUOUS") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Sürüşte (Drive Mode)",
                        subtitle = "Sadece vites D veya R iken kaydeder",
                        isSelected = state.recordingMode == "DRIVE_MODE",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("DRIVE_MODE") }
                    )
                    SegmentOptionButton(
                        title = "Yakınlık Koruması (Radar)",
                        subtitle = "Park radarları engel gördüğünde tetiklenir",
                        isSelected = state.recordingMode == "PROXIMITY_GUARD",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingModeSelected("PROXIMITY_GUARD") }
                    )
                }
            }
        }

        // Camera Layout Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Kamera Düzeni (Camera Layout)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kaydedilen videodaki 4 kameranın yerleşim şablonu (canlı uygulanır).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Standart (360 Izgara)",
                        subtitle = "4 kamera 2×2 eşit mozaik",
                        isSelected = state.recordingLayout == "standard",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingLayoutSelected("standard") }
                    )
                    SegmentOptionButton(
                        title = "Dashcam (Ön Yol Üstte)",
                        subtitle = "Ön kamera geniş, yan/arka altta",
                        isSelected = state.recordingLayout == "dashcam",
                        modifier = Modifier.weight(1f),
                        onClick = { onRecordingLayoutSelected("dashcam") }
                    )
                }

                if (state.recordingLayout == "dashcam") {
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingToggleRow(
                        title = "Dashcam Düzeninde Ön Cam Kamerasını Kullan",
                        subtitle = "Mevcut ise tepe görünümüne bağımsız ön cam sensörünü yerleştirir.",
                        checked = state.dashcamUseWindshield,
                        onCheckedChange = onToggleDashcamWindshield
                    )
                }
            }
        }

        // Proximity Guard Settings (visible when PROXIMITY_GUARD selected)
        if (state.recordingMode == "PROXIMITY_GUARD") {
            OverdriveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = 12.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Yakınlık Koruması Hassasiyeti (Proximity Guard)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Park radarları nesne algıladığında kaydın tetiklenme mesafesi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SegmentOptionButton(
                            title = "Kırmızı Bölge (< 30 cm)",
                            subtitle = "Yalnızca kritik yakınlıkta kaydet",
                            isSelected = state.proximityTriggerLevel == "RED",
                            modifier = Modifier.weight(1f),
                            onClick = { onProximityTriggerLevelSelected("RED") }
                        )
                        SegmentOptionButton(
                            title = "Sarı + Kırmızı (< 60 cm)",
                            subtitle = "Orta ve yakın mesafede tetikle",
                            isSelected = state.proximityTriggerLevel == "YELLOW_RED",
                            modifier = Modifier.weight(1f),
                            onClick = { onProximityTriggerLevelSelected("YELLOW_RED") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Olay Öncesi Kayıt Süresi (Pre-buffer): ${state.proximityPreSeconds} sn",
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
                        listOf(3, 5, 8, 10).forEach { s ->
                            PresetBadgeButton(
                                label = "$s sn",
                                isSelected = state.proximityPreSeconds == s,
                                modifier = Modifier.weight(1f),
                                onClick = { onProximityPreSecondsChange(s) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Olay Sonrası Kayıt Süresi (Post-roll): ${state.proximityPostSeconds} sn",
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
                        listOf(5, 10, 15, 20).forEach { s ->
                            PresetBadgeButton(
                                label = "$s sn",
                                isSelected = state.proximityPostSeconds == s,
                                modifier = Modifier.weight(1f),
                                onClick = { onProximityPostSecondsChange(s) }
                            )
                        }
                    }
                }
            }
        }

        // Place Tagging Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Konum Etiketleme (Place Tagging)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kayıt başlangıç GPS koordinatlarını ilçe ve şehir adına dönüştürüp klibe ekler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                SettingToggleRow(
                    title = "Kayıtları Konum İsimleriyle Etiketle",
                    subtitle = "Ters jeokodlama ile video yan dosyasına yer ismi kaydeder.",
                    checked = state.geocodingEnabled,
                    onCheckedChange = onToggleGeocodingEnabled
                )
                if (state.geocodingEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingToggleRow(
                        title = "Çevrimiçi Çözücü Kullan (OSM Nominatim)",
                        subtitle = "Cihaz içi çevrimdışı konum çözülemezse OpenStreetMap üzerinden sorgular.",
                        checked = state.geocodingOnline,
                        onCheckedChange = onToggleGeocodingOnline
                    )
                }
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
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Canlı Kayıt Durumu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                InfoRow(label = "Kayıt Motoru", value = state.currentState)
                InfoRow(
                    label = "Bugün Kaydedilen Video",
                    value = "${state.recordingsToday} klip"
                )
                InfoRow(label = "Mozaik Çözünürlük", value = state.nativeResolution)
                InfoRow(label = "Tahmini Veri Akışı", value = state.activeBitrateEstimate)
                InfoRow(label = "Aktif Kodlayıcı", value = state.recordingCodec)
                InfoRow(label = "Kamera Kare Hızı", value = "${state.targetFps} FPS")
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
    onToggleTelemetryOverlay: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Video Quality Tiers
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Video Kalite Seviyesi (Recording Quality)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Daha yüksek seviyeler daha net görüntü sağlar ancak depolama tüketimini artırır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ECONOMY" to "Tasarruf", "STANDARD" to "Standart", "HIGH" to "Yüksek", "PREMIUM" to "Premium", "MAX" to "Maksimum").forEach { (key, label) ->
                        PresetBadgeButton(
                            label = label,
                            isSelected = state.recordingQuality == key,
                            modifier = Modifier.weight(1f),
                            onClick = { onQualitySelected(key) }
                        )
                    }
                }
            }
        }

        // Codec & FPS Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Video Kodlayıcı & Kare Hızı (FPS)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Codec Row
                Text(
                    text = "Video Codec:",
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
                    SegmentOptionButton(
                        title = "H.264 (AVC)",
                        subtitle = "Geniş uyumluluk, daha düşük CPU",
                        isSelected = state.recordingCodec == "H264",
                        modifier = Modifier.weight(1f),
                        onClick = { onCodecSelected("H264") }
                    )
                    SegmentOptionButton(
                        title = "H.265 (HEVC)",
                        subtitle = "~%40 daha az disk alanı, modern",
                        isSelected = state.recordingCodec == "H265",
                        modifier = Modifier.weight(1f),
                        onClick = { onCodecSelected("H265") }
                    )
                }

                // FPS Row
                Text(
                    text = "Hedef Kare Hızı (FPS):",
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
                    listOf(10, 15, 20, 25, 30).forEach { fps ->
                        PresetBadgeButton(
                            label = "$fps FPS",
                            isSelected = state.targetFps == fps,
                            modifier = Modifier.weight(1f),
                            onClick = { onFpsSelected(fps) }
                        )
                    }
                }
            }
        }

        // Clip Segment Duration Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Klip Parça Süresi (Segment Duration)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Her bir video dosyasının kaç dakikalık bölümler halinde diske yazılacağını belirler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 5, 10).forEach { mins ->
                        PresetBadgeButton(
                            label = "$mins dakika",
                            isSelected = state.segmentDurationMinutes == mins,
                            modifier = Modifier.weight(1f),
                            onClick = { onClipDurationSelected(mins) }
                        )
                    }
                }
            }
        }

        // Fisheye Correction Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Balıkgözü Düzeltme (Fisheye Correction)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.rectifyStrength == 0) "Kapalı" else "${state.rectifyStrength} %",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Kayıtlardaki fıçı eğriliğini düzeltir. Yüksek değerler kenar pikselleri içeri çeker.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Slider(
                    value = state.rectifyStrength.toFloat(),
                    onValueChange = { onRectifyStrengthChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }

        // Telemetry Overlay Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Telemetri Bindirme (Telemetry Overlay)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingToggleRow(
                    title = "Kaydedilen Videolara Telemetri Yaz",
                    subtitle = "Hız (km/s), vites konumu, gaz/fren pedalları ve zaman damgasını videonun üzerine işler.",
                    checked = state.telemetryOverlayEnabled,
                    onCheckedChange = onToggleTelemetryOverlay
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
    onToggleNativeDvr: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "OEM İleri Sensör Kaydı (OEM Dashcam)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ön dikiz aynasındaki bağımsız OEM sensörünün dvr_*.mp4 olarak kaydedilme modu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Kapalı (Off)",
                        subtitle = "OEM sensöründen kayıt yapmaz",
                        isSelected = state.oemRecordingMode == "off",
                        modifier = Modifier.weight(1f),
                        onClick = { onOemRecordingModeSelected("off") }
                    )
                    SegmentOptionButton(
                        title = "Sürekli (Continuous)",
                        subtitle = "Sürekli dvr_*.mp4 kaydeder",
                        isSelected = state.oemRecordingMode == "continuous",
                        modifier = Modifier.weight(1f),
                        onClick = { onOemRecordingModeSelected("continuous") }
                    )
                    SegmentOptionButton(
                        title = "Akıllı (Smart)",
                        subtitle = "Pano ile eşzamanlı başlar/durur",
                        isSelected = state.oemRecordingMode == "smart",
                        modifier = Modifier.weight(1f),
                        onClick = { onOemRecordingModeSelected("smart") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingToggleRow(
                    title = "OEM Kliplerine Telemetri Yaz",
                    subtitle = "dvr_*.mp4 dosyalarına hız, GPS ve zaman damgası bindirir.",
                    checked = state.oemTelemetryOverlay,
                    onCheckedChange = onToggleOemTelemetryOverlay
                )
            }
        }

        // Native DVR Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Orijinal BYD DVR Uygulaması (com.byd.cdr)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fabrika dashcam uygulaması her kontak açılışında kamerayı kilitleyebilir. Devre dışı bırakmak kamera çakışmasını önler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                OverdriveButton(
                    text = if (state.nativeDvrDisabled) "Orijinal DVR'ı Tekrar Etkinleştir" else "Orijinal DVR'ı Devre Dışı Bırak",
                    onClick = onToggleNativeDvr,
                    modifier = Modifier.fillMaxWidth(),
                    variant = if (state.nativeDvrDisabled) OverdriveButtonVariant.TONAL else OverdriveButtonVariant.PRIMARY
                )
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
    onToggleAutoCleanup: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Sürüş Kayıt Depolama Konumu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sürüş video kayıtlarının saklanacağı ortam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Dahili Hafıza",
                        subtitle = "Baş ünitenin yerleşik depolaması",
                        isSelected = state.storageType == "INTERNAL",
                        modifier = Modifier.weight(1f),
                        onClick = { onStorageTypeSelected("INTERNAL") }
                    )
                    SegmentOptionButton(
                        title = "SD Kart / USB Sürücü",
                        subtitle = "Harici bellek kartı veya USB",
                        isSelected = state.storageType == "SDCARD",
                        modifier = Modifier.weight(1f),
                        onClick = { onStorageTypeSelected("SDCARD") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Storage Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sürüş Kayıtları İçin Ayrılan Sınır",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.storageLimitMb >= 1000) "${state.storageLimitMb / 1000} GB" else "${state.storageLimitMb} MB",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Bu sınır dolduğunda en eski rutin sürüş klipleri döngüsel olarak temizlenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5000, 10000, 20000, 50000, 100000).forEach { mb ->
                        PresetBadgeButton(
                            label = "${mb / 1000} GB",
                            isSelected = state.storageLimitMb == mb,
                            modifier = Modifier.weight(1f),
                            onClick = { onStorageLimitChange(mb) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Eski Klipleri Otomatik Sil (Döngüsel Kayıt)",
                    subtitle = "Depolama limiti dolduğunda kilitlenmemiş en eski klipleri silerek yer açar.",
                    checked = state.autoCleanup,
                    onCheckedChange = onToggleAutoCleanup
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE HELPER WIDGETS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SegmentOptionButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seçili",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PresetBadgeButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
        Spacer(modifier = Modifier.width(8.dp))
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
