package com.overdrive.app.ui.charging

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class ChargingViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ChargingRepository = ChargingRepository()
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChargingViewModel"
        private const val POLL_INTERVAL_MS = 6000L
    }

    private val _uiState = MutableStateFlow(ChargingUiState())
    val uiState: StateFlow<ChargingUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val days = _uiState.value.periodFilter.days
            val result = repository.getBootstrap(days)
            result.onSuccess { data ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        summary = data.summary,
                        sessions = data.sessions,
                        config = data.config,
                        socHistory = data.socHistory,
                        error = null
                    )
                }
            }.onFailure { err ->
                Log.w(TAG, "Failed to load bootstrap data: ${err.message}")
                // Fallback to overview
                val overviewResult = repository.getOverview(days)
                overviewResult.onSuccess { (summary, sessions) ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            summary = summary,
                            sessions = sessions,
                            error = null
                        )
                    }
                }.onFailure { overviewErr ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            error = overviewErr.message ?: "Failed to load charging data"
                        )
                    }
                }
            }
        }
    }

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refreshOverviewSilently()
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private suspend fun refreshOverviewSilently() {
        val days = _uiState.value.periodFilter.days
        val result = repository.getOverview(days)
        result.onSuccess { (summary, sessions) ->
            _uiState.update { current ->
                val updatedSelectedSession = if (current.selectedSession != null) {
                    sessions.find { it.id == current.selectedSession.id } ?: current.selectedSession
                } else null

                current.copy(
                    summary = summary,
                    sessions = sessions,
                    selectedSession = updatedSelectedSession
                )
            }
        }
    }

    fun selectTab(tab: ChargingTab) {
        _uiState.update { it.copy(currentTab = tab, isDetailOpen = false) }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        if (_uiState.value.periodFilter == filter) return
        _uiState.update { it.copy(periodFilter = filter) }
        loadData()
    }

    fun openSessionDetail(session: ChargingSession) {
        _uiState.update {
            it.copy(
                isDetailOpen = true,
                selectedSession = session,
                isDetailLoading = true,
                selectedSessionSamples = emptyList()
            )
        }
        viewModelScope.launch {
            val result = repository.getSessionDetail(session.id)
            result.onSuccess { (detailSession, samples) ->
                _uiState.update {
                    if (it.selectedSession?.id == session.id) {
                        it.copy(
                            selectedSession = detailSession,
                            selectedSessionSamples = samples,
                            isDetailLoading = false
                        )
                    } else it
                }
            }.onFailure { err ->
                Log.w(TAG, "Failed to load session details: ${err.message}")
                _uiState.update { it.copy(isDetailLoading = false) }
            }
        }
    }

    fun closeSessionDetail() {
        _uiState.update {
            it.copy(
                isDetailOpen = false,
                selectedSession = null,
                selectedSessionSamples = emptyList()
            )
        }
    }

    fun updateCost(sessionId: Long, newCost: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateCost(sessionId, newCost)
            val success = result.getOrDefault(false)
            if (success) {
                // Refresh detail and list
                val detailResult = repository.getSessionDetail(sessionId)
                detailResult.onSuccess { (detailSession, samples) ->
                    _uiState.update { current ->
                        val updatedList = current.sessions.map {
                            if (it.id == sessionId) detailSession else it
                        }
                        current.copy(
                            selectedSession = detailSession,
                            selectedSessionSamples = samples,
                            sessions = updatedList
                        )
                    }
                }
                refreshOverviewSilently()
            }
            onComplete(success)
        }
    }

    fun deleteSession(sessionId: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteSession(sessionId)
            val success = result.getOrDefault(false)
            if (success) {
                closeSessionDetail()
                loadData()
            }
            onComplete(success)
        }
    }

    fun clearHistory(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.clearHistory()
            val success = result.getOrDefault(false)
            if (success) {
                closeSessionDetail()
                loadData()
            }
            onComplete(success)
        }
    }

    fun saveConfig(
        enabled: Boolean,
        electricityRate: Double,
        currency: String,
        dcRate: Double,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val newConfig = ChargingConfigData(
                enabled = enabled,
                electricityRate = electricityRate,
                currency = currency,
                dcRate = dcRate,
                fastSampleSec = _uiState.value.config.fastSampleSec
            )
            val result = repository.saveConfig(newConfig)
            val success = result.getOrDefault(false)
            if (success) {
                _uiState.update { it.copy(config = newConfig) }
                loadData()
            }
            onComplete(success)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
