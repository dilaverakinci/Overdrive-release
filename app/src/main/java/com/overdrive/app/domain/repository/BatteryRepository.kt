package com.overdrive.app.domain.repository

import com.overdrive.app.domain.model.BatteryState
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive repository providing real-time battery, energy, and charging telemetry.
 */
interface BatteryRepository {
    /**
     * Observable, hot StateFlow stream of current vehicle battery status.
     */
    val batteryState: StateFlow<BatteryState>

    /**
     * Request immediate refresh from underlying hardware probe or cache.
     */
    suspend fun refresh()
}
