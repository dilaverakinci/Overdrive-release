package com.overdrive.app.byd.probes

import com.overdrive.app.byd.BydDataCollector
import com.overdrive.app.byd.BydVehicleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit test suite verifying Phase 2 Hardware Probes de-composition and BydDataCollector Facade.
 */
class BydModularProbesTest {

    @Test
    fun testBatteryProbeNullSafety() {
        val probe = BydBatteryProbe()
        assertNull(probe.energyDevice)
        assertNull(probe.chargingDevice)

        val builder = BydVehicleData.Builder()
        // Should execute cleanly without throwing NPE
        probe.collectBattery(builder, null, null)

        assertFalse(probe.isAcChargingCurrentLimitSupported(null))
        assertFalse(probe.setAcChargingCurrentLimit(null, 16))
    }

    @Test
    fun testDrivetrainProbeNullSafety() {
        val probe = BydDrivetrainProbe()
        assertNull(probe.speedDevice)
        assertNull(probe.engineDevice)
        assertNull(probe.gearboxDevice)
        assertNull(probe.statisticDevice)

        val builder = BydVehicleData.Builder()
        probe.collectDrivetrain(builder, null, null, null)
    }

    @Test
    fun testClimateProbeNullSafety() {
        val probe = BydClimateProbe()
        assertNull(probe.acDevice)
        assertNull(probe.pm25Device)

        val builder = BydVehicleData.Builder()
        probe.collectClimate(builder, null, null)

        assertFalse(probe.setAcPower(null, true))
        assertFalse(probe.setTemperature(null, 1, 23.5))
        assertFalse(probe.setFanLevel(null, 3))
    }

    @Test
    fun testBodyworkProbePadRotationValidation() {
        val probe = BydBodyworkProbe()
        assertEquals(1, BydBodyworkProbe.PAD_ROTATION_HORIZONTAL)
        assertEquals(2, BydBodyworkProbe.PAD_ROTATION_VERTICAL)

        // Invalid rotation values should be immediately rejected before touching hardware
        assertFalse(probe.setPadRotation(null, 0))
        assertFalse(probe.setPadRotation(null, 3))
        assertFalse(probe.setPadRotation(null, -1))

        // Null setting device with valid rotation should return false safely
        assertFalse(probe.setPadRotation(null, BydBodyworkProbe.PAD_ROTATION_HORIZONTAL))
        assertFalse(probe.setPadRotation(null, BydBodyworkProbe.PAD_ROTATION_VERTICAL))
    }

    @Test
    fun testChassisProbeNullSafety() {
        val probe = BydChassisProbe()
        assertNull(probe.tyreDevice)
        assertNull(probe.sensorDevice)
        assertNull(probe.radarDevice)

        val builder = BydVehicleData.Builder()
        probe.collectSensor(builder, null)
        probe.collectTyres(builder, null)
    }

    @Test
    fun testBydDataCollectorFacadeExposesProbes() {
        val collector = BydDataCollector.getInstance()
        assertNotNull(collector)
        assertNotNull(collector.batteryProbe)
        assertNotNull(collector.drivetrainProbe)
        assertNotNull(collector.climateProbe)
        assertNotNull(collector.bodyworkProbe)
        assertNotNull(collector.chassisProbe)
    }
}
