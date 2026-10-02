package com.overdrive.app.ui.charging

/**
 * Charging plug connection and session status.
 */
enum class ChargingStatus {
    DISCONNECTED,
    PLUGGED_IN,
    CHARGING,
    COMPLETE,
    FAULT
}

/**
 * Single historical or active charging session record.
 */
data class ChargingSession(
    val id: String,
    val timestamp: Long,
    val location: String = "Ev / AC İstasyon",
    val startSoc: Int,
    val endSoc: Int,
    val energyKwh: Float,
    val durationMinutes: Int,
    val peakPowerKw: Float = 0f,
    val costEstimate: String? = null,
    val odometerKm: Int? = null,
    val unitPrice: Float? = null,
    val totalCost: Float? = null,
    val isDc: Boolean = false,
    val chargeType: String = if (isDc) "DC" else "AC",
    val isManualEdit: Boolean = false,
)

/**
 * Complete UI state for ChargingScreen (100% Jetpack Compose Native).
 */
data class ChargingUiState(
    // Live Battery Telemetry
    val socPercent: Int = 0,
    val targetSocLimit: Int = 80,
    val estimatedRangeKm: Int = 0,
    val batteryCapacityKwh: Float = 0f,
    val batteryTempCelsius: Float = 0f,
    val isBatteryPreHeating: Boolean = false,

    // Live Charging Telemetry
    val status: ChargingStatus = ChargingStatus.DISCONNECTED,
    val livePowerKw: Float = 0f,
    val liveVoltageV: Float = 0f,
    val liveCurrentA: Float = 0f,
    val targetCurrentLimitA: Int = 16, // Allowed: 6, 8, 10, 13, 16, 32
    val remainingMinutesToTarget: Int = 0,
    val sessionEnergyAddedKwh: Float = 0f,
    val isPortUnlocked: Boolean = false,

    // Historical Sessions & Aggregates
    val totalSessionsCount: Int = 0,
    val totalEnergyDeliveredKwh: Float = 0f,
    val sessions: List<ChargingSession> = emptyList(),

    // Tab / Filter selection
    val selectedTab: ChargingTab = ChargingTab.LIVE
)

enum class ChargingTab {
    LIVE,
    SESSIONS
}
