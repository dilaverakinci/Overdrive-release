package com.overdrive.app.ui.vehicle

data class TyreData(
    val psi: Double? = null,
    val kPa: Int? = null,
    val tempC: Int? = null,
    val available: Boolean = false,
    val status: String = "normal"
)

data class ClimateData(
    val acOn: Boolean = false,
    val insideTempC: Double? = null,
    val targetTempC: Double = 22.0,
    val fanSpeed: Int = 3,
    val batteryHeat: Boolean = false
)

data class SeatsData(
    val driverHeat: Int = 0,
    val driverCool: Int = 0,
    val passengerHeat: Int = 0,
    val passengerCool: Int = 0,
    val steeringHeat: Boolean = false
)

data class LightsData(
    val daytimeLight: Boolean = true,
    val ambientEnabled: Boolean = false,
    val ambientColour: Int = 0
)

data class AdasData(
    val speedLimitWarning: Boolean = false,
    val childPresenceDetection: Boolean = false
)

data class ChargingData(
    val isCharging: Boolean = false,
    val chargeCapPercent: Int = 100,
    val acCurrentLimitState: Int = 4
)

data class VehicleState(
    val isDataAvailable: Boolean = false,
    val modelId: String = "seal",
    val overallLockState: Int = -1, // -1=unknown, 1=locked, 2=unlocked
    val cloudConnected: Boolean = false,
    val doorLockStates: Map<String, Int> = emptyMap(),
    val doorOpenStates: Map<String, Boolean> = emptyMap(),
    val windowPercent: Map<String, Int> = emptyMap(),
    val windowOpenStates: Map<String, Boolean> = emptyMap(),
    val trunkOpen: Boolean = false,
    val trunkLocked: Boolean = true,
    val socPercent: Double? = null,
    val rangeKm: Int? = null,
    val tyres: Map<String, TyreData> = mapOf(
        "fl" to TyreData(),
        "fr" to TyreData(),
        "rl" to TyreData(),
        "rr" to TyreData()
    ),
    val climate: ClimateData = ClimateData(),
    val seats: SeatsData = SeatsData(),
    val lights: LightsData = LightsData(),
    val adas: AdasData = AdasData(),
    val charging: ChargingData = ChargingData()
)

enum class VehicleCategoryTab(val id: String) {
    SECURITY("security"),
    TRUNK("trunk"),
    CLIMATE("climate"),
    SEATS("seats"),
    WINDOWS("windows"),
    LIGHTS("lights"),
    ADAS("adas"),
    CHARGING("charging"),
    SOUND("sound"),
    SYSTEM("system")
}
