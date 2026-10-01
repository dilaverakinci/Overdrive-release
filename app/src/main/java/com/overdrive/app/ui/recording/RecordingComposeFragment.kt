package com.overdrive.app.ui.recording

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
import androidx.fragment.app.Fragment
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordingComposeFragment : Fragment() {

    private var executorService: ExecutorService? = null
    private val executor: ExecutorService
        get() = executorService?.takeUnless { it.isShutdown } ?: Executors.newSingleThreadExecutor().also { executorService = it }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var uiState by mutableStateOf(RecordingUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    RecordingScreen(
                        state = uiState,
                        onTabSelected = { tab -> uiState = uiState.copy(selectedTab = tab) },
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
                        onRefresh = { loadState() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executorService?.shutdownNow()
        executorService = null
    }

    private fun loadState() {
        uiState = uiState.copy(isLoading = true)
        executor.execute {
            if (!isAdded) return@execute
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

                // Query live recording status
                var curState = if (mode != "NONE") "Etkin ($mode)" else "Boşta (Idle)"
                var isRec = false
                var recToday = 0

                try {
                    val conn = DaemonHttpClient.open("/api/recordings/stats", "GET", 1500, 2000)
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val stats = JSONObject(body)
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
                    uiState = uiState.copy(
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
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "Kayıt ayarları yüklenemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateRecordingKey(key: String, value: Any) {
        when (key) {
            "recordingMode" -> {
                val mode = value as String
                uiState = uiState.copy(
                    recordingMode = mode,
                    isRecording = mode == "CONTINUOUS"
                )
            }
            "recordingLayout" -> uiState = uiState.copy(recordingLayout = value as String)
            "dashcamUseWindshield" -> uiState = uiState.copy(dashcamUseWindshield = value as Boolean)
            "proximityTriggerLevel" -> uiState = uiState.copy(proximityTriggerLevel = value as String)
            "proximityPreSeconds" -> uiState = uiState.copy(proximityPreSeconds = value as Int)
            "proximityPostSeconds" -> uiState = uiState.copy(proximityPostSeconds = value as Int)
            "geocodingEnabled" -> uiState = uiState.copy(geocodingEnabled = value as Boolean)
            "geocodingOnline" -> uiState = uiState.copy(geocodingOnline = value as Boolean)
            "recordingQuality" -> uiState = uiState.copy(recordingQuality = value as String)
            "recordingCodec" -> uiState = uiState.copy(recordingCodec = value as String)
            "segmentDurationMinutes" -> uiState = uiState.copy(segmentDurationMinutes = value as Int)
            "rectifyStrength" -> uiState = uiState.copy(rectifyStrength = value as Int)
            "telemetryOverlayEnabled" -> uiState = uiState.copy(telemetryOverlayEnabled = value as Boolean)
            "storageType" -> uiState = uiState.copy(storageType = value as String)
            "storageLimitMb" -> uiState = uiState.copy(storageLimitMb = value as Int)
            "autoCleanup" -> uiState = uiState.copy(autoCleanup = value as Boolean)
        }

        executor.execute {
            try {
                UnifiedConfigManager.updateValues("recording", mapOf(key to value))
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Kayıt ayarı kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateCameraKey(key: String, value: Any) {
        if (key == "targetFps") {
            uiState = uiState.copy(targetFps = value as Int)
        }
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("camera", mapOf(key to value))
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Kamera ayarı kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateOemKey(key: String, value: Any) {
        when (key) {
            "recordingMode" -> uiState = uiState.copy(oemRecordingMode = value as String)
            "telemetryOverlay" -> uiState = uiState.copy(oemTelemetryOverlay = value as Boolean)
        }
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("oem", mapOf(key to value))
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "OEM ayarı kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleNativeDvr() {
        val nextState = !uiState.nativeDvrDisabled
        uiState = uiState.copy(nativeDvrDisabled = nextState)
        executor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/oem-dashcam/native-dvr/toggle", "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
                mainHandler.post {
                    Toast.makeText(
                        requireContext(),
                        if (nextState) "Orijinal DVR devre dışı bırakıldı" else "Orijinal DVR etkinleştirildi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "İşlem başarısız: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
