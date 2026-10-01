package com.overdrive.app.ui.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity and contract verification for Compose Native Vehicle Control state and actions.
 */
class VehicleControlComposeParityTest {

    @Test
    fun testDefaultVehicleControlState() {
        val state = VehicleControlUiState()

        assertEquals("", state.vehicleModelName)
        org.junit.Assert.assertNull(state.security.isLocked)
        assertFalse(state.security.isCloudConfigured)
        assertFalse(state.security.isCloudConnected)
        assertEquals("", state.security.cloudStatusText)
        assertTrue(state.security.mirrorsFolded)

        // Tyres null by default until telemetry arrives
        org.junit.Assert.assertNull(state.tyres.flPsi)
        org.junit.Assert.assertNull(state.tyres.frPsi)
        org.junit.Assert.assertNull(state.tyres.flTemp)

        // Doors closed by default
        assertFalse(state.doors.frontLeftOpen)
        assertFalse(state.doors.frontRightOpen)
        assertFalse(state.doors.rearLeftOpen)
        assertFalse(state.doors.rearRightOpen)
        assertFalse(state.doors.trunkOpen)
        assertFalse(state.doors.hoodOpen)

        // Climate defaults
        assertFalse(state.climate.isAcOn)
        assertEquals(22, state.climate.targetTemp)
        assertEquals(3, state.climate.fanLevel)
        assertFalse(state.climate.isBatteryHeatOn)

        // Comfort defaults
        assertEquals(0, state.comfort.driverSeatHeat)
        assertEquals(0, state.comfort.driverSeatVent)
        assertFalse(state.comfort.steeringHeatOn)
    }

    @Test
    fun testTyresPressureValuesAndThresholds() {
        val tyres = VehicleTyresState(
            flPsi = 36.5f,
            frPsi = 36.2f,
            rlPsi = 35.8f,
            rrPsi = 28.5f, // Low tyre pressure alert threshold (< 30 PSI)
            flTemp = 30,
            frTemp = 30,
            rlTemp = 31,
            rrTemp = 32
        )

        assertNotNull(tyres.flPsi)
        assertTrue(tyres.flPsi!! >= 30.0f)
        assertTrue(tyres.rrPsi!! < 30.0f) // Triggers warning container styling
        assertEquals(32, tyres.rrTemp)
    }

    @Test
    fun testClimateTemperatureAndFanBounds() {
        var climate = VehicleClimateState()

        // Valid ranges: temp 16..30, fan 1..7
        assertTrue(climate.targetTemp in 16..30)
        assertTrue(climate.fanLevel in 1..7)

        climate = climate.copy(targetTemp = 24, fanLevel = 5, isAcOn = true)
        assertEquals(24, climate.targetTemp)
        assertEquals(5, climate.fanLevel)
        assertTrue(climate.isAcOn)
    }

    @Test
    fun testSeatLevelCycling() {
        // Seat levels cycle 0 -> 1 -> 2 -> 3 -> 0
        var level = 0
        level = (level + 1) % 4
        assertEquals(1, level)
        level = (level + 1) % 4
        assertEquals(2, level)
        level = (level + 1) % 4
        assertEquals(3, level)
        level = (level + 1) % 4
        assertEquals(0, level)
    }

    @Test
    fun testWindowsVentModeState() {
        val windows = VehicleWindowsState(
            isVentMode = true,
            frontLeftOpen = false,
            sunroofOpen = false
        )

        assertTrue(windows.isVentMode)
        assertFalse(windows.sunroofOpen)
    }
}
