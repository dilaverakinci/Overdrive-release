package com.overdrive.app.ui.parking

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ParkingViewModelTest {

    private lateinit var viewModel: ParkingViewModel

    @Before
    fun setup() {
        val repo = ParkingRepository()
        viewModel = ParkingViewModel(repo)
    }

    @Test
    fun defaultSelectedDaysIs30() {
        assertEquals(30, viewModel.selectedDays.value)
    }

    @Test
    fun initialStatusIsNull() {
        assertNull(viewModel.status.value)
    }

    @Test
    fun initialSessionsIsEmpty() {
        assertTrue(viewModel.sessions.value.isEmpty())
    }

    @Test
    fun initialLoadingIsFalse() {
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun parkingSessionJsonParsing() {
        val json = JSONObject().apply {
            put("id", "test-session-123")
            put("start", 1700000000000L)
            put("end", 1700003600000L)
            put("duration", 3600000L)
            put("safeZone", "Home Garage")
            put("eventCount", 3)
            put("neighbourCount", 2)
            put("energyUsedKwh", 1.25)
            put("socStart", 80)
            put("socEnd", 78)

            val signage = JSONObject().apply {
                put("found", true)
                put("label", "Floor B2 - Slot 14")
            }
            put("signage", signage)

            val gps = JSONObject().apply {
                put("lat", 41.0082)
                put("lng", 28.9784)
                put("quality", "EXCELLENT")
            }
            put("gps", gps)
        }

        val session = ParkingSession.fromJson(json, isCurrent = false)

        assertEquals("test-session-123", session.id)
        assertEquals(1700000000000L, session.start)
        assertEquals(1700003600000L, session.end)
        assertEquals(3600000L, session.durationMs)
        assertEquals("Home Garage", session.place)
        assertEquals("Floor B2 - Slot 14", session.signageLabel)
        assertEquals("EXCELLENT", session.gpsQuality)
        assertEquals(3, session.eventsCount)
        assertEquals(2, session.neighboursCount)
        assertEquals(1.25, session.energyUsedKwh ?: 0.0, 0.001)
        assertEquals(80, session.socStart)
        assertEquals(78, session.socEnd)
        assertFalse(session.isLive)
    }

    @Test
    fun parkingConfigDefaults() {
        val config = ParkingConfig()
        assertFalse(config.enabled)
        assertEquals("return", config.endTrigger)
        assertTrue(config.snapshots)
        assertTrue(config.neighbours)
        assertTrue(config.signage)
        assertEquals(30, config.retentionDays)
    }
}
