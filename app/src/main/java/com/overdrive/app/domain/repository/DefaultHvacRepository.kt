package com.overdrive.app.domain.repository

import com.overdrive.app.byd.BydDataCollector
import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.engine.VehicleDataDispatcher
import com.overdrive.app.domain.mapper.VehicleDataDomainMapper
import com.overdrive.app.domain.model.HvacState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Default implementation of [HvacRepository] backed by a reactive [MutableStateFlow].
 */
class DefaultHvacRepository : HvacRepository {

    private val _hvacState = MutableStateFlow(HvacState())
    override val hvacState: StateFlow<HvacState> = _hvacState.asStateFlow()

    fun updateFromSnapshot(data: BydVehicleData) {
        _hvacState.value = VehicleDataDomainMapper.toHvacState(data)
    }

    override suspend fun setAcEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val collector = BydDataCollector.getInstance()
            collector?.setAcPower(enabled) ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setTemperature(targetTemp: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val collector = BydDataCollector.getInstance()
            collector?.setAcTemperature(1, targetTemp) ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setFanSpeed(level: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val collector = BydDataCollector.getInstance()
            collector?.setAcFanLevel(level) ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun refresh() {
        VehicleDataDispatcher.pollCurrent()
    }
}
