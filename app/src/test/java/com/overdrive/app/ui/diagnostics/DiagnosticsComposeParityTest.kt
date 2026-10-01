package com.overdrive.app.ui.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying state mutations and telemetry invariants for Diagnostics Compose Native.
 */
class DiagnosticsComposeParityTest {

    @Test
    fun initialState_hasDefaultDiagnosticsState() {
        val state = DiagnosticsUiState()
        assertEquals("Wi-Fi: Bağlı Değil", state.networkSsid)
        assertEquals("Tünel: Çevrimdışı", state.tunnelState)
        assertFalse(state.isTunnelOnline)
        assertEquals("--", state.storageUsed)
        assertEquals("--", state.storageFree)
        assertEquals("Kamera Hazır", state.cameraStatus)
        assertTrue(state.isCameraOnline)
        assertEquals("%100 Sağlık", state.batterySoh)
        assertTrue(state.isBatteryGood)
        assertFalse(state.isBatteryReviewNeeded)
    }

    @Test
    fun tunnelOnline_reflectsStateChange() {
        val state = DiagnosticsUiState()
        val onlineState = state.copy(
            tunnelState = "Tünel: Bağlı (Aktif)",
            isTunnelOnline = true
        )
        assertTrue(onlineState.isTunnelOnline)
        assertEquals("Tünel: Bağlı (Aktif)", onlineState.tunnelState)
    }

    @Test
    fun storageMetrics_reflectStorageCapacity() {
        val state = DiagnosticsUiState(
            storageUsed = "14.2 GB",
            storageFree = "48.5 GB"
        )
        assertEquals("14.2 GB", state.storageUsed)
        assertEquals("48.5 GB", state.storageFree)
    }
}
