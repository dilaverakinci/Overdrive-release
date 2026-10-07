package com.overdrive.app.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class VehicleViewModel(
    private val repository: VehicleRepository = VehicleRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VehicleState())
    val state: StateFlow<VehicleState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow<VehicleCategoryTab?>(null)
    val selectedTab: StateFlow<VehicleCategoryTab?> = _selectedTab.asStateFlow()

    private val _isCommandPending = MutableStateFlow(false)
    val isCommandPending: StateFlow<Boolean> = _isCommandPending.asStateFlow()

    private var pollingJob: Job? = null
    private var refreshJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                doRefresh()
                delay(2500)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            doRefresh()
        }
    }

    private suspend fun doRefresh() {
        val model = repository.getSelectedModelId()
        if (model != _state.value.modelId) {
            _state.value = _state.value.copy(modelId = model)
        }
        val result = repository.getVehicleState(model)
        result.onSuccess { newState ->
            _state.value = newState
        }
    }

    fun selectModel(modelId: String?, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isCommandPending.value = true
            val success = repository.selectModel(modelId)
            _isCommandPending.value = false
            if (success) {
                _state.value = _state.value.copy(modelId = modelId)
                doRefresh()
            }
            onComplete?.invoke(success)
        }
    }

    suspend fun getAvailableModels(): List<Pair<String, String>> {
        return repository.getAvailableModels()
    }

    fun toggleTab(tab: VehicleCategoryTab) {
        if (_selectedTab.value == tab) {
            _selectedTab.value = null // collapse
        } else {
            _selectedTab.value = tab // expand
        }
    }

    fun closePanel() {
        _selectedTab.value = null
    }

    private fun executeCommand(path: String, payload: JSONObject? = null, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _isCommandPending.value = true
            val res = repository.postCommand(path, payload)
            _isCommandPending.value = false
            onComplete?.invoke(res.isSuccess)
            refresh()
        }
    }

    // Security
    fun lock(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/lock", null, onComplete)

    fun unlock(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/unlock", null, onComplete)

    fun flash(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/flash", null, onComplete)

    fun findCar(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/find-car", null, onComplete)

    // Trunk
    fun openTrunk(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/trunk", JSONObject().put("action", "open"), onComplete)

    fun closeTrunk(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/trunk", JSONObject().put("action", "close"), onComplete)

    // Climate
    fun setAc(power: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/climate", JSONObject().put("power", if (power) 1 else 0), onComplete)

    fun adjustTargetTemp(delta: Double, onComplete: ((Boolean) -> Unit)? = null) {
        val cur = _state.value.climate.targetTempC
        val next = (cur + delta).coerceIn(16.0, 32.0)
        executeCommand("/api/vehicle/climate", JSONObject().put("targetTemp", next), onComplete)
    }

    fun setBatteryHeat(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/battery-heat", JSONObject().put("enabled", enabled), onComplete)

    // Seats
    fun setDriverSeatHeat(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "driver").put("heat", level), onComplete)

    fun setDriverSeatCool(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "driver").put("cool", level), onComplete)

    fun setPassengerSeatHeat(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "passenger").put("heat", level), onComplete)

    fun setPassengerSeatCool(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "passenger").put("cool", level), onComplete)

    fun setSteeringHeat(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("steeringHeat", enabled), onComplete)

    // Windows
    fun ventAllWindows(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/window", JSONObject().put("action", "vent"), onComplete)

    fun closeAllWindows(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/window", JSONObject().put("area", 0).put("command", 1), onComplete)

    fun openAllWindows(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/window", JSONObject().put("area", 0).put("command", 2), onComplete)

    // Lights
    fun setDaytimeLights(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/lights", JSONObject().put("target", "dayTimeLight").put("enable", enabled), onComplete)

    fun setAmbientLights(colorIndex: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/lights", JSONObject().put("target", "ambientColour").put("value", colorIndex), onComplete)

    // ADAS
    fun setSpeedLimitWarning(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/adas", JSONObject().put("target", "speedLimitWarning").put("enable", enabled), onComplete)

    fun setChildPresence(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/setting", JSONObject().put("target", "childPresenceDetection").put("value", if (enabled) 1 else 2), onComplete)

    // Charging
    fun startCharging(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/start-charging", JSONObject(), onComplete)

    fun setChargeCap(percent: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/charge-cap", JSONObject().put("percent", percent), onComplete)

    fun setAcCurrentLimit(state: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/ac-charge-current-limit", JSONObject().put("state", state), onComplete)

    // Audio
    fun setAvasTone(pattern: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/audio/avas-tone", JSONObject().put("pattern", pattern), onComplete)

    fun stopAvasTone(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/audio/avas-tone", JSONObject().put("stop", true), onComplete)

    fun setEngineSound(enabled: Boolean, preset: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/audio/engine-sound", JSONObject().put("on", enabled).put("preset", preset), onComplete)

    // System
    fun rebootIvi(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/system/ivi-reboot", JSONObject(), onComplete)
}
