package com.overdrive.app.ui.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IntegrationsNativeFragment : Fragment() {

    companion object {
        const val ARG_INITIAL_TAB = "initial_tab"
        const val TAB_OVERVIEW = "overview"
        const val TAB_TELEGRAM = "telegram"
        const val TAB_ABRP = "abrp"
        const val TAB_MQTT = "mqtt"
        const val TAB_BYD_CLOUD = "byd_cloud"
    }

    private val viewModel: IntegrationsViewModel by viewModels()

    // Overview Containers & Cards
    private lateinit var layoutOverviewContainer: NestedScrollView
    private lateinit var cardTelegram: MaterialCardView
    private lateinit var cardAbrp: MaterialCardView
    private lateinit var cardMqtt: MaterialCardView
    private lateinit var cardBydCloud: MaterialCardView

    // Overview Status Elements
    private lateinit var heroStatusPill: MaterialCardView
    private lateinit var tvHeroStatus: TextView
    private lateinit var dotTelegram: View
    private lateinit var tvTelegramStatus: TextView
    private lateinit var dotAbrp: View
    private lateinit var tvAbrpStatus: TextView
    private lateinit var dotMqtt: View
    private lateinit var tvMqttStatus: TextView
    private lateinit var dotBydCloud: View
    private lateinit var tvBydCloudStatus: TextView

    // Detail Container & Top Bar
    private lateinit var layoutDetailContainer: LinearLayout
    private lateinit var btnBackToOverview: LinearLayout
    private lateinit var tvDetailTitle: TextView

    // Detail Tabs
    private lateinit var layoutTelegramTab: NestedScrollView
    private lateinit var layoutAbrpTab: NestedScrollView
    private lateinit var layoutMqttTab: NestedScrollView
    private lateinit var layoutBydCloudTab: NestedScrollView

    // Telegram Sub-Cards
    private lateinit var cardTgBot: MaterialCardView
    private lateinit var cardTgPair: MaterialCardView
    private lateinit var cardTgPrefs: MaterialCardView
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

    // ABRP Sub-Cards
    private lateinit var cardAbrpStatus: MaterialCardView
    private lateinit var cardAbrpToken: MaterialCardView
    private lateinit var cardAbrpTelemetry: MaterialCardView
    private lateinit var switchAbrpEnable: MaterialSwitch
    private lateinit var etAbrpToken: EditText
    private lateinit var btnAbrpClearToken: MaterialButton
    private lateinit var btnAbrpSaveToken: MaterialButton
    private lateinit var tvAbrpSoc: TextView
    private lateinit var tvAbrpPower: TextView
    private lateinit var tvAbrpSpeed: TextView

    // MQTT Sub-Cards
    private lateinit var cardMqttConnections: MaterialCardView
    private lateinit var cardMqttAdd: MaterialCardView
    private lateinit var cardMqttTelemetry: MaterialCardView
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

    // BYD Cloud Sub-Cards
    private lateinit var cardBydAccount: MaterialCardView
    private lateinit var cardBydStatus: MaterialCardView
    private lateinit var cardBydAdvanced: MaterialCardView
    private lateinit var spnBydCountry: Spinner
    private lateinit var etBydUsername: EditText
    private lateinit var etBydPassword: EditText
    private lateinit var etBydPin: EditText
    private lateinit var btnBydLogin: MaterialButton
    private lateinit var btnBydClear: MaterialButton
    private lateinit var tvBydVin: TextView
    private lateinit var btnBydTestLights: MaterialButton
    private lateinit var btnBydTestHorn: MaterialButton

    // Sticky Bottom Sub-Tabs
    private lateinit var layoutSubBottomTabsBar: LinearLayout
    private lateinit var tabSub1: LinearLayout
    private lateinit var ivSubTab1: ImageView
    private lateinit var tvSubTab1: TextView
    private lateinit var tabSub2: LinearLayout
    private lateinit var ivSubTab2: ImageView
    private lateinit var tvSubTab2: TextView
    private lateinit var tabSub3: LinearLayout
    private lateinit var ivSubTab3: ImageView
    private lateinit var tvSubTab3: TextView

    private var activeSubTabIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_integrations_native, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupOverviewNavigation(view)
        setupDetailNavigation()
        setupTelegramActions()
        setupAbrpActions()
        setupMqttActions()
        setupBydCloudActions()
        setupBackPressedHandler()
        val initialTab = arguments?.getString(ARG_INITIAL_TAB)
        if (!initialTab.isNullOrEmpty()) {
            val targetTab = when (initialTab.lowercase()) {
                TAB_TELEGRAM, "telegram" -> IntegrationTab.TELEGRAM
                TAB_ABRP, "abrp" -> IntegrationTab.ABRP
                TAB_MQTT, "mqtt" -> IntegrationTab.MQTT
                TAB_BYD_CLOUD, "byd_cloud", "byd-cloud" -> IntegrationTab.BYD_CLOUD
                else -> null
            }
            if (targetTab != null) {
                viewModel.selectTab(targetTab)
            }
        }
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAll()
    }

    private fun initViews(view: View) {
        // Overview
        layoutOverviewContainer = view.findViewById(R.id.layoutOverviewContainer)
        heroStatusPill = view.findViewById(R.id.heroStatusPill)
        tvHeroStatus = view.findViewById(R.id.tvHeroStatus)

        cardTelegram = view.findViewById(R.id.cardTelegram)
        dotTelegram = view.findViewById(R.id.dotTelegram)
        tvTelegramStatus = view.findViewById(R.id.tvTelegramStatus)

        cardAbrp = view.findViewById(R.id.cardAbrp)
        dotAbrp = view.findViewById(R.id.dotAbrp)
        tvAbrpStatus = view.findViewById(R.id.tvAbrpStatus)

        cardMqtt = view.findViewById(R.id.cardMqtt)
        dotMqtt = view.findViewById(R.id.dotMqtt)
        tvMqttStatus = view.findViewById(R.id.tvMqttStatus)

        cardBydCloud = view.findViewById(R.id.cardBydCloud)
        dotBydCloud = view.findViewById(R.id.dotBydCloud)
        tvBydCloudStatus = view.findViewById(R.id.tvBydCloudStatus)

        // Detail
        layoutDetailContainer = view.findViewById(R.id.layoutDetailContainer)
        btnBackToOverview = view.findViewById(R.id.btnBackToOverview)
        tvDetailTitle = view.findViewById(R.id.tvDetailTitle)

        layoutTelegramTab = view.findViewById(R.id.layoutTelegramTab)
        layoutAbrpTab = view.findViewById(R.id.layoutAbrpTab)
        layoutMqttTab = view.findViewById(R.id.layoutMqttTab)
        layoutBydCloudTab = view.findViewById(R.id.layoutBydCloudTab)

        // Telegram Sub-Cards
        cardTgBot = view.findViewById(R.id.cardTgBot)
        cardTgPair = view.findViewById(R.id.cardTgPair)
        cardTgPrefs = view.findViewById(R.id.cardTgPrefs)
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

        // ABRP Sub-Cards
        cardAbrpStatus = view.findViewById(R.id.cardAbrpStatus)
        cardAbrpToken = view.findViewById(R.id.cardAbrpToken)
        cardAbrpTelemetry = view.findViewById(R.id.cardAbrpTelemetry)
        switchAbrpEnable = view.findViewById(R.id.switchAbrpEnable)
        etAbrpToken = view.findViewById(R.id.etAbrpToken)
        btnAbrpClearToken = view.findViewById(R.id.btnAbrpClearToken)
        btnAbrpSaveToken = view.findViewById(R.id.btnAbrpSaveToken)
        tvAbrpSoc = view.findViewById(R.id.tvAbrpSoc)
        tvAbrpPower = view.findViewById(R.id.tvAbrpPower)
        tvAbrpSpeed = view.findViewById(R.id.tvAbrpSpeed)

        // MQTT Sub-Cards
        cardMqttConnections = view.findViewById(R.id.cardMqttConnections)
        cardMqttAdd = view.findViewById(R.id.cardMqttAdd)
        cardMqttTelemetry = view.findViewById(R.id.cardMqttTelemetry)
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

        // BYD Cloud Sub-Cards
        cardBydAccount = view.findViewById(R.id.cardBydAccount)
        cardBydStatus = view.findViewById(R.id.cardBydStatus)
        cardBydAdvanced = view.findViewById(R.id.cardBydAdvanced)
        spnBydCountry = view.findViewById(R.id.spnBydCountry)
        etBydUsername = view.findViewById(R.id.etBydUsername)
        etBydPassword = view.findViewById(R.id.etBydPassword)
        etBydPin = view.findViewById(R.id.etBydPin)
        btnBydLogin = view.findViewById(R.id.btnBydLogin)
        btnBydClear = view.findViewById(R.id.btnBydClear)
        tvBydVin = view.findViewById(R.id.tvBydVin)
        btnBydTestLights = view.findViewById(R.id.btnBydTestLights)
        btnBydTestHorn = view.findViewById(R.id.btnBydTestHorn)

        val countries = listOf("GB (United Kingdom / Europe)", "DE (Germany / Europe)", "AU (Australia)", "NZ (New Zealand)", "SG (Singapore)", "TH (Thailand)", "IL (Israel)", "BR (Brazil)")
        spnBydCountry.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, countries)

        // Sticky Bottom Sub-Tabs
        layoutSubBottomTabsBar = view.findViewById(R.id.layoutSubBottomTabsBar)
        tabSub1 = view.findViewById(R.id.tabSub1)
        ivSubTab1 = view.findViewById(R.id.ivSubTab1)
        tvSubTab1 = view.findViewById(R.id.tvSubTab1)

        tabSub2 = view.findViewById(R.id.tabSub2)
        ivSubTab2 = view.findViewById(R.id.ivSubTab2)
        tvSubTab2 = view.findViewById(R.id.tvSubTab2)

        tabSub3 = view.findViewById(R.id.tabSub3)
        ivSubTab3 = view.findViewById(R.id.ivSubTab3)
        tvSubTab3 = view.findViewById(R.id.tvSubTab3)
    }

    private fun setupOverviewNavigation(rootView: View) {
        cardTelegram.setOnClickListener { viewModel.selectTab(IntegrationTab.TELEGRAM) }
        cardAbrp.setOnClickListener { viewModel.selectTab(IntegrationTab.ABRP) }
        cardMqtt.setOnClickListener { viewModel.selectTab(IntegrationTab.MQTT) }
        cardBydCloud.setOnClickListener { viewModel.selectTab(IntegrationTab.BYD_CLOUD) }

        rootView.findViewById<View>(R.id.btnTelegramConfigure)?.setOnClickListener { viewModel.selectTab(IntegrationTab.TELEGRAM) }
        rootView.findViewById<View>(R.id.btnAbrpConfigure)?.setOnClickListener { viewModel.selectTab(IntegrationTab.ABRP) }
        rootView.findViewById<View>(R.id.btnMqttConfigure)?.setOnClickListener { viewModel.selectTab(IntegrationTab.MQTT) }
        rootView.findViewById<View>(R.id.btnBydCloudConfigure)?.setOnClickListener { viewModel.selectTab(IntegrationTab.BYD_CLOUD) }
    }

    private fun setupDetailNavigation() {
        btnBackToOverview.setOnClickListener {
            val hasInitialTab = !arguments?.getString(ARG_INITIAL_TAB).isNullOrEmpty()
            if (hasInitialTab) {
                if (!findNavController().popBackStack()) {
                    viewModel.selectTab(IntegrationTab.OVERVIEW)
                }
            } else {
                viewModel.selectTab(IntegrationTab.OVERVIEW)
            }
        }

        tabSub1.setOnClickListener { selectSubTab(0) }
        tabSub2.setOnClickListener { selectSubTab(1) }
        tabSub3.setOnClickListener { selectSubTab(2) }
    }

    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val hasInitialTab = !arguments?.getString(ARG_INITIAL_TAB).isNullOrEmpty()
                if (hasInitialTab) {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                } else if (viewModel.activeTab.value != IntegrationTab.OVERVIEW) {
                    viewModel.selectTab(IntegrationTab.OVERVIEW)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
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
            val topic = etMqttTopic.text.toString().ifBlank { "overdrive/vehicle/telemetry" }
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
            selectSubTab(0)
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
                onTabChanged(tab)
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

    private fun onTabChanged(tab: IntegrationTab) {
        if (tab == IntegrationTab.OVERVIEW) {
            layoutOverviewContainer.visibility = View.VISIBLE
            layoutDetailContainer.visibility = View.GONE
            return
        }

        layoutOverviewContainer.visibility = View.GONE
        layoutDetailContainer.visibility = View.VISIBLE

        when (tab) {
            IntegrationTab.TELEGRAM -> {
                tvDetailTitle.setText(R.string.nav_page_telegram)
                setupSubTabViews(
                    icon1 = R.drawable.ic_smart_toy, label1 = R.string.integrations_subtab_bot,
                    icon2 = R.drawable.ic_link, label2 = R.string.integrations_subtab_pair,
                    icon3 = R.drawable.ic_settings, label3 = R.string.integrations_subtab_preferences
                )
                layoutTelegramTab.visibility = View.VISIBLE
                layoutAbrpTab.visibility = View.GONE
                layoutMqttTab.visibility = View.GONE
                layoutBydCloudTab.visibility = View.GONE
            }
            IntegrationTab.ABRP -> {
                tvDetailTitle.setText(R.string.nav_page_abrp)
                setupSubTabViews(
                    icon1 = R.drawable.ic_check_circle, label1 = R.string.integrations_subtab_status,
                    icon2 = R.drawable.ic_security_lock, label2 = R.string.integrations_subtab_token,
                    icon3 = R.drawable.ic_route, label3 = R.string.integrations_subtab_telemetry
                )
                layoutTelegramTab.visibility = View.GONE
                layoutAbrpTab.visibility = View.VISIBLE
                layoutMqttTab.visibility = View.GONE
                layoutBydCloudTab.visibility = View.GONE
            }
            IntegrationTab.MQTT -> {
                tvDetailTitle.setText(R.string.nav_page_mqtt)
                setupSubTabViews(
                    icon1 = R.drawable.ic_mqtt, label1 = R.string.integrations_subtab_connections,
                    icon2 = R.drawable.ic_add, label2 = R.string.integrations_subtab_add,
                    icon3 = R.drawable.ic_services, label3 = R.string.integrations_subtab_telemetry
                )
                layoutTelegramTab.visibility = View.GONE
                layoutAbrpTab.visibility = View.GONE
                layoutMqttTab.visibility = View.VISIBLE
                layoutBydCloudTab.visibility = View.GONE
            }
            IntegrationTab.BYD_CLOUD -> {
                tvDetailTitle.setText(R.string.nav_page_byd_cloud)
                setupSubTabViews(
                    icon1 = R.drawable.ic_person, label1 = R.string.integrations_subtab_account,
                    icon2 = R.drawable.ic_cloud, label2 = R.string.integrations_subtab_status,
                    icon3 = R.drawable.ic_vehicle_control, label3 = R.string.integrations_subtab_advanced
                )
                layoutTelegramTab.visibility = View.GONE
                layoutAbrpTab.visibility = View.GONE
                layoutMqttTab.visibility = View.GONE
                layoutBydCloudTab.visibility = View.VISIBLE
            }
            IntegrationTab.OVERVIEW -> { /* Handled above */ }
        }

        selectSubTab(0)
    }

    private fun setupSubTabViews(
        @DrawableRes icon1: Int, @StringRes label1: Int,
        @DrawableRes icon2: Int, @StringRes label2: Int,
        @DrawableRes icon3: Int, @StringRes label3: Int
    ) {
        ivSubTab1.setImageResource(icon1)
        tvSubTab1.setText(label1)

        ivSubTab2.setImageResource(icon2)
        tvSubTab2.setText(label2)

        ivSubTab3.setImageResource(icon3)
        tvSubTab3.setText(label3)
    }

    private fun selectSubTab(index: Int) {
        activeSubTabIndex = index
        val primaryColor = resolveAttrColor(androidx.appcompat.R.attr.colorPrimary)
        val onSurfaceVariant = resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
        val outlineColor = resolveAttrColor(com.google.android.material.R.attr.colorOutline)

        fun applyTab(layout: LinearLayout, iv: ImageView, tv: TextView, active: Boolean) {
            if (active) {
                layout.setBackgroundResource(R.drawable.bg_pill_state)
                iv.setColorFilter(primaryColor)
                tv.setTextColor(primaryColor)
                tv.typeface = Typeface.DEFAULT_BOLD
            } else {
                layout.setBackgroundColor(Color.TRANSPARENT)
                iv.setColorFilter(outlineColor)
                tv.setTextColor(onSurfaceVariant)
                tv.typeface = Typeface.DEFAULT
            }
        }

        applyTab(tabSub1, ivSubTab1, tvSubTab1, index == 0)
        applyTab(tabSub2, ivSubTab2, tvSubTab2, index == 1)
        applyTab(tabSub3, ivSubTab3, tvSubTab3, index == 2)

        when (viewModel.activeTab.value) {
            IntegrationTab.TELEGRAM -> {
                cardTgBot.visibility = if (index == 0) View.VISIBLE else View.GONE
                cardTgPair.visibility = if (index == 1) View.VISIBLE else View.GONE
                cardTgPrefs.visibility = if (index == 2) View.VISIBLE else View.GONE
            }
            IntegrationTab.ABRP -> {
                cardAbrpStatus.visibility = if (index == 0) View.VISIBLE else View.GONE
                cardAbrpToken.visibility = if (index == 1) View.VISIBLE else View.GONE
                cardAbrpTelemetry.visibility = if (index == 2) View.VISIBLE else View.GONE
            }
            IntegrationTab.MQTT -> {
                cardMqttConnections.visibility = if (index == 0) View.VISIBLE else View.GONE
                cardMqttAdd.visibility = if (index == 1) View.VISIBLE else View.GONE
                cardMqttTelemetry.visibility = if (index == 2) View.VISIBLE else View.GONE
            }
            IntegrationTab.BYD_CLOUD -> {
                cardBydAccount.visibility = if (index == 0) View.VISIBLE else View.GONE
                cardBydStatus.visibility = if (index == 1) View.VISIBLE else View.GONE
                cardBydAdvanced.visibility = if (index == 2) View.VISIBLE else View.GONE
            }
            IntegrationTab.OVERVIEW -> {}
        }
    }

    private fun updateSummary(summary: IntegrationsSummary) {
        // Hero Status Pill
        if (summary.allConfigured) {
            tvHeroStatus.setText(R.string.integrations_status_configured)
            heroStatusPill.setCardBackgroundColor(resolveAttrColor(com.google.android.material.R.attr.colorPrimaryContainer))
            tvHeroStatus.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnPrimaryContainer))
        } else {
            tvHeroStatus.setText(R.string.integrations_status_unknown)
            heroStatusPill.setCardBackgroundColor(resolveAttrColor(com.google.android.material.R.attr.colorSecondaryContainer))
            tvHeroStatus.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSecondaryContainer))
        }

        // Telegram Card
        bindStatus(dotTelegram, tvTelegramStatus, summary.telegramConfigured)

        // ABRP Card
        bindStatus(dotAbrp, tvAbrpStatus, summary.abrpConnected)

        // MQTT Card
        bindStatus(dotMqtt, tvMqttStatus, summary.mqttConnected)

        // BYD Cloud Card
        bindStatus(dotBydCloud, tvBydCloudStatus, summary.bydCloudConfigured)
    }

    private fun bindStatus(dot: View, label: TextView, configured: Boolean) {
        if (configured) {
            label.setText(R.string.integrations_status_configured)
            label.setTextColor(resolveAttrColor(androidx.appcompat.R.attr.colorPrimary))
            dot.setBackgroundResource(R.drawable.status_dot_online)
        } else {
            label.setText(R.string.integrations_status_not_set_up)
            label.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
            dot.setBackgroundResource(R.drawable.status_dot_offline)
        }
    }

    private fun resolveAttrColor(@AttrRes attr: Int): Int {
        val tv = TypedValue()
        requireContext().theme.resolveAttribute(attr, tv, true)
        return tv.data
    }
}
