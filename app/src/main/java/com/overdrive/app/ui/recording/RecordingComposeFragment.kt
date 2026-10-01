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
                        onToggleGeocodingEnabled = { v -> updateRecordingGeocoding(enabled = v) },
                        onToggleGeocodingOnline = { v -> updateRecordingGeocoding(online = v) },
                        onGeocodingCustomUrlChange = { url -> updateGeocodingUrl(url) },
                        onQualitySelected = { q -> updateRecordingKey("recordingQuality", q) },
                        onCodecSelected = { c -> updateRecordingKey("recordingCodec", c) },
                        onFpsSelected = { fps -> updateCameraKey("targetFps", fps) },
                        onClipDurationSelected = { mins -> updateRecordingKey("segmentDurationMinutes", mins) },
                        onRectifyStrengthChange = { str -> updateRecordingKey("rectifyStrength", str) },
                        onToggleTelemetryOverlay = { v -> updateRecordingTelemetryOverlay(v) },
                        onToggleTelemetryField = { field, add -> toggleTelemetryField(field, add) },
                        onToggleAudioRecording = { v -> updateAudioRecording(v) },
                        onOemRecordingModeSelected = { mode -> updateOemKey("recordingMode", mode) },
                        onToggleOemTelemetryOverlay = { v -> updateOemKey("telemetryOverlay", v) },
                        onToggleOemTelemetryField = { field, add -> toggleOemTelemetryField(field, add) },
                        onToggleNativeDvr = { toggleNativeDvr() },
                        onStorageTypeSelected = { t -> updateStorageType(t) },
                        onStorageLimitChange = { lim -> updateStorageLimit(lim) },
                        onToggleAutoCleanup = { v -> updateRecordingKey("autoCleanup", v) },
                        onToggleCdrCleanup = { v -> updateCdrCleanup(enabled = v) },
                        onCdrReservedSpaceChange = { mb -> updateCdrCleanup(reservedMb = mb.toLong()) },
                        onCdrProtectedHoursChange = { h -> updateCdrCleanup(hours = h) },
                        onCdrMinFilesKeepChange = { count -> updateCdrCleanup(minFiles = count) },
                        onRefresh = { loadState() },
                        onApplyChanges = { Toast.makeText(requireContext(), "Değişiklikler uygulandı", Toast.LENGTH_SHORT).show() }
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

                // Query live recording status & OEM camera status
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
                    uiState = uiState.copy(
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
                if (key == "recordingMode") {
                    try {
                        val payload = JSONObject().apply { put("recordingMode", value) }.toString()
                        val conn = DaemonHttpClient.open("/api/oem-dashcam/config", "POST", 2000, 3000)
                        conn.doOutput = true
                        conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                        conn.responseCode
                        conn.disconnect()
                    } catch (_: Throwable) {}
                }
                UnifiedConfigManager.updateValues("oem", mapOf(key to value))
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "OEM ayarı kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleOemTelemetryField(field: String, add: Boolean) {
        val currFields = uiState.oemTelemetryFields.toMutableSet()
        if (add) currFields.add(field) else currFields.remove(field)
        uiState = uiState.copy(oemTelemetryFields = currFields)
        executor.execute {
            val jsonArray = org.json.JSONArray(currFields.toList())
            try {
                UnifiedConfigManager.updateValues("oem", mapOf("telemetryFields" to jsonArray))
            } catch (_: Throwable) {}
        }
    }

    private fun updateStorageType(typeStr: String) {
        uiState = uiState.copy(storageType = typeStr)
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

    private fun updateStorageLimit(limitMb: Int) {
        val recBytes = try { com.overdrive.app.storage.StorageManager.getInstance()?.recordingsSize ?: 0L } catch (_: Throwable) { 0L }
        val limitBytes = limitMb.toLong() * 1024L * 1024L
        val usedPercent = if (limitBytes > 0) (recBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f) else 0f
        uiState = uiState.copy(
            storageLimitMb = limitMb,
            storageLimitText = "$limitMb MB sınırı",
            storageUsedPercent = usedPercent
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

    private fun toggleNativeDvr() {
        val nextDisable = !uiState.nativeDvrDisabled
        uiState = uiState.copy(nativeDvrDisabled = nextDisable)
        executor.execute {
            try {
                val endpoint = if (nextDisable) "/api/oem-dashcam/native-dvr/disable" else "/api/oem-dashcam/native-dvr/enable"
                val conn = DaemonHttpClient.open(endpoint, "POST", 2000, 3000)
                conn.responseCode
                conn.disconnect()
                mainHandler.post {
                    Toast.makeText(
                        requireContext(),
                        if (nextDisable) "Yerel DVR devre dışı bırakıldı" else "Yerel DVR etkinleştirildi",
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

    private fun updateRecordingGeocoding(enabled: Boolean? = null, online: Boolean? = null) {
        val newEnabled = enabled ?: uiState.geocodingEnabled
        val newOnline = if (!newEnabled) false else (online ?: uiState.geocodingOnline)
        uiState = uiState.copy(geocodingEnabled = newEnabled, geocodingOnline = newOnline)
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

    private fun updateGeocodingUrl(url: String) {
        uiState = uiState.copy(geocodingCustomUrl = url)
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
        uiState = uiState.copy(audioRecordingEnabled = enabled)
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
        uiState = uiState.copy(telemetryOverlayEnabled = enabled)
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

    private fun toggleTelemetryField(field: String, add: Boolean) {
        val currFields = uiState.telemetryFields.toMutableSet()
        if (add) currFields.add(field) else currFields.remove(field)
        uiState = uiState.copy(telemetryFields = currFields)
        executor.execute {
            val jsonArray = org.json.JSONArray(currFields.toList())
            try {
                val body = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("accOn", jsonArray)
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
    }

    private fun updateCdrCleanup(
        enabled: Boolean? = null,
        reservedMb: Long? = null,
        hours: Int? = null,
        minFiles: Int? = null
    ) {
        val nextEnabled = enabled ?: uiState.cdrCleanupEnabled
        val nextReserved = (reservedMb?.toInt()) ?: uiState.cdrReservedSpaceMb
        val nextHours = hours ?: uiState.cdrProtectedHours
        val nextMinFiles = minFiles ?: uiState.cdrMinFilesKeep

        uiState = uiState.copy(
            cdrCleanupEnabled = nextEnabled,
            cdrReservedSpaceMb = nextReserved,
            cdrProtectedHours = nextHours,
            cdrMinFilesKeep = nextMinFiles
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
}
