package com.overdrive.app.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.overdrive.app.R
import com.overdrive.app.ui.integrations.IntegrationTab
import com.overdrive.app.ui.integrations.IntegrationsSummary
import com.overdrive.app.ui.integrations.IntegrationsViewModel
import com.overdrive.app.ui.integrations.MqttBrokerConfig
import com.overdrive.app.ui.integrations.MqttConnectionsAdapter
import com.overdrive.app.ui.util.navigateDrillDown
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IntegrationsNativeFragment : Fragment() {

    private val viewModel: IntegrationsViewModel by viewModels()

    // Header & Tabs
    private lateinit var tvMasterStatusBadge: TextView
    private lateinit var tabBtnOverview: MaterialButton
    private lateinit var tabBtnTelegram: MaterialButton
    private lateinit var tabBtnAbrp: MaterialButton
    private lateinit var tabBtnMqtt: MaterialButton
    private lateinit var tabBtnBydCloud: MaterialButton

    // Tab Containers
    private lateinit var layoutOverviewTab: NestedScrollView
    private lateinit var layoutTelegramTab: NestedScrollView
    private lateinit var layoutAbrpTab: NestedScrollView
    private lateinit var layoutMqttTab: NestedScrollView
    private lateinit var layoutBydCloudTab: NestedScrollView

    // Overview Hub Views
    private lateinit var hubDotTelegram: View
    private lateinit var hubTvTelegramStatus: TextView
    private lateinit var hubBtnTelegramConfig: MaterialButton
    private lateinit var hubBtnTelegramLegacy: MaterialButton

    private lateinit var hubDotAbrp: View
    private lateinit var hubTvAbrpStatus: TextView
    private lateinit var hubBtnAbrpConfig: MaterialButton
    private lateinit var hubBtnAbrpLegacy: MaterialButton

    private lateinit var hubDotMqtt: View
    private lateinit var hubTvMqttStatus: TextView
    private lateinit var hubBtnMqttConfig: MaterialButton
    private lateinit var hubBtnMqttLegacy: MaterialButton

    private lateinit var hubDotBydCloud: View
    private lateinit var hubTvBydCloudStatus: TextView
    private lateinit var hubBtnBydCloudConfig: MaterialButton
    private lateinit var hubBtnBydCloudLegacy: MaterialButton

    // Telegram Tab Views
    private lateinit var tgLayoutBotInfo: LinearLayout
    private lateinit var tvTgBotUsername: TextView
    private lateinit var btnTgClearToken: MaterialButton
    private lateinit var etTgToken: EditText
    private lateinit var btnTgSaveToken: MaterialButton
    private lateinit var tgLayoutPaired: LinearLayout
    private lateinit var tvTgOwnerDetails: TextView
    private lateinit var btnTgUnpair: MaterialButton
    private lateinit var tgLayoutNotPaired: LinearLayout
    private lateinit var tvTgPinDisplay: TextView
    private lateinit var btnTgGeneratePin: MaterialButton
    private lateinit var switchTgCritical: MaterialSwitch
    private lateinit var switchTgMotion: MaterialSwitch
    private lateinit var switchTgVideo: MaterialSwitch
    private lateinit var switchTgTyre: MaterialSwitch
    private lateinit var btnTgSavePrefs: MaterialButton

    // ABRP Tab Views
    private lateinit var switchAbrpEnable: MaterialSwitch
    private lateinit var etAbrpToken: EditText
    private lateinit var btnAbrpClearToken: MaterialButton
    private lateinit var btnAbrpSaveToken: MaterialButton
    private lateinit var tvAbrpSoc: TextView
    private lateinit var tvAbrpPower: TextView
    private lateinit var tvAbrpSpeed: TextView

    // MQTT Tab Views
    private lateinit var rvMqttConnections: RecyclerView
    private lateinit var tvMqttEmptyState: TextView
    private lateinit var mqttAdapter: MqttConnectionsAdapter
    private lateinit var etMqttName: EditText
    private lateinit var etMqttHost: EditText
    private lateinit var etMqttPort: EditText
    private lateinit var etMqttUser: EditText
    private lateinit var etMqttPass: EditText
    private lateinit var etMqttTopic: EditText
    private lateinit var switchMqttTls: MaterialSwitch
    private lateinit var btnMqttSaveBroker: MaterialButton

    // BYD Cloud Tab Views
    private lateinit var spnBydCountry: Spinner
    private lateinit var etBydUsername: EditText
    private lateinit var etBydPassword: EditText
    private lateinit var etBydPin: EditText
    private lateinit var btnBydLogin: MaterialButton
    private lateinit var btnBydClear: MaterialButton
    private lateinit var cardBydStatus: MaterialCardView
    private lateinit var tvBydVin: TextView
    private lateinit var btnBydTestLights: MaterialButton
    private lateinit var btnBydTestHorn: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_integrations_native, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupTabButtons()
        setupOverviewActions()
        setupTelegramActions()
        setupAbrpActions()
        setupMqttActions()
        setupBydCloudActions()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAll()
    }

    private fun initViews(view: View) {
        tvMasterStatusBadge = view.findViewById(R.id.tvMasterStatusBadge)
        tabBtnOverview = view.findViewById(R.id.tabBtnOverview)
        tabBtnTelegram = view.findViewById(R.id.tabBtnTelegram)
        tabBtnAbrp = view.findViewById(R.id.tabBtnAbrp)
        tabBtnMqtt = view.findViewById(R.id.tabBtnMqtt)
        tabBtnBydCloud = view.findViewById(R.id.tabBtnBydCloud)

        layoutOverviewTab = view.findViewById(R.id.layoutOverviewTab)
        layoutTelegramTab = view.findViewById(R.id.layoutTelegramTab)
        layoutAbrpTab = view.findViewById(R.id.layoutAbrpTab)
        layoutMqttTab = view.findViewById(R.id.layoutMqttTab)
        layoutBydCloudTab = view.findViewById(R.id.layoutBydCloudTab)

        // Hub Views
        hubDotTelegram = view.findViewById(R.id.hubDotTelegram)
        hubTvTelegramStatus = view.findViewById(R.id.hubTvTelegramStatus)
        hubBtnTelegramConfig = view.findViewById(R.id.hubBtnTelegramConfig)
        hubBtnTelegramLegacy = view.findViewById(R.id.hubBtnTelegramLegacy)

        hubDotAbrp = view.findViewById(R.id.hubDotAbrp)
        hubTvAbrpStatus = view.findViewById(R.id.hubTvAbrpStatus)
        hubBtnAbrpConfig = view.findViewById(R.id.hubBtnAbrpConfig)
        hubBtnAbrpLegacy = view.findViewById(R.id.hubBtnAbrpLegacy)

        hubDotMqtt = view.findViewById(R.id.hubDotMqtt)
        hubTvMqttStatus = view.findViewById(R.id.hubTvMqttStatus)
        hubBtnMqttConfig = view.findViewById(R.id.hubBtnMqttConfig)
        hubBtnMqttLegacy = view.findViewById(R.id.hubBtnMqttLegacy)

        hubDotBydCloud = view.findViewById(R.id.hubDotBydCloud)
        hubTvBydCloudStatus = view.findViewById(R.id.hubTvBydCloudStatus)
        hubBtnBydCloudConfig = view.findViewById(R.id.hubBtnBydCloudConfig)
        hubBtnBydCloudLegacy = view.findViewById(R.id.hubBtnBydCloudLegacy)

        // Telegram Views
        tgLayoutBotInfo = view.findViewById(R.id.tgLayoutBotInfo)
        tvTgBotUsername = view.findViewById(R.id.tvTgBotUsername)
        btnTgClearToken = view.findViewById(R.id.btnTgClearToken)
        etTgToken = view.findViewById(R.id.etTgToken)
        btnTgSaveToken = view.findViewById(R.id.btnTgSaveToken)
        tgLayoutPaired = view.findViewById(R.id.tgLayoutPaired)
        tvTgOwnerDetails = view.findViewById(R.id.tvTgOwnerDetails)
        btnTgUnpair = view.findViewById(R.id.btnTgUnpair)
        tgLayoutNotPaired = view.findViewById(R.id.tgLayoutNotPaired)
        tvTgPinDisplay = view.findViewById(R.id.tvTgPinDisplay)
        btnTgGeneratePin = view.findViewById(R.id.btnTgGeneratePin)
        switchTgCritical = view.findViewById(R.id.switchTgCritical)
        switchTgMotion = view.findViewById(R.id.switchTgMotion)
        switchTgVideo = view.findViewById(R.id.switchTgVideo)
        switchTgTyre = view.findViewById(R.id.switchTgTyre)
        btnTgSavePrefs = view.findViewById(R.id.btnTgSavePrefs)

        // ABRP Views
        switchAbrpEnable = view.findViewById(R.id.switchAbrpEnable)
        etAbrpToken = view.findViewById(R.id.etAbrpToken)
        btnAbrpClearToken = view.findViewById(R.id.btnAbrpClearToken)
        btnAbrpSaveToken = view.findViewById(R.id.btnAbrpSaveToken)
        tvAbrpSoc = view.findViewById(R.id.tvAbrpSoc)
        tvAbrpPower = view.findViewById(R.id.tvAbrpPower)
        tvAbrpSpeed = view.findViewById(R.id.tvAbrpSpeed)

        // MQTT Views
        rvMqttConnections = view.findViewById(R.id.rvMqttConnections)
        tvMqttEmptyState = view.findViewById(R.id.tvMqttEmptyState)
        etMqttName = view.findViewById(R.id.etMqttName)
        etMqttHost = view.findViewById(R.id.etMqttHost)
        etMqttPort = view.findViewById(R.id.etMqttPort)
        etMqttUser = view.findViewById(R.id.etMqttUser)
        etMqttPass = view.findViewById(R.id.etMqttPass)
        etMqttTopic = view.findViewById(R.id.etMqttTopic)
        switchMqttTls = view.findViewById(R.id.switchMqttTls)
        btnMqttSaveBroker = view.findViewById(R.id.btnMqttSaveBroker)

        mqttAdapter = MqttConnectionsAdapter { broker ->
            viewModel.deleteMqttBroker(broker.id)
        }
        rvMqttConnections.layoutManager = LinearLayoutManager(requireContext())
        rvMqttConnections.adapter = mqttAdapter

        // BYD Cloud Views
        spnBydCountry = view.findViewById(R.id.spnBydCountry)
        etBydUsername = view.findViewById(R.id.etBydUsername)
        etBydPassword = view.findViewById(R.id.etBydPassword)
        etBydPin = view.findViewById(R.id.etBydPin)
        btnBydLogin = view.findViewById(R.id.btnBydLogin)
        btnBydClear = view.findViewById(R.id.btnBydClear)
        cardBydStatus = view.findViewById(R.id.cardBydStatus)
        tvBydVin = view.findViewById(R.id.tvBydVin)
        btnBydTestLights = view.findViewById(R.id.btnBydTestLights)
        btnBydTestHorn = view.findViewById(R.id.btnBydTestHorn)

        val countries = listOf("GB (United Kingdom / Europe)", "DE (Germany / Europe)", "AU (Australia)", "NZ (New Zealand)", "SG (Singapore)", "TH (Thailand)", "IL (Israel)", "BR (Brazil)")
        spnBydCountry.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, countries)
    }

    private fun setupTabButtons() {
        tabBtnOverview.setOnClickListener { viewModel.selectTab(IntegrationTab.OVERVIEW) }
        tabBtnTelegram.setOnClickListener { viewModel.selectTab(IntegrationTab.TELEGRAM) }
        tabBtnAbrp.setOnClickListener { viewModel.selectTab(IntegrationTab.ABRP) }
        tabBtnMqtt.setOnClickListener { viewModel.selectTab(IntegrationTab.MQTT) }
        tabBtnBydCloud.setOnClickListener { viewModel.selectTab(IntegrationTab.BYD_CLOUD) }
    }

    private fun setupOverviewActions() {
        hubBtnTelegramConfig.setOnClickListener { viewModel.selectTab(IntegrationTab.TELEGRAM) }
        hubBtnTelegramLegacy.setOnClickListener {
            findNavController().navigateDrillDown(R.id.telegramSettingsFragment)
        }

        hubBtnAbrpConfig.setOnClickListener { viewModel.selectTab(IntegrationTab.ABRP) }
        hubBtnAbrpLegacy.setOnClickListener {
            findNavController().navigateDrillDown(R.id.abrpSettingsFragment)
        }

        hubBtnMqttConfig.setOnClickListener { viewModel.selectTab(IntegrationTab.MQTT) }
        hubBtnMqttLegacy.setOnClickListener {
            findNavController().navigateDrillDown(R.id.mqttFragment)
        }

        hubBtnBydCloudConfig.setOnClickListener { viewModel.selectTab(IntegrationTab.BYD_CLOUD) }
        hubBtnBydCloudLegacy.setOnClickListener {
            findNavController().navigateDrillDown(R.id.bydCloudFragment)
        }
    }

    private fun setupTelegramActions() {
        btnTgSaveToken.setOnClickListener {
            val token = etTgToken.text.toString().trim()
            if (token.isNotEmpty()) {
                viewModel.saveTelegramToken(token)
            }
        }
        btnTgClearToken.setOnClickListener {
            viewModel.saveTelegramToken("")
        }
        btnTgGeneratePin.setOnClickListener {
            viewModel.generateTelegramPin()
        }
        btnTgUnpair.setOnClickListener {
            viewModel.unpairTelegram()
        }
        btnTgSavePrefs.setOnClickListener {
            viewModel.saveTelegramPreferences(
                videoUploads = switchTgVideo.isChecked,
                criticalAlerts = switchTgCritical.isChecked,
                motionText = switchTgMotion.isChecked,
                tyreAlerts = switchTgTyre.isChecked
            )
        }
    }

    private fun setupAbrpActions() {
        switchAbrpEnable.setOnCheckedChangeListener { _, isChecked ->
            val token = etAbrpToken.text.toString().trim()
            viewModel.saveAbrpConfig(token, isChecked, "BYD ATTO 3")
        }
        btnAbrpSaveToken.setOnClickListener {
            val token = etAbrpToken.text.toString().trim()
            viewModel.saveAbrpConfig(token, switchAbrpEnable.isChecked, "BYD ATTO 3")
        }
        btnAbrpClearToken.setOnClickListener {
            viewModel.deleteAbrpToken()
        }
    }

    private fun setupMqttActions() {
        btnMqttSaveBroker.setOnClickListener {
            val host = etMqttHost.text.toString().trim()
            if (host.isEmpty()) return@setOnClickListener
            val port = etMqttPort.text.toString().toIntOrNull() ?: 1883
            val name = etMqttName.text.toString().ifBlank { "MQTT Broker" }
            val username = etMqttUser.text.toString()
            val topic = etMqttTopic.text.toString().ifBlank { "overdrive" }
            val tls = switchMqttTls.isChecked

            val broker = MqttBrokerConfig(
                name = name,
                host = host,
                port = port,
                username = username,
                topicPrefix = topic,
                useTls = tls,
                enabled = true
            )
            viewModel.addMqttBroker(broker)
            etMqttHost.text.clear()
            etMqttUser.text.clear()
            etMqttPass.text.clear()
        }
    }

    private fun setupBydCloudActions() {
        btnBydLogin.setOnClickListener {
            val username = etBydUsername.text.toString().trim()
            val password = etBydPassword.text.toString()
            val pin = etBydPin.text.toString().trim()
            val country = spnBydCountry.selectedItem?.toString()?.substring(0, 2) ?: "GB"
            if (username.isNotEmpty() && password.isNotEmpty()) {
                viewModel.saveBydCloudSetup(username, password, pin, country)
            }
        }
        btnBydClear.setOnClickListener {
            viewModel.clearBydCloud()
        }
        btnBydTestLights.setOnClickListener {
            viewModel.testBydCloudCommand("flash_lights")
        }
        btnBydTestHorn.setOnClickListener {
            viewModel.testBydCloudCommand("horn")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTab.collectLatest { tab ->
                updateTabPills(tab)
                layoutOverviewTab.visibility = if (tab == IntegrationTab.OVERVIEW) View.VISIBLE else View.GONE
                layoutTelegramTab.visibility = if (tab == IntegrationTab.TELEGRAM) View.VISIBLE else View.GONE
                layoutAbrpTab.visibility = if (tab == IntegrationTab.ABRP) View.VISIBLE else View.GONE
                layoutMqttTab.visibility = if (tab == IntegrationTab.MQTT) View.VISIBLE else View.GONE
                layoutBydCloudTab.visibility = if (tab == IntegrationTab.BYD_CLOUD) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summary.collectLatest { summary ->
                updateSummary(summary)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.telegramState.collectLatest { tg ->
                if (tg.hasToken) {
                    tgLayoutBotInfo.visibility = View.VISIBLE
                    tvTgBotUsername.text = if (tg.botUsername.isNotBlank()) "@${tg.botUsername}" else "Bot Token Active"
                    etTgToken.hint = "••••••••••••••••••••••••••••••••"
                } else {
                    tgLayoutBotInfo.visibility = View.GONE
                    etTgToken.hint = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
                }

                if (tg.isPaired) {
                    tgLayoutPaired.visibility = View.VISIBLE
                    tgLayoutNotPaired.visibility = View.GONE
                    tvTgOwnerDetails.text = "Paired with: ${tg.ownerFirstName} (${tg.ownerUsername})"
                } else {
                    tgLayoutPaired.visibility = View.GONE
                    tgLayoutNotPaired.visibility = View.VISIBLE
                    if (tg.pinCode.isNotBlank()) {
                        tvTgPinDisplay.visibility = View.VISIBLE
                        tvTgPinDisplay.text = tg.pinCode
                    } else {
                        tvTgPinDisplay.visibility = View.GONE
                    }
                }

                switchTgCritical.isChecked = tg.criticalAlerts
                switchTgMotion.isChecked = tg.motionText
                switchTgVideo.isChecked = tg.videoUploads
                switchTgTyre.isChecked = tg.tyreAlerts
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.abrpState.collectLatest { abrp ->
                switchAbrpEnable.isChecked = abrp.running
                if (abrp.hasToken && etAbrpToken.text.isEmpty()) {
                    etAbrpToken.hint = "••••••••-••••-••••-••••-••••••••••••"
                }
                tvAbrpSoc.text = String.format("%.1f %%", abrp.socPercent)
                tvAbrpPower.text = String.format("%.1f kW", abrp.powerKw)
                tvAbrpSpeed.text = String.format("%.0f km/h", abrp.speedKmh)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mqttBrokers.collectLatest { brokers ->
                mqttAdapter.submitList(brokers)
                tvMqttEmptyState.visibility = if (brokers.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bydCloudState.collectLatest { byd ->
                if (byd.isConfigured) {
                    cardBydStatus.visibility = View.VISIBLE
                    tvBydVin.text = if (byd.vin.isNotBlank()) "VIN: ${byd.vin}" else "Account verified: ${byd.username}"
                    etBydUsername.hint = byd.username
                } else {
                    cardBydStatus.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statusMessage.collectLatest { msg ->
                msg?.let {
                    Snackbar.make(requireView(), it, Snackbar.LENGTH_SHORT).show()
                    viewModel.clearMessages()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { err ->
                err?.let {
                    Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                    viewModel.clearMessages()
                }
            }
        }
    }

    private fun updateTabPills(active: IntegrationTab) {
        val buttons = listOf(tabBtnOverview, tabBtnTelegram, tabBtnAbrp, tabBtnMqtt, tabBtnBydCloud)
        val tabs = listOf(IntegrationTab.OVERVIEW, IntegrationTab.TELEGRAM, IntegrationTab.ABRP, IntegrationTab.MQTT, IntegrationTab.BYD_CLOUD)

        buttons.forEachIndexed { i, btn ->
            val isSelected = tabs[i] == active
            if (isSelected) {
                btn.setBackgroundResource(R.drawable.bg_pill_state)
            } else {
                btn.background = null
            }
        }
    }

    private fun updateSummary(summary: IntegrationsSummary) {
        // Master status badge
        if (summary.allConfigured) {
            tvMasterStatusBadge.text = "ALL CONNECTED"
            tvMasterStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_active)
        } else {
            tvMasterStatusBadge.text = "${summary.activeCount}/4 ACTIVE"
            tvMasterStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_inactive)
        }

        // Telegram hub card
        hubDotTelegram.setBackgroundResource(if (summary.telegramConfigured) R.drawable.status_dot_online else R.drawable.status_dot_offline)
        hubTvTelegramStatus.text = if (summary.telegramConfigured) "CONFIGURED" else "NOT SET UP"

        // ABRP hub card
        hubDotAbrp.setBackgroundResource(if (summary.abrpConnected) R.drawable.status_dot_online else R.drawable.status_dot_offline)
        hubTvAbrpStatus.text = if (summary.abrpConnected) "CONNECTED" else "STOPPED"

        // MQTT hub card
        hubDotMqtt.setBackgroundResource(if (summary.mqttConnected) R.drawable.status_dot_online else R.drawable.status_dot_offline)
        hubTvMqttStatus.text = if (summary.mqttConnected) "CONNECTED" else "INACTIVE"

        // BYD Cloud hub card
        hubDotBydCloud.setBackgroundResource(if (summary.bydCloudConfigured) R.drawable.status_dot_online else R.drawable.status_dot_offline)
        hubTvBydCloudStatus.text = if (summary.bydCloudConfigured) "CONFIGURED" else "NOT SET UP"
    }
}
