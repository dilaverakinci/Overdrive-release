package com.overdrive.app.ui.trips

/**
 * Filter scope for trips history.
 */
enum class TripsFilterPeriod {
    ALL,
    THIS_WEEK,
    THIS_MONTH
}

/**
 * Model representing a single summarized driving trip session.
 */
data class TripUiItem(
    val id: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val distanceKm: Float,
    val durationMinutes: Int,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Int,
    val socStart: Int,
    val socEnd: Int,
    val energyUsedKwh: Float,
    val efficiencyKwhPer100Km: Float,
    val tripCostFormatted: String? = null,
    val drivingDnaScore: Int = 90,
    val kinematicState: String = "Şehir İçi Akıcı", // "Otoyol Seyir", "Yoğun Trafik", etc.
)

/**
 * Complete UI state for TripsScreen (100% Jetpack Compose Native).
 */
data class TripsUiState(
    val filter: TripsFilterPeriod = TripsFilterPeriod.ALL,
    val totalDistanceKm: Float = 2480.5f,
    val totalTripsCount: Int = 84,
    val totalDurationHours: Float = 52.4f,
    val overallEfficiencyKwhPer100Km: Float = 16.2f,
    val overallDnaScore: Int = 91,
    val trips: List<TripUiItem> = listOf(
        TripUiItem(
            id = 101L,
            startTimeMs = System.currentTimeMillis() - 7200000L,
            endTimeMs = System.currentTimeMillis() - 3600000L,
            distanceKm = 42.6f,
            durationMinutes = 48,
            avgSpeedKmh = 53.2f,
            maxSpeedKmh = 118,
            socStart = 85,
            socEnd = 72,
            energyUsedKwh = 6.9f,
            efficiencyKwhPer100Km = 16.2f,
            tripCostFormatted = "₺16.50",
            drivingDnaScore = 94,
            kinematicState = "Otoyol Seyir",
        ),
        TripUiItem(
            id = 100L,
            startTimeMs = System.currentTimeMillis() - 86400000L,
            endTimeMs = System.currentTimeMillis() - 86400000L + 1800000L,
            distanceKm = 14.2f,
            durationMinutes = 26,
            avgSpeedKmh = 32.8f,
            maxSpeedKmh = 65,
            socStart = 72,
            socEnd = 68,
            energyUsedKwh = 2.4f,
            efficiencyKwhPer100Km = 16.9f,
            tripCostFormatted = "₺5.80",
            drivingDnaScore = 88,
            kinematicState = "Şehir İçi Akıcı",
        ),
        TripUiItem(
            id = 99L,
            startTimeMs = System.currentTimeMillis() - 86400000L * 2,
            endTimeMs = System.currentTimeMillis() - 86400000L * 2 + 3600000L,
            distanceKm = 18.5f,
            durationMinutes = 55,
            avgSpeedKmh = 20.1f,
            maxSpeedKmh = 52,
            socStart = 68,
            socEnd = 62,
            energyUsedKwh = 3.6f,
            efficiencyKwhPer100Km = 19.4f,
            tripCostFormatted = "₺8.60",
            drivingDnaScore = 82,
            kinematicState = "Yoğun Trafik",
        )
    ),
    val selectedTripForDetail: TripUiItem? = null,
)
