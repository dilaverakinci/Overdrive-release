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

/**
 * Complete UI state for TripsScreen (100% Jetpack Compose Native).
 */
data class TripsUiState(
    val filter: TripsFilterPeriod = TripsFilterPeriod.ALL,
    val totalDistanceKm: Float = 0f,
    val totalTripsCount: Int = 0,
    val totalDurationHours: Float = 0f,
    val overallEfficiencyKwhPer100Km: Float = 0f,
    val overallDnaScore: Int = 0,
    val trips: List<TripUiItem> = emptyList(),
    val selectedTripForDetail: TripUiItem? = null,
    val scrubberIndex: Int = 0,
    val isPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isTableView: Boolean = true,
    val tripToDelete: TripUiItem? = null
) {
    companion object {
        fun fromTrips(trips: List<TripUiItem>, isTableView: Boolean = true): TripsUiState {
            if (trips.isEmpty()) return TripsUiState(isTableView = isTableView)
            val totalDist = trips.sumOf { it.distanceKm.toDouble() }.toFloat()
            val totalDurHours = trips.sumOf { it.durationMinutes.toDouble() / 60.0 }.toFloat()
            val totalEnergy = trips.sumOf { it.energyUsedKwh.toDouble() }.toFloat()
            val avgEff = if (totalDist > 0.1f) ((totalEnergy / totalDist) * 100f) else 0f
            val avgDna = trips.sumOf { it.drivingDnaScore } / trips.size
            return TripsUiState(
                totalDistanceKm = String.format(Locale.US, "%.1f", totalDist).toFloatOrNull() ?: totalDist,
                totalTripsCount = trips.size,
                totalDurationHours = String.format(Locale.US, "%.1f", totalDurHours).toFloatOrNull() ?: totalDurHours,
                overallEfficiencyKwhPer100Km = String.format(Locale.US, "%.1f", avgEff).toFloatOrNull() ?: avgEff,
                overallDnaScore = avgDna,
                trips = trips,
                isTableView = isTableView
            )
        }
    }
}
