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
                        onToggleGeocodingEnabled = { v -> updateRecordingKey("geocodingEnabled", v) },
                        onToggleGeocodingOnline = { v -> updateRecordingKey("geocodingOnline", v) },
                        onQualitySelected = { q -> updateRecordingKey("recordingQuality", q) },
                        onCodecSelected = { c -> updateRecordingKey("recordingCodec", c) },
                        onFpsSelected = { fps -> updateCameraKey("targetFps", fps) },
                        onClipDurationSelected = { mins -> updateRecordingKey("segmentDurationMinutes", mins) },
                        onRectifyStrengthChange = { str -> updateRecordingKey("rectifyStrength", str) },
                        onToggleTelemetryOverlay = { v -> updateRecordingKey("telemetryOverlayEnabled", v) },
                        onOemRecordingModeSelected = { mode -> updateOemKey("recordingMode", mode) },
                        onToggleOemTelemetryOverlay = { v -> updateOemKey("telemetryOverlay", v) },
                        onToggleNativeDvr = { toggleNativeDvr() },
                        onStorageTypeSelected = { t -> updateRecordingKey("storageType", t) },
                        onStorageLimitChange = { lim -> updateRecordingKey("storageLimitMb", lim) },
                        onToggleAutoCleanup = { v -> updateRecordingKey("autoCleanup", v) },
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
                        onEnvironmentPresetSelected = { preset -> applyEnvironmentPreset(preset) },
                        onToggleDetectPerson = { v -> updateSurveillanceKey("detectPerson", v) },
                        onToggleDetectCar = { v -> updateSurveillanceKey("detectCar", v) },
                        onToggleDetectBike = { v -> updateSurveillanceKey("detectBike", v) },
                        onToggleDetectAnimal = { v -> updateSurveillanceKey("detectAnimal", v) },
                        onSensitivityChange = { v -> updateSurveillanceKey("sensitivityLevel", v) },
                        onDistanceChange = { v -> updateSurveillanceKey("distancePreset", v) },
                        onLoiteringTimeChange = { v -> updateSurveillanceKey("loiteringTimeSeconds", v) },
                        onToggleCameraFront = { v -> updateSurveillanceKey("cameraFront", v) },
                        onToggleCameraRight = { v -> updateSurveillanceKey("cameraRight", v) },
                        onToggleCameraLeft = { v -> updateSurveillanceKey("cameraLeft", v) },
                        onToggleCameraRear = { v -> updateSurveillanceKey("cameraRear", v) },
                        onToggleSurveillanceSideCamBoost = { v -> updateSurveillanceKey("sideCamBoost", v) },
                        onToggleSurveillanceMotionHeatmap = { v -> updateSurveillanceKey("motionHeatmap", v) },
                        onToggleSurveillanceDiscardNightMotion = { v -> updateSurveillanceKey("discardEmptyNightMotion", v) },
                        onSurveillancePreRecordSecondsChange = { v -> updateSurveillanceKey("preRecordSeconds", v) },
                        onSurveillancePostRecordSecondsChange = { v -> updateSurveillanceKey("postRecordSeconds", v) },
                        onSurveillanceQualitySelected = { q -> updateSurveillanceKey("surveillanceQuality", q) },
                        onSurveillanceFpsSelected = { fps -> updateSurveillanceKey("surveillanceCameraFps", fps) },
                        onSurveillanceCodecSelected = { c -> updateSurveillanceKey("recordingCodec", c) },
                        onToggleSurveillanceTelegramPing = { v -> updateSurveillanceKey("telegramSendStartPing", v) },
                        onToggleSurveillanceOemDashcam = { v -> updateSurveillanceKey("oemDashcamEnabled", v) },
                        onToggleSurveillanceOemTrigger = { v -> updateSurveillanceKey("oemTriggerRecording", v) },
                        onToggleSurveillanceOemAutoCleanup = { v -> updateSurveillanceKey("oemAutoCleanup", v) },
                        onSurveillanceStorageTypeSelected = { t -> updateSurveillanceKey("surveillanceStorageType", t) },
                        onSurveillanceStorageLimitChange = { lim -> updateSurveillanceKey("surveillanceLimitMb", lim) },
                        onToggleAutoCleanupEvents = { v -> updateSurveillanceKey("autoCleanupEvents", v) },
                        onToggleDiscardBrightEvents = { v -> updateSurveillanceKey("discardEmptyBrightMotionEvents", v) },
                        onRefreshSurveillance = { loadSurveillanceState() },

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
                val geoEn = rec.optBoolean("geocodingEnabled", false)
                val geoOn = rec.optBoolean("geocodingOnline", false)

                val qual = rec.optString("recordingQuality", "HIGH")
                val codec = rec.optString("recordingCodec", "H264")
                val fps = cam.optInt("targetFps", 15)
                val clipMins = rec.optInt("segmentDurationMinutes", 2)
                val rectify = rec.optInt("rectifyStrength", 0)
                val telemetry = rec.optBoolean("telemetryOverlayEnabled", true)

                val oemMode = oem.optString("recordingMode", "off")
                val oemTelem = oem.optBoolean("telemetryOverlay", false)

                val storType = rec.optString("storageType", "INTERNAL")
                val storLimit = rec.optInt("storageLimitMb", 20000)
                val autoClean = rec.optBoolean("autoCleanup", true)

                var curState = if (mode != "NONE") "Etkin ($mode)" else "Boşta (Idle)"
                var isRec = false
                var recToday = 0

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
                            storageType = storType,
                            storageLimitMb = storLimit,
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
            "recordingCodec" -> curr.copy(recordingCodec = value as String)
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
        }
        executor.execute {
            try { UnifiedConfigManager.updateValues("camera", mapOf(key to value)) } catch (_: Throwable) {}
        }
    }

    private fun updateOemKey(key: String, value: Any) {
        val curr = uiState.recordingState
        val updated = when (key) {
            "recordingMode" -> curr.copy(oemRecordingMode = value as String)
            "telemetryOverlay" -> curr.copy(oemTelemetryOverlay = value as Boolean)
            else -> curr
        }
        uiState = uiState.copy(recordingState = updated)
        executor.execute {
            try { UnifiedConfigManager.updateValues("oem", mapOf(key to value)) } catch (_: Throwable) {}
        }
    }

    private fun toggleNativeDvr() {
        val next = !uiState.recordingState.nativeDvrDisabled
        uiState = uiState.copy(recordingState = uiState.recordingState.copy(nativeDvrDisabled = next))
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/oem-dashcam/native-dvr/toggle", "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
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
                val isMaster = surv.optBoolean("surveillanceEnabled", false)
                val opMode = surv.optString("operatingMode", "AUTO")
                val arm = surv.optString("armMode", "PARKED_LOCKED")
                val accOff = surv.optString("accOffMode", "STANDBY")
                val usbPower = surv.optBoolean("keepUsbPowerOnAccOff", true)
                val mobData = surv.optBoolean("mobileDataKeepAlive", false)
                val di5 = surv.optBoolean("di5CloudKeepAlive", false)
                val env = surv.optString("environmentPreset", "BALANCED")
                val person = surv.optBoolean("detectPerson", true)
                val car = surv.optBoolean("detectCar", true)
                val bike = surv.optBoolean("detectBike", true)
                val animal = surv.optBoolean("detectAnimal", false)
                val sens = surv.optInt("sensitivityLevel", 3)
                val dist = surv.optInt("distancePreset", 2)
                val loitering = surv.optInt("loiteringTimeSeconds", 5)
                val cFront = surv.optBoolean("cameraFront", true)
                val cRight = surv.optBoolean("cameraRight", true)
                val cLeft = surv.optBoolean("cameraLeft", true)
                val cRear = surv.optBoolean("cameraRear", true)
                val boost = surv.optBoolean("sideCamBoost", false)
                val heatmap = surv.optBoolean("motionHeatmap", false)
                val discardNight = surv.optBoolean("discardEmptyNightMotion", true)
                val preRec = surv.optInt("preRecordSeconds", 5)
                val postRec = surv.optInt("postRecordSeconds", 15)
                val qual = surv.optString("surveillanceQuality", "HIGH")
                val fps = surv.optInt("surveillanceCameraFps", 10)
                val survCodec = surv.optString("recordingCodec", "H264")
                val ping = surv.optBoolean("telegramSendStartPing", true)
                val oemDash = surv.optBoolean("oemDashcamEnabled", false)
                val oemTrig = surv.optBoolean("oemTriggerRecording", false)
                val oemClean = surv.optBoolean("oemAutoCleanup", false)
                val storType = surv.optString("surveillanceStorageType", "INTERNAL")
                val storLimit = surv.optInt("surveillanceLimitMb", 500)
                val autoClean = surv.optBoolean("autoCleanupEvents", true)
                val discardBright = surv.optBoolean("discardEmptyBrightMotionEvents", false)

                var isArmed = false
                try {
                    val conn = DaemonHttpClient.open("/api/surveillance/status", "GET", 1500, 2000)
                    if (conn.responseCode == 200) {
                        val statusObj = JSONObject(conn.inputStream.bufferedReader().readText())
                        isArmed = statusObj.optBoolean("armed", isMaster)
                    }
                    conn.disconnect()
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
                            environmentPreset = env,
                            detectPerson = person,
                            detectCar = car,
                            detectBike = bike,
                            detectAnimal = animal,
                            sensitivityLevel = sens,
                            distancePreset = dist,
                            loiteringTimeSeconds = loitering,
                            cameraFront = cFront,
                            cameraRight = cRight,
                            cameraLeft = cLeft,
                            cameraRear = cRear,
                            sideCamBoost = boost,
                            motionHeatmap = heatmap,
                            discardEmptyNightMotion = discardNight,
                            preRecordSeconds = preRec,
                            postRecordSeconds = postRec,
                            surveillanceQuality = qual,
                            surveillanceCameraFps = fps,
                            recordingCodec = survCodec,
                            telegramSendStartPing = ping,
                            oemDashcamEnabled = oemDash,
                            oemTriggerRecording = oemTrig,
                            oemAutoCleanup = oemClean,
                            storageType = storType,
                            storageLimitMb = storLimit,
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
            "environmentPreset" -> curr.copy(environmentPreset = value as String)
            "detectPerson" -> curr.copy(detectPerson = value as Boolean)
            "detectCar" -> curr.copy(detectCar = value as Boolean)
            "detectBike" -> curr.copy(detectBike = value as Boolean)
            "detectAnimal" -> curr.copy(detectAnimal = value as Boolean)
            "sensitivityLevel" -> curr.copy(sensitivityLevel = value as Int)
            "distancePreset" -> curr.copy(distancePreset = value as Int)
            "loiteringTimeSeconds" -> curr.copy(loiteringTimeSeconds = value as Int)
            "cameraFront" -> curr.copy(cameraFront = value as Boolean)
            "cameraRight" -> curr.copy(cameraRight = value as Boolean)
            "cameraLeft" -> curr.copy(cameraLeft = value as Boolean)
            "cameraRear" -> curr.copy(cameraRear = value as Boolean)
            "sideCamBoost" -> curr.copy(sideCamBoost = value as Boolean)
            "motionHeatmap" -> curr.copy(motionHeatmap = value as Boolean)
            "discardEmptyNightMotion" -> curr.copy(discardEmptyNightMotion = value as Boolean)
            "preRecordSeconds" -> curr.copy(preRecordSeconds = value as Int)
            "postRecordSeconds" -> curr.copy(postRecordSeconds = value as Int)
            "surveillanceQuality" -> curr.copy(surveillanceQuality = value as String)
            "surveillanceCameraFps" -> curr.copy(surveillanceCameraFps = value as Int)
            "recordingCodec" -> curr.copy(recordingCodec = value as String)
            "telegramSendStartPing" -> curr.copy(telegramSendStartPing = value as Boolean)
            "oemDashcamEnabled" -> curr.copy(oemDashcamEnabled = value as Boolean)
            "oemTriggerRecording" -> curr.copy(oemTriggerRecording = value as Boolean)
            "oemAutoCleanup" -> curr.copy(oemAutoCleanup = value as Boolean)
            "surveillanceStorageType" -> curr.copy(storageType = value as String)
            "surveillanceLimitMb" -> curr.copy(storageLimitMb = value as Int)
            "autoCleanupEvents" -> curr.copy(autoCleanupEvents = value as Boolean)
            "discardEmptyBrightMotionEvents" -> curr.copy(discardEmptyBrightEvents = value as Boolean)
            else -> curr
        }
        uiState = uiState.copy(surveillanceState = updated)
        executor.execute {
            try { UnifiedConfigManager.updateValues("surveillance", mapOf(key to value)) } catch (_: Throwable) {}
        }
    }

    private fun applyEnvironmentPreset(preset: String) {
        val curr = uiState.surveillanceState
        val updated = when (preset) {
            "SENSITIVE" -> curr.copy(environmentPreset = preset, sensitivityLevel = 5, distancePreset = 3, loiteringTimeSeconds = 3)
            "CALM" -> curr.copy(environmentPreset = preset, sensitivityLevel = 2, distancePreset = 1, loiteringTimeSeconds = 8)
            else -> curr.copy(environmentPreset = preset, sensitivityLevel = 3, distancePreset = 2, loiteringTimeSeconds = 5)
        }
        uiState = uiState.copy(surveillanceState = updated)
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("surveillance", mapOf(
                    "environmentPreset" to preset,
                    "sensitivityLevel" to updated.sensitivityLevel,
                    "distancePreset" to updated.distancePreset,
                    "loiteringTimeSeconds" to updated.loiteringTimeSeconds
                ))
            } catch (_: Throwable) {}
        }
    }

    private fun applySurveillanceChanges() {
        Toast.makeText(requireContext(), R.string.parking_toast_saved, Toast.LENGTH_SHORT).show()
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
        Toast.makeText(requireContext(), "${type.name} ayarları", Toast.LENGTH_SHORT).show()
    }

    private fun onDownloadDaemonLog(type: DaemonType) {
        Toast.makeText(requireContext(), "${type.name} günlüğü indiriliyor", Toast.LENGTH_SHORT).show()
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
