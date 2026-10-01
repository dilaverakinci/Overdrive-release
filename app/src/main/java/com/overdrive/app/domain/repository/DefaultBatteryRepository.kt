package com.overdrive.app.domain.repository

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.engine.VehicleDataDispatcher
import com.overdrive.app.domain.mapper.VehicleDataDomainMapper
import com.overdrive.app.domain.model.BatteryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default implementation of [BatteryRepository] backed by a reactive [MutableStateFlow].
 */
class DefaultBatteryRepository : BatteryRepository {

    private val _batteryState = MutableStateFlow(BatteryState())
    override val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    fun updateFromSnapshot(data: BydVehicleData) {
        _batteryState.value = VehicleDataDomainMapper.toBatteryState(data)
    }

    override suspend fun refresh() {
        VehicleDataDispatcher.pollCurrent()
    }
}
