package com.overdrive.app.domain.repository

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.engine.VehicleDataDispatcher
import com.overdrive.app.domain.mapper.VehicleDataDomainMapper
import com.overdrive.app.domain.model.ChassisState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default implementation of [ChassisRepository] backed by a reactive [MutableStateFlow].
 */
class DefaultChassisRepository : ChassisRepository {

    private val _chassisState = MutableStateFlow(ChassisState())
    override val chassisState: StateFlow<ChassisState> = _chassisState.asStateFlow()

    fun updateFromSnapshot(data: BydVehicleData) {
        _chassisState.value = VehicleDataDomainMapper.toChassisState(data)
    }

    override suspend fun refresh() {
        VehicleDataDispatcher.pollCurrent()
    }
}
