package com.overdrive.app.domain.repository

import com.overdrive.app.domain.model.BodyworkState
import kotlinx.coroutines.flow.StateFlow

/**
 * Reactive repository providing vehicle bodywork, door locks, windows, lights, and pad rotation.
 */
interface BodyworkRepository {
    /**
     * Observable, hot StateFlow stream of current vehicle bodywork and lights status.
     */
    val bodyworkState: StateFlow<BodyworkState>

    /**
     * Set central infotainment Pad rotation orientation.
     * @param portrait true for vertical (portrait), false for horizontal (landscape).
     */
    suspend fun setPadRotation(portrait: Boolean): Boolean

    /**
     * Lock all vehicle doors.
     */
    suspend fun lockDoors(): Boolean

    /**
     * Unlock all vehicle doors.
     */
    suspend fun unlockDoors(): Boolean

    /**
     * Request immediate refresh from underlying hardware probe or cache.
     */
    suspend fun refresh()
}
