package com.overdrive.app.domain.diagnostics.dtc

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.diagnostics.EcuType
import com.overdrive.app.domain.repository.RepositoryProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class DtcDiagnosticServiceTest {

    private lateinit var service: DefaultDtcDiagnosticService

    @Before
    fun setUp() {
        RepositoryProvider.resetToDefaults()
        service = DefaultDtcDiagnosticService()
    }

    @After
    fun tearDown() {
        RepositoryProvider.resetToDefaults()
    }

    @Test
    fun testDtcDictionaryKnownCodesLookup() {
        val p0a1f = DtcDictionary.lookup("P0A1F")
        assertEquals("P0A1F", p0a1f.code)
        assertEquals(EcuType.BMS, p0a1f.ecu)
        assertEquals(DtcCategory.POWERTRAIN, p0a1f.category)
        assertEquals(DtcSeverity.CRITICAL, p0a1f.severity)
        assertTrue(p0a1f.titleTr.contains("Batarya Kontrol"))

        val u0100 = DtcDictionary.lookup("U0100")
        assertEquals(DtcCategory.NETWORK, u0100.category)
        assertEquals(DtcSeverity.CRITICAL, u0100.severity)

        val c1500 = DtcDictionary.lookup("c1500") // case-insensitive
        assertEquals("C1500", c1500.code)
        assertEquals(EcuType.TPMS, c1500.ecu)
        assertEquals(DtcCategory.CHASSIS, c1500.category)
    }

    @Test
    fun testDtcDictionaryUnknownCodeDynamicFallback() {
        val unknownP = DtcDictionary.lookup("P1999")
        assertEquals("P1999", unknownP.code)
        assertEquals(DtcCategory.POWERTRAIN, unknownP.category)
        assertTrue(unknownP.titleTr.contains("P1999"))

        val unknownB = DtcDictionary.lookup("B2888")
        assertEquals("B2888", unknownB.code)
        assertEquals(DtcCategory.BODY, unknownB.category)
        assertEquals(EcuType.BCM, unknownB.ecu)
    }

    @Test
    fun testScanDtcSynthesizesFromTelemetryAnomalies() {
        // Feed vehicle data with high cell imbalance (delta > 50mV) and low 12V (< 11.8V)
        val data = BydVehicleData.Builder()
            .highCellVoltage(3.35)
            .lowCellVoltage(3.25) // delta = 100 mV
            .voltage12v(11.4)      // low 12V
            .socPercent(50.0)
            .gearMode(1)           // P
            .tyrePressure(intArrayOf(170, 240, 240, 240)) // 1.7 bar low pressure
            .build()

        RepositoryProvider.updateFromVehicleData(data)

        val dtcList = service.scanDtcCodes()
        val codes = dtcList.map { it.code }

        assertTrue("Expected P0B24 (Cell imbalance) in scan", codes.contains("P0B24"))
        assertTrue("Expected P0562 (Low 12V) in scan", codes.contains("P0562"))
        assertTrue("Expected C1500 (TPMS low pressure) in scan", codes.contains("C1500"))
    }

    @Test
    fun testSafetyGuardBlocksClearWhileMoving() {
        // Vehicle is moving in DRIVE (gearMode = 4, speed = 50 km/h)
        val data = BydVehicleData.Builder()
            .gearMode(4)
            .speedKmh(50.0)
            .build()
        RepositoryProvider.updateFromVehicleData(data)

        assertFalse(service.isSafeToClear(50.0, 4))

        try {
            service.clearDtcCodes()
            fail("Expected IllegalStateException due to moving vehicle safety violation")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("Sürüş Güvenliği Engeli") == true)
        }
    }

    @Test
    fun testClearDtcSucceedsWhenStoppedInPark() {
        // Vehicle is safely in PARK (gearMode = 1, speed = 0)
        val data = BydVehicleData.Builder()
            .gearMode(1)
            .speedKmh(0.0)
            .highCellVoltage(3.35)
            .lowCellVoltage(3.25)
            .voltage12v(11.4)
            .build()
        RepositoryProvider.updateFromVehicleData(data)

        assertTrue(service.isSafeToClear(0.0, 1))

        // First scan to populate active codes
        val found = service.scanDtcCodes()
        assertTrue(found.isNotEmpty())

        // Clear all codes
        val result = service.clearDtcCodes()
        assertTrue(result.success)
        assertTrue(result.clearedCodesCount > 0)
    }

    @Test
    fun testSelectiveClearTargetEcu() {
        val data = BydVehicleData.Builder().gearMode(1).speedKmh(0.0).build()
        RepositoryProvider.updateFromVehicleData(data)

        // Inject faults
        service.injectFaultForTesting("P0A1F") // BMS
        service.injectFaultForTesting("C0035") // ESP

        // Clear only BMS
        val result = service.clearDtcCodes(targetEcu = EcuType.BMS)
        assertTrue(result.success)
        assertEquals(1, result.clearedCodesCount)
    }
}
