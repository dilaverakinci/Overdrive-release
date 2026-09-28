package com.overdrive.app.ui.parking

import android.app.Application
import android.graphics.Bitmap
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    // In-memory 16MB image cache for thumbnails, mosaics and frames
    val imageCache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    init {
        loadData()
        startPeriodicRefresh()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            withContext(Dispatchers.IO) {
                val (cur, cfg, running) = ParkingApiClient.fetchStatus()
                val (geoEnabled, geoOnline) = ParkingApiClient.fetchGeocoding()
                val sessions = ParkingApiClient.fetchSessions(_uiState.value.filterRange.days)

                _uiState.update {
                    it.copy(
                        isRunning = running,
                        currentSession = cur,
                        config = cfg,
                        geocodingEnabled = geoEnabled,
                        geocodingOnline = geoOnline,
                        sessions = sessions,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setTab(tab: ParkingTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == ParkingTab.SETTINGS) {
            refreshSettings()
        }
    }

    fun setFilterRange(range: ParkingFilterRange) {
        _uiState.update { it.copy(filterRange = range, isLoading = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val sessions = ParkingApiClient.fetchSessions(range.days)
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
        }
    }

    fun selectSession(sessionId: String?) {
        if (sessionId == null) {
            _uiState.update { it.copy(selectedSessionId = null, detailData = null) }
            return
        }
        _uiState.update { it.copy(selectedSessionId = sessionId, isDetailLoading = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val detail = ParkingApiClient.fetchSessionDetail(sessionId)
                _uiState.update { it.copy(detailData = detail, isDetailLoading = false) }
            }
        }
    }

    fun deleteSession(sessionId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                ParkingApiClient.deleteSession(sessionId)
            }
            if (success) {
                _uiState.update { state ->
                    state.copy(
                        selectedSessionId = null,
                        detailData = null,
                        sessions = state.sessions.filter { it.sessionId != sessionId },
                        toastMessage = "deleted"
                    )
                }
                onDone()
            } else {
                _uiState.update { it.copy(toastMessage = "delete_failed") }
            }
        }
    }

    fun requeueSignage(sessionId: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                ParkingApiClient.requeueSignage(sessionId)
            }
            _uiState.update { it.copy(toastMessage = if (ok) "signage_queued" else "requeue_failed") }
            if (ok) {
                delay(1200)
                selectSession(sessionId)
            }
        }
    }

    fun updateConfig(cfg: ParkingConfigModel) {
        _uiState.update { it.copy(config = cfg) }
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                ParkingApiClient.saveConfig(cfg)
            }
            if (success) {
                _uiState.update { it.copy(toastMessage = "saved") }
                delay(600)
                refreshSettings()
            }
        }
    }

    fun updateGeocoding(enabled: Boolean, allowOnline: Boolean) {
        _uiState.update { it.copy(geocodingEnabled = enabled, geocodingOnline = allowOnline) }
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                ParkingApiClient.saveGeocoding(enabled, allowOnline)
            }
            if (success) {
                _uiState.update { it.copy(toastMessage = "saved") }
            }
        }
    }

    fun refreshSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val (_, cfg, running) = ParkingApiClient.fetchStatus()
            val (geoEnabled, geoOnline) = ParkingApiClient.fetchGeocoding()
            _uiState.update {
                it.copy(
                    isRunning = running,
                    config = cfg,
                    geocodingEnabled = geoEnabled,
                    geocodingOnline = geoOnline
                )
            }
        }
    }

    fun setLightboxUrl(url: String?) {
        _uiState.update { it.copy(lightboxUrl = url) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun loadBitmap(urlOrPath: String, onLoaded: (Bitmap?) -> Unit) {
        val cached = imageCache.get(urlOrPath)
        if (cached != null) {
            onLoaded(cached)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val bmp = ParkingApiClient.fetchBitmap(urlOrPath)
            if (bmp != null) {
                imageCache.put(urlOrPath, bmp)
            }
            withContext(Dispatchers.Main) {
                onLoaded(bmp)
            }
        }
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                if (_uiState.value.selectedSessionId == null && _uiState.value.activeTab == ParkingTab.SESSIONS) {
                    withContext(Dispatchers.IO) {
                        val (cur, _, running) = ParkingApiClient.fetchStatus()
                        _uiState.update {
                            it.copy(
                                isRunning = running,
                                currentSession = cur
                            )
                        }
                    }
                }
            }
        }
    }
}
