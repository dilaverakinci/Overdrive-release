package com.overdrive.app.domain.repository

import com.overdrive.app.byd.BydDataCollector
import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.engine.VehicleDataDispatcher
import com.overdrive.app.domain.mapper.VehicleDataDomainMapper
import com.overdrive.app.domain.model.BodyworkState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Default implementation of [BodyworkRepository] backed by a reactive [MutableStateFlow].
 */
class DefaultBodyworkRepository : BodyworkRepository {

    private val _bodyworkState = MutableStateFlow(BodyworkState())
    override val bodyworkState: StateFlow<BodyworkState> = _bodyworkState.asStateFlow()

    fun updateFromSnapshot(data: BydVehicleData) {
        _bodyworkState.value = VehicleDataDomainMapper.toBodyworkState(data)
    }

    override suspend fun setPadRotation(portrait: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val collector = BydDataCollector.getInstance()
            val mode = if (portrait) BydDataCollector.PAD_ROTATION_VERTICAL else BydDataCollector.PAD_ROTATION_HORIZONTAL
            collector?.setPadRotation(mode) ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun lockDoors(): Boolean = withContext(Dispatchers.IO) {
        try {
            val collector = BydDataCollector.getInstance()
            collector?.bodyworkProbe?.doorLockDevice != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun unlockDoors(): Boolean = withContext(Dispatchers.IO) {
        try {
            val collector = BydDataCollector.getInstance()
            collector?.bodyworkProbe?.doorLockDevice != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun refresh() {
        VehicleDataDispatcher.pollCurrent()
    }
}
