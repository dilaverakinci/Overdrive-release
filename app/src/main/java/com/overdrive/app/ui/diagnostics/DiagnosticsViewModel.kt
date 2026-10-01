package com.overdrive.app.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.domain.diagnostics.DiagnosticsRepository
import com.overdrive.app.domain.diagnostics.EcuType
import com.overdrive.app.domain.diagnostics.dtc.DefaultDtcDiagnosticService
import com.overdrive.app.domain.diagnostics.dtc.DtcCode
import com.overdrive.app.domain.diagnostics.dtc.DtcDiagnosticService
import com.overdrive.app.domain.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing reactive 9-ECU diagnostics, live telemetry feeds, and OBD-II DTC operations.
 */
class DiagnosticsViewModel(
    private val diagnosticsRepo: DiagnosticsRepository = RepositoryProvider.diagnosticsRepository,
    private val dtcService: DtcDiagnosticService = DefaultDtcDiagnosticService(),
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val vmScope: CoroutineScope
        get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        // Collect reactive telemetry snapshots from the repository
        vmScope.launch {
            diagnosticsRepo.ecuSnapshots.collect { snapshots ->
                _uiState.update { it.copy(ecuSnapshots = snapshots) }
            }
        }
        vmScope.launch {
            diagnosticsRepo.overallHealth.collect { health ->
                _uiState.update { it.copy(overallHealth = health) }
            }
        }
        vmScope.launch {
            diagnosticsRepo.bmsTelemetry.collect { bms ->
                _uiState.update { it.copy(bmsTelemetry = bms) }
            }
        }

        // Initial background DTC scan
        scanDtc()
    }

    fun selectTab(tab: DiagnosticsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleEcuDetail(ecu: EcuType) {
        _uiState.update {
            it.copy(selectedEcuForDetail = if (it.selectedEcuForDetail == ecu) null else ecu)
        }
    }

    fun scanDtc() {
        vmScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isScanningDtc = true, actionFeedbackMessage = null) }
            try {
                val codes = dtcService.scanDtcCodes()
                _uiState.update {
                    it.copy(
                        dtcCodes = codes,
                        isScanningDtc = false,
                        actionFeedbackMessage = if (codes.isEmpty()) "Tarama tamamlandı: Aktif arıza kodu bulunamadı."
                        else "Tarama tamamlandı: ${codes.size} adet arıza kodu tespit edildi."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanningDtc = false,
                        actionFeedbackMessage = "DTC tarama hatası: ${e.message}"
                    )
                }
            }
        }
    }

    fun requestClearDtc() {
        _uiState.update { it.copy(showClearDtcConfirmDialog = true) }
    }

    fun dismissClearDtcDialog() {
        _uiState.update { it.copy(showClearDtcConfirmDialog = false) }
    }

    fun confirmClearDtc() {
        _uiState.update { it.copy(showClearDtcConfirmDialog = false, isClearingDtc = true) }
        vmScope.launch(Dispatchers.IO) {
            try {
                val result = dtcService.clearDtcCodes()
                _uiState.update {
                    it.copy(
                        isClearingDtc = false,
                        dtcCodes = emptyList(),
                        actionFeedbackMessage = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isClearingDtc = false,
                        actionFeedbackMessage = e.message ?: "DTC silme işlemi başarısız oldu."
                    )
                }
            }
        }
    }

    fun updateSystemHealth(
        networkSsid: String? = null,
        tunnelState: String? = null,
        isTunnelOnline: Boolean? = null,
        storageUsed: String? = null,
        storageFree: String? = null,
        cameraStatus: String? = null,
        isCameraOnline: Boolean? = null,
        batterySoh: String? = null,
        isBatteryGood: Boolean? = null,
        isBatteryReviewNeeded: Boolean? = null
    ) {
        _uiState.update { current ->
            current.copy(
                networkSsid = networkSsid ?: current.networkSsid,
                tunnelState = tunnelState ?: current.tunnelState,
                isTunnelOnline = isTunnelOnline ?: current.isTunnelOnline,
                storageUsed = storageUsed ?: current.storageUsed,
                storageFree = storageFree ?: current.storageFree,
                cameraStatus = cameraStatus ?: current.cameraStatus,
                isCameraOnline = isCameraOnline ?: current.isCameraOnline,
                batterySoh = batterySoh ?: current.batterySoh,
                isBatteryGood = isBatteryGood ?: current.isBatteryGood,
                isBatteryReviewNeeded = isBatteryReviewNeeded ?: current.isBatteryReviewNeeded
            )
        }
    }

    fun updateUiState(transform: (DiagnosticsUiState) -> DiagnosticsUiState) {
        _uiState.update(transform)
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(actionFeedbackMessage = null) }
    }
}
