package com.overdrive.app.charging

import com.overdrive.app.charging.station.EvStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class EvStationRepositoryTest {

    @Test
    fun testEvStationComputedProperties() {
        val dcStation = EvStation(
            id = "test_1",
            operator = "Trugo",
            name = "Bolu Highway Outlet",
            city = "Bolu",
            district = "Merkez",
            address = "D-100 Karayolu",
            latitude = 40.73,
            longitude = 31.60,
            chargingType = "DC",
            maxPowerKw = 180.0,
            socketCount = 4,
            acPrice = 6.50,
            dcPrice = 8.90,
            isCustom = false
        )

        assertTrue(dcStation.isDc)
        assertEquals("Trugo - Bolu Highway Outlet", dcStation.displayTitle)
        assertEquals(8.90, dcStation.bestPricePerKwh, 0.01)

        val customAcStation = EvStation(
            id = "custom_1",
            operator = "Kişisel",
            name = "Evim Wallbox",
            city = "Ankara",
            district = "Çankaya",
            address = "Ev",
            latitude = 39.92,
            longitude = 32.85,
            chargingType = "AC",
            maxPowerKw = 11.0,
            socketCount = 1,
            acPrice = 2.60,
            dcPrice = 0.0,
            isCustom = true
        )

        assertFalse(customAcStation.isDc)
        assertEquals("⭐ Evim Wallbox", customAcStation.displayTitle)
        assertEquals(2.60, customAcStation.bestPricePerKwh, 0.01)
    }

    @Test
    fun testHaversineDistanceCalculation() {
        // Istanbul (41.0082, 28.9784) to Ankara (39.9334, 32.8597)
        val lat1 = 41.0082
        val lon1 = 28.9784
        val lat2 = 39.9334
        val lon2 = 32.8597

        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = r * c

        // Great-circle distance Istanbul-Ankara is ~350 km
        assertTrue("Distance should be around 350 km: $distance", abs(distance - 350.0) < 20.0)
    }

    @Test
    fun testEvStationsAssetDatabaseExists() {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val dbFile = if (File(userDir, "app/src/main/assets/ev_stations.db").exists()) {
            File(userDir, "app/src/main/assets/ev_stations.db")
        } else {
            File(userDir, "src/main/assets/ev_stations.db")
        }

        assertTrue("ev_stations.db asset must exist", dbFile.exists())
        assertTrue("ev_stations.db asset must be > 9MB", dbFile.length() > 9_000_000L)
    }
}
