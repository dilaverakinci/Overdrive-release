package com.overdrive.app.ui.surveillance

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveTheme

enum class SurveillanceTab(val title: String, val icon: ImageVector) {
    GENERAL("Genel", Icons.Default.Security),
    DETECTION("Algılama ve AI", Icons.Default.Visibility),
    RECORDING("Kayıt ve Kalite", Icons.Default.Videocam),
    OEM("OEM Dashcam", Icons.Default.CameraAlt),
    STORAGE("Depolama", Icons.Default.Settings)
}

data class SurveillanceUiState(
    val selectedTab: SurveillanceTab = SurveillanceTab.GENERAL,
    val isEnabled: Boolean = false,
    val isArmed: Boolean = false,
    val operatingMode: String = "onAndOff", // "onAndOff", "onOnly"
    val armMode: String = "lock", // "lock", "power"
    val accOffMode: String = "smart", // "smart", "continuous"
    val keepUsbPowerOnAccOff: Boolean = true,
    val mobileDataKeepAlive: Boolean = false,
    val di5CloudKeepAlive: Boolean = false,

    // Detection
    val environmentPreset: String = "outdoor", // "outdoor", "indoor", "street", "custom"
    val detectPerson: Boolean = true,
    val detectCar: Boolean = true,
    val detectBike: Boolean = true,
    val detectAnimal: Boolean = false,
    val sensitivityLevel: Int = 3, // 1 - 5
    val distancePreset: Int = 3, // 1 - 5
    val loiteringTimeSeconds: Int = 3,
    val cameraFront: Boolean = true,
    val cameraRight: Boolean = true,
    val cameraLeft: Boolean = true,
    val cameraRear: Boolean = true,
    val sideCamBoost: Boolean = false,
    val motionHeatmap: Boolean = false,
    val discardEmptyNightMotion: Boolean = true,

    // Recording & Quality
    val preRecordSeconds: Int = 5,
    val postRecordSeconds: Int = 15,
    val surveillanceQuality: String = "STANDARD",
    val surveillanceCameraFps: Int = 15,
    val recordingCodec: String = "H264",
    val telegramSendStartPing: Boolean = true,

    // OEM Dashcam
    val oemDashcamEnabled: Boolean = false,
    val oemTriggerRecording: Boolean = false,
    val oemAutoCleanup: Boolean = false,

    // Storage
    val storageType: String = "INTERNAL",
    val storageLimitMb: Int = 5000,
    val autoCleanupEvents: Boolean = true,
    val discardEmptyBrightEvents: Boolean = true,

    val isLoading: Boolean = false,
    val statusMessage: String? = null
)

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
    onEnvironmentPresetSelected: (String) -> Unit,
    onToggleDetectPerson: (Boolean) -> Unit,
    onToggleDetectCar: (Boolean) -> Unit,
    onToggleDetectBike: (Boolean) -> Unit,
    onToggleDetectAnimal: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onDistanceChange: (Int) -> Unit,
    onLoiteringTimeChange: (Int) -> Unit,
    onToggleCameraFront: (Boolean) -> Unit,
    onToggleCameraRight: (Boolean) -> Unit,
    onToggleCameraLeft: (Boolean) -> Unit,
    onToggleCameraRear: (Boolean) -> Unit,
    onToggleSideCamBoost: (Boolean) -> Unit,
    onToggleMotionHeatmap: (Boolean) -> Unit,
    onToggleDiscardNightMotion: (Boolean) -> Unit,
    onPreRecordSecondsChange: (Int) -> Unit,
    onPostRecordSecondsChange: (Int) -> Unit,
    onQualitySelected: (String) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onCodecSelected: (String) -> Unit,
    onToggleTelegramPing: (Boolean) -> Unit,
    onToggleOemDashcam: (Boolean) -> Unit,
    onToggleOemTrigger: (Boolean) -> Unit,
    onToggleOemAutoCleanup: (Boolean) -> Unit,
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanupEvents: (Boolean) -> Unit,
    onToggleDiscardBrightEvents: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = OverdriveTheme.dimensions.pagePaddingHorizontal,
                    vertical = OverdriveTheme.dimensions.pagePaddingTop
                )
        ) {
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
                            text = "Gözetim Ayarları",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OverdriveStatusPill(
                            label = when {
                                !state.isEnabled -> "KAPALI"
                                state.isArmed -> "DEVREDE (ARMED)"
                                else -> "AÇIK (BEKLEMEDE)"
                            },
                            status = when {
                                !state.isEnabled -> OverdrivePillStatus.INFO
                                state.isArmed -> OverdrivePillStatus.SUCCESS
                                else -> OverdrivePillStatus.WARNING
                            }
                        )
                    }
                    Text(
                        text = "Park halindeyken aracı korur, hareket ve AI nesne algılamasıyla video kaydı yapar.",
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

            // Sub-tabs row
            SurveillanceSubTabRow(
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
                    SurveillanceTab.GENERAL -> GeneralTabContent(
                        state = state,
                        onToggleMaster = onToggleMaster,
                        onOperatingModeSelected = onOperatingModeSelected,
                        onArmModeSelected = onArmModeSelected,
                        onAccOffModeSelected = onAccOffModeSelected,
                        onToggleKeepUsbPower = onToggleKeepUsbPower,
                        onToggleMobileDataKeepAlive = onToggleMobileDataKeepAlive,
                        onToggleDi5CloudKeepAlive = onToggleDi5CloudKeepAlive
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
                        onToggleCameraFront = onToggleCameraFront,
                        onToggleCameraRight = onToggleCameraRight,
                        onToggleCameraLeft = onToggleCameraLeft,
                        onToggleCameraRear = onToggleCameraRear,
                        onToggleSideCamBoost = onToggleSideCamBoost,
                        onToggleMotionHeatmap = onToggleMotionHeatmap,
                        onToggleDiscardNightMotion = onToggleDiscardNightMotion
                    )
                    SurveillanceTab.RECORDING -> RecordingTabContent(
                        state = state,
                        onPreRecordSecondsChange = onPreRecordSecondsChange,
                        onPostRecordSecondsChange = onPostRecordSecondsChange,
                        onQualitySelected = onQualitySelected,
                        onFpsSelected = onFpsSelected,
                        onCodecSelected = onCodecSelected,
                        onToggleTelegramPing = onToggleTelegramPing
                    )
                    SurveillanceTab.OEM -> OemTabContent(
                        state = state,
                        onToggleOemDashcam = onToggleOemDashcam,
                        onToggleOemTrigger = onToggleOemTrigger,
                        onToggleOemAutoCleanup = onToggleOemAutoCleanup
                    )
                    SurveillanceTab.STORAGE -> StorageTabContent(
                        state = state,
                        onStorageTypeSelected = onStorageTypeSelected,
                        onStorageLimitChange = onStorageLimitChange,
                        onToggleAutoCleanupEvents = onToggleAutoCleanupEvents,
                        onToggleDiscardBrightEvents = onToggleDiscardBrightEvents
                    )
                }
            }
        }
    }
}

@Composable
private fun SurveillanceSubTabRow(
    selectedTab: SurveillanceTab,
    onTabSelected: (SurveillanceTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SurveillanceTab.values().forEach { tab ->
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
// TAB 1: GENERAL
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
    onToggleDi5CloudKeepAlive: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Master Enable Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gözetim Sistemini Etkinleştir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hareket ve nesne algılaması ile park halindeyken çevreyi izler ve olay kaydı tutar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }

        // Operating Mode Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Çalışma Modu (Operating Mode)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Aracın kontak kapandıktan sonra Overdrive'ın uyanık kalıp kalmayacağını belirler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Park & Sürüş (ON + OFF)",
                        subtitle = "Kontak kapalıyken arka plan uyanık kalarak nöbet tutar.",
                        isSelected = state.operatingMode == "onAndOff",
                        modifier = Modifier.weight(1f),
                        onClick = { onOperatingModeSelected("onAndOff") }
                    )
                    SegmentOptionButton(
                        title = "Yalnızca Sürüş (ON Only)",
                        subtitle = "Kontak kapandığında tüm servisler uyur, pil tüketmez.",
                        isSelected = state.operatingMode == "onOnly",
                        modifier = Modifier.weight(1f),
                        onClick = { onOperatingModeSelected("onOnly") }
                    )
                }

                if (state.operatingMode == "onOnly") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ON Only seçildiğinde araç park edildiğinde nöbet / gözetim yapılamaz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Arming Trigger Mode Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Devreye Girme Koşulu (Arm Trigger)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gözetim sisteminin ne zaman tetiklenip devriye moduna geçeceği.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Kilitlendiğinde (Lock)",
                        subtitle = "Kapılar uzaktan veya anahtarla kilitlendiğinde",
                        isSelected = state.armMode == "lock",
                        modifier = Modifier.weight(1f),
                        onClick = { onArmModeSelected("lock") }
                    )
                    SegmentOptionButton(
                        title = "Kontak Kapandığında (ACC OFF)",
                        subtitle = "Güç düğmesine basılıp araç stop edildiğinde",
                        isSelected = state.armMode == "power",
                        modifier = Modifier.weight(1f),
                        onClick = { onArmModeSelected("power") }
                    )
                }
            }
        }

        // ACC OFF Recording Strategy Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Park Kayıt Stratejisi (ACC OFF Strategy)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Park halindeyken kameraların sürekli mi yoksa sadece olay anında mı yazacağı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Akıllı Olay Kaydı (Smart)",
                        subtitle = "Sadece hareket veya nesne algılandığında kaydeder.",
                        isSelected = state.accOffMode == "smart",
                        modifier = Modifier.weight(1f),
                        onClick = { onAccOffModeSelected("smart") }
                    )
                    SegmentOptionButton(
                        title = "Sürekli Park Kaydı (Continuous)",
                        subtitle = "Park süresince 7/24 kesintisiz kayıt yapar (yüksek depolama).",
                        isSelected = state.accOffMode == "continuous",
                        modifier = Modifier.weight(1f),
                        onClick = { onAccOffModeSelected("continuous") }
                    )
                }
            }
        }

        // Hardware Keep-Alive Toggles Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Donanım Uyanıklık Ayarları (Hardware Keep-Alive)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Araç uykuya geçerken periferik portların ve bağlantıların açık tutulması.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                SettingToggleRow(
                    title = "USB Gücünü Açık Tut (ACC OFF)",
                    subtitle = "Harici modem, USB depolama veya aksesuarların enerjisini korur.",
                    checked = state.keepUsbPowerOnAccOff,
                    onCheckedChange = onToggleKeepUsbPower
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Mobil Veri Uyanık Tutucu (Keep-Alive)",
                    subtitle = "Arka planda hücresel veri modemin uykuya girmesini engeller.",
                    checked = state.mobileDataKeepAlive,
                    onCheckedChange = onToggleMobileDataKeepAlive
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "DiLink 5 Bulut Servisi Uyanık Tut",
                    subtitle = "BYD yerleşik bulut bağlantı köprüsünün aktif kalmasını sağlar.",
                    checked = state.di5CloudKeepAlive,
                    onCheckedChange = onToggleDi5CloudKeepAlive
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: DETECTION & AI
// ─────────────────────────────────────────────────────────────────────────────
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
    onToggleCameraFront: (Boolean) -> Unit,
    onToggleCameraRight: (Boolean) -> Unit,
    onToggleCameraLeft: (Boolean) -> Unit,
    onToggleCameraRear: (Boolean) -> Unit,
    onToggleSideCamBoost: (Boolean) -> Unit,
    onToggleMotionHeatmap: (Boolean) -> Unit,
    onToggleDiscardNightMotion: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Environment Presets Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ortam Önayarı (Environment Preset)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Aracın bulunduğu çevreye göre optimize edilmiş filtreleme profili.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("outdoor" to "Açık Otopark", "indoor" to "Kapalı Garaj", "street" to "Sokak / Cadde", "custom" to "Özel").forEach { (key, label) ->
                        PresetBadgeButton(
                            label = label,
                            isSelected = state.environmentPreset == key,
                            modifier = Modifier.weight(1f),
                            onClick = { onEnvironmentPresetSelected(key) }
                        )
                    }
                }
            }
        }

        // AI Object Detection Classes Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Algılanacak Nesne Sınıfları (AI Detection Classes)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Yapay zeka modelinin olay kaydını tetikleyeceği hedefler.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetectionClassCheckbox(
                        label = "İnsan (Person)",
                        checked = state.detectPerson,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleDetectPerson
                    )
                    DetectionClassCheckbox(
                        label = "Araç (Vehicle)",
                        checked = state.detectCar,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleDetectCar
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetectionClassCheckbox(
                        label = "Motosiklet / Bisiklet",
                        checked = state.detectBike,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleDetectBike
                    )
                    DetectionClassCheckbox(
                        label = "Hayvan (Kedi/Köpek)",
                        checked = state.detectAnimal,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleDetectAnimal
                    )
                }
            }
        }

        // Sensitivity & Range Sliders Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Sensitivity Level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hareket Hassasiyet Eşiği",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (state.sensitivityLevel) {
                            1 -> "1 / 5 (Çok Düşük)"
                            2 -> "2 / 5 (Düşük)"
                            3 -> "3 / 5 (Orta - Önerilen)"
                            4 -> "4 / 5 (Yüksek)"
                            else -> "5 / 5 (Çok Yüksek)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = state.sensitivityLevel.toFloat(),
                    onValueChange = { onSensitivityChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detection Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Algılama Mesafesi / Nesne Boyutu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (state.distancePreset) {
                            1 -> "~3 m (Yakın)"
                            2 -> "~5 m"
                            3 -> "~8 m (Dengeli)"
                            4 -> "~10 m"
                            else -> "~15 m (Uzak)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = state.distancePreset.toFloat(),
                    onValueChange = { onDistanceChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Loitering Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Oyalanma / Bekleme Eşiği",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.loiteringTimeSeconds} saniye",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Kişinin aracın yanında bu süreden fazla durması durumunda olay tetiklenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { sec ->
                        PresetBadgeButton(
                            label = "$sec sn",
                            isSelected = state.loiteringTimeSeconds == sec,
                            modifier = Modifier.weight(1f),
                            onClick = { onLoiteringTimeChange(sec) }
                        )
                    }
                }
            }
        }

        // Active Cameras Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "İzlenen Kameralar (Surveillance Quadrants)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hangi kameraların hareket izlemesine dahil olacağını seçin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetectionClassCheckbox(
                        label = "Ön Kamera",
                        checked = state.cameraFront,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleCameraFront
                    )
                    DetectionClassCheckbox(
                        label = "Sağ Kamera",
                        checked = state.cameraRight,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleCameraRight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetectionClassCheckbox(
                        label = "Sol Kamera",
                        checked = state.cameraLeft,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleCameraLeft
                    )
                    DetectionClassCheckbox(
                        label = "Arka Kamera",
                        checked = state.cameraRear,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleCameraRear
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingToggleRow(
                    title = "Yan Kamera Hassasiyet Artırımı (Side-Cam Boost)",
                    subtitle = "Sağ ve sol kapı yanlarındaki yakın kör nokta hareketlerine ekstra duyarlılık verir.",
                    checked = state.sideCamBoost,
                    onCheckedChange = onToggleSideCamBoost
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingToggleRow(
                    title = "Hareket Isı Haritası (Motion Heatmap)",
                    subtitle = "Geliştirici izleme için ham hareket bloklarının ısı haritasını kaydeder.",
                    checked = state.motionHeatmap,
                    onCheckedChange = onToggleMotionHeatmap
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingToggleRow(
                    title = "Gece Boş Hareketleri Filtrele",
                    subtitle = "Karanlıkta far ışığı ve gürültü kaynaklı boş hareket tetiklemelerini eler.",
                    checked = state.discardEmptyNightMotion,
                    onCheckedChange = onToggleDiscardNightMotion
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: RECORDING & QUALITY
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecordingTabContent(
    state: SurveillanceUiState,
    onPreRecordSecondsChange: (Int) -> Unit,
    onPostRecordSecondsChange: (Int) -> Unit,
    onQualitySelected: (String) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onCodecSelected: (String) -> Unit,
    onToggleTelegramPing: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Buffers & Durations
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Kayıt Süreleri ve Tampon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Olay öncesi ve sonrası kayıt tamponlama süreleri.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                // Pre-record
                Text(
                    text = "Olay Öncesi Tampon (Pre-record)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hareket başlamadan önceki ${state.preRecordSeconds} saniyelik görüntü klibin başına eklenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 10).forEach { sec ->
                        PresetBadgeButton(
                            label = "$sec sn",
                            isSelected = state.preRecordSeconds == sec,
                            modifier = Modifier.weight(1f),
                            onClick = { onPreRecordSecondsChange(sec) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post-record
                Text(
                    text = "Olay Sonrası Kayıt Süresi (Post-record)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Hareket bittikten sonra klibin ${state.postRecordSeconds} saniye daha kayda devam etmesi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 60).forEach { sec ->
                        PresetBadgeButton(
                            label = "$sec sn",
                            isSelected = state.postRecordSeconds == sec,
                            modifier = Modifier.weight(1f),
                            onClick = { onPostRecordSecondsChange(sec) }
                        )
                    }
                }
            }
        }

        // Quality & FPS
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Gözetim Video Kalitesi (Park Halinde)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sürüş kamerasından bağımsız olarak park gözetim video bit hızını ayarlar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Düşük (LOW)",
                        subtitle = "Az yer kaplar",
                        isSelected = state.surveillanceQuality == "LOW",
                        modifier = Modifier.weight(1f),
                        onClick = { onQualitySelected("LOW") }
                    )
                    SegmentOptionButton(
                        title = "Standart (STANDARD)",
                        subtitle = "Önerilen denge",
                        isSelected = state.surveillanceQuality == "STANDARD",
                        modifier = Modifier.weight(1f),
                        onClick = { onQualitySelected("STANDARD") }
                    )
                    SegmentOptionButton(
                        title = "Yüksek (HIGH)",
                        subtitle = "Net ayrıntı",
                        isSelected = state.surveillanceQuality == "HIGH",
                        modifier = Modifier.weight(1f),
                        onClick = { onQualitySelected("HIGH") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera FPS
                Text(
                    text = "Park Kamera Kare Hızı (FPS)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Düşük FPS pil ve depolama tasarrufu sağlar; hareket algılama aynı kalır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 20, 25, 30).forEach { fps ->
                        PresetBadgeButton(
                            label = "$fps FPS",
                            isSelected = state.surveillanceCameraFps == fps,
                            modifier = Modifier.weight(1f),
                            onClick = { onFpsSelected(fps) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Codec
                Text(
                    text = "Video Kodlayıcı (Codec)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "H.264 (AVC)",
                        subtitle = "En yüksek uyumluluk",
                        isSelected = state.recordingCodec == "H264",
                        modifier = Modifier.weight(1f),
                        onClick = { onCodecSelected("H264") }
                    )
                    SegmentOptionButton(
                        title = "H.265 (HEVC)",
                        subtitle = "%40 daha az yer",
                        isSelected = state.recordingCodec == "H265",
                        modifier = Modifier.weight(1f),
                        onClick = { onCodecSelected("H265") }
                    )
                }
            }
        }

        // Notification Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Telegram Bildirim Ayarları",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingToggleRow(
                    title = "Kayıt Başladığında Bildirim Gönder (Ping)",
                    subtitle = "Olay kaydı başladığı an gecikmesiz 'Kayıt Devam Ediyor...' bildirimi gönderir.",
                    checked = state.telegramSendStartPing,
                    onCheckedChange = onToggleTelegramPing
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 4: OEM DASHCAM
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OemTabContent(
    state: SurveillanceUiState,
    onToggleOemDashcam: (Boolean) -> Unit,
    onToggleOemTrigger: (Boolean) -> Unit,
    onToggleOemAutoCleanup: (Boolean) -> Unit
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
                    text = "BYD Orijinal Dashcam / DVR Entegrasyonu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "BYD'nin yerleşik DVR sistemiyle çakışmadan koordineli çalışmayı sağlar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                SettingToggleRow(
                    title = "OEM Dashcam Koordinasyonu",
                    subtitle = "Orijinal araç içi DVR ve Overdrive 360 kamerasının eşzamanlı çalışmasını destekler.",
                    checked = state.oemDashcamEnabled,
                    onCheckedChange = onToggleOemDashcam
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Gözetim Olayında OEM DVR Kaydı Tetikle",
                    subtitle = "Park gözetimi alarm verdiğinde aracın yerleşik DVR'ına da acil durum kaydı emri iletir.",
                    checked = state.oemTriggerRecording,
                    onCheckedChange = onToggleOemTrigger
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "BYD CDR Otomatik Temizlik",
                    subtitle = "Orijinal BYD Dashcam kartı dolduğunda kilitli olmayan eski rutin klipleri temizler.",
                    checked = state.oemAutoCleanup,
                    onCheckedChange = onToggleOemAutoCleanup
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
    state: SurveillanceUiState,
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanupEvents: (Boolean) -> Unit,
    onToggleDiscardBrightEvents: (Boolean) -> Unit
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
                    text = "Kayıt Konumu (Storage Type)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gözetim video kayıtlarının saklanacağı ortam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Dahili Depolama",
                        subtitle = "/data/local/tmp/overdrive_recordings",
                        isSelected = state.storageType == "INTERNAL",
                        modifier = Modifier.weight(1f),
                        onClick = { onStorageTypeSelected("INTERNAL") }
                    )
                    SegmentOptionButton(
                        title = "SD Kart / Harici",
                        subtitle = "FAT32 / exFAT harici bellek kartı",
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
                        text = "Gözetim Ayrılan Alan Limiti",
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
                    text = "Bu sınır aşıldığında en eski kilitlenmemiş olay klipleri otomatik temizlenir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(500, 1000, 2000, 5000, 10000, 20000).forEach { mb ->
                        PresetBadgeButton(
                            label = if (mb >= 1000) "${mb / 1000}G" else "${mb}M",
                            isSelected = state.storageLimitMb == mb,
                            modifier = Modifier.weight(1f),
                            onClick = { onStorageLimitChange(mb) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Eski Olayları Otomatik Sil",
                    subtitle = "Ayrılan depolama sınırına ulaşıldığında en eski gözetim kliplerini temizler.",
                    checked = state.autoCleanupEvents,
                    onCheckedChange = onToggleAutoCleanupEvents
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingToggleRow(
                    title = "Flaş ve Parlama Olaylarını Kaydetme",
                    subtitle = "Ani ışık patlaması veya şimşek kaynaklı boş hareket tetiklemelerini kaydetmez.",
                    checked = state.discardEmptyBrightEvents,
                    onCheckedChange = onToggleDiscardBrightEvents
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE HELPER WIDGETS
// ─────────────────────────────────────────────────────────────────────────────
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
private fun DetectionClassCheckbox(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .border(
                1.dp,
                if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.size(20.dp)
            )
        }
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
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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
