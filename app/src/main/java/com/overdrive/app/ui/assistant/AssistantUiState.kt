package com.overdrive.app.ui.assistant

/**
 * Tab selection for AssistantScreen.
 */
enum class AssistantTab {
    CHAT,
    PROVIDER,
    PRIVACY
}

/**
 * Role of chat message sender.
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Chat message model.
 */
data class AssistantMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPending: Boolean = false,
    val actionConfirmed: Boolean = false,
    val actionSummary: String? = null,
)

/**
 * State of GenAI provider connection.
 */
enum class GenAiStatus {
    READY,
    DISABLED,
    NOT_CONFIGURED
}

/**
 * Complete UI State for AssistantScreen (100% Jetpack Compose Native & 100% Web Parity).
 */
data class AssistantUiState(
    val currentTab: AssistantTab = AssistantTab.CHAT,

    // Provider & Runtime Status
    val status: GenAiStatus = GenAiStatus.READY,
    val isEnabled: Boolean = false,
    val provider: String = "openai",
    val model: String = "gpt-5.6-sol",
    val baseUrl: String = "https://api.openai.com",
    val hasApiKey: Boolean = false,
    val maxOutputTokens: Int = 1200,
    val realtimeModel: String = "",

    // Chat State
    val messages: List<AssistantMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val isListeningVoice: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccessMessage: String? = null,

    // Generate an insight section
    val insightType: String = "overview",
    val optionalFocus: String = "",
    val alsoSendNotification: Boolean = false,
    val isGeneratingInsight: Boolean = false,
    val lastGeneratedInsight: String? = null,

    // Dashboard insights section
    val showInsightOnDashboards: Boolean = false,
    val insightFrequency: String = "off",
    val insightLocalTime: String = "8:00 PM",
    val scheduledSummaryType: String = "overview",
    val notifyWhenReady: Boolean = false,

    // Provider testing state
    val isTestingProvider: Boolean = false,
    val testResultMessage: String? = null,
    val testSuccess: Boolean? = null,

    // Privacy & Runtime Diagnostics
    val routineLearningEnabled: Boolean = false,
    val isResettingPatterns: Boolean = false,
    val parkedAvailability: String = "Available (onAndOff mode)",
    val currentTransport: String = "Idle · no provider socket",
    val proxyPolicy: String = "Dynamic · direct allowed",
    val activeRequests: Int = 0,

    // Accordion Expansion States
    val routineSuggestionsExpanded: Boolean = true,
    val evidencePacksExpanded: Boolean = true,
    val generateInsightExpanded: Boolean = true,
    val dashboardInsightsExpanded: Boolean = true,
    val bringYourOwnProviderExpanded: Boolean = true,
    val privacyRuntimeExpanded: Boolean = true,
)
