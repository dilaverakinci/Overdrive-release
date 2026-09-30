package com.overdrive.app.ui.surveillance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.unit.sp
import com.overdrive.app.ui.component.OverdrivePillStatus
import com.overdrive.app.ui.component.OverdriveStatusPill
import com.overdrive.app.ui.theme.OverdriveColors
import com.overdrive.app.ui.theme.OverdriveTheme

private val OverdriveColors.textPrimary: Color get() = onSurface
private val OverdriveColors.textSecondary: Color get() = onSurfaceVariant
private val OverdriveColors.cardBackground: Color get() = surfaceContainer
private val OverdriveColors.cardBorder: Color get() = outlineVariant
private val OverdriveColors.accentGreen: Color get() = statusSuccess
private val OverdriveColors.accentAmber: Color get() = statusWarning
private val OverdriveColors.accentRed: Color get() = statusDanger

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
    val discardEmptyNightMotion: Boolean = false,

    // Recording
    val preRecordSeconds: Int = 5,
    val postRecordSeconds: Int = 10,
    val surveillanceQuality: String = "STANDARD", // "LOW", "STANDARD", "HIGH"
    val surveillanceCameraFps: Int = 15, // 10, 15, 20, 25, 30
    val recordingCodec: String = "H264", // "H264", "H265"
    val telegramSendStartPing: Boolean = false,

    // OEM Dashcam
    val oemDashcamEnabled: Boolean = false,
    val oemTriggerRecording: Boolean = false,
    val oemAutoCleanup: Boolean = true,

    // Storage
    val storageType: String = "INTERNAL", // "INTERNAL", "SDCARD"
    val storageLimitMb: Int = 500, // 500, 1000, 2000, 5000, 10000, 20000, 50000
    val autoCleanupEvents: Boolean = true,
    val discardEmptyBrightEvents: Boolean = false,

    val todayEventsCount: Int = 0,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Gözetim Ayarları",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = OverdriveTheme.colors.accentGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Yenile",
                        tint = OverdriveTheme.colors.textSecondary
                    )
                }
            }
        }

        // Sub-tabs row
        SurveillanceSubTabRow(
            selectedTab = state.selectedTab,
            onTabSelected = onTabSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

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

@Composable
private fun SurveillanceSubTabRow(
    selectedTab: SurveillanceTab,
    onTabSelected: (SurveillanceTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121418), RoundedCornerShape(8.dp))
            .border(1.dp, OverdriveTheme.colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SurveillanceTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) OverdriveTheme.colors.primary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isSelected) OverdriveTheme.colors.primary else Color.Transparent,
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
                        tint = if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Enable Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gözetim Sistemini Etkinleştir",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Hareket ve nesne algılaması ile park halindeyken çevreyi izler ve olay kaydı tutar.",
                        fontSize = 13.sp,
                        color = OverdriveTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OverdriveTheme.colors.accentGreen,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF2C2F36)
                    )
                )
            }
        }

        // Operating Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Çalışma Modu (Operating Mode)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Aracın kontak kapandıktan sonra Overdrive'ın uyanık kalıp kalmayacağını belirler.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Açık ve Kapalı (On & Off)",
                        subtitle = "Tam işlev: Park halindeyken de gözetim devam eder",
                        isSelected = state.operatingMode == "onAndOff",
                        modifier = Modifier.weight(1f),
                        onClick = { onOperatingModeSelected("onAndOff") }
                    )
                    SegmentOptionButton(
                        title = "Yalnızca Sürüş (On Only)",
                        subtitle = "Tasarruf: Araç kapatıldığında tam uykuya geçer",
                        isSelected = state.operatingMode == "onOnly",
                        modifier = Modifier.weight(1f),
                        onClick = { onOperatingModeSelected("onOnly") }
                    )
                }

                if (state.operatingMode == "onOnly") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(OverdriveTheme.colors.accentAmber.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .border(1.dp, OverdriveTheme.colors.accentAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Uyarı: 'Yalnızca Sürüş' modunda araç park edildiğinde sistem uykuya geçeceğinden park gözetimi yapılamaz.",
                            fontSize = 12.sp,
                            color = OverdriveTheme.colors.accentAmber
                        )
                    }
                }
            }
        }

        // Arm Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Devreye Girme Modu (Arm Mode)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Gözetim sisteminin ne zaman devreye gireceğini seçin.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Kapılar Kilitlendiğinde (On Lock)",
                        subtitle = "Kapı kilit sinyaliyle devreye girer",
                        isSelected = state.armMode == "lock",
                        modifier = Modifier.weight(1f),
                        onClick = { onArmModeSelected("lock") }
                    )
                    SegmentOptionButton(
                        title = "Araç Kapandığında (On Power-Off)",
                        subtitle = "Kontak kapandığı an doğrudan başlar",
                        isSelected = state.armMode == "power",
                        modifier = Modifier.weight(1f),
                        onClick = { onArmModeSelected("power") }
                    )
                }
            }
        }

        // ACC-OFF Mode Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Park Halinde Kayıt Şekli (ACC-OFF Mode)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Park sırasında yalnızca olay anları mı yoksa kesintisiz kayıt mı yapılacağını belirler.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentOptionButton(
                        title = "Akıllı (Smart - Önerilen)",
                        subtitle = "Hareket + AI ile olay klipi kaydeder",
                        isSelected = state.accOffMode == "smart",
                        modifier = Modifier.weight(1f),
                        onClick = { onAccOffModeSelected("smart") }
                    )
                    SegmentOptionButton(
                        title = "Sürekli Kayıt (Continuous)",
                        subtitle = "Filtresiz sürekli 4-kamera video kaydeder",
                        isSelected = state.accOffMode == "continuous",
                        modifier = Modifier.weight(1f),
                        onClick = { onAccOffModeSelected("continuous") }
                    )
                }
            }
        }

        // Power & Cellular Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Güç ve Bağlantı Koruma",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleRow(
                    title = "Park Halinde USB Gücünü Açık Tut",
                    subtitle = "Telefon şarjı veya aksesuarlar için USB portlarına güç vermeye devam eder.",
                    checked = state.keepUsbPowerOnAccOff,
                    onCheckedChange = onToggleKeepUsbPower
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Park Halinde Hücresel Veriyi Açık Tut",
                    subtitle = "Bazı BYD modellerinde veri modülü uyur; uzaktan erişim için açık tutulmasını sağlar.",
                    checked = state.mobileDataKeepAlive,
                    onCheckedChange = onToggleMobileDataKeepAlive
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "DiLink 5 Bulut Canlı Nabız (Heartbeat)",
                    subtitle = "DiLink 5 sistemlerinde BYD Cloud üzerinde gerçek zamanlı uyanık kalma sinyali gönderir.",
                    checked = state.di5CloudKeepAlive,
                    onCheckedChange = onToggleDi5CloudKeepAlive
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: DETECTION
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Environment Preset
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Ortam Ön Ayarı (Environment Preset)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Park koşullarınıza göre filtreleme ve duyarlılık dengesini ayarlar.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetBadgeButton(
                        label = "Açık Alan (Outdoor)",
                        isSelected = state.environmentPreset == "outdoor",
                        modifier = Modifier.weight(1f),
                        onClick = { onEnvironmentPresetSelected("outdoor") }
                    )
                    PresetBadgeButton(
                        label = "Garaj (Garage)",
                        isSelected = state.environmentPreset == "indoor",
                        modifier = Modifier.weight(1f),
                        onClick = { onEnvironmentPresetSelected("indoor") }
                    )
                    PresetBadgeButton(
                        label = "Sokak / Cadde",
                        isSelected = state.environmentPreset == "street",
                        modifier = Modifier.weight(1f),
                        onClick = { onEnvironmentPresetSelected("street") }
                    )
                }
            }
        }

        // AI Object Detection Classes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Yapay Zekâ Nesne Algılama Sınıfları",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Hareket algılandığında hangi nesnelerin olay kaydı tetikleyeceğini seçin.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetectionClassCheckbox(
                        label = "Yaya / İnsan",
                        checked = state.detectPerson,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleDetectPerson
                    )
                    DetectionClassCheckbox(
                        label = "Araç / Otomobil",
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
                        label = "Bisiklet / Motor",
                        checked = state.detectBike,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleDetectBike
                    )
                    DetectionClassCheckbox(
                        label = "Evcil Hayvan",
                        checked = state.detectAnimal,
                        modifier = Modifier.weight(1f),
                        onCheckedChange = onToggleDetectAnimal
                    )
                }
            }
        }

        // Sensitivity & Distance Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Sensitivity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hareket Duyarlılığı (Sensitivity)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Seviye ${state.sensitivityLevel} / 5",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.accentGreen
                    )
                }
                Slider(
                    value = state.sensitivityLevel.toFloat(),
                    onValueChange = { onSensitivityChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = OverdriveTheme.colors.accentGreen,
                        activeTrackColor = OverdriveTheme.colors.accentGreen
                    )
                )
                Text(
                    text = when (state.sensitivityLevel) {
                        1 -> "1: Sıkı (Yalnızca büyük ve ani hareketler)"
                        2 -> "2: Muhafazakâr (Rüzgarlı ağaçları eler)"
                        3 -> "3: Dengeli (Normal yaya ve araç hareketleri)"
                        4 -> "4: Hassas (Küçük ve hafif hareketleri yakalar)"
                        else -> "5: Maksimum (İç mekan/garaj için agresif duyarlılık)"
                    },
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Distance Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Algılama Mesafesi / Nesne Boyutu",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = when (state.distancePreset) {
                            1 -> "~3 m (Yakın)"
                            2 -> "~5 m"
                            3 -> "~8 m (Dengeli)"
                            4 -> "~10 m"
                            else -> "~15 m (Uzak)"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.accentGreen
                    )
                }
                Slider(
                    value = state.distancePreset.toFloat(),
                    onValueChange = { onDistanceChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = OverdriveTheme.colors.accentGreen,
                        activeTrackColor = OverdriveTheme.colors.accentGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loitering Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Oyalanma / Bekleme Eşiği",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "${state.loiteringTimeSeconds} saniye",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.accentGreen
                    )
                }
                Text(
                    text = "Kişinin aracın yanında bu süreden fazla durması durumunda olay tetiklenir.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "İzlenen Kameralar (Surveillance Quadrants)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Hangi kameraların hareket izlemesine dahil olacağını seçin.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
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

                Spacer(modifier = Modifier.height(12.dp))
                SettingToggleRow(
                    title = "Yan Kamera Hassasiyet Artırımı (Side-Cam Boost)",
                    subtitle = "Sağ ve sol kapı yanlarındaki yakın kör nokta hareketlerine ekstra duyarlılık verir.",
                    checked = state.sideCamBoost,
                    onCheckedChange = onToggleSideCamBoost
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingToggleRow(
                    title = "Hareket Isı Haritası (Motion Heatmap)",
                    subtitle = "Geliştirici izleme için ham hareket bloklarının ısı haritasını kaydeder.",
                    checked = state.motionHeatmap,
                    onCheckedChange = onToggleMotionHeatmap
                )
                Spacer(modifier = Modifier.height(8.dp))
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Buffers & Durations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Kayıt Süreleri ve Tampon",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Olay öncesi ve sonrası kayıt tamponlama süreleri.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Pre-record
                Text(
                    text = "Olay Öncesi Tampon (Pre-record)",
                    fontSize = 14.sp,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Hareket başlamadan önceki ${state.preRecordSeconds} saniyelik görüntü klibin başına eklenir.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Post-record
                Text(
                    text = "Olay Sonrası Kayıt Süresi (Post-record)",
                    fontSize = 14.sp,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Hareket bittikten sonra klibin ${state.postRecordSeconds} saniye daha kayda devam etmesi.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gözetim Video Kalitesi (Park Halinde)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Sürüş kamerasından bağımsız olarak park gözetim video bit hızını ayarlar.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Camera FPS
                Text(
                    text = "Park Kamera Kare Hızı (FPS)",
                    fontSize = 14.sp,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Düşük FPS pil ve depolama tasarrufu sağlar; hareket algılama aynı kalır.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
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

                Spacer(modifier = Modifier.height(14.dp))

                // Codec
                Text(
                    text = "Video Kodlayıcı (Codec)",
                    fontSize = 14.sp,
                    color = OverdriveTheme.colors.textPrimary
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Telegram Bildirim Ayarları",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BYD Orijinal Dashcam / DVR Entegrasyonu",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "BYD'nin yerleşik DVR sistemiyle çakışmadan koordineli çalışmayı sağlar.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OverdriveTheme.colors.cardBackground),
            border = BorderStroke(1.dp, OverdriveTheme.colors.cardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Kayıt Konumu (Storage Type)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OverdriveTheme.colors.textPrimary
                )
                Text(
                    text = "Gözetim video kayıtlarının saklanacağı ortam.",
                    fontSize = 13.sp,
                    color = OverdriveTheme.colors.textSecondary,
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

                Spacer(modifier = Modifier.height(16.dp))

                // Storage Limit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gözetim Ayrılan Alan Limiti",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = if (state.storageLimitMb >= 1000) "${state.storageLimitMb / 1000} GB" else "${state.storageLimitMb} MB",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = OverdriveTheme.colors.accentGreen
                    )
                }
                Text(
                    text = "Bu sınır aşıldığında en eski kilitlenmemiş olay klipleri otomatik temizlenir.",
                    fontSize = 12.sp,
                    color = OverdriveTheme.colors.textSecondary,
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

                Spacer(modifier = Modifier.height(14.dp))

                SettingToggleRow(
                    title = "Eski Olayları Otomatik Sil",
                    subtitle = "Ayrılan depolama sınırına ulaşıldığında en eski gözetim kliplerini temizler.",
                    checked = state.autoCleanupEvents,
                    onCheckedChange = onToggleAutoCleanupEvents
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                if (isSelected) OverdriveTheme.colors.primary.copy(alpha = 0.2f)
                else Color(0xFF16181D)
            )
            .border(
                1.dp,
                if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seçili",
                        tint = OverdriveTheme.colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray,
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
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) OverdriveTheme.colors.primary.copy(alpha = 0.2f)
                else Color(0xFF16181D)
            )
            .border(
                1.dp,
                if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.textSecondary
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
                if (checked) OverdriveTheme.colors.accentGreen.copy(alpha = 0.12f)
                else Color(0xFF16181D)
            )
            .border(
                1.dp,
                if (checked) OverdriveTheme.colors.accentGreen.copy(alpha = 0.5f) else OverdriveTheme.colors.cardBorder,
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
                fontSize = 13.sp,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (checked) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
            )
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = OverdriveTheme.colors.accentGreen,
                    checkmarkColor = Color.Black,
                    uncheckedColor = Color.Gray
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OverdriveTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = OverdriveTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OverdriveTheme.colors.accentGreen,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF2C2F36)
            )
        )
    }
}
