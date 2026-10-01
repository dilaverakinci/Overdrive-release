package com.overdrive.app.domain.model

/**
 * Standard vehicle transmission gear representation.
 */
enum class Gear {
    P, R, N, D, M, S, UNKNOWN
}

/**
 * Vehicle energy consumption mode.
 */
enum class EnergyMode {
    EV, HEV, UNKNOWN
}

/**
 * Driving dynamic operation mode.
 */
enum class OperationMode {
    ECO, NORMAL, SPORT, SNOW, UNKNOWN
}

/**
 * Immutable domain model representing vehicle powertrain, speed, torque, and dynamics.
 */
data class PowertrainState(
    val speedKmh: Double = 0.0,
    val accelPercent: Int = 0,
    val brakePercent: Int = 0,
    val frontMotorSpeedRpm: Int = 0,
    val rearMotorSpeedRpm: Int = 0,
    val frontMotorTorqueNm: Double = 0.0,
    val engineSpeedRpm: Int = 0,
    val enginePowerKw: Double = 0.0,
    val gear: Gear = Gear.UNKNOWN,
    val gearRaw: Int = 0,
    val energyMode: EnergyMode = EnergyMode.EV,
    val operationMode: OperationMode = OperationMode.NORMAL,
    val totalMileageKm: Int = 0,
    val evMileageKm: Int = 0,
    val hevMileageKm: Int = 0,
    val currentTripMileageKm: Double = 0.0,
    val avgFuelConPer100Km: Double = Double.NaN,
    val avgElecConPer100Km: Double = Double.NaN,
    val timestamp: Long = System.currentTimeMillis()
)
