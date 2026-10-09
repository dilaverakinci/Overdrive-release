package com.overdrive.app.ui.fragment

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.overdrive.app.R
import com.overdrive.app.ui.charging.ChargingCurveView
import com.overdrive.app.ui.charging.ChargingSample
import com.overdrive.app.ui.charging.ChargingSession
import com.overdrive.app.ui.charging.ChargingSessionAdapter
import com.overdrive.app.ui.charging.ChargingTab
import com.overdrive.app.ui.charging.ChargingUiState
import com.overdrive.app.ui.charging.ChargingViewModel
import com.overdrive.app.ui.charging.LocationTariff
import com.overdrive.app.ui.charging.PeriodFilter
import com.overdrive.app.ui.charging.SocGaugeView
import com.overdrive.app.ui.charging.SocHistoryChartView
import com.overdrive.app.ui.charging.TemperatureCurveView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChargingNativeFragment : Fragment() {

    private val viewModel: ChargingViewModel by viewModels()

    // Global loading & scroll
    private lateinit var progressLoading: ProgressBar
    private lateinit var scrollViewContent: NestedScrollView

    // Bottom Tabs
    private lateinit var layoutBottomTabsBar: LinearLayout
    private lateinit var tabBottomSessions: LinearLayout
    private lateinit var ivBottomTabSessions: ImageView
    private lateinit var tvBottomTabSessions: TextView
    private lateinit var tabBottomStats: LinearLayout
    private lateinit var ivBottomTabStats: ImageView
    private lateinit var tvBottomTabStats: TextView
    private lateinit var tabBottomSettings: LinearLayout
    private lateinit var ivBottomTabSettings: ImageView
    private lateinit var tvBottomTabSettings: TextView

    // Top Segmented Period Controls (Sessions Tab)
    private lateinit var btnFilter7d: MaterialButton
    private lateinit var btnFilter30d: MaterialButton
    private lateinit var btnFilterAll: MaterialButton
    private lateinit var btnFilterCustom: MaterialButton

    // Collapsible Custom Date Range Picker
    private lateinit var layoutCustomRangeRow: LinearLayout
    private lateinit var btnChargeFrom: MaterialButton
    private lateinit var btnChargeTo: MaterialButton
    private lateinit var btnApplyCustomRange: MaterialButton
    private val fromCalendar: Calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    private val toCalendar: Calendar = Calendar.getInstance()

    // Containers
    private lateinit var containerSessions: LinearLayout
    private lateinit var containerStats: LinearLayout
    private lateinit var containerSettings: LinearLayout
    private lateinit var containerDetail: LinearLayout

    // Sessions Tab: Summary Cards
    private lateinit var tvSummarySessions: TextView
    private lateinit var tvSummaryEnergy: TextView
    private lateinit var tvSummaryCost: TextView
    private lateinit var tvSummaryDcAc: TextView
    private lateinit var tvSummaryRangeGained: TextView
    private lateinit var cardSummaryEstimateDisclosure: MaterialCardView
    private lateinit var layoutSummaryEstimateHeader: LinearLayout
    private lateinit var tvSummaryEstimateToggle: TextView
    private lateinit var tvSummaryEstimateDesc: TextView

    // Sessions Tab: Live Hero Card
    private lateinit var cardLiveHero: MaterialCardView
    private lateinit var tvLiveStatus: TextView
    private lateinit var tvLiveSoC: TextView
    private lateinit var tvLiveDetails: TextView

    // Sessions Tab: Session List & Sorting
    private lateinit var spinnerSortSessions: Spinner
    private lateinit var rvChargingSessions: RecyclerView
    private lateinit var cardEmptySessions: MaterialCardView
    private lateinit var sessionAdapter: ChargingSessionAdapter
    private var isSortOldestFirst = false

    // Stats Tab Views
    private lateinit var btnStats7d: MaterialButton
    private lateinit var btnStats30d: MaterialButton
    private lateinit var btnStatsAll: MaterialButton
    private lateinit var socGaugeView: SocGaugeView
    private lateinit var tvStatsRange: TextView
    private lateinit var tvStatsSoh: TextView
    private lateinit var tvStatsAvgPowerLabel: TextView
    private lateinit var tvStatsAvgPower: TextView
    private lateinit var tvStatsAvgPowerUnit: TextView
    private lateinit var tvStatsAvgPowerSub: TextView
    private lateinit var tvStatsCostLabel: TextView
    private lateinit var tvStatsCostPerKwh: TextView
    private lateinit var tvStatsCostSub: TextView
    private lateinit var cardCompletionHero: MaterialCardView
    private lateinit var tvCompletionValue: TextView
    private lateinit var tvCompletionSub: TextView
    private lateinit var cardStatsEstimateDisclosure: MaterialCardView
    private lateinit var layoutStatsEstimateHeader: LinearLayout
    private lateinit var tvStatsEstimateToggle: TextView
    private lateinit var tvStatsEstimateDesc: TextView
    private lateinit var btnSoc24h: MaterialButton
    private lateinit var btnSoc7d: MaterialButton
    private lateinit var btnSoc30d: MaterialButton
    private lateinit var viewStatsSocCurve: SocHistoryChartView
    private lateinit var tvLifetimeEnergy: TextView
    private lateinit var tvLifetimeSessions: TextView
    private lateinit var tvLifetimeCost: TextView
    private lateinit var statsEmptyState: MaterialCardView

    // Settings Tab Views
    private lateinit var switchAutoRecord: MaterialSwitch
    private lateinit var spinnerCurrency: Spinner
    private lateinit var etElectricityRate: TextInputEditText
    private lateinit var etDcRate: TextInputEditText
    private lateinit var btnApplySettings: MaterialButton
    private lateinit var btnTariffAdd: MaterialButton
    private lateinit var btnClearHistory: MaterialButton

    // Tariffs Section Views
    private lateinit var tvTariffFallbackNote: TextView
    private lateinit var layoutTariffsList: LinearLayout
    private lateinit var layoutTariffEmpty: LinearLayout
    private lateinit var layoutTariffEditor: LinearLayout
    private lateinit var tvTariffEditorTitle: TextView
    private lateinit var tilTariffLabel: TextInputLayout
    private lateinit var etTariffLabel: TextInputEditText
    private lateinit var tilTariffAcRate: TextInputLayout
    private lateinit var etTariffAcRate: TextInputEditText
    private lateinit var tilTariffDcRate: TextInputLayout
    private lateinit var etTariffDcRate: TextInputEditText
    private lateinit var tilTariffRadius: TextInputLayout
    private lateinit var etTariffRadius: TextInputEditText
    private lateinit var tvTariffLocation: TextView
    private lateinit var tvTariffError: TextView
    private lateinit var btnTariffCancel: MaterialButton
    private lateinit var btnTariffSave: MaterialButton

    // Detail Panel Views
    private lateinit var btnBackFromDetail: MaterialButton
    private lateinit var tvDetailTitle: TextView
    private lateinit var btnDeleteCurrentSession: MaterialButton
    private lateinit var tvDetailDuration: TextView
    private lateinit var tvDetailSoc: TextView
    private lateinit var tvDetailLocation: TextView
    private lateinit var tvDetailEnergy: TextView
    private lateinit var tvDetailAvgPower: TextView
    private lateinit var tvDetailPeakPower: TextView
    private lateinit var tvDetailRangeGained: TextView
    private lateinit var tvDetailCost: TextView
    private lateinit var layoutEditCost: LinearLayout
    private lateinit var tvDetailTemp: TextView
    private lateinit var viewPowerCurve: ChargingCurveView
    private lateinit var viewTempCurve: TemperatureCurveView

    private val currencies = listOf("₺", "$", "€", "£", "¥", "CHF", "AUD", "CAD")
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    private var backCallback: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_charging_native, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupListeners()
        setupBackHandler()
        observeViewModel()
    }

    private fun initViews(v: View) {
        progressLoading = v.findViewById(R.id.progressLoading)
        scrollViewContent = v.findViewById(R.id.scrollViewContent)

        // Bottom tabs
        layoutBottomTabsBar = v.findViewById(R.id.layoutBottomTabsBar)
        tabBottomSessions = v.findViewById(R.id.tabBottomSessions)
        ivBottomTabSessions = v.findViewById(R.id.ivBottomTabSessions)
        tvBottomTabSessions = v.findViewById(R.id.tvBottomTabSessions)
        tabBottomStats = v.findViewById(R.id.tabBottomStats)
        ivBottomTabStats = v.findViewById(R.id.ivBottomTabStats)
        tvBottomTabStats = v.findViewById(R.id.tvBottomTabStats)
        tabBottomSettings = v.findViewById(R.id.tabBottomSettings)
        ivBottomTabSettings = v.findViewById(R.id.ivBottomTabSettings)
        tvBottomTabSettings = v.findViewById(R.id.tvBottomTabSettings)

        // Segmented filter buttons
        btnFilter7d = v.findViewById(R.id.btnFilter7d)
        btnFilter30d = v.findViewById(R.id.btnFilter30d)
        btnFilterAll = v.findViewById(R.id.btnFilterAll)
        btnFilterCustom = v.findViewById(R.id.btnFilterCustom)

        // Custom date range
        layoutCustomRangeRow = v.findViewById(R.id.layoutCustomRangeRow)
        btnChargeFrom = v.findViewById(R.id.btnChargeFrom)
        btnChargeTo = v.findViewById(R.id.btnChargeTo)
        btnApplyCustomRange = v.findViewById(R.id.btnApplyCustomRange)
        updateDateButtonsText()

        // Containers
        containerSessions = v.findViewById(R.id.containerSessions)
        containerStats = v.findViewById(R.id.containerStats)
        containerSettings = v.findViewById(R.id.containerSettings)
        containerDetail = v.findViewById(R.id.containerDetail)

        // Sessions Tab: Summary Cards
        tvSummarySessions = v.findViewById(R.id.tvSummarySessions)
        tvSummaryEnergy = v.findViewById(R.id.tvSummaryEnergy)
        tvSummaryCost = v.findViewById(R.id.tvSummaryCost)
        tvSummaryDcAc = v.findViewById(R.id.tvSummaryDcAc)
        tvSummaryRangeGained = v.findViewById(R.id.tvSummaryRangeGained)
        cardSummaryEstimateDisclosure = v.findViewById(R.id.cardSummaryEstimateDisclosure)
        layoutSummaryEstimateHeader = v.findViewById(R.id.layoutSummaryEstimateHeader)
        tvSummaryEstimateToggle = v.findViewById(R.id.tvSummaryEstimateToggle)
        tvSummaryEstimateDesc = v.findViewById(R.id.tvSummaryEstimateDesc)

        // Live Hero Card
        cardLiveHero = v.findViewById(R.id.cardLiveHero)
        tvLiveStatus = v.findViewById(R.id.tvLiveStatus)
        tvLiveSoC = v.findViewById(R.id.tvLiveSoC)
        tvLiveDetails = v.findViewById(R.id.tvLiveDetails)

        // Session list & sort
        spinnerSortSessions = v.findViewById(R.id.spinnerSortSessions)
        rvChargingSessions = v.findViewById(R.id.rvChargingSessions)
        cardEmptySessions = v.findViewById(R.id.cardEmptySessions)

        // Stats Tab Views
        btnStats7d = v.findViewById(R.id.btnStats7d)
        btnStats30d = v.findViewById(R.id.btnStats30d)
        btnStatsAll = v.findViewById(R.id.btnStatsAll)
        socGaugeView = v.findViewById(R.id.socGaugeView)
        tvStatsRange = v.findViewById(R.id.tvStatsRange)
        tvStatsSoh = v.findViewById(R.id.tvStatsSoh)
        tvStatsAvgPowerLabel = v.findViewById(R.id.tvStatsAvgPowerLabel)
        tvStatsAvgPower = v.findViewById(R.id.tvStatsAvgPower)
        tvStatsAvgPowerUnit = v.findViewById(R.id.tvStatsAvgPowerUnit)
        tvStatsAvgPowerSub = v.findViewById(R.id.tvStatsAvgPowerSub)
        tvStatsCostLabel = v.findViewById(R.id.tvStatsCostLabel)
        tvStatsCostPerKwh = v.findViewById(R.id.tvStatsCostPerKwh)
        tvStatsCostSub = v.findViewById(R.id.tvStatsCostSub)
        cardCompletionHero = v.findViewById(R.id.cardCompletionHero)
        tvCompletionValue = v.findViewById(R.id.tvCompletionValue)
        tvCompletionSub = v.findViewById(R.id.tvCompletionSub)
        cardStatsEstimateDisclosure = v.findViewById(R.id.cardStatsEstimateDisclosure)
        layoutStatsEstimateHeader = v.findViewById(R.id.layoutStatsEstimateHeader)
        tvStatsEstimateToggle = v.findViewById(R.id.tvStatsEstimateToggle)
        tvStatsEstimateDesc = v.findViewById(R.id.tvStatsEstimateDesc)
        btnSoc24h = v.findViewById(R.id.btnSoc24h)
        btnSoc7d = v.findViewById(R.id.btnSoc7d)
        btnSoc30d = v.findViewById(R.id.btnSoc30d)
        viewStatsSocCurve = v.findViewById(R.id.viewStatsSocCurve)
        tvLifetimeEnergy = v.findViewById(R.id.tvLifetimeEnergy)
        tvLifetimeSessions = v.findViewById(R.id.tvLifetimeSessions)
        tvLifetimeCost = v.findViewById(R.id.tvLifetimeCost)
        statsEmptyState = v.findViewById(R.id.statsEmptyState)

        // Settings Tab Views
        switchAutoRecord = v.findViewById(R.id.switchAutoRecord)
        spinnerCurrency = v.findViewById(R.id.spinnerCurrency)
        etElectricityRate = v.findViewById(R.id.etElectricityRate)
        etDcRate = v.findViewById(R.id.etDcRate)
        btnApplySettings = v.findViewById(R.id.btnApplySettings)
        btnTariffAdd = v.findViewById(R.id.btnTariffAdd)
        btnClearHistory = v.findViewById(R.id.btnClearHistory)

        // Tariffs Section Views
        tvTariffFallbackNote = v.findViewById(R.id.tvTariffFallbackNote)
        layoutTariffsList = v.findViewById(R.id.layoutTariffsList)
        layoutTariffEmpty = v.findViewById(R.id.layoutTariffEmpty)
        layoutTariffEditor = v.findViewById(R.id.layoutTariffEditor)
        tvTariffEditorTitle = v.findViewById(R.id.tvTariffEditorTitle)
        tilTariffLabel = v.findViewById(R.id.tilTariffLabel)
        etTariffLabel = v.findViewById(R.id.etTariffLabel)
        tilTariffAcRate = v.findViewById(R.id.tilTariffAcRate)
        etTariffAcRate = v.findViewById(R.id.etTariffAcRate)
        tilTariffDcRate = v.findViewById(R.id.tilTariffDcRate)
        etTariffDcRate = v.findViewById(R.id.etTariffDcRate)
        tilTariffRadius = v.findViewById(R.id.tilTariffRadius)
        etTariffRadius = v.findViewById(R.id.etTariffRadius)
        tvTariffLocation = v.findViewById(R.id.tvTariffLocation)
        tvTariffError = v.findViewById(R.id.tvTariffError)
        btnTariffCancel = v.findViewById(R.id.btnTariffCancel)
        btnTariffSave = v.findViewById(R.id.btnTariffSave)

        // Detail Panel Views
        btnBackFromDetail = v.findViewById(R.id.btnBackFromDetail)
        tvDetailTitle = v.findViewById(R.id.tvDetailTitle)
        btnDeleteCurrentSession = v.findViewById(R.id.btnDeleteCurrentSession)
        tvDetailDuration = v.findViewById(R.id.tvDetailDuration)
        tvDetailSoc = v.findViewById(R.id.tvDetailSoc)
        tvDetailLocation = v.findViewById(R.id.tvDetailLocation)
        tvDetailEnergy = v.findViewById(R.id.tvDetailEnergy)
        tvDetailAvgPower = v.findViewById(R.id.tvDetailAvgPower)
        tvDetailPeakPower = v.findViewById(R.id.tvDetailPeakPower)
        tvDetailRangeGained = v.findViewById(R.id.tvDetailRangeGained)
        tvDetailCost = v.findViewById(R.id.tvDetailCost)
        layoutEditCost = v.findViewById(R.id.layoutEditCost)
        tvDetailTemp = v.findViewById(R.id.tvDetailTemp)
        viewPowerCurve = v.findViewById(R.id.viewPowerCurve)
        viewTempCurve = v.findViewById(R.id.viewTempCurve)

        // Populate currency spinner
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = spinnerAdapter

        // Populate sort spinner
        val sortOptions = listOf(
            getString(R.string.charge_sort_recent),
            getString(R.string.charge_sort_oldest)
        )
        val sortAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSortSessions.adapter = sortAdapter
    }

    private fun setupRecyclerView() {
        sessionAdapter = ChargingSessionAdapter(
            onSessionClick = { session ->
                viewModel.openSessionDetail(session)
                scrollViewContent.scrollTo(0, 0)
            },
            onDeleteClick = { session ->
                showDeleteConfirmDialog(session.id)
            }
        )
        rvChargingSessions.layoutManager = LinearLayoutManager(requireContext())
        rvChargingSessions.adapter = sessionAdapter
    }

    private fun setupListeners() {
        // Sticky bottom navigation tabs
        tabBottomSessions.setOnClickListener {
            viewModel.selectTab(ChargingTab.SESSIONS)
        }
        tabBottomStats.setOnClickListener {
            viewModel.selectTab(ChargingTab.STATS)
        }
        tabBottomSettings.setOnClickListener {
            viewModel.selectTab(ChargingTab.SETTINGS)
        }

        // Sessions Tab: Top segmented period filters
        btnFilter7d.setOnClickListener {
            layoutCustomRangeRow.visibility = View.GONE
            viewModel.setPeriodFilter(PeriodFilter.DAYS_7)
        }
        btnFilter30d.setOnClickListener {
            layoutCustomRangeRow.visibility = View.GONE
            viewModel.setPeriodFilter(PeriodFilter.DAYS_30)
        }
        btnFilterAll.setOnClickListener {
            layoutCustomRangeRow.visibility = View.GONE
            viewModel.setPeriodFilter(PeriodFilter.ALL_TIME)
        }
        btnFilterCustom.setOnClickListener {
            val isCurrentlyVisible = layoutCustomRangeRow.visibility == View.VISIBLE
            layoutCustomRangeRow.visibility = if (isCurrentlyVisible) View.GONE else View.VISIBLE
            if (!isCurrentlyVisible) {
                setSegmentedButtonStyle(btnFilter7d, false)
                setSegmentedButtonStyle(btnFilter30d, false)
                setSegmentedButtonStyle(btnFilterAll, false)
                setSegmentedButtonStyle(btnFilterCustom, true)
            }
        }

        // Custom date range pickers
        btnChargeFrom.setOnClickListener {
            showDatePicker(fromCalendar) {
                updateDateButtonsText()
            }
        }
        btnChargeTo.setOnClickListener {
            showDatePicker(toCalendar) {
                updateDateButtonsText()
            }
        }
        btnApplyCustomRange.setOnClickListener {
            applyCustomDateFilter()
        }

        // Estimate disclosures expand/collapse
        layoutSummaryEstimateHeader.setOnClickListener {
            val isExpanded = tvSummaryEstimateDesc.visibility == View.VISIBLE
            tvSummaryEstimateDesc.visibility = if (isExpanded) View.GONE else View.VISIBLE
            tvSummaryEstimateToggle.text = if (isExpanded) "+" else "−"
        }
        layoutStatsEstimateHeader.setOnClickListener {
            val isExpanded = tvStatsEstimateDesc.visibility == View.VISIBLE
            tvStatsEstimateDesc.visibility = if (isExpanded) View.GONE else View.VISIBLE
            tvStatsEstimateToggle.text = if (isExpanded) "+" else "−"
        }

        // Sort spinner
        spinnerSortSessions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val shouldBeOldest = (position == 1)
                if (isSortOldestFirst != shouldBeOldest) {
                    isSortOldestFirst = shouldBeOldest
                    applySortToSessions(viewModel.uiState.value.sessions)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Stats Tab: Period Pills
        btnStats7d.setOnClickListener { viewModel.setPeriodFilter(PeriodFilter.DAYS_7) }
        btnStats30d.setOnClickListener { viewModel.setPeriodFilter(PeriodFilter.DAYS_30) }
        btnStatsAll.setOnClickListener { viewModel.setPeriodFilter(PeriodFilter.ALL_TIME) }

        // Stats Tab: SoC history range
        btnSoc24h.setOnClickListener {
            setSocPeriodButtons(24)
            viewModel.loadSocHistory(24)
        }
        btnSoc7d.setOnClickListener {
            setSocPeriodButtons(168)
            viewModel.loadSocHistory(168)
        }
        btnSoc30d.setOnClickListener {
            setSocPeriodButtons(720)
            viewModel.loadSocHistory(720)
        }

        // Settings actions
        btnApplySettings.setOnClickListener { applySettings() }
        btnClearHistory.setOnClickListener { showClearHistoryDialog() }
        btnTariffAdd.setOnClickListener { openTariffEditor(null) }
        btnTariffCancel.setOnClickListener { viewModel.closeTariffEditor() }
        btnTariffSave.setOnClickListener { saveTariff() }

        // Detail panel actions
        btnBackFromDetail.setOnClickListener { viewModel.closeSessionDetail() }
        btnDeleteCurrentSession.setOnClickListener {
            viewModel.uiState.value.selectedSession?.let { session ->
                showDeleteConfirmDialog(session.id)
            }
        }
        layoutEditCost.setOnClickListener {
            viewModel.uiState.value.selectedSession?.let { session ->
                showEditCostDialog(session)
            }
        }
    }

    private fun setupBackHandler() {
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (viewModel.uiState.value.isDetailOpen) {
                    viewModel.closeSessionDetail()
                } else if (viewModel.uiState.value.isTariffEditorOpen) {
                    viewModel.closeTariffEditor()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback!!)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUi(state)
                }
            }
        }
    }

    private fun renderUi(state: ChargingUiState) {
        progressLoading.visibility = if (state.isLoading || state.isDetailLoading) View.VISIBLE else View.GONE
        backCallback?.isEnabled = state.isDetailOpen || state.isTariffEditorOpen

        if (state.isDetailOpen) {
            layoutBottomTabsBar.visibility = View.GONE
            containerSessions.visibility = View.GONE
            containerStats.visibility = View.GONE
            containerSettings.visibility = View.GONE
            containerDetail.visibility = View.VISIBLE
            renderDetail(state)
        } else {
            layoutBottomTabsBar.visibility = View.VISIBLE
            containerDetail.visibility = View.GONE

            updateBottomTabsBar(state.currentTab)
            updatePeriodButtons(state.periodFilter)

            containerSessions.visibility = if (state.currentTab == ChargingTab.SESSIONS) View.VISIBLE else View.GONE
            containerStats.visibility = if (state.currentTab == ChargingTab.STATS) View.VISIBLE else View.GONE
            containerSettings.visibility = if (state.currentTab == ChargingTab.SETTINGS) View.VISIBLE else View.GONE

            when (state.currentTab) {
                ChargingTab.SESSIONS -> renderSessions(state)
                ChargingTab.STATS -> renderStats(state)
                ChargingTab.SETTINGS -> renderSettings(state)
            }
        }
    }

    private fun isNightMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateBottomTabsBar(currentTab: ChargingTab) {
        val isNight = isNightMode()
        val activeBg = R.drawable.bg_bottom_tab_active
        val activeColor = if (isNight) Color.parseColor("#00D4AA") else Color.parseColor("#004D40")
        val inactiveColor = if (isNight) Color.parseColor("#8AFFFFFF") else Color.parseColor("#757575")

        // Sessions Tab
        val isSessions = (currentTab == ChargingTab.SESSIONS)
        tabBottomSessions.setBackgroundResource(if (isSessions) activeBg else android.R.color.transparent)
        ivBottomTabSessions.imageTintList = ColorStateList.valueOf(if (isSessions) activeColor else inactiveColor)
        tvBottomTabSessions.setTextColor(if (isSessions) activeColor else inactiveColor)
        tvBottomTabSessions.paint.isFakeBoldText = isSessions

        // Stats Tab
        val isStats = (currentTab == ChargingTab.STATS)
        tabBottomStats.setBackgroundResource(if (isStats) activeBg else android.R.color.transparent)
        ivBottomTabStats.imageTintList = ColorStateList.valueOf(if (isStats) activeColor else inactiveColor)
        tvBottomTabStats.setTextColor(if (isStats) activeColor else inactiveColor)
        tvBottomTabStats.paint.isFakeBoldText = isStats

        // Settings Tab
        val isSettings = (currentTab == ChargingTab.SETTINGS)
        tabBottomSettings.setBackgroundResource(if (isSettings) activeBg else android.R.color.transparent)
        ivBottomTabSettings.imageTintList = ColorStateList.valueOf(if (isSettings) activeColor else inactiveColor)
        tvBottomTabSettings.setTextColor(if (isSettings) activeColor else inactiveColor)
        tvBottomTabSettings.paint.isFakeBoldText = isSettings
    }

    private fun updatePeriodButtons(currentFilter: PeriodFilter) {
        val isCustomOpen = layoutCustomRangeRow.visibility == View.VISIBLE
        setSegmentedButtonStyle(btnFilter7d, currentFilter == PeriodFilter.DAYS_7 && !isCustomOpen)
        setSegmentedButtonStyle(btnFilter30d, currentFilter == PeriodFilter.DAYS_30 && !isCustomOpen)
        setSegmentedButtonStyle(btnFilterAll, currentFilter == PeriodFilter.ALL_TIME && !isCustomOpen)
        setSegmentedButtonStyle(btnFilterCustom, isCustomOpen)

        // Also update stats period pills
        setSegmentedButtonStyle(btnStats7d, currentFilter == PeriodFilter.DAYS_7)
        setSegmentedButtonStyle(btnStats30d, currentFilter == PeriodFilter.DAYS_30)
        setSegmentedButtonStyle(btnStatsAll, currentFilter == PeriodFilter.ALL_TIME)
    }

    private fun setSegmentedButtonStyle(button: MaterialButton, active: Boolean) {
        val isNight = isNightMode()
        if (active) {
            val bgTint = if (isNight) Color.parseColor("#2600D4AA") else Color.parseColor("#1F007A62")
            val primaryColor = if (isNight) Color.parseColor("#00D4AA") else Color.parseColor("#007A62")
            button.backgroundTintList = ColorStateList.valueOf(bgTint)
            button.strokeColor = ColorStateList.valueOf(primaryColor)
            button.strokeWidth = 2
            button.setTextColor(primaryColor)
            button.iconTint = ColorStateList.valueOf(primaryColor)
            button.paint.isFakeBoldText = true
        } else {
            val textCol = if (isNight) Color.parseColor("#8AFFFFFF") else Color.parseColor("#616161")
            button.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            button.strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
            button.strokeWidth = 0
            button.setTextColor(textCol)
            button.iconTint = ColorStateList.valueOf(textCol)
            button.paint.isFakeBoldText = false
        }
    }

    private fun setSocPeriodButtons(hours: Int) {
        setSegmentedButtonStyle(btnSoc24h, hours == 24)
        setSegmentedButtonStyle(btnSoc7d, hours == 168)
        setSegmentedButtonStyle(btnSoc30d, hours == 720)
    }

    private fun updateDateButtonsText() {
        btnChargeFrom.text = "${getString(R.string.charge_range_from)}: ${shortDateFormat.format(fromCalendar.time)}"
        btnChargeTo.text = "${getString(R.string.charge_range_to)}: ${shortDateFormat.format(toCalendar.time)}"
    }

    private fun showDatePicker(calendar: Calendar, onDateSet: () -> Unit) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDateSet()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun applyCustomDateFilter() {
        val diffMs = toCalendar.timeInMillis - fromCalendar.timeInMillis
        val diffDays = (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(1).toInt()
        val filter = when {
            diffDays <= 7 -> PeriodFilter.DAYS_7
            diffDays <= 30 -> PeriodFilter.DAYS_30
            else -> PeriodFilter.ALL_TIME
        }
        viewModel.setPeriodFilter(filter)
        Toast.makeText(
            requireContext(),
            "${shortDateFormat.format(fromCalendar.time)} → ${shortDateFormat.format(toCalendar.time)} ($diffDays d)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderSessions(state: ChargingUiState) {
        val summary = state.summary
        val live = summary.live

        // Live Hero Card
        if (live.charging || live.plugged) {
            cardLiveHero.visibility = View.VISIBLE
            val statusText = if (live.charging) {
                getString(R.string.charge_state_charging)
            } else {
                getString(R.string.charge_state_plugged)
            }
            tvLiveStatus.text = statusText
            tvLiveSoC.text = if (live.socPercent > 0) "${Math.round(live.socPercent)}%" else "--"

            val pwr = if (live.powerKw > 0) "${String.format(Locale.US, "%.1f", live.powerKw)} kW" else "--"
            val ttf = if (live.timeToFullMin > 0) "${live.timeToFullMin} min" else "--"
            val kwh = if (live.sessionKwh > 0) "${String.format(Locale.US, "%.1f", live.sessionKwh)} kWh" else "--"
            tvLiveDetails.text = "Power: $pwr • Time to full: $ttf • Added: $kwh"
        } else {
            cardLiveHero.visibility = View.GONE
        }

        // Summary Cards - display '--' if 0 or empty, matching charging.js
        tvSummarySessions.text = if (summary.periodSessions > 0) summary.periodSessions.toString() else "--"

        val isCharging = live.charging
        val liveKwh = if (live.sessionKwh > 0) live.sessionKwh else 0.0
        val livePowerEstimated = isCharging && live.isEstimated && live.powerKw > 0
        val periodEnergyApproximate = summary.periodEstimatedSessions > 0 || (isCharging && liveKwh > 0 && live.isEstimated)
        val prefix = if (periodEnergyApproximate) "~" else ""

        tvSummaryEnergy.text = if (summary.periodEnergyKwh > 0) {
            "$prefix${String.format(Locale.US, "%.1f", summary.periodEnergyKwh)} kWh"
        } else "--"

        val curr = state.config.currency.ifEmpty { "₺" }
        tvSummaryCost.text = if (summary.periodCost > 0) {
            "$prefix${String.format(Locale.US, "%.2f", summary.periodCost)} $curr".trim()
        } else "--"

        tvSummaryDcAc.text = if (summary.periodSessions > 0) {
            "${summary.periodDcCount} / ${summary.periodAcCount}"
        } else "--"

        tvSummaryRangeGained.text = if (summary.periodRangeGained > 0) {
            "$prefix+${summary.periodRangeGained} km"
        } else "--"

        // Estimated Values Disclosure (Sessions Tab)
        val liveRate = if (state.config.dcRate > 0) state.config.dcRate else state.config.electricityRate
        val liveCost = if (isCharging && liveKwh > 0 && liveRate > 0) liveKwh * liveRate else 0.0
        val showSummaryEstimate = periodEnergyApproximate || livePowerEstimated || liveCost > 0
        cardSummaryEstimateDisclosure.visibility = if (showSummaryEstimate) View.VISIBLE else View.GONE

        // Sessions List with sorting
        applySortToSessions(state.sessions)
    }

    private fun applySortToSessions(sessions: List<ChargingSession>) {
        val sortedList = if (isSortOldestFirst) {
            sessions.sortedBy { it.startTime }
        } else {
            sessions.sortedByDescending { it.startTime }
        }
        sessionAdapter.submitList(sortedList)
        cardEmptySessions.visibility = if (sortedList.isEmpty()) View.VISIBLE else View.GONE
        rvChargingSessions.visibility = if (sortedList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun renderStats(state: ChargingUiState) {
        val summary = state.summary
        val live = summary.live

        // Sync SoC period pills
        setSocPeriodButtons(state.socHours)

        // Gauge & SoC info (fallback to latest snapshot if vehicle is sleeping/unplugged)
        val (snapSoc, snapRange, snapSoh) = viewModel.getLatestBatterySnapshot()
        val currentSoc = if (live.socPercent > 0) live.socPercent else snapSoc
        socGaugeView.setSoc(currentSoc)

        val effectiveRange = if (live.rangeKm > 0) live.rangeKm else snapRange
        val rangeText = if (effectiveRange != null && effectiveRange > 0) "${Math.round(effectiveRange)} km" else "--"
        tvStatsRange.text = rangeText

        val effectiveSoh = if (live.sohPercent > 0) live.sohPercent else snapSoh
        val sohText = if (effectiveSoh != null && effectiveSoh > 0) "SOH ${Math.round(effectiveSoh)}%" else "SOH --"
        tvStatsSoh.text = sohText

        // Average Power Hero Card
        val isCharging = live.charging
        val (pwrVal, pwrLive) = if (isCharging && !live.isEstimated && live.powerKw > 0.15) {
            Pair(live.powerKw, true)
        } else {
            var totalEnergy = 0.0
            var totalHours = 0.0
            var fallbackTotal = 0.0
            var fallbackCount = 0
            for (s in state.sessions) {
                if (s.isEstimated) continue
                val dur = s.durationMinutes ?: 0L
                val nrg = s.energyAdded ?: 0.0
                val avg = s.avgPower ?: 0.0
                if (nrg > 0 && dur > 0) {
                    totalEnergy += nrg
                    totalHours += dur / 60.0
                } else if (avg > 0) {
                    fallbackTotal += avg
                    fallbackCount++
                }
            }
            if (totalEnergy > 0 && totalHours > 0) {
                Pair(totalEnergy / totalHours, false)
            } else if (fallbackCount > 0) {
                Pair(fallbackTotal / fallbackCount, false)
            } else if (summary.periodSessions > 0 && summary.periodEnergyKwh > 0) {
                Pair(summary.periodEnergyKwh / summary.periodSessions, false)
            } else {
                Pair(0.0, false)
            }
        }

        tvStatsAvgPowerLabel.text = getString(R.string.charge_hero_avg_power)
        if (pwrVal > 0) {
            tvStatsAvgPower.text = String.format(Locale.US, "%.1f", pwrVal)
        } else {
            tvStatsAvgPower.text = "--"
        }
        tvStatsAvgPowerSub.text = when {
            pwrLive -> getString(R.string.charge_power_live)
            pwrVal > 0 -> getString(R.string.charge_power_period)
            else -> getString(R.string.charge_power_waiting)
        }

        // Cost Hero Card
        val curr = state.config.currency.ifEmpty { "₺" }
        val liveRate = if (state.config.dcRate > 0) state.config.dcRate else state.config.electricityRate
        val liveKwh = if (live.sessionKwh > 0) live.sessionKwh else 0.0
        val measured = summary.avgCostPerKwh ?: 0.0
        val periodEnergyApproximate = summary.periodEstimatedSessions > 0 || (isCharging && liveKwh > 0 && live.isEstimated)
        val prefix = if (periodEnergyApproximate) "~" else ""

        if (isCharging && liveKwh > 0 && liveRate > 0) {
            tvStatsCostLabel.text = getString(R.string.charge_hero_cost_session)
            tvStatsCostPerKwh.text = "${String.format(Locale.US, "%.2f", liveKwh * liveRate)} $curr".trim()
            tvStatsCostSub.text = getString(R.string.charge_cost_estimated)
        } else if (summary.periodCost > 0) {
            tvStatsCostLabel.text = getString(R.string.charge_hero_cost_period)
            tvStatsCostPerKwh.text = "$prefix${String.format(Locale.US, "%.2f", summary.periodCost)} $curr".trim()
            tvStatsCostSub.text = if (measured > 0) {
                "$prefix${String.format(Locale.US, "%.2f", measured)} $curr / kWh".trim()
            } else ""
        } else if (state.config.electricityRate > 0) {
            tvStatsCostLabel.text = getString(R.string.charge_hero_cost)
            tvStatsCostPerKwh.text = "${String.format(Locale.US, "%.2f", state.config.electricityRate)} $curr".trim()
            tvStatsCostSub.text = getString(R.string.charge_cost_configured)
        } else {
            tvStatsCostLabel.text = getString(R.string.charge_hero_cost)
            tvStatsCostPerKwh.text = "--"
            tvStatsCostSub.text = ""
        }

        // Completion Hero Card (Time to Full)
        val completion = when {
            live.fault -> null
            live.full -> Pair(getString(R.string.charge_completion_complete), "")
            live.plugged && !live.charging -> Pair(getString(R.string.charge_completion_waiting), "")
            live.charging && live.timeToFullMin > 0 -> {
                val mins = live.timeToFullMin.coerceAtLeast(1)
                val primary = if (mins >= 60) {
                    val hrs = mins / 60
                    val remMins = mins % 60
                    if (remMins > 0) "${hrs} hr ${remMins} min" else "${hrs} hr"
                } else {
                    "${mins} min"
                }
                val completedAt = System.currentTimeMillis() + mins * 60_000L
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                val clockStr = timeFmt.format(Date(completedAt))
                val secondary = getString(R.string.charge_completion_full_at, clockStr)
                Pair(primary, secondary)
            }
            else -> null
        }

        if (completion != null) {
            cardCompletionHero.visibility = View.VISIBLE
            tvCompletionValue.text = completion.first
            tvCompletionSub.text = completion.second
        } else {
            cardCompletionHero.visibility = View.GONE
        }

        // Stats Estimated Values Disclosure
        val lifetimeEnergyApproximate = summary.lifetimeEstimatedSessions > 0
        val livePowerEstimated = isCharging && live.isEstimated && live.powerKw > 0
        val liveCost = if (isCharging && liveKwh > 0 && liveRate > 0) liveKwh * liveRate else 0.0
        val showStatsEstimate = periodEnergyApproximate || lifetimeEnergyApproximate || livePowerEstimated || liveCost > 0
        cardStatsEstimateDisclosure.visibility = if (showStatsEstimate) View.VISIBLE else View.GONE

        // Lifetime stats
        tvLifetimeEnergy.text = if (summary.lifetimeEnergyKwh > 0) {
            "${String.format(Locale.US, "%.1f", summary.lifetimeEnergyKwh)} kWh"
        } else "--"
        tvLifetimeSessions.text = if (summary.lifetimeSessions > 0) summary.lifetimeSessions.toString() else "--"
        tvLifetimeCost.text = if (summary.lifetimeCost > 0) {
            "${String.format(Locale.US, "%.2f", summary.lifetimeCost)} $curr".trim()
        } else "--"

        // Empty state vs chart
        statsEmptyState.visibility = if (summary.lifetimeSessions == 0 && state.sessions.isEmpty()) View.VISIBLE else View.GONE

        // SoC History curve
        viewStatsSocCurve.setPoints(state.socHistory, state.socHours)
    }

    private fun renderSettings(state: ChargingUiState) {
        val cfg = state.config
        switchAutoRecord.isChecked = cfg.enabled

        if (!etElectricityRate.hasFocus()) {
            etElectricityRate.setText(if (cfg.electricityRate > 0) cfg.electricityRate.toString() else "")
        }
        if (!etDcRate.hasFocus()) {
            etDcRate.setText(if (cfg.dcRate > 0) cfg.dcRate.toString() else "")
        }

        val currencyIdx = currencies.indexOf(cfg.currency)
        if (currencyIdx >= 0) {
            spinnerCurrency.setSelection(currencyIdx)
        }

        renderTariffs(state)
    }

    private fun renderTariffs(state: ChargingUiState) {
        // Fallback note
        val defaultTariff = state.tariffs.find { it.id == state.defaultTariffId }
        if (defaultTariff != null) {
            val label = defaultTariff.label.ifEmpty { getString(R.string.charge_tariff_unnamed) }
            tvTariffFallbackNote.text = getString(R.string.charge_tariff_fallback_default, label)
            tvTariffFallbackNote.visibility = View.VISIBLE
        } else if (state.config.electricityRate > 0) {
            val rateStr = "${String.format(Locale.US, "%.2f", state.config.electricityRate)} ${state.config.currency}".trim()
            tvTariffFallbackNote.text = getString(R.string.charge_tariff_fallback_global, rateStr)
            tvTariffFallbackNote.visibility = View.VISIBLE
        } else {
            tvTariffFallbackNote.visibility = View.GONE
        }

        // List vs Empty
        if (state.tariffs.isEmpty()) {
            layoutTariffEmpty.visibility = View.VISIBLE
            layoutTariffsList.visibility = View.GONE
            layoutTariffsList.removeAllViews()
        } else {
            layoutTariffEmpty.visibility = View.GONE
            layoutTariffsList.visibility = View.VISIBLE
            layoutTariffsList.removeAllViews()

            val inflater = LayoutInflater.from(requireContext())
            for (tariff in state.tariffs) {
                val itemView = inflater.inflate(R.layout.item_charging_tariff, layoutTariffsList, false)
                val tvTariffLabel = itemView.findViewById<TextView>(R.id.tvTariffLabel)
                val tvBadgeHere = itemView.findViewById<TextView>(R.id.tvBadgeHere)
                val tvBadgeDefault = itemView.findViewById<TextView>(R.id.tvBadgeDefault)
                val btnTariffDefault = itemView.findViewById<ImageButton>(R.id.btnTariffDefault)
                val btnTariffEdit = itemView.findViewById<ImageButton>(R.id.btnTariffEdit)
                val btnTariffDelete = itemView.findViewById<ImageButton>(R.id.btnTariffDelete)
                val tvTariffRates = itemView.findViewById<TextView>(R.id.tvTariffRates)
                val tvTariffSub = itemView.findViewById<TextView>(R.id.tvTariffSub)

                tvTariffLabel.text = tariff.label.ifEmpty { getString(R.string.charge_tariff_unnamed) }
                tvBadgeHere.visibility = if (tariff.id == state.matchedTariffId) View.VISIBLE else View.GONE
                tvBadgeDefault.visibility = if (tariff.id == state.defaultTariffId) View.VISIBLE else View.GONE

                val isDefault = (tariff.id == state.defaultTariffId)
                if (isDefault) {
                    val primaryColor = if (isNightMode()) Color.parseColor("#00D4AA") else Color.parseColor("#007A62")
                    btnTariffDefault.imageTintList = ColorStateList.valueOf(primaryColor)
                } else {
                    val defaultTint = if (isNightMode()) Color.parseColor("#8AFFFFFF") else Color.parseColor("#757575")
                    btnTariffDefault.imageTintList = ColorStateList.valueOf(defaultTint)
                }

                btnTariffDefault.setOnClickListener {
                    viewModel.setDefaultTariff(tariff) { success ->
                        if (!success) {
                            Toast.makeText(requireContext(), R.string.charge_tariff_err_save, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                btnTariffEdit.setOnClickListener {
                    openTariffEditor(tariff)
                }

                btnTariffDelete.setOnClickListener {
                    showDeleteTariffDialog(tariff)
                }

                // Rates
                val cur = tariff.currency.ifEmpty { state.config.currency.ifEmpty { "$" } }
                val rateParts = mutableListOf<String>()
                if (tariff.acRate > 0) {
                    rateParts.add("${cur}${String.format(Locale.US, "%.2f", tariff.acRate)} ${getString(R.string.charge_tariff_ac_short)}")
                }
                if (tariff.dcRate > 0) {
                    rateParts.add("${cur}${String.format(Locale.US, "%.2f", tariff.dcRate)} ${getString(R.string.charge_tariff_dc_short)}")
                }
                if (rateParts.isEmpty()) {
                    rateParts.add(getString(R.string.charge_tariff_no_rate))
                }
                tvTariffRates.text = rateParts.joinToString(" · ")

                // Sub
                val subParts = mutableListOf<String>()
                subParts.add("${tariff.radiusM} m")
                if (tariff.useCount > 0) {
                    subParts.add(getString(R.string.charge_tariff_used_count, tariff.useCount))
                }
                if (tariff.lat != 0.0 || tariff.lng != 0.0) {
                    subParts.add(String.format(Locale.US, "%.3f, %.3f", tariff.lat, tariff.lng))
                }
                tvTariffSub.text = subParts.joinToString(" · ")

                layoutTariffsList.addView(itemView)
            }
        }

        // Editor
        if (state.isTariffEditorOpen) {
            layoutTariffEditor.visibility = View.VISIBLE
            btnTariffAdd.visibility = View.GONE

            val t = state.editingTariff
            if (t != null && (t.lat != 0.0 || t.lng != 0.0)) {
                tvTariffLocation.text = String.format(Locale.US, "%.5f, %.5f", t.lat, t.lng)
            } else if (state.currentGpsLat != null && state.currentGpsLng != null &&
                (state.currentGpsLat != 0.0 || state.currentGpsLng != 0.0)) {
                tvTariffLocation.text = String.format(Locale.US, "%.5f, %.5f", state.currentGpsLat, state.currentGpsLng)
            } else {
                tvTariffLocation.setText(R.string.charge_tariff_no_gps)
            }

            if (!state.tariffError.isNullOrEmpty()) {
                tvTariffError.text = state.tariffError
                tvTariffError.visibility = View.VISIBLE
            } else {
                tvTariffError.visibility = View.GONE
            }

            btnTariffSave.isEnabled = !state.isTariffSaving
            btnTariffCancel.isEnabled = !state.isTariffSaving
        } else {
            layoutTariffEditor.visibility = View.GONE
            btnTariffAdd.visibility = View.VISIBLE
        }
    }

    private fun openTariffEditor(tariff: LocationTariff?) {
        viewModel.openTariffEditor(tariff)
        tvTariffEditorTitle.setText(if (tariff == null) R.string.charge_tariff_new_title else R.string.charge_tariff_edit_title)
        etTariffLabel.setText(tariff?.label ?: "")

        val defaultAc = if (tariff != null) {
            if (tariff.acRate > 0) tariff.acRate.toString() else ""
        } else {
            val global = viewModel.uiState.value.config.electricityRate
            if (global > 0) global.toString() else ""
        }
        etTariffAcRate.setText(defaultAc)

        val defaultDc = if (tariff != null) {
            if (tariff.dcRate > 0) tariff.dcRate.toString() else ""
        } else {
            val globalDc = viewModel.uiState.value.config.dcRate
            if (globalDc > 0) globalDc.toString() else ""
        }
        etTariffDcRate.setText(defaultDc)

        etTariffRadius.setText((tariff?.radiusM ?: 50).toString())
        tvTariffError.visibility = View.GONE
        tvTariffError.text = ""
        etTariffLabel.requestFocus()
    }

    private fun saveTariff() {
        val label = etTariffLabel.text?.toString()?.trim() ?: ""
        val acRateStr = etTariffAcRate.text?.toString()?.trim() ?: ""
        val dcRateStr = etTariffDcRate.text?.toString()?.trim() ?: ""
        val radiusStr = etTariffRadius.text?.toString()?.trim() ?: ""

        val acRate = acRateStr.toDoubleOrNull() ?: 0.0
        val dcRate = dcRateStr.toDoubleOrNull() ?: 0.0
        val radius = radiusStr.toIntOrNull() ?: 50

        if (acRate <= 0 && dcRate <= 0) {
            showTariffInlineError(getString(R.string.charge_tariff_err_no_rate))
            return
        }
        if (acRate < 0 || dcRate < 0 || acRate >= 100000 || dcRate >= 100000) {
            showTariffInlineError(getString(R.string.charge_tariff_err_rate_range))
            return
        }
        if (radius < 25 || radius > 2000) {
            showTariffInlineError(getString(R.string.charge_tariff_err_radius))
            return
        }
        if (label.length > 48) {
            showTariffInlineError(getString(R.string.charge_tariff_err_label))
            return
        }
        val editing = viewModel.uiState.value.editingTariff
        val dupe = viewModel.uiState.value.tariffs.any {
            it.label.isNotBlank() && label.isNotBlank() &&
            it.label.equals(label, ignoreCase = true) &&
            (editing == null || it.id != editing.id)
        }
        if (dupe) {
            showTariffInlineError(getString(R.string.charge_tariff_err_dupe_label))
            return
        }

        tvTariffError.visibility = View.GONE
        val isNew = (editing == null)
        viewModel.saveTariff(label, acRate, dcRate, radius) { success, error ->
            if (success) {
                val toastMsg = if (isNew) {
                    getString(R.string.charge_tariff_auto_hint, radius)
                } else {
                    getString(R.string.charge_tariff_saved)
                }
                Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_LONG).show()
            } else {
                showTariffInlineError(error ?: getString(R.string.charge_tariff_err_save))
            }
        }
    }

    private fun showTariffInlineError(error: String) {
        tvTariffError.text = error
        tvTariffError.visibility = View.VISIBLE
    }

    private fun showDeleteTariffDialog(tariff: LocationTariff) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.common_delete)
            .setMessage(R.string.charge_tariff_delete_confirm)
            .setPositiveButton(R.string.common_delete) { _, _ ->
                viewModel.deleteTariff(tariff) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), R.string.charge_tariff_deleted, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), R.string.charge_tariff_err_delete, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun renderDetail(state: ChargingUiState) {
        val session = state.selectedSession ?: return

        tvDetailTitle.text = if (session.startTime > 0) dateFormat.format(Date(session.startTime)) else "--"
        btnDeleteCurrentSession.visibility = if (session.inProgress) View.GONE else View.VISIBLE

        tvDetailDuration.text = if (session.durationMinutes != null && session.durationMinutes > 0) {
            "${session.durationMinutes} min"
        } else if (session.inProgress) {
            getString(R.string.charge_in_progress)
        } else "--"

        val startSocText = session.startSoc?.let { "${Math.round(it)}%" } ?: "--"
        val endSocText = session.endSoc?.let { "${Math.round(it)}%" } ?: "--"
        tvDetailSoc.text = "$startSocText → $endSocText"

        val place = session.placeLabel
        tvDetailLocation.text = if (!place.isNullOrEmpty()) place else "--"

        val pfx = if (session.isEstimated) "~" else ""
        tvDetailEnergy.text = session.energyAdded?.let { "$pfx${String.format(Locale.US, "%.1f", it)} kWh" } ?: "--"
        tvDetailAvgPower.text = session.avgPower?.let { "${String.format(Locale.US, "%.1f", it)} kW" } ?: "--"
        tvDetailPeakPower.text = session.peakPower?.let { "${String.format(Locale.US, "%.1f", it)} kW" } ?: "--"
        tvDetailRangeGained.text = session.rangeGained?.let { "+$it km" } ?: "--"

        val curr = session.currency.ifEmpty { "₺" }
        tvDetailCost.text = session.cost?.let { "${String.format(Locale.US, "%.2f", it)} $curr".trim() } ?: "--"
        tvDetailTemp.text = session.tempAvg?.let { "${Math.round(it)}°C" } ?: "--"

        // Set curves
        viewPowerCurve.setSamples(state.selectedSessionSamples)
        viewTempCurve.setSamples(state.selectedSessionSamples)
    }

    private fun showDeleteConfirmDialog(sessionId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.charge_delete_session_title)
            .setMessage(R.string.charge_delete_confirm_msg)
            .setPositiveButton(R.string.common_delete) { _, _ ->
                viewModel.deleteSession(sessionId) { success ->
                    val msg = if (success) R.string.charge_delete_success else R.string.charge_delete_failed
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun showClearHistoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_clear_charging_history, null)
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.charge_settings_clear) { _, _ ->
                viewModel.clearHistory { success ->
                    val msg = if (success) R.string.charge_clear_history_success else R.string.charge_clear_history_failed
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun showEditCostDialog(session: ChargingSession) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_cost, null)
        val etCost = dialogView.findViewById<TextInputEditText>(R.id.etSessionCost)
        session.cost?.let {
            if (it >= 0) etCost.setText(it.toString())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.common_save) { _, _ ->
                val costStr = etCost.text?.toString()?.trim()
                val newCost = if (costStr.isNullOrEmpty()) -1.0 else costStr.toDoubleOrNull() ?: -1.0
                viewModel.updateCost(session.id, newCost) { success ->
                    val msg = if (success) R.string.charge_cost_saved else R.string.charge_cost_save_failed
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }

    private fun applySettings() {
        val enabled = switchAutoRecord.isChecked
        val rate = etElectricityRate.text?.toString()?.toDoubleOrNull() ?: 0.0
        val dcRate = etDcRate.text?.toString()?.toDoubleOrNull() ?: 0.0
        val selectedCurrency = spinnerCurrency.selectedItem?.toString() ?: "₺"

        viewModel.saveConfig(enabled, rate, selectedCurrency, dcRate) { success ->
            val msg = if (success) R.string.charge_settings_saved else R.string.charge_settings_save_failed
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }
}
