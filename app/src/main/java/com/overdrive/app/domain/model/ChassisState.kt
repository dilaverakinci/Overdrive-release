package com.overdrive.app.domain.model

/**
 * Immutable domain model representing tyre pressure monitoring (TPMS), radar sensors, and chassis dynamics.
 */
data class ChassisState(
    val tyrePressureFlKpa: Int = 0,
    val tyrePressureFrKpa: Int = 0,
    val tyrePressureRlKpa: Int = 0,
    val tyrePressureRrKpa: Int = 0,
    val tyrePressureFlBar: Float = 0.0f,
    val tyrePressureFrBar: Float = 0.0f,
    val tyrePressureRlBar: Float = 0.0f,
    val tyrePressureRrBar: Float = 0.0f,
    val tyreTempFlC: Int = 0,
    val tyreTempFrC: Int = 0,
    val tyreTempRlC: Int = 0,
    val tyreTempRrC: Int = 0,
    val tyreSystemHealthOk: Boolean = true,
    val radarDistances: List<Int> = emptyList(),
    val slopeDegrees: Double = 0.0,
    val steeringAngleDegrees: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
