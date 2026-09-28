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
 * Complete UI State for AssistantScreen (100% Jetpack Compose Native).
 */
data class AssistantUiState(
    val currentTab: AssistantTab = AssistantTab.CHAT,

    // Provider & Runtime Status
    val status: GenAiStatus = GenAiStatus.READY,
    val isEnabled: Boolean = true,
    val provider: String = "gemini",
    val model: String = "gemini-2.5-flash",
    val baseUrl: String = "",
    val hasApiKey: Boolean = true,
    val maxOutputTokens: Int = 1200,

    // Chat State
    val messages: List<AssistantMessage> = emptyList(),
    val inputText: String = "",
    val isThinking: Boolean = false,
    val isListeningVoice: Boolean = false,
    val errorMessage: String? = null,

    // Privacy & Runtime Diagnostics
    val routineLearningEnabled: Boolean = false,
    val parkedAvailability: String = "Mevcut",
    val currentTransport: String = "Doğrudan",
    val proxyPolicy: String = "Doğrudan",
    val activeRequests: Int = 0,

    // Action feedback
    val saveSuccessMessage: String? = null,
)
