package com.overdrive.app.domain.diagnostics

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.repository.RepositoryProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosticsDomainTest {

    @Before
    fun setUp() {
        RepositoryProvider.resetToDefaults()
    }

    @After
    fun tearDown() {
        RepositoryProvider.resetToDefaults()
    }

    @Test
    fun testAllNineEcusAreDefinedWithValidProperties() {
        val ecus = EcuType.values()
        assertEquals(9, ecus.size)
        for (ecu in ecus) {
            assertTrue(ecu.titleTr.isNotBlank())
            assertTrue(ecu.titleEn.isNotBlank())
            assertTrue(ecu.dtcPrefix in listOf('P', 'C', 'B', 'U'))
            assertTrue(ecu.displayName.startsWith(ecu.name))
        }
    }

    @Test
    fun testEcuHealthStatusSeverityLogic() {
        assertTrue(EcuHealthStatus.NORMAL.isHealthy)
        assertFalse(EcuHealthStatus.WARNING.isHealthy)
        assertFalse(EcuHealthStatus.FAULT.isHealthy)
        assertFalse(EcuHealthStatus.OFFLINE.isHealthy)

        assertTrue(EcuHealthStatus.FAULT.isCritical)
        assertFalse(EcuHealthStatus.WARNING.isCritical)
        assertFalse(EcuHealthStatus.NORMAL.isCritical)
    }

    @Test
    fun testDefaultDiagnosticsRepositoryInitialStateIsOffline() {
        val repo = DefaultDiagnosticsRepository()
        assertEquals(EcuHealthStatus.OFFLINE, repo.overallHealth.value)
        assertEquals(EcuHealthStatus.OFFLINE, repo.bmsTelemetry.value.status)
        assertEquals(EcuHealthStatus.OFFLINE, repo.mcuTelemetry.value.status)
        assertEquals(EcuHealthStatus.OFFLINE, repo.tpmsTelemetry.value.status)
    }

    @Test
    fun testHealthyVehicleProducesNormalSnapshotsAndDeltaCalculation() {
        val repo = DefaultDiagnosticsRepository()
        val data = BydVehicleData.Builder()
            .highCellVoltage(3.28)
            .lowCellVoltage(3.25)
            .highCellTempC(28.0)
            .lowCellTempC(26.0)
            .avgCellTempC(27.0)
            .socPercent(80.0)
            .sohPercent(99.0)
            .hvPackVoltage(400.0)
            .voltage12v(13.4)
            .frontMotorSpeed(3200)
            .frontMotorTorque(150.0)
            .gearMode(4) // 4 = D
            .speedKmh(65.0)
            .tyrePressure(intArrayOf(240, 240, 240, 240)) // 240 kPa = 2.4 Bar
            .build()

        repo.updateFromSnapshot(data)

        val bms = repo.bmsTelemetry.value
        assertEquals(30, bms.cellVoltageDeltaMv)
        assertFalse(bms.isCellImbalanceWarning)
        assertEquals(EcuHealthStatus.NORMAL, bms.status)

        val mcu = repo.mcuTelemetry.value
        assertEquals(3200, mcu.frontMotorRpm)
        assertEquals(150.0, mcu.frontMotorTorqueNm, 0.01)
        assertEquals(EcuHealthStatus.NORMAL, mcu.status)

        val tpms = repo.tpmsTelemetry.value
        assertEquals(2.4, tpms.flPressureBar, 0.01)
        assertFalse(tpms.hasPressureWarning)
        assertEquals(EcuHealthStatus.NORMAL, tpms.status)

        val snapshots = repo.ecuSnapshots.value
        assertEquals(9, snapshots.size)
        assertEquals(EcuHealthStatus.NORMAL, snapshots[EcuType.BMS]?.status)
        assertEquals(EcuHealthStatus.NORMAL, snapshots[EcuType.MCU]?.status)
        assertEquals(EcuHealthStatus.NORMAL, snapshots[EcuType.TPMS]?.status)

        assertEquals(EcuHealthStatus.NORMAL, repo.overallHealth.value)
    }

    @Test
    fun testBmsImbalanceTriggersWarningAndOverallWarning() {
        val repo = DefaultDiagnosticsRepository()
        // 80 mV difference between high and low cell (> 50 mV threshold)
        val data = BydVehicleData.Builder()
            .highCellVoltage(3.32)
            .lowCellVoltage(3.24)
            .socPercent(65.0)
            .voltage12v(13.2)
            .gearMode(1) // P
            .build()

        repo.updateFromSnapshot(data)

        val bms = repo.bmsTelemetry.value
        assertEquals(80, bms.cellVoltageDeltaMv)
        assertTrue(bms.isCellImbalanceWarning)
        assertEquals(EcuHealthStatus.WARNING, bms.status)
        assertEquals(EcuHealthStatus.WARNING, repo.overallHealth.value)

        val bmsSnapshot = repo.ecuSnapshots.value[EcuType.BMS]
        assertNotNull(bmsSnapshot)
        assertEquals(EcuHealthStatus.WARNING, bmsSnapshot?.status)
        assertTrue(bmsSnapshot?.errorNotes?.contains("dengesizliği") == true)
    }

    @Test
    fun testTpmsLowPressureTriggersWarning() {
        val repo = DefaultDiagnosticsRepository()
        val data = BydVehicleData.Builder()
            .socPercent(70.0)
            .highCellVoltage(3.25)
            .lowCellVoltage(3.24)
            .voltage12v(13.2)
            .gearMode(1)
            .tyrePressure(intArrayOf(180, 240, 240, 240)) // 180 kPa = 1.8 Bar (< 2.0 Bar)
            .build()

        repo.updateFromSnapshot(data)

        val tpms = repo.tpmsTelemetry.value
        assertTrue(tpms.hasPressureWarning)
        assertEquals(EcuHealthStatus.WARNING, tpms.status)
        assertEquals(EcuHealthStatus.WARNING, repo.overallHealth.value)
    }

    @Test
    fun testRepositoryProviderIntegratesDiagnosticsRepository() {
        val data = BydVehicleData.Builder()
            .highCellVoltage(3.28)
            .lowCellVoltage(3.26)
            .socPercent(90.0)
            .voltage12v(13.8)
            .gearMode(4)
            .build()

        RepositoryProvider.updateFromVehicleData(data)

        val repo = RepositoryProvider.diagnosticsRepository
        assertNotNull(repo)
        val bms = repo.bmsTelemetry.value
        assertEquals(20, bms.cellVoltageDeltaMv)
        assertEquals(EcuHealthStatus.NORMAL, bms.status)
    }
}
