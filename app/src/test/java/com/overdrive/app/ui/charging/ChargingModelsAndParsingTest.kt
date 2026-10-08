package com.overdrive.app.ui.charging

import android.app.Application
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingModelsAndParsingTest {

    private class FakeApplication : Application()

    @Test
    fun testParseSocPointsWithExtendedTelemetry() {
        val repo = ChargingRepository()
        val jsonArray = JSONArray("""
            [
                {
                    "t": 1728345600000,
                    "soc": 65.5,
                    "charging": 1,
                    "range": 280.0,
                    "soh": 99.0,
                    "power": 45.2
                },
                {
                    "t": 1728349200000,
                    "soc": 82.0,
                    "charging": 0,
                    "range": 350.0,
                    "soh": 99.0
                }
            ]
        """.trimIndent())

        val points = repo.parseSocPoints(jsonArray)
        assertEquals(2, points.size)

        val p1 = points[0]
        assertEquals(1728345600000L, p1.timestamp)
        assertEquals(65.5, p1.soc, 0.001)
        assertTrue(p1.charging)
        assertEquals(280.0, p1.range!!, 0.001)
        assertEquals(99.0, p1.soh!!, 0.001)
        assertEquals(45.2, p1.powerKw!!, 0.001)

        val p2 = points[1]
        assertEquals(1728349200000L, p2.timestamp)
        assertEquals(82.0, p2.soc, 0.001)
        assertFalse(p2.charging)
        assertEquals(350.0, p2.range!!, 0.001)
        assertNull(p2.powerKw)
    }

    @Test
    fun testParseSessionsAndSummary() {
        val repo = ChargingRepository()
        val summaryJson = JSONObject("""
            {
                "periodSessions": 5,
                "periodEnergyKwh": 110.5,
                "periodCost": 220.0,
                "lifetimeSessions": 45,
                "lifetimeEnergyKwh": 1250.0,
                "lifetimeCost": 2500.0,
                "avgCostPerKwh": 2.0,
                "live": {
                    "charging": true,
                    "plugged": true,
                    "socPercent": 75.0,
                    "powerKw": 50.0,
                    "timeToFullMin": 30,
                    "sessionKwh": 15.0
                }
            }
        """.trimIndent())

        val summary = repo.parseSummary(summaryJson)
        assertEquals(5, summary.periodSessions)
        assertEquals(110.5, summary.periodEnergyKwh, 0.001)
        assertEquals(220.0, summary.periodCost, 0.001)
        assertEquals(45, summary.lifetimeSessions)
        assertTrue(summary.live.charging)
        assertTrue(summary.live.plugged)
        assertEquals(75.0, summary.live.socPercent, 0.001)
        assertEquals(50.0, summary.live.powerKw, 0.001)
        assertEquals(30, summary.live.timeToFullMin)
    }

    @Test
    fun testSocHistoryPointDefaults() {
        val pt = SocHistoryPoint(timestamp = 1000L, soc = 85.0)
        assertEquals(1000L, pt.timestamp)
        assertEquals(85.0, pt.soc, 0.001)
        assertFalse(pt.charging)
        assertNull(pt.range)
        assertNull(pt.soh)
        assertNull(pt.powerKw)
    }

    @Test
    fun testChargingUiStateDefaultSocHours() {
        val state = ChargingUiState()
        assertEquals(168, state.socHours)
        assertTrue(state.socHistory.isEmpty())
    }
}
