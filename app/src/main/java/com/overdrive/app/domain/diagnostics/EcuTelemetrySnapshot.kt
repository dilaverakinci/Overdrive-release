package com.overdrive.app.domain.diagnostics

/**
 * Detailed telemetry state for Battery Management System (BMS).
 */
data class BmsTelemetry(
    val cellVoltageMinV: Double = Double.NaN,
    val cellVoltageMaxV: Double = Double.NaN,
    val cellVoltageDeltaMv: Int = 0,
    val cellTempMinC: Double = Double.NaN,
    val cellTempMaxC: Double = Double.NaN,
    val cellTempAvgC: Double = Double.NaN,
    val packVoltageV: Double = Double.NaN,
    val packCurrentA: Double = Double.NaN,
    val socPercent: Double = Double.NaN,
    val sohPercent: Double = Double.NaN,
    val lowVoltage12v: Double = Double.NaN,
    val isCellImbalanceWarning: Boolean = false,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for Motor Control Unit (MCU).
 */
data class McuTelemetry(
    val frontMotorRpm: Int = 0,
    val rearMotorRpm: Int = 0,
    val frontMotorTorqueNm: Double = Double.NaN,
    val rearMotorTorqueNm: Double = Double.NaN,
    val motorTempC: Double = Double.NaN,
    val inverterTempC: Double = Double.NaN,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for Vehicle Control Unit (VCU).
 */
data class VcuTelemetry(
    val powerState: String = "OFF",
    val gear: String = "P",
    val driveMode: String = "NORMAL",
    val speedKmh: Double = 0.0,
    val acceleratorPedalPercent: Double = 0.0,
    val isBrakePressed: Boolean = false,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for Electronic Stability Program / Chassis (ESP).
 */
data class EspTelemetry(
    val flWheelSpeedKmh: Double = Double.NaN,
    val frWheelSpeedKmh: Double = Double.NaN,
    val rlWheelSpeedKmh: Double = Double.NaN,
    val rrWheelSpeedKmh: Double = Double.NaN,
    val steeringAngleDeg: Double = Double.NaN,
    val isAbsActive: Boolean = false,
    val isEspActive: Boolean = false,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for Body Control Module (BCM).
 */
data class BcmTelemetry(
    val doorsOpenCount: Int = 0,
    val isLocked: Boolean = true,
    val isHeadlightOn: Boolean = false,
    val isHighBeamOn: Boolean = false,
    val isHazardLightOn: Boolean = false,
    val seatbeltsFastenedCount: Int = 0,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for HVAC & Thermal Management.
 */
data class HvacTelemetry(
    val cabinTempC: Double = Double.NaN,
    val targetTempC: Double = Double.NaN,
    val isAcOn: Boolean = false,
    val fanLevel: Int = 0,
    val pm25Inside: Int = -1,
    val pm25Outside: Int = -1,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for Tire Pressure Monitoring System (TPMS).
 */
data class TpmsTelemetry(
    val flPressureBar: Double = Double.NaN,
    val frPressureBar: Double = Double.NaN,
    val rlPressureBar: Double = Double.NaN,
    val rrPressureBar: Double = Double.NaN,
    val flTempC: Int = -1,
    val frTempC: Int = -1,
    val rlTempC: Int = -1,
    val rrTempC: Int = -1,
    val hasPressureWarning: Boolean = false,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for Electric Power Steering (EPS).
 */
data class EpsTelemetry(
    val steeringAngleDeg: Double = Double.NaN,
    val assistMode: String = "COMFORT",
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Detailed telemetry state for Electronic Parking Brake (EPB).
 */
data class EpbTelemetry(
    val isParkBrakeEngaged: Boolean = true,
    val isAutoHoldActive: Boolean = false,
    val status: EcuHealthStatus = EcuHealthStatus.OFFLINE
)

/**
 * Standardized snapshot summary for each ECU on the diagnostic dashboard.
 */
data class EcuTelemetrySnapshot(
    val ecu: EcuType,
    val status: EcuHealthStatus,
    val timestampMs: Long = System.currentTimeMillis(),
    val primaryMetric: String = "",
    val secondaryMetric: String = "",
    val details: Map<String, String> = emptyMap(),
    val errorNotes: String? = null
)
