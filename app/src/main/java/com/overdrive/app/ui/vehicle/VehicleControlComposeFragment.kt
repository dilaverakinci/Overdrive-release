package com.overdrive.app.ui.vehicle

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.overdrive.app.byd.cloud.BydCloudConfig
import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.OperationMode
import com.overdrive.app.domain.repository.RepositoryProvider
import androidx.navigation.fragment.findNavController
import com.overdrive.app.R
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import com.overdrive.app.telemetry.VehiclePowerEstimator

/**
 * 100% Jetpack Compose Native Fragment for Vehicle Control & Live Cockpit.
 * Directly replaces WebViewFragment for /vehicle (vehicle-control.html).
 */
class VehicleControlComposeFragment : Fragment() {

    private var uiState by mutableStateOf(VehicleControlUiState())

    private val mainHandler = Handler(Looper.getMainLooper())
    private var workerExecutor = Executors.newSingleThreadExecutor()
    private var isRefreshing = false

    private val refreshRunnable = Runnable {
        refreshVehicleStateAsync()
    }

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
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    VehicleControlScreen(
                        state = uiState,
                        onLockClick = {
                            executeCommand("/api/vehicle/lock", null, "Araç kilitlendi") {
                                uiState = uiState.copy(security = uiState.security.copy(isLocked = true))
                            }
                        },
                        onUnlockClick = {
                            executeCommand("/api/vehicle/unlock", null, "Araç kilitleri açıldı") {
                                uiState = uiState.copy(security = uiState.security.copy(isLocked = false))
                            }
                        },
                        onFlashClick = {
                            executeCommand("/api/vehicle/flash", null, "Flaşörler yakıldı")
                        },
                        onFindCarClick = {
                            executeCommand("/api/vehicle/find-car", null, "Aracı bul: Korna ve ışıklar aktif")
                        },
                        onToggleTrunk = {
                            val next = !uiState.doors.trunkOpen
                            val body = if (next) "{\"action\":\"open\"}" else "{\"action\":\"close\"}"
                            executeCommand("/api/vehicle/trunk", body, if (next) "Bagaj kapağı açılıyor" else "Bagaj kapağı kapatılıyor") {
                                uiState = uiState.copy(doors = uiState.doors.copy(trunkOpen = next))
                            }
                        },
                        onToggleHood = {
                            val next = !uiState.doors.hoodOpen
                            executeCommand("/api/vehicle/hood", null, if (next) "Ön kaput açıldı" else "Ön kaput kapatıldı") {
                                uiState = uiState.copy(doors = uiState.doors.copy(hoodOpen = next))
                            }
                        },
                        onWindowsCloseAll = {
                            executeCommand("/api/vehicle/window", "{\"action\":\"close\"}", "Tüm camlar kapatılıyor") {
                                uiState = uiState.copy(
                                    windows = uiState.windows.copy(
                                        frontLeftOpen = false,
                                        frontRightOpen = false,
                                        rearLeftOpen = false,
                                        rearRightOpen = false,
                                        isVentMode = false
                                    )
                                )
                            }
                        },
                        onWindowsOpenAll = {
                            executeCommand("/api/vehicle/window", "{\"action\":\"open\"}", "Tüm camlar açılıyor") {
                                uiState = uiState.copy(
                                    windows = uiState.windows.copy(
                                        frontLeftOpen = true,
                                        frontRightOpen = true,
                                        rearLeftOpen = true,
                                        rearRightOpen = true,
                                        isVentMode = false
                                    )
                                )
                            }
                        },
                        onWindowsVentMode = {
                            val next = !uiState.windows.isVentMode
                            val action = if (next) "vent" else "close"
                            executeCommand("/api/vehicle/window", "{\"action\":\"$action\"}", if (next) "Havalandırma modu aktif (2 cm)" else "Camlar kapatıldı") {
                                uiState = uiState.copy(windows = uiState.windows.copy(isVentMode = next))
                            }
                        },
                        onToggleSunroof = {
                            val next = !uiState.windows.sunroofOpen
                            val action = if (next) "open" else "close"
                            executeCommand("/api/vehicle/window", "{\"action\":\"$action\",\"target\":\"sunroof\"}", if (next) "Sunroof / perde açılıyor" else "Sunroof / perde kapatılıyor") {
                                uiState = uiState.copy(windows = uiState.windows.copy(sunroofOpen = next))
                            }
                        },
                        onToggleClimate = {
                            val next = !uiState.climate.isAcOn
                            val action = if (next) "on" else "off"
                            executeCommand("/api/vehicle/climate", "{\"action\":\"$action\"}", if (next) "Klima çalıştırıldı" else "Klima durduruldu") {
                                uiState = uiState.copy(climate = uiState.climate.copy(isAcOn = next))
                            }
                        },
                        onTempDown = {
                            if (uiState.climate.targetTemp > 16) {
                                val next = uiState.climate.targetTemp - 1
                                executeCommand("/api/vehicle/climate", "{\"temperature\":$next}", "$next°C") {
                                    uiState = uiState.copy(climate = uiState.climate.copy(targetTemp = next))
                                }
                            }
                        },
                        onTempUp = {
                            if (uiState.climate.targetTemp < 30) {
                                val next = uiState.climate.targetTemp + 1
                                executeCommand("/api/vehicle/climate", "{\"temperature\":$next}", "$next°C") {
                                    uiState = uiState.copy(climate = uiState.climate.copy(targetTemp = next))
                                }
                            }
                        },
                        onFanDown = {
                            if (uiState.climate.fanLevel > 1) {
                                val next = uiState.climate.fanLevel - 1
                                executeCommand("/api/vehicle/climate", "{\"fan\":$next}", "Fan: $next") {
                                    uiState = uiState.copy(climate = uiState.climate.copy(fanLevel = next))
                                }
                            }
                        },
                        onFanUp = {
                            if (uiState.climate.fanLevel < 7) {
                                val next = uiState.climate.fanLevel + 1
                                executeCommand("/api/vehicle/climate", "{\"fan\":$next}", "Fan: $next") {
                                    uiState = uiState.copy(climate = uiState.climate.copy(fanLevel = next))
                                }
                            }
                        },
                        onToggleBatteryHeat = {
                            val next = !uiState.climate.isBatteryHeatOn
                            val action = if (next) "on" else "off"
                            executeCommand("/api/vehicle/battery-heat", "{\"action\":\"$action\"}", if (next) "Batarya ön ısıtma başlatıldı" else "Batarya ısıtma durduruldu") {
                                uiState = uiState.copy(climate = uiState.climate.copy(isBatteryHeatOn = next))
                            }
                        },
                        onCycleDriverSeatHeat = {
                            val next = (uiState.comfort.driverSeatHeat + 1) % 4
                            executeCommand("/api/vehicle/seat", "{\"seat\":\"driver\",\"heat\":$next}", "Sürücü koltuk ısıtma: $next") {
                                uiState = uiState.copy(comfort = uiState.comfort.copy(driverSeatHeat = next))
                            }
                        },
                        onCycleDriverSeatVent = {
                            val next = (uiState.comfort.driverSeatVent + 1) % 4
                            executeCommand("/api/vehicle/seat", "{\"seat\":\"driver\",\"vent\":$next}", "Sürücü koltuk havalandırma: $next") {
                                uiState = uiState.copy(comfort = uiState.comfort.copy(driverSeatVent = next))
                            }
                        },
                        onCyclePassengerSeatHeat = {
                            val next = (uiState.comfort.passengerSeatHeat + 1) % 4
                            executeCommand("/api/vehicle/seat", "{\"seat\":\"passenger\",\"heat\":$next}", "Yolcu koltuk ısıtma: $next") {
                                uiState = uiState.copy(comfort = uiState.comfort.copy(passengerSeatHeat = next))
                            }
                        },
                        onCyclePassengerSeatVent = {
                            val next = (uiState.comfort.passengerSeatVent + 1) % 4
                            executeCommand("/api/vehicle/seat", "{\"seat\":\"passenger\",\"vent\":$next}", "Yolcu koltuk havalandırma: $next") {
                                uiState = uiState.copy(comfort = uiState.comfort.copy(passengerSeatVent = next))
                            }
                        },
                        onToggleSteeringHeat = {
                            val next = !uiState.comfort.steeringHeatOn
                            executeCommand("/api/vehicle/seat", "{\"steering\":$next}", if (next) "Direksiyon ısıtma açık" else "Direksiyon ısıtma kapalı") {
                                uiState = uiState.copy(comfort = uiState.comfort.copy(steeringHeatOn = next))
                            }
                        },
                        onToggleMirrors = {
                            val next = !uiState.security.mirrorsFolded
                            val action = if (next) "fold" else "unfold"
                            executeCommand("/api/vehicle/mirrors", "{\"action\":\"$action\"}", if (next) "Yan aynalar katlandı" else "Yan aynalar açıldı") {
                                uiState = uiState.copy(security = uiState.security.copy(mirrorsFolded = next))
                            }
                        },
                        onRotateScreen = {
                            toggleScreenOrientation()
                        },
                        onTabSelected = { tab ->
                            uiState = uiState.copy(selectedTab = tab)
                        },
                        onToggle3DMode = {
                            val next = !uiState.is3DMode
                            uiState = uiState.copy(is3DMode = next)
                            showFeedback(if (next) "3D Görünüm Modu Aktif" else "2D Perspektif Modu Aktif")
                        },
                        onToggleDrl = {
                            val next = !uiState.isDrlOn
                            executeCommand("/api/vehicle/lights", "{\"target\":\"drl\",\"enable\":$next}", if (next) "Gündüz farları (DRL) açıldı" else "Gündüz farları kapatıldı") {
                                uiState = uiState.copy(isDrlOn = next)
                            }
                        },
                        onSelectAmbientColor = { preset ->
                            executeCommand("/api/vehicle/lights", "{\"target\":\"ambient\",\"color\":$preset}", "Ambiyans rengi ayarlandı") {
                                uiState = uiState.copy(ambientColorPreset = preset)
                            }
                        },
                        onToggleSlw = {
                            val next = !uiState.slwEnabled
                            executeCommand("/api/vehicle/adas", "{\"target\":\"speedLimitWarning\",\"enable\":$next}", if (next) "Hız sınırı uyarısı (SLW) aktif" else "Hız sınırı uyarısı kapatıldı") {
                                uiState = uiState.copy(slwEnabled = next)
                            }
                        },
                        onToggleCpd = {
                            val next = !uiState.cpdEnabled
                            val v = if (next) 1 else 2
                            executeCommand("/api/vehicle/setting", "{\"target\":\"childPresenceDetection\",\"value\":$v}", if (next) "Çocuk varlığı algılama (CPD) aktif" else "Çocuk varlığı algılama kapatıldı") {
                                uiState = uiState.copy(cpdEnabled = next)
                            }
                        },
                        onStartCharging = {
                            executeCommand("/api/vehicle/start-charging", null, "Şarj başlatılıyor...")
                        },
                        onToggleSmartCharge = {
                            val next = !uiState.smartChargeEnabled
                            executeCommand("/api/vehicle/charging-schedule", "{\"enabled\":$next}", if (next) "Akıllı şarj devrede" else "Akıllı şarj kapatıldı") {
                                uiState = uiState.copy(smartChargeEnabled = next)
                            }
                        },
                        onSetChargeCap = { percent ->
                            executeCommand("/api/vehicle/charge-cap", "{\"percent\":$percent}", "Şarj limiti: %$percent") {
                                uiState = uiState.copy(chargeCapPercent = percent)
                            }
                        },
                        onSetAcCurrentLimit = { amps ->
                            val stateVal = when (amps) {
                                6 -> 1
                                8 -> 2
                                10 -> 3
                                16 -> 4
                                else -> 5
                            }
                            executeCommand("/api/vehicle/ac-charge-current-limit", "{\"state\":$stateVal}", "AC Akım Limiti: $amps A") {
                                uiState = uiState.copy(acCurrentLimit = amps)
                            }
                        },
                        onPlayAvasTone = { tone ->
                            executeCommand("/api/audio/avas-tone", "{\"pattern\":$tone}", "Dış hoparlör tonu çalınıyor ($tone)") {
                                uiState = uiState.copy(activeAvasTone = tone)
                            }
                        },
                        onStopAvas = {
                            executeCommand("/api/audio/avas-tone", "{\"stop\":true}", "Dış ses durduruldu") {
                                uiState = uiState.copy(activeAvasTone = null)
                            }
                        },
                        onToggleEngineSound = {
                            val next = !uiState.isEngineSoundOn
                            executeCommand("/api/audio/engine-sound", "{\"on\":$next,\"preset\":1}", if (next) "Motor ses simülatörü açıldı" else "Motor sesi kapatıldı") {
                                uiState = uiState.copy(isEngineSoundOn = next)
                            }
                        },
                        onRebootIvi = {
                            executeCommand("/api/system/ivi-reboot", "{\"confirm\":true}", "Multimedya yeniden başlatılıyor...")
                        },
                        onSelectDriveMode = { mode ->
                            uiState = uiState.copy(powertrain = uiState.powertrain.copy(operationMode = mode))
                            val modeInt = when (mode) {
                                OperationMode.NORMAL -> 1
                                OperationMode.ECO -> 2
                                OperationMode.SPORT -> 3
                                OperationMode.SNOW -> 4
                                else -> 1
                            }
                            val modeName = when (mode) {
                                OperationMode.NORMAL -> "Normal"
                                OperationMode.ECO -> "Eco"
                                OperationMode.SPORT -> "Sport"
                                OperationMode.SNOW -> "Kar"
                                else -> "Normal"
                            }
                            executeCommand(
                                "/api/vehicle/drive-mode",
                                "{\"mode\":$modeInt}",
                                "Sürüş modu: $modeName moduna alınıyor..."
                            )
                        },
                        onSelectModelClick = {
                            showVehicleModelPickerDialog()
                        },
                        onNavigateToCharging = {
                            try {
                                findNavController().navigate(R.id.chargingFragment)
                            } catch (e: Exception) {
                                android.util.Log.w("VehicleControl", "Cannot navigate to chargingFragment", e)
                            }
                        },
                        onResetSinceCharge = {
                            resetSinceCharge()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeRepositoryFlows()
    }

    override fun onResume() {
        super.onResume()
        refreshVehicleModel()
        refreshCloudStatus()
        mainHandler.removeCallbacks(refreshRunnable)
        refreshVehicleStateAsync()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(refreshRunnable)
        isRefreshing = false
        super.onPause()
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacks(refreshRunnable)
        isRefreshing = false
        workerExecutor.shutdownNow()
        workerExecutor = Executors.newSingleThreadExecutor()
        super.onDestroyView()
    }

    private fun refreshVehicleModel() {
        val modelId = VehicleTopDownArt.getSelectedModelId(context)
        val modelDisplayName = if (!modelId.isNullOrEmpty()) {
            VehicleTopDownArt.displayNameFor(modelId, context)
        } else {
            ""
        }
        val isAwd = (modelId?.contains("awd", ignoreCase = true) == true) || (modelId?.contains("sealion7", ignoreCase = true) == true)
        uiState = uiState.copy(
            selectedModelId = modelId,
            vehicleModelName = modelDisplayName,
            isAwd = isAwd
        )

        // Asynchronously revalidate against /api/models/selected (handles remote changes and unset state)
        workerExecutor.execute {
            try {
                val conn = DaemonHttpClient.open("/api/models/selected", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val m = when {
                        json.has("selectedModelId") && !json.isNull("selectedModelId") ->
                            json.optString("selectedModelId", "")
                        json.has("modelSource") && json.optString("modelSource", "unset") == "unset" -> ""
                        else -> json.optString("modelId", "")
                    }
                    val finalId = m.ifEmpty { null }
                    val finalName = if (!finalId.isNullOrEmpty()) {
                        VehicleTopDownArt.displayNameFor(finalId, context)
                    } else {
                        ""
                    }
                    val finalAwd = (finalId?.contains("awd", ignoreCase = true) == true) || (finalId?.contains("sealion7", ignoreCase = true) == true)
                    mainHandler.post {
                        if (!isAdded || view == null) return@post
                        uiState = uiState.copy(
                            selectedModelId = finalId,
                            vehicleModelName = finalName,
                            isAwd = finalAwd
                        )
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun showVehicleModelPickerDialog() {
        val ctx = context ?: return
        val models = listOf(
            "seal" to "BYD Seal",
            "sealion7" to "BYD Sealion 7",
            "sealu" to "BYD Seal U",
            "sealudmi" to "BYD Seal U DM-i",
            "atto3" to "BYD Atto 3",
            "atto3evo" to "BYD Atto 3 Evo",
            "atto2" to "BYD Atto 2",
            "dolphin" to "BYD Dolphin",
            "han" to "BYD Han",
            "tang" to "BYD Tang",
            "seagull" to "BYD Seagull",
            "destroyer05" to "BYD Destroyer 05",
            "m6" to "BYD M6",
            "shark" to "BYD Shark",
            "" to "Seçimi Kaldır (Model Yok / Varsayılan)"
        )

        val titles = models.map { it.second }.toTypedArray()
        val currentId = uiState.selectedModelId ?: ""
        val currentIndex = models.indexOfFirst { it.first.equals(currentId, ignoreCase = true) }.let {
            if (it >= 0) it else models.lastIndex
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Araç Modeli Seçin")
            .setSingleChoiceItems(titles, currentIndex) { dialog, which ->
                val selected = models[which].first
                selectVehicleModel(selected.ifEmpty { null })
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun selectVehicleModel(modelId: String?) {
        val toastMsg = if (!modelId.isNullOrEmpty()) {
            "${VehicleTopDownArt.displayNameFor(modelId, context)} seçildi"
        } else {
            "Araç seçimi temizlendi"
        }
        showFeedback(toastMsg)

        workerExecutor.execute {
            // 1. Post to daemon /api/models/selected
            try {
                val conn = DaemonHttpClient.open("/api/models/selected", "POST", 3000, 5000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                val body = if (modelId.isNullOrEmpty()) {
                    "{\"clearModelSelection\":true}"
                } else {
                    "{\"modelId\":\"$modelId\"}"
                }
                conn.outputStream.use { it.write(body.toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}

            // 2. Persist to UnifiedConfigManager
            try {
                val vehicle = com.overdrive.app.config.UnifiedConfigManager.getVehicle()
                if (modelId.isNullOrEmpty()) {
                    vehicle.put("modelSource", com.overdrive.app.config.VehicleModelSelection.SOURCE_UNSET)
                } else {
                    vehicle.put("modelId", modelId)
                    vehicle.put("modelSource", com.overdrive.app.config.VehicleModelSelection.SOURCE_USER)
                }
                com.overdrive.app.config.UnifiedConfigManager.setVehicle(vehicle)
            } catch (_: Throwable) {}

            // 3. Persist to SharedPreferences
            try {
                val prefs = context?.getSharedPreferences("overdrive_vehicle", Context.MODE_PRIVATE)
                prefs?.edit()?.apply {
                    if (modelId.isNullOrEmpty()) {
                        remove("selected_model_id")
                    } else {
                        putString("selected_model_id", modelId)
                    }
                    apply()
                }
            } catch (_: Throwable) {}

            // 4. Refresh Compose UI state
            mainHandler.post {
                if (!isAdded || view == null) return@post
                refreshVehicleModel()
            }
        }
    }

    private fun refreshCloudStatus() {
        val cloudConfig = BydCloudConfig.fromUnifiedConfig()
        val configured = cloudConfig.isConfigured
        if (!configured) {
            uiState = uiState.copy(
                security = uiState.security.copy(
                    isCloudConfigured = false,
                    isCloudConnected = false,
                    cloudStatusText = ""
                )
            )
            return
        }

        uiState = uiState.copy(
            security = uiState.security.copy(
                isCloudConfigured = true
            )
        )

        workerExecutor.execute {
            var isOnline = false
            var statusText = "Offline"
            var conn: HttpURLConnection? = null
            try {
                conn = DaemonHttpClient.open("/api/bydcloud/status", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val status = json.optJSONObject("status")
                    isOnline = status?.optBoolean("connected", false) ?: json.optBoolean("connected", false)
                    if (isOnline) statusText = "Online"
                }
            } catch (_: Throwable) {
            } finally {
                try { conn?.disconnect() } catch (_: Throwable) {}
            }

            mainHandler.post {
                if (!isAdded || view == null) return@post
                uiState = uiState.copy(
                    security = uiState.security.copy(
                        isCloudConnected = isOnline,
                        cloudStatusText = statusText
                    )
                )
            }
        }
    }

    private fun observeRepositoryFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    RepositoryProvider.bodyworkRepository.bodyworkState.collect { bodywork ->
                        if (bodywork.isLocked != null) {
                            uiState = uiState.copy(
                                security = uiState.security.copy(isLocked = bodywork.isLocked),
                                doors = uiState.doors.copy(
                                    frontLeftOpen = bodywork.doorOpenFl,
                                    frontRightOpen = bodywork.doorOpenFr,
                                    rearLeftOpen = bodywork.doorOpenRl,
                                    rearRightOpen = bodywork.doorOpenRr,
                                    trunkOpen = bodywork.trunkOpen,
                                    hoodOpen = bodywork.hoodOpen
                                )
                            )
                        }
                    }
                }
                launch {
                    RepositoryProvider.chassisRepository.chassisState.collect { chassis ->
                        if (chassis.tyrePressureFlKpa > 0 || chassis.tyrePressureFrKpa > 0) {
                            val kpaToPsi = 0.1450377f
                            val flKpa = if (chassis.tyrePressureFlKpa > 0) chassis.tyrePressureFlKpa else null
                            val frKpa = if (chassis.tyrePressureFrKpa > 0) chassis.tyrePressureFrKpa else null
                            val rlKpa = if (chassis.tyrePressureRlKpa > 0) chassis.tyrePressureRlKpa else null
                            val rrKpa = if (chassis.tyrePressureRrKpa > 0) chassis.tyrePressureRrKpa else null
                            uiState = uiState.copy(
                                tyres = VehicleTyresState(
                                    flPsi = flKpa?.let { Math.round(it * kpaToPsi * 10f) / 10f },
                                    frPsi = frKpa?.let { Math.round(it * kpaToPsi * 10f) / 10f },
                                    rlPsi = rlKpa?.let { Math.round(it * kpaToPsi * 10f) / 10f },
                                    rrPsi = rrKpa?.let { Math.round(it * kpaToPsi * 10f) / 10f },
                                    flTemp = if (chassis.tyreTempFlC > -40) chassis.tyreTempFlC else null,
                                    frTemp = if (chassis.tyreTempFrC > -40) chassis.tyreTempFrC else null,
                                    rlTemp = if (chassis.tyreTempRlC > -40) chassis.tyreTempRlC else null,
                                    rrTemp = if (chassis.tyreTempRrC > -40) chassis.tyreTempRrC else null,
                                    flKpa = flKpa,
                                    frKpa = frKpa,
                                    rlKpa = rlKpa,
                                    rrKpa = rrKpa,
                                )
                            )
                        }
                    }
                }
                launch {
                    RepositoryProvider.hvacRepository.hvacState.collect { hvac ->
                        if (hvac.driverSetpointTemp > 0) {
                            uiState = uiState.copy(
                                climate = uiState.climate.copy(
                                    isAcOn = hvac.isAcOn,
                                    targetTemp = hvac.driverSetpointTemp.toInt().coerceIn(16, 30),
                                    fanLevel = if (hvac.fanSpeed in 1..7) hvac.fanSpeed else uiState.climate.fanLevel
                                )
                            )
                        }
                    }
                }
                launch {
                    RepositoryProvider.powertrainRepository.powertrainState.collect { pt ->
                        uiState = uiState.copy(
                            powertrain = uiState.powertrain.copy(
                                speedKmh = if (pt.speedKmh >= 0) pt.speedKmh else uiState.powertrain.speedKmh,
                                gear = if (pt.gear != com.overdrive.app.domain.model.Gear.UNKNOWN) pt.gear else uiState.powertrain.gear,
                                operationMode = pt.operationMode
                            )
                        )
                    }
                }
                launch {
                    RepositoryProvider.batteryRepository.batteryState.collect { bat ->
                        uiState = uiState.copy(
                            battery = uiState.battery.copy(
                                socPercent = bat.socPercent.toInt().coerceIn(0, 100),
                                elecRangeKm = bat.elecRangeKm,
                                batteryCapacityKwh = if (bat.remainKwh > 0) Math.round(bat.remainKwh * 10.0) / 10.0 else uiState.battery.batteryCapacityKwh,
                                batteryTempC = if (!bat.avgCellTempC.isNaN()) bat.avgCellTempC.toInt() else uiState.battery.batteryTempC,
                                sohPercent = if (bat.sohPercent > 0) bat.sohPercent else uiState.battery.sohPercent,
                                isCharging = bat.isCharging,
                                chargingPowerKw = bat.chargingPowerKw,
                                voltage12v = if (bat.voltage12v > 0) bat.voltage12v else uiState.battery.voltage12v
                            )
                        )
                    }
                }
            }
        }
    }

    private fun scheduleNextRefresh() {
        if (!isResumed) return
        mainHandler.removeCallbacks(refreshRunnable)
        // Adaptive telemetry rate:
        // Driving/Moving/Charging: 100ms (10 Hz) for instantaneous speed/power bar responsiveness
        // Parked/Stationary: 300ms (~3.3 Hz) for resource preservation
        val isActive = uiState.powertrain.speedKmh > 0.5 ||
                uiState.powertrain.gear != Gear.P ||
                uiState.battery.isCharging
        val nextDelay = if (isActive) 100L else 300L
        mainHandler.postDelayed(refreshRunnable, nextDelay)
    }

    private fun pollFastInMemoryTelemetry() {
        try {
            val collector = com.overdrive.app.byd.BydDataCollector.getInstance()
            if (collector != null && collector.isInitialized) {
                val d = collector.data
                if (d != null) {
                    val spd = collector.readCurrentSpeedKmh()
                    val rawSpeed = if (!spd.isNaN() && spd >= 0) spd else (if (!d.speedKmh.isNaN()) d.speedKmh else 0.0)

                    val g = com.overdrive.app.recording.RecordingModeManager.gearToString(d.gearMode)
                    val gearVal = when (g?.uppercase(Locale.ROOT)) {
                        "P" -> Gear.P
                        "R" -> Gear.R
                        "N" -> Gear.N
                        "D" -> Gear.D
                        "M" -> Gear.M
                        "S" -> Gear.S
                        else -> uiState.powertrain.gear
                    }

                    // Strict speed deadband:
                    // Under 1.8 km/h or parked gear (P) -> strictly 0.0 km/h
                    val speedFiltered = if (rawSpeed < 1.8 || gearVal == Gear.P) 0.0 else rawSpeed
                    val speedVal = if (abs(speedFiltered - uiState.powertrain.speedKmh) < 0.25 && speedFiltered > 0.0) {
                        uiState.powertrain.speedKmh
                    } else {
                        speedFiltered
                    }

                    val opModeVal = when (d.operationMode) {
                        1 -> OperationMode.NORMAL
                        2 -> OperationMode.ECO
                        3 -> OperationMode.SPORT
                        4 -> OperationMode.SNOW
                        0 -> OperationMode.NORMAL
                        else -> uiState.powertrain.operationMode
                    }

                    val liveAccel = try { collector.readAccelNow() } catch (_: Throwable) { -1 }
                    val liveBrake = try { collector.readBrakeNow() } catch (_: Throwable) { -1 }

                    val accelVal = when {
                        liveAccel in 0..100 -> liveAccel
                        d.accelPercent in 0..100 -> d.accelPercent
                        else -> 0
                    }

                    val brakeVal = when {
                        liveBrake in 0..100 -> liveBrake
                        d.brakePercent in 0..100 -> d.brakePercent
                        else -> 0
                    }
                    val isCharging = (d.chargingGunState == 1 || d.chargingState == 1) || (!d.chargingPowerKw.isNaN() && d.chargingPowerKw > 0.1)
                    val chgKw = if (!d.chargingPowerKw.isNaN()) d.chargingPowerKw else 0.0

                    // 3-Katmanlı Güç Tahmin Motoru (Sıfır Hız Rejen Filtresi & Fizik Modeli Dahil)
                    val targetPowerKw = VehiclePowerEstimator.calculateLivePowerKw(
                        context = context,
                        speedKmh = speedVal,
                        gear = gearVal,
                        accelPercent = accelVal,
                        brakePercent = brakeVal,
                        isAcOn = uiState.climate.isAcOn,
                        fanLevel = uiState.climate.fanLevel,
                        isCharging = isCharging,
                        chargingPowerKw = chgKw,
                        deltaTimeSec = 0.1
                    )
                    val smoothedPower = VehiclePowerEstimator.smoothPowerForDisplay(targetPowerKw, uiState.powertrain.powerKw)

                    // Battery & Range Telemetry
                    val newSoc = if (!d.socPercent.isNaN() && d.socPercent >= 0) d.socPercent.toInt().coerceIn(0, 100) else uiState.battery.socPercent
                    val newElecRange = if (d.elecRangeKm != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE && d.elecRangeKm >= 0) d.elecRangeKm else uiState.battery.elecRangeKm
                    val newRemainKwh = if (!d.remainKwh.isNaN() && d.remainKwh > 0) Math.round(d.remainKwh * 10.0) / 10.0 else uiState.battery.batteryCapacityKwh
                    val newSoh = if (!d.sohPercent.isNaN() && d.sohPercent > 0) d.sohPercent else uiState.battery.sohPercent
                    val newCellTemp = if (!d.avgCellTempC.isNaN()) d.avgCellTempC.toInt() else uiState.battery.batteryTempC
                    val new12v = if (!d.voltage12v.isNaN() && d.voltage12v > 0) d.voltage12v else uiState.battery.voltage12v

                    // Son 50 km tüketimi ve Genel tüketim
                    val avg50Km = if (!d.last50KmConsumption.isNaN() && d.last50KmConsumption > 0.0 && d.last50KmConsumption < 100.0) {
                        Math.round(d.last50KmConsumption * 10.0) / 10.0
                    } else uiState.battery.avg50KmKwh

                    val avgLifetime = if (!d.avgElecConPer100Km.isNaN() && d.avgElecConPer100Km > 0.0 && d.avgElecConPer100Km < 100.0) {
                        Math.round(d.avgElecConPer100Km * 10.0) / 10.0
                    } else uiState.battery.avgLifetimeKwh

                    val totalOdo = if (d.totalMileageKm != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE && d.totalMileageKm > 0) {
                        d.totalMileageKm.toDouble()
                    } else {
                        try {
                            val odo = com.overdrive.app.trips.OdometerReader.getInstance().readOdometerKm()
                            if (odo > 0) odo else uiState.battery.totalMileageKm
                        } catch (_: Throwable) {
                            uiState.battery.totalMileageKm
                        }
                    }

                    // SinceChargeManager & TripAnalyticsManager
                    val tam = com.overdrive.app.daemon.CameraDaemon.getTripAnalyticsManager()
                    val scm = com.overdrive.app.telemetry.SinceChargeManager.getInstance()
                    scm.update(d, tam)

                    val sinceLastChargeKm = scm.getSinceLastChargeKm()
                    val sinceLastChargeAvg = scm.getSinceLastChargeAvgKwh()
                    val realisticRangeRaw = scm.getRealisticRangeKm()
                    val realisticRange = if (realisticRangeRaw > 0) realisticRangeRaw else (if (uiState.battery.realisticRangeKm > 0) uiState.battery.realisticRangeKm else newElecRange)

                    val activeTrip = tam?.activeTrip
                    val activeTripKm = activeTrip?.let { Math.round(it.distanceKm * 10.0) / 10.0 } ?: 0.0
                    val activeTripMinutes = activeTrip?.let { Math.max(0, it.durationSeconds / 60) } ?: 0

                    val tripId = activeTrip?.startTime ?: 1L
                    if (speedVal > 0.5 || gearVal != Gear.P) {
                        scm.trackActiveTripRegen(smoothedPower, 0.1, tripId)
                    }
                    val measuredRegen = activeTrip?.let { at ->
                        if (at.energyPerKm < 0 && at.distanceKm > 0) {
                            Math.round(Math.abs(at.energyPerKm * at.distanceKm) * 100.0) / 100.0
                        } else if (at.elecConStart >= 0 && at.elecConEnd >= at.elecConStart && at.kwhStart > 0 && at.kwhEnd > 0) {
                            val grossKwh = at.elecConEnd - at.elecConStart
                            val netKwh = at.kwhStart - at.kwhEnd
                            if (grossKwh > netKwh) Math.round((grossKwh - netKwh) * 100.0) / 100.0 else 0.0
                        } else 0.0
                    } ?: 0.0
                    val regenKwh = maxOf(scm.getActiveTripRegenKwh(), measuredRegen)

                    // Tyres (from d.tyrePressure and d.tyreTemperature)
                    val tp = d.tyrePressure
                    val tt = d.tyreTemperature ?: collector.getTyreTemperatures()
                    val flKpa = if (tp != null && tp.size > 0 && tp[0] > 0 && tp[0] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) tp[0] else null
                    val frKpa = if (tp != null && tp.size > 1 && tp[1] > 0 && tp[1] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) tp[1] else null
                    val rlKpa = if (tp != null && tp.size > 2 && tp[2] > 0 && tp[2] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) tp[2] else null
                    val rrKpa = if (tp != null && tp.size > 3 && tp[3] > 0 && tp[3] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) tp[3] else null

                    val flTemp = if (tt != null && tt.size > 0 && tt[0] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE && tt[0] > -50 && tt[0] < 150) tt[0] else null
                    val frTemp = if (tt != null && tt.size > 1 && tt[1] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE && tt[1] > -50 && tt[1] < 150) tt[1] else null
                    val rlTemp = if (tt != null && tt.size > 2 && tt[2] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE && tt[2] > -50 && tt[2] < 150) tt[2] else null
                    val rrTemp = if (tt != null && tt.size > 3 && tt[3] != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE && tt[3] > -50 && tt[3] < 150) tt[3] else null

                    val updatedTyres = VehicleTyresState(
                        flPsi = flKpa?.let { Math.round(it * 0.1450377f * 10f) / 10f },
                        frPsi = frKpa?.let { Math.round(it * 0.1450377f * 10f) / 10f },
                        rlPsi = rlKpa?.let { Math.round(it * 0.1450377f * 10f) / 10f },
                        rrPsi = rrKpa?.let { Math.round(it * 0.1450377f * 10f) / 10f },
                        flTemp = flTemp,
                        frTemp = frTemp,
                        rlTemp = rlTemp,
                        rrTemp = rrTemp,
                        flKpa = flKpa,
                        frKpa = frKpa,
                        rlKpa = rlKpa,
                        rrKpa = rrKpa,
                    )

                    // Door & Lid Open States
                    val ds = collector.readAllDoorOpenStates()
                    val updatedDoors = uiState.doors.copy(
                        frontLeftOpen = if (ds.size > 0 && ds[0] >= 0) ds[0] == 1 else uiState.doors.frontLeftOpen,
                        frontRightOpen = if (ds.size > 1 && ds[1] >= 0) ds[1] == 1 else uiState.doors.frontRightOpen,
                        rearLeftOpen = if (ds.size > 2 && ds[2] >= 0) ds[2] == 1 else uiState.doors.rearLeftOpen,
                        rearRightOpen = if (ds.size > 3 && ds[3] >= 0) ds[3] == 1 else uiState.doors.rearRightOpen,
                        hoodOpen = if (ds.size > 4 && ds[4] >= 0) ds[4] == 1 else uiState.doors.hoodOpen,
                        trunkOpen = if (ds.size > 5 && ds[5] >= 0) ds[5] == 1 else uiState.doors.trunkOpen,
                    )

                    // Door Locks (Overall lock status)
                    val lockVal = if (d.doorLockStatus != null && d.doorLockStatus.size >= 7 && d.doorLockStatus[6] in 1..2) {
                        d.doorLockStatus[6] == 1 // 1=locked, 2=unlocked
                    } else if (d.doorLockStatus != null && d.doorLockStatus.size >= 4) {
                        val anyUnlocked = d.doorLockStatus.take(4).any { it == 2 }
                        val allLocked = d.doorLockStatus.take(4).all { it == 1 }
                        if (anyUnlocked) false else if (allLocked) true else uiState.security.isLocked
                    } else {
                        uiState.security.isLocked
                    }

                    // Windows
                    val wp = d.windowOpenPercent
                    val updatedWindows = if (wp != null && wp.size >= 4) {
                        uiState.windows.copy(
                            frontLeftOpen = wp[0] > 0,
                            frontRightOpen = wp[1] > 0,
                            rearLeftOpen = wp[2] > 0,
                            rearRightOpen = wp[3] > 0,
                            sunroofOpen = if (wp.size >= 5) wp[4] > 0 else uiState.windows.sunroofOpen,
                            sunshadeOpen = if (wp.size >= 6) wp[5] > 0 else uiState.windows.sunshadeOpen,
                        )
                    } else uiState.windows

                    uiState = uiState.copy(
                        powertrain = uiState.powertrain.copy(
                            speedKmh = speedVal,
                            gear = gearVal,
                            powerKw = smoothedPower,
                            operationMode = opModeVal,
                            accelPedalPercent = accelVal,
                            brakePedalPercent = brakeVal
                        ),
                        battery = uiState.battery.copy(
                            socPercent = newSoc,
                            elecRangeKm = newElecRange,
                            realisticRangeKm = realisticRange,
                            batteryCapacityKwh = newRemainKwh,
                            batteryTempC = newCellTemp,
                            sohPercent = newSoh,
                            isCharging = isCharging,
                            chargingPowerKw = chgKw,
                            voltage12v = new12v,
                            avg50KmKwh = avg50Km,
                            avgLifetimeKwh = avgLifetime,
                            sinceLastChargeKm = sinceLastChargeKm,
                            sinceLastChargeAvgKwh = sinceLastChargeAvg,
                            activeTripKm = activeTripKm,
                            activeTripMinutes = activeTripMinutes,
                            regenKwh = regenKwh,
                            totalMileageKm = totalOdo,
                        ),
                        tyres = updatedTyres,
                        doors = updatedDoors,
                        security = uiState.security.copy(
                            isLocked = lockVal
                        ),
                        windows = updatedWindows,
                    )
                }
            }
        } catch (_: Throwable) {}
    }

    private fun refreshVehicleStateAsync() {
        if (isRefreshing) return
        isRefreshing = true

        val collector = com.overdrive.app.byd.BydDataCollector.getInstance()
        if (collector != null && collector.isInitialized) {
            // Running directly on vehicle: 100% in-process zero-latency telemetry (no loopback HTTP overhead or race conditions)
            pollFastInMemoryTelemetry()
            isRefreshing = false
            scheduleNextRefresh()
            return
        }

        // Off-vehicle / Remote Dev Fallback: query /api/vehicle/state over HTTP
        workerExecutor.execute {
            var conn: HttpURLConnection? = null
            var parsedJson: JSONObject? = null
            try {
                conn = DaemonHttpClient.open("/api/vehicle/state", "GET", 1500, 2000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    parsedJson = JSONObject(body)
                }
            } catch (_: Throwable) {
            } finally {
                try { conn?.disconnect() } catch (_: Throwable) {}
            }

            mainHandler.post {
                isRefreshing = false
                if (!isAdded || view == null) return@post

                if (parsedJson != null) {
                    applyVehicleStateJson(parsedJson)
                }
                scheduleNextRefresh()
            }
        }
    }

    private fun applyVehicleStateJson(json: JSONObject) {
        // Doors & Lock
        val doors = json.optJSONObject("doors")
        val doorOpen = json.optJSONObject("doorOpen")
        val overall = doors?.optInt("overall", -1) ?: -1
        val isLocked: Boolean? = when (overall) {
            1 -> true
            2 -> false
            else -> uiState.security.isLocked
        }

        val updatedDoors = uiState.doors.copy(
            frontLeftOpen = doorOpen?.optBoolean("lf") ?: uiState.doors.frontLeftOpen,
            frontRightOpen = doorOpen?.optBoolean("rf") ?: uiState.doors.frontRightOpen,
            rearLeftOpen = doorOpen?.optBoolean("lr") ?: uiState.doors.rearLeftOpen,
            rearRightOpen = doorOpen?.optBoolean("rr") ?: uiState.doors.rearRightOpen,
            trunkOpen = doorOpen?.optBoolean("trunk") ?: uiState.doors.trunkOpen,
            hoodOpen = doorOpen?.optBoolean("hood") ?: uiState.doors.hoodOpen
        )

        // Tyres
        val tyres = json.optJSONObject("tyres")
        val anyTyres = tyres?.optBoolean("available", false) ?: false
        val newTyresState = if (anyTyres) {
            val fl = tyres.optJSONObject("fl")
            val fr = tyres.optJSONObject("fr")
            val rl = tyres.optJSONObject("rl")
            val rr = tyres.optJSONObject("rr")
            VehicleTyresState(
                flPsi = if (fl != null && fl.has("psi")) fl.optDouble("psi").toFloat() else null,
                frPsi = if (fr != null && fr.has("psi")) fr.optDouble("psi").toFloat() else null,
                rlPsi = if (rl != null && rl.has("psi")) rl.optDouble("psi").toFloat() else null,
                rrPsi = if (rr != null && rr.has("psi")) rr.optDouble("psi").toFloat() else null,
                flTemp = if (fl != null && fl.has("temperatureC")) fl.optInt("temperatureC") else null,
                frTemp = if (fr != null && fr.has("temperatureC")) fr.optInt("temperatureC") else null,
                rlTemp = if (rl != null && rl.has("temperatureC")) rl.optInt("temperatureC") else null,
                rrTemp = if (rr != null && rr.has("temperatureC")) rr.optInt("temperatureC") else null,
                flKpa = if (fl != null && fl.has("kPa")) fl.optInt("kPa") else null,
                frKpa = if (fr != null && fr.has("kPa")) fr.optInt("kPa") else null,
                rlKpa = if (rl != null && rl.has("kPa")) rl.optInt("kPa") else null,
                rrKpa = if (rr != null && rr.has("kPa")) rr.optInt("kPa") else null,
            )
        } else {
            uiState.tyres
        }

        // Climate
        val climate = json.optJSONObject("climate")
        val acOn = climate?.optBoolean("acOn", uiState.climate.isAcOn) ?: uiState.climate.isAcOn
        val setpoint = climate?.optInt("setpointDriver", uiState.climate.targetTemp)?.coerceIn(16, 30) ?: uiState.climate.targetTemp
        val fan = climate?.optInt("fanLevel", uiState.climate.fanLevel)?.coerceIn(1, 7) ?: uiState.climate.fanLevel
        val batteryHeat = json.optBoolean("batteryHeat", uiState.climate.isBatteryHeatOn)

        // Seats
        val seats = json.optJSONObject("seats")
        val dHeat = seats?.optInt("driverHeat", uiState.comfort.driverSeatHeat)?.coerceIn(0, 3) ?: uiState.comfort.driverSeatHeat
        val pHeat = seats?.optInt("passengerHeat", uiState.comfort.passengerSeatHeat)?.coerceIn(0, 3) ?: uiState.comfort.passengerSeatHeat
        val dVent = seats?.optInt("driverVent", uiState.comfort.driverSeatVent)?.coerceIn(0, 3) ?: uiState.comfort.driverSeatVent
        val pVent = seats?.optInt("passengerVent", uiState.comfort.passengerSeatVent)?.coerceIn(0, 3) ?: uiState.comfort.passengerSeatVent
        val steeringHeat = seats?.optBoolean("steeringHeat", uiState.comfort.steeringHeatOn) ?: uiState.comfort.steeringHeatOn

        // Battery Telemetry
        val bat = json.optJSONObject("battery")
        val newSoc = bat?.optDouble("soc", uiState.battery.socPercent.toDouble())?.toInt()?.coerceIn(0, 100) ?: uiState.battery.socPercent
        val newRangeKm = bat?.optInt("rangeKm", uiState.battery.elecRangeKm)?.takeIf { it >= 0 } ?: uiState.battery.elecRangeKm
        val newRemainKwh = bat?.optDouble("remainKwh", uiState.battery.batteryCapacityKwh)?.takeIf { it > 0 } ?: uiState.battery.batteryCapacityKwh
        val newSoh = bat?.optDouble("sohPercent", uiState.battery.sohPercent)?.takeIf { it > 0 } ?: uiState.battery.sohPercent
        val newCellTemp = bat?.optInt("cellTempC", uiState.battery.batteryTempC) ?: uiState.battery.batteryTempC
        val new12v = bat?.optDouble("voltage12v", uiState.battery.voltage12v)?.takeIf { it > 0 } ?: uiState.battery.voltage12v
        val newIsCharging = bat?.optBoolean("isCharging", uiState.battery.isCharging) ?: uiState.battery.isCharging
        val newChargingKw = bat?.optDouble("chargingPowerKw", uiState.battery.chargingPowerKw)?.takeIf { it >= 0 } ?: uiState.battery.chargingPowerKw
        val newRealisticRange = if (bat != null && bat.has("realisticRangeKm")) {
            val r = bat.optInt("realisticRangeKm", 0)
            if (r > 0) r else (if (uiState.battery.realisticRangeKm > 0) uiState.battery.realisticRangeKm else newRangeKm)
        } else (if (uiState.battery.realisticRangeKm > 0) uiState.battery.realisticRangeKm else newRangeKm)
        val newAvg50Km = if (bat != null && bat.has("avg50KmKwh")) {
            val v = bat.optDouble("avg50KmKwh", 0.0)
            if (v > 0.0) v else uiState.battery.avg50KmKwh
        } else uiState.battery.avg50KmKwh

        val newAvgLifetime = if (bat != null && bat.has("avgLifetimeKwh")) {
            val v = bat.optDouble("avgLifetimeKwh", 0.0)
            if (v > 0.0) v else uiState.battery.avgLifetimeKwh
        } else uiState.battery.avgLifetimeKwh

        val newSinceLastChargeKm = if (bat != null && bat.has("sinceLastChargeKm")) {
            val v = bat.optDouble("sinceLastChargeKm", 0.0)
            if (v > 0.0) v else uiState.battery.sinceLastChargeKm
        } else uiState.battery.sinceLastChargeKm

        val newSinceLastChargeAvg = if (bat != null && bat.has("sinceLastChargeAvgKwh")) {
            val v = bat.optDouble("sinceLastChargeAvgKwh", 0.0)
            if (v > 0.0) v else uiState.battery.sinceLastChargeAvgKwh
        } else uiState.battery.sinceLastChargeAvgKwh

        val newActiveTripKm = if (bat != null && bat.has("activeTripKm")) {
            val v = bat.optDouble("activeTripKm", 0.0)
            if (v > 0.0) v else uiState.battery.activeTripKm
        } else uiState.battery.activeTripKm

        val newActiveTripMinutes = if (bat != null && bat.has("activeTripMinutes")) {
            val v = bat.optInt("activeTripMinutes", 0)
            if (v > 0) v else uiState.battery.activeTripMinutes
        } else uiState.battery.activeTripMinutes

        val newRegenKwh = if (bat != null && bat.has("regenKwh")) {
            val v = bat.optDouble("regenKwh", 0.0)
            if (v > 0.0) v else uiState.battery.regenKwh
        } else uiState.battery.regenKwh

        val newTotalMileage = if (bat != null && bat.has("totalMileageKm")) {
            bat.optDouble("totalMileageKm", 0.0)
        } else uiState.battery.totalMileageKm

        // Powertrain Telemetry
        val pt = json.optJSONObject("powertrain")
        val rawSpeedKmh = pt?.optDouble("speedKmh", uiState.powertrain.speedKmh)?.takeIf { it >= 0 } ?: uiState.powertrain.speedKmh
        val newSpeedKmh = if (abs(rawSpeedKmh - uiState.powertrain.speedKmh) < 0.25 && rawSpeedKmh > 0.0) {
            uiState.powertrain.speedKmh
        } else {
            rawSpeedKmh
        }

        val gearStr = pt?.optString("gear")
        val newGear = when (gearStr?.uppercase(Locale.ROOT)) {
            "P" -> Gear.P
            "R" -> Gear.R
            "N" -> Gear.N
            "D" -> Gear.D
            "M" -> Gear.M
            "S" -> Gear.S
            else -> uiState.powertrain.gear
        }
        val opModeVal = pt?.optInt("operationMode", 0) ?: 0
        val newOpMode = when (opModeVal) {
            1 -> OperationMode.NORMAL
            2 -> OperationMode.ECO
            3 -> OperationMode.SPORT
            4 -> OperationMode.SNOW
            0 -> OperationMode.NORMAL
            else -> uiState.powertrain.operationMode
        }

        val accel = pt?.optInt("accelPercent", uiState.powertrain.accelPedalPercent) ?: uiState.powertrain.accelPedalPercent
        val brake = pt?.optInt("brakePercent", uiState.powertrain.brakePedalPercent) ?: uiState.powertrain.brakePedalPercent

        // Live Power Estimation / Extraction (Doğrulanmış 3 Katmanlı Motor & Sıfır Hız Filtresi)
        val targetPowerKw = VehiclePowerEstimator.calculateLivePowerKw(
            context = context,
            speedKmh = newSpeedKmh,
            gear = newGear,
            accelPercent = accel,
            brakePercent = brake,
            isAcOn = acOn,
            fanLevel = fan,
            isCharging = newIsCharging,
            chargingPowerKw = newChargingKw,
            deltaTimeSec = 0.1
        )
        val smoothedPower = VehiclePowerEstimator.smoothPowerForDisplay(targetPowerKw, uiState.powertrain.powerKw)

        val tpmsNormal = listOfNotNull(newTyresState.flPsi, newTyresState.frPsi, newTyresState.rlPsi, newTyresState.rrPsi)
            .let { list -> list.isEmpty() || list.all { it in 28f..48f } }
        val batteryNormal = newCellTemp in -20..55 && new12v >= 11.0
        val updatedHealth = uiState.health.copy(
            tpms = uiState.health.tpms.copy(isNormal = tpmsNormal),
            tractionBattery = uiState.health.tractionBattery.copy(isNormal = batteryNormal)
        )

        uiState = uiState.copy(
            security = uiState.security.copy(isLocked = isLocked),
            doors = updatedDoors,
            tyres = newTyresState,
            health = updatedHealth,
            climate = uiState.climate.copy(
                isAcOn = acOn,
                targetTemp = setpoint,
                fanLevel = fan,
                isBatteryHeatOn = batteryHeat
            ),
            comfort = uiState.comfort.copy(
                driverSeatHeat = dHeat,
                passengerSeatHeat = pHeat,
                driverSeatVent = dVent,
                passengerSeatVent = pVent,
                steeringHeatOn = steeringHeat
            ),
            powertrain = uiState.powertrain.copy(
                speedKmh = newSpeedKmh,
                gear = newGear,
                powerKw = smoothedPower,
                operationMode = newOpMode,
                accelPedalPercent = accel,
                brakePedalPercent = brake
            ),

            battery = uiState.battery.copy(
                socPercent = newSoc,
                elecRangeKm = newRangeKm,
                realisticRangeKm = newRealisticRange,
                batteryCapacityKwh = newRemainKwh,
                batteryTempC = newCellTemp,
                sohPercent = newSoh,
                isCharging = newIsCharging,
                chargingPowerKw = newChargingKw,
                voltage12v = new12v,
                avg50KmKwh = newAvg50Km,
                avgLifetimeKwh = newAvgLifetime,
                sinceLastChargeKm = newSinceLastChargeKm,
                sinceLastChargeAvgKwh = newSinceLastChargeAvg,
                activeTripKm = newActiveTripKm,
                activeTripMinutes = newActiveTripMinutes,
                regenKwh = newRegenKwh,
                totalMileageKm = newTotalMileage,
            )
        )
    }

    private fun estimateLivePowerKw(
        speedKmh: Double,
        gear: Gear,
        accelPercent: Int,
        brakePercent: Int,
        isAcOn: Boolean
    ): Double {
        return VehiclePowerEstimator.calculateLivePowerKw(
            context = context,
            speedKmh = speedKmh,
            gear = gear,
            accelPercent = accelPercent,
            brakePercent = brakePercent,
            isAcOn = isAcOn,
            fanLevel = uiState.climate.fanLevel,
            isCharging = uiState.battery.isCharging,
            chargingPowerKw = uiState.battery.chargingPowerKw,
            deltaTimeSec = 0.1
        )
    }

    private fun resetSinceCharge() {
        executeCommand("/api/vehicle/telemetry/reset-since-charge", null, "Son şarj ve gerçekçi menzil referansı sıfırlandı") {
            try {
                com.overdrive.app.telemetry.SinceChargeManager.getInstance().reset()
            } catch (_: Throwable) {}
            mainHandler.post {
                pollFastInMemoryTelemetry()
            }
        }
    }

    private fun executeCommand(
        path: String,
        body: String? = null,
        feedbackMessage: String,
        optimisticUpdate: (() -> Unit)? = null
    ) {
        optimisticUpdate?.invoke()
        showFeedback(feedbackMessage)

        workerExecutor.execute {
            var conn: HttpURLConnection? = null
            try {
                conn = DaemonHttpClient.open(path, "POST", 3000, 5000)
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                conn.responseCode
            } catch (_: Throwable) {
            } finally {
                try { conn?.disconnect() } catch (_: Throwable) {}
            }
            mainHandler.postDelayed({
                refreshVehicleStateAsync()
            }, 300L)
        }
    }

    private fun toggleScreenOrientation() {
        val activity = activity ?: return
        val currentOrientation = activity.requestedOrientation
        val target = if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        activity.requestedOrientation = target
        showFeedback("Ekran yönü değiştirildi")
    }

    private fun showFeedback(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
