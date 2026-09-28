package com.overdrive.app.ui.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying state mutations, actions, and safety invariants for Trips Compose Native.
 */
class TripsComposeParityTest {

    @Test
    fun initialState_hasDefaultTripsState() {
        val state = TripsUiState()
        assertEquals(TripsFilterPeriod.ALL, state.filter)
        assertEquals(84, state.totalTripsCount)
        assertEquals(3, state.trips.size)
        assertTrue(state.totalDistanceKm > 2000f)
        assertEquals(91, state.overallDnaScore)
        assertNull(state.selectedTripForDetail)
        assertEquals(0, state.scrubberIndex)
        assertFalse(state.isPlaying)
    }

    @Test
    fun filterSelection_updatesActiveFilter() {
        val state = TripsUiState()
        val next = state.copy(filter = TripsFilterPeriod.THIS_WEEK)
        assertEquals(TripsFilterPeriod.THIS_WEEK, next.filter)
    }

    @Test
    fun tripItem_hasValidEfficiencyAndSpeed() {
        val state = TripsUiState()
        val firstTrip = state.trips.first()
        assertEquals(101L, firstTrip.id)
        assertEquals(42.6f, firstTrip.distanceKm, 0.01f)
        assertEquals(94, firstTrip.drivingDnaScore)
        assertEquals("Otoyol Seyir", firstTrip.kinematicState)
    }

    @Test
    fun tripItem_hasTelemetryPointsWithValidCoordinates() {
        val state = TripsUiState()
        val firstTrip = state.trips.first()
        assertTrue(firstTrip.telemetryPoints.isNotEmpty())
        assertEquals(60, firstTrip.telemetryPoints.size)

        val firstPoint = firstTrip.telemetryPoints.first()
        assertTrue(firstPoint.lat > 0.0)
        assertTrue(firstPoint.lon > 0.0)
        assertTrue(firstPoint.headingDegrees in 0f..360f)
        assertEquals(0, firstPoint.elapsedSeconds)
    }

    @Test
    fun selectTripForDetail_updatesSelectedTripAndScrubber() {
        val state = TripsUiState()
        val targetTrip = state.trips.first()
        val detailState = state.copy(
            selectedTripForDetail = targetTrip,
            scrubberIndex = 0,
            isPlaying = false
        )
        assertNotNull(detailState.selectedTripForDetail)
        assertEquals(101L, detailState.selectedTripForDetail?.id)
        assertEquals(0, detailState.scrubberIndex)

        // Scrubbing
        val scrubbedState = detailState.copy(scrubberIndex = 25)
        assertEquals(25, scrubbedState.scrubberIndex)

        // Toggle playback
        val playingState = scrubbedState.copy(isPlaying = true)
        assertTrue(playingState.isPlaying)

        // Back to list
        val listState = playingState.copy(selectedTripForDetail = null, isPlaying = false)
        assertNull(listState.selectedTripForDetail)
        assertFalse(listState.isPlaying)
    }

    @Test
    fun fromTrips_calculatesAggregatedMetrics() {
        val testTrips = listOf(
            TripUiItem(
                id = 100L,
                startTimeMs = 1000L,
                endTimeMs = 2000L,
                distanceKm = 10f,
                durationMinutes = 20,
                avgSpeedKmh = 30f,
                maxSpeedKmh = 60,
                socStart = 80,
                socEnd = 75,
                energyUsedKwh = 1.6f,
                efficiencyKwhPer100Km = 16.0f,
                drivingDnaScore = 90
            ),
            TripUiItem(
                id = 99L,
                startTimeMs = 3000L,
                endTimeMs = 4000L,
                distanceKm = 20f,
                durationMinutes = 40,
                avgSpeedKmh = 30f,
                maxSpeedKmh = 70,
                socStart = 75,
                socEnd = 70,
                energyUsedKwh = 3.2f,
                efficiencyKwhPer100Km = 16.0f,
                drivingDnaScore = 94
            )
        )
        val state = TripsUiState.fromTrips(testTrips)
        assertEquals(2, state.totalTripsCount)
        assertEquals(30f, state.totalDistanceKm, 0.1f)
        assertEquals(1.0f, state.totalDurationHours, 0.1f)
        assertEquals(92, state.overallDnaScore)
        assertEquals(16.0f, state.overallEfficiencyKwhPer100Km, 0.5f)
        assertEquals(2, state.trips.size)
    }
}
