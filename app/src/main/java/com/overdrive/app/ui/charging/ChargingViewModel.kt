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
import org.json.JSONObject

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
                val tariffsPayload = data.tariffsPayload
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        summary = data.summary,
                        sessions = data.sessions,
                        config = data.config,
                        socHistory = data.socHistory,
                        tariffs = tariffsPayload?.tariffs ?: current.tariffs,
                        defaultTariffId = tariffsPayload?.defaultTariffId ?: current.defaultTariffId,
                        matchedTariffId = tariffsPayload?.matchedTariffId ?: current.matchedTariffId,
                        currentGpsLat = tariffsPayload?.lat ?: current.currentGpsLat,
                        currentGpsLng = tariffsPayload?.lng ?: current.currentGpsLng,
                        error = null
                    )
                }
                if (tariffsPayload == null) {
                    loadTariffs()
                }
            }.onFailure { err ->
                Log.w(TAG, "Failed to load bootstrap data: ${err.message}")
                loadTariffs()
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

    fun loadSocHistory(hours: Int) {
        viewModelScope.launch {
            val result = repository.getSocHistory(hours)
            result.onSuccess { points ->
                _uiState.update { it.copy(socHistory = points, socHours = hours) }
            }.onFailure { err ->
                Log.w(TAG, "Failed to load SoC history for $hours hours: ${err.message}")
            }
        }
    }

    fun getLatestBatterySnapshot(): Triple<Double?, Double?, Double?> {
        var soc: Double? = null
        var range: Double? = null
        var soh: Double? = null
        val history = _uiState.value.socHistory
        for (i in history.indices.reversed()) {
            val item = history[i]
            if (soc == null && item.soc in 0.0..100.0) {
                soc = item.soc
            }
            if (range == null && item.range != null && item.range >= 0.0) {
                range = item.range
            }
            if (soh == null && item.soh != null && item.soh in 1.0..100.0) {
                soh = item.soh
            }
            if (soc != null && range != null && soh != null) break
        }
        return Triple(soc, range, soh)
    }

    fun loadTariffs() {
        viewModelScope.launch {
            repository.getTariffs().onSuccess { payload ->
                _uiState.update { current ->
                    current.copy(
                        tariffs = payload.tariffs,
                        defaultTariffId = payload.defaultTariffId,
                        matchedTariffId = payload.matchedTariffId,
                        currentGpsLat = payload.lat ?: current.currentGpsLat,
                        currentGpsLng = payload.lng ?: current.currentGpsLng
                    )
                }
            }.onFailure { err ->
                Log.w(TAG, "loadTariffs error: ${err.message}")
            }
        }
    }

    fun openTariffEditor(tariff: LocationTariff? = null) {
        if (tariff == null) {
            loadTariffs()
        }
        _uiState.update {
            it.copy(
                isTariffEditorOpen = true,
                editingTariff = tariff,
                tariffError = null
            )
        }
    }

    fun closeTariffEditor() {
        _uiState.update {
            it.copy(
                isTariffEditorOpen = false,
                editingTariff = null,
                tariffError = null
            )
        }
    }

    fun setTariffError(error: String?) {
        _uiState.update { it.copy(tariffError = error) }
    }

    fun saveTariff(
        label: String,
        acRate: Double,
        dcRate: Double,
        radiusM: Int,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingTariff
            val currency = _uiState.value.config.currency.ifEmpty { "$" }
            val body = JSONObject().apply {
                put("label", label.trim())
                put("acRate", acRate)
                put("dcRate", dcRate)
                put("radiusM", radiusM)
                put("currency", currency)
                if (editing != null) {
                    put("id", editing.id)
                } else {
                    val lat = _uiState.value.currentGpsLat
                    val lng = _uiState.value.currentGpsLng
                    if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                        put("lat", lat)
                        put("lng", lng)
                    }
                }
            }
            _uiState.update { it.copy(isTariffSaving = true, tariffError = null) }
            val result = repository.saveTariff(body)
            _uiState.update { it.copy(isTariffSaving = false) }
            result.onSuccess {
                closeTariffEditor()
                loadTariffs()
                loadData()
                onComplete(true, null)
            }.onFailure { err ->
                val msg = err.message ?: "Failed to save tariff"
                _uiState.update { it.copy(tariffError = msg) }
                onComplete(false, msg)
            }
        }
    }

    fun deleteTariff(tariff: LocationTariff, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteTariff(tariff.id)
            val success = result.getOrDefault(false)
            if (success) {
                loadTariffs()
                loadData()
            }
            onComplete(success)
        }
    }

    fun setDefaultTariff(tariff: LocationTariff, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val nextId = if (tariff.id == _uiState.value.defaultTariffId) "" else tariff.id
            val result = repository.setDefaultTariff(nextId)
            val success = result.getOrDefault(false)
            if (success) {
                loadTariffs()
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
