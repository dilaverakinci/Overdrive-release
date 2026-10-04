package com.overdrive.app.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.monitor.GpsMonitor
import com.overdrive.app.server.StreamingApiHandler
import com.overdrive.app.ui.vehicle.VehicleTopDownArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LiveViewViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(LiveViewUiState())
    val uiState: StateFlow<LiveViewUiState> = _uiState.asStateFlow()

    init {
        // Load selected vehicle model
        loadVehicleModel()

        // Restore last desired view mode if previously selected, defaulting to FRONT for instant single-camera stream
        val lastModeId = StreamingApiHandler.getLastDesiredViewMode()
        val initialMode = if (lastModeId in 1..6) {
            LiveCameraMode.fromId(lastModeId)
        } else {
            LiveCameraMode.FRONT
        }
        _uiState.update { it.copy(activeCamera = initialMode) }

        // Feature-gate OEM Dashcam on car diagram
        val hasOem = try {
            UnifiedConfigManager.isAnyOemDashcamTriggerEnabled()
        } catch (_: Throwable) {
            false
        }
        _uiState.update { it.copy(hasOemDashcam = hasOem) }

        // Periodically refresh GPS fix snapshot
        viewModelScope.launch {
            while (isActive) {
                refreshGpsLocation()
                delay(3000)
            }
        }

        // Fetch streaming status & quality from backend, and start streaming initial camera immediately
        viewModelScope.launch {
            selectCamera(initialMode)
            fetchInitialStatus()
        }
    }

    private fun loadVehicleModel() {
        val modelId = UnifiedConfigManager.getSelectedVehicleModelId()
            ?: UnifiedConfigManager.getVehicle().optString("modelId", "seal").ifEmpty { "seal" }
        val name = VehicleTopDownArt.displayNameFor(modelId)
        _uiState.update { it.copy(vehicleModelId = modelId, vehicleModelName = name) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8080/api/models/selected")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val effectiveId = if (json.has("selectedModelId") && !json.isNull("selectedModelId")) {
                        json.optString("selectedModelId", modelId)
                    } else {
                        json.optString("modelId", modelId)
                    }
                    val effectiveName = VehicleTopDownArt.displayNameFor(effectiveId)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(vehicleModelId = effectiveId, vehicleModelName = effectiveName) }
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {
                // Fallback already set
            }
        }
    }

    private fun refreshGpsLocation() {
        try {
            val fix = GpsMonitor.getInstance().fixSnapshot
            if (fix != null && fix.hasLocation()) {
                val diffMs = System.currentTimeMillis() - fix.lastUpdate
                val ageSec = if (diffMs > 0) diffMs / 1000L else 0L
                val freshness = if (ageSec < 5L) {
                    "Şimdi"
                } else if (ageSec < 60L) {
                    "${ageSec}s önce"
                } else {
                    "${ageSec / 60L}dk önce"
                }

                _uiState.update { current ->
                    current.copy(
                        vehicleLatitude = fix.latitude,
                        vehicleLongitude = fix.longitude,
                        vehicleHeading = fix.heading,
                        lastGpsUpdateText = freshness
                    )
                }
            }
        } catch (_: Throwable) {
            // Ignore if GpsMonitor not initialized
        }
    }

    private suspend fun fetchInitialStatus() {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8080/api/stream/status")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 1500
                conn.readTimeout = 1500
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(text)
                    val active = json.optBoolean("active", true)
                    val fps = json.optInt("fps", 15)
                    val modeId = json.optInt("viewMode", 0)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                connectionState = if (active) StreamConnectionState.LIVE else StreamConnectionState.IDLE,
                                fps = fps,
                                activeCamera = LiveCameraMode.fromId(modeId)
                            )
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {
                // Daemon starting or not available
            }
        }
    }

    fun selectCamera(mode: LiveCameraMode) {
        _uiState.update { it.copy(activeCamera = mode, connectionState = StreamConnectionState.CONNECTING) }
        StreamingApiHandler.setLastDesiredViewMode(mode.id)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8080/api/stream/view/${mode.id}")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                // Handled gracefully
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(connectionState = StreamConnectionState.LIVE) }
            }
        }
    }

    fun setQuality(quality: StreamQuality) {
        _uiState.update { it.copy(selectedQuality = quality, fps = quality.fps) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8080/api/stream/quality/${quality.key}")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 2000
                conn.readTimeout = 2000
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                // Handled gracefully
            }
        }
    }

    fun requestDeterrent(kind: String) {
        _uiState.update { it.copy(confirmDeterrentKind = kind) }
    }

    fun dismissDeterrentDialog() {
        _uiState.update { it.copy(confirmDeterrentKind = null) }
    }

    fun confirmDeterrent() {
        val kind = _uiState.value.confirmDeterrentKind ?: return
        _uiState.update { it.copy(confirmDeterrentKind = null) }

        val mode = when (kind) {
            "horn", "sound" -> com.overdrive.app.surveillance.deterrent.DeterrentMode.SOUND_ONLY
            "flash", "lights" -> com.overdrive.app.surveillance.deterrent.DeterrentMode.LIGHTS_ONLY
            "full" -> com.overdrive.app.surveillance.deterrent.DeterrentMode.FULL_DETERRENT
            else -> com.overdrive.app.surveillance.deterrent.DeterrentMode.SOUND_ONLY
        }

        // Trigger local coordinator immediately
        try {
            com.overdrive.app.surveillance.deterrent.SentryDeterrentCoordinator.getInstance()
                .triggerDeterrent(mode, force = true)
        } catch (_: Throwable) {}

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val endpoint = if (kind == "horn" || kind == "sound") "horn" else if (kind == "flash" || kind == "lights") "flash" else "full"
                val url = URL("http://127.0.0.1:8080/api/vehicle/deterrent/$endpoint")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {
                // Cloud / hardware fallback
            }
        }
    }

    fun toggleCabinListening() {
        _uiState.update { it.copy(isCabinListening = !it.isCabinListening) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun toggleMapExpanded() {
        _uiState.update { it.copy(isMapExpanded = !it.isMapExpanded) }
    }
}
