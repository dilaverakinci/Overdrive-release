package com.overdrive.app.ui.automations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.logging.DaemonLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutomationsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AutomationsRepository = AutomationsRepository(application)
) : AndroidViewModel(application) {

    private val logger = DaemonLogger.getInstance("AutomationsViewModel")

    private val _uiState = MutableStateFlow(AutomationsUiState(isLoading = true))
    val uiState: StateFlow<AutomationsUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadAllData()
    }

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(10_000L)
                refreshDataSilently()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun selectTab(tab: AutomationsTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setSortMode(mode: AutomationSortMode) {
        _uiState.update { state ->
            val sorted = sortAutomations(state.automations, mode)
            state.copy(sortMode = mode, automations = sorted)
        }
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val autoResult = repository.getAutomations()
            val groupsResult = repository.getActionGroups()
            val settingsResult = repository.getSettings()

            val rawAutos = autoResult.getOrDefault(emptyList())
            val sortedAutos = sortAutomations(rawAutos, _uiState.value.sortMode)
            val groups = groupsResult.getOrDefault(emptyList())
            val settings = settingsResult.getOrDefault(AutomationSettingsItem())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    automations = sortedAutos,
                    actionGroups = groups,
                    settings = settings,
                    errorMessage = if (autoResult.isFailure) "Could not load automations" else null
                )
            }
        }
    }

    private suspend fun refreshDataSilently() {
        val autoResult = repository.getAutomations()
        val groupsResult = repository.getActionGroups()
        val settingsResult = repository.getSettings()

        if (autoResult.isSuccess || groupsResult.isSuccess || settingsResult.isSuccess) {
            _uiState.update { state ->
                val autos = autoResult.getOrNull()?.let { sortAutomations(it, state.sortMode) } ?: state.automations
                val groups = groupsResult.getOrNull() ?: state.actionGroups
                val settings = settingsResult.getOrNull() ?: state.settings
                state.copy(
                    automations = autos,
                    actionGroups = groups,
                    settings = settings
                )
            }
        }
    }

    fun toggleAutomationMode(item: AutomationItem) {
        val nextMode = when (item.mode) {
            "automatic" -> "disabled"
            "disabled" -> "automatic"
            else -> "automatic"
        }
        setAutomationMode(item.id, nextMode)
    }

    fun setAutomationMode(id: String, mode: String) {
        viewModelScope.launch {
            // Optimistic update
            _uiState.update { state ->
                val updated = state.automations.map {
                    if (it.id == id) it.copy(
                        mode = mode,
                        isDisabled = (mode == "disabled" || mode == "manual"),
                        isManualOnly = (mode == "manual")
                    ) else it
                }
                state.copy(automations = sortAutomations(updated, state.sortMode))
            }
            val res = repository.setAutomationMode(id, mode)
            if (res.isFailure) {
                refreshDataSilently()
                _uiState.update { it.copy(errorMessage = "Failed to update mode") }
            }
        }
    }

    fun testAutomation(id: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(testRunStatus = "Running $name...") }
            val res = repository.testAutomation(id)
            if (res.isSuccess) {
                _uiState.update { it.copy(testRunStatus = "Fired $name successfully") }
                delay(2500)
                _uiState.update { it.copy(testRunStatus = null) }
                refreshDataSilently()
            } else {
                _uiState.update { it.copy(testRunStatus = "Failed to fire $name") }
                delay(3000)
                _uiState.update { it.copy(testRunStatus = null) }
            }
        }
    }

    fun deleteAutomation(id: String) {
        viewModelScope.launch {
            val res = repository.deleteAutomation(id)
            if (res.isSuccess) {
                _uiState.update { state ->
                    val filtered = state.automations.filterNot { it.id == id }
                    state.copy(automations = filtered)
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not delete automation") }
            }
        }
    }

    fun runActionGroup(group: ActionGroupItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(testRunStatus = "Running group: ${group.name}...") }
            val res = repository.runActionGroup(group.id)
            if (res.isSuccess) {
                _uiState.update { it.copy(testRunStatus = "Executed group ${group.name}") }
                delay(2500)
                _uiState.update { it.copy(testRunStatus = null) }
            } else {
                _uiState.update { it.copy(testRunStatus = "Failed to run group ${group.name}") }
                delay(3000)
                _uiState.update { it.copy(testRunStatus = null) }
            }
        }
    }

    fun deleteActionGroup(id: String) {
        viewModelScope.launch {
            val res = repository.deleteActionGroup(id)
            if (res.isSuccess) {
                _uiState.update { state ->
                    state.copy(actionGroups = state.actionGroups.filterNot { it.id == id })
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Could not delete action group") }
            }
        }
    }

    fun updateSafetyGuard(guardKey: String, enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                val cur = state.settings
                val updated = when (guardKey) {
                    "doorLocks" -> cur.copy(doorLocksGuard = enabled)
                    "trunk" -> cur.copy(trunkGuard = enabled)
                    "mirrorFold" -> cur.copy(mirrorFoldGuard = enabled)
                    "positioning" -> cur.copy(positioningGuard = enabled)
                    "headlightOff" -> cur.copy(headlightOffGuard = enabled)
                    "displayBrightness" -> cur.copy(displayBrightnessGuard = enabled)
                    "displayPower" -> cur.copy(displayPowerGuard = enabled)
                    "screenMedia" -> cur.copy(screenMediaGuard = enabled)
                    else -> cur
                }
                state.copy(settings = updated)
            }
            val res = repository.updateSafetyGuard(guardKey, enabled)
            if (res.isFailure) {
                refreshDataSilently()
                _uiState.update { it.copy(errorMessage = "Failed to update safety guard") }
            }
        }
    }

    fun updateAllowShell(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(settings = state.settings.copy(allowShell = enabled))
            }
            val res = repository.updateAllowShell(enabled)
            if (res.isFailure) {
                refreshDataSilently()
                _uiState.update { it.copy(errorMessage = "Failed to update shell permission") }
            }
        }
    }

    private fun sortAutomations(list: List<AutomationItem>, mode: AutomationSortMode): List<AutomationItem> {
        return when (mode) {
            AutomationSortMode.DEFAULT -> list
            AutomationSortMode.NAME_AZ -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            AutomationSortMode.NAME_ZA -> list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.displayName })
            AutomationSortMode.RECENT -> list.sortedByDescending { it.lastTriggered }
            AutomationSortMode.RUNS -> list.sortedByDescending { it.triggerCount }
            AutomationSortMode.ENABLED_FIRST -> list.sortedWith(compareBy({ !it.isRunningEnabled }, { it.displayName }))
            AutomationSortMode.DISABLED_FIRST -> list.sortedWith(compareBy({ it.isRunningEnabled }, { it.displayName }))
        }
    }
}
