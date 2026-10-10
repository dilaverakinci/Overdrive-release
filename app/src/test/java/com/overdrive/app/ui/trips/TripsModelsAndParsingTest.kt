package com.overdrive.app.ui.trips

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripsModelsAndParsingTest {

    @Test
    fun testTripRecordParsing() {
        val jsonStr = """
            {
                "id": 12345,
                "startTime": 1728345600000,
                "endTime": 1728349200000,
                "durationSeconds": 3600,
                "distanceKm": 42.5,
                "energyUsedKwh": 7.8,
                "tripCost": 15.60,
                "overallScore": 92,
                "kinematicState": "CITY_COMMUTE",
                "avgSpeedKmh": 42.5,
                "maxSpeedKmh": 85,
                "efficiencyScore": 95,
                "anticipationScore": 90,
                "smoothnessScore": 93,
                "speedDisciplineScore": 88,
                "consistencyScore": 91,
                "isPhev": false
            }
        """.trimIndent()

        val item = TripRecordItem.fromJson(JSONObject(jsonStr))
        assertEquals(12345L, item.id)
        assertEquals(1728345600000L, item.startTime)
        assertEquals(1728349200000L, item.endTime)
        assertEquals(3600, item.durationSeconds)
        assertEquals(42.5, item.distanceKm, 0.001)
        assertEquals(7.8, item.energyUsedKwh, 0.001)
        assertEquals(15.60, item.tripCost, 0.001)
        assertEquals(92, item.overallScore)
        assertEquals("CITY_COMMUTE", item.kinematicState)
        assertEquals(42.5, item.avgSpeedKmh, 0.001)
        assertEquals(85, item.maxSpeedKmh)
        assertEquals(95, item.efficiencyScore)
        assertEquals(false, item.isPhev)
        assertEquals(18.35, item.consumptionKwhPer100Km, 0.01)
        assertEquals(5.45, item.efficiencyKmPerKwh, 0.01)
    }

    @Test
    fun testDnaScoresParsing() {
        val jsonStr = """
            {
                "efficiency": 92,
                "anticipation": 85,
                "smoothness": 87,
                "speedDiscipline": 84,
                "consistency": 91,
                "overall": 88
            }
        """.trimIndent()

        val item = DnaScoresItem.fromJson(JSONObject(jsonStr))
        assertEquals(92, item.efficiency)
        assertEquals(85, item.anticipation)
        assertEquals(87, item.smoothness)
        assertEquals(84, item.speedDiscipline)
        assertEquals(91, item.consistency)
        assertEquals(88, item.overall)
    }

    @Test
    fun testRangeEstimateParsing() {
        val jsonStr = """
            {
                "predictedRangeKm": 385.0,
                "lowerBoundKm": 360.0,
                "upperBoundKm": 410.0,
                "bucketKey": "temp_20_25",
                "sampleCount": 15,
                "builtInRangeKm": 420
            }
        """.trimIndent()

        val item = RangeEstimateItem.fromJson(JSONObject(jsonStr))
        assertEquals(385.0, item.predictedRangeKm, 0.001)
        assertEquals(360.0, item.lowerBoundKm, 0.001)
        assertEquals(410.0, item.upperBoundKm, 0.001)
        assertEquals("temp_20_25", item.bucketKey)
        assertEquals(15, item.sampleCount)
        assertEquals(420, item.builtInRangeKm)
    }

    @Test
    fun testTripConfigParsing() {
        val jsonStr = """
            {
                "enabled": true,
                "electricityRate": 3.25,
                "currency": "TRY",
                "distanceUnit": "km",
                "fuelPricePerL": 42.50
            }
        """.trimIndent()

        val item = TripConfigItem.fromJson(JSONObject(jsonStr))
        assertEquals(true, item.enabled)
        assertEquals(3.25, item.electricityRate, 0.001)
        assertEquals("TRY", item.currency)
        assertEquals("km", item.distanceUnit)
        assertEquals(42.50, item.fuelPricePerL, 0.001)
    }

    @Test
    fun testTripStorageParsing() {
        val jsonStr = """
            {
                "storageType": "INTERNAL",
                "limitMb": 2048,
                "usedMb": 128.5,
                "tripsCount": 42,
                "storagePath": "/data/user/0/com.overdrive.app/files/trips"
            }
        """.trimIndent()

        val item = TripStorageItem.fromJson(JSONObject(jsonStr))
        assertEquals("INTERNAL", item.storageType)
        assertEquals(2048L, item.limitMb)
        assertEquals(128.5, item.usedMb, 0.001)
        assertEquals(42, item.tripsCount)
        assertEquals("/data/user/0/com.overdrive.app/files/trips", item.storagePath)
    }
}
