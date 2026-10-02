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
                            uiState = uiState.copy(
                                tyres = VehicleTyresState(
                                    flPsi = if (chassis.tyrePressureFlKpa > 0) Math.round(chassis.tyrePressureFlKpa * kpaToPsi * 10f) / 10f else null,
                                    frPsi = if (chassis.tyrePressureFrKpa > 0) Math.round(chassis.tyrePressureFrKpa * kpaToPsi * 10f) / 10f else null,
                                    rlPsi = if (chassis.tyrePressureRlKpa > 0) Math.round(chassis.tyrePressureRlKpa * kpaToPsi * 10f) / 10f else null,
                                    rrPsi = if (chassis.tyrePressureRrKpa > 0) Math.round(chassis.tyrePressureRrKpa * kpaToPsi * 10f) / 10f else null,
                                    flTemp = if (chassis.tyreTempFlC > 0) chassis.tyreTempFlC else null,
                                    frTemp = if (chassis.tyreTempFrC > 0) chassis.tyreTempFrC else null,
                                    rlTemp = if (chassis.tyreTempRlC > 0) chassis.tyreTempRlC else null,
                                    rrTemp = if (chassis.tyreTempRrC > 0) chassis.tyreTempRrC else null,
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
                            powertrain = VehiclePowertrainUiState(
                                speedKmh = pt.speedKmh,
                                powerKw = if (kotlin.math.abs(pt.enginePowerKw) > 0.01) pt.enginePowerKw else uiState.powertrain.powerKw,
                                gear = if (pt.gear != com.overdrive.app.domain.model.Gear.UNKNOWN) pt.gear else uiState.powertrain.gear,
                                operationMode = pt.operationMode
                            )
                        )
                    }
                }
                launch {
                    RepositoryProvider.batteryRepository.batteryState.collect { bat ->
                        val power = if (bat.isCharging && bat.chargingPowerKw > 0) {
                            -bat.chargingPowerKw
                        } else if (kotlin.math.abs(bat.hvBatteryPowerKw) > 0.01) {
                            bat.hvBatteryPowerKw
                        } else {
                            uiState.powertrain.powerKw
                        }
                        uiState = uiState.copy(
                            battery = VehicleBatteryUiState(
                                socPercent = bat.socPercent.toInt().coerceIn(0, 100),
                                elecRangeKm = bat.elecRangeKm,
                                batteryCapacityKwh = if (bat.remainKwh > 0) Math.round(bat.remainKwh * 10.0) / 10.0 else 71.8,
                                batteryTempC = if (!bat.avgCellTempC.isNaN()) bat.avgCellTempC.toInt() else 25,
                                sohPercent = if (bat.sohPercent > 0) bat.sohPercent else 100.0,
                                isCharging = bat.isCharging,
                                chargingPowerKw = bat.chargingPowerKw,
                                voltage12v = if (bat.voltage12v > 0) bat.voltage12v else 12.8
                            ),
                            powertrain = if (kotlin.math.abs(power) > 0.01) uiState.powertrain.copy(powerKw = power) else uiState.powertrain
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
        // Driving/Moving/Charging: 200ms (5 Hz) for instantaneous speed/power bar responsiveness
        // Parked/Stationary: 1000ms (1 Hz) for resource preservation
        val isActive = uiState.powertrain.speedKmh > 0.5 ||
                uiState.powertrain.gear != Gear.P ||
                uiState.battery.isCharging
        val nextDelay = if (isActive) 200L else 1000L
        mainHandler.postDelayed(refreshRunnable, nextDelay)
    }

    private fun refreshVehicleStateAsync() {
        if (isRefreshing) return
        isRefreshing = true

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

            // Also poll in-memory repositories if collector is initialized in this process
            try {
                com.overdrive.app.domain.engine.VehicleDataDispatcher.pollCurrent()
            } catch (_: Throwable) {}

            var directJson: JSONObject? = parsedJson
            if (directJson == null) {
                try {
                    val collector = com.overdrive.app.byd.BydDataCollector.getInstance()
                    if (collector != null && collector.isInitialized) {
                        val d = collector.data
                        if (d != null) {
                            val synth = JSONObject()
                            val bat = JSONObject()
                            if (!d.socPercent.isNaN()) bat.put("soc", d.socPercent)
                            if (d.elecRangeKm != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) bat.put("rangeKm", d.elecRangeKm)
                            if (!d.remainKwh.isNaN()) bat.put("remainKwh", d.remainKwh)
                            val isChg = d.chargingGunState == 1 || d.chargingState == 1 || (!d.chargingPowerKw.isNaN() && d.chargingPowerKw > 0.1)
                            bat.put("isCharging", isChg)
                            if (!d.chargingPowerKw.isNaN()) bat.put("chargingPowerKw", d.chargingPowerKw)
                            if (!d.voltage12v.isNaN()) bat.put("voltage12v", d.voltage12v)
                            synth.put("battery", bat)

                            val pt = JSONObject()
                            val spd = collector.readCurrentSpeedKmh()
                            pt.put("speedKmh", if (!spd.isNaN() && spd >= 0) spd else (if (!d.speedKmh.isNaN()) d.speedKmh else 0.0))
                            pt.put("gear", com.overdrive.app.recording.RecordingModeManager.gearToString(d.gearMode))
                            pt.put("operationMode", if (d.operationMode != com.overdrive.app.byd.BydVehicleData.UNAVAILABLE) d.operationMode else 2)
                            synth.put("powertrain", pt)
                            directJson = synth
                        }
                    }
                } catch (_: Throwable) {}
            }

            val json = directJson

            mainHandler.post {
                isRefreshing = false
                if (!isAdded || view == null) return@post

                if (json != null) {
                    applyVehicleStateJson(json)
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
        val newRealisticRange = bat?.optInt("realisticRangeKm", uiState.battery.realisticRangeKm)?.takeIf { it > 0 } ?: newRangeKm
        val newAvg50Km = if (bat != null && bat.has("avg50KmKwh")) bat.optDouble("avg50KmKwh", 0.0) else uiState.battery.avg50KmKwh
        val newAvgLifetime = if (bat != null && bat.has("avgLifetimeKwh")) bat.optDouble("avgLifetimeKwh", 0.0) else uiState.battery.avgLifetimeKwh
        val newSinceLastChargeKm = if (bat != null && bat.has("sinceLastChargeKm")) bat.optDouble("sinceLastChargeKm", 0.0) else uiState.battery.sinceLastChargeKm
        val newSinceLastChargeAvg = if (bat != null && bat.has("sinceLastChargeAvgKwh")) bat.optDouble("sinceLastChargeAvgKwh", 0.0) else uiState.battery.sinceLastChargeAvgKwh
        val newActiveTripKm = if (bat != null && bat.has("activeTripKm")) bat.optDouble("activeTripKm", 0.0) else uiState.battery.activeTripKm
        val newActiveTripMinutes = if (bat != null && bat.has("activeTripMinutes")) bat.optInt("activeTripMinutes", 0) else uiState.battery.activeTripMinutes
        val newRegenKwh = if (bat != null && bat.has("regenKwh")) bat.optDouble("regenKwh", 0.0) else uiState.battery.regenKwh

        // Powertrain Telemetry
        val pt = json.optJSONObject("powertrain")
        val newSpeedKmh = pt?.optDouble("speedKmh", uiState.powertrain.speedKmh)?.takeIf { it >= 0 } ?: uiState.powertrain.speedKmh
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
            1 -> OperationMode.ECO
            2 -> OperationMode.NORMAL
            3 -> OperationMode.SPORT
            4 -> OperationMode.SNOW
            else -> uiState.powertrain.operationMode
        }

        // Live Power Estimation / Extraction
        var rawPowerKw = pt?.optDouble("powerKw", uiState.powertrain.powerKw) ?: uiState.powertrain.powerKw
        val accel = pt?.optInt("accelPercent", uiState.powertrain.accelPedalPercent) ?: uiState.powertrain.accelPedalPercent
        val brake = pt?.optInt("brakePercent", uiState.powertrain.brakePedalPercent) ?: uiState.powertrain.brakePedalPercent

        if (newIsCharging && newChargingKw > 0) {
            rawPowerKw = -newChargingKw
        } else if (abs(rawPowerKw) <= 0.05 && (newSpeedKmh > 1.5 || newGear != Gear.P)) {
            rawPowerKw = estimateLivePowerKw(newSpeedKmh, newGear, accel, brake, acOn)
        }

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
                powerKw = rawPowerKw,
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
                regenKwh = newRegenKwh
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
        val auxKw = if (isAcOn) 1.8 else 0.35
        if (gear == Gear.P || gear == Gear.N || speedKmh <= 1.5) {
            return auxKw
        }
        if (gear == Gear.D || gear == Gear.R || gear == Gear.S || gear == Gear.M) {
            if (accelPercent > 0) {
                val ratio = accelPercent / 100.0
                val cruisingKw = 2.8 + (speedKmh * 0.16)
                val accelDemandKw = (ratio * 20.0) + (Math.pow(ratio, 1.85) * 160.0)
                val drivingKw = if (ratio <= 0.25) {
                    (cruisingKw * (0.65 + 1.2 * ratio)) + (ratio * 16.0)
                } else {
                    (cruisingKw * 0.9) + accelDemandKw
                }
                return Math.max(1.0, Math.round((drivingKw + auxKw) * 10.0) / 10.0)
            } else if (brakePercent > 0) {
                if (speedKmh <= 3.0) return auxKw
                val taper = ((speedKmh - 3.0) / 7.0).coerceIn(0.0, 1.0)
                val speedFactor = Math.min(1.0, speedKmh / 65.0) * taper
                val baseRegen = (2.8 + (speedFactor * 5.5)) * taper
                val brakeRatio = brakePercent / 100.0
                val maxRegen = 35.0
                val brakeRegen = Math.pow(brakeRatio, 0.9) * maxRegen * taper
                val grossRegen = Math.min(50.0, baseRegen + brakeRegen)
                val netRegen = -(grossRegen - (auxKw * 0.5))
                return Math.min(-0.2, Math.round(netRegen * 10.0) / 10.0)
            } else if (speedKmh > 3.0) {
                val taper = ((speedKmh - 3.0) / 7.0).coerceIn(0.0, 1.0)
                val speedFactor = Math.min(1.0, speedKmh / 65.0) * taper
                val coastingRegen = (2.5 + (speedFactor * 5.0)) * taper
                val netRegen = -(coastingRegen - (auxKw * 0.5))
                return Math.min(-0.2, Math.round(netRegen * 10.0) / 10.0)
            }
        }
        return auxKw
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
