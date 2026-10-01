package com.overdrive.app.ui.vehicle

import android.content.pm.ActivityInfo
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.overdrive.app.byd.cloud.BydCloudConfig
import com.overdrive.app.domain.repository.RepositoryProvider
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.concurrent.Executors

/**
 * 100% Jetpack Compose Native Fragment for Vehicle Control.
 * Directly replaces WebViewFragment for /vehicle (vehicle-control.html).
 */
class VehicleControlComposeFragment : Fragment() {

    private var uiState by mutableStateOf(VehicleControlUiState())

    private val mainHandler = Handler(Looper.getMainLooper())
    private var workerExecutor = Executors.newSingleThreadExecutor()

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshVehicleStateAsync()
            if (isResumed) {
                mainHandler.postDelayed(this, 5000L)
            }
        }
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
        mainHandler.post(refreshRunnable)
    }

    override fun onPause() {
        mainHandler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacks(refreshRunnable)
        workerExecutor.shutdownNow()
        workerExecutor = Executors.newSingleThreadExecutor()
        super.onDestroyView()
    }

    private fun refreshVehicleModel() {
        val modelDisplayName = VehicleTopDownArt.getSelectedModelDisplayName(context)
        uiState = uiState.copy(vehicleModelName = modelDisplayName)
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
            }
        }
    }

    private fun refreshVehicleStateAsync() {
        workerExecutor.execute {
            var conn: HttpURLConnection? = null
            var parsedJson: JSONObject? = null
            try {
                conn = DaemonHttpClient.open("/api/vehicle/state", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    parsedJson = JSONObject(body)
                }
            } catch (_: Throwable) {
            } finally {
                try { conn?.disconnect() } catch (_: Throwable) {}
            }

            val json = parsedJson ?: return@execute

            mainHandler.post {
                if (!isAdded || view == null) return@post

                // Doors & Lock
                val doors = json.optJSONObject("doors")
                val overall = doors?.optInt("overall", -1) ?: -1
                val isLocked: Boolean? = when (overall) {
                    1 -> true
                    2 -> false
                    else -> uiState.security.isLocked
                }

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

                uiState = uiState.copy(
                    security = uiState.security.copy(isLocked = isLocked),
                    tyres = newTyresState,
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
                    )
                )
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
            }, 800L)
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
