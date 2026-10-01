package com.overdrive.app.domain.engine

import com.overdrive.app.byd.BydDataCollector
import com.overdrive.app.byd.BydVehicleData
import com.overdrive.app.domain.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * High-performance, reactive telemetry dispatcher that converts raw vehicle data snapshots
 * into domain streams for Kotlin StateFlow repositories.
 */
object VehicleDataDispatcher {

    private val _dataFlow = MutableSharedFlow<BydVehicleData>(extraBufferCapacity = 64)
    val dataFlow: SharedFlow<BydVehicleData> = _dataFlow.asSharedFlow()

    /**
     * Dispatch a newly produced vehicle data snapshot to all domain repositories and subscribers.
     */
    @JvmStatic
    fun dispatch(data: BydVehicleData?) {
        if (data == null) return
        _dataFlow.tryEmit(data)
        RepositoryProvider.updateFromVehicleData(data)
    }

    /**
     * Immediate polling of the latest snapshot from BydDataCollector into domain streams.
     */
    @JvmStatic
    fun pollCurrent() {
        try {
            val collector = BydDataCollector.getInstance()
            if (collector != null) {
                val data = collector.data
                if (data != null) {
                    dispatch(data)
                }
            }
        } catch (e: Exception) {
            // Ignored on test JVM or uninitialized collector
        }
    }
}
