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

    private val _sessions = MutableStateFlow<List<ParkingSession>>(emptyList())
    val sessions: StateFlow<List<ParkingSession>> = _sessions.asStateFlow()

    private val _selectedDays = MutableStateFlow(30)
    val selectedDays: StateFlow<Int> = _selectedDays.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadStatus()
            loadSessions(_selectedDays.value)
            _isLoading.value = false
        }
    }

    private suspend fun loadStatus() {
        val res = repository.getStatus()
        if (res.isSuccess) {
            _status.value = res.getOrNull()
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
            }
            onComplete?.invoke(success)
        }
    }
}
