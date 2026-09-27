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
    val peakPowerKw: Float,
    val costEstimate: String? = null,
)

/**
 * Complete UI state for ChargingScreen (100% Jetpack Compose Native).
 */
data class ChargingUiState(
    // Live Battery Telemetry
    val socPercent: Int = 74,
    val targetSocLimit: Int = 80,
    val estimatedRangeKm: Int = 412,
    val batteryCapacityKwh: Float = 82.5f,
    val batteryTempCelsius: Float = 24.5f,
    val isBatteryPreHeating: Boolean = false,

    // Live Charging Telemetry
    val status: ChargingStatus = ChargingStatus.CHARGING,
    val livePowerKw: Float = 11.2f,
    val liveVoltageV: Float = 230f,
    val liveCurrentA: Float = 16.0f,
    val targetCurrentLimitA: Int = 16, // Allowed: 6, 8, 10, 13, 16, 32
    val remainingMinutesToTarget: Int = 38,
    val sessionEnergyAddedKwh: Float = 8.4f,
    val isPortUnlocked: Boolean = false,

    // Historical Sessions & Aggregates
    val totalSessionsCount: Int = 42,
    val totalEnergyDeliveredKwh: Float = 984.6f,
    val sessions: List<ChargingSession> = listOf(
        ChargingSession(
            id = "ch-104",
            timestamp = System.currentTimeMillis() - 86400000L,
            location = "Ev (AC)",
            startSoc = 32,
            endSoc = 80,
            energyKwh = 39.6f,
            durationMinutes = 210,
            peakPowerKw = 11.0f,
            costEstimate = "₺95.00"
        ),
        ChargingSession(
            id = "ch-103",
            timestamp = System.currentTimeMillis() - 86400000L * 3,
            location = "Trugo DC Hızlı Şarj",
            startSoc = 18,
            endSoc = 85,
            energyKwh = 55.2f,
            durationMinutes = 34,
            peakPowerKw = 120.0f,
            costEstimate = "₺414.00"
        ),
        ChargingSession(
            id = "ch-102",
            timestamp = System.currentTimeMillis() - 86400000L * 6,
            location = "İşyeri (AC)",
            startSoc = 50,
            endSoc = 80,
            energyKwh = 24.8f,
            durationMinutes = 135,
            peakPowerKw = 11.0f,
            costEstimate = "₺0.00"
        )
    ),

    // Tab / Filter selection
    val selectedTab: ChargingTab = ChargingTab.LIVE
)

enum class ChargingTab {
    LIVE,
    SESSIONS
}
