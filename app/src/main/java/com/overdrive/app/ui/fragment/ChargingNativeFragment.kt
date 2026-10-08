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
import com.overdrive.app.R
import com.overdrive.app.ui.charging.ChargingCurveView
import com.overdrive.app.ui.charging.ChargingSample
import com.overdrive.app.ui.charging.ChargingSession
import com.overdrive.app.ui.charging.ChargingSessionAdapter
import com.overdrive.app.ui.charging.ChargingTab
import com.overdrive.app.ui.charging.ChargingUiState
import com.overdrive.app.ui.charging.ChargingViewModel
import com.overdrive.app.ui.charging.PeriodFilter
import com.overdrive.app.ui.charging.SocGaugeView
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
    private lateinit var tvStatsAvgPower: TextView
    private lateinit var tvStatsCostPerKwh: TextView
    private lateinit var btnSoc24h: MaterialButton
    private lateinit var btnSoc7d: MaterialButton
    private lateinit var btnSoc30d: MaterialButton
    private lateinit var viewStatsSocCurve: ChargingCurveView
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
    private val shortDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

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
        tvStatsAvgPower = v.findViewById(R.id.tvStatsAvgPower)
        tvStatsCostPerKwh = v.findViewById(R.id.tvStatsCostPerKwh)
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
        btnSoc24h.setOnClickListener { setSocPeriodButtons(24) }
        btnSoc7d.setOnClickListener { setSocPeriodButtons(168) }
        btnSoc30d.setOnClickListener { setSocPeriodButtons(720) }

        // Settings actions
        btnApplySettings.setOnClickListener { applySettings() }
        btnClearHistory.setOnClickListener { showClearHistoryDialog() }
        btnTariffAdd.setOnClickListener {
            Toast.makeText(requireContext(), R.string.charge_tariff_add_here, Toast.LENGTH_SHORT).show()
        }

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
        backCallback?.isEnabled = state.isDetailOpen

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

        // Summary Cards
        tvSummarySessions.text = summary.periodSessions.toString()
        val prefix = if (summary.periodEstimatedSessions > 0) "~" else ""
        tvSummaryEnergy.text = "$prefix${String.format(Locale.US, "%.1f", summary.periodEnergyKwh)} kWh"

        val curr = state.config.currency.ifEmpty { "₺" }
        tvSummaryCost.text = "${String.format(Locale.US, "%.2f", summary.periodCost)} $curr".trim()
        tvSummaryDcAc.text = "${summary.periodDcCount} / ${summary.periodAcCount}"
        tvSummaryRangeGained.text = "+${summary.periodRangeGained} km"

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

        // Gauge & SoC info
        val currentSoc = if (live.socPercent > 0) live.socPercent else 0.0
        socGaugeView.setSoc(currentSoc)

        val rangeText = if (live.rangeKm > 0) "${Math.round(live.rangeKm)} km" else "--"
        tvStatsRange.text = rangeText

        val sohText = if (live.sohPercent > 0) "SOH ${Math.round(live.sohPercent)}%" else "SOH --"
        tvStatsSoh.text = sohText

        // Average power & cost per kWh
        val avgPower = if (summary.periodSessions > 0 && summary.periodEnergyKwh > 0) {
            String.format(Locale.US, "%.1f kW", summary.periodEnergyKwh / summary.periodSessions)
        } else "-- kW"
        tvStatsAvgPower.text = avgPower

        val curr = state.config.currency.ifEmpty { "₺" }
        val costPerKwh = summary.avgCostPerKwh
        tvStatsCostPerKwh.text = if (costPerKwh != null && costPerKwh > 0) {
            "${String.format(Locale.US, "%.2f", costPerKwh)} $curr / kWh".trim()
        } else "--"

        // Lifetime stats
        tvLifetimeEnergy.text = "${String.format(Locale.US, "%.1f", summary.lifetimeEnergyKwh)} kWh"
        tvLifetimeSessions.text = summary.lifetimeSessions.toString()
        tvLifetimeCost.text = "${String.format(Locale.US, "%.2f", summary.lifetimeCost)} $curr".trim()

        // Empty state vs chart
        statsEmptyState.visibility = if (summary.lifetimeSessions == 0 && state.sessions.isEmpty()) View.VISIBLE else View.GONE

        // SoC History curve
        val samples = state.socHistory.map {
            ChargingSample(t = it.timestamp, powerKw = null, soc = it.soc, temp = null, tempHigh = null, tempLow = null)
        }
        viewStatsSocCurve.setSamples(samples)
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
