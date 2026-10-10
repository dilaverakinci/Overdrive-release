package com.overdrive.app.ui.seatpositions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SeatPositionsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: SeatPositionsRepository = SeatPositionsRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SeatPositionsState(isLoading = true))
    val state: StateFlow<SeatPositionsState> = _state.asStateFlow()

    private var pollingJob: Job? = null
    private var refreshJob: Job? = null

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                doRefresh()
                delay(3000)
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
        val posResult = repository.getPositions()
        val curResult = repository.getCurrentPosition()
        val autoResult = repository.getAutomations()

        val positions = posResult.getOrNull()?.positions ?: _state.value.positions
        val gate = curResult.getOrNull()?.gate ?: posResult.getOrNull()?.gate ?: _state.value.gate
        val currentProfile = posResult.getOrNull()?.currentProfile ?: _state.value.currentProfile
        val currentAxes = curResult.getOrNull()?.axes ?: _state.value.currentAxes
        val currentAmbient = curResult.getOrNull()?.ambientColour ?: _state.value.currentAmbient
        val palette = curResult.getOrNull()?.palette?.takeIf { it.isNotEmpty() } ?: _state.value.palette
        val colourMax = curResult.getOrNull()?.colourMax ?: _state.value.colourMax
        val automations = autoResult.getOrNull() ?: _state.value.automations

        val matched = SeatGeometryHelper.findMatchingPosition(currentAxes, positions)

        _state.value = _state.value.copy(
            positions = positions,
            gate = gate,
            currentProfile = currentProfile,
            currentAxes = currentAxes,
            currentAmbient = currentAmbient,
            palette = palette,
            colourMax = colourMax,
            automations = automations,
            matchedPosition = matched,
            isLoading = false
        )
    }

    fun applyPosition(
        position: SeatPosition,
        ackModel: Boolean = false,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isApplying = true, applyingPositionId = position.id)
            val result = repository.applyPosition(position.id, ackModel)
            _state.value = _state.value.copy(isApplying = false, applyingPositionId = null)

            result.fold(
                onSuccess = {
                    doRefresh()
                    onComplete?.invoke(true, null)
                },
                onFailure = { error ->
                    onComplete?.invoke(false, error.message)
                }
            )
        }
    }

    fun createPosition(
        name: String,
        parts: String = "all",
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.createPosition(name, parts)
            result.fold(
                onSuccess = {
                    doRefresh()
                    onComplete?.invoke(true, null)
                },
                onFailure = { error ->
                    onComplete?.invoke(false, error.message)
                }
            )
        }
    }

    fun saveOverPosition(
        position: SeatPosition,
        parts: String = "all",
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.saveOverPosition(position.id, parts)
            result.fold(
                onSuccess = {
                    doRefresh()
                    onComplete?.invoke(true, null)
                },
                onFailure = { error ->
                    onComplete?.invoke(false, error.message)
                }
            )
        }
    }

    fun renamePosition(
        position: SeatPosition,
        newName: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.renamePosition(position.id, newName)
            result.fold(
                onSuccess = {
                    doRefresh()
                    onComplete?.invoke(true, null)
                },
                onFailure = { error ->
                    onComplete?.invoke(false, error.message)
                }
            )
        }
    }

    fun setAlias(
        position: SeatPosition,
        alias: String?,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.setAlias(position.id, alias)
            result.fold(
                onSuccess = {
                    doRefresh()
                    onComplete?.invoke(true, null)
                },
                onFailure = { error ->
                    onComplete?.invoke(false, error.message)
                }
            )
        }
    }

    fun deletePosition(
        position: SeatPosition,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deletePosition(position.id)
            result.fold(
                onSuccess = {
                    doRefresh()
                    onComplete?.invoke(true, null)
                },
                onFailure = { error ->
                    onComplete?.invoke(false, error.message)
                }
            )
        }
    }

    fun setAmbientColour(
        position: SeatPosition,
        colourIndex: Int,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.setAmbientColour(position.id, colourIndex)
            result.fold(
                onSuccess = {
                    doRefresh()
                    onComplete?.invoke(true, null)
                },
                onFailure = { error ->
                    onComplete?.invoke(false, error.message)
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
