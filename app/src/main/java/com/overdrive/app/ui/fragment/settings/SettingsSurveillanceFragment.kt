package com.overdrive.app.ui.fragment.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.overdrive.app.R
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Settings → Surveillance pane (Pure Native Material 3).
 *
 * Provides complete control over:
 *  - Master Sentry toggle, operating mode, arm mode, parked capture mode
 *  - Low-power mode, battery cutoff %, USB & mobile data keep-alive
 *  - Environment presets, motion sensitivity, zones, loitering time
 *  - AI object tracking (Person, Vehicle, Bicycle, Animal) & active camera filters
 *  - Pre/post event buffers, quality tier, FPS & telemetry overlay
 *  - Screen deterrent warning & test trigger
 */
class SettingsSurveillanceFragment : Fragment() {

    private val TAG = "SettingsSurveillance"

    // 1. Mode & Power
    private lateinit var rowMasterEnable: View
    private lateinit var swSurvMaster: MaterialSwitch
    private lateinit var groupOperatingMode: MaterialButtonToggleGroup
    private lateinit var groupArmMode: MaterialButtonToggleGroup
    private lateinit var groupAccOffMode: MaterialButtonToggleGroup
    private lateinit var swLowPower: MaterialSwitch
    private lateinit var sliderLowSoc: Slider
    private lateinit var tvLowSocValue: TextView
    private lateinit var swKeepUsb: MaterialSwitch
    private lateinit var swMobileData: MaterialSwitch

    // 2. Detection & AI Tracking
    private lateinit var groupEnvPreset: MaterialButtonToggleGroup
    private lateinit var sliderSensitivity: Slider
    private lateinit var tvSensitivityValue: TextView
    private lateinit var groupDetectionZone: MaterialButtonToggleGroup
    private lateinit var sliderLoitering: Slider
    private lateinit var tvLoiteringValue: TextView
    private lateinit var chipDetectPerson: Chip
    private lateinit var chipDetectVehicle: Chip
    private lateinit var chipDetectBicycle: Chip
    private lateinit var chipDetectAnimal: Chip
    private lateinit var swCamFront: MaterialSwitch
    private lateinit var swCamRear: MaterialSwitch
    private lateinit var swCamLeft: MaterialSwitch
    private lateinit var swCamRight: MaterialSwitch

    // 3. Buffer & Video Quality
    private lateinit var sliderPreBuffer: Slider
    private lateinit var tvPreBufferValue: TextView
    private lateinit var sliderPostBuffer: Slider
    private lateinit var tvPostBufferValue: TextView
    private lateinit var groupSurvQuality: MaterialButtonToggleGroup
    private lateinit var groupSurvFps: MaterialButtonToggleGroup
    private lateinit var swSurvTelemetry: MaterialSwitch

    // 4. Deterrent Actions
    private lateinit var swScreenWarning: MaterialSwitch
    private lateinit var sliderWarningDuration: Slider
    private lateinit var tvWarningDurationValue: TextView
    private lateinit var btnTestScreenWarning: MaterialButton

    private var isBinding = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings_surveillance_native, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        loadAllSettings()
    }

    override fun onResume() {
        super.onResume()
        loadAllSettings()
    }

    private fun initViews(view: View) {
        rowMasterEnable = view.findViewById(R.id.rowMasterEnable)
        swSurvMaster = view.findViewById(R.id.swSurvMaster)
        groupOperatingMode = view.findViewById(R.id.groupOperatingMode)
        groupArmMode = view.findViewById(R.id.groupArmMode)
        groupAccOffMode = view.findViewById(R.id.groupAccOffMode)
        swLowPower = view.findViewById(R.id.swLowPower)
        sliderLowSoc = view.findViewById(R.id.sliderLowSoc)
        tvLowSocValue = view.findViewById(R.id.tvLowSocValue)
        swKeepUsb = view.findViewById(R.id.swKeepUsb)
        swMobileData = view.findViewById(R.id.swMobileData)

        groupEnvPreset = view.findViewById(R.id.groupEnvPreset)
        sliderSensitivity = view.findViewById(R.id.sliderSensitivity)
        tvSensitivityValue = view.findViewById(R.id.tvSensitivityValue)
        groupDetectionZone = view.findViewById(R.id.groupDetectionZone)
        sliderLoitering = view.findViewById(R.id.sliderLoitering)
        tvLoiteringValue = view.findViewById(R.id.tvLoiteringValue)
        chipDetectPerson = view.findViewById(R.id.chipDetectPerson)
        chipDetectVehicle = view.findViewById(R.id.chipDetectVehicle)
        chipDetectBicycle = view.findViewById(R.id.chipDetectBicycle)
        chipDetectAnimal = view.findViewById(R.id.chipDetectAnimal)
        swCamFront = view.findViewById(R.id.swCamFront)
        swCamRear = view.findViewById(R.id.swCamRear)
        swCamLeft = view.findViewById(R.id.swCamLeft)
        swCamRight = view.findViewById(R.id.swCamRight)

        sliderPreBuffer = view.findViewById(R.id.sliderPreBuffer)
        tvPreBufferValue = view.findViewById(R.id.tvPreBufferValue)
        sliderPostBuffer = view.findViewById(R.id.sliderPostBuffer)
        tvPostBufferValue = view.findViewById(R.id.tvPostBufferValue)
        groupSurvQuality = view.findViewById(R.id.groupSurvQuality)
        groupSurvFps = view.findViewById(R.id.groupSurvFps)
        swSurvTelemetry = view.findViewById(R.id.swSurvTelemetry)

        swScreenWarning = view.findViewById(R.id.swScreenWarning)
        sliderWarningDuration = view.findViewById(R.id.sliderWarningDuration)
        tvWarningDurationValue = view.findViewById(R.id.tvWarningDurationValue)
        btnTestScreenWarning = view.findViewById(R.id.btnTestScreenWarning)
    }

    private fun setupListeners() {
        // Master switch
        rowMasterEnable.setOnClickListener { swSurvMaster.isChecked = !swSurvMaster.isChecked }
        swSurvMaster.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveSurvConfig("enabled", isChecked)
        }

        // Operating Mode
        groupOperatingMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val mode = if (checkedId == R.id.btnOpOnOnly) "onOnly" else "onAndOff"
            saveSurvConfig("operatingMode", mode)
        }

        // Arm Mode
        groupArmMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val mode = if (checkedId == R.id.btnArmPower) "power" else "lock"
            saveSurvConfig("armMode", mode)
        }

        // ACC-OFF Mode
        groupAccOffMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val mode = if (checkedId == R.id.btnAccOffContinuous) "continuous" else "smart"
            saveSurvConfig("accOffMode", mode)
        }

        // Low power
        swLowPower.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveSurvConfig("lowPowerMode", isChecked)
        }

        // Low SoC cutoff
        sliderLowSoc.addOnChangeListener { _, value, fromUser ->
            tvLowSocValue.text = "${value.toInt()}%"
            if (fromUser && !isBinding) {
                saveSurvConfig("lowSocCutoffPercent", value.toInt())
            }
        }

        // Keep USB
        swKeepUsb.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveSurvConfig("keepUsbPower", isChecked)
        }

        // Keep mobile data
        swMobileData.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveSurvConfig("keepMobileData", isChecked)
        }

        // Environment Preset
        groupEnvPreset.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val preset = when (checkedId) {
                R.id.btnEnvGarage -> "garage"
                R.id.btnEnvStreet -> "street"
                else -> "outdoor"
            }
            saveSurvConfig("envPreset", preset)
        }

        // Sensitivity
        sliderSensitivity.addOnChangeListener { _, value, fromUser ->
            val v = value.toInt()
            tvSensitivityValue.text = when (v) {
                1 -> "1 (Lowest)"
                2 -> "2 (Low)"
                4 -> "4 (High)"
                5 -> "5 (Highest)"
                else -> "3 (Default)"
            }
            if (fromUser && !isBinding) {
                saveSurvConfig("sensitivity", v)
            }
        }

        // Detection Zone
        groupDetectionZone.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val zone = when (checkedId) {
                R.id.btnZoneClose -> "close"
                R.id.btnZoneExtended -> "extended"
                else -> "normal"
            }
            saveSurvConfig("detectionZone", zone)
        }

        // Loitering Time
        sliderLoitering.addOnChangeListener { _, value, fromUser ->
            tvLoiteringValue.text = "${value.toInt()}s"
            if (fromUser && !isBinding) {
                saveSurvConfig("loiteringTime", value.toInt())
            }
        }

        // AI Object Tracking Chips
        chipDetectPerson.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("detectPerson", isChecked)
        }
        chipDetectVehicle.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("detectCar", isChecked)
        }
        chipDetectBicycle.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("detectBike", isChecked)
        }
        chipDetectAnimal.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("detectAnimal", isChecked)
        }

        // Camera Controls
        swCamFront.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("cameraFront", isChecked)
        }
        swCamRear.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("cameraRear", isChecked)
        }
        swCamLeft.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("cameraLeft", isChecked)
        }
        swCamRight.setOnCheckedChangeListener { _, isChecked ->
            if (!isBinding) saveSurvConfig("cameraRight", isChecked)
        }

        // Pre/Post Buffers
        sliderPreBuffer.addOnChangeListener { _, value, fromUser ->
            tvPreBufferValue.text = "${value.toInt()}s"
            if (fromUser && !isBinding) {
                saveSurvConfig("preRecordSeconds", value.toInt())
            }
        }
        sliderPostBuffer.addOnChangeListener { _, value, fromUser ->
            tvPostBufferValue.text = "${value.toInt()}s"
            if (fromUser && !isBinding) {
                saveSurvConfig("postRecordSeconds", value.toInt())
            }
        }

        // Quality Tier
        groupSurvQuality.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val tier = when (checkedId) {
                R.id.btnSurvTierEco -> "ECONOMY"
                R.id.btnSurvTierHigh -> "HIGH"
                R.id.btnSurvTierPrem -> "PREMIUM"
                R.id.btnSurvTierMax -> "MAX"
                else -> "STANDARD"
            }
            saveSurvConfig("recordingQuality", tier)
        }

        // FPS
        groupSurvFps.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val fps = when (checkedId) {
                R.id.btnSurvFps10 -> 10
                R.id.btnSurvFps20 -> 20
                R.id.btnSurvFps25 -> 25
                R.id.btnSurvFps30 -> 30
                else -> 15
            }
            saveSurvConfig("fps", fps)
        }

        // Telemetry Overlay
        swSurvTelemetry.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveSurvTelemetry(isChecked)
        }

        // Screen Deterrent
        swScreenWarning.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveSurvConfig("screenDeterrentEnabled", isChecked)
        }
        sliderWarningDuration.addOnChangeListener { _, value, fromUser ->
            tvWarningDurationValue.text = "${value.toInt()}s"
            if (fromUser && !isBinding) {
                saveSurvConfig("screenDeterrentDuration", value.toInt())
            }
        }
        btnTestScreenWarning.setOnClickListener {
            testScreenDeterrent()
        }
    }

    private fun loadAllSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cfg = httpGet("/api/surveillance/config") ?: JSONObject()
                val telemetryObj = httpGet("/api/settings/telemetry-overlay")

                isBinding = true

                // Master & Power
                swSurvMaster.isChecked = cfg.optBoolean("enabled", false)

                val opMode = cfg.optString("operatingMode", "onAndOff")
                if (opMode == "onOnly") groupOperatingMode.check(R.id.btnOpOnOnly) else groupOperatingMode.check(R.id.btnOpOnOff)

                val armMode = cfg.optString("armMode", "lock")
                if (armMode == "power") groupArmMode.check(R.id.btnArmPower) else groupArmMode.check(R.id.btnArmLock)

                val accOffMode = cfg.optString("accOffMode", "smart")
                if (accOffMode == "continuous") groupAccOffMode.check(R.id.btnAccOffContinuous) else groupAccOffMode.check(R.id.btnAccOffSmart)

                swLowPower.isChecked = cfg.optBoolean("lowPowerMode", false)

                val cutoff = cfg.optInt("lowSocCutoffPercent", 10).coerceIn(0, 30)
                sliderLowSoc.value = cutoff.toFloat()
                tvLowSocValue.text = "$cutoff%"

                swKeepUsb.isChecked = cfg.optBoolean("keepUsbPower", true)
                swMobileData.isChecked = cfg.optBoolean("keepMobileData", false)

                // Detection & AI
                val envPreset = cfg.optString("envPreset", "outdoor")
                when (envPreset) {
                    "garage" -> groupEnvPreset.check(R.id.btnEnvGarage)
                    "street" -> groupEnvPreset.check(R.id.btnEnvStreet)
                    else -> groupEnvPreset.check(R.id.btnEnvOutdoor)
                }

                val sens = cfg.optInt("sensitivity", 3).coerceIn(1, 5)
                sliderSensitivity.value = sens.toFloat()
                tvSensitivityValue.text = when (sens) {
                    1 -> "1 (Lowest)"
                    2 -> "2 (Low)"
                    4 -> "4 (High)"
                    5 -> "5 (Highest)"
                    else -> "3 (Default)"
                }

                val zone = cfg.optString("detectionZone", "normal")
                when (zone) {
                    "close" -> groupDetectionZone.check(R.id.btnZoneClose)
                    "extended" -> groupDetectionZone.check(R.id.btnZoneExtended)
                    else -> groupDetectionZone.check(R.id.btnZoneNormal)
                }

                val loitering = cfg.optInt("loiteringTime", 3).coerceIn(1, 10)
                sliderLoitering.value = loitering.toFloat()
                tvLoiteringValue.text = "${loitering}s"

                chipDetectPerson.isChecked = cfg.optBoolean("detectPerson", true)
                chipDetectVehicle.isChecked = cfg.optBoolean("detectCar", true)
                chipDetectBicycle.isChecked = cfg.optBoolean("detectBike", false)
                chipDetectAnimal.isChecked = cfg.optBoolean("detectAnimal", false)

                swCamFront.isChecked = cfg.optBoolean("cameraFront", true)
                swCamRear.isChecked = cfg.optBoolean("cameraRear", true)
                swCamLeft.isChecked = cfg.optBoolean("cameraLeft", true)
                swCamRight.isChecked = cfg.optBoolean("cameraRight", true)

                // Buffer & Quality
                val preRec = cfg.optInt("preRecordSeconds", 5).coerceIn(2, 15)
                sliderPreBuffer.value = preRec.toFloat()
                tvPreBufferValue.text = "${preRec}s"

                val postRec = cfg.optInt("postRecordSeconds", 10).coerceIn(5, 30)
                sliderPostBuffer.value = postRec.toFloat()
                tvPostBufferValue.text = "${postRec}s"

                val tier = cfg.optString("recordingQuality", "HIGH")
                when (tier) {
                    "ECONOMY" -> groupSurvQuality.check(R.id.btnSurvTierEco)
                    "STANDARD" -> groupSurvQuality.check(R.id.btnSurvTierStd)
                    "PREMIUM" -> groupSurvQuality.check(R.id.btnSurvTierPrem)
                    "MAX" -> groupSurvQuality.check(R.id.btnSurvTierMax)
                    else -> groupSurvQuality.check(R.id.btnSurvTierHigh)
                }

                val fps = cfg.optInt("fps", 15)
                when (fps) {
                    10 -> groupSurvFps.check(R.id.btnSurvFps10)
                    20 -> groupSurvFps.check(R.id.btnSurvFps20)
                    25 -> groupSurvFps.check(R.id.btnSurvFps25)
                    30 -> groupSurvFps.check(R.id.btnSurvFps30)
                    else -> groupSurvFps.check(R.id.btnSurvFps15)
                }

                swSurvTelemetry.isChecked = telemetryObj?.optBoolean("surveillanceEnabled", true) ?: true

                // Deterrent
                swScreenWarning.isChecked = cfg.optBoolean("screenDeterrentEnabled", false)
                val duration = cfg.optInt("screenDeterrentDuration", 8).coerceIn(3, 30)
                sliderWarningDuration.value = duration.toFloat()
                tvWarningDurationValue.text = "${duration}s"

                isBinding = false
            } catch (e: Exception) {
                Log.w(TAG, "Error loading surveillance settings: ${e.message}")
                isBinding = false
            }
        }
    }

    private fun saveSurvConfig(key: String, value: Any) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject().apply { put(key, value) }
                httpPost("/api/surveillance/config", payload)
                showMessage(getString(R.string.settings_surveillance_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update surveillance setting $key", e)
            }
        }
    }

    private fun saveSurvTelemetry(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject().apply { put("surveillanceEnabled", enabled) }
                httpPost("/api/settings/telemetry-overlay", payload)
                showMessage(getString(R.string.settings_surveillance_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update surveillance telemetry", e)
            }
        }
    }

    private fun testScreenDeterrent() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                httpPost("/api/surveillance/screen-deterrent/test", JSONObject())
                showMessage(getString(R.string.settings_surveillance_test_triggered))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to trigger screen deterrent test", e)
            }
        }
    }

    private fun showMessage(msg: String) {
        view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_SHORT).show() }
    }

    private suspend fun httpGet(path: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open(path, "GET", 2500, 2500)
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                JSONObject(text)
            } else {
                conn.disconnect()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun httpPost(path: String, payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open(path, "POST", 3000, 3000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.bufferedWriter().use { it.write(payload.toString()) }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (e: Exception) {
            false
        }
    }
}
