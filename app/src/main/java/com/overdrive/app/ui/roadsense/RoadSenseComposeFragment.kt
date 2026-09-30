package com.overdrive.app.ui.roadsense

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.navmap.NavMapConfig
import com.overdrive.app.navmap.RoadSenseMapActivity
import com.overdrive.app.roadsense.config.RoadSenseConfig
import com.overdrive.app.services.RoadSenseChimePlaybackService
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * 100% Jetpack Compose Native Fragment for RoadSense Road Hazard & Surface Sensing.
 * Completely replaces legacy WebViewFragment (/road-sense).
 */
class RoadSenseComposeFragment : Fragment() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "RoadSenseWorker").apply { isDaemon = true }
    }

    private var uiState by mutableStateOf(RoadSenseUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    RoadSenseScreen(
                        state = uiState,
                        onTabSelected = { tab ->
                            uiState = uiState.copy(selectedTab = tab)
                        },
                        onToggleMaster = { enabled ->
                            updateRoadSenseConfig("enabled" to enabled) {
                                uiState = uiState.copy(isMasterEnabled = enabled)
                            }
                        },
                        onToggleOverlayVisible = { visible ->
                            updateRoadSenseConfig("overlayVisible" to visible) {
                                uiState = uiState.copy(overlayVisible = visible)
                            }
                        },
                        onToggleCalibrationMode = { cal ->
                            updateRoadSenseConfig("calibrationMode" to cal) {
                                uiState = uiState.copy(calibrationMode = cal)
                            }
                        },
                        onToggleCornerAdaptive = { adaptive ->
                            updateRoadSenseConfig("cornerYawSpeedAdaptive" to adaptive) {
                                uiState = uiState.copy(cornerYawSpeedAdaptive = adaptive)
                            }
                        },
                        onToggleWarnEnabled = { warn ->
                            updateRoadSenseConfig("warnEnabled" to warn) {
                                uiState = uiState.copy(warnEnabled = warn)
                            }
                        },
                        onSelectWarnMode = { mode ->
                            updateRoadSenseConfig("warnMode" to mode) {
                                uiState = uiState.copy(warnMode = mode)
                            }
                        },
                        onSelectAudioChannel = { ch ->
                            updateRoadSenseConfig("warnAudioChannel" to ch) {
                                uiState = uiState.copy(warnAudioChannel = ch)
                            }
                        },
                        onAudioVolumeChange = { vol ->
                            uiState = uiState.copy(warnAudioVolume = vol)
                            updateRoadSenseConfig("warnAudioVolume" to vol)
                        },
                        onTestChime = { severity ->
                            playTestChime(severity)
                        },
                        onLeadSecondsChange = { sec ->
                            uiState = uiState.copy(warnLeadSeconds = sec)
                            updateRoadSenseConfig("warnLeadSeconds" to sec.toDouble())
                        },
                        onSensitivityChange = { sens ->
                            uiState = uiState.copy(detectionSensitivity = sens)
                            updateRoadSenseConfig("detectionSensitivity" to sens.toDouble())
                        },
                        onToggleSeverityMinor = { enabled ->
                            updateRoadSenseConfig("warnSeverityMinor" to enabled) {
                                uiState = uiState.copy(severityMinor = enabled)
                            }
                        },
                        onToggleSeverityModerate = { enabled ->
                            updateRoadSenseConfig("warnSeverityModerate" to enabled) {
                                uiState = uiState.copy(severityModerate = enabled)
                            }
                        },
                        onToggleSeveritySevere = { enabled ->
                            updateRoadSenseConfig("warnSeveritySevere" to enabled) {
                                uiState = uiState.copy(severitySevere = enabled)
                            }
                        },
                        onOpenHazardMap = {
                            openHazardMapActivity()
                        },
                        onRoutingApiKeyChange = { key ->
                            uiState = uiState.copy(routingApiKeyInput = key)
                        },
                        onSaveRoutingKey = {
                            saveRoutingApiKey()
                        },
                        onToggleAutoProjectCluster = { project ->
                            updateNavMapConfig("autoProjectCluster" to project) {
                                uiState = uiState.copy(autoProjectCluster = project)
                            }
                        },
                        onToggleBsEnabled = { enabled ->
                            updateBlindSpotConfig("enabled" to enabled) {
                                uiState = uiState.copy(bsEnabled = enabled)
                            }
                        },
                        onSelectBsMergeMode = { mode ->
                            updateBlindSpotConfig("mergeMode" to mode) {
                                uiState = uiState.copy(bsMergeMode = mode)
                            }
                        },
                        onBsMinSpeedChange = { minSpeed ->
                            uiState = uiState.copy(bsMinSpeedKmh = minSpeed)
                            updateBlindSpotConfig("minSpeedKmh" to minSpeed)
                        },
                        onBsMaxSpeedChange = { maxSpeed ->
                            uiState = uiState.copy(bsMaxSpeedKmh = maxSpeed)
                            updateBlindSpotConfig("maxSpeedKmh" to maxSpeed)
                        },
                        onToggleBsSuppressReverse = { suppress ->
                            updateBlindSpotConfig("suppressReverse" to suppress) {
                                uiState = uiState.copy(bsSuppressReverse = suppress)
                            }
                        },
                        onBsRectifyChange = { strength ->
                            uiState = uiState.copy(bsRectifyStrength = strength)
                            updateBlindSpotConfig("rectifyStrength" to strength)
                        },
                        onToggleCrowdUpload = { upload ->
                            updateRoadSenseConfig("crowdUpload" to upload) {
                                uiState = uiState.copy(crowdUpload = upload)
                            }
                        },
                        onToggleCrowdDownload = { download ->
                            updateRoadSenseConfig("crowdDownload" to download) {
                                uiState = uiState.copy(crowdDownload = download)
                            }
                        },
                        onClearLocalHazardsRequest = {
                            uiState = uiState.copy(showClearLocalDialog = true)
                        },
                        onClearLocalHazardsConfirm = {
                            clearLocalHazards()
                        },
                        onClearLocalHazardsDismiss = {
                            uiState = uiState.copy(showClearLocalDialog = false)
                        },
                        onClearCloudHazardsRequest = {
                            uiState = uiState.copy(showClearCloudDialog = true)
                        },
                        onClearCloudHazardsConfirm = {
                            clearCloudHazards()
                        },
                        onClearCloudHazardsDismiss = {
                            uiState = uiState.copy(showClearCloudDialog = false)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAllConfig()
    }

    private fun loadAllConfig() {
        worker.execute {
            try {
                UnifiedConfigManager.forceReload()
                val snap = RoadSenseConfig.snapshot()

                // Blind spot section
                val config = UnifiedConfigManager.loadConfig()
                val bs = config.optJSONObject("blindspot") ?: JSONObject()
                val navMap = config.optJSONObject("navMap") ?: JSONObject()

                val bsEnabled = bs.optBoolean("enabled", false)
                val bsMergeMode = bs.optString("mergeMode", "both")
                val bsMinSpeed = bs.optInt("minSpeedKmh", 0)
                val bsMaxSpeed = bs.optInt("maxSpeedKmh", 0)
                val bsSuppressRev = bs.optBoolean("suppressReverse", false)
                val bsRectify = bs.optInt("rectifyStrength", 0)

                val navMapConf = NavMapConfig.fromUnifiedConfig()
                val autoProject = navMap.optBoolean("autoProjectCluster", false)

                val count = 0L

                mainHandler.post {
                    uiState = uiState.copy(
                        isMasterEnabled = snap.enabled,
                        overlayVisible = snap.overlayVisible,
                        calibrationMode = snap.calibrationMode,
                        cornerYawSpeedAdaptive = snap.cornerYawSpeedAdaptive,
                        warnEnabled = snap.warnEnabled,
                        warnMode = when (snap.warnMode) {
                            RoadSenseConfig.WarnMode.AUDIO -> "audio"
                            RoadSenseConfig.WarnMode.VISUAL -> "visual"
                            else -> "both"
                        },
                        warnAudioChannel = snap.warnAudioChannel,
                        warnAudioVolume = snap.warnAudioVolume,
                        warnLeadSeconds = snap.warnLeadSeconds,
                        detectionSensitivity = snap.detectionSensitivity,
                        severityMinor = snap.severityMinor,
                        severityModerate = snap.severityModerate,
                        severitySevere = snap.severitySevere,
                        hasRoutingKey = navMapConf.routingApiKey.isNotBlank(),
                        routingEndpointInput = navMapConf.routingEndpoint,
                        autoProjectCluster = autoProject,
                        bsEnabled = bsEnabled,
                        bsMergeMode = bsMergeMode,
                        bsMinSpeedKmh = bsMinSpeed,
                        bsMaxSpeedKmh = bsMaxSpeed,
                        bsSuppressReverse = bsSuppressRev,
                        bsRectifyStrength = bsRectify,
                        crowdUpload = snap.crowdUpload,
                        crowdDownload = snap.crowdDownload,
                        syncWorkerUrl = snap.syncWorkerUrl ?: RoadSenseConfig.DEFAULT_WORKER_URL,
                        localHazardCount = count,
                        isLoading = false
                    )
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    showToast("Yapılandırma yüklenemedi: ${t.message}")
                }
            }
        }
    }

    private fun updateRoadSenseConfig(vararg pairs: Pair<String, Any>, onSuccess: (() -> Unit)? = null) {
        worker.execute {
            try {
                val data = JSONObject()
                for ((k, v) in pairs) {
                    data.put(k, v)
                }
                val ok = UnifiedConfigManager.updateSection("roadSense", data)
                if (ok && onSuccess != null) {
                    mainHandler.post { onSuccess() }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Ayar kaydedilemedi: ${t.message}") }
            }
        }
    }

    private fun updateBlindSpotConfig(vararg pairs: Pair<String, Any>, onSuccess: (() -> Unit)? = null) {
        worker.execute {
            try {
                val data = JSONObject()
                for ((k, v) in pairs) {
                    data.put(k, v)
                }
                val ok = UnifiedConfigManager.updateSection("blindspot", data)
                if (ok && onSuccess != null) {
                    mainHandler.post { onSuccess() }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Kör nokta ayarı kaydedilemedi: ${t.message}") }
            }
        }
    }

    private fun updateNavMapConfig(vararg pairs: Pair<String, Any>, onSuccess: (() -> Unit)? = null) {
        worker.execute {
            try {
                val data = JSONObject()
                for ((k, v) in pairs) {
                    data.put(k, v)
                }
                val ok = UnifiedConfigManager.updateSection("navMap", data)
                if (ok && onSuccess != null) {
                    mainHandler.post { onSuccess() }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Harita ayarı kaydedilemedi: ${t.message}") }
            }
        }
    }

    private fun playTestChime(severity: String) {
        try {
            val ctx = requireContext()
            val intent = Intent(ctx, RoadSenseChimePlaybackService::class.java).apply {
                putExtra("resName", "roadsense_chime_$severity")
                putExtra("channel", uiState.warnAudioChannel)
                putExtra("volumePercent", uiState.warnAudioVolume)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
            showToast("Uyarı sesi çalınıyor: $severity")
        } catch (t: Throwable) {
            showToast("Ses çalınamadı: ${t.message}")
        }
    }

    private fun openHazardMapActivity() {
        try {
            val intent = Intent(requireContext(), RoadSenseMapActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (t: Throwable) {
            showToast("Harita açılamadı: ${t.message}")
        }
    }

    private fun saveRoutingApiKey() {
        val key = uiState.routingApiKeyInput.trim()
        val endpoint = uiState.routingEndpointInput.trim()
        if (key.isBlank()) {
            showToast("Lütfen geçerli bir Valhalla API anahtarı girin")
            return
        }

        worker.execute {
            try {
                val data = JSONObject().apply {
                    put("routingApiKey", key)
                    if (endpoint.isNotBlank()) put("routingEndpoint", endpoint)
                    put("enabled", true)
                }
                val ok = UnifiedConfigManager.updateSection("navMap", data)
                mainHandler.post {
                    if (ok) {
                        uiState = uiState.copy(hasRoutingKey = true, routingApiKeyInput = "")
                        showToast("Navigasyon API anahtarı başarıyla kaydedildi")
                    } else {
                        showToast("Kayıt başarısız")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post { showToast("Hata: ${t.message}") }
            }
        }
    }

    private fun clearLocalHazards() {
        worker.execute {
            try {
                val conn = DaemonHttpClient.open("/api/roadsense/delete-local", "POST", 2000, 3000)
                conn.doOutput = true
                val code = conn.responseCode
                conn.disconnect()
                mainHandler.post {
                    uiState = uiState.copy(showClearLocalDialog = false)
                    if (code in 200..299) {
                        showToast("Yerel tehlikeler ve kalibrasyonlar temizlendi")
                        loadAllConfig()
                    } else {
                        showToast("Temizleme başarısız (HTTP $code)")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(showClearLocalDialog = false)
                    showToast("Hata: ${t.message}")
                }
            }
        }
    }

    private fun clearCloudHazards() {
        worker.execute {
            try {
                val conn = DaemonHttpClient.open("/api/roadsense/delete-cloud", "POST", 2000, 3000)
                conn.doOutput = true
                val code = conn.responseCode
                conn.disconnect()
                mainHandler.post {
                    uiState = uiState.copy(showClearCloudDialog = false)
                    if (code in 200..299) {
                        showToast("Buluttaki cihaz kayıtları temizlendi")
                    } else {
                        showToast("Bulut temizleme başarısız (HTTP $code)")
                    }
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(showClearCloudDialog = false)
                    showToast("Hata: ${t.message}")
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(context ?: return, msg, Toast.LENGTH_SHORT).show()
    }
}
