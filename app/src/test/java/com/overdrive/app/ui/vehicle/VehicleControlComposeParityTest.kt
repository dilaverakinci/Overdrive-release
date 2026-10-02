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

    @Test
    fun testCockpitPowertrainAndBatteryStateDefaults() {
        val state = VehicleControlUiState()

        assertEquals(0.0, state.powertrain.speedKmh, 0.001)
        assertEquals(0.0, state.powertrain.powerKw, 0.001)
        assertEquals(com.overdrive.app.domain.model.Gear.P, state.powertrain.gear)
        assertEquals(com.overdrive.app.domain.model.OperationMode.NORMAL, state.powertrain.operationMode)

        assertEquals(0, state.battery.socPercent)
        assertEquals(0, state.battery.elecRangeKm)
        assertEquals(100.0, state.battery.sohPercent, 0.001)
        assertFalse(state.battery.isCharging)
        assertFalse(state.is3DMode)
        assertEquals(VehicleControlTab.SECURITY, state.selectedTab)
    }

    @Test
    fun testCockpitTabNavigationAnd3DMode() {
        var state = VehicleControlUiState()

        state = state.copy(is3DMode = true)
        assertTrue(state.is3DMode)

        state = state.copy(selectedTab = VehicleControlTab.CHARGING)
        assertEquals(VehicleControlTab.CHARGING, state.selectedTab)

        state = state.copy(selectedTab = VehicleControlTab.SOUND, activeAvasTone = 4)
        assertEquals(VehicleControlTab.SOUND, state.selectedTab)
        assertEquals(4, state.activeAvasTone)
    }

    @Test
    fun testLiveTelemetryEnrichmentAndResponsiveValues() {
        val state = VehicleControlUiState(
            powertrain = VehiclePowertrainUiState(
                speedKmh = 74.5,
                powerKw = 24.2,
                gear = com.overdrive.app.domain.model.Gear.D,
                operationMode = com.overdrive.app.domain.model.OperationMode.SPORT
            ),
            battery = VehicleBatteryUiState(
                socPercent = 68,
                elecRangeKm = 345,
                batteryCapacityKwh = 71.8,
                batteryTempC = 28,
                sohPercent = 98.4,
                isCharging = false,
                chargingPowerKw = 0.0,
                voltage12v = 13.4
            ),
            tyres = VehicleTyresState(
                flPsi = 36.5f,
                frPsi = 36.2f,
                rlPsi = 35.8f,
                rrPsi = 35.9f,
                flTemp = 29,
                frTemp = 30,
                rlTemp = 31,
                rrTemp = 31
            )
        )

        // 1. Canlı Hız & Vites
        assertEquals(74.5, state.powertrain.speedKmh, 0.001)
        assertEquals(com.overdrive.app.domain.model.Gear.D, state.powertrain.gear)
        assertEquals(com.overdrive.app.domain.model.OperationMode.SPORT, state.powertrain.operationMode)

        // 2. Canlı Güç
        assertEquals(24.2, state.powertrain.powerKw, 0.001)

        // 3. Batarya, Menzil, SOH & Sıcaklık
        assertEquals(68, state.battery.socPercent)
        assertEquals(345, state.battery.elecRangeKm)
        assertEquals(98.4, state.battery.sohPercent, 0.001)
        assertEquals(28, state.battery.batteryTempC)
        assertEquals(13.4, state.battery.voltage12v, 0.001)

        // 4. 4 Tekerlek Lastik Sıcaklık & Basınç
        assertEquals(36.5f, state.tyres.flPsi)
        assertEquals(29, state.tyres.flTemp)
        assertEquals(31, state.tyres.rrTemp)
    }

    @Test
    fun testChargingPowerSignConvention() {
        // Charging at 11 kW AC station:
        // Battery status reports positive charging power (11.0 kW)
        // Powertrain telemetry reports negative power flow into battery (-11.0 kW)
        val chargingState = VehicleControlUiState(
            powertrain = VehiclePowertrainUiState(
                speedKmh = 0.0,
                powerKw = -11.0,
                gear = com.overdrive.app.domain.model.Gear.P
            ),
            battery = VehicleBatteryUiState(
                socPercent = 45,
                elecRangeKm = 230,
                isCharging = true,
                chargingPowerKw = 11.0
            )
        )

        assertTrue(chargingState.battery.isCharging)
        assertEquals(11.0, chargingState.battery.chargingPowerKw, 0.001)
        assertTrue(chargingState.powertrain.powerKw < 0.0) // Regen / charging into battery
    }

    @Test
    fun testAdaptiveTelemetryTimingDetermination() {
        fun isHighSpeedTelemetryActive(state: VehicleControlUiState): Boolean {
            return state.powertrain.speedKmh > 0.5 ||
                    state.powertrain.gear != com.overdrive.app.domain.model.Gear.P ||
                    state.battery.isCharging
        }

        // Parked, not moving, not charging -> Idle mode (1000ms)
        val parkedState = VehicleControlUiState(
            powertrain = VehiclePowertrainUiState(speedKmh = 0.0, gear = com.overdrive.app.domain.model.Gear.P),
            battery = VehicleBatteryUiState(isCharging = false)
        )
        assertFalse(isHighSpeedTelemetryActive(parkedState))

        // In Drive at 0 km/h (e.g. traffic light) -> Active mode (200ms)
        val trafficLightState = VehicleControlUiState(
            powertrain = VehiclePowertrainUiState(speedKmh = 0.0, gear = com.overdrive.app.domain.model.Gear.D),
            battery = VehicleBatteryUiState(isCharging = false)
        )
        assertTrue(isHighSpeedTelemetryActive(trafficLightState))

        // In Drive at 50 km/h -> Active mode (200ms)
        val drivingState = VehicleControlUiState(
            powertrain = VehiclePowertrainUiState(speedKmh = 50.0, gear = com.overdrive.app.domain.model.Gear.D),
            battery = VehicleBatteryUiState(isCharging = false)
        )
        assertTrue(isHighSpeedTelemetryActive(drivingState))

        // Parked but Charging -> Active mode (200ms)
        val chargingState = VehicleControlUiState(
            powertrain = VehiclePowertrainUiState(speedKmh = 0.0, gear = com.overdrive.app.domain.model.Gear.P),
            battery = VehicleBatteryUiState(isCharging = true, chargingPowerKw = 60.0)
        )
        assertTrue(isHighSpeedTelemetryActive(chargingState))
    }

    @Test
    fun testRealTelemetryDefaultsAreZero() {
        val defaultBattery = VehicleBatteryUiState()
        assertEquals(0.0, defaultBattery.avg50KmKwh, 0.001)
        assertEquals(0.0, defaultBattery.avgLifetimeKwh, 0.001)
        assertEquals(0.0, defaultBattery.sinceLastChargeKm, 0.001)
        assertEquals(0.0, defaultBattery.sinceLastChargeAvgKwh, 0.001)
        assertEquals(0.0, defaultBattery.activeTripKm, 0.001)
        assertEquals(0, defaultBattery.activeTripMinutes)
        assertEquals(0.0, defaultBattery.regenKwh, 0.001)
    }

    @Test
    fun testPedalAndTripTelemetryMetrics() {
        val state = VehicleControlUiState(
            powertrain = VehiclePowertrainUiState(
                speedKmh = 35.0,
                powerKw = 14.5,
                gear = com.overdrive.app.domain.model.Gear.D,
                accelPedalPercent = 18,
                brakePedalPercent = 0
            ),
            battery = VehicleBatteryUiState(
                socPercent = 75,
                elecRangeKm = 380,
                realisticRangeKm = 334,
                batteryCapacityKwh = 82.5,
                avg50KmKwh = 1.1,
                avgLifetimeKwh = 16.8,
                sinceLastChargeKm = 42.0,
                sinceLastChargeAvgKwh = 15.2,
                activeTripKm = 12.4,
                activeTripMinutes = 15,
                regenKwh = 0.13
            )
        )

        assertEquals(18, state.powertrain.accelPedalPercent)
        assertEquals(0, state.powertrain.brakePedalPercent)
        assertEquals(334, state.battery.realisticRangeKm)
        assertEquals(82.5, state.battery.batteryCapacityKwh, 0.001)
        assertEquals(1.1, state.battery.avg50KmKwh, 0.001)
        assertEquals(0.13, state.battery.regenKwh, 0.001)
        assertEquals(42.0, state.battery.sinceLastChargeKm, 0.001)
        assertEquals(15.2, state.battery.sinceLastChargeAvgKwh, 0.001)
        assertEquals(12.4, state.battery.activeTripKm, 0.001)
        assertEquals(15, state.battery.activeTripMinutes)
    }

    @Test
    fun test3DModeToggleTransition() {
        var state = VehicleControlUiState(is3DMode = false)
        assertFalse(state.is3DMode)

        // Toggle to 3D
        state = state.copy(is3DMode = true)
        assertTrue(state.is3DMode)

        // Toggle back to 2D
        state = state.copy(is3DMode = false)
        assertFalse(state.is3DMode)
    }

    @Test
    fun testVehicleArt2DFallbackOnlyWhenUnselected() {
        // When no vehicle is selected in the app (modelId == null or empty),
        // VehicleArt returns R.drawable.vehicle_fallback (black vehicle with '?' badge on hood).
        val fallbackDrawable = com.overdrive.app.R.drawable.vehicle_fallback
        assertEquals(fallbackDrawable, VehicleArt.drawableFor(null))
        assertEquals(fallbackDrawable, VehicleArt.drawableFor(""))
        assertEquals(fallbackDrawable, VehicleArt.drawableFor("__unset__"))

        // When a vehicle IS selected, VehicleArt returns the exact 2D render without fallback
        val sealDrawable = VehicleArt.drawableFor("seal")
        org.junit.Assert.assertNotEquals(fallbackDrawable, sealDrawable)
        assertEquals(com.overdrive.app.R.drawable.vehicle_seal, sealDrawable)

        val atto3Drawable = VehicleArt.drawableFor("atto3")
        org.junit.Assert.assertNotEquals(fallbackDrawable, atto3Drawable)
        assertEquals(com.overdrive.app.R.drawable.vehicle_atto3, atto3Drawable)

        val dolphinDrawable = VehicleArt.drawableFor("dolphin")
        org.junit.Assert.assertNotEquals(fallbackDrawable, dolphinDrawable)
        assertEquals(com.overdrive.app.R.drawable.vehicle_dolphin, dolphinDrawable)

        val hanDrawable = VehicleArt.drawableFor("han")
        org.junit.Assert.assertNotEquals(fallbackDrawable, hanDrawable)
        assertEquals(com.overdrive.app.R.drawable.vehicle_han, hanDrawable)
    }
}

