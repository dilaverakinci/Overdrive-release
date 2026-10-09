package com.overdrive.app.ui.fragment.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.overdrive.app.R
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Settings → Recording pane (Pure Native Material 3).
 *
 * Provides complete control over:
 *  - ACC-ON Recording Mode (None, Drive Mode, Continuous, Proximity Guard)
 *  - Camera Layout (Standard 360 grid vs Dashcam forward+sides) & Windshield sensor
 *  - Video Quality Tier (Economy, Standard, High, Premium, Max), Codec (H.264/H.265), FPS, Duration
 *  - Fisheye Rectification
 *  - Telemetry Overlay & Cabin Audio Recording
 *  - Storage Location (Internal, SD, USB), Limits and Auto-Cleanup
 */
class SettingsRecordingFragment : Fragment() {

    private val TAG = "SettingsRecording"

    // UI elements - Mode
    private lateinit var rowModeNone: View
    private lateinit var rowModeDrive: View
    private lateinit var rowModeContinuous: View
    private lateinit var rowModeProximity: View
    private lateinit var rbModeNone: RadioButton
    private lateinit var rbModeDrive: RadioButton
    private lateinit var rbModeContinuous: RadioButton
    private lateinit var rbModeProximity: RadioButton

    // UI elements - Layout
    private lateinit var groupCameraLayout: MaterialButtonToggleGroup
    private lateinit var swWindshieldCamera: MaterialSwitch

    // UI elements - Quality & Codec
    private lateinit var groupQualityTier: MaterialButtonToggleGroup
    private lateinit var groupCodec: MaterialButtonToggleGroup
    private lateinit var groupFps: MaterialButtonToggleGroup
    private lateinit var groupDuration: MaterialButtonToggleGroup
    private lateinit var sliderRectify: Slider
    private lateinit var tvRectifyValue: TextView

    // UI elements - Telemetry & Audio
    private lateinit var rowTelemetryOverlay: View
    private lateinit var swTelemetryOverlay: MaterialSwitch
    private lateinit var rowAudioRecording: View
    private lateinit var swAudioRecording: MaterialSwitch

    // UI elements - Storage
    private lateinit var groupStorageLocation: MaterialButtonToggleGroup
    private lateinit var tvStorageUsage: TextView
    private lateinit var progressStorage: LinearProgressIndicator
    private lateinit var sliderStorageLimit: Slider
    private lateinit var tvStorageLimitValue: TextView
    private lateinit var swAutoCleanup: MaterialSwitch

    private var isBinding = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings_recording_native, container, false)

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
        rowModeNone = view.findViewById(R.id.rowModeNone)
        rowModeDrive = view.findViewById(R.id.rowModeDrive)
        rowModeContinuous = view.findViewById(R.id.rowModeContinuous)
        rowModeProximity = view.findViewById(R.id.rowModeProximity)
        rbModeNone = view.findViewById(R.id.rbModeNone)
        rbModeDrive = view.findViewById(R.id.rbModeDrive)
        rbModeContinuous = view.findViewById(R.id.rbModeContinuous)
        rbModeProximity = view.findViewById(R.id.rbModeProximity)

        groupCameraLayout = view.findViewById(R.id.groupCameraLayout)
        swWindshieldCamera = view.findViewById(R.id.swWindshieldCamera)

        groupQualityTier = view.findViewById(R.id.groupQualityTier)
        groupCodec = view.findViewById(R.id.groupCodec)
        groupFps = view.findViewById(R.id.groupFps)
        groupDuration = view.findViewById(R.id.groupDuration)
        sliderRectify = view.findViewById(R.id.sliderRectify)
        tvRectifyValue = view.findViewById(R.id.tvRectifyValue)

        rowTelemetryOverlay = view.findViewById(R.id.rowTelemetryOverlay)
        swTelemetryOverlay = view.findViewById(R.id.swTelemetryOverlay)
        rowAudioRecording = view.findViewById(R.id.rowAudioRecording)
        swAudioRecording = view.findViewById(R.id.swAudioRecording)

        groupStorageLocation = view.findViewById(R.id.groupStorageLocation)
        tvStorageUsage = view.findViewById(R.id.tvStorageUsage)
        progressStorage = view.findViewById(R.id.progressStorage)
        sliderStorageLimit = view.findViewById(R.id.sliderStorageLimit)
        tvStorageLimitValue = view.findViewById(R.id.tvStorageLimitValue)
        swAutoCleanup = view.findViewById(R.id.swAutoCleanup)
    }

    private fun setupListeners() {
        // Mode clicks
        rowModeNone.setOnClickListener { if (!isBinding) updateRecordingMode("NONE") }
        rowModeDrive.setOnClickListener { if (!isBinding) updateRecordingMode("DRIVE_MODE") }
        rowModeContinuous.setOnClickListener { if (!isBinding) updateRecordingMode("CONTINUOUS") }
        rowModeProximity.setOnClickListener { if (!isBinding) updateRecordingMode("PROXIMITY_GUARD") }

        // Layout
        groupCameraLayout.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val layout = if (checkedId == R.id.btnLayoutDashcam) "dashcam" else "standard"
            saveLayoutSettings(layout, swWindshieldCamera.isChecked)
        }
        swWindshieldCamera.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            val layout = if (groupCameraLayout.checkedButtonId == R.id.btnLayoutDashcam) "dashcam" else "standard"
            saveLayoutSettings(layout, isChecked)
        }

        // Quality Tier
        groupQualityTier.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val tier = when (checkedId) {
                R.id.btnTierEconomy -> "ECONOMY"
                R.id.btnTierHigh -> "HIGH"
                R.id.btnTierPremium -> "PREMIUM"
                R.id.btnTierMax -> "MAX"
                else -> "STANDARD"
            }
            saveQualityProperty("recordingQuality", tier)
        }

        // Codec
        groupCodec.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val codec = if (checkedId == R.id.btnCodecH265) "H265" else "H264"
            saveQualityProperty("recordingCodec", codec)
        }

        // FPS
        groupFps.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val fps = when (checkedId) {
                R.id.btnFps10 -> 10
                R.id.btnFps20 -> 20
                R.id.btnFps25 -> 25
                R.id.btnFps30 -> 30
                else -> 15
            }
            saveQualityProperty("fps", fps)
        }

        // Clip Duration
        groupDuration.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val mins = when (checkedId) {
                R.id.btnDuration5m -> 5
                R.id.btnDuration10m -> 10
                else -> 2
            }
            saveQualityProperty("segmentDurationMinutes", mins)
        }

        // Fisheye Slider
        sliderRectify.addOnChangeListener { _, value, fromUser ->
            tvRectifyValue.text = "${value.toInt()}%"
            if (fromUser && !isBinding) {
                saveQualityProperty("rectifyStrength", value.toInt())
            }
        }

        // Telemetry & Audio
        rowTelemetryOverlay.setOnClickListener { swTelemetryOverlay.isChecked = !swTelemetryOverlay.isChecked }
        swTelemetryOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveTelemetryOverlay(isChecked)
        }

        rowAudioRecording.setOnClickListener { swAudioRecording.isChecked = !swAudioRecording.isChecked }
        swAudioRecording.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveAudioRecording(isChecked)
        }

        // Storage Location
        groupStorageLocation.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isBinding || !isChecked) return@addOnButtonCheckedListener
            val type = when (checkedId) {
                R.id.btnStorageSd -> "SD_CARD"
                R.id.btnStorageUsb -> "USB"
                else -> "INTERNAL"
            }
            saveStorageSettings(storageType = type)
        }

        // Storage Limit
        sliderStorageLimit.addOnChangeListener { _, value, fromUser ->
            tvStorageLimitValue.text = "${value.toInt()} GB"
            if (fromUser && !isBinding) {
                saveStorageSettings(limitGb = value.toInt())
            }
        }

        swAutoCleanup.setOnCheckedChangeListener { _, isChecked ->
            if (isBinding) return@setOnCheckedChangeListener
            saveStorageSettings(autoCleanup = isChecked)
        }
    }

    private fun updateModeRadioButtons(mode: String) {
        rbModeNone.isChecked = (mode == "NONE" || mode.isBlank())
        rbModeDrive.isChecked = (mode == "DRIVE_MODE")
        rbModeContinuous.isChecked = (mode == "CONTINUOUS")
        rbModeProximity.isChecked = (mode == "PROXIMITY_GUARD")
    }

    private fun loadAllSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Recording Mode
                val modeObj = httpGet("/api/recording/mode")
                val currentMode = modeObj?.optString("mode", "NONE") ?: "NONE"

                // 2. Camera Layout
                val layoutObj = httpGet("/api/settings/recording-layout")
                val currentLayout = layoutObj?.optString("layout", "standard") ?: "standard"
                val useWindshield = layoutObj?.optBoolean("useWindshield", false) ?: false

                // 3. Quality & Codec
                val qualityObj = httpGet("/api/settings/quality")
                val currentTier = qualityObj?.optString("recordingQuality", "STANDARD") ?: "STANDARD"
                val currentCodec = qualityObj?.optString("recordingCodec", "H264") ?: "H264"
                val currentFps = qualityObj?.optInt("fps", 15) ?: 15
                val currentDuration = qualityObj?.optInt("segmentDurationMinutes", 2) ?: 2
                val currentRectify = qualityObj?.optInt("rectifyStrength", 0) ?: 0

                // 4. Telemetry & Audio
                val telemetryObj = httpGet("/api/settings/telemetry-overlay")
                val telemetryAccOn = telemetryObj?.optBoolean("accOnEnabled", true) ?: true

                val audioObj = httpGet("/api/settings/audio-recording")
                val audioEnabled = audioObj?.optBoolean("enabled", false) ?: false

                // 5. Storage
                val storageObj = httpGet("/api/settings/storage")
                val storageType = storageObj?.optString("storageType", "INTERNAL") ?: "INTERNAL"
                val limitMb = storageObj?.optLong("storageLimitMb", 20480L) ?: 20480L
                val autoCleanup = storageObj?.optBoolean("autoCleanup", true) ?: true
                val usedBytes = storageObj?.optLong("usedBytes", 0L) ?: 0L
                val totalBytes = storageObj?.optLong("totalBytes", 0L) ?: 0L

                // Bind to UI
                isBinding = true
                updateModeRadioButtons(currentMode)

                if (currentLayout == "dashcam") {
                    groupCameraLayout.check(R.id.btnLayoutDashcam)
                } else {
                    groupCameraLayout.check(R.id.btnLayoutStandard)
                }
                swWindshieldCamera.isChecked = useWindshield

                when (currentTier) {
                    "ECONOMY" -> groupQualityTier.check(R.id.btnTierEconomy)
                    "HIGH" -> groupQualityTier.check(R.id.btnTierHigh)
                    "PREMIUM" -> groupQualityTier.check(R.id.btnTierPremium)
                    "MAX" -> groupQualityTier.check(R.id.btnTierMax)
                    else -> groupQualityTier.check(R.id.btnTierStandard)
                }

                if (currentCodec == "H265") groupCodec.check(R.id.btnCodecH265) else groupCodec.check(R.id.btnCodecH264)

                when (currentFps) {
                    10 -> groupFps.check(R.id.btnFps10)
                    20 -> groupFps.check(R.id.btnFps20)
                    25 -> groupFps.check(R.id.btnFps25)
                    30 -> groupFps.check(R.id.btnFps30)
                    else -> groupFps.check(R.id.btnFps15)
                }

                when (currentDuration) {
                    5 -> groupDuration.check(R.id.btnDuration5m)
                    10 -> groupDuration.check(R.id.btnDuration10m)
                    else -> groupDuration.check(R.id.btnDuration2m)
                }

                sliderRectify.value = currentRectify.toFloat().coerceIn(0f, 100f)
                tvRectifyValue.text = "$currentRectify%"

                swTelemetryOverlay.isChecked = telemetryAccOn
                swAudioRecording.isChecked = audioEnabled

                when (storageType) {
                    "SD_CARD" -> groupStorageLocation.check(R.id.btnStorageSd)
                    "USB" -> groupStorageLocation.check(R.id.btnStorageUsb)
                    else -> groupStorageLocation.check(R.id.btnStorageInternal)
                }

                val limitGb = (limitMb / 1024L).toInt().coerceIn(1, 100)
                sliderStorageLimit.value = limitGb.toFloat()
                tvStorageLimitValue.text = "$limitGb GB"
                swAutoCleanup.isChecked = autoCleanup

                if (totalBytes > 0) {
                    val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val pct = ((usedBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                    tvStorageUsage.text = String.format("%.1f GB / %.1f GB (%d%%)", usedGb, totalGb, pct)
                    progressStorage.progress = pct.coerceIn(0, 100)
                } else {
                    tvStorageUsage.text = "Available"
                    progressStorage.progress = 10
                }

                isBinding = false
            } catch (e: Exception) {
                Log.w(TAG, "Error loading recording settings: ${e.message}")
                isBinding = false
            }
        }
    }

    private fun updateRecordingMode(mode: String) {
        updateModeRadioButtons(mode)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject().apply { put("mode", mode) }
                httpPost("/api/recording/mode", payload)
                showMessage(getString(R.string.settings_recording_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update recording mode", e)
            }
        }
    }

    private fun saveLayoutSettings(layout: String, useWindshield: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject().apply {
                    put("layout", layout)
                    put("useWindshield", useWindshield)
                }
                httpPost("/api/settings/recording-layout", payload)
                showMessage(getString(R.string.settings_recording_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update layout settings", e)
            }
        }
    }

    private fun saveQualityProperty(key: String, value: Any) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject().apply { put(key, value) }
                httpPost("/api/settings/quality", payload)
                showMessage(getString(R.string.settings_recording_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update quality property $key", e)
            }
        }
    }

    private fun saveTelemetryOverlay(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject().apply { put("accOnEnabled", enabled) }
                httpPost("/api/settings/telemetry-overlay", payload)
                showMessage(getString(R.string.settings_recording_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update telemetry overlay", e)
            }
        }
    }

    private fun saveAudioRecording(enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject().apply { put("enabled", enabled) }
                httpPost("/api/settings/audio-recording", payload)
                showMessage(getString(R.string.settings_recording_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update audio recording", e)
            }
        }
    }

    private fun saveStorageSettings(storageType: String? = null, limitGb: Int? = null, autoCleanup: Boolean? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val payload = JSONObject()
                storageType?.let { payload.put("storageType", it) }
                limitGb?.let { payload.put("storageLimitMb", it * 1024L) }
                autoCleanup?.let { payload.put("autoCleanup", it) }
                httpPost("/api/settings/storage", payload)
                showMessage(getString(R.string.settings_recording_saved))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update storage settings", e)
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
