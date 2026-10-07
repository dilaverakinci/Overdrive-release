package com.overdrive.app.ui.assistant

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.json.JSONObject

class AssistantViewModelTest {

    private class FakeApplication : Application()

    private class FakeAssistantRepository(app: Application) : AssistantRepository(app) {
        var statusToReturn: GenAiStatus? = GenAiStatus(enabled = true, configured = true, model = "gpt-5.6-sol")
        var chatResponseToReturn: String? = "Test AI response"
        var testResult: Pair<Boolean, String?> = Pair(true, null)

        override suspend fun fetchStatus(): GenAiStatus? = statusToReturn
        override suspend fun saveConfig(payload: JSONObject): GenAiStatus? = statusToReturn
        override suspend fun testProvider(): Pair<Boolean, String?> = testResult
        override suspend fun sendChatMessage(prompt: String, mode: String): String? = chatResponseToReturn
        override suspend fun fetchRoutines(): List<String> = listOf("Auto climate 22C")
        override suspend fun fetchIncidentPacks(): List<String> = listOf("Pack 1")
    }

    private lateinit var app: FakeApplication
    private lateinit var repo: FakeAssistantRepository
    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setup() {
        app = FakeApplication()
        repo = FakeAssistantRepository(app)
        viewModel = AssistantViewModel(app, repo)
    }

    @Test
    fun initialStateHasAssistantTab() {
        assertEquals(AssistantTab.ASSISTANT, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun tabSelectionUpdatesUiState() {
        viewModel.selectTab(AssistantTab.PROVIDER)
        assertEquals(AssistantTab.PROVIDER, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(AssistantTab.PRIVACY)
        assertEquals(AssistantTab.PRIVACY, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(AssistantTab.ASSISTANT)
        assertEquals(AssistantTab.ASSISTANT, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun clearChatEmptiesMessages() {
        viewModel.clearChat()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun clearToastSetsToastMessageToNull() {
        viewModel.clearToast()
        assertNull(viewModel.uiState.value.toastMessage)
    }
}
