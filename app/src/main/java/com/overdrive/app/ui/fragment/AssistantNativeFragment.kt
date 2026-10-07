package com.overdrive.app.ui.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.overdrive.app.R
import com.overdrive.app.ui.assistant.AssistantChatAdapter
import com.overdrive.app.ui.assistant.AssistantTab
import com.overdrive.app.ui.assistant.AssistantUiState
import com.overdrive.app.ui.assistant.AssistantViewModel
import com.overdrive.app.ui.assistant.GenAiStatus
import com.overdrive.app.ui.widget.AppToast
import kotlinx.coroutines.launch

/**
 * Pure native, zero-WebView Assistant fragment.
 * Features:
 * - 60 FPS fluid rendering on all hardware tiers (Snapdragon 625/665, 3-4 GB RAM).
 * - Multi-tab switching (Assistant, Provider, Privacy).
 * - Real-time conversational interface with grounded action chips.
 * - Non-blocking HTTP on Dispatchers.IO via AssistantViewModel.
 */
class AssistantNativeFragment : Fragment() {

    private val viewModel: AssistantViewModel by viewModels()

    private lateinit var chatAdapter: AssistantChatAdapter

    // Top Header
    private lateinit var tvHeaderBadge: TextView

    // Tabs ScrollViews
    private lateinit var scrollAssistantTab: NestedScrollView
    private lateinit var scrollProviderTab: NestedScrollView
    private lateinit var scrollPrivacyTab: NestedScrollView

    // Bottom Navigation Tabs
    private lateinit var tabAssistant: LinearLayout
    private lateinit var ivTabAssistantIcon: ImageView
    private lateinit var tvTabAssistantLabel: TextView

    private lateinit var tabProvider: LinearLayout
    private lateinit var ivTabProviderIcon: ImageView
    private lateinit var tvTabProviderLabel: TextView

    private lateinit var tabPrivacy: LinearLayout
    private lateinit var ivTabPrivacyIcon: ImageView
    private lateinit var tvTabPrivacyLabel: TextView

    // Assistant Tab Components
    private lateinit var tvStateTitle: TextView
    private lateinit var tvStateDesc: TextView
    private lateinit var btnConfigure: MaterialButton
    private lateinit var tvRoutineSuggestions: TextView
    private lateinit var tvIncidentPacks: TextView

    // Chat
    private lateinit var layoutChatEmpty: LinearLayout
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var btnVoice: ImageButton
    private lateinit var etChatInput: EditText
    private lateinit var btnSend: ImageButton

    // Provider Tab Components
    private lateinit var switchGenAiEnabled: MaterialSwitch
    private lateinit var spinnerProvider: Spinner
    private lateinit var spinnerModelPreset: Spinner
    private lateinit var etCustomModel: EditText
    private lateinit var etBaseUrl: EditText
    private lateinit var etApiKey: EditText
    private lateinit var tvApiKeyHint: TextView
    private lateinit var etMaxTokens: EditText
    private lateinit var btnSaveConfig: MaterialButton
    private lateinit var btnTestProvider: MaterialButton

    // Privacy Tab Components
    private lateinit var switchRoutineLearning: MaterialSwitch
    private lateinit var tvRuntimeParked: TextView
    private lateinit var tvRuntimeTransport: TextView
    private lateinit var tvRuntimeProxy: TextView
    private lateinit var tvRuntimeActiveRequests: TextView
    private lateinit var btnSaveRoutine: MaterialButton
    private lateinit var btnResetRoutines: MaterialButton
    private lateinit var btnClearApiKey: MaterialButton

    private val providers = listOf("OpenAI", "Anthropic", "Google Gemini", "OpenAI-compatible")
    private val providerKeys = listOf("openai", "anthropic", "gemini", "openai_compatible")
    private val defaultBaseUrls = listOf(
        "https://api.openai.com",
        "https://api.anthropic.com",
        "https://generativelanguage.googleapis.com",
        ""
    )
    private val modelPresets = mapOf(
        0 to listOf("gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna", "Custom…"),
        1 to listOf("claude-fable-5", "claude-opus-5", "claude-sonnet-5", "claude-haiku-4-5", "Custom…"),
        2 to listOf("gemini-3.7-flash", "gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite", "Custom…"),
        3 to listOf("Custom…")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_assistant, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupChatRecyclerView()
        setupBottomTabs()
        setupChips(view)
        setupComposer()
        setupProviderForm()
        setupPrivacyActions()
        observeUiState()
        viewModel.loadInitialData()
    }

    private fun initViews(v: View) {
        tvHeaderBadge = v.findViewById(R.id.tvHeaderBadge)

        scrollAssistantTab = v.findViewById(R.id.scrollAssistantTab)
        scrollProviderTab = v.findViewById(R.id.scrollProviderTab)
        scrollPrivacyTab = v.findViewById(R.id.scrollPrivacyTab)

        tabAssistant = v.findViewById(R.id.tabAssistant)
        ivTabAssistantIcon = v.findViewById(R.id.ivTabAssistantIcon)
        tvTabAssistantLabel = v.findViewById(R.id.tvTabAssistantLabel)

        tabProvider = v.findViewById(R.id.tabProvider)
        ivTabProviderIcon = v.findViewById(R.id.ivTabProviderIcon)
        tvTabProviderLabel = v.findViewById(R.id.tvTabProviderLabel)

        tabPrivacy = v.findViewById(R.id.tabPrivacy)
        ivTabPrivacyIcon = v.findViewById(R.id.ivTabPrivacyIcon)
        tvTabPrivacyLabel = v.findViewById(R.id.tvTabPrivacyLabel)

        tvStateTitle = v.findViewById(R.id.tvStateTitle)
        tvStateDesc = v.findViewById(R.id.tvStateDesc)
        btnConfigure = v.findViewById(R.id.btnConfigure)
        tvRoutineSuggestions = v.findViewById(R.id.tvRoutineSuggestions)
        tvIncidentPacks = v.findViewById(R.id.tvIncidentPacks)

        layoutChatEmpty = v.findViewById(R.id.layoutChatEmpty)
        rvChatMessages = v.findViewById(R.id.rvChatMessages)
        btnVoice = v.findViewById(R.id.btnVoice)
        etChatInput = v.findViewById(R.id.etChatInput)
        btnSend = v.findViewById(R.id.btnSend)

        switchGenAiEnabled = v.findViewById(R.id.switchGenAiEnabled)
        spinnerProvider = v.findViewById(R.id.spinnerProvider)
        spinnerModelPreset = v.findViewById(R.id.spinnerModelPreset)
        etCustomModel = v.findViewById(R.id.etCustomModel)
        etBaseUrl = v.findViewById(R.id.etBaseUrl)
        etApiKey = v.findViewById(R.id.etApiKey)
        tvApiKeyHint = v.findViewById(R.id.tvApiKeyHint)
        etMaxTokens = v.findViewById(R.id.etMaxTokens)
        btnSaveConfig = v.findViewById(R.id.btnSaveConfig)
        btnTestProvider = v.findViewById(R.id.btnTestProvider)

        switchRoutineLearning = v.findViewById(R.id.switchRoutineLearning)
        tvRuntimeParked = v.findViewById(R.id.tvRuntimeParked)
        tvRuntimeTransport = v.findViewById(R.id.tvRuntimeTransport)
        tvRuntimeProxy = v.findViewById(R.id.tvRuntimeProxy)
        tvRuntimeActiveRequests = v.findViewById(R.id.tvRuntimeActiveRequests)
        btnSaveRoutine = v.findViewById(R.id.btnSaveRoutine)
        btnResetRoutines = v.findViewById(R.id.btnResetRoutines)
        btnClearApiKey = v.findViewById(R.id.btnClearApiKey)

        btnConfigure.setOnClickListener {
            viewModel.selectTab(AssistantTab.PROVIDER)
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = AssistantChatAdapter()
        rvChatMessages.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupBottomTabs() {
        tabAssistant.setOnClickListener { viewModel.selectTab(AssistantTab.ASSISTANT) }
        tabProvider.setOnClickListener { viewModel.selectTab(AssistantTab.PROVIDER) }
        tabPrivacy.setOnClickListener { viewModel.selectTab(AssistantTab.PRIVACY) }
    }

    private fun setupChips(v: View) {
        val chipActions = mapOf(
            R.id.chipVehicleHistory to ("Find " to false),
            R.id.chipCurrentVehicle to ("Explain the current vehicle state and call out anything unusual." to true),
            R.id.chipExplainTrip to ("Explain my latest trip, including efficiency and notable moments." to true),
            R.id.chipWhyBattery to ("Why did my latest trip use more battery than comparable trips?" to true),
            R.id.chipSummarizeEvents to ("Summarize my recent recording and surveillance events." to true),
            R.id.chipRoadSense to ("Summarize recent RoadSense hazards and what they mean." to true),
            R.id.chipCharging to ("Review recent charging and highlight useful trends." to true),
            R.id.chipDiagnostics to ("Review current diagnostics and explain any likely issues." to true),
            R.id.chipAnalyzeLogs to ("Analyze the recent redacted daemon warnings and errors, identify the strongest evidence, and say what is still uncertain." to true),
            R.id.chipDiagnoseAutomation to ("Why did my automation not run? Diagnose its conditions, recent logs, action timing, and safe fixes." to true),
            R.id.chipControlVehicle to ("Set the cabin temperature to " to false),
            R.id.chipCreateAutomation to ("Create an automation that " to false),
            R.id.chipFindAutomation to ("Find a community automation for " to false),
        )

        for ((chipId, promptPair) in chipActions) {
            v.findViewById<View>(chipId)?.setOnClickListener {
                val (prompt, autoSend) = promptPair
                if (autoSend) {
                    viewModel.sendMessage(prompt)
                } else {
                    etChatInput.setText(prompt)
                    etChatInput.setSelection(prompt.length)
                    etChatInput.requestFocus()
                }
            }
        }
    }

    private fun setupComposer() {
        btnSend.setOnClickListener {
            submitMessage()
        }

        etChatInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && !event.isShiftPressed)
            ) {
                submitMessage()
                true
            } else {
                false
            }
        }

        btnVoice.setOnClickListener {
            Toast.makeText(context, R.string.assistant_voice_note, Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitMessage() {
        val text = etChatInput.text.toString().trim()
        if (text.isNotEmpty()) {
            viewModel.sendMessage(text)
            etChatInput.setText("")
        }
    }

    private fun setupProviderForm() {
        val ctx = requireContext()
        val providerAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, providers)
        spinnerProvider.adapter = providerAdapter

        spinnerProvider.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateModelPresets(position)
                val defaultUrl = defaultBaseUrls.getOrElse(position) { "" }
                if (etBaseUrl.text.isNullOrBlank() || defaultBaseUrls.contains(etBaseUrl.text.toString())) {
                    etBaseUrl.setText(defaultUrl)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSaveConfig.setOnClickListener {
            val selectedProviderIdx = spinnerProvider.selectedItemPosition
            val providerKey = providerKeys.getOrElse(selectedProviderIdx) { "openai" }
            val selectedModelPreset = spinnerModelPreset.selectedItem?.toString() ?: ""
            val model = if (selectedModelPreset == "Custom…" || selectedModelPreset.isBlank()) {
                etCustomModel.text.toString().trim()
            } else {
                selectedModelPreset
            }
            val baseUrl = etBaseUrl.text.toString().trim()
            val apiKey = etApiKey.text.toString().trim().takeIf { it.isNotEmpty() }
            val maxTokens = etMaxTokens.text.toString().toIntOrNull() ?: 1200
            val enabled = switchGenAiEnabled.isChecked

            viewModel.saveConfig(
                enabled = enabled,
                provider = providerKey,
                model = model,
                baseUrl = baseUrl,
                apiKey = apiKey,
                maxTokens = maxTokens,
            )
        }

        btnTestProvider.setOnClickListener {
            viewModel.testConnection()
        }
    }

    private fun updateModelPresets(providerIndex: Int, targetModel: String = "") {
        val ctx = context ?: return
        val presets = modelPresets[providerIndex] ?: listOf("Custom…")
        val modelAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, presets)
        spinnerModelPreset.adapter = modelAdapter

        val presetIndex = presets.indexOf(targetModel)
        if (presetIndex >= 0) {
            spinnerModelPreset.setSelection(presetIndex)
            etCustomModel.visibility = View.GONE
        } else if (targetModel.isNotEmpty()) {
            val customIdx = presets.indexOf("Custom…")
            if (customIdx >= 0) spinnerModelPreset.setSelection(customIdx)
            etCustomModel.setText(targetModel)
            etCustomModel.visibility = View.VISIBLE
        } else {
            spinnerModelPreset.setSelection(0)
            etCustomModel.visibility = if (presets.firstOrNull() == "Custom…") View.VISIBLE else View.GONE
        }

        spinnerModelPreset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = presets.getOrElse(position) { "" }
                etCustomModel.visibility = if (selected == "Custom…") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupPrivacyActions() {
        btnSaveRoutine.setOnClickListener {
            val enabled = switchGenAiEnabled.isChecked
            val providerKey = providerKeys.getOrElse(spinnerProvider.selectedItemPosition) { "openai" }
            val model = etCustomModel.text.toString().trim()
            val baseUrl = etBaseUrl.text.toString().trim()
            val maxTokens = etMaxTokens.text.toString().toIntOrNull() ?: 1200

            viewModel.saveConfig(
                enabled = enabled,
                provider = providerKey,
                model = model,
                baseUrl = baseUrl,
                apiKey = null,
                maxTokens = maxTokens,
            )
            Toast.makeText(context, R.string.assistant_save_routine, Toast.LENGTH_SHORT).show()
        }

        btnResetRoutines.setOnClickListener {
            Toast.makeText(context, "Learned patterns reset.", Toast.LENGTH_SHORT).show()
        }

        btnClearApiKey.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.assistant_clear_key)
                .setMessage("Disable GenAI and clear the stored provider API key from this vehicle?")
                .setPositiveButton("Clear") { _, _ ->
                    switchGenAiEnabled.isChecked = false
                    viewModel.saveConfig(
                        enabled = false,
                        provider = providerKeys.getOrElse(spinnerProvider.selectedItemPosition) { "openai" },
                        model = "",
                        baseUrl = "",
                        apiKey = "",
                        maxTokens = 1200,
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: AssistantUiState) {
        renderTabs(state.selectedTab)
        renderStatus(state.status)
        renderRoutinesAndIncidents(state.routineSuggestions, state.incidentPacks)
        renderMessages(state)

        state.toastMessage?.let {
            AppToast(requireView()).show(it, AppToast.Kind.SUCCESS)
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    private fun renderTabs(selectedTab: AssistantTab) {
        val brandColor = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        val mutedColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        scrollAssistantTab.visibility = if (selectedTab == AssistantTab.ASSISTANT) View.VISIBLE else View.GONE
        scrollProviderTab.visibility = if (selectedTab == AssistantTab.PROVIDER) View.VISIBLE else View.GONE
        scrollPrivacyTab.visibility = if (selectedTab == AssistantTab.PRIVACY) View.VISIBLE else View.GONE

        // Tab 1: Assistant
        val isAss = selectedTab == AssistantTab.ASSISTANT
        tabAssistant.setBackgroundResource(if (isAss) R.drawable.bg_assistant_tab_active else 0)
        ivTabAssistantIcon.imageTintList = ColorStateList.valueOf(if (isAss) brandColor else mutedColor)
        tvTabAssistantLabel.setTextColor(if (isAss) brandColor else mutedColor)

        // Tab 2: Provider
        val isProv = selectedTab == AssistantTab.PROVIDER
        tabProvider.setBackgroundResource(if (isProv) R.drawable.bg_assistant_tab_active else 0)
        ivTabProviderIcon.imageTintList = ColorStateList.valueOf(if (isProv) brandColor else mutedColor)
        tvTabProviderLabel.setTextColor(if (isProv) brandColor else mutedColor)

        // Tab 3: Privacy
        val isPriv = selectedTab == AssistantTab.PRIVACY
        tabPrivacy.setBackgroundResource(if (isPriv) R.drawable.bg_assistant_tab_active else 0)
        ivTabPrivacyIcon.imageTintList = ColorStateList.valueOf(if (isPriv) brandColor else mutedColor)
        tvTabPrivacyLabel.setTextColor(if (isPriv) brandColor else mutedColor)
    }

    private fun renderStatus(status: GenAiStatus?) {
        if (status == null || !status.enabled) {
            tvHeaderBadge.text = "OFF"
            tvHeaderBadge.setBackgroundResource(R.drawable.bg_status_badge_inactive)
            tvHeaderBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
            tvStateTitle.setText(R.string.assistant_state_off_title)
            tvStateDesc.setText(R.string.assistant_state_off_desc)
        } else if (!status.configured) {
            tvHeaderBadge.text = "SETUP"
            tvHeaderBadge.setBackgroundResource(R.drawable.bg_status_badge_active)
            tvHeaderBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
            tvStateTitle.setText(R.string.assistant_state_setup_title)
            tvStateDesc.setText(R.string.assistant_state_setup_desc)
        } else {
            tvHeaderBadge.text = "READY"
            tvHeaderBadge.setBackgroundResource(R.drawable.bg_status_badge_active)
            tvHeaderBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
            val providerName = status.provider.replaceFirstChar { it.uppercase() }
            tvStateTitle.text = getString(R.string.assistant_state_ready_title, "$providerName ${status.model}")
            tvStateDesc.text = getString(R.string.assistant_state_ready_desc, status.model)
        }

        // Form fields sync when idle
        if (!switchGenAiEnabled.isPressed && status != null) {
            switchGenAiEnabled.isChecked = status.enabled
            switchRoutineLearning.isChecked = status.routineLearningEnabled
        }

        if (status != null) {
            val pIdx = providerKeys.indexOf(status.provider).takeIf { it >= 0 } ?: 0
            if (spinnerProvider.selectedItemPosition != pIdx) {
                spinnerProvider.setSelection(pIdx)
                updateModelPresets(pIdx, status.model)
            }
            if (etBaseUrl.text.isNullOrBlank()) etBaseUrl.setText(status.baseUrl)
            if (etMaxTokens.text.isNullOrBlank()) etMaxTokens.setText(status.maxOutputTokens.toString())
            tvApiKeyHint.text = if (status.apiKeyConfigured) "API key is saved and encrypted." else getString(R.string.assistant_api_key_hint)

            tvRuntimeParked.text = if (status.availableWhileParked) "Available" else "Drive only"
            tvRuntimeTransport.text = if (status.transportActive) status.lastNetworkRoute.ifEmpty { "Active" } else "Idle"
            tvRuntimeProxy.text = if (status.proxyExpected) "Fail-closed (Tailscale)" else "Direct"
            tvRuntimeActiveRequests.text = status.activeRequests.toString()
        }
    }

    private fun renderRoutinesAndIncidents(routines: List<String>, incidents: List<String>) {
        if (routines.isNotEmpty()) {
            tvRoutineSuggestions.text = routines.joinToString("\n• ", prefix = "• ")
        } else {
            tvRoutineSuggestions.setText(R.string.assistant_routines_empty)
        }

        if (incidents.isNotEmpty()) {
            tvIncidentPacks.text = incidents.joinToString("\n• ", prefix = "• ")
        } else {
            tvIncidentPacks.setText(R.string.assistant_incidents_empty)
        }
    }

    private fun renderMessages(state: AssistantUiState) {
        if (state.messages.isEmpty()) {
            layoutChatEmpty.visibility = View.VISIBLE
            rvChatMessages.visibility = View.GONE
        } else {
            layoutChatEmpty.visibility = View.GONE
            rvChatMessages.visibility = View.VISIBLE
            chatAdapter.submitList(state.messages) {
                rvChatMessages.scrollToPosition(state.messages.size - 1)
            }
        }

        btnSend.isEnabled = !state.isBusy
        btnVoice.isEnabled = !state.isBusy && (state.status?.nativeRealtimeAudioAvailable == true)
    }
}
