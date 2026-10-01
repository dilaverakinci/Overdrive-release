package com.overdrive.app.domain.repository

import com.overdrive.app.domain.model.ChassisState
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive repository providing TPMS tyre pressures/temperatures and radar proximity telemetry.
 */
interface ChassisRepository {
    /**
     * Observable, hot StateFlow stream of current vehicle chassis and TPMS status.
     */
    val chassisState: StateFlow<ChassisState>

    /**
     * Request immediate refresh from underlying hardware probe or cache.
     */
    suspend fun refresh()
}
