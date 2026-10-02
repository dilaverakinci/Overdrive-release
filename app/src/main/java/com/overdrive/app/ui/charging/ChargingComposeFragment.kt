package com.overdrive.app.ui.charging

import android.os.Bundle
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
import com.overdrive.app.domain.repository.RepositoryProvider
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.util.DaemonHttpClient
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * 100% Jetpack Compose Native Fragment for Charging and Battery Management.
 * Directly replaces legacy WebViewFragment for /charging.
 * Observes real vehicle battery telemetry and controls charging parameters.
 */
class ChargingComposeFragment : Fragment() {

    private var uiState by mutableStateOf(ChargingUiState())
    private val workerExecutor = Executors.newSingleThreadExecutor()

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
                    ChargingScreen(
                        state = uiState,
                        onTabSelected = { tab ->
                            uiState = uiState.copy(selectedTab = tab)
                        },
                        onTargetSocChange = { targetSoc ->
                            uiState = uiState.copy(targetSocLimit = targetSoc)
                            sendDaemonPost("/api/vehicle/charge-cap", "{\"percent\":$targetSoc}")
                            showFeedback("Hedef şarj sınırı: %$targetSoc")
                        },
                        onCurrentLimitChange = { amp ->
                            uiState = uiState.copy(targetCurrentLimitA = amp)
                            val stateVal = when (amp) {
                                6 -> 1
                                8 -> 2
                                10 -> 3
                                16 -> 4
                                else -> 5
                            }
                            sendDaemonPost("/api/vehicle/ac-charge-current-limit", "{\"state\":$stateVal}")
                            showFeedback("Maksimum şarj akımı: ${amp}A")
                        },
                        onTogglePortLock = {
                            val next = !uiState.isPortUnlocked
                            uiState = uiState.copy(isPortUnlocked = next)
                            sendDaemonPost("/api/vehicle/port-lock", "{\"unlocked\":$next}")
                            showFeedback(if (next) "Şarj tabanca kilidi çözüldü" else "Şarj tabancası kilitlendi")
                        },
                        onToggleBatteryPreHeat = {
                            val next = !uiState.isBatteryPreHeating
                            uiState = uiState.copy(isBatteryPreHeating = next)
                            sendDaemonPost("/api/vehicle/battery-preheat", "{\"enabled\":$next}")
                            showFeedback(if (next) "Batarya ön ısıtma başlatıldı" else "Batarya ön ısıtma durduruldu")
                        },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeBatteryTelemetry()
    }

    override fun onDestroyView() {
        workerExecutor.shutdownNow()
        super.onDestroyView()
    }

    private fun observeBatteryTelemetry() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                RepositoryProvider.batteryRepository.batteryState.collect { bat ->
                    val chargingStatus = when {
                        bat.isCharging -> ChargingStatus.CHARGING
                        bat.chargingGunState == 1 -> ChargingStatus.PLUGGED_IN
                        bat.chargingPercent >= 100 || (bat.socPercent >= 99.5 && bat.chargingGunState == 1) -> ChargingStatus.COMPLETE
                        else -> ChargingStatus.DISCONNECTED
                    }

                    val powerKw = if (bat.isCharging && bat.chargingPowerKw > 0) {
                        bat.chargingPowerKw.toFloat()
                    } else 0f

                    val voltage = if (bat.hvPackVoltage > 0) {
                        bat.hvPackVoltage.toFloat()
                    } else if (bat.isCharging) 230f else 0f

                    val current = if (bat.hvPackCurrentAmps > 0) {
                        bat.hvPackCurrentAmps.toFloat()
                    } else if (powerKw > 0f && voltage > 0f) {
                        (powerKw * 1000f) / voltage
                    } else 0f

                    uiState = uiState.copy(
                        socPercent = bat.socPercent.toInt().coerceIn(0, 100),
                        estimatedRangeKm = bat.elecRangeKm,
                        batteryCapacityKwh = if (bat.remainKwh > 0) bat.remainKwh.toFloat() else uiState.batteryCapacityKwh,
                        batteryTempCelsius = if (!bat.avgCellTempC.isNaN()) bat.avgCellTempC.toFloat() else uiState.batteryTempCelsius,
                        status = chargingStatus,
                        livePowerKw = powerKw,
                        liveVoltageV = voltage,
                        liveCurrentA = current,
                        remainingMinutesToTarget = if (bat.isCharging) bat.chargingRestTimeMinutes else 0,
                        targetSocLimit = if (bat.socTargetPercent in 50..100) bat.socTargetPercent else uiState.targetSocLimit,
                    )
                }
            }
        }
    }

    private fun sendDaemonPost(endpoint: String, jsonBody: String) {
        workerExecutor.execute {
            try {
                val conn = DaemonHttpClient.open(endpoint, "POST", 3000, 5000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(jsonBody.toByteArray()) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private fun showFeedback(message: String) {
        context?.let {
            Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
        }
    }
}
