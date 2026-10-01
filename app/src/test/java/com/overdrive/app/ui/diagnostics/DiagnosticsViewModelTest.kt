package com.overdrive.app.ui.diagnostics

import com.overdrive.app.domain.diagnostics.DefaultDiagnosticsRepository
import com.overdrive.app.domain.diagnostics.EcuType
import com.overdrive.app.domain.diagnostics.dtc.DefaultDtcDiagnosticService
import com.overdrive.app.domain.diagnostics.dtc.DtcCode
import com.overdrive.app.domain.diagnostics.dtc.DtcDictionary
import com.overdrive.app.domain.repository.RepositoryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosticsViewModelTest {

    private lateinit var repo: DefaultDiagnosticsRepository
    private lateinit var dtcService: DefaultDtcDiagnosticService
    private lateinit var viewModel: DiagnosticsViewModel

    @Before
    fun setUp() {
        RepositoryProvider.resetToDefaults()
        repo = DefaultDiagnosticsRepository()
        dtcService = DefaultDtcDiagnosticService()
        viewModel = DiagnosticsViewModel(repo, dtcService, CoroutineScope(Dispatchers.Default))
    }

    @After
    fun tearDown() {
        RepositoryProvider.resetToDefaults()
    }

    @Test
    fun testInitialTabAndDefaultState() {
        val state = viewModel.uiState.value
        assertEquals(DiagnosticsTab.SYSTEM_TOOLS, state.selectedTab)
        assertFalse(state.showClearDtcConfirmDialog)
        assertFalse(state.isClearingDtc)
        assertNull(state.selectedEcuForDetail)
    }

    @Test
    fun testSelectTabTogglesTabState() {
        viewModel.selectTab(DiagnosticsTab.VEHICLE_ECU)
        assertEquals(DiagnosticsTab.VEHICLE_ECU, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(DiagnosticsTab.SYSTEM_TOOLS)
        assertEquals(DiagnosticsTab.SYSTEM_TOOLS, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testToggleEcuDetailExpandsAndCollapses() {
        viewModel.toggleEcuDetail(EcuType.BMS)
        assertEquals(EcuType.BMS, viewModel.uiState.value.selectedEcuForDetail)

        viewModel.toggleEcuDetail(EcuType.BMS)
        assertNull(viewModel.uiState.value.selectedEcuForDetail)

        viewModel.toggleEcuDetail(EcuType.MCU)
        assertEquals(EcuType.MCU, viewModel.uiState.value.selectedEcuForDetail)
    }

    @Test
    fun testClearDtcDialogLifecycle() {
        // Request clear
        viewModel.requestClearDtc()
        assertTrue(viewModel.uiState.value.showClearDtcConfirmDialog)

        // Dismiss without clearing
        viewModel.dismissClearDtcDialog()
        assertFalse(viewModel.uiState.value.showClearDtcConfirmDialog)
    }

    @Test
    fun testUpdateSystemHealthReflectsInState() {
        viewModel.updateSystemHealth(
            networkSsid = "BYD-WiFi-5G",
            tunnelState = "Tünel: Çevrimiçi",
            isTunnelOnline = true,
            storageUsed = "14.2 GB",
            storageFree = "48.6 GB",
            cameraStatus = "4x AVM Aktif",
            batterySoh = "%98 Sağlık",
            isBatteryGood = true
        )

        val state = viewModel.uiState.value
        assertEquals("BYD-WiFi-5G", state.networkSsid)
        assertEquals("Tünel: Çevrimiçi", state.tunnelState)
        assertTrue(state.isTunnelOnline)
        assertEquals("14.2 GB", state.storageUsed)
        assertEquals("48.6 GB", state.storageFree)
        assertEquals("4x AVM Aktif", state.cameraStatus)
        assertEquals("%98 Sağlık", state.batterySoh)
        assertTrue(state.isBatteryGood)
    }

    @Test
    fun testUpdateUiStateDirectMutation() {
        viewModel.updateUiState {
            it.copy(
                dtcCodes = listOf(DtcDictionary.lookup("P0A1F")),
                actionFeedbackMessage = "Test mesajı"
            )
        }

        val state = viewModel.uiState.value
        assertEquals(1, state.dtcCodes.size)
        assertEquals("P0A1F", state.dtcCodes[0].code)
        assertEquals("Test mesajı", state.actionFeedbackMessage)

        viewModel.dismissFeedback()
        assertNull(viewModel.uiState.value.actionFeedbackMessage)
    }
}
