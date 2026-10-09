package com.overdrive.app.ui.charging

/**
 * Data models for the native Charging feature.
 */

data class ChargingSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long,
    val inProgress: Boolean,
    val chargingNow: Boolean,
    val startSoc: Double?,
    val endSoc: Double?,
    val energyAdded: Double?,
    val peakPower: Double?,
    val avgPower: Double?,
    val rangeGained: Int?,
    val isDc: Boolean?,
    val electricityRate: Double?,
    val cost: Double?,
    val currency: String,
    val timeToFullMin: Int?,
    val tempHigh: Double?,
    val tempLow: Double?,
    val tempAvg: Double?,
    val durationMinutes: Long?,
    val lat: Double?,
    val lng: Double?,
    val placeLabel: String?,
    val startOdometerKm: Int?,
    val tariffLabel: String? = null,
    val isEstimated: Boolean = false,
    val livePowerKw: Double? = null
)

data class ChargingLiveState(
    val charging: Boolean = false,
    val plugged: Boolean = false,
    val full: Boolean = false,
    val fault: Boolean = false,
    val socPercent: Double = 0.0,
    val sessionKwh: Double = 0.0,
    val sessionEnergyIncomplete: Boolean = false,
    val sessionEnergyEstimated: Boolean = false,
    val sessionEnergySource: String = "",
    val timeToFullMin: Int = -1,
    val powerKw: Double = 0.0,
    val isEstimated: Boolean = false,
    val rangeKm: Double = -1.0,
    val sohPercent: Double = -1.0
)

data class SohPoint(
    val dayEpoch: Long,
    val sohPercent: Double
)

data class ChargingDailyPoint(
    val dayEpoch: Long,
    val sessions: Int,
    val energy: Double,
    val cost: Double,
    val incomplete: Int,
    val estimated: Int
)

data class ChargingSummary(
    val periodSessions: Int = 0,
    val periodEnergyKwh: Double = 0.0,
    val periodCost: Double = 0.0,
    val periodDcCount: Int = 0,
    val periodAcCount: Int = 0,
    val periodRangeGained: Int = 0,
    val periodIncompleteSessions: Int = 0,
    val periodEstimatedSessions: Int = 0,
    val avgCostPerKwh: Double? = null,
    val lifetimeSessions: Int = 0,
    val lifetimeEnergyKwh: Double = 0.0,
    val lifetimeCost: Double = 0.0,
    val lifetimeIncompleteSessions: Int = 0,
    val lifetimeEstimatedSessions: Int = 0,
    val sohTrend: List<SohPoint> = emptyList(),
    val daily: List<ChargingDailyPoint> = emptyList(),
    val live: ChargingLiveState = ChargingLiveState()
)

data class LocationTariff(
    val id: String,
    val label: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Int,
    val acRate: Double,
    val dcRate: Double,
    val currency: String,
    val enabled: Boolean = true,
    val createdAt: Long = 0L,
    val lastUsedAt: Long = 0L,
    val useCount: Int = 0
)

data class TariffsPayload(
    val tariffs: List<LocationTariff> = emptyList(),
    val defaultTariffId: String? = null,
    val matchedTariffId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val globalRate: Double? = null,
    val globalDcRate: Double? = null,
    val currency: String? = null
)

data class ChargingBootstrapData(
    val summary: ChargingSummary,
    val sessions: List<ChargingSession>,
    val config: ChargingConfigData,
    val socHistory: List<SocHistoryPoint>,
    val tariffsPayload: TariffsPayload? = null
)

data class ChargingSample(
    val t: Long,
    val powerKw: Double?,
    val soc: Double?,
    val temp: Double?,
    val tempHigh: Double?,
    val tempLow: Double?
)

data class ChargingConfigData(
    val enabled: Boolean = false,
    val electricityRate: Double = 0.0,
    val currency: String = "",
    val dcRate: Double = 0.0,
    val fastSampleSec: Int = 5
)

data class SocHistoryPoint(
    val timestamp: Long,
    val soc: Double,
    val charging: Boolean = false,
    val range: Double? = null,
    val soh: Double? = null,
    val powerKw: Double? = null
)

enum class PeriodFilter(val days: Int) {
    DAYS_7(7),
    DAYS_30(30),
    ALL_TIME(0)
}

enum class ChargingTab {
    SESSIONS,
    STATS,
    SETTINGS
}

data class ChargingUiState(
    val currentTab: ChargingTab = ChargingTab.SESSIONS,
    val periodFilter: PeriodFilter = PeriodFilter.DAYS_7,
    val summary: ChargingSummary = ChargingSummary(),
    val sessions: List<ChargingSession> = emptyList(),
    val selectedSession: ChargingSession? = null,
    val selectedSessionSamples: List<ChargingSample> = emptyList(),
    val socHistory: List<SocHistoryPoint> = emptyList(),
    val socHours: Int = 168,
    val config: ChargingConfigData = ChargingConfigData(),
    val tariffs: List<LocationTariff> = emptyList(),
    val defaultTariffId: String? = null,
    val matchedTariffId: String? = null,
    val currentGpsLat: Double? = null,
    val currentGpsLng: Double? = null,
    val isTariffEditorOpen: Boolean = false,
    val editingTariff: LocationTariff? = null,
    val tariffError: String? = null,
    val isTariffSaving: Boolean = false,
    val isLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val error: String? = null,
    val isDetailOpen: Boolean = false
)
