package com.overdrive.app.ui.assistant

enum class AssistantTab {
    ASSISTANT,
    PROVIDER,
    PRIVACY
}

data class GenAiStatus(
    val enabled: Boolean = false,
    val configured: Boolean = false,
    val provider: String = "openai",
    val model: String = "",
    val baseUrl: String = "",
    val maxOutputTokens: Int = 1200,
    val apiKeyConfigured: Boolean = false,
    val realtimeModel: String = "",
    val nativeRealtimeAudioAvailable: Boolean = false,
    val availableWhileParked: Boolean = false,
    val transportActive: Boolean = false,
    val lastNetworkRoute: String = "",
    val proxyExpected: Boolean = false,
    val activeRequests: Int = 0,
    val routineLearningEnabled: Boolean = false,
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = false,
    val isError: Boolean = false,
)

data class AssistantUiState(
    val selectedTab: AssistantTab = AssistantTab.ASSISTANT,
    val status: GenAiStatus? = null,
    val messages: List<ChatMessage> = emptyList(),
    val routineSuggestions: List<String> = emptyList(),
    val incidentPacks: List<String> = emptyList(),
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
)
