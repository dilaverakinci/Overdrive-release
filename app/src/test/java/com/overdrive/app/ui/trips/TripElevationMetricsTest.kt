package com.overdrive.app.ui.trips

import org.junit.Assert.*
import org.junit.Test

class TripElevationMetricsTest {

    @Test
    fun testElevationProfileMetricsCalculations() {
        // Test basic uphill, downhill and flat distance calculation logic
        val metrics = ElevationProfileMetrics(
            minAltitudeM = 770.0,
            maxAltitudeM = 800.0,
            totalGainM = 35.0,
            totalLossM = 45.0,
            uphillDistanceKm = 1.5,
            downhillDistanceKm = 2.0,
            flatDistanceKm = 0.5,
            totalDistanceKm = 4.0
        )

        assertEquals(770.0, metrics.minAltitudeM, 0.01)
        assertEquals(800.0, metrics.maxAltitudeM, 0.01)
        assertEquals(35.0, metrics.totalGainM, 0.01)
        assertEquals(45.0, metrics.totalLossM, 0.01)

        // Uphill: 1.5 / 4.0 = 37.5% -> 38%
        assertEquals(38, metrics.uphillPercent)
        // Downhill: 2.0 / 4.0 = 50%
        assertEquals(50, metrics.downhillPercent)
        // Flat: 0.5 / 4.0 = 12.5% -> 13%
        assertEquals(13, metrics.flatPercent)
    }

    @Test
    fun testZeroDistanceMetricsSafePercentages() {
        val emptyMetrics = ElevationProfileMetrics(
            minAltitudeM = 0.0,
            maxAltitudeM = 0.0,
            totalGainM = 0.0,
            totalLossM = 0.0,
            uphillDistanceKm = 0.0,
            downhillDistanceKm = 0.0,
            flatDistanceKm = 0.0,
            totalDistanceKm = 0.0
        )

        assertEquals(0, emptyMetrics.uphillPercent)
        assertEquals(0, emptyMetrics.downhillPercent)
        assertEquals(0, emptyMetrics.flatPercent)
    }
}
