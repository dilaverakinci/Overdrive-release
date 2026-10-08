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
        executeCommand("/api/vehicle/climate", JSONObject().put("action", "set_temp").put("zone", 1).put("temp", next).put("targetTemp", next)) { success ->
            if (success) {
                _state.value = _state.value.copy(climate = _state.value.climate.copy(targetTempC = next))
            }
            onComplete?.invoke(success)
        }
    }

    fun adjustFanSpeed(delta: Int, onComplete: ((Boolean) -> Unit)? = null) {
        val cur = _state.value.climate.fanSpeed
        val next = (cur + delta).coerceIn(1, 7)
        executeCommand("/api/vehicle/climate", JSONObject().put("action", "set_fan").put("fan", next).put("fanSpeed", next)) { success ->
            if (success) {
                _state.value = _state.value.copy(climate = _state.value.climate.copy(fanSpeed = next))
            }
            onComplete?.invoke(success)
        }
    }

    fun setBatteryHeat(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/battery-heat", JSONObject().put("enabled", enabled)) { success ->
            if (success) {
                _state.value = _state.value.copy(climate = _state.value.climate.copy(batteryHeat = enabled))
            }
            onComplete?.invoke(success)
        }

    // Seats
    fun setDriverSeatHeat(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "driver").put("heat", level)) { success ->
            if (success) {
                _state.value = _state.value.copy(seats = _state.value.seats.copy(driverHeat = level))
            }
            onComplete?.invoke(success)
        }

    fun setDriverSeatCool(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "driver").put("cool", level)) { success ->
            if (success) {
                _state.value = _state.value.copy(seats = _state.value.seats.copy(driverCool = level))
            }
            onComplete?.invoke(success)
        }

    fun setPassengerSeatHeat(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "passenger").put("heat", level)) { success ->
            if (success) {
                _state.value = _state.value.copy(seats = _state.value.seats.copy(passengerHeat = level))
            }
            onComplete?.invoke(success)
        }

    fun setPassengerSeatCool(level: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("seat", "passenger").put("cool", level)) { success ->
            if (success) {
                _state.value = _state.value.copy(seats = _state.value.seats.copy(passengerCool = level))
            }
            onComplete?.invoke(success)
        }

    fun setSteeringHeat(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("steeringHeat", enabled)) { success ->
            if (success) {
                _state.value = _state.value.copy(seats = _state.value.seats.copy(steeringHeat = enabled))
            }
            onComplete?.invoke(success)
        }

    fun recallSeatPosition(pos: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("action", "position").put("position", pos), onComplete)

    fun saveSeatPosition(pos: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/seat", JSONObject().put("action", "save").put("position", pos), onComplete)

    // Windows
    fun setWindowPosition(area: Int, percent: Int, onComplete: ((Boolean) -> Unit)? = null) {
        val key = when (area) {
            1 -> "lf"
            2 -> "rf"
            3 -> "lr"
            4 -> "rr"
            5 -> "sunroof"
            6 -> "sunshade"
            else -> null
        }
        executeCommand("/api/vehicle/window", JSONObject().put("area", area).put("targetPercent", percent)) { success ->
            if (success && key != null) {
                val updated = _state.value.windowPercent.toMutableMap()
                updated[key] = percent
                _state.value = _state.value.copy(windowPercent = updated)
            }
            onComplete?.invoke(success)
        }
    }

    fun ventAllWindows(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/window", JSONObject().put("action", "vent")) { success ->
            if (success) {
                val updated = _state.value.windowPercent.toMutableMap()
                for (k in listOf("lf", "rf", "lr", "rr")) updated[k] = 15
                _state.value = _state.value.copy(windowPercent = updated)
            }
            onComplete?.invoke(success)
        }

    fun closeAllWindows(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/window", JSONObject().put("area", 0).put("command", 2)) { success ->
            if (success) {
                val updated = _state.value.windowPercent.toMutableMap()
                for (k in listOf("lf", "rf", "lr", "rr", "sunroof", "sunshade")) updated[k] = 0
                _state.value = _state.value.copy(windowPercent = updated)
            }
            onComplete?.invoke(success)
        }

    fun openAllWindows(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/window", JSONObject().put("area", 0).put("command", 1)) { success ->
            if (success) {
                val updated = _state.value.windowPercent.toMutableMap()
                for (k in listOf("lf", "rf", "lr", "rr", "sunroof", "sunshade")) updated[k] = 100
                _state.value = _state.value.copy(windowPercent = updated)
            }
            onComplete?.invoke(success)
        }

    // Lights
    fun setDaytimeLights(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/lights", JSONObject().put("target", "dayTimeLight").put("enable", enabled)) { success ->
            if (success) {
                _state.value = _state.value.copy(lights = _state.value.lights.copy(daytimeLight = enabled))
            }
            onComplete?.invoke(success)
        }

    fun setAmbientLights(colorIndex: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/lights", JSONObject().put("target", "ambientColour").put("value", colorIndex)) { success ->
            if (success) {
                _state.value = _state.value.copy(lights = _state.value.lights.copy(ambientColour = colorIndex))
            }
            onComplete?.invoke(success)
        }

    // ADAS
    fun setSpeedLimitWarning(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/adas", JSONObject().put("target", "speedLimitWarning").put("enable", enabled)) { success ->
            if (success) {
                _state.value = _state.value.copy(adas = _state.value.adas.copy(speedLimitWarning = enabled))
            }
            onComplete?.invoke(success)
        }

    fun setChildPresence(enabled: Boolean, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/setting", JSONObject().put("target", "childPresenceDetection").put("value", if (enabled) 1 else 2)) { success ->
            if (success) {
                _state.value = _state.value.copy(adas = _state.value.adas.copy(childPresenceDetection = enabled))
            }
            onComplete?.invoke(success)
        }

    // Charging
    fun startCharging(onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/start-charging", JSONObject(), onComplete)

    fun setChargeCap(percent: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/charge-cap", JSONObject().put("percent", percent)) { success ->
            if (success) {
                _state.value = _state.value.copy(charging = _state.value.charging.copy(chargeCapPercent = percent))
            }
            onComplete?.invoke(success)
        }

    fun setAcCurrentLimit(state: Int, onComplete: ((Boolean) -> Unit)? = null) =
        executeCommand("/api/vehicle/ac-charge-current-limit", JSONObject().put("state", state)) { success ->
            if (success) {
                _state.value = _state.value.copy(charging = _state.value.charging.copy(acCurrentLimitState = state))
            }
            onComplete?.invoke(success)
        }

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
