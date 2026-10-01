package com.overdrive.app.domain.repository

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.engine.VehicleDataDispatcher
import com.overdrive.app.domain.model.BatteryState
import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.PadOrientation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite verifying Phase 3 Reactive StateFlow Repositories and VehicleDataDispatcher.
 */
class RepositoryReactiveFlowTest {

    @Before
    fun setUp() {
        RepositoryProvider.resetToDefaults()
    }

    @After
    fun tearDown() {
        RepositoryProvider.resetToDefaults()
    }

    @Test
    fun testDispatcherUpdatesRepositoriesImmediately() {
        val testData = BydVehicleData.Builder()
            .socPercent(92.5)
            .remainKwh(55.4)
            .voltage12v(14.1)
            .speedKmh(110.0)
            .gearMode(4) // Gear.D
            .acStartState(1)
            .acSetpointDriver(20)
            .tyrePressure(intArrayOf(245, 245, 255, 255))
            .chargingPowerKw(60.0)
            .build()

        // Dispatch snapshot
        VehicleDataDispatcher.dispatch(testData)

        // Verify Battery StateFlow
        val battery = RepositoryProvider.batteryRepository.batteryState.value
        assertEquals(92.5, battery.socPercent, 0.001)
        assertEquals(55.4, battery.remainKwh, 0.001)
        assertEquals(14.1, battery.voltage12v, 0.001)
        assertTrue(battery.isCharging)
        assertTrue(battery.isFastCharging)

        // Verify Powertrain StateFlow
        val powertrain = RepositoryProvider.powertrainRepository.powertrainState.value
        assertEquals(110.0, powertrain.speedKmh, 0.001)
        assertEquals(Gear.D, powertrain.gear)

        // Verify HVAC StateFlow
        val hvac = RepositoryProvider.hvacRepository.hvacState.value
        assertTrue(hvac.isAcOn)
        assertEquals(20.0, hvac.driverSetpointTemp, 0.001)

        // Verify Chassis StateFlow
        val chassis = RepositoryProvider.chassisRepository.chassisState.value
        assertEquals(245, chassis.tyrePressureFlKpa)
        assertEquals(2.45f, chassis.tyrePressureFlBar, 0.01f)
    }

    @Test
    fun testConsecutiveUpdatesStreamCorrectly() {
        // Step 1: Low speed in D
        val data1 = BydVehicleData.Builder()
            .speedKmh(30.0)
            .gearMode(4) // D
            .socPercent(80.0)
            .build()
        VehicleDataDispatcher.dispatch(data1)

        assertEquals(30.0, RepositoryProvider.powertrainRepository.powertrainState.value.speedKmh, 0.001)
        assertEquals(Gear.D, RepositoryProvider.powertrainRepository.powertrainState.value.gear)
        assertEquals(80.0, RepositoryProvider.batteryRepository.batteryState.value.socPercent, 0.001)

        // Step 2: Parked
        val data2 = BydVehicleData.Builder()
            .speedKmh(0.0)
            .gearMode(1) // P
            .socPercent(79.9)
            .build()
        VehicleDataDispatcher.dispatch(data2)

        assertEquals(0.0, RepositoryProvider.powertrainRepository.powertrainState.value.speedKmh, 0.001)
        assertEquals(Gear.P, RepositoryProvider.powertrainRepository.powertrainState.value.gear)
        assertEquals(79.9, RepositoryProvider.batteryRepository.batteryState.value.socPercent, 0.001)
    }

    @Test
    fun testRepositoryProviderCustomOverride() {
        val customFlow = MutableStateFlow(BatteryState(socPercent = 99.9))
        val customRepo = object : BatteryRepository {
            override val batteryState: StateFlow<BatteryState> = customFlow
            override suspend fun refresh() {}
        }

        RepositoryProvider.setBatteryRepository(customRepo)
        assertEquals(99.9, RepositoryProvider.batteryRepository.batteryState.value.socPercent, 0.001)

        RepositoryProvider.resetToDefaults()
        assertEquals(0.0, RepositoryProvider.batteryRepository.batteryState.value.socPercent, 0.001)
    }

    @Test
    fun testRepositoryRefreshCallsSafely() = runBlocking {
        // Calling refresh on default repositories without active hardware should safely execute
        RepositoryProvider.batteryRepository.refresh()
        RepositoryProvider.powertrainRepository.refresh()
        RepositoryProvider.hvacRepository.refresh()
        RepositoryProvider.bodyworkRepository.refresh()
        RepositoryProvider.chassisRepository.refresh()

        assertNotNull(RepositoryProvider.batteryRepository.batteryState.value)
    }
}
