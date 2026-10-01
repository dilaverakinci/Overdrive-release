package com.overdrive.app.ui.settings

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.overdrive.app.BuildConfig
import com.overdrive.app.R
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.overlay.StatusOverlayService
import com.overdrive.app.overlay.StatusOverlayUiWriter
import com.overdrive.app.roadsense.config.RoadSenseConfig
import com.overdrive.app.roadsense.overlay.RoadSenseOverlayService
import com.overdrive.app.ui.MainActivity
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.dialog.LanguagePickerDialog
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.util.PreferencesManager
import com.overdrive.app.ui.util.navigateDrillDown
import com.overdrive.app.updater.AppUpdater
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SettingsComposeFragment : Fragment() {

    private var executorService: ExecutorService? = null
    private val executor: ExecutorService
        get() = executorService?.takeUnless { it.isShutdown } ?: Executors.newSingleThreadExecutor().also { executorService = it }
    private val mainHandler = Handler(Looper.getMainLooper())

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
                        onOpenThemePicker = { showThemeDialog() },
                        onOpenLanguagePicker = { showLanguageDialog() },
                        onNavigateRecording = { findNavController().navigateDrillDown(R.id.recordingSettingsWebFragment) },
                        onNavigateSurveillance = { findNavController().navigateDrillDown(R.id.surveillanceSettingsWebFragment) },
                        onNavigateSecurity = { findNavController().navigateDrillDown(R.id.settingsSecurityFragment) },
                        onNavigateDaemons = { findNavController().navigateDrillDown(R.id.daemonsFragment) },
                        onNavigateTelegram = { findNavController().navigateDrillDown(R.id.telegramSettingsFragment) },
                        onNavigateAbrp = { findNavController().navigateDrillDown(R.id.abrpSettingsFragment) },
                        onNavigateMqtt = { findNavController().navigateDrillDown(R.id.mqttFragment) },
                        onNavigateBydCloud = { findNavController().navigateDrillDown(R.id.bydCloudFragment) },
                        onToggleCameraOverlay = { on -> toggleOverlay("cameraVisible", on) },
                        onToggleTripOverlay = { on -> toggleOverlay("tripVisible", on) },
                        onToggleRoadSenseOverlay = { on -> toggleRoadSenseOverlay(on) },
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
        loadInitialState()
    }

    override fun onResume() {
        super.onResume()
        loadInitialState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executorService?.shutdownNow()
        executorService = null
    }

    private fun loadInitialState() {
        val currentThemeMode = PreferencesManager.getThemeMode()
        val themeLabel = when (currentThemeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_theme_dark)
            else -> getString(R.string.settings_theme_auto)
        }

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

        val rsVisible = try {
            RoadSenseConfig.snapshot(false).overlayVisible
        } catch (_: Throwable) { false }

        uiState = uiState.copy(
            themeModeLabel = themeLabel,
            languageLabel = langLabel,
            roadSenseOverlayEnabled = rsVisible,
            installedVersion = AppUpdater.getInstalledVersion(),
            appId = BuildConfig.APPLICATION_ID
        )

        val appContext = context?.applicationContext

        // Load config & daemon integration statuses off-thread
        executor.execute {
            if (!isAdded) return@execute
            val resolvedVer = appContext?.let { AppUpdater.getDisplayVersion(it) } ?: AppUpdater.getInstalledVersion()

            // Overlays from UnifiedConfig
            val overlayCfg = try {
                UnifiedConfigManager.loadConfig().optJSONObject("statusOverlay")
            } catch (_: Throwable) { null }

            val camVisible = overlayCfg?.optBoolean("cameraVisible", false) ?: false
            val tripVisible = overlayCfg?.optBoolean("tripVisible", false) ?: false

            // Check Telegram status
            var tgConn = false
            try {
                val conn = DaemonHttpClient.open("/api/telegram/status", "GET", 1500, 2000)
                if (conn.responseCode == 200) {
                    val root = JSONObject(conn.inputStream.bufferedReader().readText())
                    tgConn = root.optBoolean("paired", false) || root.optBoolean("running", false)
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            // Check ABRP status
            var abrpConn = false
            try {
                val conn = DaemonHttpClient.open("/api/abrp/status", "GET", 1500, 2000)
                if (conn.responseCode == 200) {
                    val root = JSONObject(conn.inputStream.bufferedReader().readText())
                    abrpConn = root.optBoolean("connected", false)
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            // Check MQTT status
            var mqttConn = false
            try {
                val conn = DaemonHttpClient.open("/api/mqtt/status", "GET", 1500, 2000)
                if (conn.responseCode == 200) {
                    val root = JSONObject(conn.inputStream.bufferedReader().readText())
                    val conns = root.optJSONArray("connections")
                    mqttConn = conns != null && conns.length() > 0
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            // Check BYD Cloud status
            var bydConn = false
            try {
                val conn = DaemonHttpClient.open("/api/bydcloud/status", "GET", 1500, 2000)
                if (conn.responseCode == 200) {
                    val root = JSONObject(conn.inputStream.bufferedReader().readText())
                    val st = root.optJSONObject("status")
                    bydConn = st?.optBoolean("verified", false) ?: false
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    installedVersion = resolvedVer,
                    cameraOverlayEnabled = camVisible,
                    tripOverlayEnabled = tripVisible,
                    telegramConnected = tgConn,
                    abrpConnected = abrpConn,
                    mqttConnected = mqttConn,
                    bydCloudConnected = bydConn
                )
            }
        }
    }

    private fun showThemeDialog() {
        val labels = arrayOf(
            getString(R.string.settings_theme_auto),
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark)
        )
        val current = PreferencesManager.getThemeMode()
        val checked = when (current) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_theme_label)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val mode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                PreferencesManager.setThemeMode(mode)
                loadInitialState()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val act = activity ?: return
        LanguagePickerDialog.show(act) {
            act.recreate()
        }
    }

    private fun toggleOverlay(key: String, value: Boolean) {
        if (key == "cameraVisible") {
            uiState = uiState.copy(cameraOverlayEnabled = value)
        } else if (key == "tripVisible") {
            uiState = uiState.copy(tripOverlayEnabled = value)
        }

        StatusOverlayUiWriter.write(key, value) { ok ->
            if (ok) {
                kickOverlayRefresh()
            } else {
                mainHandler.post {
                    loadInitialState()
                }
            }
        }
    }

    private fun toggleRoadSenseOverlay(value: Boolean) {
        uiState = uiState.copy(roadSenseOverlayEnabled = value)
        StatusOverlayUiWriter.writeWith(
            "roadSense.overlayVisible",
            { ok ->
                if (ok) {
                    context?.let { RoadSenseOverlayService.syncWithConfig(it) }
                } else {
                    mainHandler.post { loadInitialState() }
                }
            }
        ) {
            RoadSenseConfig.setOverlayVisible(value)
        }
    }

    private fun kickOverlayRefresh() {
        val ctx = context ?: return
        StatusOverlayService.startIfPermitted(ctx)
    }
}
