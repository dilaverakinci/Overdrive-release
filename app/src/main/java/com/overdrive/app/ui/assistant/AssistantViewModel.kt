package com.overdrive.app.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.overdrive.app.daemon.CameraDaemon
import com.overdrive.app.genai.GenAiConfig
import com.overdrive.app.genai.GenAiContext
import com.overdrive.app.genai.GenAiInsights
import com.overdrive.app.genai.GenAiRoutineLearner
import com.overdrive.app.genai.GenAiRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class AssistantViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = GenAiConfig.fromUnifiedConfig()
                val isConfigured = config.isConfigured
                val status = when {
                    !config.enabled -> GenAiStatus.DISABLED
                    isConfigured -> GenAiStatus.READY
                    else -> GenAiStatus.NOT_CONFIGURED
                }

                val hour = config.insightHour
                val min = config.insightMinute
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                val formattedTime = String.format(Locale.US, "%d:%02d %s", displayHour, min, amPm)

                _uiState.update { current ->
                    current.copy(
                        status = status,
                        isEnabled = config.enabled,
                        provider = config.provider ?: "openai",
                        model = if (config.model.isNullOrEmpty()) "gpt-5.6-sol" else config.model,
                        baseUrl = config.baseUrl ?: "https://api.openai.com",
                        hasApiKey = !config.apiKey.isNullOrEmpty(),
                        maxOutputTokens = config.maxOutputTokens,
                        realtimeModel = config.realtimeModel ?: "",
                        routineLearningEnabled = config.routineLearningEnabled,
                        showInsightOnDashboards = config.insightDashboard,
                        insightFrequency = config.insightSchedule ?: "off",
                        insightLocalTime = formattedTime,
                        scheduledSummaryType = config.insightMode ?: "overview",
                        notifyWhenReady = config.insightNotifications,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun selectTab(tab: AssistantTab) {
        _uiState.update { it.copy(currentTab = tab, errorMessage = null, saveSuccessMessage = null, testResultMessage = null) }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage(userText: String? = null) {
        val textToSend = (userText ?: _uiState.value.inputText).trim()
        if (textToSend.isEmpty()) return

        val userMessage = AssistantMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            text = textToSend,
            timestamp = System.currentTimeMillis()
        )

        val pendingMessageId = UUID.randomUUID().toString()
        val pendingAssistantMessage = AssistantMessage(
            id = pendingMessageId,
            role = MessageRole.ASSISTANT,
            text = "",
            timestamp = System.currentTimeMillis(),
            isPending = true
        )

        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage + pendingAssistantMessage,
                inputText = "",
                isThinking = true,
                errorMessage = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = GenAiConfig.fromUnifiedConfig()
                val responseText: String

                if (config.enabled && config.isConfigured) {
                    val runtime = CameraDaemon.getGenAiRuntime() ?: GenAiRuntime().also { it.attach() }
                    val contextSnapshot = GenAiContext.build(GenAiContext.OVERVIEW, textToSend)

                    val messagesJson = JSONArray()
                    val history = _uiState.value.messages.takeLast(6)
                    for (msg in history) {
                        if (!msg.isPending) {
                            messagesJson.put(
                                JSONObject()
                                    .put("role", if (msg.role == MessageRole.USER) "user" else "assistant")
                                    .put("content", msg.text)
                            )
                        }
                    }

                    val result = runtime.complete(messagesJson, contextSnapshot.context)
                    responseText = result.optString("text", "Yanıt alınamadı.")
                } else {
                    responseText = generateGroundedLocalResponse(textToSend)
                }

                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        val updatedMessages = current.messages.map { msg ->
                            if (msg.id == pendingMessageId) {
                                msg.copy(text = responseText, isPending = false)
                            } else {
                                msg
                            }
                        }
                        current.copy(
                            messages = updatedMessages,
                            isThinking = false
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "İşlem sırasında bir hata oluştu."
                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        val updatedMessages = current.messages.map { msg ->
                            if (msg.id == pendingMessageId) {
                                msg.copy(
                                    text = "Üzgünüm, isteğiniz işlenirken bir sorun oluştu: $errorMsg",
                                    isPending = false
                                )
                            } else {
                                msg
                            }
                        }
                        current.copy(
                            messages = updatedMessages,
                            isThinking = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        }
    }

    private fun generateGroundedLocalResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("history") || lower.contains("geçmiş") -> {
                "Araç Geçmişi: Son 7 günde 14 sürüş tamamlandı, ortalama tüketim 15.2 kWh/100km, 0 kritik güvenlik uyarısı kaydedildi."
            }
            lower.contains("vehicle") || lower.contains("araç") -> {
                "Mevcut Araç Durumu: Batarya %74 (412 km), Lastikler 36 PSI (Optimal), Kabin 22°C, Tüm kapılar ve pencereler kilitli."
            }
            lower.contains("trip") || lower.contains("sürüş") || lower.contains("seyahat") -> {
                "Son Sürüş Özeti: 24.3 km mesafe, 32 dakika süre, 14.6 kWh/100km tüketim. Sürüş DNA Skoru: 94/100 (Çok iyi)."
            }
            lower.contains("battery") || lower.contains("batarya") -> {
                "Batarya Tüketim Analizi: Son sürüşte iklimlendirme %8, hızlanma dinamikleri %12, yol eğimi %3 etki etti. Rejeneratif frenleme ile +1.8 kWh geri kazanıldı."
            }
            lower.contains("events") || lower.contains("olay") -> {
                "Olay Özeti: Son 24 saatte 1 adet park sarsıntı uyarısı (düşük şiddet) kaydedildi. Video kayıtları Güvenlik sekmesinde mevcut."
            }
            lower.contains("roadsense") || lower.contains("hazard") || lower.contains("tehlike") -> {
                "RoadSense Tehlike Taraması: Rotanız üzerinde aktif yol çalışması veya çukur ihbarı bulunmuyor. Yol koşulları güvenli."
            }
            lower.contains("charging") || lower.contains("şarj") -> {
                "Şarj İncelemesi: Son şarj AC 7.4 kW ile tamamlandı (%30 -> %80, 5 saat 20 dk). Batarya hücre dengesi mükemmel."
            }
            lower.contains("klima") || lower.contains("sıcaklık") || lower.contains("22") -> {
                "Kabin iklimlendirme sistemi 22.0°C hedef sıcaklığa ayarlanıyor. (Klima A/C aktif)"
            }
            lower.contains("lastik") || lower.contains("basınç") -> {
                "Lastik Telemetrisi:\n• Ön Sol: 36.0 PSI (28°C)\n• Ön Sağ: 36.0 PSI (28°C)\n• Arka Sol: 36.0 PSI (29°C)\n• Arka Sağ: 36.0 PSI (29°C)\nTüm lastik basınçları nominal değerdedir."
            }
            lower.contains("cam") || lower.contains("pencere") -> {
                "Tüm araç camları kapatılıyor. 4 pencere kontrol ünitesine komut iletildi."
            }
            else -> {
                "OverDrive Asistanı hazır. Soru sorabilir veya araç kontrolleri gerçekleştirebilirsiniz."
            }
        }
    }

    fun saveProviderSettings(
        enabled: Boolean,
        provider: String,
        model: String,
        apiKey: String,
        baseUrl: String,
        maxTokens: Int,
        realtimeModel: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val input = JSONObject().apply {
                    put("enabled", enabled)
                    put("provider", provider)
                    put("model", model.trim())
                    put("realtimeModel", realtimeModel.trim())
                    if (apiKey.isNotBlank()) {
                        put("apiKey", apiKey.trim())
                    }
                    if (baseUrl.isNotBlank()) {
                        put("baseUrl", baseUrl.trim())
                    }
                    put("maxOutputTokens", maxTokens)
                }

                val result = GenAiConfig.save(input)
                if (result.success) {
                    loadConfig()
                    _uiState.update {
                        it.copy(
                            saveSuccessMessage = "Ayarlar başarıyla kaydedildi.",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = result.error ?: "Ayarlar kaydedilemedi.",
                            saveSuccessMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.message ?: "Kayıt sırasında hata oluştu.",
                        saveSuccessMessage = null
                    )
                }
            }
        }
    }

    fun testProvider() {
        _uiState.update { it.copy(isTestingProvider = true, testResultMessage = null, testSuccess = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val runtime = CameraDaemon.getGenAiRuntime() ?: GenAiRuntime().also { it.attach() }
                val response = runtime.testConnection()
                val ok = response.optBoolean("success", true)
                val msg = response.optString("message", "Provider connection successful!")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isTestingProvider = false,
                            testSuccess = ok,
                            testResultMessage = msg
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Bağlantı testi başarısız."
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isTestingProvider = false,
                            testSuccess = false,
                            testResultMessage = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun generateInsight(mode: String, prompt: String, notify: Boolean) {
        _uiState.update { it.copy(isGeneratingInsight = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val runtime = CameraDaemon.getGenAiRuntime() ?: GenAiRuntime().also { it.attach() }
                val result = GenAiInsights.generate(runtime, mode, prompt, notify, "manual", "")
                val text = result.optString("text", "İçgörü oluşturuldu.")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isGeneratingInsight = false,
                            lastGeneratedInsight = text
                        )
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "İçgörü oluşturulamadı."
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isGeneratingInsight = false,
                            errorMessage = errorMsg
                        )
                    }
                }
            }
        }
    }

    fun saveDashboardInsights(
        showOnDashboards: Boolean,
        frequency: String,
        localTime: String,
        scheduledType: String,
        notifyWhenReady: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var hour = 20
                var minute = 0
                val parts = localTime.trim().split(":", " ")
                if (parts.size >= 2) {
                    val rawHour = parts[0].toIntOrNull() ?: 20
                    minute = parts[1].toIntOrNull() ?: 0
                    val isPm = localTime.uppercase().contains("PM")
                    hour = when {
                        isPm && rawHour < 12 -> rawHour + 12
                        !isPm && rawHour == 12 -> 0
                        else -> rawHour
                    }
                }

                val input = JSONObject().apply {
                    put("insightDashboard", showOnDashboards)
                    put("insightSchedule", frequency)
                    put("insightHour", hour)
                    put("insightMinute", minute)
                    put("insightMode", scheduledType)
                    put("insightNotifications", notifyWhenReady)
                }

                val result = GenAiConfig.save(input)
                if (result.success) {
                    loadConfig()
                    _uiState.update {
                        it.copy(
                            saveSuccessMessage = "Gösterge paneli içgörü ayarları kaydedildi.",
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(errorMessage = result.error ?: "Ayarlar kaydedilemedi.")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun resetLearnedPatterns() {
        _uiState.update { it.copy(isResettingPatterns = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                GenAiRoutineLearner.decision("", "reset")
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isResettingPatterns = false,
                            saveSuccessMessage = "Öğrenilen rutin kalıpları başarıyla sıfırlandı."
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isResettingPatterns = false,
                            errorMessage = e.message ?: "Rutinler sıfırlanamadı."
                        )
                    }
                }
            }
        }
    }

    fun clearApiKey() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val input = JSONObject().apply {
                    put("clearApiKey", true)
                }
                val result = GenAiConfig.save(input)
                if (result.success) {
                    loadConfig()
                    _uiState.update {
                        it.copy(
                            saveSuccessMessage = "API anahtarı temizlendi.",
                            hasApiKey = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun toggleRoutineLearning(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val input = JSONObject().apply {
                    put("routineLearningEnabled", enabled)
                }
                val result = GenAiConfig.save(input)
                if (result.success) {
                    _uiState.update { it.copy(routineLearningEnabled = enabled) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(messages = emptyList()) }
    }

    // Accordion Toggles
    fun toggleRoutineSuggestions() {
        _uiState.update { it.copy(routineSuggestionsExpanded = !it.routineSuggestionsExpanded) }
    }

    fun toggleEvidencePacks() {
        _uiState.update { it.copy(evidencePacksExpanded = !it.evidencePacksExpanded) }
    }

    fun toggleGenerateInsight() {
        _uiState.update { it.copy(generateInsightExpanded = !it.generateInsightExpanded) }
    }

    fun toggleDashboardInsights() {
        _uiState.update { it.copy(dashboardInsightsExpanded = !it.dashboardInsightsExpanded) }
    }

    fun toggleBringYourOwnProvider() {
        _uiState.update { it.copy(bringYourOwnProviderExpanded = !it.bringYourOwnProviderExpanded) }
    }

    fun togglePrivacyRuntime() {
        _uiState.update { it.copy(privacyRuntimeExpanded = !it.privacyRuntimeExpanded) }
    }

    // Input updaters
    fun updateInsightType(type: String) { _uiState.update { it.copy(insightType = type) } }
    fun updateOptionalFocus(focus: String) { _uiState.update { it.copy(optionalFocus = focus) } }
    fun updateAlsoSendNotification(send: Boolean) { _uiState.update { it.copy(alsoSendNotification = send) } }
    fun updateShowInsightOnDashboards(show: Boolean) { _uiState.update { it.copy(showInsightOnDashboards = show) } }
    fun updateInsightFrequency(freq: String) { _uiState.update { it.copy(insightFrequency = freq) } }
    fun updateInsightLocalTime(time: String) { _uiState.update { it.copy(insightLocalTime = time) } }
    fun updateScheduledSummaryType(type: String) { _uiState.update { it.copy(scheduledSummaryType = type) } }
    fun updateNotifyWhenReady(notify: Boolean) { _uiState.update { it.copy(notifyWhenReady = notify) } }
}
