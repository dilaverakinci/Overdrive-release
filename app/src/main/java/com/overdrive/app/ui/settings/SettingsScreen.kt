package com.overdrive.app.ui.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.overdrive.app.R
import com.overdrive.app.communication.RemoteCommunicationSettings
import com.overdrive.app.logging.LogLevel
import com.overdrive.app.ui.component.OverdriveButton
import com.overdrive.app.ui.component.OverdriveButtonVariant
import com.overdrive.app.ui.component.OverdriveCard
import com.overdrive.app.ui.component.OverdriveDialog
import com.overdrive.app.ui.daemons.DaemonsScreen
import com.overdrive.app.ui.daemons.DaemonsUiState
import com.overdrive.app.ui.model.DaemonType
import com.overdrive.app.ui.recording.RecordingScreen
import com.overdrive.app.ui.recording.RecordingTab
import com.overdrive.app.ui.recording.RecordingUiState
import com.overdrive.app.ui.security.SettingsSecurityScreen
import com.overdrive.app.ui.security.SettingsSecurityUiState
import com.overdrive.app.ui.surveillance.SurveillanceScreen
import com.overdrive.app.ui.surveillance.SurveillanceTab
import com.overdrive.app.ui.surveillance.SurveillanceUiState
import com.overdrive.app.ui.theme.OverdriveTheme

enum class SettingsSubrailSection(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val hasChevron: Boolean = false
) {
    APPEARANCE(R.string.settings_section_appearance, R.drawable.ic_dashboard),
    RECORDING(R.string.settings_section_recording, R.drawable.ic_recording, hasChevron = true),
    SURVEILLANCE(R.string.settings_section_surveillance, R.drawable.ic_sentry, hasChevron = true),
    OVERLAY(R.string.settings_section_overlay, R.drawable.ic_overlay_rec_active),
    SECURITY(R.string.settings_section_security, R.drawable.ic_security_lock),
    DAEMONS(R.string.settings_section_daemons, R.drawable.ic_services),
    PRIVACY(R.string.settings_section_privacy, R.drawable.ic_delete);
}

data class NavigationToggleItem(
    val key: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val isVisible: Boolean
)

data class SettingsUiState(
    val currentSection: SettingsSubrailSection = SettingsSubrailSection.APPEARANCE,

    // Appearance
    val themeMode: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
    val languageLabel: String = "Türkçe",
    val languageCountText: String = "8 / 8 dil",
    val navigationOptions: List<NavigationToggleItem> = emptyList(),

    // Recording (delegated to RecordingUiState)
    val recordingState: RecordingUiState = RecordingUiState(),

    // Surveillance (delegated to SurveillanceUiState)
    val surveillanceState: SurveillanceUiState = SurveillanceUiState(),

    // Overlay
    val cameraOverlayEnabled: Boolean = true,
    val replayOverlayEnabled: Boolean = true,
    val tripOverlayEnabled: Boolean = true,
    val roadSenseOverlayEnabled: Boolean = false,
    val remoteVoiceEnabled: Boolean = true,
    val remoteAudioChannel: String = RemoteCommunicationSettings.AUDIO_CHANNEL_MEDIA,
    val remoteListenerEnabled: Boolean = false,
    val remoteOutputOverrideEnabled: Boolean = false,
    val remoteOutputLevel: Int = 70,
    val remoteMessagesEnabled: Boolean = true,
    val remoteOverlayPermissionGranted: Boolean = false,
    val remoteEmergencyDisabled: Boolean = false,

    // Security (delegated to SettingsSecurityUiState)
    val securityState: SettingsSecurityUiState = SettingsSecurityUiState(),

    // Daemons (delegated to DaemonsUiState)
    val daemonsState: DaemonsUiState = DaemonsUiState(),

    // Privacy & Data
    val storageClipsText: String = "—",
    val storageSizeText: String = "—",
    val logLevel: LogLevel = LogLevel.INFO,
    val screenshotPrivacyEnabled: Boolean = false,

    // Reset & Build
    val installedVersion: String = "",
    val appId: String = "",
    val showResetDialog: Boolean = false
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSelectSection: (SettingsSubrailSection) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenHelp: () -> Unit,

    // Appearance
    onSelectThemeMode: (Int) -> Unit,
    onToggleNavigationOption: (String, Boolean) -> Unit,
    onResetNavigationOptions: () -> Unit,

    // Recording
    onRecordingTabSelected: (RecordingTab) -> Unit,
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
    onToggleNativeDvr: () -> Unit,
    onStorageTypeSelected: (String) -> Unit,
    onStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanup: (Boolean) -> Unit,
    onToggleRecordingCdrCleanup: (Boolean) -> Unit = {},
    onRecordingCdrReservedSpaceChange: (Int) -> Unit = {},
    onRecordingCdrProtectedHoursChange: (Int) -> Unit = {},
    onRecordingCdrMinFilesKeepChange: (Int) -> Unit = {},
    onRefreshRecording: () -> Unit,

    // Surveillance
    onSurveillanceTabSelected: (SurveillanceTab) -> Unit,
    onToggleSurveillanceMaster: (Boolean) -> Unit,
    onOperatingModeSelected: (String) -> Unit,
    onArmModeSelected: (String) -> Unit,
    onAccOffModeSelected: (String) -> Unit,
    onToggleKeepUsbPower: (Boolean) -> Unit,
    onToggleMobileDataKeepAlive: (Boolean) -> Unit,
    onToggleDi5CloudKeepAlive: (Boolean) -> Unit,
    onToggleLowPowerMode: (Boolean) -> Unit = {},
    onLowSocCutoffChange: (Int) -> Unit = {},
    onToggleScreenDeterrent: (Boolean) -> Unit = {},
    onScreenDeterrentDurationChange: (Int) -> Unit = {},
    onScreenDeterrentMessageChange: (String) -> Unit = {},
    onToggleSurveillanceGeocodingEnabled: (Boolean) -> Unit = {},
    onToggleSurveillanceGeocodingOnline: (Boolean) -> Unit = {},
    onSurveillanceGeocodingCustomUrlChange: (String) -> Unit = {},
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
    onToggleSurveillanceSideCamBoost: (Boolean) -> Unit,
    onToggleSurveillanceMotionHeatmap: (Boolean) -> Unit,
    onToggleSurveillanceDiscardNightMotion: (Boolean) -> Unit,
    onSurveillancePreRecordSecondsChange: (Int) -> Unit,
    onSurveillancePostRecordSecondsChange: (Int) -> Unit,
    onSurveillanceQualitySelected: (String) -> Unit,
    onSurveillanceFpsSelected: (Int) -> Unit,
    onSurveillanceCodecSelected: (String) -> Unit,
    onToggleSurveillanceTelegramPing: (Boolean) -> Unit,
    onToggleSurveillanceTelemetryOverlay: (Boolean) -> Unit = {},
    onToggleSurveillanceTelemetryField: (String, Boolean) -> Unit = { _, _ -> },
    onToggleSurveillanceOemDashcam: (Boolean) -> Unit,
    onToggleSurveillanceOemTrigger: (Boolean) -> Unit,
    onToggleSurveillanceOemAutoCleanup: (Boolean) -> Unit,
    onSurveillanceStorageTypeSelected: (String) -> Unit,
    onSurveillanceStorageLimitChange: (Int) -> Unit,
    onToggleAutoCleanupEvents: (Boolean) -> Unit,
    onToggleDiscardBrightEvents: (Boolean) -> Unit,
    onToggleSurveillanceCdrCleanup: (Boolean) -> Unit = {},
    onSurveillanceCdrReservedSpaceChange: (Int) -> Unit = {},
    onSurveillanceCdrProtectedHoursChange: (Int) -> Unit = {},
    onSurveillanceCdrMinFilesKeepChange: (Int) -> Unit = {},
    onRefreshSurveillance: () -> Unit,

    // Overlay
    onToggleCameraOverlay: (Boolean) -> Unit,
    onToggleReplayOverlay: (Boolean) -> Unit,
    onToggleTripOverlay: (Boolean) -> Unit,
    onToggleRoadSenseOverlay: (Boolean) -> Unit,
    onToggleRemoteVoice: (Boolean) -> Unit,
    onSelectRemoteAudioChannel: (String) -> Unit,
    onToggleRemoteListener: (Boolean) -> Unit,
    onToggleRemoteOutputOverride: (Boolean) -> Unit,
    onRemoteOutputLevelChange: (Int) -> Unit,
    onToggleRemoteMessages: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onTestSpeaker: () -> Unit,
    onTestMessage: () -> Unit,
    onToggleRemoteEmergency: (Boolean) -> Unit,

    // Security
    onSecurityToggleClick: () -> Unit,
    onSecurityChangePinClick: () -> Unit,
    onSecurityAutoLockClick: () -> Unit,
    onSecurityDismissDialog: () -> Unit,
    onSecuritySetPinSubmit: (String, String) -> Unit,
    onSecurityDisablePinSubmit: (String) -> Unit,
    onSecurityChangeStep1Submit: (String) -> Unit,
    onSecurityChangeStep2Submit: (String, String) -> Unit,
    onSecuritySelectAutoLock: (Long) -> Unit,

    // Daemons
    onToggleDaemon: (DaemonType, Boolean) -> Unit,
    onConfigureDaemon: (DaemonType) -> Unit,
    onDownloadDaemonLog: ((DaemonType) -> Unit)?,
    onToggleWifiAutoEnable: (Boolean) -> Unit,

    // Privacy & Data
    onSelectLogLevel: (LogLevel) -> Unit,
    onToggleScreenshotPrivacy: (Boolean) -> Unit,
    onOpenResetDialog: () -> Unit,
    onConfirmReset: () -> Unit,
    onDismissResetDialog: () -> Unit,

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
                    start = OverdriveTheme.dimensions.pagePaddingHorizontal,
                    end = OverdriveTheme.dimensions.pagePaddingHorizontal,
                    top = OverdriveTheme.dimensions.pagePaddingTop,
                    bottom = OverdriveTheme.dimensions.pagePaddingBottom
                )
        ) {
            // Top Header: Title "Ayarlar" on left, Globe & Help icons on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.rail_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenLanguagePicker,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_language),
                            contentDescription = stringResource(R.string.settings_language_card_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenHelp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_help),
                            contentDescription = stringResource(R.string.onboarding_help_cd),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Two-Pane Horizontal Body: Subrail on left, Detail Pane on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Subrail (~230dp wide)
                SettingsSubrail(
                    currentSection = state.currentSection,
                    onSelectSection = onSelectSection,
                    modifier = Modifier
                        .width(230.dp)
                        .fillMaxHeight()
                )

                // Right Detail Pane inside rounded surfaceContainerLow
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    when (state.currentSection) {
                        SettingsSubrailSection.APPEARANCE -> AppearancePane(
                            state = state,
                            onSelectThemeMode = onSelectThemeMode,
                            onOpenLanguagePicker = onOpenLanguagePicker,
                            onToggleNavigationOption = onToggleNavigationOption,
                            onResetNavigationOptions = onResetNavigationOptions
                        )

                        SettingsSubrailSection.RECORDING -> RecordingScreen(
                            state = state.recordingState,
                            onTabSelected = onRecordingTabSelected,
                            onRecordingModeSelected = onRecordingModeSelected,
                            onRecordingLayoutSelected = onRecordingLayoutSelected,
                            onToggleDashcamWindshield = onToggleDashcamWindshield,
                            onProximityTriggerLevelSelected = onProximityTriggerLevelSelected,
                            onProximityPreSecondsChange = onProximityPreSecondsChange,
                            onProximityPostSecondsChange = onProximityPostSecondsChange,
                            onToggleGeocodingEnabled = onToggleGeocodingEnabled,
                            onToggleGeocodingOnline = onToggleGeocodingOnline,
                            onGeocodingCustomUrlChange = onGeocodingCustomUrlChange,
                            onQualitySelected = onQualitySelected,
                            onCodecSelected = onCodecSelected,
                            onFpsSelected = onFpsSelected,
                            onClipDurationSelected = onClipDurationSelected,
                            onRectifyStrengthChange = onRectifyStrengthChange,
                            onToggleTelemetryOverlay = onToggleTelemetryOverlay,
                            onToggleTelemetryField = onToggleTelemetryField,
                            onToggleAudioRecording = onToggleAudioRecording,
                            onOemRecordingModeSelected = onOemRecordingModeSelected,
                            onToggleOemTelemetryOverlay = onToggleOemTelemetryOverlay,
                            onToggleNativeDvr = onToggleNativeDvr,
                            onStorageTypeSelected = onStorageTypeSelected,
                            onStorageLimitChange = onStorageLimitChange,
                            onToggleAutoCleanup = onToggleAutoCleanup,
                            onToggleCdrCleanup = onToggleRecordingCdrCleanup,
                            onCdrReservedSpaceChange = onRecordingCdrReservedSpaceChange,
                            onCdrProtectedHoursChange = onRecordingCdrProtectedHoursChange,
                            onCdrMinFilesKeepChange = onRecordingCdrMinFilesKeepChange,
                            onRefresh = onRefreshRecording,
                            showHeader = false,
                            modifier = Modifier.padding(14.dp)
                        )

                        SettingsSubrailSection.SURVEILLANCE -> SurveillanceScreen(
                            state = state.surveillanceState,
                            onTabSelected = onSurveillanceTabSelected,
                            onToggleMaster = onToggleSurveillanceMaster,
                            onOperatingModeSelected = onOperatingModeSelected,
                            onArmModeSelected = onArmModeSelected,
                            onAccOffModeSelected = onAccOffModeSelected,
                            onToggleKeepUsbPower = onToggleKeepUsbPower,
                            onToggleMobileDataKeepAlive = onToggleMobileDataKeepAlive,
                            onToggleDi5CloudKeepAlive = onToggleDi5CloudKeepAlive,
                            onToggleLowPowerMode = onToggleLowPowerMode,
                            onLowSocCutoffChange = onLowSocCutoffChange,
                            onToggleScreenDeterrent = onToggleScreenDeterrent,
                            onScreenDeterrentDurationChange = onScreenDeterrentDurationChange,
                            onScreenDeterrentMessageChange = onScreenDeterrentMessageChange,
                            onToggleGeocodingEnabled = onToggleSurveillanceGeocodingEnabled,
                            onToggleGeocodingOnline = onToggleSurveillanceGeocodingOnline,
                            onGeocodingCustomUrlChange = onSurveillanceGeocodingCustomUrlChange,
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
                            onToggleSideCamBoost = onToggleSurveillanceSideCamBoost,
                            onToggleMotionHeatmap = onToggleSurveillanceMotionHeatmap,
                            onToggleDiscardNightMotion = onToggleSurveillanceDiscardNightMotion,
                            onPreRecordSecondsChange = onSurveillancePreRecordSecondsChange,
                            onPostRecordSecondsChange = onSurveillancePostRecordSecondsChange,
                            onQualitySelected = onSurveillanceQualitySelected,
                            onFpsSelected = onSurveillanceFpsSelected,
                            onCodecSelected = onSurveillanceCodecSelected,
                            onToggleTelegramPing = onToggleSurveillanceTelegramPing,
                            onToggleTelemetryOverlay = onToggleSurveillanceTelemetryOverlay,
                            onToggleTelemetryField = onToggleSurveillanceTelemetryField,
                            onToggleOemDashcam = onToggleSurveillanceOemDashcam,
                            onToggleOemTrigger = onToggleSurveillanceOemTrigger,
                            onToggleOemAutoCleanup = onToggleSurveillanceOemAutoCleanup,
                            onStorageTypeSelected = onSurveillanceStorageTypeSelected,
                            onStorageLimitChange = onSurveillanceStorageLimitChange,
                            onToggleAutoCleanupEvents = onToggleAutoCleanupEvents,
                            onToggleDiscardBrightEvents = onToggleDiscardBrightEvents,
                            onToggleCdrCleanup = onToggleSurveillanceCdrCleanup,
                            onCdrReservedSpaceChange = onSurveillanceCdrReservedSpaceChange,
                            onCdrProtectedHoursChange = onSurveillanceCdrProtectedHoursChange,
                            onCdrMinFilesKeepChange = onSurveillanceCdrMinFilesKeepChange,
                            onRefresh = onRefreshSurveillance,
                            showHeader = false,
                            modifier = Modifier.padding(14.dp)
                        )

                        SettingsSubrailSection.OVERLAY -> OverlayPane(
                            state = state,
                            onToggleCameraOverlay = onToggleCameraOverlay,
                            onToggleReplayOverlay = onToggleReplayOverlay,
                            onToggleTripOverlay = onToggleTripOverlay,
                            onToggleRoadSenseOverlay = onToggleRoadSenseOverlay,
                            onToggleRemoteVoice = onToggleRemoteVoice,
                            onSelectRemoteAudioChannel = onSelectRemoteAudioChannel,
                            onToggleRemoteListener = onToggleRemoteListener,
                            onToggleRemoteOutputOverride = onToggleRemoteOutputOverride,
                            onRemoteOutputLevelChange = onRemoteOutputLevelChange,
                            onToggleRemoteMessages = onToggleRemoteMessages,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onTestSpeaker = onTestSpeaker,
                            onTestMessage = onTestMessage,
                            onToggleRemoteEmergency = onToggleRemoteEmergency
                        )

                        SettingsSubrailSection.SECURITY -> SettingsSecurityScreen(
                            state = state.securityState,
                            onToggleClick = onSecurityToggleClick,
                            onChangePinClick = onSecurityChangePinClick,
                            onAutoLockClick = onSecurityAutoLockClick,
                            onDismissDialog = onSecurityDismissDialog,
                            onSetPinSubmit = onSecuritySetPinSubmit,
                            onDisablePinSubmit = onSecurityDisablePinSubmit,
                            onChangeStep1Submit = onSecurityChangeStep1Submit,
                            onChangeStep2Submit = onSecurityChangeStep2Submit,
                            onSelectAutoLock = onSecuritySelectAutoLock,
                            showHeader = false,
                            modifier = Modifier.padding(14.dp)
                        )

                        SettingsSubrailSection.DAEMONS -> DaemonsScreen(
                            state = state.daemonsState,
                            onToggleDaemon = onToggleDaemon,
                            onConfigureDaemon = onConfigureDaemon,
                            onDownloadLog = onDownloadDaemonLog,
                            onToggleWifiAutoEnable = onToggleWifiAutoEnable,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )

                        SettingsSubrailSection.PRIVACY -> PrivacyPane(
                            state = state,
                            onSelectLogLevel = onSelectLogLevel,
                            onToggleScreenshotPrivacy = onToggleScreenshotPrivacy,
                            onOpenResetDialog = onOpenResetDialog
                        )
                    }
                }
            }
        }
    }

    // Reset confirmation dialog
    if (state.showResetDialog) {
        OverdriveDialog(
            onDismissRequest = onDismissResetDialog,
            title = stringResource(R.string.settings_action_reset_data),
            positiveButtonText = stringResource(R.string.settings_action_reset_data),
            onPositiveClick = onConfirmReset,
            negativeButtonText = stringResource(R.string.action_cancel),
            onNegativeClick = onDismissResetDialog
        ) {
            Text(
                text = stringResource(R.string.settings_privacy_reset_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsSubrail(
    currentSection: SettingsSubrailSection,
    onSelectSection: (SettingsSubrailSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Overline header: "AYARLAR"
        Text(
            text = stringResource(R.string.settings_subrail_overline),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        SettingsSubrailSection.values().forEach { section ->
            val isSelected = section == currentSection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    )
                    .clickable { onSelectSection(section) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(section.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(section.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (section.hasChevron) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearancePane(
    state: SettingsUiState,
    onSelectThemeMode: (Int) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onToggleNavigationOption: (String, Boolean) -> Unit,
    onResetNavigationOptions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Header
        Column(modifier = Modifier.padding(bottom = 2.dp)) {
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_appearance_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Theme Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_theme_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Auto Tile
                    ThemePreviewTile(
                        label = stringResource(R.string.settings_theme_auto),
                        isSelected = state.themeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFFE8EAED), Color(0xFF202124))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(OverdriveTheme.colors.primary)
                            )
                        }
                    }

                    // Light Tile
                    ThemePreviewTile(
                        label = stringResource(R.string.settings_theme_light),
                        isSelected = state.themeMode == AppCompatDelegate.MODE_NIGHT_NO,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectThemeMode(AppCompatDelegate.MODE_NIGHT_NO) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.1f)
                                    .background(Color(0xFFF7FAF7))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFFE4E9E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 30.dp, height = 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(OverdriveTheme.colors.primary)
                                )
                            }
                        }
                    }

                    // Dark Tile
                    ThemePreviewTile(
                        label = stringResource(R.string.settings_theme_dark),
                        isSelected = state.themeMode == AppCompatDelegate.MODE_NIGHT_YES,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectThemeMode(AppCompatDelegate.MODE_NIGHT_YES) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.1f)
                                    .background(Color(0xFF111413))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF1B3B32)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 30.dp, height = 6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(OverdriveTheme.colors.primary)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val captionRes = when (state.themeMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> R.string.settings_theme_active_light_caption
                    AppCompatDelegate.MODE_NIGHT_YES -> R.string.settings_theme_active_dark_caption
                    else -> R.string.settings_theme_active_auto_caption
                }
                Text(
                    text = stringResource(captionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Language Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp,
            onClick = onOpenLanguagePicker
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_language),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_language_card_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state.languageLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.languageCountText.ifEmpty { "8 / 8 dil destekleniyor" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Navigation Rail Visibility Section
        Text(
            text = stringResource(R.string.settings_navigation_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            text = stringResource(R.string.settings_navigation_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                state.navigationOptions.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleNavigationOption(option.key, !option.isVisible) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(option.iconRes),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = option.isVisible,
                            onCheckedChange = { checked -> onToggleNavigationOption(option.key, checked) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }

                    if (index < state.navigationOptions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_navigation_fixed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onResetNavigationOptions) {
                        Icon(
                            painter = painterResource(R.drawable.ic_update),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_navigation_reset),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewTile(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    swatch: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        swatch()

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverlayPane(
    state: SettingsUiState,
    onToggleCameraOverlay: (Boolean) -> Unit,
    onToggleReplayOverlay: (Boolean) -> Unit,
    onToggleTripOverlay: (Boolean) -> Unit,
    onToggleRoadSenseOverlay: (Boolean) -> Unit,
    onToggleRemoteVoice: (Boolean) -> Unit,
    onSelectRemoteAudioChannel: (String) -> Unit,
    onToggleRemoteListener: (Boolean) -> Unit,
    onToggleRemoteOutputOverride: (Boolean) -> Unit,
    onRemoteOutputLevelChange: (Int) -> Unit,
    onToggleRemoteMessages: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onTestSpeaker: () -> Unit,
    onTestMessage: () -> Unit,
    onToggleRemoteEmergency: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Header
        Column(modifier = Modifier.padding(bottom = 2.dp)) {
            Text(
                text = stringResource(R.string.settings_section_overlay),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_overlay_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Overlays Toggles Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OverlayItemRow(
                    iconRes = R.drawable.ic_recording,
                    title = stringResource(R.string.settings_overlay_camera_title),
                    subtitle = stringResource(R.string.settings_overlay_camera_subtitle),
                    checked = state.cameraOverlayEnabled,
                    onCheckedChange = onToggleCameraOverlay
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                OverlayItemRow(
                    iconRes = R.drawable.ic_overlay_replay_active,
                    title = stringResource(R.string.settings_overlay_replay_title),
                    subtitle = stringResource(R.string.settings_overlay_replay_subtitle),
                    checked = state.replayOverlayEnabled,
                    onCheckedChange = onToggleReplayOverlay
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                OverlayItemRow(
                    iconRes = R.drawable.ic_overlay_trip_active,
                    title = stringResource(R.string.settings_overlay_trip_title),
                    subtitle = stringResource(R.string.settings_overlay_trip_subtitle),
                    checked = state.tripOverlayEnabled,
                    onCheckedChange = onToggleTripOverlay
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                OverlayItemRow(
                    iconRes = R.drawable.ic_roadsense,
                    title = stringResource(R.string.settings_overlay_roadsense_title),
                    subtitle = stringResource(R.string.settings_overlay_roadsense_subtitle),
                    checked = state.roadSenseOverlayEnabled,
                    onCheckedChange = onToggleRoadSenseOverlay
                )
            }
        }

        // Remote Communication Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_overlay_mic_active),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_remote_communication_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_remote_communication_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val isEmergency = state.remoteEmergencyDisabled
                val contentAlpha = if (isEmergency) 0.4f else 1f

                // Remote Voice
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isEmergency) { onToggleRemoteVoice(!state.remoteVoiceEnabled) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_remote_voice_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Switch(
                        checked = state.remoteVoiceEnabled && !isEmergency,
                        onCheckedChange = onToggleRemoteVoice,
                        enabled = !isEmergency
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Audio Channel Segmented Controls
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(R.string.settings_remote_audio_channel_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Text(
                        text = stringResource(R.string.settings_remote_audio_channel_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isMedia = state.remoteAudioChannel == RemoteCommunicationSettings.AUDIO_CHANNEL_MEDIA
                        OutlinedSegmentButton(
                            text = stringResource(R.string.settings_remote_audio_channel_media),
                            isSelected = isMedia,
                            enabled = !isEmergency,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectRemoteAudioChannel(RemoteCommunicationSettings.AUDIO_CHANNEL_MEDIA) }
                        )

                        OutlinedSegmentButton(
                            text = stringResource(R.string.settings_remote_audio_channel_navigation),
                            isSelected = !isMedia,
                            enabled = !isEmergency,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectRemoteAudioChannel(RemoteCommunicationSettings.AUDIO_CHANNEL_NAVIGATION) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Remote Listener
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isEmergency) { onToggleRemoteListener(!state.remoteListenerEnabled) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_remote_listener_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                        )
                        Text(
                            text = stringResource(R.string.settings_remote_listener_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                    }
                    Switch(
                        checked = state.remoteListenerEnabled && !isEmergency,
                        onCheckedChange = onToggleRemoteListener,
                        enabled = !isEmergency
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Output Override Switch & Slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isEmergency) { onToggleRemoteOutputOverride(!state.remoteOutputOverrideEnabled) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_remote_output_override_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                        )
                        Text(
                            text = stringResource(R.string.settings_remote_output_override_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                        )
                    }
                    Switch(
                        checked = state.remoteOutputOverrideEnabled && !isEmergency,
                        onCheckedChange = onToggleRemoteOutputOverride,
                        enabled = !isEmergency
                    )
                }

                if (state.remoteOutputOverrideEnabled && !isEmergency) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_remote_output_level),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "%${state.remoteOutputLevel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = state.remoteOutputLevel.toFloat(),
                        onValueChange = { onRemoteOutputLevelChange(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Remote Messages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isEmergency) { onToggleRemoteMessages(!state.remoteMessagesEnabled) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_remote_messages_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                    Switch(
                        checked = state.remoteMessagesEnabled && !isEmergency,
                        onCheckedChange = onToggleRemoteMessages,
                        enabled = !isEmergency
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Overlay Permission Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestOverlayPermission() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_remote_overlay_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            if (state.remoteOverlayPermissionGranted) R.string.settings_remote_overlay_allowed
                            else R.string.settings_remote_overlay_required
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.remoteOverlayPermissionGranted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverdriveButton(
                        text = stringResource(R.string.settings_remote_test_speaker),
                        onClick = onTestSpeaker,
                        variant = OverdriveButtonVariant.OUTLINED,
                        enabled = !isEmergency && state.remoteVoiceEnabled,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_volume_on),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OverdriveButton(
                        text = stringResource(R.string.settings_remote_test_message),
                        onClick = onTestMessage,
                        variant = OverdriveButtonVariant.OUTLINED,
                        enabled = !isEmergency && state.remoteMessagesEnabled,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_notifications),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Emergency Kill Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleRemoteEmergency(!state.remoteEmergencyDisabled) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_remote_emergency_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.settings_remote_emergency_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = state.remoteEmergencyDisabled,
                        onCheckedChange = onToggleRemoteEmergency,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onError,
                            checkedTrackColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayItemRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp)
            )
        }

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

@Composable
private fun OutlinedSegmentButton(
    text: String,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

@Composable
private fun PrivacyPane(
    state: SettingsUiState,
    onSelectLogLevel: (LogLevel) -> Unit,
    onToggleScreenshotPrivacy: (Boolean) -> Unit,
    onOpenResetDialog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Header
        Column(modifier = Modifier.padding(bottom = 2.dp)) {
            Text(
                text = stringResource(R.string.settings_privacy_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.settings_privacy_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Stance Card
        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = stringResource(R.string.settings_privacy_stance_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_privacy_stance_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Local Storage Overline & Card
        Text(
            text = stringResource(R.string.settings_privacy_overline_storage),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )

        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_recording),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_storage_clips_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = state.storageClipsText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dashboard),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_storage_size_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = state.storageSizeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Diagnostics Log Level Overline & Card
        Text(
            text = stringResource(R.string.settings_privacy_overline_diagnostics),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )

        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_console),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_log_level_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        LogLevel.DEBUG to R.string.settings_privacy_log_level_debug,
                        LogLevel.INFO to R.string.settings_privacy_log_level_info,
                        LogLevel.WARN to R.string.settings_privacy_log_level_warn,
                        LogLevel.ERROR to R.string.settings_privacy_log_level_error
                    ).forEach { (level, stringId) ->
                        OutlinedSegmentButton(
                            text = stringResource(stringId),
                            isSelected = state.logLevel == level,
                            enabled = true,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectLogLevel(level) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val descRes = when (state.logLevel) {
                    LogLevel.DEBUG -> R.string.settings_privacy_log_level_debug_desc
                    LogLevel.INFO -> R.string.settings_privacy_log_level_info_desc
                    LogLevel.WARN -> R.string.settings_privacy_log_level_warn_desc
                    LogLevel.ERROR -> R.string.settings_privacy_log_level_error_desc
                }
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.logLevel == LogLevel.DEBUG) {
                    Text(
                        text = stringResource(R.string.settings_privacy_log_level_note_verbose),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else if (state.logLevel == LogLevel.WARN || state.logLevel == LogLevel.ERROR) {
                    Text(
                        text = stringResource(R.string.settings_privacy_log_level_note_quiet),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // Developer Tools Overline & Card
        Text(
            text = stringResource(R.string.settings_privacy_overline_developer_tools),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )

        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleScreenshotPrivacy(!state.screenshotPrivacyEnabled) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_security_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_screenshot_privacy_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_screenshot_privacy_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Switch(
                    checked = state.screenshotPrivacyEnabled,
                    onCheckedChange = onToggleScreenshotPrivacy
                )
            }
        }

        // Reset Overline & Danger Card
        Text(
            text = stringResource(R.string.settings_privacy_overline_reset),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
        )

        OverdriveCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = 12.dp,
            backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            borderWidth = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_action_reset_data),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = stringResource(R.string.settings_privacy_reset_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                OverdriveButton(
                    text = stringResource(R.string.settings_action_reset_data),
                    onClick = onOpenResetDialog,
                    variant = OverdriveButtonVariant.DANGER,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Build info footer
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "OverDrive ${state.installedVersion.ifEmpty { "v51.8" }} · ${state.appId.ifEmpty { "com.overdrive.app" }}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
