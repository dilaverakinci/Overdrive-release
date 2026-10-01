package com.overdrive.app.domain.model

/**
 * Immutable domain model representing climate control, cabin comfort, and air quality.
 */
data class HvacState(
    val insideTempC: Double = Double.NaN,
    val outsideTempC: Double = Double.NaN,
    val isAcOn: Boolean = false,
    val fanSpeed: Int = 0,
    val driverSetpointTemp: Double = 22.0,
    val passengerSetpointTemp: Double = 22.0,
    val tempUnitCelsius: Boolean = true,
    val cycleMode: Int = 0, // 0: Auto/Outside, 1: Internal Recirculation
    val windMode: Int = 0,
    val driverSeatHeat: Int = 0, // 0=off, 1=low, 2=high
    val passengerSeatHeat: Int = 0,
    val driverSeatCool: Int = 0,
    val passengerSeatCool: Int = 0,
    val steeringWheelHeat: Boolean = false,
    val pm25Inside: Int = 0,
    val pm25Outside: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
