package com.overdrive.app.ui.parking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParkingViewModel(
    private val repository: ParkingRepository = ParkingRepository()
) : ViewModel() {

    private val _status = MutableStateFlow<ParkingStatus?>(null)
    val status: StateFlow<ParkingStatus?> = _status.asStateFlow()

    private val _geocoding = MutableStateFlow<GeocodingConfig?>(null)
    val geocoding: StateFlow<GeocodingConfig?> = _geocoding.asStateFlow()

    private val _sessions = MutableStateFlow<List<ParkingSession>>(emptyList())
    val sessions: StateFlow<List<ParkingSession>> = _sessions.asStateFlow()

    private val _selectedDays = MutableStateFlow(30)
    val selectedDays: StateFlow<Int> = _selectedDays.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _detail = MutableStateFlow<ParkingDetail?>(null)
    val detail: StateFlow<ParkingDetail?> = _detail.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadStatus()
            loadGeocoding()
            loadSessions(_selectedDays.value)
            _isLoading.value = false
        }
    }

    suspend fun loadStatus() {
        val res = repository.getStatus()
        if (res.isSuccess) {
            _status.value = res.getOrNull()
        }
    }

    suspend fun loadGeocoding() {
        val res = repository.getGeocodingConfig()
        if (res.isSuccess) {
            _geocoding.value = res.getOrNull()
        }
    }

    private suspend fun loadSessions(days: Int) {
        val res = repository.getSessions(days)
        if (res.isSuccess) {
            _sessions.value = res.getOrDefault(emptyList())
        }
    }

    fun filterByDays(days: Int) {
        _selectedDays.value = days
        viewModelScope.launch {
            _isLoading.value = true
            loadSessions(days)
            _isLoading.value = false
        }
    }

    fun saveConfig(newConfig: ParkingConfig, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.saveConfig(newConfig)
            val success = res.getOrDefault(false)
            if (success) {
                _status.value = _status.value?.copy(config = newConfig, enabled = newConfig.enabled)
                // Reload status to sync daemon state
                loadStatus()
            }
            onComplete?.invoke(success)
        }
    }

    fun saveGeocoding(enabled: Boolean, allowOnline: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        val next = GeocodingConfig(enabled = enabled, allowOnline = if (enabled) allowOnline else false, inherited = false)
        viewModelScope.launch {
            val res = repository.saveGeocodingConfig(next)
            val success = res.getOrDefault(false)
            if (success) {
                _geocoding.value = next
            }
            onComplete?.invoke(success)
        }
    }

    fun openSessionDetail(sessionId: String) {
        viewModelScope.launch {
            _detailLoading.value = true
            val res = repository.getSessionDetail(sessionId)
            if (res.isSuccess) {
                _detail.value = res.getOrNull()
            }
            _detailLoading.value = false
        }
    }

    fun closeSessionDetail() {
        _detail.value = null
    }

    fun requeueSignage(sessionId: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.requeueSignage(sessionId)
            val success = res.getOrDefault(false)
            if (success) {
                openSessionDetail(sessionId)
            }
            onComplete?.invoke(success)
        }
    }

    fun deleteSession(sessionId: String, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val res = repository.deleteSession(sessionId)
            val success = res.getOrDefault(false)
            if (success) {
                _sessions.value = _sessions.value.filter { it.id != sessionId }
                if (_detail.value?.session?.id == sessionId) {
                    _detail.value = null
                }
            }
            onComplete?.invoke(success)
        }
    }
}
