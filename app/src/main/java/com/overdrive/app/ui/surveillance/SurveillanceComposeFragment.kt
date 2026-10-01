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
                        onToggleDiLink5KeepAlive = { v -> updateSurveillanceKey("diLink5KeepAlive", v) },
                        onToggleLowPowerMode = { v -> updateSurveillanceKey("lowPowerMode", v) },
                        onLowSocCutoffChange = { v -> updatePowerKey("lowSocCutoffPercent", v) },
                        onToggleParkingIntelligence = { v -> updateSurveillanceKey("parkingIntelligenceEnabled", v) },
                        onToggleScreenDeterrent = { v -> updateSurveillanceKey("screenDeterrentEnabled", v) },
                        onScreenDeterrentDurationChange = { v -> updateSurveillanceKey("screenDeterrentDurationSeconds", v) },
                        onScreenDeterrentMessageChange = { msg -> updateSurveillanceKey("screenDeterrentMessage", msg) },
                        onScreenDeterrentThemeChange = { t -> updateSurveillanceKey("screenDeterrentTheme", t) },
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
                        onApproachTriggerChange = { v -> updateSurveillanceKey("approachTriggerSeconds", v) },
                        onShadowFilterSelected = { s -> updateSurveillanceKey("shadowFilter", s) },
                        onToggleRecordOnStrongMotion = { v -> updateSurveillanceKey("recordOnStrongMotion", v) },
                        onToggleDiscardEmptyMotion = { v -> updateSurveillanceKey("discardEmptyMotion", v) },
                        onToggleDiscardNightMotion = { v -> updateSurveillanceKey("discardEmptyMotionAtNight", v) },
                        onToggleCameraFront = { v -> updateSurveillanceKey("cameraFront", v) },
                        onToggleCameraRight = { v -> updateSurveillanceKey("cameraRight", v) },
                        onToggleCameraLeft = { v -> updateSurveillanceKey("cameraLeft", v) },
                        onToggleCameraRear = { v -> updateSurveillanceKey("cameraRear", v) },
                        onToggleSideCamBoost = { v -> toggleSideCamBoost(v) },
                        onPreRecordSecondsChange = { v -> updateSurveillanceKey("preRecordSeconds", v) },
                        onPostRecordSecondsChange = { v -> updateSurveillanceKey("postRecordSeconds", v) },
                        onQualitySelected = { q -> updateRecordingKey("surveillanceQuality", q) },
                        onCodecSelected = { c -> updateRecordingKey("recordingCodec", c) },
                        onFpsSelected = { fps -> updateCameraKey("surveillanceTargetFps", fps) },
                        onClipDurationSelected = { mins -> updateSurveillanceSegmentDuration(mins) },
                        onRecordingLayoutSelected = { l -> updateSurveillanceLayout(l) },
                        onToggleTelemetryOverlay = { v -> updateSurveillanceTelemetryOverlay(v) },
                        onToggleTelemetryField = { field, add -> toggleTelemetryField(field, add) },
                        onRectifyStrengthChange = { s -> updateSurveillanceRectify(s) },
                        onOemRecordingModeSelected = { mode -> updateOemKey("recordingMode", mode) },
                        onToggleOemTelemetryOverlay = { v -> updateOemKey("telemetryOverlay", v) },
                        onToggleOemTelemetryField = { f, add -> toggleOemTelemetryField(f, add) },
                        onToggleNativeDvr = { toggleNativeDvr() },
                        onStorageTypeSelected = { t -> updateSurveillanceStorageType(t) },
                        onStorageLimitChange = { lim -> updateSurveillanceStorageLimit(lim) },
                        onToggleAutoCleanupEvents = { v -> updateSurveillanceKey("autoCleanupEvents", v) },
                        onToggleDiscardBrightEvents = { v -> updateSurveillanceKey("discardEmptyBrightMotionEvents", v) },
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
                val parkingIntel = surv.optBoolean("parkingIntelligenceEnabled", false)

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

                // Status check from daemon
                var isArmed = false
                var oemUnset = true
                var dvrDisabled = false
                try {
                    val conn = DaemonHttpClient.open("/api/surveillance/status", "GET", 1500, 2000)
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().readText()
                        val statusObj = JSONObject(body)
                        isArmed = statusObj.optBoolean("armed", isMaster)
                    }
                    conn.disconnect()
                } catch (_: Throwable) {}

                try {
                    val oemConn = DaemonHttpClient.open("/api/oem-dashcam/status", "GET", 1500, 2000)
                    if (oemConn.responseCode == 200) {
                        val oemBody = oemConn.inputStream.bufferedReader().readText()
                        val oemJson = JSONObject(oemBody)
                        oemUnset = oemJson.optBoolean("cameraProbeUnset", true)
                        dvrDisabled = oemJson.optBoolean("nativeDvrDisabled", false)
                    }
                    oemConn.disconnect()
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
                        diLink5KeepAlive = diLink5,
                        lowPowerMode = lowPower,
                        lowSocCutoff = lowSoc,
                        parkingIntelligenceEnabled = parkingIntel,
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
                        oemRecordingMode = oemMode,
                        oemTelemetryOverlay = oemTelem,
                        oemTelemetryFields = oemTelemSet,
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
            "diLink5KeepAlive" -> uiState = uiState.copy(diLink5KeepAlive = value as Boolean)
            "parkingIntelligenceEnabled" -> uiState = uiState.copy(parkingIntelligenceEnabled = value as Boolean)
            "environmentPreset" -> uiState = uiState.copy(environmentPreset = value as String)
            "detectPerson" -> uiState = uiState.copy(detectPerson = value as Boolean)
            "detectCar" -> uiState = uiState.copy(detectCar = value as Boolean)
            "detectBike" -> uiState = uiState.copy(detectBike = value as Boolean)
            "detectAnimal" -> uiState = uiState.copy(detectAnimal = value as Boolean)
            "sensitivityLevel" -> uiState = uiState.copy(sensitivityLevel = value as Int)
            "distancePreset" -> uiState = uiState.copy(distancePreset = value as Int)
            "loiteringTimeSeconds" -> uiState = uiState.copy(loiteringTimeSeconds = value as Int)
            "approachTriggerSeconds" -> uiState = uiState.copy(approachTriggerSeconds = value as Int)
            "shadowFilter" -> uiState = uiState.copy(shadowFilter = value as String)
            "recordOnStrongMotion" -> uiState = uiState.copy(recordOnStrongMotion = value as Boolean)
            "discardEmptyMotion" -> uiState = uiState.copy(discardEmptyMotion = value as Boolean)
            "cameraFront" -> uiState = uiState.copy(cameraFront = value as Boolean)
            "cameraRight" -> uiState = uiState.copy(cameraRight = value as Boolean)
            "cameraLeft" -> uiState = uiState.copy(cameraLeft = value as Boolean)
            "cameraRear" -> uiState = uiState.copy(cameraRear = value as Boolean)
            "discardEmptyMotionAtNight" -> uiState = uiState.copy(discardEmptyNightMotion = value as Boolean)
            "preRecordSeconds" -> uiState = uiState.copy(preRecordSeconds = value as Int)
            "postRecordSeconds" -> uiState = uiState.copy(postRecordSeconds = value as Int)
            "autoCleanupEvents" -> uiState = uiState.copy(autoCleanupEvents = value as Boolean)
            "discardEmptyBrightMotionEvents" -> uiState = uiState.copy(discardEmptyBrightEvents = value as Boolean)
            "lowPowerMode" -> uiState = uiState.copy(lowPowerMode = value as Boolean)
            "screenDeterrentEnabled" -> uiState = uiState.copy(screenDeterrentEnabled = value as Boolean)
            "screenDeterrentDurationSeconds" -> uiState = uiState.copy(screenDeterrentDuration = value as Int)
            "screenDeterrentMessage" -> uiState = uiState.copy(screenDeterrentMessage = value as String)
            "screenDeterrentTheme" -> uiState = uiState.copy(screenDeterrentTheme = value as String)
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

    private fun updateSurveillanceSegmentDuration(mins: Int) {
        uiState = uiState.copy(segmentDurationMinutes = mins)
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("surveillanceSegmentDurationMinutes" to mins))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceLayout(layout: String) {
        uiState = uiState.copy(recordingLayout = layout)
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("surveillanceRecordingLayout" to layout))
            } catch (_: Throwable) {}
        }
    }

    private fun updateSurveillanceRectify(strength: Int) {
        uiState = uiState.copy(rectifyStrength = strength)
        executor.execute {
            try {
                UnifiedConfigManager.updateValues("recording", mapOf("surveillanceRectifyStrength" to strength))
            } catch (_: Throwable) {}
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

    private fun updateSurveillanceStorageType(typeStr: String) {
        uiState = uiState.copy(storageType = typeStr)
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
            storageLimitMb = limitMb,
            storageLimitText = "$limitMb MB sınırı",
            storageUsedPercent = usedPercent
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
