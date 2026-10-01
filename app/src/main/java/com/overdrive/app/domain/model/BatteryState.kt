package com.overdrive.app.domain.model

/**
 * Immutable domain model representing vehicle battery, energy, and charging status.
 */
data class BatteryState(
    val socPercent: Double = 0.0,
    val socHevPercent: Double = 0.0,
    val socTargetPercent: Int = 0,
    val capacityAh: Double = 0.0,
    val remainKwh: Double = 0.0,
    val sohPercent: Double = 100.0,
    val voltage12v: Double = 0.0,
    val voltage12vLevel: Int = 0,
    val voltage12vAtMs: Long = 0L,
    val highCellTempC: Double = Double.NaN,
    val lowCellTempC: Double = Double.NaN,
    val avgCellTempC: Double = Double.NaN,
    val highCellVoltage: Double = Double.NaN,
    val lowCellVoltage: Double = Double.NaN,
    val hvPackVoltage: Double = 0.0,
    val hvPackCurrentAmps: Double = 0.0,
    val hvBatteryPowerKw: Double = 0.0,
    val elecRangeKm: Int = 0,
    val fuelRangeKm: Int = 0,
    val chargingState: Int = 0,
    val chargingGunState: Int = 0,
    val chargerWorkState: Int = 0,
    val chargingMode: Int = 0,
    val chargingPowerKw: Double = 0.0,
    val chargingPercent: Int = 0,
    val chargingRestTimeMinutes: Int = 0,
    val isCharging: Boolean = false,
    val isFastCharging: Boolean = false,
    val isVtolActive: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
