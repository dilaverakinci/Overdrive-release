package com.overdrive.app.domain.repository

import com.overdrive.app.domain.model.HvacState
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive repository providing vehicle climate control, cabin comfort, and air quality telemetry.
 */
interface HvacRepository {
    /**
     * Observable, hot StateFlow stream of current vehicle climate status.
     */
    val hvacState: StateFlow<HvacState>

    /**
     * Turn A/C power on or off.
     */
    suspend fun setAcEnabled(enabled: Boolean): Boolean

    /**
     * Adjust target cabin setpoint temperature in degrees Celsius.
     */
    suspend fun setTemperature(targetTemp: Double): Boolean

    /**
     * Adjust blower fan speed level.
     */
    suspend fun setFanSpeed(level: Int): Boolean

    /**
     * Request immediate refresh from underlying hardware probe or cache.
     */
    suspend fun refresh()
}
