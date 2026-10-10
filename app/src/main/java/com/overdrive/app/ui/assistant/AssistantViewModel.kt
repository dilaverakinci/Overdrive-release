package com.overdrive.app.ui.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AssistantViewModel(
    application: Application,
    private val repository: AssistantRepository,
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AssistantRepository(application.applicationContext),
    )

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun selectTab(tab: AssistantTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun loadInitialData() {
        viewModelScope.launch {
            val status = repository.fetchStatus()
            val routines = repository.fetchRoutines()
            val incidents = repository.fetchIncidentPacks()
            _uiState.value = _uiState.value.copy(
                status = status,
                routineSuggestions = routines,
                incidentPacks = incidents,
            )
        }
    }

    fun sendMessage(prompt: String, mode: String = "general") {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _uiState.value.isBusy) return

        val userMessage = ChatMessage(isUser = true, text = trimmed)
        val pendingAssistantMessage = ChatMessage(isUser = false, text = "", isPending = true)

        val updatedMessages = _uiState.value.messages + userMessage + pendingAssistantMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isBusy = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            try {
                val response = repository.sendChatMessage(trimmed, mode)
                val resolvedMessage = if (!response.isNullOrEmpty()) {
                    ChatMessage(isUser = false, text = response)
                } else {
                    ChatMessage(isUser = false, text = "No response from provider.", isError = true)
                }
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.dropLast(1) + resolvedMessage,
                    isBusy = false,
                )
            } catch (t: Throwable) {
                val errorMessage = ChatMessage(
                    isUser = false,
                    text = t.message ?: "Failed to generate response.",
                    isError = true,
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.dropLast(1) + errorMessage,
                    isBusy = false,
                    errorMessage = t.message,
                )
            }
        }
    }

    fun saveConfig(
        enabled: Boolean,
        provider: String,
        model: String,
        baseUrl: String,
        apiKey: String?,
        maxTokens: Int,
        realtimeModel: String = "",
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val payload = JSONObject().apply {
                put("enabled", enabled)
                put("provider", provider)
                put("model", model)
                put("baseUrl", baseUrl)
                put("maxOutputTokens", maxTokens)
                put("realtimeModel", realtimeModel)
                if (!apiKey.isNullOrBlank()) {
                    put("apiKey", apiKey.trim())
                }
            }
            val newStatus = repository.saveConfig(payload)
            if (newStatus != null) {
                _uiState.value = _uiState.value.copy(
                    status = newStatus,
                    isBusy = false,
                    toastMessage = "Settings saved successfully",
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = "Failed to save settings",
                )
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val (ok, error) = repository.testProvider()
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                toastMessage = if (ok) "Provider connection test passed!" else "Test failed: $error",
                errorMessage = if (!ok) error else null,
            )
        }
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
