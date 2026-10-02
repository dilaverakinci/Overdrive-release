package com.overdrive.app.ui.settings

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.overdrive.app.BuildConfig
import com.overdrive.app.R
import com.overdrive.app.auth.PinManager
import com.overdrive.app.communication.RemoteCommunicationSettings
import com.overdrive.app.config.ConfigManager
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.logging.LogLevel
import com.overdrive.app.logging.LogManager
import com.overdrive.app.overlay.MessageOverlayService
import com.overdrive.app.overlay.OverlayPermissionChecker
import com.overdrive.app.overlay.StatusOverlayService
import com.overdrive.app.overlay.StatusOverlayUiWriter
import com.overdrive.app.roadsense.config.RoadSenseConfig
import com.overdrive.app.roadsense.overlay.RoadSenseOverlayService
import com.overdrive.app.server.LocaleManager
import com.overdrive.app.services.RemoteVoiceService
import com.overdrive.app.ui.MainActivity
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.daemons.DaemonDialogManager
import com.overdrive.app.ui.dialog.LanguagePickerDialog
import com.overdrive.app.ui.fragment.WebViewFragment
import com.overdrive.app.ui.model.DaemonType
import com.overdrive.app.ui.navigation.NavigationRailCatalog
import com.overdrive.app.ui.security.AutoLockOption
import com.overdrive.app.ui.security.SecurityDialogMode
import com.overdrive.app.ui.security.SettingsSecurityUiState
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.util.PreferencesManager
import com.overdrive.app.ui.util.RecordingScanner
import com.overdrive.app.ui.viewmodel.DaemonsViewModel
import com.overdrive.app.updater.AppUpdater
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SettingsComposeFragment : Fragment() {

    private var executorService: ExecutorService? = null
    private val executor: ExecutorService
        get() = executorService?.takeUnless { it.isShutdown }
            ?: Executors.newSingleThreadExecutor { r ->
                Thread(r, "SettingsIO").apply { isDaemon = true }
            }.also { executorService = it }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val daemonsViewModel: DaemonsViewModel by activityViewModels()

    private var uiState by mutableStateOf(SettingsUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    SettingsScreen(
                        state = uiState,
                        onSelectSection = { section ->
                            uiState = uiState.copy(currentSection = section)
                            onSectionChanged(section)
                        },
                        onOpenLanguagePicker = { showLanguageDialog() },
                        onOpenHelp = { (activity as? MainActivity)?.startOnboardingReplay() },

                        // Appearance
                        onSelectThemeMode = { mode -> selectThemeMode(mode) },
                        onToggleNavigationOption = { key, visible -> toggleNavigationOption(key, visible) },
                        onResetNavigationOptions = { resetNavigationOptions() },

                        // Recording
                        onRecordingTabSelected = { tab ->
                            uiState = uiState.copy(
                                recordingState = uiState.recordingState.copy(selectedTab = tab)
                            )
                        },
                        onRecordingModeSelected = { mode -> updateRecordingKey("recordingMode", mode) },
                        onRecordingLayoutSelected = { layout -> updateRecordingKey("recordingLayout", layout) },
                        onToggleDashcamWindshield = { v -> updateRecordingKey("dashcamUseWindshield", v) },
                        onProximityTriggerLevelSelected = { lvl -> updateRecordingKey("proximityTriggerLevel", lvl) },
                        onProximityPreSecondsChange = { v -> updateRecordingKey("proximityPreSeconds", v) },
                        onProximityPostSecondsChange = { v -> updateRecordingKey("proximityPostSeconds", v) },
                        onToggleGeocodingEnabled = { v -> updateRecordingGeocoding(enabled = v) },
                        onToggleGeocodingOnline = { v -> updateRecordingGeocoding(online = v) },
                        onGeocodingCustomUrlChange = { url -> updateGeocodingUrl(url) },
                        onQualitySelected = { q -> updateRecordingKey("recordingQuality", q) },
                        onCodecSelected = { c -> updateRecordingKey("recordingCodec", c) },
                        onFpsSelected = { fps -> updateCameraKey("targetFps", fps) },
                        onClipDurationSelected = { mins -> updateRecordingKey("segmentDurationMinutes", mins) },
                        onRectifyStrengthChange = { str -> updateRecordingKey("rectifyStrength", str) },
                        onToggleTelemetryOverlay = { v -> updateRecordingTelemetryOverlay(v) },
                        onToggleTelemetryField = { field, add -> toggleTelemetryField("pano", field, add) },
                        onToggleAudioRecording = { v -> updateAudioRecording(v) },
                        onOemRecordingModeSelected = { mode -> updateOemKey("recordingMode", mode) },
                        onToggleOemTelemetryOverlay = { v -> updateOemKey("telemetryOverlay", v) },
                        onToggleNativeDvr = { toggleNativeDvr() },
                        onStorageTypeSelected = { t -> updateRecordingStorageType(t) },
                        onStorageLimitChange = { lim -> updateRecordingStorageLimit(lim) },
                        onToggleAutoCleanup = { v -> updateRecordingKey("autoCleanup", v) },
                        onToggleRecordingCdrCleanup = { v -> updateCdrCleanup(enabled = v) },
                        onRecordingCdrReservedSpaceChange = { mb -> updateCdrCleanup(reservedMb = mb.toLong()) },
                        onRecordingCdrProtectedHoursChange = { h -> updateCdrCleanup(hours = h) },
                        onRecordingCdrMinFilesKeepChange = { count -> updateCdrCleanup(minFiles = count) },
                        onToggleRecordingOemTelemetryField = { field, add -> toggleTelemetryField("oem", field, add) },
                        onApplyRecordingChanges = { applyRecordingChanges() },
                        onRefreshRecording = { loadRecordingState() },

                        // Surveillance
                        onSurveillanceTabSelected = { tab ->
                            uiState = uiState.copy(
                                surveillanceState = uiState.surveillanceState.copy(selectedTab = tab)
                            )
                        },
                        onToggleSurveillanceMaster = { enabled -> toggleSurveillanceMaster(enabled) },
                        onOperatingModeSelected = { mode -> updateSurveillanceKey("operatingMode", mode) },
                        onArmModeSelected = { mode -> updateSurveillanceKey("armMode", mode) },
                        onAccOffModeSelected = { mode -> updateSurveillanceKey("accOffMode", mode) },
                        onToggleKeepUsbPower = { v -> updateSurveillanceKey("keepUsbPowerOnAccOff", v) },
                        onToggleMobileDataKeepAlive = { v -> updateSurveillanceKey("mobileDataKeepAlive", v) },
                        onToggleDi5CloudKeepAlive = { v -> updateSurveillanceKey("di5CloudKeepAlive", v) },
                        onToggleDiLink5KeepAlive = { v -> updateSurveillanceKey("diLink5KeepAlive", v) },
                        onToggleLowPowerMode = { v -> updateSurveillanceKey("lowPowerMode", v) },
                        onLowSocCutoffChange = { v -> updatePowerKey("lowSocCutoffPercent", v) },
                        onToggleSurveillanceSchedule = { en -> toggleSurveillanceSchedule(en) },
                        onToggleParkingIntelligence = { v -> updateSurveillanceKey("parkingIntelligenceEnabled", v) },
                        onToggleParkingStills = { v -> updateSurveillanceParkingSubSetting("snapshots", v) },
                        onToggleNeighbourTimeline = { v -> updateSurveillanceParkingSubSetting("neighbours", v) },
                        onToggleGarageSignage = { v -> updateSurveillanceParkingSubSetting("signage", v) },
                        onToggleScreenDeterrent = { v -> updateSurveillanceKey("screenDeterrentEnabled", v) },
                        onScreenDeterrentDurationChange = { v -> updateSurveillanceKey("screenDeterrentDurationSeconds", v) },
                        onScreenDeterrentMessageChange = { msg -> updateSurveillanceKey("screenDeterrentMessage", msg) },
                        onScreenDeterrentThemeChange = { t -> updateSurveillanceKey("screenDeterrentTheme", t) },
                        onToggleSurveillanceGeocodingEnabled = { v -> updateSurveillanceGeocoding(enabled = v) },
                        onToggleSurveillanceGeocodingOnline = { v -> updateSurveillanceGeocoding(online = v) },
                        onSurveillanceGeocodingCustomUrlChange = { url -> updateGeocodingUrl(url) },
                        onEnvironmentPresetSelected = { preset -> applyEnvironmentPreset(preset) },
                        onToggleDetectPerson = { v -> updateSurveillanceKey("detectPerson", v) },
                        onToggleDetectCar = { v -> updateSurveillanceKey("detectCar", v) },
                        onToggleDetectBike = { v -> updateSurveillanceKey("detectBike", v) },
                        onToggleDetectAnimal = { v -> updateSurveillanceKey("detectAnimal", v) },
                        onSensitivityChange = { v -> updateSurveillanceKey("sensitivityLevel", v) },
                        onDistanceChange = { v -> updateSurveillanceKey("distancePreset", v) },
                        onLoiteringTimeChange = { v -> updateSurveillanceKey("loiteringTimeSeconds", v) },
                        onApproachTriggerChange = { v -> updateSurveillanceKey("approachTriggerSeconds", v) },
                        onShadowFilterSelected = { s -> updateSurveillanceKey("shadowFilter", s) },
                        onToggleRecordOnStrongMotion = { v -> updateSurveillanceKey("recordOnStrongMotion", v) },
                        onToggleDiscardEmptyMotion = { v -> updateSurveillanceKey("discardEmptyMotion", v) },
                        onToggleCameraFront = { v -> updateSurveillanceKey("cameraFront", v) },
                        onToggleCameraRight = { v -> updateSurveillanceKey("cameraRight", v) },
                        onToggleCameraLeft = { v -> updateSurveillanceKey("cameraLeft", v) },
                        onToggleCameraRear = { v -> updateSurveillanceKey("cameraRear", v) },
                        onToggleSurveillanceSideCamBoost = { v -> updateSurveillanceKey("sideCamBoost", v) },
                        onToggleSurveillanceDiscardNightMotion = { v -> updateSurveillanceKey("discardEmptyMotionAtNight", v) },
                        onSurveillancePreRecordSecondsChange = { v -> updateSurveillanceKey("preRecordSeconds", v) },
                        onSurveillancePostRecordSecondsChange = { v -> updateSurveillanceKey("postRecordSeconds", v) },
                        onSurveillanceQualitySelected = { q -> updateRecordingKey("surveillanceQuality", q) },
                        onSurveillanceFpsSelected = { fps -> updateCameraKey("surveillanceTargetFps", fps) },
                        onSurveillanceCodecSelected = { c -> updateRecordingKey("recordingCodec", c) },
                        onSurveillanceClipDurationSelected = { mins -> updateSurveillanceSegmentDuration(mins) },
                        onSurveillanceRecordingLayoutSelected = { l -> updateSurveillanceLayout(l) },
                        onSurveillanceRectifyStrengthChange = { s -> updateSurveillanceRectify(s) },
                        onToggleSurveillanceTelemetryOverlay = { v -> updateSurveillanceTelemetryOverlay(v) },
                        onToggleSurveillanceTelemetryField = { field, add -> toggleTelemetryField("surveillance", field, add) },
                        onSurveillanceOemRecordingModeSelected = { m -> updateOemKey("surveillanceMode", m) },
                        onToggleSurveillanceOemTelemetryOverlay = { v -> updateOemKey("telemetryOverlay", v) },
                        onToggleSurveillanceOemTelemetryField = { f, add -> toggleTelemetryField("oem", f, add) },
                        onToggleSurveillanceNativeDvr = { toggleNativeDvr() },
                        onSurveillanceStorageTypeSelected = { t -> updateSurveillanceStorageType(t) },
                        onSurveillanceStorageLimitChange = { lim -> updateSurveillanceStorageLimit(lim) },
                        onToggleAutoCleanupEvents = { v -> updateSurveillanceKey("autoCleanupEvents", v) },
                        onToggleDiscardBrightEvents = { v -> updateSurveillanceKey("discardEmptyBrightMotionEvents", v) },
                        onToggleSurveillanceCdrCleanup = { v -> updateCdrCleanup(enabled = v) },
                        onSurveillanceCdrReservedSpaceChange = { mb -> updateCdrCleanup(reservedMb = mb.toLong()) },
                        onSurveillanceCdrProtectedHoursChange = { h -> updateCdrCleanup(hours = h) },
                        onSurveillanceCdrMinFilesKeepChange = { count -> updateCdrCleanup(minFiles = count) },
                        onRefreshSurveillance = { loadSurveillanceState() },
                        onApplySurveillanceChanges = { applySurveillanceChanges() },

                        // Overlay
                        onToggleCameraOverlay = { on -> toggleOverlay("cameraVisible", on) },
                        onToggleReplayOverlay = { on -> toggleOverlay("replayVisible", on) },
                        onToggleTripOverlay = { on -> toggleOverlay("tripVisible", on) },
                        onToggleRoadSenseOverlay = { on -> toggleRoadSenseOverlay(on) },
                        onToggleRemoteVoice = { on -> updateRemoteComm { it.voiceEnabled = on } },
                        onSelectRemoteAudioChannel = { ch -> updateRemoteComm { it.audioChannel = ch } },
                        onToggleRemoteListener = { on -> updateRemoteComm { it.listenerEnabled = on } },
                        onToggleRemoteOutputOverride = { on -> updateRemoteComm { it.outputOverrideEnabled = on } },
                        onRemoteOutputLevelChange = { lvl -> updateRemoteComm { it.outputLevel = lvl } },
                        onToggleRemoteMessages = { on -> updateRemoteComm { it.messagesEnabled = on } },
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onTestSpeaker = { testSpeaker() },
                        onTestMessage = { testMessage() },
                        onToggleRemoteEmergency = { on -> updateRemoteComm { it.emergencyDisabled = on } },

                        // Security
                        onSecurityToggleClick = { onSecurityToggleClick() },
                        onSecurityChangePinClick = {
                            uiState = uiState.copy(
                                securityState = uiState.securityState.copy(
                                    dialogMode = SecurityDialogMode.CHANGE_PIN_STEP1,
                                    dialogError = null
                                )
                            )
                        },
                        onSecurityAutoLockClick = {
                            uiState = uiState.copy(
                                securityState = uiState.securityState.copy(
                                    dialogMode = SecurityDialogMode.AUTO_LOCK,
                                    dialogError = null
                                )
                            )
                        },
                        onSecurityDismissDialog = {
                            uiState = uiState.copy(
                                securityState = uiState.securityState.copy(dialogMode = null, dialogError = null)
                            )
                        },
                        onSecuritySetPinSubmit = { pin, confirm -> handleSetPinSubmit(pin, confirm) },
                        onSecurityDisablePinSubmit = { pin -> handleDisablePinSubmit(pin) },
                        onSecurityChangeStep1Submit = { currentPin -> handleChangeStep1Submit(currentPin) },
                        onSecurityChangeStep2Submit = { newPin, confirmPin -> handleChangeStep2Submit(newPin, confirmPin) },
                        onSecuritySelectAutoLock = { ms -> handleSelectAutoLock(ms) },

                        // Daemons
                        onToggleDaemon = { type, enabled -> onDaemonToggled(type, enabled) },
                        onConfigureDaemon = { type -> onDaemonConfigureClicked(type) },
                        onDownloadDaemonLog = if (BuildConfig.DEBUG || BuildConfig.LOG_CAPTURE) {
                            { type -> onDownloadDaemonLog(type) }
                        } else null,
                        onToggleWifiAutoEnable = { enabled -> persistWifiAutoEnable(enabled) },

                        // Privacy & Data
                        onSelectLogLevel = { level -> selectLogLevel(level) },
                        onToggleScreenshotPrivacy = { enabled -> toggleScreenshotPrivacy(enabled) },
                        onOpenResetDialog = { uiState = uiState.copy(showResetDialog = true) },
                        onConfirmReset = {
                            uiState = uiState.copy(showResetDialog = false)
                            (activity as? MainActivity)?.invokeResetDataDialog()
                        },
                        onDismissResetDialog = { uiState = uiState.copy(showResetDialog = false) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resolveInitialSection()
        loadAppearanceState()
        loadOverlayState()
        loadRecordingState()
        loadSurveillanceState()
        loadSecurityState()
        observeDaemons()
        loadPrivacyStorageState()
    }

    override fun onResume() {
        super.onResume()
        loadAppearanceState()
        loadOverlayState()
        loadSecurityState()
        loadPrivacyStorageState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executorService?.shutdownNow()
        executorService = null
    }

    private fun resolveInitialSection() {
        val target = arguments?.getString("settings_subrail_section")
            ?: arguments?.getString("settingsSubrailSection")
        if (!target.isNullOrEmpty()) {
            val matched = SettingsSubrailSection.values().firstOrNull {
                it.name.equals(target, ignoreCase = true)
            }
            if (matched != null) {
                uiState = uiState.copy(currentSection = matched)
            }
        }
    }

    private fun onSectionChanged(section: SettingsSubrailSection) {
        when (section) {
            SettingsSubrailSection.RECORDING -> loadRecordingState()
            SettingsSubrailSection.SURVEILLANCE -> loadSurveillanceState()
            SettingsSubrailSection.SECURITY -> loadSecurityState()
            SettingsSubrailSection.PRIVACY -> loadPrivacyStorageState()
            SettingsSubrailSection.OVERLAY -> loadOverlayState()
            else -> {}
        }
    }

    // =========================================================================
    // APPEARANCE
    // =========================================================================

    private fun loadAppearanceState() {
        val currentThemeMode = PreferencesManager.getThemeMode()

        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!locales.isEmpty) locales[0]?.toLanguageTag() else null
        val displayLocale = resources.configuration.locales[0]
        val langLabel = if (tag.isNullOrEmpty()) {
            getString(R.string.settings_theme_auto).substringBefore('(').trim()
                .ifEmpty { displayLocale.displayLanguage }
        } else {
            Locale.forLanguageTag(tag).getDisplayName(displayLocale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(displayLocale) else it.toString() }
        }

        val visibleKeys = PreferencesManager.getVisibleNavigationKeys(NavigationRailCatalog.customizableKeys)
        val navItems = NavigationRailCatalog.customizableOptions.map { opt ->
            NavigationToggleItem(
                key = opt.key,
                labelRes = opt.labelRes,
                iconRes = opt.iconRes,
                isVisible = opt.key in visibleKeys
            )
        }

        uiState = uiState.copy(
            themeMode = currentThemeMode,
            languageLabel = langLabel,
            languageCountText = getString(R.string.settings_language_count_format, LocaleManager.SUPPORTED.size, LocaleManager.SUPPORTED.size),
            navigationOptions = navItems,
            installedVersion = AppUpdater.getInstalledVersion(),
            appId = BuildConfig.APPLICATION_ID
        )
    }

    private fun selectThemeMode(mode: Int) {
        uiState = uiState.copy(themeMode = mode)
        broadcastThemeToWebViews(modeToTheme(mode))
        PreferencesManager.setThemeMode(mode)
    }

    private fun modeToTheme(mode: Int): String = when (mode) {
        AppCompatDelegate.MODE_NIGHT_NO -> "light"
        AppCompatDelegate.MODE_NIGHT_YES -> "dark"
        else -> {
            val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (night) "dark" else "light"
        }
    }

    private fun broadcastThemeToWebViews(theme: String) {
        val fm = activity?.supportFragmentManager ?: return
        collectWebViewFragments(fm).forEach { it.applyTheme(theme) }
    }

    private fun collectWebViewFragments(fm: FragmentManager): List<WebViewFragment> {
        val out = mutableListOf<WebViewFragment>()
        for (f in fm.fragments) {
            if (f is WebViewFragment) out.add(f)
            if (f.isAdded) out.addAll(collectWebViewFragments(f.childFragmentManager))
        }
        return out
    }

    private fun showLanguageDialog() {
        val act = activity ?: return
        LanguagePickerDialog.show(act) { act.recreate() }
    }

    private fun toggleNavigationOption(key: String, visible: Boolean) {
        val updated = uiState.navigationOptions.map {
            if (it.key == key) it.copy(isVisible = visible) else it
        }
        uiState = uiState.copy(navigationOptions = updated)
        PreferencesManager.setNavigationItemVisible(key, visible, NavigationRailCatalog.customizableKeys)
        (activity as? MainActivity)?.refreshNavigationRailVisibility()
    }

    private fun resetNavigationOptions() {
        PreferencesManager.resetNavigationVisibility()
        loadAppearanceState()
        (activity as? MainActivity)?.refreshNavigationRailVisibility()
        Toast.makeText(requireContext(), R.string.settings_navigation_reset_done, Toast.LENGTH_SHORT).show()
    }

    // =========================================================================
    // OVERLAYS & REMOTE COMMUNICATION
    // =========================================================================

    private fun loadOverlayState() {
        val ctx = context ?: return
        val overlaysAllowed = OverlayPermissionChecker.isGranted(ctx)
        val rsVisible = try { RoadSenseConfig.snapshot(false).overlayVisible } catch (_: Throwable) { false }

        executor.execute {
            val overlayCfg = try {
                UnifiedConfigManager.getStatusOverlay()
            } catch (_: Throwable) { null }

            val cam = overlayCfg?.optBoolean("cameraVisible", true) ?: true
            val replay = overlayCfg?.optBoolean("replayVisible", true) ?: true
            val trip = overlayCfg?.optBoolean("tripVisible", true) ?: true

            val remote = runCatching { RemoteCommunicationSettings.load() }.getOrNull()

            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    cameraOverlayEnabled = cam,
                    replayOverlayEnabled = replay,
                    tripOverlayEnabled = trip,
                    roadSenseOverlayEnabled = rsVisible,
                    remoteOverlayPermissionGranted = overlaysAllowed,
                    remoteVoiceEnabled = remote?.voiceEnabled ?: true,
                    remoteAudioChannel = remote?.audioChannel ?: RemoteCommunicationSettings.AUDIO_CHANNEL_MEDIA,
                    remoteListenerEnabled = remote?.listenerEnabled ?: false,
                    remoteOutputOverrideEnabled = remote?.outputLevelOverrideEnabled ?: false,
                    remoteOutputLevel = remote?.outputLevel ?: 70,
                    remoteMessagesEnabled = remote?.messagesEnabled ?: true,
                    remoteEmergencyDisabled = remote?.emergencyDisabled ?: false
                )
            }
        }
    }

    private fun toggleOverlay(key: String, enabled: Boolean) {
        when (key) {
            "cameraVisible" -> uiState = uiState.copy(cameraOverlayEnabled = enabled)
            "replayVisible" -> uiState = uiState.copy(replayOverlayEnabled = enabled)
            "tripVisible" -> uiState = uiState.copy(tripOverlayEnabled = enabled)
        }
        StatusOverlayUiWriter.write(key, enabled) { ok ->
            if (ok) {
                context?.let { StatusOverlayService.startIfPermitted(it) }
            } else {
                loadOverlayState()
            }
        }
    }

    private fun toggleRoadSenseOverlay(enabled: Boolean) {
        uiState = uiState.copy(roadSenseOverlayEnabled = enabled)
        StatusOverlayUiWriter.writeWith(
            "roadSense.overlayVisible",
            { ok ->
                if (ok) context?.let { RoadSenseOverlayService.syncWithConfig(it) }
                else loadOverlayState()
            }
        ) { RoadSenseConfig.setOverlayVisible(enabled) }
    }

    private class RemoteCommBuilder {
        var voiceEnabled: Boolean? = null
        var audioChannel: String? = null
        var listenerEnabled: Boolean? = null
        var outputOverrideEnabled: Boolean? = null
        var outputLevel: Int? = null
        var messagesEnabled: Boolean? = null
        var emergencyDisabled: Boolean? = null
    }

    private fun updateRemoteComm(block: (RemoteCommBuilder) -> Unit) {
        val b = RemoteCommBuilder().apply(block)
        if (b.voiceEnabled != null) uiState = uiState.copy(remoteVoiceEnabled = b.voiceEnabled!!)
        if (b.audioChannel != null) uiState = uiState.copy(remoteAudioChannel = b.audioChannel!!)
        if (b.listenerEnabled != null) uiState = uiState.copy(remoteListenerEnabled = b.listenerEnabled!!)
        if (b.outputOverrideEnabled != null) uiState = uiState.copy(remoteOutputOverrideEnabled = b.outputOverrideEnabled!!)
        if (b.outputLevel != null) uiState = uiState.copy(remoteOutputLevel = b.outputLevel!!)
        if (b.messagesEnabled != null) uiState = uiState.copy(remoteMessagesEnabled = b.messagesEnabled!!)
        if (b.emergencyDisabled != null) uiState = uiState.copy(remoteEmergencyDisabled = b.emergencyDisabled!!)

        executor.execute {
            RemoteCommunicationSettings.update(
                b.voiceEnabled,
                b.outputLevel,
                b.outputOverrideEnabled,
                b.messagesEnabled,
                b.emergencyDisabled,
                b.listenerEnabled
            )
        }
    }

    private fun requestOverlayPermission() {
        val act = activity ?: return
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${act.packageName}")
            )
            act.startActivity(intent)
        } catch (_: Throwable) {
            Toast.makeText(act, R.string.settings_remote_overlay_required, Toast.LENGTH_SHORT).show()
        }
    }

    private fun testSpeaker() {
        val ctx = context ?: return
        RemoteVoiceService.startSpeakerTest(ctx)
        Toast.makeText(ctx, R.string.settings_remote_speaker_started, Toast.LENGTH_SHORT).show()
    }

    private fun testMessage() {
        val ctx = context ?: return
        if (!OverlayPermissionChecker.isGranted(ctx)) {
            Toast.makeText(ctx, R.string.settings_remote_overlay_required, Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return
        }
        val intent = Intent(ctx, MessageOverlayService::class.java)
            .putExtra("kind", "toast")
            .putExtra("message", getString(R.string.settings_remote_test_message_body))
            .putExtra("severity", "info")
            .putExtra("position", "top")
            .putExtra("duration", "short")
        ContextCompat.startForegroundService(ctx, intent)
    }

    // =========================================================================
    // RECORDING
    // =========================================================================

    private fun loadRecordingState() {
        uiState = uiState.copy(recordingState = uiState.recordingState.copy(isLoading = true))
        executor.execute {
            try {
                val fullConfig = UnifiedConfigManager.loadConfig()
                val rec = fullConfig.optJSONObject("recording") ?: JSONObject()
                val cam = fullConfig.optJSONObject("camera") ?: JSONObject()
                val oem = fullConfig.optJSONObject("oem") ?: JSONObject()

                val mode = rec.optString("recordingMode", "NONE")
                val layout = rec.optString("recordingLayout", "standard")
                val windshield = rec.optBoolean("dashcamUseWindshield", false)
                val proxLvl = rec.optString("proximityTriggerLevel", "RED")
                val proxPre = rec.optInt("proximityPreSeconds", 5)
                val proxPost = rec.optInt("proximityPostSeconds", 10)
                val geocoding = try { UnifiedConfigManager.getGeocoding() } catch (_: Throwable) { JSONObject() }
                val recGeo = geocoding.optJSONObject("recording") ?: JSONObject()
                val advGeo = geocoding.optJSONObject("advanced") ?: JSONObject()
                val geoEn = recGeo.optBoolean("enabled", false)
                val geoOn = recGeo.optBoolean("allowOnline", false)
                val geoUrl = advGeo.optString("customNominatimBase", "")

                val audioEn = rec.optBoolean("audioEnabled", false)

                val telemetryArray = try { UnifiedConfigManager.getTelemetryOverlayFields("pano") } catch (_: Throwable) { null }
                val telemetrySet = mutableSetOf<String>()
                if (telemetryArray != null) {
                    for (i in 0 until telemetryArray.length()) {
                        telemetrySet.add(telemetryArray.optString(i))
                    }
                } else {
                    telemetrySet.addAll(listOf("speed", "time", "lat_lon", "gear", "battery_12v", "soc"))
                }

                val qual = rec.optString("recordingQuality", "HIGH")
                val codec = rec.optString("recordingCodec", "H264")
                val fps = cam.optInt("targetFps", 15)
                val clipMins = rec.optInt("segmentDurationMinutes", 2)
                val rectify = rec.optInt("rectifyStrength", 0)
                val telemetry = rec.optBoolean("telemetryOverlayEnabled", true)

                var oemMode = oem.optString("recordingMode", "off")
                val oemTelem = oem.optBoolean("telemetryOverlay", false)
                val oemTelemArray = try { oem.optJSONArray("telemetryFields") } catch (_: Throwable) { null }
                val oemTelemSet = mutableSetOf<String>()
                if (oemTelemArray != null) {
                    for (i in 0 until oemTelemArray.length()) oemTelemSet.add(oemTelemArray.optString(i))
                } else {
                    oemTelemSet.addAll(listOf("speed", "timestamp", "location", "batteryPercent"))
                }

                val storage = try { com.overdrive.app.storage.StorageManager.getInstance() } catch (_: Throwable) { null }
                val storType = storage?.recordingsStorageType?.name ?: rec.optString("storageType", "INTERNAL")
                val storLimit = storage?.recordingsLimitMb?.toInt() ?: rec.optInt("storageLimitMb", 90000)
                val autoClean = rec.optBoolean("autoCleanup", true)

                val sdAvail = storage?.isSdCardAvailable ?: false
                val sdStatus = if (sdAvail) "SD Kartı: Kullanılabilir" else "SD Kartı: tespit edilmedi"
                val sdSpace = if (sdAvail && storage != null) {
                    "${com.overdrive.app.storage.StorageManager.formatSize(storage.sdCardFreeSpace)} ücretsiz / ${com.overdrive.app.storage.StorageManager.formatSize(storage.sdCardTotalSpace)} toplam"
                } else null

                val usbAvail = storage?.isUsbAvailable ?: false
                val usbStatus = if (usbAvail) "USB: Kullanılabilir" else "USB: tespit edilmedi"
                val usbSpace = if (usbAvail && storage != null) {
                    "${com.overdrive.app.storage.StorageManager.formatSize(storage.usbFreeSpace)} ücretsiz / ${com.overdrive.app.storage.StorageManager.formatSize(storage.usbTotalSpace)} toplam"
                } else null

                val recBytes = storage?.recordingsSize ?: 0L
                val usedText = "${com.overdrive.app.storage.StorageManager.formatSize(recBytes)} kullanılır"
                val limitText = "$storLimit MB sınırı"
                val intTotal = storage?.internalTotalSpace ?: (256L * 1024 * 1024 * 1024)
                val volumeTotalText = com.overdrive.app.storage.StorageManager.formatSize(intTotal)
                val limitBytes = storLimit.toLong() * 1024L * 1024L
                val usedPercent = if (limitBytes > 0) (recBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f) else 0f

                val cleaner = try { com.overdrive.app.storage.ExternalStorageCleaner.getInstance() } catch (_: Throwable) { null }
                val cdrClean = cleaner?.isEnabled ?: false
                val cdrReserved = (cleaner?.reservedSpaceMb ?: 2000L).toInt()
                val cdrHours = cleaner?.protectedHours ?: 24
                val cdrMin = cleaner?.minFilesKeep ?: 10

                var curState = if (mode != "NONE") "Etkin ($mode)" else "Boşta (Idle)"
                var isRec = false
                var recToday = 0
                val oemUnset = UnifiedConfigManager.resolveOemDashcamId() < 0
                var dvrInstalled = false
                var dvrDisabled = false
                var oemPipelineStatus = "Boşta"

                try {
                    val conn = DaemonHttpClient.open("/api/recordings/stats", "GET", 1500, 2000)
                    if (conn.responseCode == 200) {
                        val stats = JSONObject(conn.inputStream.bufferedReader().readText())
                        recToday = stats.optInt("countToday", 0)
                        val engine = stats.optString("state", "")
                        if (engine.isNotBlank()) {
                            curState = engine
                            isRec = engine.contains("REC", ignoreCase = true) || engine.contains("RECORD", ignoreCase = true)
                        }
                    }
                    conn.disconnect()
                } catch (_: Throwable) {}

                try {
                    val dvrConn = DaemonHttpClient.open("/api/oem-dashcam/native-dvr/status", "GET", 1500, 2000)
                    if (dvrConn.responseCode == 200) {
                        val dvrBody = dvrConn.inputStream.bufferedReader().readText()
                        val dvrJson = JSONObject(dvrBody)
                        val dvrState = dvrJson.optString("state", "")
                        dvrInstalled = dvrState != "not_installed"
                        dvrDisabled = dvrState == "disabled"
                    }
                    dvrConn.disconnect()
                } catch (_: Throwable) {}

                try {
                    val oemConn = DaemonHttpClient.open("/api/oem-dashcam/config", "GET", 1500, 2000)
                    if (oemConn.responseCode == 200) {
                        val oemBody = oemConn.inputStream.bufferedReader().readText()
                        val oemJson = JSONObject(oemBody)
                        oemMode = oemJson.optString("recordingMode", oemMode)
                        val isRunning = oemJson.optBoolean("pipelineRunning", false)
                        val isRecording = oemJson.optBoolean("recording", false)
                        oemPipelineStatus = when {
                            isRecording -> "Kaydediliyor"
                            isRunning -> "Aktif"
                            else -> "Boşta"
                        }
                    }
                    oemConn.disconnect()
                } catch (_: Throwable) {}

                mainHandler.post {
                    if (!isAdded) return@post
                    uiState = uiState.copy(
                        recordingState = uiState.recordingState.copy(
                            recordingMode = mode,
                            recordingLayout = layout,
                            dashcamUseWindshield = windshield,
                            proximityTriggerLevel = proxLvl,
                            proximityPreSeconds = proxPre,
                            proximityPostSeconds = proxPost,
                            geocodingEnabled = geoEn,
                            geocodingOnline = geoOn,
                            geocodingCustomUrl = geoUrl,
                            audioRecordingEnabled = audioEn,
                            telemetryFields = telemetrySet,
                            cdrCleanupEnabled = cdrClean,
                            cdrReservedSpaceMb = cdrReserved,
                            cdrProtectedHours = cdrHours,
                            cdrMinFilesKeep = cdrMin,
                            currentState = curState,
                            isRecording = isRec,
                            recordingsToday = recToday,
                            recordingQuality = qual,
                            recordingCodec = codec,
                            targetFps = fps,
                            segmentDurationMinutes = clipMins,
                            rectifyStrength = rectify,
                            telemetryOverlayEnabled = telemetry,
                            oemRecordingMode = oemMode,
                            oemTelemetryOverlay = oemTelem,
                            oemTelemetryFields = oemTelemSet,
                            oemPipelineStatus = oemPipelineStatus,
                            nativeDvrInstalled = dvrInstalled,
                            cameraProbeUnset = oemUnset,
                            nativeDvrDisabled = dvrDisabled,
                            storageType = storType,
                            storageLimitMb = storLimit,
                            storageUsedText = usedText,
                            storageLimitText = limitText,
                            storageVolumeTotalText = volumeTotalText,
                            storageUsedPercent = usedPercent,
                            sdCardAvailable = sdAvail,
                            sdCardStatusText = sdStatus,
                            sdCardSpaceInfo = sdSpace,
                            usbAvailable = usbAvail,
                            usbStatusText = usbStatus,
                            usbSpaceInfo = usbSpace,
                            autoCleanup = autoClean,
                            isLoading = false
                        )
                    )
                }
            } catch (_: Throwable) {
                mainHandler.post {
                    if (!isAdded) return@post
                    uiState = uiState.copy(recordingState = uiState.recordingState.copy(isLoading = false))
                }
            }
        }
    }

    private fun updateRecordingKey(key: String, value: Any) {
        if (key == "surveillanceQuality") {
            uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(surveillanceQuality = value as String))
            executor.execute {
                try { UnifiedConfigManager.updateValues("recording", mapOf(key to value)) } catch (_: Throwable) {}
            }
            return
        }
        val curr = uiState.recordingState
        val updated = when (key) {
            "recordingMode" -> curr.copy(recordingMode = value as String, isRecording = (value as String) == "CONTINUOUS")
            "recordingLayout" -> curr.copy(recordingLayout = value as String)
            "dashcamUseWindshield" -> curr.copy(dashcamUseWindshield = value as Boolean)
            "proximityTriggerLevel" -> curr.copy(proximityTriggerLevel = value as String)
            "proximityPreSeconds" -> curr.copy(proximityPreSeconds = value as Int)
            "proximityPostSeconds" -> curr.copy(proximityPostSeconds = value as Int)
            "geocodingEnabled" -> curr.copy(geocodingEnabled = value as Boolean)
            "geocodingOnline" -> curr.copy(geocodingOnline = value as Boolean)
            "recordingQuality" -> curr.copy(recordingQuality = value as String)
            "recordingCodec" -> {
                uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(recordingCodec = value as String))
                curr.copy(recordingCodec = value as String)
            }
            "segmentDurationMinutes" -> curr.copy(segmentDurationMinutes = value as Int)
            "rectifyStrength" -> curr.copy(rectifyStrength = value as Int)
            "telemetryOverlayEnabled" -> curr.copy(telemetryOverlayEnabled = value as Boolean)
            "storageType" -> curr.copy(storageType = value as String)
            "storageLimitMb" -> curr.copy(storageLimitMb = value as Int)
            "autoCleanup" -> curr.copy(autoCleanup = value as Boolean)
            else -> curr
        }
        uiState = uiState.copy(recordingState = updated)
        executor.execute {
            try { UnifiedConfigManager.updateValues("recording", mapOf(key to value)) } catch (_: Throwable) {}
        }
    }

    private fun updateCameraKey(key: String, value: Any) {
        if (key == "targetFps") {
            uiState = uiState.copy(recordingState = uiState.recordingState.copy(targetFps = value as Int))
        } else if (key == "surveillanceTargetFps") {
            uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(surveillanceCameraFps = value as Int))
        }
        executor.execute {
            try { UnifiedConfigManager.updateValues("camera", mapOf(key to value)) } catch (_: Throwable) {}
        }
    }

    private fun updateOemKey(key: String, value: Any) {
        val currRec = uiState.recordingState
        val updatedRec = when (key) {
            "recordingMode" -> currRec.copy(oemRecordingMode = value as String)
            "telemetryOverlay" -> currRec.copy(oemTelemetryOverlay = value as Boolean)
            else -> currRec
        }
        val currSurv = uiState.surveillanceState
        val updatedSurv = when (key) {
            "surveillanceMode", "recordingMode" -> currSurv.copy(oemRecordingMode = value as String)
            "telemetryOverlay" -> currSurv.copy(oemTelemetryOverlay = value as Boolean)
            else -> currSurv
        }
        uiState = uiState.copy(recordingState = updatedRec, surveillanceState = updatedSurv)
        executor.execute {
            try {
                if (key == "recordingMode" || key == "surveillanceMode") {
                    try {
                        val payload = JSONObject().apply { put(key, value) }.toString()
                        val conn = DaemonHttpClient.open("/api/oem-dashcam/config", "POST", 2000, 3000)
                        conn.doOutput = true
                        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                        conn.responseCode
                        conn.disconnect()
                    } catch (_: Throwable) {}
                }
                UnifiedConfigManager.updateValues("oemDashcam", mapOf(key to value))
                UnifiedConfigManager.updateValues("oem", mapOf(key to value))
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "OEM ayarı kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleNativeDvr() {
        val next = !uiState.recordingState.nativeDvrDisabled
        uiState = uiState.copy(
            recordingState = uiState.recordingState.copy(nativeDvrDisabled = next),
            surveillanceState = uiState.surveillanceState.copy(nativeDvrDisabled = next)
        )
        executor.execute {
            try {
                val endpoint = if (next) "/api/oem-dashcam/native-dvr/disable" else "/api/oem-dashcam/native-dvr/enable"
                val conn = DaemonHttpClient.open(endpoint, "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun updateRecordingStorageType(typeStr: String) {
        uiState = uiState.copy(recordingState = uiState.recordingState.copy(storageType = typeStr))
        executor.execute {
            try {
                val smType = when (typeStr.uppercase()) {
                    "SD_CARD" -> com.overdrive.app.storage.StorageManager.StorageType.SD_CARD
                    "USB" -> com.overdrive.app.storage.StorageManager.StorageType.USB
                    else -> com.overdrive.app.storage.StorageManager.StorageType.INTERNAL
                }
                com.overdrive.app.storage.StorageManager.getInstance()?.setRecordingsStorageType(smType)
                val json = JSONObject().apply {
                    put("recordingsStorageType", typeStr)
                }.toString()
                val conn = DaemonHttpClient.open("/api/settings/storage", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("storageType" to typeStr))
                UnifiedConfigManager.updateValues("storage", mapOf("recordingsStorageType" to typeStr))
            } catch (_: Throwable) {}
        }
    }

    private fun updateRecordingStorageLimit(limitMb: Int) {
        val recBytes = try { com.overdrive.app.storage.StorageManager.getInstance()?.recordingsSize ?: 0L } catch (_: Throwable) { 0L }
        val limitBytes = limitMb.toLong() * 1024L * 1024L
        val usedPercent = if (limitBytes > 0) (recBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f) else 0f
        uiState = uiState.copy(
            recordingState = uiState.recordingState.copy(
                storageLimitMb = limitMb,
                storageLimitText = "$limitMb MB sınırı",
                storageUsedPercent = usedPercent
            )
        )
        executor.execute {
            try {
                com.overdrive.app.storage.StorageManager.getInstance()?.setRecordingsLimitMb(limitMb.toLong())
                val json = JSONObject().apply {
                    put("recordingsLimitMb", limitMb)
                }.toString()
                val conn = DaemonHttpClient.open("/api/settings/storage", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("storageLimitMb" to limitMb))
                UnifiedConfigManager.updateValues("storage", mapOf("recordingsLimitMb" to limitMb))
            } catch (_: Throwable) {}
        }
    }

    private fun applyRecordingChanges() {
        Toast.makeText(requireContext(), R.string.parking_toast_saved, Toast.LENGTH_SHORT).show()
    }

    // =========================================================================
    // SURVEILLANCE
    // =========================================================================

    private fun loadSurveillanceState() {
        uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(isLoading = true))
        executor.execute {
            try {
                val fullConfig = UnifiedConfigManager.loadConfig()
                val surv = fullConfig.optJSONObject("surveillance") ?: JSONObject()
                val rec = fullConfig.optJSONObject("recording") ?: JSONObject()
                val cam = fullConfig.optJSONObject("camera") ?: JSONObject()
                val oem = fullConfig.optJSONObject("oem") ?: JSONObject()

                val isMaster = surv.optBoolean("surveillanceEnabled", UnifiedConfigManager.isSurveillanceEnabled())
                val opMode = surv.optString("operatingMode", "onAndOff")
                val arm = surv.optString("armMode", "lock")
                val accOff = surv.optString("accOffMode", "smart")
                val usbPower = surv.optBoolean("keepUsbPowerOnAccOff", true)
                val mobData = surv.optBoolean("mobileDataKeepAlive", false)
                val di5 = surv.optBoolean("di5CloudKeepAlive", false)
                val diLink5 = surv.optBoolean("diLink5KeepAlive", false)
                val parking = fullConfig.optJSONObject("parking") ?: JSONObject()
                val parkingIntel = parking.optBoolean("enabled", surv.optBoolean("parkingIntelligenceEnabled", false))
                val parkingStills = parking.optBoolean("snapshots", true)
                val neighbourTimeline = parking.optBoolean("neighbours", true)
                val garageSignage = parking.optBoolean("signage", true)
                val scheduleEn = surv.optBoolean("scheduleEnabled", false)

                val env = surv.optString("environmentPreset", "outdoor")
                val person = surv.optBoolean("detectPerson", true)
                val car = surv.optBoolean("detectCar", true)
                val bike = surv.optBoolean("detectBike", true)
                val animal = surv.optBoolean("detectAnimal", false)
                val sens = surv.optInt("sensitivityLevel", 3)
                val dist = surv.optInt("distancePreset", 3)
                val loitering = surv.optInt("loiteringTimeSeconds", 3)
                val approachTrigger = surv.optInt("approachTriggerSeconds", 0)
                val shadow = surv.optString("shadowFilter", "off")
                val strongMotion = surv.optBoolean("recordOnStrongMotion", true)
                val emptyMotion = surv.optBoolean("discardEmptyMotion", true)
                val discardNight = surv.optBoolean("discardEmptyMotionAtNight", true)

                val cFront = surv.optBoolean("cameraFront", true)
                val cRight = surv.optBoolean("cameraRight", true)
                val cLeft = surv.optBoolean("cameraLeft", true)
                val cRear = surv.optBoolean("cameraRear", true)
                val boost = surv.optBoolean("sideCamBoost", false)

                val preRec = surv.optInt("preRecordSeconds", 5)
                val postRec = surv.optInt("postRecordSeconds", 10)
                val qual = rec.optString("surveillanceQuality", "STANDARD")
                val fps = cam.optInt("surveillanceTargetFps", 15)
                val codec = rec.optString("recordingCodec", "H264")
                val clipMins = rec.optInt("surveillanceSegmentDurationMinutes", 2)
                val layout = rec.optString("surveillanceRecordingLayout", "standard")
                val rectify = rec.optInt("surveillanceRectifyStrength", 0)

                val oemMode = oem.optString("recordingMode", "off")
                val oemTelem = oem.optBoolean("telemetryOverlay", false)
                val oemTelemArray = try { oem.optJSONArray("telemetryFields") } catch (_: Throwable) { null }
                val oemTelemSet = mutableSetOf<String>()
                if (oemTelemArray != null) {
                    for (i in 0 until oemTelemArray.length()) oemTelemSet.add(oemTelemArray.optString(i))
                } else {
                    oemTelemSet.addAll(listOf("speed", "timestamp", "location", "batteryPercent"))
                }

                val storage = try { com.overdrive.app.storage.StorageManager.getInstance() } catch (_: Throwable) { null }
                val storType = storage?.surveillanceStorageType?.name ?: surv.optString("surveillanceStorageType", "INTERNAL")
                val storLimit = storage?.surveillanceLimitMb?.toInt() ?: surv.optInt("surveillanceLimitMb", 500)
                val autoClean = surv.optBoolean("autoCleanupEvents", true)
                val discardBright = surv.optBoolean("discardEmptyBrightMotionEvents", true)

                val sdAvail = storage?.isSdCardAvailable ?: false
                val sdStatus = if (sdAvail) "SD Kartı: Kullanılabilir" else "SD Kartı: tespit edilmedi"
                val sdSpace = if (sdAvail && storage != null) {
                    "${com.overdrive.app.storage.StorageManager.formatSize(storage.sdCardFreeSpace)} ücretsiz / ${com.overdrive.app.storage.StorageManager.formatSize(storage.sdCardTotalSpace)} toplam"
                } else null

                val usbAvail = storage?.isUsbAvailable ?: false
                val usbStatus = if (usbAvail) "USB: Kullanılabilir" else "USB: tespit edilmedi"
                val usbSpace = if (usbAvail && storage != null) {
                    "${com.overdrive.app.storage.StorageManager.formatSize(storage.usbFreeSpace)} ücretsiz / ${com.overdrive.app.storage.StorageManager.formatSize(storage.usbTotalSpace)} toplam"
                } else null

                val survBytes = storage?.surveillanceSize ?: 0L
                val usedText = "${com.overdrive.app.storage.StorageManager.formatSize(survBytes)} kullanılır"
                val limitText = "$storLimit MB sınırı"
                val intTotal = storage?.internalTotalSpace ?: (256L * 1024 * 1024 * 1024)
                val volumeTotalText = com.overdrive.app.storage.StorageManager.formatSize(intTotal)
                val limitBytes = storLimit.toLong() * 1024L * 1024L
                val usedPercent = if (limitBytes > 0) (survBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f) else 0f

                val powerConfig = fullConfig.optJSONObject("power") ?: JSONObject()
                val lowPower = surv.optBoolean("lowPowerMode", true)
                val lowSoc = powerConfig.optInt("lowSocCutoffPercent", 20)
                val screenDetEn = surv.optBoolean("screenDeterrentEnabled", false)
                val screenDetDur = surv.optInt("screenDeterrentDurationSeconds", 10)
                val screenDetMsg = surv.optString("screenDeterrentMessage", "")
                val screenDetTheme = surv.optString("screenDeterrentTheme", "sentry1")

                val geocoding = try { UnifiedConfigManager.getGeocoding() } catch (_: Throwable) { JSONObject() }
                val survGeo = geocoding.optJSONObject("surveillance") ?: JSONObject()
                val advGeo = geocoding.optJSONObject("advanced") ?: JSONObject()
                val survGeoEn = survGeo.optBoolean("enabled", false)
                val survGeoOn = survGeo.optBoolean("allowOnline", false)
                val survGeoUrl = advGeo.optString("customNominatimBase", "")

                val survTelemEn = try { UnifiedConfigManager.isTelemetryOverlayEnabledFor("surveillance") } catch (_: Throwable) { false }
                val survTelemArray = try { UnifiedConfigManager.getTelemetryOverlayFields("surveillance") } catch (_: Throwable) { null }
                val survTelemSet = mutableSetOf<String>()
                if (survTelemArray != null) {
                    for (i in 0 until survTelemArray.length()) {
                        survTelemSet.add(survTelemArray.optString(i))
                    }
                } else {
                    survTelemSet.addAll(listOf("time", "lat_lon", "battery_12v", "soc"))
                }

                val cleaner = try { com.overdrive.app.storage.ExternalStorageCleaner.getInstance() } catch (_: Throwable) { null }
                val cdrClean = cleaner?.isEnabled ?: false
                val cdrReserved = (cleaner?.reservedSpaceMb ?: 2000L).toInt()
                val cdrHours = cleaner?.protectedHours ?: 24
                val cdrMin = cleaner?.minFilesKeep ?: 10

                var isArmed = false
                val oemUnset = UnifiedConfigManager.resolveOemDashcamId() < 0
                var oemSurvMode = UnifiedConfigManager.getOemSurveillanceMode()
                var oemPipelineStatus = "Boşta"
                var dvrInstalled = false
                var dvrDisabled = false
                var curState = "Boşta"
                var eventsToday = 0

                try {
                    val statsConn = DaemonHttpClient.open("/api/recordings/stats", "GET", 1500, 2000)
                    if (statsConn.responseCode == 200) {
                        val statsBody = statsConn.inputStream.bufferedReader().readText()
                        val statsJson = JSONObject(statsBody)
                        val byType = statsJson.optJSONObject("byType")
                        val sentry = byType?.optJSONObject("sentry")
                        eventsToday = sentry?.optInt("todayCount", 0) ?: statsJson.optInt("sentryTodayCount", 0)
                    }
                    statsConn.disconnect()
                } catch (_: Throwable) {}

                try {
                    val conn = DaemonHttpClient.open("/api/surveillance/status", "GET", 1500, 2000)
                    if (conn.responseCode == 200) {
                        val statusObj = JSONObject(conn.inputStream.bufferedReader().readText())
                        val stObj = statusObj.optJSONObject("status") ?: statusObj
                        isArmed = stObj.optBoolean("armed", isMaster) || stObj.optBoolean("active", false)
                        val safeZone = stObj.optBoolean("safeZoneSuppressed", false) || stObj.optBoolean("inSafeZone", false)
                        val active = stObj.optBoolean("active", false) || stObj.optBoolean("recording", false)
                        curState = when {
                            safeZone -> "Güvenli Bölge"
                            active -> "Aktif"
                            else -> "Boşta"
                        }
                    }
                    conn.disconnect()
                } catch (_: Throwable) {}

                try {
                    val dvrConn = DaemonHttpClient.open("/api/oem-dashcam/native-dvr/status", "GET", 1500, 2000)
                    if (dvrConn.responseCode == 200) {
                        val dvrBody = dvrConn.inputStream.bufferedReader().readText()
                        val dvrJson = JSONObject(dvrBody)
                        val dvrState = dvrJson.optString("state", "")
                        dvrInstalled = dvrState != "not_installed"
                        dvrDisabled = dvrState == "disabled"
                    }
                    dvrConn.disconnect()
                } catch (_: Throwable) {}

                try {
                    val oemConn = DaemonHttpClient.open("/api/oem-dashcam/config", "GET", 1500, 2000)
                    if (oemConn.responseCode == 200) {
                        val oemBody = oemConn.inputStream.bufferedReader().readText()
                        val oemJson = JSONObject(oemBody)
                        oemSurvMode = oemJson.optString("surveillanceMode", oemSurvMode)
                        val isRunning = oemJson.optBoolean("pipelineRunning", false)
                        val isRecording = oemJson.optBoolean("recording", false)
                        oemPipelineStatus = when {
                            isRecording -> "Kaydediliyor"
                            isRunning -> "Aktif"
                            else -> "Boşta"
                        }
                    }
                    oemConn.disconnect()
                } catch (_: Throwable) {}

                mainHandler.post {
                    if (!isAdded) return@post
                    uiState = uiState.copy(
                        surveillanceState = uiState.surveillanceState.copy(
                            isEnabled = isMaster,
                            isArmed = isArmed,
                            operatingMode = opMode,
                            armMode = arm,
                            accOffMode = accOff,
                            keepUsbPowerOnAccOff = usbPower,
                            mobileDataKeepAlive = mobData,
                            di5CloudKeepAlive = di5,
                            diLink5KeepAlive = diLink5,
                            lowPowerMode = lowPower,
                            lowSocCutoff = lowSoc,
                            currentState = curState,
                            eventsToday = eventsToday,
                            scheduleEnabled = scheduleEn,
                            parkingIntelligenceEnabled = parkingIntel,
                            parkingStillsEnabled = parkingStills,
                            neighbourTimelineEnabled = neighbourTimeline,
                            garageSignageEnabled = garageSignage,
                            screenDeterrentEnabled = screenDetEn,
                            screenDeterrentDuration = screenDetDur,
                            screenDeterrentMessage = screenDetMsg,
                            screenDeterrentTheme = screenDetTheme,
                            geocodingEnabled = survGeoEn,
                            geocodingOnline = survGeoOn,
                            geocodingCustomUrl = survGeoUrl,
                            telemetryOverlayEnabled = survTelemEn,
                            telemetryFields = survTelemSet,
                            cdrCleanupEnabled = cdrClean,
                            cdrReservedSpaceMb = cdrReserved,
                            cdrProtectedHours = cdrHours,
                            cdrMinFilesKeep = cdrMin,
                            environmentPreset = env,
                            detectPerson = person,
                            detectCar = car,
                            detectBike = bike,
                            detectAnimal = animal,
                            sensitivityLevel = sens,
                            distancePreset = dist,
                            loiteringTimeSeconds = loitering,
                            approachTriggerSeconds = approachTrigger,
                            shadowFilter = shadow,
                            recordOnStrongMotion = strongMotion,
                            discardEmptyMotion = emptyMotion,
                            discardEmptyNightMotion = discardNight,
                            cameraFront = cFront,
                            cameraRight = cRight,
                            cameraLeft = cLeft,
                            cameraRear = cRear,
                            sideCamBoost = boost,
                            preRecordSeconds = preRec,
                            postRecordSeconds = postRec,
                            surveillanceQuality = qual,
                            surveillanceCameraFps = fps,
                            recordingCodec = codec,
                            segmentDurationMinutes = clipMins,
                            recordingLayout = layout,
                            rectifyStrength = rectify,
                            oemRecordingMode = oemSurvMode,
                            oemTelemetryOverlay = oemTelem,
                            oemTelemetryFields = oemTelemSet,
                            oemPipelineStatus = oemPipelineStatus,
                            nativeDvrInstalled = dvrInstalled,
                            cameraProbeUnset = oemUnset,
                            nativeDvrDisabled = dvrDisabled,
                            storageType = storType,
                            storageLimitMb = storLimit,
                            storageUsedText = usedText,
                            storageLimitText = limitText,
                            storageVolumeTotalText = volumeTotalText,
                            storageUsedPercent = usedPercent,
                            sdCardAvailable = sdAvail,
                            sdCardStatusText = sdStatus,
                            sdCardSpaceInfo = sdSpace,
                            usbAvailable = usbAvail,
                            usbStatusText = usbStatus,
                            usbSpaceInfo = usbSpace,
                            autoCleanupEvents = autoClean,
                            discardEmptyBrightEvents = discardBright,
                            isLoading = false
                        )
                    )
                }
            } catch (_: Throwable) {
                mainHandler.post {
                    if (!isAdded) return@post
                    uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(isLoading = false))
                }
            }
        }
    }

    private fun toggleSurveillanceMaster(enabled: Boolean) {
        uiState = uiState.copy(
            surveillanceState = uiState.surveillanceState.copy(isEnabled = enabled, isArmed = enabled)
        )
        executor.execute {
            try {
                UnifiedConfigManager.setSurveillanceEnabled(enabled)
                UnifiedConfigManager.updateValues("surveillance", mapOf("surveillanceEnabled" to enabled))
                val endpoint = if (enabled) "/api/surveillance/enable" else "/api/surveillance/disable"
                val conn = DaemonHttpClient.open(endpoint, "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceKey(key: String, value: Any) {
        val curr = uiState.surveillanceState
        val updated = when (key) {
            "operatingMode" -> curr.copy(operatingMode = value as String)
            "armMode" -> curr.copy(armMode = value as String)
            "accOffMode" -> curr.copy(accOffMode = value as String)
            "keepUsbPowerOnAccOff" -> curr.copy(keepUsbPowerOnAccOff = value as Boolean)
            "mobileDataKeepAlive" -> curr.copy(mobileDataKeepAlive = value as Boolean)
            "di5CloudKeepAlive" -> curr.copy(di5CloudKeepAlive = value as Boolean)
            "diLink5KeepAlive" -> curr.copy(diLink5KeepAlive = value as Boolean)
            "parkingIntelligenceEnabled" -> curr.copy(parkingIntelligenceEnabled = value as Boolean)
            "environmentPreset" -> curr.copy(environmentPreset = value as String)
            "detectPerson" -> curr.copy(detectPerson = value as Boolean)
            "detectCar" -> curr.copy(detectCar = value as Boolean)
            "detectBike" -> curr.copy(detectBike = value as Boolean)
            "detectAnimal" -> curr.copy(detectAnimal = value as Boolean)
            "sensitivityLevel" -> curr.copy(sensitivityLevel = value as Int)
            "distancePreset" -> curr.copy(distancePreset = value as Int)
            "loiteringTimeSeconds" -> curr.copy(loiteringTimeSeconds = value as Int)
            "approachTriggerSeconds" -> curr.copy(approachTriggerSeconds = value as Int)
            "shadowFilter" -> curr.copy(shadowFilter = value as String)
            "recordOnStrongMotion" -> curr.copy(recordOnStrongMotion = value as Boolean)
            "discardEmptyMotion" -> curr.copy(discardEmptyMotion = value as Boolean)
            "discardEmptyMotionAtNight" -> curr.copy(discardEmptyNightMotion = value as Boolean)
            "cameraFront" -> curr.copy(cameraFront = value as Boolean)
            "cameraRight" -> curr.copy(cameraRight = value as Boolean)
            "cameraLeft" -> curr.copy(cameraLeft = value as Boolean)
            "cameraRear" -> curr.copy(cameraRear = value as Boolean)
            "sideCamBoost" -> curr.copy(sideCamBoost = value as Boolean)
            "preRecordSeconds" -> curr.copy(preRecordSeconds = value as Int)
            "postRecordSeconds" -> curr.copy(postRecordSeconds = value as Int)
            "autoCleanupEvents" -> curr.copy(autoCleanupEvents = value as Boolean)
            "discardEmptyBrightMotionEvents" -> curr.copy(discardEmptyBrightEvents = value as Boolean)
            "lowPowerMode" -> curr.copy(lowPowerMode = value as Boolean)
            "screenDeterrentEnabled" -> curr.copy(screenDeterrentEnabled = value as Boolean)
            "screenDeterrentDurationSeconds" -> curr.copy(screenDeterrentDuration = value as Int)
            "screenDeterrentMessage" -> curr.copy(screenDeterrentMessage = value as String)
            "screenDeterrentTheme" -> curr.copy(screenDeterrentTheme = value as String)
            else -> curr
        }
        uiState = uiState.copy(surveillanceState = updated)
        executor.execute {
            try {
                if (key == "parkingIntelligenceEnabled") {
                    try {
                        val payload = JSONObject().apply { put("enabled", value) }.toString()
                        val conn = DaemonHttpClient.open("/api/parking/config", "POST", 2000, 3000)
                        conn.doOutput = true
                        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                        conn.responseCode
                        conn.disconnect()
                    } catch (_: Throwable) {}
                    try {
                        UnifiedConfigManager.updateValues("parking", mapOf("enabled" to value))
                    } catch (_: Throwable) {}
                }
                UnifiedConfigManager.updateValues("surveillance", mapOf(key to value))
            } catch (_: Throwable) {}
        }
    }

    private fun toggleSurveillanceSchedule(enabled: Boolean) {
        uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(scheduleEnabled = enabled))
        executor.execute {
            try {
                try {
                    val payload = JSONObject().apply { put("scheduleEnabled", enabled) }.toString()
                    val conn = DaemonHttpClient.open("/api/surveillance/config", "POST", 2000, 3000)
                    conn.doOutput = true
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Throwable) {}
                UnifiedConfigManager.updateValues("surveillance", mapOf("scheduleEnabled" to enabled))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceParkingSubSetting(key: String, value: Boolean) {
        val curr = uiState.surveillanceState
        val updated = when (key) {
            "snapshots" -> curr.copy(parkingStillsEnabled = value)
            "neighbours" -> curr.copy(neighbourTimelineEnabled = value)
            "signage" -> curr.copy(garageSignageEnabled = value)
            else -> curr
        }
        uiState = uiState.copy(surveillanceState = updated)
        executor.execute {
            try {
                try {
                    val payload = JSONObject().apply { put(key, value) }.toString()
                    val conn = DaemonHttpClient.open("/api/parking/config", "POST", 2000, 3000)
                    conn.doOutput = true
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Throwable) {}
                UnifiedConfigManager.updateValues("parking", mapOf(key to value))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceSegmentDuration(mins: Int) {
        uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(segmentDurationMinutes = mins))
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("surveillanceSegmentDurationMinutes" to mins))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceLayout(layout: String) {
        uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(recordingLayout = layout))
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("surveillanceRecordingLayout" to layout))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceRectify(strength: Int) {
        uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(rectifyStrength = strength))
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("surveillanceRectifyStrength" to strength))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceStorageType(typeStr: String) {
        uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(storageType = typeStr))
        executor.execute {
            try {
                val smType = when (typeStr.uppercase()) {
                    "SD_CARD" -> com.overdrive.app.storage.StorageManager.StorageType.SD_CARD
                    "USB" -> com.overdrive.app.storage.StorageManager.StorageType.USB
                    else -> com.overdrive.app.storage.StorageManager.StorageType.INTERNAL
                }
                com.overdrive.app.storage.StorageManager.getInstance()?.setSurveillanceStorageType(smType)
                val json = JSONObject().apply {
                    put("surveillanceStorageType", typeStr)
                }.toString()
                val conn = DaemonHttpClient.open("/api/settings/storage", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
            try {
                UnifiedConfigManager.updateValues("surveillance", mapOf("surveillanceStorageType" to typeStr))
                UnifiedConfigManager.updateValues("storage", mapOf("surveillanceStorageType" to typeStr))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceStorageLimit(limitMb: Int) {
        val survBytes = try { com.overdrive.app.storage.StorageManager.getInstance()?.surveillanceSize ?: 0L } catch (_: Throwable) { 0L }
        val limitBytes = limitMb.toLong() * 1024L * 1024L
        val usedPercent = if (limitBytes > 0) (survBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f) else 0f
        uiState = uiState.copy(
            surveillanceState = uiState.surveillanceState.copy(
                storageLimitMb = limitMb,
                storageLimitText = "$limitMb MB sınırı",
                storageUsedPercent = usedPercent
            )
        )
        executor.execute {
            try {
                com.overdrive.app.storage.StorageManager.getInstance()?.setSurveillanceLimitMb(limitMb.toLong())
                val json = JSONObject().apply {
                    put("surveillanceLimitMb", limitMb)
                }.toString()
                val conn = DaemonHttpClient.open("/api/settings/storage", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
            try {
                UnifiedConfigManager.updateValues("surveillance", mapOf("surveillanceLimitMb" to limitMb))
                UnifiedConfigManager.updateValues("storage", mapOf("surveillanceLimitMb" to limitMb))
            } catch (_: Throwable) {}
        }
    }

    private fun applyEnvironmentPreset(preset: String) {
        val (sens, dist) = when (preset) {
            "outdoor" -> 3 to 3
            "indoor" -> 5 to 2
            "street" -> 2 to 4
            else -> 3 to 3
        }
        val curr = uiState.surveillanceState
        val updated = curr.copy(
            environmentPreset = preset,
            sensitivityLevel = sens,
            distancePreset = dist
        )
        uiState = uiState.copy(surveillanceState = updated)
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("surveillance", mapOf(
                    "environmentPreset" to preset,
                    "sensitivityLevel" to sens,
                    "distancePreset" to dist
                ))
            } catch (_: Throwable) {}
        }
    }

    private fun applySurveillanceChanges() {
        Toast.makeText(requireContext(), R.string.parking_toast_saved, Toast.LENGTH_SHORT).show()
    }

    private fun updateRecordingGeocoding(enabled: Boolean? = null, online: Boolean? = null) {
        val curr = uiState.recordingState
        val newEnabled = enabled ?: curr.geocodingEnabled
        val newOnline = if (!newEnabled) false else (online ?: curr.geocodingOnline)
        uiState = uiState.copy(recordingState = curr.copy(geocodingEnabled = newEnabled, geocodingOnline = newOnline))
        executor.execute {
            try {
                val delta = JSONObject().apply {
                    put("recording", JSONObject().apply {
                        put("enabled", newEnabled)
                        put("allowOnline", newOnline)
                    })
                }
                val payload = delta.toString()
                val conn = DaemonHttpClient.open("/api/settings/geocoding", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                val current = UnifiedConfigManager.getGeocoding()
                val rec = current.optJSONObject("recording") ?: JSONObject()
                rec.put("enabled", newEnabled)
                rec.put("allowOnline", newOnline)
                current.put("recording", rec)
                UnifiedConfigManager.setGeocoding(current)
            }
        }
    }

    private fun updateSurveillanceGeocoding(enabled: Boolean? = null, online: Boolean? = null) {
        val curr = uiState.surveillanceState
        val newEnabled = enabled ?: curr.geocodingEnabled
        val newOnline = if (!newEnabled) false else (online ?: curr.geocodingOnline)
        uiState = uiState.copy(surveillanceState = curr.copy(geocodingEnabled = newEnabled, geocodingOnline = newOnline))
        executor.execute {
            try {
                val delta = JSONObject().apply {
                    put("surveillance", JSONObject().apply {
                        put("enabled", newEnabled)
                        put("allowOnline", newOnline)
                    })
                }
                val payload = delta.toString()
                val conn = DaemonHttpClient.open("/api/settings/geocoding", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                val current = UnifiedConfigManager.getGeocoding()
                val sur = current.optJSONObject("surveillance") ?: JSONObject()
                sur.put("enabled", newEnabled)
                sur.put("allowOnline", newOnline)
                current.put("surveillance", sur)
                UnifiedConfigManager.setGeocoding(current)
            }
        }
    }

    private fun updateGeocodingUrl(url: String) {
        uiState = uiState.copy(
            recordingState = uiState.recordingState.copy(geocodingCustomUrl = url),
            surveillanceState = uiState.surveillanceState.copy(geocodingCustomUrl = url)
        )
        executor.execute {
            try {
                val delta = JSONObject().apply {
                    put("advanced", JSONObject().apply {
                        put("customNominatimBase", url.trim())
                    })
                }
                val payload = delta.toString()
                val conn = DaemonHttpClient.open("/api/settings/geocoding", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                val current = UnifiedConfigManager.getGeocoding()
                val adv = current.optJSONObject("advanced") ?: JSONObject()
                adv.put("customNominatimBase", url.trim())
                current.put("advanced", adv)
                UnifiedConfigManager.setGeocoding(current)
            }
        }
    }

    private fun updateAudioRecording(enabled: Boolean) {
        uiState = uiState.copy(recordingState = uiState.recordingState.copy(audioRecordingEnabled = enabled))
        executor.execute {
            try {
                val payload = JSONObject().apply { put("enabled", enabled) }.toString()
                val conn = DaemonHttpClient.open("/api/settings/audio-recording", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                UnifiedConfigManager.updateValues("recording", mapOf("audioEnabled" to enabled))
            }
        }
    }

    private fun updateRecordingTelemetryOverlay(enabled: Boolean) {
        uiState = uiState.copy(recordingState = uiState.recordingState.copy(telemetryOverlayEnabled = enabled))
        executor.execute {
            try {
                val payload = JSONObject().apply {
                    put("enabled", enabled)
                    put("panoEnabled", enabled)
                }.toString()
                val conn = DaemonHttpClient.open("/api/settings/telemetry-overlay", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                UnifiedConfigManager.setTelemetryOverlay(JSONObject().apply {
                    put("enabled", enabled)
                    put("panoEnabled", enabled)
                })
                UnifiedConfigManager.updateValues("recording", mapOf("telemetryOverlayEnabled" to enabled))
            }
        }
    }

    private fun updateSurveillanceTelemetryOverlay(enabled: Boolean) {
        uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(telemetryOverlayEnabled = enabled))
        executor.execute {
            try {
                val payload = JSONObject().apply { put("surveillanceEnabled", enabled) }.toString()
                val conn = DaemonHttpClient.open("/api/settings/telemetry-overlay", "POST", 2000, 3000)
                conn.doOutput = true
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                UnifiedConfigManager.setTelemetryOverlay(JSONObject().apply { put("surveillanceEnabled", enabled) })
            }
        }
    }

    private fun toggleTelemetryField(flow: String, field: String, add: Boolean) {
        if (flow == "pano") {
            val currFields = uiState.recordingState.telemetryFields.toMutableSet()
            if (add) currFields.add(field) else currFields.remove(field)
            uiState = uiState.copy(recordingState = uiState.recordingState.copy(telemetryFields = currFields))
            executor.execute {
                val jsonArray = org.json.JSONArray(currFields.toList())
                try {
                    val flowKey = "accOn"
                    val body = JSONObject().apply {
                        put("fields", JSONObject().apply {
                            put(flowKey, jsonArray)
                        })
                    }.toString()
                    val conn = DaemonHttpClient.open("/api/settings/telemetry-overlay", "POST", 2000, 3000)
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Throwable) {
                    UnifiedConfigManager.setTelemetryOverlayFields("pano", jsonArray)
                }
            }
        } else if (flow == "oem") {
            val currFields = uiState.recordingState.oemTelemetryFields.toMutableSet()
            if (add) currFields.add(field) else currFields.remove(field)
            uiState = uiState.copy(
                recordingState = uiState.recordingState.copy(oemTelemetryFields = currFields),
                surveillanceState = uiState.surveillanceState.copy(oemTelemetryFields = currFields)
            )
            executor.execute {
                val jsonArray = org.json.JSONArray(currFields.toList())
                try {
                    UnifiedConfigManager.updateValues("oem", mapOf("telemetryFields" to jsonArray))
                } catch (_: Throwable) {}
            }
        } else {
            val currFields = uiState.surveillanceState.telemetryFields.toMutableSet()
            if (add) currFields.add(field) else currFields.remove(field)
            uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(telemetryFields = currFields))
            executor.execute {
                val jsonArray = org.json.JSONArray(currFields.toList())
                try {
                    val body = JSONObject().apply {
                        put("fields", JSONObject().apply {
                            put("surveillance", jsonArray)
                        })
                    }.toString()
                    val conn = DaemonHttpClient.open("/api/settings/telemetry-overlay", "POST", 2000, 3000)
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Throwable) {
                    UnifiedConfigManager.setTelemetryOverlayFields("surveillance", jsonArray)
                }
            }
        }
    }

    private fun updatePowerKey(key: String, value: Any) {
        if (key == "lowSocCutoffPercent") {
            uiState = uiState.copy(surveillanceState = uiState.surveillanceState.copy(lowSocCutoff = value as Int))
        }
        executor.execute {
            try { UnifiedConfigManager.updateValues("power", mapOf(key to value)) } catch (_: Throwable) {}
        }
    }

    private fun updateCdrCleanup(
        enabled: Boolean? = null,
        reservedMb: Long? = null,
        hours: Int? = null,
        minFiles: Int? = null
    ) {
        val recCurr = uiState.recordingState
        val surCurr = uiState.surveillanceState
        val nextEnabled = enabled ?: recCurr.cdrCleanupEnabled
        val nextReserved = (reservedMb?.toInt()) ?: recCurr.cdrReservedSpaceMb
        val nextHours = hours ?: recCurr.cdrProtectedHours
        val nextMinFiles = minFiles ?: recCurr.cdrMinFilesKeep

        uiState = uiState.copy(
            recordingState = recCurr.copy(
                cdrCleanupEnabled = nextEnabled,
                cdrReservedSpaceMb = nextReserved,
                cdrProtectedHours = nextHours,
                cdrMinFilesKeep = nextMinFiles
            ),
            surveillanceState = surCurr.copy(
                cdrCleanupEnabled = nextEnabled,
                cdrReservedSpaceMb = nextReserved,
                cdrProtectedHours = nextHours,
                cdrMinFilesKeep = nextMinFiles
            )
        )

        executor.execute {
            try {
                val cleaner = com.overdrive.app.storage.ExternalStorageCleaner.getInstance() ?: return@execute
                if (enabled != null) {
                    cleaner.setEnabled(enabled)
                }
                if (reservedMb != null) {
                    cleaner.setReservedSpaceMb(reservedMb)
                }
                if (hours != null) {
                    cleaner.setProtectedHours(hours)
                }
                if (minFiles != null) {
                    cleaner.setMinFilesKeep(minFiles)
                }
            } catch (_: Throwable) {}
        }
    }

    // =========================================================================
    // SECURITY & PIN
    // =========================================================================

    private val AUTO_LOCK_OPTIONS = listOf(
        0L to R.string.settings_security_autolock_immediate,
        60_000L to R.string.settings_security_autolock_1min,
        300_000L to R.string.settings_security_autolock_5min,
        900_000L to R.string.settings_security_autolock_15min,
        -1L to R.string.settings_security_autolock_never
    )

    private fun loadSecurityState() {
        val options = AUTO_LOCK_OPTIONS.map { (ms, resId) -> AutoLockOption(ms, getString(resId)) }
        executor.execute {
            val enabled = try { PinManager.isEnabled() } catch (_: Throwable) { false }
            val autoLockMs = try { PinManager.getAutoLockMs() } catch (_: Throwable) { 300_000L }
            val label = options.firstOrNull { it.ms == autoLockMs }?.label
                ?: getString(R.string.settings_security_autolock_5min)

            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    securityState = uiState.securityState.copy(
                        isEnabled = enabled,
                        autoLockMs = autoLockMs,
                        autoLockLabel = label,
                        autoLockOptions = options,
                        isLoading = false
                    )
                )
            }
        }
    }

    private fun onSecurityToggleClick() {
        executor.execute {
            val enabled = try { PinManager.isEnabled() } catch (_: Throwable) { false }
            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    securityState = uiState.securityState.copy(
                        dialogMode = if (enabled) SecurityDialogMode.DISABLE_PIN else SecurityDialogMode.SET_PIN,
                        dialogError = null
                    )
                )
            }
        }
    }

    private fun validatePinPair(pin: String, confirm: String): Int? {
        if (pin.length !in 4..8) {
            return R.string.settings_security_error_length
        }
        if (!pin.all { it.isDigit() }) {
            return R.string.settings_security_error_numeric
        }
        if (pin != confirm) {
            return R.string.settings_security_error_mismatch
        }
        return null
    }

    private fun handleSetPinSubmit(pin: String, confirm: String) {
        val err = validatePinPair(pin, confirm)
        if (err != null) {
            uiState = uiState.copy(
                securityState = uiState.securityState.copy(dialogError = getString(err))
            )
            return
        }
        uiState = uiState.copy(securityState = uiState.securityState.copy(isLoading = true, dialogError = null))
        executor.execute {
            val result = PinManager.setPin(pin)
            mainHandler.post {
                if (!isAdded) return@post
                if (result == PinManager.SetResult.OK) {
                    Toast.makeText(requireContext(), R.string.settings_security_toast_set_ok, Toast.LENGTH_SHORT).show()
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(dialogMode = null, dialogError = null)
                    )
                    loadSecurityState()
                } else {
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(
                            isLoading = false,
                            dialogError = getString(R.string.settings_security_error_persist)
                        )
                    )
                }
            }
        }
    }

    private fun handleDisablePinSubmit(pin: String) {
        if (pin.isBlank()) {
            uiState = uiState.copy(
                securityState = uiState.securityState.copy(dialogError = getString(R.string.settings_security_error_wrong))
            )
            return
        }
        uiState = uiState.copy(securityState = uiState.securityState.copy(isLoading = true, dialogError = null))
        executor.execute {
            val verify = PinManager.verify(pin)
            val disabled = if (verify == PinManager.VerifyResult.OK) PinManager.disable() else false
            mainHandler.post {
                if (!isAdded) return@post
                if (verify == PinManager.VerifyResult.OK && disabled) {
                    Toast.makeText(requireContext(), R.string.settings_security_toast_disable_ok, Toast.LENGTH_SHORT).show()
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(dialogMode = null, dialogError = null)
                    )
                    loadSecurityState()
                } else {
                    val errRes = when (verify) {
                        PinManager.VerifyResult.LOCKED_OUT -> R.string.settings_security_error_locked_out
                        else -> R.string.settings_security_error_wrong
                    }
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(isLoading = false, dialogError = getString(errRes))
                    )
                }
            }
        }
    }

    private fun handleChangeStep1Submit(currentPin: String) {
        if (currentPin.isBlank()) {
            uiState = uiState.copy(
                securityState = uiState.securityState.copy(dialogError = getString(R.string.settings_security_error_wrong))
            )
            return
        }
        uiState = uiState.copy(securityState = uiState.securityState.copy(isLoading = true, dialogError = null))
        executor.execute {
            val result = PinManager.verify(currentPin)
            mainHandler.post {
                if (!isAdded) return@post
                if (result == PinManager.VerifyResult.OK) {
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(
                            isLoading = false,
                            dialogMode = SecurityDialogMode.CHANGE_PIN_STEP2,
                            dialogError = null
                        )
                    )
                } else {
                    val errRes = if (result == PinManager.VerifyResult.LOCKED_OUT)
                        R.string.settings_security_error_locked_out else R.string.settings_security_error_wrong
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(isLoading = false, dialogError = getString(errRes))
                    )
                }
            }
        }
    }

    private fun handleChangeStep2Submit(newPin: String, confirmPin: String) {
        val err = validatePinPair(newPin, confirmPin)
        if (err != null) {
            uiState = uiState.copy(
                securityState = uiState.securityState.copy(dialogError = getString(err))
            )
            return
        }
        uiState = uiState.copy(securityState = uiState.securityState.copy(isLoading = true, dialogError = null))
        executor.execute {
            val result = PinManager.setPin(newPin)
            mainHandler.post {
                if (!isAdded) return@post
                if (result == PinManager.SetResult.OK) {
                    Toast.makeText(requireContext(), R.string.settings_security_toast_set_ok, Toast.LENGTH_SHORT).show()
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(dialogMode = null, dialogError = null)
                    )
                    loadSecurityState()
                } else {
                    uiState = uiState.copy(
                        securityState = uiState.securityState.copy(
                            isLoading = false,
                            dialogError = getString(R.string.settings_security_error_persist)
                        )
                    )
                }
            }
        }
    }

    private fun handleSelectAutoLock(ms: Long) {
        executor.execute {
            PinManager.setAutoLockMs(ms)
            mainHandler.post {
                if (!isAdded) return@post
                Toast.makeText(requireContext(), R.string.parking_toast_saved, Toast.LENGTH_SHORT).show()
                uiState = uiState.copy(
                    securityState = uiState.securityState.copy(dialogMode = null, dialogError = null)
                )
                loadSecurityState()
            }
        }
    }

    // =========================================================================
    // DAEMONS
    // =========================================================================

    private fun observeDaemons() {
        daemonsViewModel.daemonStates.observe(viewLifecycleOwner) { states ->
            val sortedList = states.values.sortedBy { it.type.ordinal }
            uiState = uiState.copy(
                daemonsState = uiState.daemonsState.copy(daemons = sortedList)
            )
        }

        refreshWifiAutoEnable()
        checkZrokTokenStatus()
    }

    private fun checkZrokTokenStatus() {
        daemonsViewModel.zrokController.hasEnableToken { hasToken ->
            activity?.runOnUiThread {
                if (!hasToken) {
                    daemonsViewModel.updateZrokNeedsConfig(getString(R.string.daemon_config_no_token))
                }
            }
        }
    }

    private fun refreshWifiAutoEnable() {
        uiState = uiState.copy(
            daemonsState = uiState.daemonsState.copy(isWifiAutoEnableLoading = true)
        )
        executor.execute {
            val enabled = runCatching {
                UnifiedConfigManager.forceReload()
                UnifiedConfigManager.isWifiAutoEnableEnabled()
            }.getOrDefault(true)

            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    daemonsState = uiState.daemonsState.copy(
                        isWifiAutoEnable = enabled,
                        isWifiAutoEnableLoading = false
                    )
                )
            }
        }
    }

    private fun persistWifiAutoEnable(enabled: Boolean) {
        uiState = uiState.copy(
            daemonsState = uiState.daemonsState.copy(isWifiAutoEnableLoading = true)
        )
        executor.execute {
            var applied = false
            try {
                val conn = DaemonHttpClient.open("/api/keymap/fire", "POST", 2000, 7000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val payload = JSONObject().put("kind", "radio").put("radio", "wifi").put("enable", enabled)
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                applied = conn.responseCode in 200..299
                conn.disconnect()
            } catch (_: Throwable) {}

            UnifiedConfigManager.forceReload()
            val persisted = UnifiedConfigManager.isWifiAutoEnableEnabled()

            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    daemonsState = uiState.daemonsState.copy(
                        isWifiAutoEnable = persisted,
                        isWifiAutoEnableLoading = false
                    )
                )
            }
        }
    }

    private fun onDaemonToggled(type: DaemonType, enabled: Boolean) {
        daemonsViewModel.daemonStartupManager?.onDaemonToggled(type, enabled)
        if (enabled) daemonsViewModel.startDaemon(type) else daemonsViewModel.stopDaemon(type)
    }

    private fun onDaemonConfigureClicked(type: DaemonType) {
        DaemonDialogManager.showConfigDialog(type, this, daemonsViewModel)
    }

    private fun onDownloadDaemonLog(type: DaemonType) {
        DaemonDialogManager.downloadLog(type, this, daemonsViewModel)
    }

    // =========================================================================
    // PRIVACY & DATA
    // =========================================================================

    private fun loadPrivacyStorageState() {
        val ctx = context?.applicationContext ?: return
        val currentLevel = ConfigManager.getInstance(ctx).getLoggingConfig().minLevel
        val screenshotPriv = UnifiedConfigManager.isScreenshotPrivacyModeEnabled()

        uiState = uiState.copy(
            logLevel = currentLevel,
            screenshotPrivacyEnabled = screenshotPriv
        )

        executor.execute {
            val scan = try {
                RecordingScanner.scanRecordings(ctx)
            } catch (_: Throwable) { null }

            mainHandler.post {
                if (!isAdded) return@post
                if (scan != null) {
                    val clips = "${scan.size} klip"
                    val totalBytes = scan.sumOf { it.sizeBytes }
                    val sizeGb = String.format(Locale.US, "%.2f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))
                    uiState = uiState.copy(storageClipsText = clips, storageSizeText = sizeGb)
                } else {
                    uiState = uiState.copy(
                        storageClipsText = getString(R.string.settings_privacy_storage_unavailable),
                        storageSizeText = getString(R.string.settings_privacy_storage_unavailable)
                    )
                }
            }
        }
    }

    private fun selectLogLevel(level: LogLevel) {
        val ctx = context?.applicationContext ?: return
        uiState = uiState.copy(logLevel = level)
        val cfg = ConfigManager.getInstance(ctx)
        val existing = cfg.getLoggingConfig()
        if (existing.minLevel != level) {
            LogManager.getInstance().warn("Settings", "Log level changed: ${existing.minLevel} -> $level")
            cfg.updateLoggingConfig(existing.copy(minLevel = level))
        }
    }

    private fun toggleScreenshotPrivacy(enabled: Boolean) {
        uiState = uiState.copy(screenshotPrivacyEnabled = enabled)
        if (UnifiedConfigManager.setScreenshotPrivacyModeEnabled(enabled)) {
            (activity as? MainActivity)?.applyScreenshotPrivacyMode(enabled)
        } else {
            loadPrivacyStorageState()
            Toast.makeText(requireContext(), R.string.settings_screenshot_privacy_save_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
