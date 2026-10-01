package com.overdrive.app.surveillance.deterrent

import android.content.Context
import com.overdrive.app.byd.HazardLightProbe
import com.overdrive.app.byd.routing.VehicleCommandRouter
import com.overdrive.app.daemon.DaemonBootstrap
import com.overdrive.app.logging.DaemonLogger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Actuates vehicle hazard lights or headlamps for Sentry Deterrent.
 * First tries local hardware actuation via [HazardLightProbe], and falls back to
 * cloud remote actuation via [VehicleCommandRouter] if local execution fails.
 */
class DeterrentLightActuator(
    private val contextProvider: () -> Context? = { DaemonBootstrap.getContext() },
    private val localActuator: (Context, Long) -> Boolean = { ctx, holdMs ->
        val candidate = HazardLightProbe.candidateById("A") ?: HazardLightProbe.candidates()[0]
        val result = HazardLightProbe.runCandidate(ctx, candidate, holdMs)
        val onArr = result.optJSONArray("on")
        var accepted = false
        if (onArr != null && onArr.length() > 0) {
            for (i in 0 until onArr.length()) {
                val item = onArr.optJSONObject(i)
                if (item != null && item.optBoolean("accepted", false)) {
                    accepted = true
                    break
                }
            }
        }
        accepted
    },
    private val cloudActuator: () -> Boolean = {
        val result = VehicleCommandRouter.getInstance().execute(VehicleCommandRouter.FlashLightsCommand())
        result != null && result.outcome == VehicleCommandRouter.Outcome.SUCCESS
    }
) {

    private val logger = DaemonLogger.getInstance("DeterrentLightActuator")
    private val isActuating = AtomicBoolean(false)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "DeterrentLightWorker").apply { isDaemon = true }
    }

    enum class ActuationMethod {
        LOCAL_HARDWARE,
        CLOUD_REMOTE,
        FAILED
    }

    data class ActuationResult(
        val success: Boolean,
        val method: ActuationMethod,
        val detail: String
    )

    /**
     * Executes the hazard light flash synchronously.
     */
    fun triggerHazardFlashSync(holdMs: Long = 2500L): ActuationResult {
        if (!isActuating.compareAndSet(false, true)) {
            logger.debug("Light actuation already in flight, skipping.")
            return ActuationResult(false, ActuationMethod.FAILED, "Already in flight")
        }

        try {
            logger.info("Attempting local hazard light actuation ($holdMs ms)...")
            val ctx = contextProvider()
            if (ctx != null) {
                try {
                    val localSuccess = localActuator(ctx, holdMs)
                    if (localSuccess) {
                        logger.info("Local hazard light actuation succeeded.")
                        return ActuationResult(true, ActuationMethod.LOCAL_HARDWARE, "Local HAL set success")
                    }
                    logger.warn("Local hazard light actuation rejected by HAL, attempting cloud fallback...")
                } catch (e: Throwable) {
                    logger.warn("Local hazard actuation threw exception: ${e.message}, falling back to cloud...")
                }
            } else {
                logger.warn("No Context available for local actuation, attempting cloud fallback...")
            }

            // Cloud fallback
            val cloudSuccess = cloudActuator()
            return if (cloudSuccess) {
                logger.info("Cloud hazard light actuation succeeded.")
                ActuationResult(true, ActuationMethod.CLOUD_REMOTE, "Cloud FLASHLIGHTNOWHISTLE success")
            } else {
                logger.warn("Cloud hazard light actuation failed or unavailable.")
                ActuationResult(false, ActuationMethod.FAILED, "Both local and cloud actuation failed")
            }
        } finally {
            isActuating.set(false)
        }
    }

    /**
     * Executes the hazard light flash asynchronously on a background worker thread.
     */
    fun triggerHazardFlashAsync(holdMs: Long = 2500L, onComplete: ((ActuationResult) -> Unit)? = null) {
        executor.execute {
            val res = triggerHazardFlashSync(holdMs)
            onComplete?.invoke(res)
        }
    }

    fun isCurrentlyActuating(): Boolean = isActuating.get()
}
