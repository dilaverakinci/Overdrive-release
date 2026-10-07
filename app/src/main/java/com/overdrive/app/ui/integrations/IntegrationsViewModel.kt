package com.overdrive.app.ui.integrations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class IntegrationsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: IntegrationsRepository = IntegrationsRepository(application)
) : AndroidViewModel(application) {

    private val _activeTab = MutableStateFlow(IntegrationTab.OVERVIEW)
    val activeTab: StateFlow<IntegrationTab> = _activeTab.asStateFlow()

    private val _summary = MutableStateFlow(IntegrationsSummary())
    val summary: StateFlow<IntegrationsSummary> = _summary.asStateFlow()

    private val _telegramState = MutableStateFlow(TelegramConfigState())
    val telegramState: StateFlow<TelegramConfigState> = _telegramState.asStateFlow()

    private val _abrpState = MutableStateFlow(AbrpConfigState())
    val abrpState: StateFlow<AbrpConfigState> = _abrpState.asStateFlow()

    private val _mqttBrokers = MutableStateFlow<List<MqttBrokerConfig>>(emptyList())
    val mqttBrokers: StateFlow<List<MqttBrokerConfig>> = _mqttBrokers.asStateFlow()

    private val _bydCloudState = MutableStateFlow(BydCloudConfigState())
    val bydCloudState: StateFlow<BydCloudConfigState> = _bydCloudState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAll()
    }

    fun selectTab(tab: IntegrationTab) {
        _activeTab.value = tab
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _summary.value = repository.fetchSummary()
                _telegramState.value = repository.getTelegramStatus()
                _abrpState.value = repository.getAbrpStatus()
                _mqttBrokers.value = repository.getMqttConnections()
                _bydCloudState.value = repository.getBydCloudStatus()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to refresh integrations"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ================== TELEGRAM ==================

    fun saveTelegramToken(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.saveTelegramToken(token.trim())
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "Telegram token saved successfully"
                _telegramState.value = repository.getTelegramStatus()
                _summary.value = repository.fetchSummary()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to save Telegram token"
            }
        }
    }

    fun generateTelegramPin() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.generateTelegramPin()
            _isLoading.value = false
            res.onSuccess { pin ->
                _statusMessage.value = "Pairing PIN generated: $pin"
                _telegramState.value = repository.getTelegramStatus()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to generate PIN"
            }
        }
    }

    fun unpairTelegram() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.unpairTelegram()
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "Telegram bot unpaired"
                _telegramState.value = repository.getTelegramStatus()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to unpair"
            }
        }
    }

    fun saveTelegramPreferences(
        videoUploads: Boolean,
        criticalAlerts: Boolean,
        motionText: Boolean,
        tyreAlerts: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateTelegramPreferences(videoUploads, criticalAlerts, motionText, tyreAlerts)
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "Notification settings updated"
                _telegramState.value = repository.getTelegramStatus()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to update settings"
            }
        }
    }

    // ================== ABRP ==================

    fun saveAbrpConfig(token: String, enabled: Boolean, carModel: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.saveAbrpConfig(token.trim(), enabled, carModel)
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "ABRP configuration updated"
                _abrpState.value = repository.getAbrpStatus()
                _summary.value = repository.fetchSummary()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to save ABRP settings"
            }
        }
    }

    fun deleteAbrpToken() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteAbrpToken()
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "ABRP token cleared"
                _abrpState.value = repository.getAbrpStatus()
                _summary.value = repository.fetchSummary()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to clear ABRP token"
            }
        }
    }

    // ================== MQTT ==================

    fun addMqttBroker(broker: MqttBrokerConfig) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.addMqttConnection(broker)
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "MQTT broker '${broker.name}' saved"
                _mqttBrokers.value = repository.getMqttConnections()
                _summary.value = repository.fetchSummary()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to add MQTT broker"
            }
        }
    }

    fun deleteMqttBroker(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteMqttConnection(id)
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "MQTT broker removed"
                _mqttBrokers.value = repository.getMqttConnections()
                _summary.value = repository.fetchSummary()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to delete MQTT broker"
            }
        }
    }

    // ================== BYD CLOUD ==================

    fun saveBydCloudSetup(username: String, password: String, controlPin: String, countryCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.saveBydCloudSetup(username, password, controlPin, countryCode)
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "BYD Cloud connected successfully"
                _bydCloudState.value = repository.getBydCloudStatus()
                _summary.value = repository.fetchSummary()
            }.onFailure {
                _errorMessage.value = it.message ?: "BYD Cloud connection failed"
            }
        }
    }

    fun testBydCloudCommand(action: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.testBydCloudCommand(action)
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "Command sent successfully"
            }.onFailure {
                _errorMessage.value = it.message ?: "Command failed"
            }
        }
    }

    fun clearBydCloud() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.clearBydCloud()
            _isLoading.value = false
            res.onSuccess {
                _statusMessage.value = "BYD Cloud credentials removed"
                _bydCloudState.value = repository.getBydCloudStatus()
                _summary.value = repository.fetchSummary()
            }.onFailure {
                _errorMessage.value = it.message ?: "Failed to clear credentials"
            }
        }
    }
}
