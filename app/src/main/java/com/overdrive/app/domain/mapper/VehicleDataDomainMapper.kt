package com.overdrive.app.domain.mapper

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.model.BatteryState
import com.overdrive.app.domain.model.BodyworkState
import com.overdrive.app.domain.model.ChassisState
import com.overdrive.app.domain.model.EnergyMode
import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.HvacState
import com.overdrive.app.domain.model.OperationMode
import com.overdrive.app.domain.model.PadOrientation
import com.overdrive.app.domain.model.PowertrainState

/**
 * Maps raw [BydVehicleData] hardware snapshots to strongly-typed, immutable domain state models.
 */
object VehicleDataDomainMapper {

    @JvmStatic
    fun toBatteryState(data: BydVehicleData?): BatteryState {
        if (data == null) return BatteryState()

        val isChargingNow = data.chargingGunState == 1 || data.chargingState == 1 || (!data.chargingPowerKw.isNaN() && data.chargingPowerKw > 0.1)
        val isFast = (!data.chargingPowerKw.isNaN() && data.chargingPowerKw > 22.0) || data.chargingMode == 2

        val restMinutes = if (data.chargingRestTimeMinutes != BydVehicleData.UNAVAILABLE) {
            val hours = if (data.chargingRestTimeHours != BydVehicleData.UNAVAILABLE) data.chargingRestTimeHours else 0
            (hours * 60) + data.chargingRestTimeMinutes
        } else 0

        return BatteryState(
            socPercent = if (data.socPercent.isNaN()) 0.0 else data.socPercent,
            socHevPercent = if (data.socHevPercent.isNaN()) 0.0 else data.socHevPercent,
            socTargetPercent = if (data.socTargetPercent == BydVehicleData.UNAVAILABLE) 0 else data.socTargetPercent,
            capacityAh = if (data.capacityAh.isNaN()) 0.0 else data.capacityAh,
            remainKwh = if (data.remainKwh.isNaN()) 0.0 else data.remainKwh,
            sohPercent = if (data.sohPercent.isNaN()) 100.0 else data.sohPercent,
            voltage12v = if (data.voltage12v.isNaN()) 0.0 else data.voltage12v,
            voltage12vLevel = if (data.voltageLevelRaw == BydVehicleData.UNAVAILABLE) 0 else data.voltageLevelRaw,
            voltage12vAtMs = data.voltage12vAtMs,
            highCellTempC = data.highCellTempC,
            lowCellTempC = data.lowCellTempC,
            avgCellTempC = data.avgCellTempC,
            highCellVoltage = data.highCellVoltage,
            lowCellVoltage = data.lowCellVoltage,
            hvPackVoltage = if (data.hvPackVoltage.isNaN()) 0.0 else data.hvPackVoltage,
            hvPackCurrentAmps = if (data.hvPackCurrentAmps.isNaN()) 0.0 else data.hvPackCurrentAmps,
            hvBatteryPowerKw = if (data.hvBatteryPowerKw.isNaN()) 0.0 else data.hvBatteryPowerKw,
            elecRangeKm = if (data.elecRangeKm == BydVehicleData.UNAVAILABLE) 0 else data.elecRangeKm,
            fuelRangeKm = if (data.fuelRangeKm == BydVehicleData.UNAVAILABLE) 0 else data.fuelRangeKm,
            chargingState = if (data.chargingState == BydVehicleData.UNAVAILABLE) 0 else data.chargingState,
            chargingGunState = if (data.chargingGunState == BydVehicleData.UNAVAILABLE) 0 else data.chargingGunState,
            chargerWorkState = if (data.chargerWorkState == BydVehicleData.UNAVAILABLE) 0 else data.chargerWorkState,
            chargingMode = if (data.chargingMode == BydVehicleData.UNAVAILABLE) 0 else data.chargingMode,
            chargingPowerKw = if (data.chargingPowerKw.isNaN()) 0.0 else data.chargingPowerKw,
            chargingPercent = if (data.chargingPercent == BydVehicleData.UNAVAILABLE) 0 else data.chargingPercent,
            chargingRestTimeMinutes = restMinutes,
            isCharging = isChargingNow,
            isFastCharging = isFast,
            isVtolActive = data.vtolCharging,
            timestamp = if (data.timestamp > 0) data.timestamp else System.currentTimeMillis()
        )
    }

    @JvmStatic
    fun toPowertrainState(data: BydVehicleData?): PowertrainState {
        if (data == null) return PowertrainState()

        val gear = when (data.gearMode) {
            1 -> Gear.P
            2 -> Gear.R
            3 -> Gear.N
            4 -> Gear.D
            5 -> Gear.M
            6 -> Gear.S
            else -> Gear.UNKNOWN
        }

        val energy = when (data.energyMode) {
            1 -> EnergyMode.EV
            2 -> EnergyMode.HEV
            else -> EnergyMode.EV
        }

        val operation = when (data.operationMode) {
            1 -> OperationMode.ECO
            2 -> OperationMode.NORMAL
            3 -> OperationMode.SPORT
            4 -> OperationMode.SNOW
            else -> OperationMode.NORMAL
        }

        return PowertrainState(
            speedKmh = if (data.speedKmh.isNaN()) 0.0 else data.speedKmh,
            accelPercent = if (data.accelPercent == BydVehicleData.UNAVAILABLE) 0 else data.accelPercent,
            brakePercent = if (data.brakePercent == BydVehicleData.UNAVAILABLE) 0 else data.brakePercent,
            frontMotorSpeedRpm = if (data.frontMotorSpeed == BydVehicleData.UNAVAILABLE) 0 else data.frontMotorSpeed,
            rearMotorSpeedRpm = if (data.rearMotorSpeed == BydVehicleData.UNAVAILABLE) 0 else data.rearMotorSpeed,
            frontMotorTorqueNm = if (data.frontMotorTorque.isNaN()) 0.0 else data.frontMotorTorque,
            engineSpeedRpm = if (data.engineSpeedRpm == BydVehicleData.UNAVAILABLE) 0 else data.engineSpeedRpm,
            enginePowerKw = if (data.enginePowerKw.isNaN()) 0.0 else data.enginePowerKw,
            gear = gear,
            gearRaw = data.gearMode,
            energyMode = energy,
            operationMode = operation,
            totalMileageKm = if (data.totalMileageKm == BydVehicleData.UNAVAILABLE) 0 else data.totalMileageKm,
            evMileageKm = if (data.evMileageKm == BydVehicleData.UNAVAILABLE) 0 else data.evMileageKm,
            hevMileageKm = if (data.hevMileageKm == BydVehicleData.UNAVAILABLE) 0 else data.hevMileageKm,
            currentTripMileageKm = if (data.currentTripMileageKm.isNaN()) 0.0 else data.currentTripMileageKm,
            avgFuelConPer100Km = data.avgFuelConPer100Km,
            avgElecConPer100Km = data.avgElecConPer100Km,
            timestamp = if (data.timestamp > 0) data.timestamp else System.currentTimeMillis()
        )
    }

    @JvmStatic
    fun toHvacState(data: BydVehicleData?): HvacState {
        if (data == null) return HvacState()

        val seatHeat = data.seatHeat
        val seatCool = data.seatCool

        return HvacState(
            insideTempC = data.insideTempC,
            outsideTempC = data.outsideTempC,
            isAcOn = data.acStartState == 1,
            fanSpeed = if (data.acFanLevel == BydVehicleData.UNAVAILABLE) 0 else data.acFanLevel,
            driverSetpointTemp = if (data.acSetpointDriver == BydVehicleData.UNAVAILABLE) 22.0 else data.acSetpointDriver.toDouble(),
            passengerSetpointTemp = if (data.acSetpointPassenger == BydVehicleData.UNAVAILABLE) 22.0 else data.acSetpointPassenger.toDouble(),
            tempUnitCelsius = data.tempUnit != 1,
            cycleMode = if (data.acCycleMode == BydVehicleData.UNAVAILABLE) 0 else data.acCycleMode,
            windMode = if (data.acWindMode == BydVehicleData.UNAVAILABLE) 0 else data.acWindMode,
            driverSeatHeat = if (seatHeat != null && seatHeat.isNotEmpty() && seatHeat[0] != BydVehicleData.UNAVAILABLE) seatHeat[0] else 0,
            passengerSeatHeat = if (seatHeat != null && seatHeat.size > 1 && seatHeat[1] != BydVehicleData.UNAVAILABLE) seatHeat[1] else 0,
            driverSeatCool = if (seatCool != null && seatCool.isNotEmpty() && seatCool[0] != BydVehicleData.UNAVAILABLE) seatCool[0] else 0,
            passengerSeatCool = if (seatCool != null && seatCool.size > 1 && seatCool[1] != BydVehicleData.UNAVAILABLE) seatCool[1] else 0,
            steeringWheelHeat = data.steeringWheelHeat == 2,
            pm25Inside = if (data.pm25Inside == BydVehicleData.UNAVAILABLE) 0 else data.pm25Inside,
            pm25Outside = if (data.pm25Outside == BydVehicleData.UNAVAILABLE) 0 else data.pm25Outside,
            timestamp = if (data.timestamp > 0) data.timestamp else System.currentTimeMillis()
        )
    }

    @JvmStatic
    fun toBodyworkState(data: BydVehicleData?): BodyworkState {
        if (data == null) return BodyworkState()

        val windows = data.windowOpenPercent

        val isLocked = if (data.doorLockStatus != null && data.doorLockStatus.isNotEmpty()) {
            data.doorLockStatus.all { it != 0 && it != -1 }
        } else null

        return BodyworkState(
            isLocked = isLocked,
            doorOpenFl = false,
            doorOpenFr = false,
            doorOpenRl = false,
            doorOpenRr = false,
            hoodOpen = false,
            trunkOpen = false,
            chargePortOpen = false,
            windowPercentFl = if (windows != null && windows.isNotEmpty() && windows[0] != BydVehicleData.UNAVAILABLE) windows[0] else 0,
            windowPercentFr = if (windows != null && windows.size > 1 && windows[1] != BydVehicleData.UNAVAILABLE) windows[1] else 0,
            windowPercentRl = if (windows != null && windows.size > 2 && windows[2] != BydVehicleData.UNAVAILABLE) windows[2] else 0,
            windowPercentRr = if (windows != null && windows.size > 3 && windows[3] != BydVehicleData.UNAVAILABLE) windows[3] else 0,
            sunroofState = if (data.sunroofState == BydVehicleData.UNAVAILABLE) 0 else data.sunroofState,
            sunshadePercent = if (data.sunshadePercent == BydVehicleData.UNAVAILABLE) 0 else data.sunshadePercent,
            lowBeamOn = data.lowBeam,
            highBeamOn = data.highBeam,
            frontFogOn = data.frontFog,
            rearFogOn = data.rearFog,
            hazardOn = data.hazard,
            daytimeLightOn = data.dayTimeLight,
            wiperActive = data.wiperState == 1 || data.autoWiperState == 1,
            padOrientation = PadOrientation.HORIZONTAL,
            ambientColor = if (data.ambientColourKnown) data.ambientColour else 0,
            ambientEnabled = data.ambientEnabled == 1,
            timestamp = if (data.timestamp > 0) data.timestamp else System.currentTimeMillis()
        )
    }

    @JvmStatic
    fun toChassisState(data: BydVehicleData?): ChassisState {
        if (data == null) return ChassisState()

        val tpmsKpa = data.tyrePressure
        val tpmsTemp = data.tyreTemperature
        val radar = data.radarDistances

        val flKpa = if (tpmsKpa != null && tpmsKpa.isNotEmpty() && tpmsKpa[0] != BydVehicleData.UNAVAILABLE) tpmsKpa[0] else 0
        val frKpa = if (tpmsKpa != null && tpmsKpa.size > 1 && tpmsKpa[1] != BydVehicleData.UNAVAILABLE) tpmsKpa[1] else 0
        val rlKpa = if (tpmsKpa != null && tpmsKpa.size > 2 && tpmsKpa[2] != BydVehicleData.UNAVAILABLE) tpmsKpa[2] else 0
        val rrKpa = if (tpmsKpa != null && tpmsKpa.size > 3 && tpmsKpa[3] != BydVehicleData.UNAVAILABLE) tpmsKpa[3] else 0

        val flTemp = if (tpmsTemp != null && tpmsTemp.isNotEmpty() && tpmsTemp[0] != BydVehicleData.UNAVAILABLE) tpmsTemp[0] else 0
        val frTemp = if (tpmsTemp != null && tpmsTemp.size > 1 && tpmsTemp[1] != BydVehicleData.UNAVAILABLE) tpmsTemp[1] else 0
        val rlTemp = if (tpmsTemp != null && tpmsTemp.size > 2 && tpmsTemp[2] != BydVehicleData.UNAVAILABLE) tpmsTemp[2] else 0
        val rrTemp = if (tpmsTemp != null && tpmsTemp.size > 3 && tpmsTemp[3] != BydVehicleData.UNAVAILABLE) tpmsTemp[3] else 0

        val radarList = radar?.toList() ?: emptyList()

        return ChassisState(
            tyrePressureFlKpa = flKpa,
            tyrePressureFrKpa = frKpa,
            tyrePressureRlKpa = rlKpa,
            tyrePressureRrKpa = rrKpa,
            tyrePressureFlBar = flKpa / 100.0f,
            tyrePressureFrBar = frKpa / 100.0f,
            tyrePressureRlBar = rlKpa / 100.0f,
            tyrePressureRrBar = rrKpa / 100.0f,
            tyreTempFlC = flTemp,
            tyreTempFrC = frTemp,
            tyreTempRlC = rlTemp,
            tyreTempRrC = rrTemp,
            tyreSystemHealthOk = data.tyreSystemState != BydVehicleData.TYRE_PRESSURE_STATE_OVERPRESSURE &&
                    data.tyreSystemState != BydVehicleData.TYRE_PRESSURE_STATE_UNDERPRESSURE,
            radarDistances = radarList,
            slopeDegrees = if (data.slopeDegrees.isNaN()) 0.0 else data.slopeDegrees,
            steeringAngleDegrees = if (data.steeringAngleDegrees.isNaN()) 0.0 else data.steeringAngleDegrees,
            timestamp = if (data.timestamp > 0) data.timestamp else System.currentTimeMillis()
        )
    }
}
