package com.overdrive.app.domain.repository

import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.engine.VehicleDataDispatcher
import com.overdrive.app.domain.mapper.VehicleDataDomainMapper
import com.overdrive.app.domain.model.PowertrainState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default implementation of [PowertrainRepository] backed by a reactive [MutableStateFlow].
 */
class DefaultPowertrainRepository : PowertrainRepository {

    private val _powertrainState = MutableStateFlow(PowertrainState())
    override val powertrainState: StateFlow<PowertrainState> = _powertrainState.asStateFlow()

    fun updateFromSnapshot(data: BydVehicleData) {
        _powertrainState.value = VehicleDataDomainMapper.toPowertrainState(data)
    }

    override suspend fun refresh() {
        VehicleDataDispatcher.pollCurrent()
    }
}
