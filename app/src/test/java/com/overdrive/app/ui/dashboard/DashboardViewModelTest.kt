package com.overdrive.app.ui.dashboard

import com.overdrive.app.domain.model.BatteryState
import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.PowertrainState
import com.overdrive.app.domain.repository.BatteryRepository
import com.overdrive.app.domain.repository.PowertrainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying Phase 5 DashboardViewModel reactive StateFlow integration.
 */
class DashboardViewModelTest {

    private class MockBatteryRepo(initial: BatteryState) : BatteryRepository {
        val flow = MutableStateFlow(initial)
        override val batteryState: StateFlow<BatteryState> = flow
        override suspend fun refresh() {}
    }

    private class MockPowertrainRepo(initial: PowertrainState) : PowertrainRepository {
        val flow = MutableStateFlow(initial)
        override val powertrainState: StateFlow<PowertrainState> = flow
        override suspend fun refresh() {}
    }

    @Test
    fun testDefaultSnapshotIsNull() {
        val snapshot = DashboardViewModel.mapToSnapshot(BatteryState(), PowertrainState())
        assertNull(snapshot)
    }

    @Test
    fun testPopulatedSnapshotMapping() {
        val battery = BatteryState(
            socPercent = 88.0,
            elecRangeKm = 420,
            isCharging = true,
            chargingGunState = 1,
            chargingPowerKw = 55.5,
            chargingRestTimeMinutes = 25
        )

        val powertrain = PowertrainState(
            speedKmh = 72.5,
            gear = Gear.D
        )

        val snapshot = DashboardViewModel.mapToSnapshot(battery, powertrain)
        assertNotNull(snapshot)
        assertEquals(88.0, snapshot!!.socPercent!!, 0.001)
        assertEquals(72.5, snapshot.speedKmh!!, 0.001)
        assertEquals("D", snapshot.gear)
        assertEquals(420, snapshot.range?.value)
        assertEquals(DashboardDistance.Unit.KILOMETRES, snapshot.range?.unit)
        assertTrue(snapshot.isAccOn == true)

        assertNotNull(snapshot.charging)
        assertTrue(snapshot.charging!!.charging)
        assertTrue(snapshot.charging!!.plugged)
        assertEquals(55.5, snapshot.charging!!.powerKw!!, 0.001)
        assertEquals(25, snapshot.charging!!.timeToFullMinutes)
    }

    @Test
    fun testGearMappingVariations() {
        assertEquals("P", DashboardViewModel.mapToSnapshot(BatteryState(socPercent = 50.0), PowertrainState(gear = Gear.P))?.gear)
        assertEquals("R", DashboardViewModel.mapToSnapshot(BatteryState(socPercent = 50.0), PowertrainState(gear = Gear.R))?.gear)
        assertEquals("N", DashboardViewModel.mapToSnapshot(BatteryState(socPercent = 50.0), PowertrainState(gear = Gear.N))?.gear)
        assertEquals("D", DashboardViewModel.mapToSnapshot(BatteryState(socPercent = 50.0), PowertrainState(gear = Gear.D))?.gear)
        assertEquals("M", DashboardViewModel.mapToSnapshot(BatteryState(socPercent = 50.0), PowertrainState(gear = Gear.M))?.gear)
        assertEquals("S", DashboardViewModel.mapToSnapshot(BatteryState(socPercent = 50.0), PowertrainState(gear = Gear.S))?.gear)
    }
}
