package com.overdrive.app.domain

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.mapper.VehicleDataDomainMapper
import com.overdrive.app.domain.model.BatteryState
import com.overdrive.app.domain.model.BodyworkState
import com.overdrive.app.domain.model.ChassisState
import com.overdrive.app.domain.model.EnergyMode
import com.overdrive.app.domain.model.Gear
import com.overdrive.app.domain.model.HvacState
import com.overdrive.app.domain.model.OperationMode
import com.overdrive.app.domain.model.PadOrientation
import com.overdrive.app.domain.model.PowertrainState
import com.overdrive.app.domain.repository.BatteryRepository
import com.overdrive.app.domain.repository.BodyworkRepository
import com.overdrive.app.domain.repository.ChassisRepository
import com.overdrive.app.domain.repository.HvacRepository
import com.overdrive.app.domain.repository.PowertrainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract test suite ensuring Phase 1 Domain Models, Mappers, and Repository Contracts
 * are fully compliant, null-safe, and immutable.
 */
class DomainModelContractTest {

    @Test
    fun testDefaultBatteryState() {
        val state = BatteryState()
        assertEquals(0.0, state.socPercent, 0.001)
        assertEquals(100.0, state.sohPercent, 0.001)
        assertFalse(state.isCharging)
        assertFalse(state.isFastCharging)
        assertFalse(state.isVtolActive)
        assertTrue(state.highCellTempC.isNaN())
    }

    @Test
    fun testDefaultPowertrainState() {
        val state = PowertrainState()
        assertEquals(0.0, state.speedKmh, 0.001)
        assertEquals(Gear.UNKNOWN, state.gear)
        assertEquals(EnergyMode.EV, state.energyMode)
        assertEquals(OperationMode.NORMAL, state.operationMode)
    }

    @Test
    fun testDefaultHvacState() {
        val state = HvacState()
        assertFalse(state.isAcOn)
        assertEquals(22.0, state.driverSetpointTemp, 0.001)
        assertEquals(22.0, state.passengerSetpointTemp, 0.001)
        assertTrue(state.insideTempC.isNaN())
    }

    @Test
    fun testDefaultBodyworkState() {
        val state = BodyworkState()
        assertTrue(state.isLocked)
        assertFalse(state.doorOpenFl)
        assertFalse(state.trunkOpen)
        assertEquals(PadOrientation.HORIZONTAL, state.padOrientation)
    }

    @Test
    fun testDefaultChassisState() {
        val state = ChassisState()
        assertEquals(0, state.tyrePressureFlKpa)
        assertEquals(0.0f, state.tyrePressureFlBar, 0.001f)
        assertTrue(state.tyreSystemHealthOk)
        assertTrue(state.radarDistances.isEmpty())
    }

    @Test
    fun testMapperWithNullVehicleData() {
        val battery = VehicleDataDomainMapper.toBatteryState(null)
        val powertrain = VehicleDataDomainMapper.toPowertrainState(null)
        val hvac = VehicleDataDomainMapper.toHvacState(null)
        val bodywork = VehicleDataDomainMapper.toBodyworkState(null)
        val chassis = VehicleDataDomainMapper.toChassisState(null)

        assertNotNull(battery)
        assertNotNull(powertrain)
        assertNotNull(hvac)
        assertNotNull(bodywork)
        assertNotNull(chassis)
        assertEquals(0.0, battery.socPercent, 0.001)
        assertEquals(Gear.UNKNOWN, powertrain.gear)
    }

    @Test
    fun testMapperWithPopulatedVehicleData() {
        val builder = BydVehicleData.Builder()
            .socPercent(78.5)
            .sohPercent(99.0)
            .remainKwh(48.2)
            .voltage12v(13.8)
            .speedKmh(65.0)
            .gearMode(4) // Gear.D
            .energyMode(1) // EV
            .operationMode(3) // SPORT
            .acStartState(1) // AC On
            .acSetpointDriver(21)
            .tyrePressure(intArrayOf(240, 240, 250, 250))
            .tyreTemperature(intArrayOf(25, 26, 25, 26))
            .radarDistances(intArrayOf(10, 20, 30))
            .chargingPowerKw(45.0)
            .chargingGunState(1)

        val data = builder.build()

        val battery = VehicleDataDomainMapper.toBatteryState(data)
        assertEquals(78.5, battery.socPercent, 0.001)
        assertEquals(48.2, battery.remainKwh, 0.001)
        assertEquals(13.8, battery.voltage12v, 0.001)
        assertTrue(battery.isCharging)
        assertTrue(battery.isFastCharging)

        val powertrain = VehicleDataDomainMapper.toPowertrainState(data)
        assertEquals(65.0, powertrain.speedKmh, 0.001)
        assertEquals(Gear.D, powertrain.gear)
        assertEquals(EnergyMode.EV, powertrain.energyMode)
        assertEquals(OperationMode.SPORT, powertrain.operationMode)

        val hvac = VehicleDataDomainMapper.toHvacState(data)
        assertTrue(hvac.isAcOn)
        assertEquals(21.0, hvac.driverSetpointTemp, 0.001)

        val chassis = VehicleDataDomainMapper.toChassisState(data)
        assertEquals(240, chassis.tyrePressureFlKpa)
        assertEquals(2.40f, chassis.tyrePressureFlBar, 0.01f)
        assertEquals(250, chassis.tyrePressureRlKpa)
        assertEquals(2.50f, chassis.tyrePressureRlBar, 0.01f)
        assertEquals(3, chassis.radarDistances.size)
        assertEquals(10, chassis.radarDistances[0])
    }

    @Test
    fun testRepositoryContractsMock() = runBlocking {
        val testBatteryFlow = MutableStateFlow(BatteryState(socPercent = 85.0))
        val testRepo = object : BatteryRepository {
            override val batteryState: StateFlow<BatteryState> = testBatteryFlow
            override suspend fun refresh() {
                testBatteryFlow.value = testBatteryFlow.value.copy(socPercent = 90.0)
            }
        }

        assertEquals(85.0, testRepo.batteryState.value.socPercent, 0.001)
        testRepo.refresh()
        assertEquals(90.0, testRepo.batteryState.value.socPercent, 0.001)
    }
}
