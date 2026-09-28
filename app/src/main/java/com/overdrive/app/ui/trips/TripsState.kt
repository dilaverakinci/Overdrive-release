package com.overdrive.app.ui.trips

import java.util.Locale

/**
 * Filter scope for trips history.
 */
enum class TripsFilterPeriod {
    ALL,
    THIS_WEEK,
    THIS_MONTH
}

/**
 * High-resolution telemetry sample along a driving route.
 */
data class TripTelemetryPoint(
    val timestampMs: Long,
    val elapsedSeconds: Int,
    val lat: Double,
    val lon: Double,
    val speedKmh: Int,
    val accelPedalPercent: Int,
    val brakePedalPercent: Int,
    val socPercent: Float,
    val powerKw: Float = 0f,
    val altitudeM: Double = 0.0,
    val headingDegrees: Float = 0f
)

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
    val kinematicState: String = "Şehir İçi Akıcı",
    val anticipationScore: Int = 92,
    val smoothnessScore: Int = 90,
    val speedDisciplineScore: Int = 95,
    val efficiencyScore: Int = 89,
    val consistencyScore: Int = 93,
    val elevationGainM: Int = 145,
    val elevationLossM: Int = 130,
    val telemetryPoints: List<TripTelemetryPoint> = emptyList()
)

/**
 * Synthesize realistic GPS & vehicle dynamics telemetry points along a route.
 */
fun generateMockTelemetryPoints(
    tripId: Long,
    durationMinutes: Int,
    maxSpeedKmh: Int,
    socStart: Int,
    socEnd: Int
): List<TripTelemetryPoint> {
    val sampleCount = 60
    val baseLat = 40.9850 + (tripId % 7) * 0.015
    val baseLon = 29.0400 + (tripId % 7) * 0.018
    val points = ArrayList<TripTelemetryPoint>(sampleCount)
    val totalSeconds = durationMinutes * 60

    var currentLat = baseLat
    var currentLon = baseLon
    var heading = 40f

    for (i in 0 until sampleCount) {
        val fraction = i.toFloat() / (sampleCount - 1).coerceAtLeast(1)
        val elapsedSec = (fraction * totalSeconds).toInt()
        val soc = socStart - (socStart - socEnd) * fraction

        // Smooth curve calculation
        val turnAngle = (Math.sin(fraction.toDouble() * Math.PI * 3.5) * 35.0).toFloat()
        heading = (40f + fraction * 75f + turnAngle + 360f) % 360f

        val stepDist = 0.0016
        val rad = Math.toRadians(heading.toDouble())
        currentLat += Math.cos(rad) * stepDist
        currentLon += Math.sin(rad) * stepDist

        // Realistic automotive speed curve
        val speedFactor = when {
            fraction < 0.08f -> fraction / 0.08f
            fraction > 0.92f -> (1f - fraction) / 0.08f
            fraction in 0.42f..0.50f -> 0.35f
            else -> (0.65f + 0.35f * Math.sin(fraction.toDouble() * Math.PI * 5.0)).toFloat()
        }
        val speed = (maxSpeedKmh * speedFactor).coerceIn(0f, maxSpeedKmh.toFloat()).toInt()

        val accel = if (speedFactor > 0.7f && fraction < 0.85f) {
            ((speedFactor - 0.6f) * 120f).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val brake = if (fraction > 0.90f || (fraction in 0.40f..0.45f)) 40 else 0
        val power = if (brake > 0) -22.0f else (speed * 0.42f)
        val altitude = 35.0 + Math.sin(fraction.toDouble() * Math.PI * 2.0) * 55.0

        points.add(
            TripTelemetryPoint(
                timestampMs = System.currentTimeMillis() - (totalSeconds - elapsedSec) * 1000L,
                elapsedSeconds = elapsedSec,
                lat = currentLat,
                lon = currentLon,
                speedKmh = speed,
                accelPedalPercent = accel,
                brakePedalPercent = brake,
                socPercent = soc,
                powerKw = power,
                altitudeM = altitude,
                headingDegrees = heading
            )
        )
    }
    return points
}

private val defaultTrip101 = TripUiItem(
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
    anticipationScore = 95,
    smoothnessScore = 92,
    speedDisciplineScore = 96,
    efficiencyScore = 93,
    consistencyScore = 94,
    elevationGainM = 165,
    elevationLossM = 140,
    telemetryPoints = generateMockTelemetryPoints(101L, 48, 118, 85, 72)
)

private val defaultTrip100 = TripUiItem(
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
    anticipationScore = 89,
    smoothnessScore = 87,
    speedDisciplineScore = 91,
    efficiencyScore = 86,
    consistencyScore = 87,
    elevationGainM = 65,
    elevationLossM = 70,
    telemetryPoints = generateMockTelemetryPoints(100L, 26, 65, 72, 68)
)

private val defaultTrip99 = TripUiItem(
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
    anticipationScore = 80,
    smoothnessScore = 81,
    speedDisciplineScore = 85,
    efficiencyScore = 82,
    consistencyScore = 82,
    elevationGainM = 90,
    elevationLossM = 85,
    telemetryPoints = generateMockTelemetryPoints(99L, 55, 52, 68, 62)
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
    val trips: List<TripUiItem> = listOf(defaultTrip101, defaultTrip100, defaultTrip99),
    val selectedTripForDetail: TripUiItem? = null,
    val scrubberIndex: Int = 0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f
) {
    companion object {
        fun fromTrips(trips: List<TripUiItem>): TripsUiState {
            if (trips.isEmpty()) return TripsUiState()
            val totalDist = trips.sumOf { it.distanceKm.toDouble() }.toFloat()
            val totalDurHours = trips.sumOf { it.durationMinutes.toDouble() / 60.0 }.toFloat()
            val totalEnergy = trips.sumOf { it.energyUsedKwh.toDouble() }.toFloat()
            val avgEff = if (totalDist > 0.1f) ((totalEnergy / totalDist) * 100f) else 16.2f
            val avgDna = trips.sumOf { it.drivingDnaScore } / trips.size
            return TripsUiState(
                totalDistanceKm = String.format(Locale.US, "%.1f", totalDist).toFloatOrNull() ?: totalDist,
                totalTripsCount = trips.size,
                totalDurationHours = String.format(Locale.US, "%.1f", totalDurHours).toFloatOrNull() ?: totalDurHours,
                overallEfficiencyKwhPer100Km = String.format(Locale.US, "%.1f", avgEff).toFloatOrNull() ?: avgEff,
                overallDnaScore = avgDna,
                trips = trips
            )
        }
    }
}
