package com.overdrive.app.ui.vehicle

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.Fragment
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.theme.OverdriveTheme

/**
 * 100% Jetpack Compose Native Fragment for Vehicle Control.
 * Directly replaces WebViewFragment for /vehicle (vehicle-control.html).
 */
class VehicleControlComposeFragment : Fragment() {

    private var uiState by mutableStateOf(VehicleControlUiState())

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
                            uiState = uiState.copy(security = uiState.security.copy(isLocked = true))
                            showFeedback("Araç kilitlendi")
                        },
                        onUnlockClick = {
                            uiState = uiState.copy(security = uiState.security.copy(isLocked = false))
                            showFeedback("Araç kilitleri açıldı")
                        },
                        onFlashClick = {
                            showFeedback("Flaşörler yakıldı")
                        },
                        onFindCarClick = {
                            showFeedback("Aracı bul: Korna ve ışıklar aktif")
                        },
                        onToggleTrunk = {
                            val next = !uiState.doors.trunkOpen
                            uiState = uiState.copy(doors = uiState.doors.copy(trunkOpen = next))
                            showFeedback(if (next) "Bagaj kapağı açılıyor" else "Bagaj kapağı kapatılıyor")
                        },
                        onToggleHood = {
                            val next = !uiState.doors.hoodOpen
                            uiState = uiState.copy(doors = uiState.doors.copy(hoodOpen = next))
                            showFeedback(if (next) "Ön kaput açıldı" else "Ön kaput kapatıldı")
                        },
                        onWindowsCloseAll = {
                            uiState = uiState.copy(
                                windows = uiState.windows.copy(
                                    frontLeftOpen = false,
                                    frontRightOpen = false,
                                    rearLeftOpen = false,
                                    rearRightOpen = false,
                                    isVentMode = false
                                )
                            )
                            showFeedback("Tüm camlar kapatılıyor")
                        },
                        onWindowsOpenAll = {
                            uiState = uiState.copy(
                                windows = uiState.windows.copy(
                                    frontLeftOpen = true,
                                    frontRightOpen = true,
                                    rearLeftOpen = true,
                                    rearRightOpen = true,
                                    isVentMode = false
                                )
                            )
                            showFeedback("Tüm camlar açılıyor")
                        },
                        onWindowsVentMode = {
                            val next = !uiState.windows.isVentMode
                            uiState = uiState.copy(windows = uiState.windows.copy(isVentMode = next))
                            showFeedback(if (next) "Havalandırma modu aktif (2 cm)" else "Camlar kapatıldı")
                        },
                        onToggleSunroof = {
                            val next = !uiState.windows.sunroofOpen
                            uiState = uiState.copy(windows = uiState.windows.copy(sunroofOpen = next))
                            showFeedback(if (next) "Sunroof / perde açılıyor" else "Sunroof / perde kapatılıyor")
                        },
                        onToggleClimate = {
                            val next = !uiState.climate.isAcOn
                            uiState = uiState.copy(climate = uiState.climate.copy(isAcOn = next))
                            showFeedback(if (next) "Klima çalıştırıldı" else "Klima durduruldu")
                        },
                        onTempDown = {
                            if (uiState.climate.targetTemp > 16) {
                                val next = uiState.climate.targetTemp - 1
                                uiState = uiState.copy(climate = uiState.climate.copy(targetTemp = next))
                            }
                        },
                        onTempUp = {
                            if (uiState.climate.targetTemp < 30) {
                                val next = uiState.climate.targetTemp + 1
                                uiState = uiState.copy(climate = uiState.climate.copy(targetTemp = next))
                            }
                        },
                        onFanDown = {
                            if (uiState.climate.fanLevel > 1) {
                                val next = uiState.climate.fanLevel - 1
                                uiState = uiState.copy(climate = uiState.climate.copy(fanLevel = next))
                            }
                        },
                        onFanUp = {
                            if (uiState.climate.fanLevel < 7) {
                                val next = uiState.climate.fanLevel + 1
                                uiState = uiState.copy(climate = uiState.climate.copy(fanLevel = next))
                            }
                        },
                        onToggleBatteryHeat = {
                            val next = !uiState.climate.isBatteryHeatOn
                            uiState = uiState.copy(climate = uiState.climate.copy(isBatteryHeatOn = next))
                            showFeedback(if (next) "Batarya ön ısıtma başlatıldı" else "Batarya ısıtma durduruldu")
                        },
                        onCycleDriverSeatHeat = {
                            val next = (uiState.comfort.driverSeatHeat + 1) % 4
                            uiState = uiState.copy(comfort = uiState.comfort.copy(driverSeatHeat = next))
                        },
                        onCycleDriverSeatVent = {
                            val next = (uiState.comfort.driverSeatVent + 1) % 4
                            uiState = uiState.copy(comfort = uiState.comfort.copy(driverSeatVent = next))
                        },
                        onCyclePassengerSeatHeat = {
                            val next = (uiState.comfort.passengerSeatHeat + 1) % 4
                            uiState = uiState.copy(comfort = uiState.comfort.copy(passengerSeatHeat = next))
                        },
                        onCyclePassengerSeatVent = {
                            val next = (uiState.comfort.passengerSeatVent + 1) % 4
                            uiState = uiState.copy(comfort = uiState.comfort.copy(passengerSeatVent = next))
                        },
                        onToggleSteeringHeat = {
                            val next = !uiState.comfort.steeringHeatOn
                            uiState = uiState.copy(comfort = uiState.comfort.copy(steeringHeatOn = next))
                            showFeedback(if (next) "Direksiyon ısıtma açık" else "Direksiyon ısıtma kapalı")
                        },
                        onToggleMirrors = {
                            val next = !uiState.security.mirrorsFolded
                            uiState = uiState.copy(security = uiState.security.copy(mirrorsFolded = next))
                            showFeedback(if (next) "Yan aynalar katlandı" else "Yan aynalar açıldı")
                        },
                        onRotateScreen = {
                            toggleScreenOrientation()
                        }
                    )
                }
            }
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
