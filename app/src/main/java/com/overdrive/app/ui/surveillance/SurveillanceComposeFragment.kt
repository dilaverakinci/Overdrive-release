package com.overdrive.app.ui.surveillance

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
import com.overdrive.app.surveillance.SurveillanceConfig
import com.overdrive.app.surveillance.SurveillanceConfigManager
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SurveillanceComposeFragment : Fragment() {

    private var executorService: ExecutorService? = null
    private val executor: ExecutorService
        get() = executorService?.takeUnless { it.isShutdown } ?: Executors.newSingleThreadExecutor().also { executorService = it }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val configManager = SurveillanceConfigManager()

    private var uiState by mutableStateOf(SurveillanceUiState())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            setContent {
                OverdriveTheme {
                    SurveillanceScreen(
                        state = uiState,
                        onTabSelected = { tab -> uiState = uiState.copy(selectedTab = tab) },
                        onToggleMaster = { enabled -> toggleMaster(enabled) },
                        onOperatingModeSelected = { mode -> updateSurveillanceKey("operatingMode", mode) },
                        onArmModeSelected = { mode -> updateSurveillanceKey("armMode", mode) },
                        onAccOffModeSelected = { mode -> updateSurveillanceKey("accOffMode", mode) },
                        onToggleKeepUsbPower = { v -> updateSurveillanceKey("keepUsbPowerOnAccOff", v) },
                        onToggleMobileDataKeepAlive = { v -> updateSurveillanceKey("mobileDataKeepAlive", v) },
                        onToggleDi5CloudKeepAlive = { v -> updateSurveillanceKey("di5CloudKeepAlive", v) },
                        onToggleLowPowerMode = { v -> updateSurveillanceKey("lowPowerMode", v) },
                        onLowSocCutoffChange = { v -> updatePowerKey("lowSocCutoffPercent", v) },
                        onToggleScreenDeterrent = { v -> updateSurveillanceKey("screenDeterrentEnabled", v) },
                        onScreenDeterrentDurationChange = { v -> updateSurveillanceKey("screenDeterrentDurationSeconds", v) },
                        onScreenDeterrentMessageChange = { msg -> updateSurveillanceKey("screenDeterrentMessage", msg) },
                        onToggleGeocodingEnabled = { v -> updateSurveillanceGeocoding(enabled = v) },
                        onToggleGeocodingOnline = { v -> updateSurveillanceGeocoding(online = v) },
                        onGeocodingCustomUrlChange = { url -> updateGeocodingUrl(url) },
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
                        onToggleSideCamBoost = { v -> toggleSideCamBoost(v) },
                        onToggleMotionHeatmap = { v -> updateSurveillanceKey("motionHeatmap", v) },
                        onToggleDiscardNightMotion = { v -> updateSurveillanceKey("discardEmptyMotionAtNight", v) },
                        onPreRecordSecondsChange = { v -> updateSurveillanceKey("preRecordSeconds", v) },
                        onPostRecordSecondsChange = { v -> updateSurveillanceKey("postRecordSeconds", v) },
                        onQualitySelected = { q -> updateRecordingKey("surveillanceQuality", q) },
                        onFpsSelected = { fps -> updateCameraKey("surveillanceTargetFps", fps) },
                        onCodecSelected = { c -> updateRecordingKey("recordingCodec", c) },
                        onToggleTelegramPing = { v -> updateSurveillanceKey("telegramSendStartPing", v) },
                        onToggleTelemetryOverlay = { v -> updateSurveillanceTelemetryOverlay(v) },
                        onToggleTelemetryField = { field, add -> toggleTelemetryField(field, add) },
                        onToggleOemDashcam = { v -> updateOemKey("enabled", v) },
                        onToggleOemTrigger = { v -> updateOemKey("triggerRecording", v) },
                        onToggleOemAutoCleanup = { v -> updateOemKey("autoCleanup", v) },
                        onStorageTypeSelected = { t -> updateSurveillanceKey("surveillanceStorageType", t) },
                        onStorageLimitChange = { lim -> updateSurveillanceKey("surveillanceLimitMb", lim) },
                        onToggleAutoCleanupEvents = { v -> updateSurveillanceKey("autoCleanupEvents", v) },
                        onToggleDiscardBrightEvents = { v -> updateSurveillanceKey("discardEmptyBrightMotionEvents", v) },
                        onToggleCdrCleanup = { v -> updateCdrCleanup(enabled = v) },
                        onCdrReservedSpaceChange = { mb -> updateCdrCleanup(reservedMb = mb.toLong()) },
                        onCdrProtectedHoursChange = { h -> updateCdrCleanup(hours = h) },
                        onCdrMinFilesKeepChange = { count -> updateCdrCleanup(minFiles = count) },
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

                val env = surv.optString("environmentPreset", "outdoor")
                val person = surv.optBoolean("detectPerson", true)
                val car = surv.optBoolean("detectCar", true)
                val bike = surv.optBoolean("detectBike", true)
                val animal = surv.optBoolean("detectAnimal", false)
                val sens = surv.optInt("sensitivityLevel", 3)
                val dist = surv.optInt("distancePreset", 3)
                val loitering = surv.optInt("loiteringTimeSeconds", 3)

                val cFront = surv.optBoolean("cameraFront", true)
                val cRight = surv.optBoolean("cameraRight", true)
                val cLeft = surv.optBoolean("cameraLeft", true)
                val cRear = surv.optBoolean("cameraRear", true)
                val heatmap = surv.optBoolean("motionHeatmap", false)
                val discardNight = surv.optBoolean("discardEmptyMotionAtNight", false)

                val preRec = surv.optInt("preRecordSeconds", 5)
                val postRec = surv.optInt("postRecordSeconds", 10)
                val qual = rec.optString("surveillanceQuality", "STANDARD")
                val fps = cam.optInt("surveillanceTargetFps", 15)
                val codec = rec.optString("recordingCodec", "H264")
                val tgPing = surv.optBoolean("telegramSendStartPing", false)

                val oemEn = oem.optBoolean("enabled", false)
                val oemTrig = oem.optBoolean("triggerRecording", false)
                val oemClean = oem.optBoolean("autoCleanup", true)

                val storType = surv.optString("surveillanceStorageType", "INTERNAL")
                val storLimit = surv.optInt("surveillanceLimitMb", 500)
                val autoClean = surv.optBoolean("autoCleanupEvents", true)
                val discardBright = surv.optBoolean("discardEmptyBrightMotionEvents", false)

                val powerConfig = fullConfig.optJSONObject("power") ?: JSONObject()
                val lowPower = surv.optBoolean("lowPowerMode", false)
                val lowSoc = powerConfig.optInt("lowSocCutoffPercent", 10)
                val screenDetEn = surv.optBoolean("screenDeterrentEnabled", false)
                val screenDetDur = surv.optInt("screenDeterrentDurationSeconds", 8)
                val screenDetMsg = surv.optString("screenDeterrentMessage", "")

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

                // Status check from daemon
                var isArmed = false
                try {
                    val conn = DaemonHttpClient.open("/api/surveillance/status", "GET", 1500, 2000)
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val statusObj = JSONObject(body)
                        isArmed = statusObj.optBoolean("armed", isMaster)
                    }
                    conn.disconnect()
                } catch (_: Throwable) {}

                mainHandler.post {
                    uiState = uiState.copy(
                        isEnabled = isMaster,
                        isArmed = isArmed,
                        operatingMode = opMode,
                        armMode = arm,
                        accOffMode = accOff,
                        keepUsbPowerOnAccOff = usbPower,
                        mobileDataKeepAlive = mobData,
                        di5CloudKeepAlive = di5,
                        lowPowerMode = lowPower,
                        lowSocCutoff = lowSoc,
                        screenDeterrentEnabled = screenDetEn,
                        screenDeterrentDuration = screenDetDur,
                        screenDeterrentMessage = screenDetMsg,
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
                        cameraFront = cFront,
                        cameraRight = cRight,
                        cameraLeft = cLeft,
                        cameraRear = cRear,
                        motionHeatmap = heatmap,
                        discardEmptyNightMotion = discardNight,
                        preRecordSeconds = preRec,
                        postRecordSeconds = postRec,
                        surveillanceQuality = qual,
                        surveillanceCameraFps = fps,
                        recordingCodec = codec,
                        telegramSendStartPing = tgPing,
                        oemDashcamEnabled = oemEn,
                        oemTriggerRecording = oemTrig,
                        oemAutoCleanup = oemClean,
                        storageType = storType,
                        storageLimitMb = storLimit,
                        autoCleanupEvents = autoClean,
                        discardEmptyBrightEvents = discardBright,
                        isLoading = false
                    )
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    uiState = uiState.copy(isLoading = false)
                    Toast.makeText(requireContext(), "Ayarlar yüklenemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleMaster(enabled: Boolean) {
        uiState = uiState.copy(isEnabled = enabled, isArmed = enabled)
        executor.execute {
            try {
                UnifiedConfigManager.setSurveillanceEnabled(enabled)
                UnifiedConfigManager.updateValues("surveillance", mapOf("surveillanceEnabled" to enabled))

                val endpoint = if (enabled) "/api/surveillance/enable" else "/api/surveillance/disable"
                try {
                    val conn = DaemonHttpClient.open(endpoint, "POST", 2000, 3000)
                    conn.responseCode
                    conn.disconnect()
                } catch (_: Throwable) {}

                mainHandler.post {
                    Toast.makeText(
                        requireContext(),
                        if (enabled) "Gözetim devrede" else "Gözetim kapatıldı",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Ayar kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateSurveillanceKey(key: String, value: Any) {
        // Optimistic UI update
        when (key) {
            "operatingMode" -> uiState = uiState.copy(operatingMode = value as String)
            "armMode" -> uiState = uiState.copy(armMode = value as String)
            "accOffMode" -> uiState = uiState.copy(accOffMode = value as String)
            "keepUsbPowerOnAccOff" -> uiState = uiState.copy(keepUsbPowerOnAccOff = value as Boolean)
            "mobileDataKeepAlive" -> uiState = uiState.copy(mobileDataKeepAlive = value as Boolean)
            "di5CloudKeepAlive" -> uiState = uiState.copy(di5CloudKeepAlive = value as Boolean)
            "environmentPreset" -> uiState = uiState.copy(environmentPreset = value as String)
            "detectPerson" -> uiState = uiState.copy(detectPerson = value as Boolean)
            "detectCar" -> uiState = uiState.copy(detectCar = value as Boolean)
            "detectBike" -> uiState = uiState.copy(detectBike = value as Boolean)
            "detectAnimal" -> uiState = uiState.copy(detectAnimal = value as Boolean)
            "sensitivityLevel" -> uiState = uiState.copy(sensitivityLevel = value as Int)
            "distancePreset" -> uiState = uiState.copy(distancePreset = value as Int)
            "loiteringTimeSeconds" -> uiState = uiState.copy(loiteringTimeSeconds = value as Int)
            "cameraFront" -> uiState = uiState.copy(cameraFront = value as Boolean)
            "cameraRight" -> uiState = uiState.copy(cameraRight = value as Boolean)
            "cameraLeft" -> uiState = uiState.copy(cameraLeft = value as Boolean)
            "cameraRear" -> uiState = uiState.copy(cameraRear = value as Boolean)
            "motionHeatmap" -> uiState = uiState.copy(motionHeatmap = value as Boolean)
            "discardEmptyMotionAtNight" -> uiState = uiState.copy(discardEmptyNightMotion = value as Boolean)
            "preRecordSeconds" -> uiState = uiState.copy(preRecordSeconds = value as Int)
            "postRecordSeconds" -> uiState = uiState.copy(postRecordSeconds = value as Int)
            "telegramSendStartPing" -> uiState = uiState.copy(telegramSendStartPing = value as Boolean)
            "surveillanceStorageType" -> uiState = uiState.copy(storageType = value as String)
            "surveillanceLimitMb" -> uiState = uiState.copy(storageLimitMb = value as Int)
            "autoCleanupEvents" -> uiState = uiState.copy(autoCleanupEvents = value as Boolean)
            "discardEmptyBrightMotionEvents" -> uiState = uiState.copy(discardEmptyBrightEvents = value as Boolean)
            "lowPowerMode" -> uiState = uiState.copy(lowPowerMode = value as Boolean)
            "screenDeterrentEnabled" -> uiState = uiState.copy(screenDeterrentEnabled = value as Boolean)
            "screenDeterrentDurationSeconds" -> uiState = uiState.copy(screenDeterrentDuration = value as Int)
            "screenDeterrentMessage" -> uiState = uiState.copy(screenDeterrentMessage = value as String)
        }

        executor.execute {
            try {
                UnifiedConfigManager.updateValues("surveillance", mapOf(key to value))
            } catch (t: Throwable) {
                mainHandler.post {
                    Toast.makeText(requireContext(), "Ayar kaydedilemedi: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateRecordingKey(key: String, value: Any) {
        when (key) {
            "surveillanceQuality" -> uiState = uiState.copy(surveillanceQuality = value as String)
            "recordingCodec" -> uiState = uiState.copy(recordingCodec = value as String)
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
        if (key == "surveillanceTargetFps") {
            uiState = uiState.copy(surveillanceCameraFps = value as Int)
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
            "enabled" -> uiState = uiState.copy(oemDashcamEnabled = value as Boolean)
            "triggerRecording" -> uiState = uiState.copy(oemTriggerRecording = value as Boolean)
            "autoCleanup" -> uiState = uiState.copy(oemAutoCleanup = value as Boolean)
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

    private fun applyEnvironmentPreset(preset: String) {
        val (sens, dist) = when (preset) {
            "outdoor" -> 3 to 3
            "indoor" -> 5 to 2
            "street" -> 2 to 4
            else -> 3 to 3
        }
        uiState = uiState.copy(
            environmentPreset = preset,
            sensitivityLevel = sens,
            distancePreset = dist
        )
        executor.execute {
            UnifiedConfigManager.updateValues(
                "surveillance",
                mapOf(
                    "environmentPreset" to preset,
                    "sensitivityLevel" to sens,
                    "distancePreset" to dist
                )
            )
        }
    }

    private fun toggleSideCamBoost(enabled: Boolean) {
        uiState = uiState.copy(sideCamBoost = enabled)
        executor.execute {
            UnifiedConfigManager.updateValues("surveillance", mapOf("sideCamBoost" to enabled))
        }
    }

    private fun updatePowerKey(key: String, value: Any) {
        if (key == "lowSocCutoffPercent") {
            uiState = uiState.copy(lowSocCutoff = value as Int)
        }
        executor.execute {
            try { UnifiedConfigManager.updateValues("power", mapOf(key to value)) } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceGeocoding(enabled: Boolean? = null, online: Boolean? = null) {
        val newEnabled = enabled ?: uiState.geocodingEnabled
        val newOnline = if (!newEnabled) false else (online ?: uiState.geocodingOnline)
        uiState = uiState.copy(geocodingEnabled = newEnabled, geocodingOnline = newOnline)
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

    private fun updateSurveillanceTelemetryOverlay(enabled: Boolean) {
        uiState = uiState.copy(telemetryOverlayEnabled = enabled)
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

    private fun toggleTelemetryField(field: String, add: Boolean) {
        val currFields = uiState.telemetryFields.toMutableSet()
        if (add) currFields.add(field) else currFields.remove(field)
        uiState = uiState.copy(telemetryFields = currFields)
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
