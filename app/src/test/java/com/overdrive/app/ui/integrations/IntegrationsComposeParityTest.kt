package com.overdrive.app.ui.integrations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying state mutations, actions, and safety invariants for Integrations Compose Native.
 */
class IntegrationsComposeParityTest {

    @Test
    fun initialState_hasDefaultIntegrationsState() {
        val state = IntegrationsUiState()
        assertFalse(state.telegramConfigured)
        assertFalse(state.abrpConnected)
        assertFalse(state.mqttConnected)
        assertFalse(state.bydCloudConfigured)
        assertFalse(state.safeKeepConfigured)
        assertFalse(state.allReady)
    }

    @Test
    fun allReady_evaluatesTrueOnlyWhenAllServicesAreConnected() {
        val partialState = IntegrationsUiState(
            telegramConfigured = true,
            abrpConnected = true,
            mqttConnected = false,
            bydCloudConfigured = true
        )
        assertFalse(partialState.allReady)

        val fullState = partialState.copy(mqttConnected = true)
        assertTrue(fullState.allReady)
    }

    @Test
    fun connectionStatus_updatesStateCorrectly() {
        val state = IntegrationsUiState()
        val updated = state.copy(telegramConfigured = true, bydCloudConfigured = true, safeKeepConfigured = true)
        assertTrue(updated.telegramConfigured)
        assertTrue(updated.bydCloudConfigured)
        assertTrue(updated.safeKeepConfigured)
        assertFalse(updated.abrpConnected)
    }
}
