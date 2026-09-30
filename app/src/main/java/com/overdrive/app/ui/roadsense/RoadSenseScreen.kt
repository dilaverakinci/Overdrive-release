package com.overdrive.app.ui.roadsense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.overdrive.app.R
import com.overdrive.app.roadsense.config.RoadSenseConfig
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
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

enum class RoadSenseTab(val label: String) {
    GENERAL("Genel"),
    WARNINGS("Uyarılar & Ses"),
    MAP_ROUTING("Harita & Rota"),
    BLIND_SPOT("Kör Nokta"),
    DATA_SYNC("Veri & Topluluk")
}

data class RoadSenseUiState(
    val selectedTab: RoadSenseTab = RoadSenseTab.GENERAL,
    val isMasterEnabled: Boolean = false,
    val overlayVisible: Boolean = true,
    val calibrationMode: Boolean = false,
    val cornerYawSpeedAdaptive: Boolean = true,

    // Warnings
    val warnEnabled: Boolean = true,
    val warnMode: String = "both", // "both", "audio", "visual"
    val warnAudioChannel: String = "navigation", // "navigation", "media", "voice", "alarm"
    val warnAudioVolume: Int = 75,
    val warnLeadSeconds: Float = 4.0f,
    val detectionSensitivity: Float = 1.0f,
    val severityMinor: Boolean = true,
    val severityModerate: Boolean = true,
    val severitySevere: Boolean = true,

    // Map & Routing
    val hasRoutingKey: Boolean = false,
    val routingApiKeyInput: String = "",
    val routingEndpointInput: String = "https://api.stadiamaps.com/route/v1",
    val autoProjectCluster: Boolean = false,

    // Blind Spot
    val bsEnabled: Boolean = false,
    val bsMergeMode: String = "both", // "both", "side", "rear"
    val bsMinSpeedKmh: Int = 0,
    val bsMaxSpeedKmh: Int = 0,
    val bsSuppressReverse: Boolean = false,
    val bsRectifyStrength: Int = 0,

    // Data & Crowdsource
    val crowdUpload: Boolean = false,
    val crowdDownload: Boolean = false,
    val syncWorkerUrl: String = RoadSenseConfig.DEFAULT_WORKER_URL,
    val localHazardCount: Long = 0L,

    // Dialogs
    val showClearLocalDialog: Boolean = false,
    val showClearCloudDialog: Boolean = false,
    val isLoading: Boolean = false
)

@Composable
fun RoadSenseScreen(
    state: RoadSenseUiState,
    onTabSelected: (RoadSenseTab) -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleOverlayVisible: (Boolean) -> Unit,
    onToggleCalibrationMode: (Boolean) -> Unit,
    onToggleCornerAdaptive: (Boolean) -> Unit,
    onToggleWarnEnabled: (Boolean) -> Unit,
    onSelectWarnMode: (String) -> Unit,
    onSelectAudioChannel: (String) -> Unit,
    onAudioVolumeChange: (Int) -> Unit,
    onTestChime: (severity: String) -> Unit,
    onLeadSecondsChange: (Float) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onToggleSeverityMinor: (Boolean) -> Unit,
    onToggleSeverityModerate: (Boolean) -> Unit,
    onToggleSeveritySevere: (Boolean) -> Unit,
    onOpenHazardMap: () -> Unit,
    onRoutingApiKeyChange: (String) -> Unit,
    onSaveRoutingKey: () -> Unit,
    onToggleAutoProjectCluster: (Boolean) -> Unit,
    onToggleBsEnabled: (Boolean) -> Unit,
    onSelectBsMergeMode: (String) -> Unit,
    onBsMinSpeedChange: (Int) -> Unit,
    onBsMaxSpeedChange: (Int) -> Unit,
    onToggleBsSuppressReverse: (Boolean) -> Unit,
    onBsRectifyChange: (Int) -> Unit,
    onToggleCrowdUpload: (Boolean) -> Unit,
    onToggleCrowdDownload: (Boolean) -> Unit,
    onClearLocalHazardsRequest: () -> Unit,
    onClearLocalHazardsConfirm: () -> Unit,
    onClearLocalHazardsDismiss: () -> Unit,
    onClearCloudHazardsRequest: () -> Unit,
    onClearCloudHazardsConfirm: () -> Unit,
    onClearCloudHazardsDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OverdriveTheme.colors.background)
    ) {
        // Top Header
        RoadSenseHeader(
            state = state,
            onTabSelected = onTabSelected
        )

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (state.selectedTab) {
                RoadSenseTab.GENERAL -> {
                    GeneralTabContent(
                        state = state,
                        onToggleMaster = onToggleMaster,
                        onToggleOverlayVisible = onToggleOverlayVisible,
                        onToggleCalibrationMode = onToggleCalibrationMode,
                        onToggleCornerAdaptive = onToggleCornerAdaptive,
                        onOpenMap = onOpenHazardMap
                    )
                }
                RoadSenseTab.WARNINGS -> {
                    WarningsTabContent(
                        state = state,
                        onToggleWarnEnabled = onToggleWarnEnabled,
                        onSelectWarnMode = onSelectWarnMode,
                        onSelectAudioChannel = onSelectAudioChannel,
                        onAudioVolumeChange = onAudioVolumeChange,
                        onTestChime = onTestChime,
                        onLeadSecondsChange = onLeadSecondsChange,
                        onSensitivityChange = onSensitivityChange,
                        onToggleSeverityMinor = onToggleSeverityMinor,
                        onToggleSeverityModerate = onToggleSeverityModerate,
                        onToggleSeveritySevere = onToggleSeveritySevere
                    )
                }
                RoadSenseTab.MAP_ROUTING -> {
                    MapRoutingTabContent(
                        state = state,
                        onOpenMap = onOpenHazardMap,
                        onRoutingApiKeyChange = onRoutingApiKeyChange,
                        onSaveRoutingKey = onSaveRoutingKey,
                        onToggleAutoProjectCluster = onToggleAutoProjectCluster
                    )
                }
                RoadSenseTab.BLIND_SPOT -> {
                    BlindSpotTabContent(
                        state = state,
                        onToggleBsEnabled = onToggleBsEnabled,
                        onSelectBsMergeMode = onSelectBsMergeMode,
                        onBsMinSpeedChange = onBsMinSpeedChange,
                        onBsMaxSpeedChange = onBsMaxSpeedChange,
                        onToggleBsSuppressReverse = onToggleBsSuppressReverse,
                        onBsRectifyChange = onBsRectifyChange
                    )
                }
                RoadSenseTab.DATA_SYNC -> {
                    DataSyncTabContent(
                        state = state,
                        onToggleCrowdUpload = onToggleCrowdUpload,
                        onToggleCrowdDownload = onToggleCrowdDownload,
                        onClearLocal = onClearLocalHazardsRequest,
                        onClearCloud = onClearCloudHazardsRequest
                    )
                }
            }
        }
    }

    // Clear Local Hazards Dialog
    if (state.showClearLocalDialog) {
        OverdriveDialog(
            title = "Yerel Tehlikeleri Temizle",
            onDismissRequest = onClearLocalHazardsDismiss,
            positiveButtonText = "Tümünü Sil",
            onPositiveClick = onClearLocalHazardsConfirm,
            negativeButtonText = "İptal",
            onNegativeClick = onClearLocalHazardsDismiss
        ) {
            Text(
                text = "Cihazda kayıtlı tüm yol tehlikeleri (kasisler, çukurlar, bozuk yollar) ve kalibrasyon geçmişi silinecektir. Devam etmek istiyor musunuz?",
                style = MaterialTheme.typography.bodyMedium,
                color = OverdriveTheme.colors.textSecondary
            )
        }
    }

    // Clear Cloud Hazards Dialog
    if (state.showClearCloudDialog) {
        OverdriveDialog(
            title = "Bulut Verilerini Sil",
            onDismissRequest = onClearCloudHazardsDismiss,
            positiveButtonText = "Buluttan Sil",
            onPositiveClick = onClearCloudHazardsConfirm,
            negativeButtonText = "İptal",
            onNegativeClick = onClearCloudHazardsDismiss
        ) {
            Text(
                text = "Bu cihaz tarafından bulut sunucusuna yüklenmiş tüm tehlike tespitleri silinecektir. Bu işlem geri alınamaz.",
                style = MaterialTheme.typography.bodyMedium,
                color = OverdriveTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun RoadSenseHeader(
    state: RoadSenseUiState,
    onTabSelected: (RoadSenseTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = OverdriveTheme.colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(OverdriveTheme.colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_roadsense),
                            contentDescription = null,
                            tint = OverdriveTheme.colors.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "RoadSense Yol Algılama",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = OverdriveTheme.colors.textPrimary
                            )

                            OverdriveStatusPill(
                                status = if (state.isMasterEnabled) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO,
                                label = if (state.isMasterEnabled) "AKTİF" else "KAPALI"
                            )
                        }

                        Text(
                            text = "Kasis, çukur ve engebeleri IMU/GPS ile tespit eder ve yaklaşırken önceden uyarır",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-tabs row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                RoadSenseTab.values().forEach { tab ->
                    val isSelected = state.selectedTab == tab
                    val bgColor by animateColorAsState(
                        if (isSelected) OverdriveTheme.colors.primary else Color.Transparent,
                        label = "rsTabBg"
                    )
                    val textColor by animateColorAsState(
                        if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary,
                        label = "rsTabText"
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTabSelected(tab) },
                        color = bgColor,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 1: GENERAL
// -----------------------------------------------------------------------------
@Composable
private fun GeneralTabContent(
    state: RoadSenseUiState,
    onToggleMaster: (Boolean) -> Unit,
    onToggleOverlayVisible: (Boolean) -> Unit,
    onToggleCalibrationMode: (Boolean) -> Unit,
    onToggleCornerAdaptive: (Boolean) -> Unit,
    onOpenMap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Switch Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RoadSense Sistemini Etkinleştir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Aracın dahili hareket sensörleri (IMU) ve GPS ile zemin kusurlarını otomatik algılar ve bilinen tehlikelere yaklaşırken uyarır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.isMasterEnabled,
                    onCheckedChange = onToggleMaster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Overlay Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kayan Ekran Rozeti ve Uyarı Kartı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Sürüş sırasında ekranda RoadSense durum rozetini ve tehlikeye yaklaşırken görsel uyarı kartını gösterir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.overlayVisible,
                    onCheckedChange = onToggleOverlayVisible,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Calibration Mode Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IMU Kalibrasyon & Etiketleme Modu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Bir kasis veya çukurdan geçildiğinde ekranda onay kartı çıkararak yapay zeka modelinin doğruluğunu artırmanızı sağlar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.calibrationMode,
                    onCheckedChange = onToggleCalibrationMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Corner Adaptive Yaw Filter
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dönüşlerde Hıza Duyarlı Filtre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Viraj ve kavşak dönüşlerinde merkezkaç kuvvetinin yalancı tümsek olarak algılanmasını hıza bağlı dinamik filtreler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.cornerYawSpeedAdaptive,
                    onCheckedChange = onToggleCornerAdaptive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Quick Map Button Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zemin Tehlikeleri Haritası",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Şehrinizdeki tüm kayıtlı kasis, çukur ve bozuk yolları harita üzerinde inceleyin ve rota oluşturun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                OverdriveButton(
                    text = "Haritayı Aç",
                    onClick = onOpenMap,
                    variant = OverdriveButtonVariant.PRIMARY,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_roadsense_map),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 2: WARNINGS & AUDIO
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarningsTabContent(
    state: RoadSenseUiState,
    onToggleWarnEnabled: (Boolean) -> Unit,
    onSelectWarnMode: (String) -> Unit,
    onSelectAudioChannel: (String) -> Unit,
    onAudioVolumeChange: (Int) -> Unit,
    onTestChime: (String) -> Unit,
    onLeadSecondsChange: (Float) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onToggleSeverityMinor: (Boolean) -> Unit,
    onToggleSeverityModerate: (Boolean) -> Unit,
    onToggleSeveritySevere: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warn Master Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Yaklaşma Uyarılarını Etkinleştir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Kayıtlı bir tehlikeye doğru sürüş yaparken zamanında bildirim almanızı sağlar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.warnEnabled,
                    onCheckedChange = onToggleWarnEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Warn Mode Selection (Both / Audio / Visual)
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Uyarı Türü",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val modeOptions = listOf(
                        "both" to "Görsel ve Sesli",
                        "audio" to "Yalnızca Sesli",
                        "visual" to "Yalnızca Görsel"
                    )

                    modeOptions.forEach { (modeKey, modeLabel) ->
                        val isSelected = state.warnMode == modeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectWarnMode(modeKey) },
                            color = if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = modeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audio Channel & Volume & Test Chime Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Uyarı Sesi & Hoparlör Akışı",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                // Channel Dropdown
                var channelDropdownExpanded by remember { mutableStateOf(false) }
                val channelNames = mapOf(
                    "navigation" to "Navigasyon Akışı (Müziği Kısar, Önerilen)",
                    "media" to "Medya Akışı",
                    "voice" to "Sesli Asistan Akışı",
                    "alarm" to "Alarm Akışı"
                )

                ExposedDropdownMenuBox(
                    expanded = channelDropdownExpanded,
                    onExpandedChange = { channelDropdownExpanded = !channelDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = channelNames[state.warnAudioChannel] ?: state.warnAudioChannel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OverdriveTheme.colors.primary,
                            unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                            focusedTextColor = OverdriveTheme.colors.textPrimary,
                            unfocusedTextColor = OverdriveTheme.colors.textPrimary
                        ),
                        label = { Text("Çalma Akışı", color = OverdriveTheme.colors.textSecondary) }
                    )

                    ExposedDropdownMenu(
                        expanded = channelDropdownExpanded,
                        onDismissRequest = { channelDropdownExpanded = false },
                        modifier = Modifier.background(OverdriveTheme.colors.cardBackground)
                    ) {
                        channelNames.forEach { (chKey, chLabel) ->
                            DropdownMenuItem(
                                text = { Text(chLabel, color = OverdriveTheme.colors.textPrimary) },
                                onClick = {
                                    onSelectAudioChannel(chKey)
                                    channelDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Volume Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Uyarı Sesi Seviyesi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "%${state.warnAudioVolume}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.primary
                        )
                    }
                    Slider(
                        value = state.warnAudioVolume.toFloat(),
                        onValueChange = { onAudioVolumeChange(it.toInt()) },
                        valueRange = 10f..100f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = OverdriveTheme.colors.primary,
                            activeTrackColor = OverdriveTheme.colors.primary
                        )
                    )
                }

                // Test Chimes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Uyarı Sesini Test Et",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverdriveButton(
                            text = "Hafif (Kasis)",
                            onClick = { onTestChime("minor") },
                            variant = OverdriveButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                        OverdriveButton(
                            text = "Orta (Çukur)",
                            onClick = { onTestChime("moderate") },
                            variant = OverdriveButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                        OverdriveButton(
                            text = "Şiddetli (Bozuk Yol)",
                            onClick = { onTestChime("severe") },
                            variant = OverdriveButtonVariant.OUTLINED,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Timing & Sensitivity Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Zamanlama & Algılama Hassasiyeti",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                // Lead Time Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Önceden Uyarma Süresi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "${state.warnLeadSeconds.toInt()} saniye",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.primary
                        )
                    }
                    Slider(
                        value = state.warnLeadSeconds,
                        onValueChange = onLeadSecondsChange,
                        valueRange = 2f..8f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = OverdriveTheme.colors.primary,
                            activeTrackColor = OverdriveTheme.colors.primary
                        )
                    )
                }

                // Sensitivity Multiplier Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Algılama Hassasiyeti Çarpanı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        val sensLabel = when {
                            state.detectionSensitivity < 0.9f -> "Yüksek Hassasiyet"
                            state.detectionSensitivity > 1.1f -> "Düşük Hassasiyet"
                            else -> "Dengeli (Varsayılan)"
                        }
                        Text(
                            text = "${String.format("%.1f", state.detectionSensitivity)}x ($sensLabel)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.primary
                        )
                    }
                    Slider(
                        value = state.detectionSensitivity,
                        onValueChange = onSensitivityChange,
                        valueRange = 0.7f..1.3f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = OverdriveTheme.colors.primary,
                            activeTrackColor = OverdriveTheme.colors.primary
                        )
                    )
                }
            }
        }

        // Severity Gates Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Uyarı Verilecek Tehlike Şiddetleri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                // Minor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hafif Tehlikeler (Hız Kasisleri)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Yoldaki standart hız tümsekleri ve küçük yükseltiler.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = state.severityMinor,
                        onCheckedChange = onToggleSeverityMinor,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverdriveTheme.colors.primary,
                            checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                            uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                        )
                    )
                }

                HorizontalDivider(color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.3f))

                // Moderate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Orta Tehlikeler (Çukurlar)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Yol çukurları, seviye farkı olan rögar kapakları ve zemin göçükleri.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = state.severityModerate,
                        onCheckedChange = onToggleSeverityModerate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverdriveTheme.colors.primary,
                            checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                            uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                        )
                    )
                }

                HorizontalDivider(color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.3f))

                // Severe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Şiddetli Tehlikeler (Bozuk Zemin / Derin Çukurlar)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Lastik ve süspansiyona zarar verebilecek keskin yarıklar ve derin çukurlar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = state.severitySevere,
                        onCheckedChange = onToggleSeveritySevere,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverdriveTheme.colors.primary,
                            checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                            uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                        )
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 3: MAP & ROUTING
// -----------------------------------------------------------------------------
@Composable
private fun MapRoutingTabContent(
    state: RoadSenseUiState,
    onOpenMap: () -> Unit,
    onRoutingApiKeyChange: (String) -> Unit,
    onSaveRoutingKey: () -> Unit,
    onToggleAutoProjectCluster: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Open Map Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Canlı Tehlike Haritası",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Tüm yol tehlikelerini görebileceğiniz, rota çizebileceğiniz ve tehlikeleri yönetebileceğiniz tam ekran yerel haritayı açar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }

                    OverdriveButton(
                        text = "Haritayı Aç",
                        onClick = onOpenMap,
                        variant = OverdriveButtonVariant.PRIMARY,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_roadsense_map),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }

        // Valhalla Routing BYOK Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Navigasyon ve Rota Hesaplama (BYOK)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Harita görüntüleme ücretsizdir. Adım adım navigasyon ve rota çizimi için ücretsiz Stadia Maps Valhalla API anahtarınızı girebilirsiniz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }

                    OverdriveStatusPill(
                        status = if (state.hasRoutingKey) OverdrivePillStatus.SUCCESS else OverdrivePillStatus.INFO,
                        label = if (state.hasRoutingKey) "ANAHTAR TANIMLI" else "ANAHTAR YOK"
                    )
                }

                OutlinedTextField(
                    value = state.routingApiKeyInput,
                    onValueChange = onRoutingApiKeyChange,
                    label = { Text("Valhalla API Anahtarı", color = OverdriveTheme.colors.textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OverdriveTheme.colors.primary,
                        unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                        focusedTextColor = OverdriveTheme.colors.textPrimary,
                        unfocusedTextColor = OverdriveTheme.colors.textPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OverdriveButton(
                        text = "Anahtarı Kaydet",
                        onClick = onSaveRoutingKey,
                        variant = OverdriveButtonVariant.PRIMARY,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Instrument Cluster Projection Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gösterge Paneline Otomatik Yansıt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Araç çalıştığında (ACC-ON) haritayı ve tehlikeleri sürücü gösterge ekranına otomatik yansıtır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.autoProjectCluster,
                    onCheckedChange = onToggleAutoProjectCluster,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 4: BLIND SPOT CAMERA
// -----------------------------------------------------------------------------
@Composable
private fun BlindSpotTabContent(
    state: RoadSenseUiState,
    onToggleBsEnabled: (Boolean) -> Unit,
    onSelectBsMergeMode: (String) -> Unit,
    onBsMinSpeedChange: (Int) -> Unit,
    onBsMaxSpeedChange: (Int) -> Unit,
    onToggleBsSuppressReverse: (Boolean) -> Unit,
    onBsRectifyChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Blind Spot Master Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kör Nokta Kamera Kartını Etkinleştir",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "Dönüş sinyali verildiğinde ilgili tarafın kamera görüntüsünü ekranda kayan kart olarak gösterir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OverdriveTheme.colors.textSecondary
                    )
                }

                Switch(
                    checked = state.bsEnabled,
                    onCheckedChange = onToggleBsEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OverdriveTheme.colors.primary,
                        checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                        uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                    )
                )
            }
        }

        // Camera Feed Selection (Both / Side / Rear)
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Kamera Görünüm Modu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val mergeOptions = listOf(
                        "both" to "Birleşik (Arka + Yan)",
                        "side" to "Yalnızca Yan Kamera",
                        "rear" to "Yalnızca Arka Kamera"
                    )

                    mergeOptions.forEach { (modeKey, modeLabel) ->
                        val isSelected = state.bsMergeMode == modeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectBsMergeMode(modeKey) },
                            color = if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) OverdriveTheme.colors.primary else OverdriveTheme.colors.cardBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = modeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OverdriveTheme.colors.textPrimary else OverdriveTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Speed Range Filter
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Hız Aralığı Filtresi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                Text(
                    text = "Kör nokta kamerasının yalnızca belirli hız aralıklarında açılmasını sağlayabilirsiniz (0 = sınırsız).",
                    style = MaterialTheme.typography.bodySmall,
                    color = OverdriveTheme.colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = if (state.bsMinSpeedKmh > 0) state.bsMinSpeedKmh.toString() else "",
                        onValueChange = { onBsMinSpeedChange(it.toIntOrNull() ?: 0) },
                        label = { Text("Minimum Hız (km/s)", color = OverdriveTheme.colors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OverdriveTheme.colors.primary,
                            unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                            focusedTextColor = OverdriveTheme.colors.textPrimary,
                            unfocusedTextColor = OverdriveTheme.colors.textPrimary
                        )
                    )

                    OutlinedTextField(
                        value = if (state.bsMaxSpeedKmh > 0) state.bsMaxSpeedKmh.toString() else "",
                        onValueChange = { onBsMaxSpeedChange(it.toIntOrNull() ?: 0) },
                        label = { Text("Maksimum Hız (km/s)", color = OverdriveTheme.colors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OverdriveTheme.colors.primary,
                            unfocusedBorderColor = OverdriveTheme.colors.cardBorder,
                            focusedTextColor = OverdriveTheme.colors.textPrimary,
                            unfocusedTextColor = OverdriveTheme.colors.textPrimary
                        )
                    )
                }

                // Suppress in Reverse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Geri Viteste Gizle",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Geri manevra sırasında aracın orijinal 360 kamerasını engellememek için kartı gizler.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = state.bsSuppressReverse,
                        onCheckedChange = onToggleBsSuppressReverse,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverdriveTheme.colors.primary,
                            checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                            uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                        )
                    )
                }
            }
        }

        // Fisheye Dewarp Slider
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Balıkgözü Lens Düzeltmesi (Dewarp)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.textPrimary
                    )
                    Text(
                        text = "%${state.bsRectifyStrength}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OverdriveTheme.colors.primary
                    )
                }
                Slider(
                    value = state.bsRectifyStrength.toFloat(),
                    onValueChange = { onBsRectifyChange(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = OverdriveTheme.colors.primary,
                        activeTrackColor = OverdriveTheme.colors.primary
                    )
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB 5: DATA & CROWDSOURCING
// -----------------------------------------------------------------------------
@Composable
private fun DataSyncTabContent(
    state: RoadSenseUiState,
    onToggleCrowdUpload: (Boolean) -> Unit,
    onToggleCrowdDownload: (Boolean) -> Unit,
    onClearLocal: () -> Unit,
    onClearCloud: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Crowdsource Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Topluluk Tehlike Paylaşımı (Crowdsource)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.textPrimary
                )

                Text(
                    text = "Diğer Overdrive kullanıcılarının karşılaştığı tehlikeleri indirebilir veya kendi tespit ettiğiniz zemin kusurlarını anonim olarak paylaşabilirsiniz.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OverdriveTheme.colors.textSecondary
                )

                // Download
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Topluluk Tehlikelerini İndir",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Buluttaki onaylanmış kasis ve çukurları cihazınıza indirerek uyarı almanızı sağlar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = state.crowdDownload,
                        onCheckedChange = onToggleCrowdDownload,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverdriveTheme.colors.primary,
                            checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                            uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                        )
                    )
                }

                HorizontalDivider(color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.3f))

                // Upload
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tespit Edilen Tehlikeleri Paylaş (Yükle)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Yüksek güvenilirlikli tehlike tespitlerinizi anonim olarak buluta gönderir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = state.crowdUpload,
                        onCheckedChange = onToggleCrowdUpload,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverdriveTheme.colors.primary,
                            checkedTrackColor = OverdriveTheme.colors.primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = OverdriveTheme.colors.textSecondary,
                            uncheckedTrackColor = OverdriveTheme.colors.cardBorder
                        )
                    )
                }
            }
        }

        // Danger Zone: Data Wipe Card
        OverdriveCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Veritabanı ve Temizleme İşlemleri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OverdriveTheme.colors.accentRed
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yerel Tehlike Verilerini Sil",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Cihazda kayıtlı tüm yerel tespitleri ve rota geçmişini sıfırlar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }

                    OverdriveButton(
                        text = "Yerel Verileri Sil",
                        onClick = onClearLocal,
                        variant = OverdriveButtonVariant.DANGER
                    )
                }

                HorizontalDivider(color = OverdriveTheme.colors.cardBorder.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Buluttaki Cihaz Kayıtlarını Sil",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OverdriveTheme.colors.textPrimary
                        )
                        Text(
                            text = "Bu cihaz tarafından buluta yüklenmiş tüm tehlikeleri sunucudan kaldırır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = OverdriveTheme.colors.textSecondary
                        )
                    }

                    OverdriveButton(
                        text = "Buluttan Sil",
                        onClick = onClearCloud,
                        variant = OverdriveButtonVariant.DANGER
                    )
                }
            }
        }
    }
}
