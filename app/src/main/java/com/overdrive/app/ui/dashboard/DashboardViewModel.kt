package com.overdrive.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.domain.model.BatteryState
import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.PowertrainState
import com.overdrive.app.domain.repository.BatteryRepository
import com.overdrive.app.domain.repository.PowertrainRepository
import com.overdrive.app.domain.repository.RepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Modern Reactive ViewModel for DashboardComposeFragment.
 * Subscribes to real-time StateFlow domain repositories to provide instant 60-120 FPS UI updates
 * without waiting for 5-second HTTP polling loops.
 */
class DashboardViewModel(
    batteryRepository: BatteryRepository = RepositoryProvider.batteryRepository,
    powertrainRepository: PowertrainRepository = RepositoryProvider.powertrainRepository,
) : ViewModel() {

    val vehicleSnapshot: StateFlow<DashboardVehicleSnapshot?> = combine(
        batteryRepository.batteryState,
        powertrainRepository.powertrainState
    ) { battery: BatteryState, powertrain: PowertrainState ->
        mapToSnapshot(battery, powertrain)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun refresh() {
        viewModelScope.launch {
            RepositoryProvider.batteryRepository.refresh()
            RepositoryProvider.powertrainRepository.refresh()
        }
    }

    companion object {
        fun mapToSnapshot(battery: BatteryState, powertrain: PowertrainState): DashboardVehicleSnapshot? {
            // If both battery and powertrain are unpopulated (initial state with 0s/defaults), return null
            if (battery.socPercent <= 0.0 && powertrain.speedKmh <= 0.0 && powertrain.gear == Gear.UNKNOWN) {
                return null
            }

            val gearStr = when (powertrain.gear) {
                Gear.P -> "P"
                Gear.R -> "R"
                Gear.N -> "N"
                Gear.D -> "D"
                Gear.M -> "M"
                Gear.S -> "S"
                Gear.UNKNOWN -> null
            }

            val rangeDistance = if (battery.elecRangeKm > 0) {
                DashboardDistance(battery.elecRangeKm, DashboardDistance.Unit.KILOMETRES)
            } else null

            val chargingSnapshot = if (battery.isCharging) {
                DashboardChargingSnapshot(
                    charging = true,
                    plugged = battery.chargingGunState == 1,
                    full = battery.socPercent >= 99.9,
                    fault = false,
                    stateName = if (battery.isFastCharging) "FAST_CHARGING" else "CHARGING",
                    powerKw = if (battery.chargingPowerKw > 0.0) battery.chargingPowerKw else null,
                    powerEstimated = false,
                    timeToFullMinutes = if (battery.chargingRestTimeMinutes > 0) battery.chargingRestTimeMinutes else null,
                    sessionKwh = null,
                    sessionEnergyIncomplete = false,
                    sessionEnergyEstimated = false,
                    sessionEnergySource = null
                )
            } else null

            return DashboardVehicleSnapshot(
                socPercent = battery.socPercent,
                range = rangeDistance,
                charging = chargingSnapshot,
                activeRecordingCameras = null,
                rangeDetails = null,
                gear = gearStr,
                speedKmh = powertrain.speedKmh,
                isAccOn = powertrain.gear != Gear.P || powertrain.speedKmh > 0.0 || battery.isCharging,
                isRecording = null,
                isGpuSurveillance = null
            )
        }
    }
}
