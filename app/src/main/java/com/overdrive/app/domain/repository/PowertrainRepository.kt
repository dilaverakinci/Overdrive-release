package com.overdrive.app.domain.repository

import com.overdrive.app.domain.model.PowertrainState
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive repository providing real-time vehicle speed, motor torque, gear, and dynamics.
 */
interface PowertrainRepository {
    /**
     * Observable, hot StateFlow stream of current vehicle powertrain status.
     */
    val powertrainState: StateFlow<PowertrainState>

    /**
     * Request immediate refresh from underlying hardware probe or cache.
     */
    suspend fun refresh()
}
