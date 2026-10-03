package com.overdrive.app.ui.fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.overdrive.app.R
import com.overdrive.app.ui.MainActivity
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.diagnostics.DiagnosticsScreen
import com.overdrive.app.ui.diagnostics.DiagnosticsUiState
import com.overdrive.app.ui.diagnostics.DiagnosticsViewModel
import com.overdrive.app.ui.model.DaemonStatus
import com.overdrive.app.ui.model.DaemonType
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.util.RecordingScanner
import com.overdrive.app.ui.util.navigateDrillDown
import com.overdrive.app.ui.viewmodel.DaemonsViewModel
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 100% Jetpack Compose Native Diagnostics Fragment.
 * Power-user surface for system health (Network, Storage, Camera, Battery)
 * and diagnostic tools (ADB Console, Traffic Monitor, Camera Probe, Battery Health).
 */
class DiagnosticsFragment : Fragment() {

    private val daemonsViewModel: DaemonsViewModel by activityViewModels()
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()

    private var uiState: DiagnosticsUiState
        get() = diagnosticsViewModel.uiState.value
        set(value) { diagnosticsViewModel.updateUiState { value } }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var ssidRefreshRunnable: Runnable? = null
    private var storageExecutor: ExecutorService? = null
    private var batteryExecutor: ExecutorService? = null

    private var sohFileObserver: android.os.FileObserver? = null
    private val sohFilePath = "/data/local/tmp/abrp_soh_estimate.properties"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = OverdriveComposeContainer(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OverdriveTheme {
                val state by diagnosticsViewModel.uiState.collectAsState()
                DiagnosticsScreen(
                    state = state,
                    onTabSelected = { diagnosticsViewModel.selectTab(it) },
                    onScanDtcClick = { diagnosticsViewModel.scanDtc() },
                    onRequestClearDtcClick = { diagnosticsViewModel.requestClearDtc() },
                    onConfirmClearDtc = { diagnosticsViewModel.confirmClearDtc() },
                    onDismissClearDtcDialog = { diagnosticsViewModel.dismissClearDtcDialog() },
                    onDismissFeedback = { diagnosticsViewModel.dismissFeedback() },
                    onToggleEcuDetail = { diagnosticsViewModel.toggleEcuDetail(it) },
                    onAdbClick = {
                        findNavController().navigateDrillDown(R.id.adbConsoleFragment)
                    },
                    onEnableBydAdbClick = {
                        com.overdrive.app.byd.adb.BydAdbManager.enableWirelessAdbAsync(requireContext()) { success, msg ->
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    (if (success) "✓ " else "⚠ ") + msg,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onTrafficClick = {
                        (activity as? MainActivity)?.invokeTrafficMonitorAction()
                    },
                    onCameraProbeClick = {
                        (activity as? MainActivity)?.invokeReconfigureCameraAction()
                    },
                    onBatteryHealthClick = {
                        (activity as? MainActivity)?.invokeBatteryHealthAction()
                    },
                    onBatteryLongClick = {
                        (activity as? MainActivity)?.resetAndReplayOnboarding()
                        Toast.makeText(
                            requireContext(),
                            R.string.onboarding_reset_toast,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onSettingsShortcutClick = {
                        findNavController().navigate(
                            R.id.settingsFragment,
                            null,
                            com.overdrive.app.ui.util.NavOptionsExt.m3FadeThrough()
                        )
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tunnel-state-driven refresh (changes immediately when daemons toggle).
        val tunnelObserver = Observer<String?> { _ -> updateNetworkTile() }
        daemonsViewModel.cloudflaredController.tunnelUrl.observe(viewLifecycleOwner, tunnelObserver)
        daemonsViewModel.zrokController.tunnelUrl.observe(viewLifecycleOwner, tunnelObserver)
        daemonsViewModel.tailscaleController.tunnelUrl.observe(viewLifecycleOwner, tunnelObserver)
        daemonsViewModel.daemonStates.observe(viewLifecycleOwner) { _ ->
            updateNetworkTile()
            updateCameraTile()
        }

        // First paint.
        updateNetworkTile()
        updateCameraTile()

        // SSID polling: Wi-Fi network can change without a system broadcast.
        scheduleSsidRefresh()
    }

    override fun onResume() {
        super.onResume()
        refreshStorageTile()
        updateCameraTile()
        refreshBatteryTile()
        startSohFileObserver()
    }

    override fun onPause() {
        super.onPause()
        stopSohFileObserver()
    }

    @Suppress("DEPRECATION")
    private fun startSohFileObserver() {
        if (sohFileObserver != null) return
        sohFileObserver = object : android.os.FileObserver(
            sohFilePath,
            android.os.FileObserver.CLOSE_WRITE
                or android.os.FileObserver.DELETE
                or android.os.FileObserver.DELETE_SELF
                or android.os.FileObserver.MOVED_TO
                or android.os.FileObserver.CREATE
        ) {
            override fun onEvent(event: Int, path: String?) {
                mainHandler.post {
                    if (isAdded) refreshBatteryTile()
                }
            }
        }.also { it.startWatching() }
    }

    private fun stopSohFileObserver() {
        sohFileObserver?.stopWatching()
        sohFileObserver = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ssidRefreshRunnable?.let { mainHandler.removeCallbacks(it) }
        ssidRefreshRunnable = null
        storageExecutor?.shutdownNow()
        storageExecutor = null
        batteryExecutor?.shutdownNow()
        batteryExecutor = null
        stopSohFileObserver()
    }

    // ============== Network tile ==============

    private fun scheduleSsidRefresh() {
        val r = object : Runnable {
            override fun run() {
                if (!isAdded) return
                updateNetworkTile()
                mainHandler.postDelayed(this, SSID_REFRESH_INTERVAL_MS)
            }
        }
        ssidRefreshRunnable = r
        mainHandler.postDelayed(r, SSID_REFRESH_INTERVAL_MS)
    }

    private fun updateNetworkTile() {
        val ctx = context ?: return
        val topLine = computeNetworkTopLine(ctx)
        val (stateLabelRes, dotRes) = computeTunnelState()
        val tunnelStr = getString(
            R.string.diagnostics_network_tunnel_label,
            getString(stateLabelRes)
        )
        uiState = uiState.copy(
            networkSsid = topLine,
            tunnelState = tunnelStr,
            isTunnelOnline = (dotRes == R.drawable.status_dot_online)
        )
    }

    private fun computeNetworkTopLine(ctx: Context): String {
        try {
            val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifi?.connectionInfo
            val rawSsid = info?.ssid
            val networkId = info?.networkId ?: -1
            if (!rawSsid.isNullOrBlank() &&
                rawSsid != WifiManager.UNKNOWN_SSID &&
                rawSsid != "0x" &&
                networkId != -1
            ) {
                val stripped = if (rawSsid.length >= 2 &&
                    rawSsid.startsWith("\"") &&
                    rawSsid.endsWith("\"")
                ) {
                    rawSsid.substring(1, rawSsid.length - 1)
                } else {
                    rawSsid
                }
                if (stripped.isNotBlank()) return stripped
            }
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }

        return try {
            val cm = ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager
            @Suppress("DEPRECATION")
            val ni = cm?.activeNetworkInfo
            @Suppress("DEPRECATION")
            when {
                ni == null || !ni.isConnected -> getString(R.string.diagnostics_network_offline)
                ni.type == ConnectivityManager.TYPE_MOBILE -> getString(R.string.diagnostics_network_mobile)
                ni.type == ConnectivityManager.TYPE_ETHERNET -> getString(R.string.diagnostics_network_ethernet)
                ni.type == ConnectivityManager.TYPE_WIFI -> getString(R.string.diagnostics_network_offline)
                else -> getString(R.string.diagnostics_network_offline)
            }
        } catch (_: Throwable) {
            getString(R.string.diagnostics_network_offline)
        }
    }

    private fun computeTunnelState(): Pair<Int, Int> {
        val anyUrl = listOf(
            daemonsViewModel.cloudflaredController.tunnelUrl.value,
            daemonsViewModel.zrokController.tunnelUrl.value,
            daemonsViewModel.tailscaleController.tunnelUrl.value
        ).any { !it.isNullOrEmpty() }

        if (anyUrl) {
            return R.string.diagnostics_tunnel_state_online to R.drawable.status_dot_online
        }

        val states = daemonsViewModel.daemonStates.value
        val anyStarting = states?.values?.any {
            (it.type == DaemonType.CLOUDFLARED_TUNNEL ||
                it.type == DaemonType.ZROK_TUNNEL ||
                it.type == DaemonType.TAILSCALE_TUNNEL) &&
                it.status == DaemonStatus.STARTING
        } == true

        return if (anyStarting) {
            R.string.diagnostics_tunnel_state_connecting to R.drawable.status_dot_starting
        } else {
            R.string.diagnostics_tunnel_state_offline to R.drawable.status_dot_offline
        }
    }

    // ============== Storage tile ==============

    private fun refreshStorageTile() {
        val ctx = context?.applicationContext ?: return
        val executor = storageExecutor ?: Executors.newSingleThreadExecutor()
            .also { storageExecutor = it }

        executor.execute {
            var clipCount = 0
            var usedBytes = 0L
            var freeBytes = 0L
            try {
                val recordingsDir = RecordingScanner.getRecordingsDir(ctx)
                val sentryDir = RecordingScanner.getSentryEventsDir(ctx)
                val proximityDir = RecordingScanner.getProximityEventsDir(ctx)
                clipCount += countMp4(recordingsDir)
                clipCount += countMp4(sentryDir)
                clipCount += countMp4(proximityDir)
                usedBytes += sumMp4Sizes(recordingsDir)
                usedBytes += sumMp4Sizes(sentryDir)
                usedBytes += sumMp4Sizes(proximityDir)

                val statFsTarget = when {
                    recordingsDir.exists() -> recordingsDir
                    sentryDir.exists() -> sentryDir
                    else -> null
                }
                if (statFsTarget != null) {
                    try {
                        val stat = StatFs(statFsTarget.absolutePath)
                        freeBytes = stat.availableBytes
                    } catch (_: Throwable) {
                        freeBytes = 0L
                    }
                }
            } catch (_: Throwable) {
            }

            val usedHuman = Formatter.formatShortFileSize(ctx, usedBytes)
            val freeHuman = Formatter.formatShortFileSize(ctx, freeBytes)

            mainHandler.post {
                if (!isAdded) return@post
                uiState = uiState.copy(
                    storageUsed = getString(R.string.diagnostics_storage_used_line, clipCount, usedHuman),
                    storageFree = getString(R.string.diagnostics_storage_free_line, freeHuman)
                )
            }
        }
    }

    private fun countMp4(dir: File): Int {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return 0
        val files = dir.listFiles() ?: return 0
        var n = 0
        for (f in files) {
            if (f.isFile && f.name.endsWith(".mp4") && f.length() > 0L) n++
        }
        return n
    }

    private fun sumMp4Sizes(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return 0L
        val files = dir.listFiles() ?: return 0L
        var sum = 0L
        for (f in files) {
            if (f.isFile && f.name.endsWith(".mp4")) sum += f.length()
        }
        return sum
    }

    // ============== Camera tile ==============

    private fun updateCameraTile() {
        val daemonState = daemonsViewModel.daemonStates.value
            ?.get(DaemonType.CAMERA_DAEMON)
        if (daemonState == null || daemonState.status != DaemonStatus.RUNNING) {
            uiState = uiState.copy(
                cameraStatus = getString(R.string.diagnostics_camera_value_offline),
                isCameraOnline = false
            )
            return
        }

        var probedId = -1
        var manualOverride = false
        try {
            val cfg = com.overdrive.app.config.UnifiedConfigManager.loadConfig()
            val cam = cfg.optJSONObject("camera")
            if (cam != null) {
                probedId = cam.optInt("probedCameraId", -1)
                manualOverride = cam.optBoolean("manualOverride", false)
            }
        } catch (_: Throwable) {
        }

        when {
            probedId < 0 -> {
                uiState = uiState.copy(
                    cameraStatus = getString(R.string.diagnostics_camera_value_probing),
                    isCameraOnline = false
                )
            }
            manualOverride -> {
                uiState = uiState.copy(
                    cameraStatus = getString(R.string.diagnostics_camera_value_camera_n_manual, probedId),
                    isCameraOnline = true
                )
            }
            else -> {
                uiState = uiState.copy(
                    cameraStatus = getString(R.string.diagnostics_camera_value_camera_n, probedId),
                    isCameraOnline = true
                )
            }
        }
    }

    // ============== Battery tile ==============

    private fun refreshBatteryTile() {
        val executor = batteryExecutor ?: Executors.newSingleThreadExecutor()
            .also { batteryExecutor = it }

        executor.execute {
            var sohPercent: Double? = null
            var frameMismatch = false
            try {
                val conn = com.overdrive.app.util.DaemonHttpClient.open(
                    "/api/performance/soh", "GET", 1500, 2000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(body)
                    val source = json.optString("displaySource", "unavailable")
                    val value = json.optDouble("displaySoh", -1.0)
                    if (source != "unavailable" && value > 0.0 && value <= 100.0) {
                        sohPercent = value
                    }
                    val frame = json.optJSONObject("frameAnchor")
                    if (frame != null) frameMismatch = frame.optBoolean("mismatch", false)
                }
                conn.disconnect()
            } catch (_: Throwable) { }

            val finalSoh = sohPercent
            val finalMismatch = frameMismatch
            mainHandler.post {
                if (!isAdded) return@post
                val sohText = if (finalSoh == null) {
                    getString(R.string.diagnostics_battery_value_pending)
                } else {
                    getString(R.string.diagnostics_battery_value_soh, finalSoh)
                }
                val isGood = (finalSoh != null && finalSoh >= 80.0 && !finalMismatch)
                uiState = uiState.copy(
                    batterySoh = sohText,
                    isBatteryGood = isGood,
                    isBatteryReviewNeeded = finalMismatch
                )
            }
        }
    }

    companion object {
        private const val SSID_REFRESH_INTERVAL_MS = 5_000L
    }
}
