package com.overdrive.app.roadsense.store

import com.overdrive.app.database.OverdriveSqliteMaster
import com.overdrive.app.roadsense.detect.HazardType
import com.overdrive.app.roadsense.detect.RoadSenseHazard
import com.overdrive.app.roadsense.detect.Severity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoadSenseStoreTest {

    private lateinit var master: OverdriveSqliteMaster
    private lateinit var store: RoadSenseStore

    @Before
    fun setUp() {
        master = OverdriveSqliteMaster.useInMemoryForTesting()
        store = RoadSenseStore(master)
        store.init()
    }

    @After
    fun tearDown() {
        store.stop()
        master.close()
    }

    @Test
    fun testUpsertDetectionAndMerge() {
        val now = 1700000000000L
        val hazard1 = RoadSenseHazard(
            lat = 40.7128,
            lng = -74.0060,
            type = HazardType.POTHOLE,
            severity = Severity.MINOR,
            headingDeg = 90.0f,
            confidence = 0.6f,
            speedKmh = 45.0f,
            aVertPeak = 1.2f,
            tMs = now
        )

        val id1 = store.upsertDetection(hazard1, now)
        assertTrue(id1.isNotEmpty())

        val ahead1 = store.queryAhead(40.7128, -74.0060, 90.0, 10)
        assertEquals(1, ahead1.size)
        assertEquals(id1, ahead1[0].id)
        assertEquals(0, ahead1[0].status)
        assertEquals(1, ahead1[0].observations)
        assertEquals(Severity.MINOR, ahead1[0].hazard.severity)

        // Merge second detection at virtually the same spot (2 meters away) with higher severity
        val now2 = now + 10_000L
        val hazard2 = RoadSenseHazard(
            lat = 40.71281,
            lng = -74.00601,
            type = HazardType.POTHOLE,
            severity = Severity.SEVERE,
            headingDeg = 92.0f,
            confidence = 0.85f,
            speedKmh = 40.0f,
            aVertPeak = 2.4f,
            tMs = now2
        )

        val id2 = store.upsertDetection(hazard2, now2)
        assertEquals(id1, id2) // Merged into same id

        val ahead2 = store.queryAhead(40.7128, -74.0060, 90.0, 10)
        assertEquals(1, ahead2.size)
        assertEquals(1, ahead2[0].status) // Promoted to locally confirmed at K=2
        assertEquals(2, ahead2[0].observations)
        assertEquals(Severity.SEVERE, ahead2[0].hazard.severity)
        assertEquals(0.85f, ahead2[0].hazard.confidence, 1e-4f)

        // Test Bbox query
        val inBbox = store.queryByBbox(40.0, -75.0, 41.0, -73.0, 10)
        assertEquals(1, inBbox.size)

        // Mark human verified
        store.markHumanVerified(id1, true, Severity.SEVERE.level, HazardType.POTHOLE.ordinal, now2 + 5000L)
        val verified = store.queryAhead(40.7128, -74.0060, 90.0, 10)
        assertTrue(verified[0].humanVerified)

        // Delete all
        val deleted = store.deleteAllLocal()
        assertEquals(1L, deleted)
        assertEquals(0, store.queryAhead(40.7128, -74.0060, 90.0, 10).size)
    }
}
