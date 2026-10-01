package com.overdrive.app.surveillance.deterrent

import com.overdrive.app.logging.DaemonLogger
import com.overdrive.app.surveillance.ScreenDeterrent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Coordinator for Sentry Deterrent 2.0.
 * Orchestrates visual screen takeover, PCM acoustic alarms, and hazard light actuation
 * with cooldown anti-spam safeguards.
 */
class SentryDeterrentCoordinator(
    private val audioPlayer: DeterrentAudioPlayer = DeterrentAudioPlayer(),
    private val lightActuator: DeterrentLightActuator = DeterrentLightActuator(),
    private val screenTrigger: () -> Unit = {
        try {
            ScreenDeterrent.getInstance().onMotionDetected()
        } catch (e: Throwable) {
            DaemonLogger.getInstance("SentryDeterrentCoordinator")
                .warn("Screen deterrent trigger failed: ${e.message}")
        }
    },
    private val screenCancel: () -> Unit = {
        try {
            ScreenDeterrent.getInstance().cancel()
        } catch (_: Throwable) {}
    },
    private val cooldownMs: Long = 15_000L
) {

    private val logger = DaemonLogger.getInstance("SentryDeterrentCoordinator")
    private val lastTriggerTimeMs = AtomicLong(0L)
    private val executor: ExecutorService = Executors.newCachedThreadPool { r ->
        Thread(r, "SentryDeterrentCoordinatorWorker").apply { isDaemon = true }
    }

    data class DeterrentExecutionReport(
        val mode: DeterrentMode,
        val triggered: Boolean,
        val throttled: Boolean,
        val executedActions: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Triggers security deterrent actions according to [mode].
     * @param mode Target deterrent mode (SCREEN_ONLY, LIGHTS_ONLY, SOUND_ONLY, FULL_DETERRENT)
     * @param force If true, bypasses the anti-spam cooldown check (e.g. manual user trigger from UI)
     */
    fun triggerDeterrent(mode: DeterrentMode, force: Boolean = false): DeterrentExecutionReport {
        val now = System.currentTimeMillis()
        val last = lastTriggerTimeMs.get()

        if (!force && (now - last < cooldownMs)) {
            val remainingSec = (cooldownMs - (now - last)) / 1000
            logger.debug("Deterrent throttled (cooldown active: ${remainingSec}s remaining). Mode: $mode")
            return DeterrentExecutionReport(
                mode = mode,
                triggered = false,
                throttled = true,
                executedActions = emptyList()
            )
        }

        lastTriggerTimeMs.set(now)
        val actions = mutableListOf<String>()

        logger.info("Executing Sentry Deterrent 2.0 with mode: $mode (forced=$force)")

        when (mode) {
            DeterrentMode.SCREEN_ONLY -> {
                executor.execute { screenTrigger() }
                actions.add("SCREEN")
            }
            DeterrentMode.LIGHTS_ONLY -> {
                lightActuator.triggerHazardFlashAsync(2500L)
                actions.add("LIGHTS")
            }
            DeterrentMode.SOUND_ONLY -> {
                audioPlayer.playWarningAlarm(2500)
                actions.add("SOUND")
            }
            DeterrentMode.FULL_DETERRENT -> {
                executor.execute { screenTrigger() }
                lightActuator.triggerHazardFlashAsync(2500L)
                audioPlayer.playWarningAlarm(2500)
                actions.add("SCREEN")
                actions.add("LIGHTS")
                actions.add("SOUND")
            }
        }

        return DeterrentExecutionReport(
            mode = mode,
            triggered = true,
            throttled = false,
            executedActions = actions
        )
    }

    /**
     * Cancels active visual and audio deterrents.
     */
    fun cancelAll() {
        logger.info("Cancelling all active deterrents...")
        screenCancel()
    }

    fun isAudioPlaying(): Boolean = audioPlayer.isCurrentlyPlaying()
    fun isLightsActuating(): Boolean = lightActuator.isCurrentlyActuating()
    fun getLastTriggerTimeMs(): Long = lastTriggerTimeMs.get()

    companion object {
        @Volatile
        private var instance: SentryDeterrentCoordinator? = null

        fun getInstance(): SentryDeterrentCoordinator {
            return instance ?: synchronized(this) {
                instance ?: SentryDeterrentCoordinator().also { instance = it }
            }
        }

        /**
         * For testing: sets or resets the singleton instance.
         */
        fun setInstanceForTesting(testInstance: SentryDeterrentCoordinator?) {
            synchronized(this) {
                instance = testInstance
            }
        }
    }
}
