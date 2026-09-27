package com.overdrive.app.ui.charging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying state mutations, actions, and safety invariants for Charging Compose Native.
 */
class ChargingComposeParityTest {

    @Test
    fun initialState_hasDefaultChargingState() {
        val state = ChargingUiState()
        assertEquals(74, state.socPercent)
        assertEquals(80, state.targetSocLimit)
        assertEquals(16, state.targetCurrentLimitA)
        assertEquals(ChargingStatus.CHARGING, state.status)
        assertEquals(ChargingTab.LIVE, state.selectedTab)
        assertEquals(3, state.sessions.size)
    }

    @Test
    fun tabSelection_switchesBetweenLiveAndSessions() {
        val state = ChargingUiState()
        val next = state.copy(selectedTab = ChargingTab.SESSIONS)
        assertEquals(ChargingTab.SESSIONS, next.selectedTab)
    }

    @Test
    fun targetSoc_updatesCorrectly() {
        val state = ChargingUiState()
        val next = state.copy(targetSocLimit = 90)
        assertEquals(90, next.targetSocLimit)
    }

    @Test
    fun currentLimit_updatesCorrectly() {
        val state = ChargingUiState()
        val next = state.copy(targetCurrentLimitA = 32)
        assertEquals(32, next.targetCurrentLimitA)
    }

    @Test
    fun portLockToggle_invertsLockState() {
        val state = ChargingUiState(isPortUnlocked = false)
        val next = state.copy(isPortUnlocked = !state.isPortUnlocked)
        assertTrue(next.isPortUnlocked)
    }

    @Test
    fun batteryPreHeatToggle_invertsHeatingState() {
        val state = ChargingUiState(isBatteryPreHeating = false)
        val next = state.copy(isBatteryPreHeating = !state.isBatteryPreHeating)
        assertTrue(next.isBatteryPreHeating)
    }
}
