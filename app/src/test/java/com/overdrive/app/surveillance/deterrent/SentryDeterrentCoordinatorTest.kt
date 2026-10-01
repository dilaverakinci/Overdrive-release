package com.overdrive.app.surveillance.deterrent

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class SentryDeterrentCoordinatorTest {

    @Test
    fun testDeterrentModeValues() {
        assertEquals("Sadece Ekran", DeterrentMode.SCREEN_ONLY.titleTr)
        assertEquals("Sadece Flaşör", DeterrentMode.LIGHTS_ONLY.titleTr)
        assertEquals("Sadece Sesli İkaz", DeterrentMode.SOUND_ONLY.titleTr)
        assertTrue(DeterrentMode.FULL_DETERRENT.titleTr.contains("Tam Caydırıcılık"))
    }

    @Test
    fun testLightActuatorLocalSuccess() {
        var localCalled = false
        var cloudCalled = false

        val dummyContext = android.content.ContextWrapper(null)
        val actuator = DeterrentLightActuator(
            contextProvider = { dummyContext },
            localActuator = { _, _ ->
                localCalled = true
                true
            },
            cloudActuator = {
                cloudCalled = true
                true
            }
        )

        val result = actuator.triggerHazardFlashSync(1000L)
        assertTrue(result.success)
        assertEquals(DeterrentLightActuator.ActuationMethod.LOCAL_HARDWARE, result.method)
        assertTrue(localCalled)
        assertFalse(cloudCalled)
    }

    @Test
    fun testLightActuatorCloudFallbackWhenLocalFails() {
        var localCalled = false
        var cloudCalled = false

        val dummyContext = android.content.ContextWrapper(null)
        val actuator = DeterrentLightActuator(
            contextProvider = { dummyContext },
            localActuator = { _, _ ->
                localCalled = true
                false // Local rejected by HAL
            },
            cloudActuator = {
                cloudCalled = true
                true // Cloud succeeds
            }
        )

        val result = actuator.triggerHazardFlashSync(1000L)
        assertTrue(result.success)
        assertEquals(DeterrentLightActuator.ActuationMethod.CLOUD_REMOTE, result.method)
        assertTrue(localCalled)
        assertTrue(cloudCalled)
    }

    @Test
    fun testLightActuatorCloudFallbackWhenNoContext() {
        var cloudCalled = false

        val actuator = DeterrentLightActuator(
            contextProvider = { null },
            localActuator = { _, _ -> true },
            cloudActuator = {
                cloudCalled = true
                true
            }
        )

        val result = actuator.triggerHazardFlashSync(1000L)
        assertTrue(result.success)
        assertEquals(DeterrentLightActuator.ActuationMethod.CLOUD_REMOTE, result.method)
        assertTrue(cloudCalled)
    }

    @Test
    fun testLightActuatorFailureWhenBothFail() {
        val actuator = DeterrentLightActuator(
            contextProvider = { null },
            localActuator = { _, _ -> false },
            cloudActuator = { false }
        )

        val result = actuator.triggerHazardFlashSync(1000L)
        assertFalse(result.success)
        assertEquals(DeterrentLightActuator.ActuationMethod.FAILED, result.method)
    }

    @Test
    fun testCoordinatorTriggersScreenOnly() {
        val screenTriggered = AtomicBoolean(false)
        val coordinator = SentryDeterrentCoordinator(
            audioPlayer = DeterrentAudioPlayer(),
            lightActuator = DeterrentLightActuator(contextProvider = { null }, cloudActuator = { true }),
            screenTrigger = { screenTriggered.set(true) },
            screenCancel = {},
            cooldownMs = 15000L
        )

        val report = coordinator.triggerDeterrent(DeterrentMode.SCREEN_ONLY, force = true)
        assertTrue(report.triggered)
        assertFalse(report.throttled)
        assertEquals(listOf("SCREEN"), report.executedActions)
        Thread.sleep(50)
        assertTrue(screenTriggered.get())
    }

    @Test
    fun testCoordinatorTriggersLightsOnly() {
        val lightsCalled = AtomicBoolean(false)
        val lightActuator = DeterrentLightActuator(
            contextProvider = { null },
            localActuator = { _, _ -> false },
            cloudActuator = {
                lightsCalled.set(true)
                true
            }
        )

        val coordinator = SentryDeterrentCoordinator(
            audioPlayer = DeterrentAudioPlayer(),
            lightActuator = lightActuator,
            screenTrigger = {},
            screenCancel = {},
            cooldownMs = 15000L
        )

        val report = coordinator.triggerDeterrent(DeterrentMode.LIGHTS_ONLY, force = true)
        assertTrue(report.triggered)
        assertEquals(listOf("LIGHTS"), report.executedActions)
        Thread.sleep(100)
        assertTrue(lightsCalled.get())
    }

    @Test
    fun testCoordinatorTriggersFullDeterrent() {
        val screenTriggered = AtomicBoolean(false)
        val lightsCalled = AtomicBoolean(false)

        val lightActuator = DeterrentLightActuator(
            contextProvider = { null },
            cloudActuator = {
                lightsCalled.set(true)
                true
            }
        )

        val coordinator = SentryDeterrentCoordinator(
            audioPlayer = DeterrentAudioPlayer(),
            lightActuator = lightActuator,
            screenTrigger = { screenTriggered.set(true) },
            screenCancel = {},
            cooldownMs = 15000L
        )

        val report = coordinator.triggerDeterrent(DeterrentMode.FULL_DETERRENT, force = true)
        assertTrue(report.triggered)
        assertTrue(report.executedActions.contains("SCREEN"))
        assertTrue(report.executedActions.contains("LIGHTS"))
        assertTrue(report.executedActions.contains("SOUND"))
    }

    @Test
    fun testCoordinatorCooldownEnforcementAndBypass() {
        val screenTriggerCount = AtomicInteger(0)
        val coordinator = SentryDeterrentCoordinator(
            audioPlayer = DeterrentAudioPlayer(),
            lightActuator = DeterrentLightActuator(contextProvider = { null }, cloudActuator = { true }),
            screenTrigger = { screenTriggerCount.incrementAndGet() },
            screenCancel = {},
            cooldownMs = 5000L // 5 seconds
        )

        // First trigger succeeds
        val report1 = coordinator.triggerDeterrent(DeterrentMode.SCREEN_ONLY, force = false)
        assertTrue(report1.triggered)
        assertFalse(report1.throttled)

        // Second trigger immediately after should be throttled
        val report2 = coordinator.triggerDeterrent(DeterrentMode.SCREEN_ONLY, force = false)
        assertFalse(report2.triggered)
        assertTrue(report2.throttled)
        assertTrue(report2.executedActions.isEmpty())

        // Third trigger with force = true should bypass cooldown
        val report3 = coordinator.triggerDeterrent(DeterrentMode.SCREEN_ONLY, force = true)
        assertTrue(report3.triggered)
        assertFalse(report3.throttled)
        assertEquals(listOf("SCREEN"), report3.executedActions)
    }

    @Test
    fun testCoordinatorCancelAll() {
        val screenCancelled = AtomicBoolean(false)
        val coordinator = SentryDeterrentCoordinator(
            audioPlayer = DeterrentAudioPlayer(),
            lightActuator = DeterrentLightActuator(contextProvider = { null }, cloudActuator = { true }),
            screenTrigger = {},
            screenCancel = { screenCancelled.set(true) },
            cooldownMs = 5000L
        )

        coordinator.cancelAll()
        assertTrue(screenCancelled.get())
    }
}
