package com.overdrive.app.ui.cockpit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.ui.dashboard.DashboardStateReducer
import com.overdrive.app.ui.dashboard.DashboardUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel managing cockpit dashboard state using StateFlow.
 * Eliminates background threads leaking across fragment views and provides
 * low-overhead reactive state updates for in-vehicle head units.
 */
class CockpitViewModel(
    application: Application,
    private val repository: CockpitVehicleRepository,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        CockpitVehicleRepository(application.applicationContext),
    )

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var todayClips: Int? = null
    private var storageSummary: DashboardUiState.StorageSummary? = null

    companion object {
        private const val POLLING_INTERVAL_MS = 5_000L
    }

    fun refreshVehicleStatus(showLoading: Boolean = false) {
        if (showLoading && _uiState.value.vehicle !is DashboardUiState.VehicleState.Ready) {
            _uiState.value = DashboardStateReducer.statusLoading(_uiState.value)
        }
        viewModelScope.launch {
            val result = repository.fetchVehicleStatus()
            _uiState.value = DashboardStateReducer.status(_uiState.value, result)
        }
    }

    fun refreshRecordingsCount() {
        viewModelScope.launch {
            todayClips = repository.fetchTodayClipCount()
            _uiState.value = DashboardStateReducer.recordings(
                _uiState.value,
                todayClips,
                storageSummary
            )
        }
    }

    fun updateStorageSummary(storage: DashboardUiState.StorageSummary?) {
        storageSummary = storage
        _uiState.value = DashboardStateReducer.recordings(
            _uiState.value,
            todayClips,
            storageSummary
        )
    }

    fun setRemoteExpanded(expanded: Boolean) {
        _uiState.value = DashboardStateReducer.remoteExpanded(_uiState.value, expanded)
    }

    fun setActivityRows(rows: List<DashboardUiState.ActivityRow>?) {
        _uiState.value = DashboardStateReducer.activity(_uiState.value, rows)
    }

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            // Immediate initial load
            refreshVehicleStatus(showLoading = false)
            refreshRecordingsCount()

            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                val result = repository.fetchVehicleStatus()
                _uiState.value = DashboardStateReducer.status(_uiState.value, result)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }
}
