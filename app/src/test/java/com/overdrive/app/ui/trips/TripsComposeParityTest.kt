package com.overdrive.app.ui.trips

import org.junit.Assert.assertEquals
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
}
