package com.overdrive.app.ui.fragment

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.overdrive.app.R
import com.overdrive.app.ui.trips.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TripsNativeFragment : Fragment() {

    private val viewModel: TripsViewModel by viewModels()

    // Segmented period filter buttons
    private lateinit var layoutSegmentedFilterContainer: LinearLayout
    private lateinit var btnFilter7d: MaterialButton
    private lateinit var btnFilter14d: MaterialButton
    private lateinit var btnFilter30d: MaterialButton
    private lateinit var btnFilterCustom: MaterialButton

    // Custom date range
    private lateinit var layoutCustomRangeRow: LinearLayout
    private lateinit var btnTripFrom: MaterialButton
    private lateinit var btnTripTo: MaterialButton
    private lateinit var btnApplyCustomRange: MaterialButton
    private val fromCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    private val toCalendar = Calendar.getInstance()
    private val shortDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Bottom Tabs
    private lateinit var layoutBottomTabsBar: LinearLayout
    private lateinit var tabBottomTrips: LinearLayout
    private lateinit var ivBottomTabTrips: ImageView
    private lateinit var tvBottomTabTrips: TextView
    private lateinit var tabBottomStats: LinearLayout
    private lateinit var ivBottomTabStats: ImageView
    private lateinit var tvBottomTabStats: TextView
    private lateinit var tabBottomStorage: LinearLayout
    private lateinit var ivBottomTabStorage: ImageView
    private lateinit var tvBottomTabStorage: TextView

    private lateinit var progressLoading: ProgressBar
    private lateinit var btnEnableTripsFromEmpty: MaterialButton

    // Containers
    private lateinit var containerTrips: LinearLayout
    private lateinit var containerStats: NestedScrollView
    private lateinit var containerStorage: NestedScrollView
    private lateinit var containerDetail: NestedScrollView

    // Trips Tab Views
    private lateinit var tvSummaryTrips: TextView
    private lateinit var tvSummaryTripsLabel: TextView
    private lateinit var tvSummaryDistance: TextView
    private lateinit var tvSummaryDistanceLabel: TextView
    private lateinit var tvSummaryDuration: TextView
    private lateinit var tvSummaryDurationLabel: TextView
    private lateinit var tvSummaryAvgScore: TextView
    private lateinit var tvSummaryAvgScoreLabel: TextView
    private lateinit var tvSummaryEnergy: TextView
    private lateinit var tvSummaryEnergyLabel: TextView
    private lateinit var tvSummaryConsumption: TextView
    private lateinit var tvSummaryConsumptionLabel: TextView
    private lateinit var tvSummaryEfficiency: TextView
    private lateinit var tvSummaryEfficiencyLabel: TextView
    private lateinit var tvSummaryCost: TextView
    private lateinit var tvSummaryCostLabel: TextView
    private lateinit var recyclerTrips: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var tripsAdapter: TripsAdapter

    // Stats Tab Views
    private lateinit var gaugeDriverScore: DriverScoreGaugeView
    private lateinit var tvDriverScoreRating: TextView
    private lateinit var tvPersonalizedRangeKm: TextView
    private lateinit var tvRangeConfidenceNote: TextView
    private lateinit var tvCostPerKm: TextView
    private lateinit var tvTotalTripCostStats: TextView
    private lateinit var radarDnaChart: DnaRadarView
    private lateinit var progressAnticipation: ProgressBar
    private lateinit var tvScoreAnticipation: TextView
    private lateinit var progressSmoothness: ProgressBar
    private lateinit var tvScoreSmoothness: TextView
    private lateinit var progressSpeedDisc: ProgressBar
    private lateinit var tvScoreSpeedDisc: TextView
    private lateinit var progressEfficiency: ProgressBar
    private lateinit var tvScoreEfficiency: TextView
    private lateinit var progressConsistency: ProgressBar
    private lateinit var tvScoreConsistency: TextView

    // Storage Tab Views
    private lateinit var switchTripAnalytics: MaterialSwitch
    private lateinit var spinnerTripCurrency: Spinner
    private lateinit var etTripElectricityRate: TextInputEditText
    private lateinit var toggleDistanceUnit: MaterialButtonToggleGroup
    private lateinit var btnUnitKm: MaterialButton
    private lateinit var btnUnitMiles: MaterialButton
    private lateinit var toggleStorageLocation: MaterialButtonToggleGroup
    private lateinit var btnStorageInternal: MaterialButton
    private lateinit var btnStorageSd: MaterialButton
    private lateinit var btnStorageUsb: MaterialButton
    private lateinit var tvStorageUsageVal: TextView
    private lateinit var progressStorageUsage: ProgressBar
    private lateinit var btnApplyTripSettings: MaterialButton
    private lateinit var btnRecoverTrips: MaterialButton
    private lateinit var tvRecoveryStatus: TextView

    // Detail Drill-in Views
    private lateinit var btnBackToTrips: MaterialButton
    private lateinit var btnRescoreTrip: MaterialButton
    private lateinit var btnDeleteDetailTrip: MaterialButton
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailSubtitle: TextView
    private lateinit var tvDetailDistance: TextView
    private lateinit var tvDetailDuration: TextView
    private lateinit var tvDetailEnergy: TextView
    private lateinit var tvDetailConsumption: TextView
    private lateinit var tvDetailAvgSpeed: TextView
    private lateinit var tvDetailMaxSpeed: TextView
    private lateinit var tvDetailSocUsed: TextView
    private lateinit var tvDetailCost: TextView
    private lateinit var tvDetailElevation: TextView
    private lateinit var tvDetailTemp: TextView
    private lateinit var tvDetailOverallScore: TextView
    private lateinit var tvDetailProfile: TextView
    private lateinit var chartTimeline: TripTimelineChartView
    private lateinit var histogramSpeed: SpeedHistogramView

    private val supportedCurrencies = listOf("₺", "$", "€", "£", "₹", "¥")
    private var isProgrammaticChange = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_trips_native, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupListeners()
        setupAdapter()
        setupBackPressHandling()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    private fun initViews(v: View) {
        layoutSegmentedFilterContainer = v.findViewById(R.id.layoutSegmentedFilterContainer)
        btnFilter7d = v.findViewById(R.id.btnFilter7d)
        btnFilter14d = v.findViewById(R.id.btnFilter14d)
        btnFilter30d = v.findViewById(R.id.btnFilter30d)
        btnFilterCustom = v.findViewById(R.id.btnFilterCustom)

        layoutCustomRangeRow = v.findViewById(R.id.layoutCustomRangeRow)
        btnTripFrom = v.findViewById(R.id.btnTripFrom)
        btnTripTo = v.findViewById(R.id.btnTripTo)
        btnApplyCustomRange = v.findViewById(R.id.btnApplyCustomRange)
        updateDateButtonsText()

        layoutBottomTabsBar = v.findViewById(R.id.layoutBottomTabsBar)
        tabBottomTrips = v.findViewById(R.id.tabBottomTrips)
        ivBottomTabTrips = v.findViewById(R.id.ivBottomTabTrips)
        tvBottomTabTrips = v.findViewById(R.id.tvBottomTabTrips)
        tabBottomStats = v.findViewById(R.id.tabBottomStats)
        ivBottomTabStats = v.findViewById(R.id.ivBottomTabStats)
        tvBottomTabStats = v.findViewById(R.id.tvBottomTabStats)
        tabBottomStorage = v.findViewById(R.id.tabBottomStorage)
        ivBottomTabStorage = v.findViewById(R.id.ivBottomTabStorage)
        tvBottomTabStorage = v.findViewById(R.id.tvBottomTabStorage)

        progressLoading = v.findViewById(R.id.progressLoading)
        btnEnableTripsFromEmpty = v.findViewById(R.id.btnEnableTripsFromEmpty)

        containerTrips = v.findViewById(R.id.containerTrips)
        containerStats = v.findViewById(R.id.containerStats)
        containerStorage = v.findViewById(R.id.containerStorage)
        containerDetail = v.findViewById(R.id.containerDetail)

        tvSummaryTrips = v.findViewById(R.id.tvSummaryTrips)
        tvSummaryTripsLabel = v.findViewById(R.id.tvSummaryTripsLabel)
        tvSummaryDistance = v.findViewById(R.id.tvSummaryDistance)
        tvSummaryDistanceLabel = v.findViewById(R.id.tvSummaryDistanceLabel)
        tvSummaryDuration = v.findViewById(R.id.tvSummaryDuration)
        tvSummaryDurationLabel = v.findViewById(R.id.tvSummaryDurationLabel)
        tvSummaryAvgScore = v.findViewById(R.id.tvSummaryAvgScore)
        tvSummaryAvgScoreLabel = v.findViewById(R.id.tvSummaryAvgScoreLabel)
        tvSummaryEnergy = v.findViewById(R.id.tvSummaryEnergy)
        tvSummaryEnergyLabel = v.findViewById(R.id.tvSummaryEnergyLabel)
        tvSummaryConsumption = v.findViewById(R.id.tvSummaryConsumption)
        tvSummaryConsumptionLabel = v.findViewById(R.id.tvSummaryConsumptionLabel)
        tvSummaryEfficiency = v.findViewById(R.id.tvSummaryEfficiency)
        tvSummaryEfficiencyLabel = v.findViewById(R.id.tvSummaryEfficiencyLabel)
        tvSummaryCost = v.findViewById(R.id.tvSummaryCost)
        tvSummaryCostLabel = v.findViewById(R.id.tvSummaryCostLabel)
        recyclerTrips = v.findViewById(R.id.recyclerTrips)
        layoutEmptyState = v.findViewById(R.id.layoutEmptyState)

        gaugeDriverScore = v.findViewById(R.id.gaugeDriverScore)
        tvDriverScoreRating = v.findViewById(R.id.tvDriverScoreRating)
        tvPersonalizedRangeKm = v.findViewById(R.id.tvPersonalizedRangeKm)
        tvRangeConfidenceNote = v.findViewById(R.id.tvRangeConfidenceNote)
        tvCostPerKm = v.findViewById(R.id.tvCostPerKm)
        tvTotalTripCostStats = v.findViewById(R.id.tvTotalTripCostStats)
        radarDnaChart = v.findViewById(R.id.radarDnaChart)
        progressAnticipation = v.findViewById(R.id.progressAnticipation)
        tvScoreAnticipation = v.findViewById(R.id.tvScoreAnticipation)
        progressSmoothness = v.findViewById(R.id.progressSmoothness)
        tvScoreSmoothness = v.findViewById(R.id.tvScoreSmoothness)
        progressSpeedDisc = v.findViewById(R.id.progressSpeedDisc)
        tvScoreSpeedDisc = v.findViewById(R.id.tvScoreSpeedDisc)
        progressEfficiency = v.findViewById(R.id.progressEfficiency)
        tvScoreEfficiency = v.findViewById(R.id.tvScoreEfficiency)
        progressConsistency = v.findViewById(R.id.progressConsistency)
        tvScoreConsistency = v.findViewById(R.id.tvScoreConsistency)

        switchTripAnalytics = v.findViewById(R.id.switchTripAnalytics)
        spinnerTripCurrency = v.findViewById(R.id.spinnerTripCurrency)
        etTripElectricityRate = v.findViewById(R.id.etTripElectricityRate)
        toggleDistanceUnit = v.findViewById(R.id.toggleDistanceUnit)
        btnUnitKm = v.findViewById(R.id.btnUnitKm)
        btnUnitMiles = v.findViewById(R.id.btnUnitMiles)
        toggleStorageLocation = v.findViewById(R.id.toggleStorageLocation)
        btnStorageInternal = v.findViewById(R.id.btnStorageInternal)
        btnStorageSd = v.findViewById(R.id.btnStorageSd)
        btnStorageUsb = v.findViewById(R.id.btnStorageUsb)
        tvStorageUsageVal = v.findViewById(R.id.tvStorageUsageVal)
        progressStorageUsage = v.findViewById(R.id.progressStorageUsage)
        btnApplyTripSettings = v.findViewById(R.id.btnApplyTripSettings)
        btnRecoverTrips = v.findViewById(R.id.btnRecoverTrips)
        tvRecoveryStatus = v.findViewById(R.id.tvRecoveryStatus)

        btnBackToTrips = v.findViewById(R.id.btnBackToTrips)
        btnRescoreTrip = v.findViewById(R.id.btnRescoreTrip)
        btnDeleteDetailTrip = v.findViewById(R.id.btnDeleteDetailTrip)
        tvDetailTitle = v.findViewById(R.id.tvDetailTitle)
        tvDetailSubtitle = v.findViewById(R.id.tvDetailSubtitle)
        tvDetailDistance = v.findViewById(R.id.tvDetailDistance)
        tvDetailDuration = v.findViewById(R.id.tvDetailDuration)
        tvDetailEnergy = v.findViewById(R.id.tvDetailEnergy)
        tvDetailConsumption = v.findViewById(R.id.tvDetailConsumption)
        tvDetailAvgSpeed = v.findViewById(R.id.tvDetailAvgSpeed)
        tvDetailMaxSpeed = v.findViewById(R.id.tvDetailMaxSpeed)
        tvDetailSocUsed = v.findViewById(R.id.tvDetailSocUsed)
        tvDetailCost = v.findViewById(R.id.tvDetailCost)
        tvDetailElevation = v.findViewById(R.id.tvDetailElevation)
        tvDetailTemp = v.findViewById(R.id.tvDetailTemp)
        tvDetailOverallScore = v.findViewById(R.id.tvDetailOverallScore)
        tvDetailProfile = v.findViewById(R.id.tvDetailProfile)
        chartTimeline = v.findViewById(R.id.chartTimeline)
        histogramSpeed = v.findViewById(R.id.histogramSpeed)

        // Setup currency spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, supportedCurrencies)
        spinnerTripCurrency.adapter = adapter
    }

    private fun setupListeners() {
        tabBottomTrips.setOnClickListener { viewModel.selectTab(TripsTab.TRIPS) }
        tabBottomStats.setOnClickListener { viewModel.selectTab(TripsTab.STATS) }
        tabBottomStorage.setOnClickListener { viewModel.selectTab(TripsTab.STORAGE) }

        btnFilter7d.setOnClickListener {
            layoutCustomRangeRow.visibility = View.GONE
            viewModel.setPeriodFilter(PeriodFilter.DAYS_7)
        }
        btnFilter14d.setOnClickListener {
            layoutCustomRangeRow.visibility = View.GONE
            viewModel.setPeriodFilter(PeriodFilter.DAYS_14)
        }
        btnFilter30d.setOnClickListener {
            layoutCustomRangeRow.visibility = View.GONE
            viewModel.setPeriodFilter(PeriodFilter.DAYS_30)
        }
        btnFilterCustom.setOnClickListener {
            val isCurrentlyVisible = layoutCustomRangeRow.visibility == View.VISIBLE
            layoutCustomRangeRow.visibility = if (isCurrentlyVisible) View.GONE else View.VISIBLE
            if (!isCurrentlyVisible) {
                setSegmentedButtonStyle(btnFilter7d, false)
                setSegmentedButtonStyle(btnFilter14d, false)
                setSegmentedButtonStyle(btnFilter30d, false)
                setSegmentedButtonStyle(btnFilterCustom, true)
            }
        }

        btnTripFrom.setOnClickListener {
            showDatePicker(fromCalendar) { updateDateButtonsText() }
        }
        btnTripTo.setOnClickListener {
            showDatePicker(toCalendar) { updateDateButtonsText() }
        }
        btnApplyCustomRange.setOnClickListener {
            applyCustomDateFilter()
        }

        btnEnableTripsFromEmpty.setOnClickListener {
            viewModel.updateAnalyticsEnabled(true)
        }

        switchTripAnalytics.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) {
                viewModel.updateAnalyticsEnabled(isChecked)
            }
        }

        btnApplyTripSettings.setOnClickListener {
            val rate = etTripElectricityRate.text.toString().toDoubleOrNull() ?: 0.0
            val currency = spinnerTripCurrency.selectedItem?.toString() ?: "₺"
            val distanceUnit = if (toggleDistanceUnit.checkedButtonId == R.id.btnUnitMiles) "mi" else "km"
            viewModel.saveConfigSettings(rate, currency, distanceUnit)

            val storageType = when (toggleStorageLocation.checkedButtonId) {
                R.id.btnStorageSd -> "SD_CARD"
                R.id.btnStorageUsb -> "USB"
                else -> "INTERNAL"
            }
            viewModel.saveStorageSettings(storageType, viewModel.uiState.value.storage?.limitMb ?: 1000L)
        }

        btnRecoverTrips.setOnClickListener {
            viewModel.startTripRecovery()
        }

        btnBackToTrips.setOnClickListener {
            viewModel.closeTripDetail()
        }

        btnRescoreTrip.setOnClickListener {
            viewModel.uiState.value.activeTripDetail?.let {
                viewModel.rescoreTrip(it.id)
            }
        }

        btnDeleteDetailTrip.setOnClickListener {
            viewModel.uiState.value.activeTripDetail?.let {
                showDeleteConfirmDialog(it)
            }
        }
    }

    private fun setupAdapter() {
        tripsAdapter = TripsAdapter(
            onItemClick = { trip -> viewModel.openTripDetail(trip) },
            onDeleteClick = { trip -> showDeleteConfirmDialog(trip) }
        )
        recyclerTrips.layoutManager = LinearLayoutManager(requireContext())
        recyclerTrips.adapter = tripsAdapter
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (containerDetail.visibility == View.VISIBLE) {
                    viewModel.closeTripDetail()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUi(state)
                }
            }
        }
    }

    private fun renderUi(state: TripsUiState) {
        progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Detail Drill-in View vs Tab Content
        if (state.activeTripDetail != null) {
            containerDetail.visibility = View.VISIBLE
            containerTrips.visibility = View.GONE
            containerStats.visibility = View.GONE
            containerStorage.visibility = View.GONE
            layoutSegmentedFilterContainer.visibility = View.GONE
            layoutCustomRangeRow.visibility = View.GONE
            layoutBottomTabsBar.visibility = View.GONE

            renderDetail(state.activeTripDetail, state.telemetrySamples)
            return
        }

        containerDetail.visibility = View.GONE
        layoutBottomTabsBar.visibility = View.VISIBLE

        // Render Tabs and Period Filters
        updateBottomTabsBar(state.activeTab)
        updatePeriodButtons(state.periodFilter)

        when (state.activeTab) {
            TripsTab.TRIPS -> {
                layoutSegmentedFilterContainer.visibility = View.VISIBLE
                containerTrips.visibility = View.VISIBLE
                containerStats.visibility = View.GONE
                containerStorage.visibility = View.GONE
                renderTripsTab(state)
            }
            TripsTab.STATS -> {
                layoutSegmentedFilterContainer.visibility = View.GONE
                layoutCustomRangeRow.visibility = View.GONE
                containerTrips.visibility = View.GONE
                containerStats.visibility = View.VISIBLE
                containerStorage.visibility = View.GONE
                renderStatsTab(state)
            }
            TripsTab.STORAGE -> {
                layoutSegmentedFilterContainer.visibility = View.GONE
                layoutCustomRangeRow.visibility = View.GONE
                containerTrips.visibility = View.GONE
                containerStats.visibility = View.GONE
                containerStorage.visibility = View.VISIBLE
                renderStorageTab(state)
            }
        }

        state.infoMessage?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
        state.error?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    private fun isNightMode(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateBottomTabsBar(currentTab: TripsTab) {
        val isNight = isNightMode()
        val activeBg = R.drawable.bg_bottom_tab_active
        val activeColor = if (isNight) Color.parseColor("#00D4AA") else Color.parseColor("#004D40")
        val inactiveColor = if (isNight) Color.parseColor("#8AFFFFFF") else Color.parseColor("#757575")

        // Trips Tab
        val isTrips = (currentTab == TripsTab.TRIPS)
        tabBottomTrips.setBackgroundResource(if (isTrips) activeBg else android.R.color.transparent)
        ivBottomTabTrips.imageTintList = ColorStateList.valueOf(if (isTrips) activeColor else inactiveColor)
        tvBottomTabTrips.setTextColor(if (isTrips) activeColor else inactiveColor)
        tvBottomTabTrips.paint.isFakeBoldText = isTrips

        // Stats Tab
        val isStats = (currentTab == TripsTab.STATS)
        tabBottomStats.setBackgroundResource(if (isStats) activeBg else android.R.color.transparent)
        ivBottomTabStats.imageTintList = ColorStateList.valueOf(if (isStats) activeColor else inactiveColor)
        tvBottomTabStats.setTextColor(if (isStats) activeColor else inactiveColor)
        tvBottomTabStats.paint.isFakeBoldText = isStats

        // Storage Tab
        val isStorage = (currentTab == TripsTab.STORAGE)
        tabBottomStorage.setBackgroundResource(if (isStorage) activeBg else android.R.color.transparent)
        ivBottomTabStorage.imageTintList = ColorStateList.valueOf(if (isStorage) activeColor else inactiveColor)
        tvBottomTabStorage.setTextColor(if (isStorage) activeColor else inactiveColor)
        tvBottomTabStorage.paint.isFakeBoldText = isStorage
    }

    private fun updatePeriodButtons(currentFilter: PeriodFilter) {
        val isCustomOpen = layoutCustomRangeRow.visibility == View.VISIBLE
        setSegmentedButtonStyle(btnFilter7d, currentFilter == PeriodFilter.DAYS_7 && !isCustomOpen)
        setSegmentedButtonStyle(btnFilter14d, currentFilter == PeriodFilter.DAYS_14 && !isCustomOpen)
        setSegmentedButtonStyle(btnFilter30d, currentFilter == PeriodFilter.DAYS_30 && !isCustomOpen)
        setSegmentedButtonStyle(btnFilterCustom, isCustomOpen)
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

    private fun updateDateButtonsText() {
        btnTripFrom.text = "${getString(R.string.trip_range_from)}: ${shortDateFormat.format(fromCalendar.time)}"
        btnTripTo.text = "${getString(R.string.trip_range_to)}: ${shortDateFormat.format(toCalendar.time)}"
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
            diffDays <= 14 -> PeriodFilter.DAYS_14
            diffDays <= 30 -> PeriodFilter.DAYS_30
            else -> PeriodFilter.ALL
        }
        viewModel.setPeriodFilter(filter)
        Toast.makeText(
            requireContext(),
            "${shortDateFormat.format(fromCalendar.time)} → ${shortDateFormat.format(toCalendar.time)} ($diffDays d)",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderTripsTab(state: TripsUiState) {
        val isMiles = (state.config?.distanceUnit == "mi")
        val isNight = isNightMode()
        val brandPrimaryColor = if (isNight) Color.parseColor("#00D4AA") else Color.parseColor("#007A62")

        // 1. Trips
        tvSummaryTrips.text = state.summary.tripCount.toString()

        // 2. Distance
        val distVal = if (isMiles) state.summary.totalDistanceKm * 0.621371 else state.summary.totalDistanceKm
        tvSummaryDistance.text = String.format(Locale.US, "%.1f", distVal)
        tvSummaryDistanceLabel.text = if (isMiles) "mi" else "km"

        // 3. Duration (Hours)
        val totalHours = state.summary.totalDurationSeconds / 3600.0
        tvSummaryDuration.text = String.format(Locale.US, "%.1f", totalHours)

        // 4. Avg Score
        tvSummaryAvgScore.text = state.summary.avgScore?.toString() ?: "--"

        // 5. Energy (kWh)
        tvSummaryEnergy.text = if (state.summary.totalEnergyKwh > 0.0) {
            String.format(Locale.US, "%.1f", state.summary.totalEnergyKwh)
        } else {
            "--"
        }

        // 6. Consumption (kWh/100km or kWh/100mi)
        tvSummaryConsumptionLabel.text = if (isMiles) "kWh/100mi" else "kWh/100km"
        val cons = state.summary.avgConsumptionKwhPer100Km
        if (cons != null && cons > 0.0) {
            val displayCons = if (isMiles) cons / 0.621371 else cons
            tvSummaryConsumption.text = String.format(Locale.US, "%.1f", displayCons)
        } else {
            tvSummaryConsumption.text = "--"
        }
        tvSummaryConsumption.setTextColor(brandPrimaryColor)

        // 7. Efficiency (km/kWh or mi/kWh)
        tvSummaryEfficiencyLabel.text = if (isMiles) "mi/kWh" else "km/kWh"
        val eff = state.summary.avgEfficiencyKmPerKwh
        if (eff != null && eff > 0.0) {
            val displayEff = if (isMiles) eff * 0.621371 else eff
            tvSummaryEfficiency.text = String.format(Locale.US, "%.1f", displayEff)
        } else {
            tvSummaryEfficiency.text = "--"
        }
        tvSummaryEfficiency.setTextColor(brandPrimaryColor)

        // 8. Cost
        val currency = state.config?.currency ?: "₺"
        val electricityRate = state.config?.electricityRate ?: 0.0
        val cost = state.summary.totalCost
        if (cost > 0.0) {
            tvSummaryCost.text = String.format(Locale.US, "%s%.1f", currency, cost)
        } else if (state.summary.totalEnergyKwh > 0.0 && electricityRate > 0.0) {
            val computedCost = state.summary.totalEnergyKwh * electricityRate
            tvSummaryCost.text = String.format(Locale.US, "%s%.1f", currency, computedCost)
        } else {
            tvSummaryCost.text = "--"
        }

        val isRecordingEnabled = state.config?.enabled == true
        btnEnableTripsFromEmpty.visibility = if (isRecordingEnabled) View.GONE else View.VISIBLE

        if (state.trips.isEmpty()) {
            recyclerTrips.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            recyclerTrips.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
            tripsAdapter.submitList(state.trips)
        }
    }

    private fun renderStatsTab(state: TripsUiState) {
        val dna = state.dnaScores
        val overallScore = dna?.overall ?: -1
        gaugeDriverScore.setScore(overallScore)
        tvDriverScoreRating.text = when {
            overallScore >= 80 -> "Smooth & Optimal"
            overallScore >= 60 -> "Moderate & Balanced"
            overallScore > 0 -> "Aggressive / Needs Coaching"
            else -> "Drive more to unlock score"
        }

        // Personalized range
        val range = state.rangeEstimate
        if (range != null && range.predictedRangeKm > 0) {
            tvPersonalizedRangeKm.text = range.predictedRangeKm.toInt().toString()
            tvRangeConfidenceNote.text = "Estimated range based on recent driving efficiency"
        } else {
            tvPersonalizedRangeKm.text = "--"
            tvRangeConfidenceNote.text = getString(R.string.trip_stats_range_note)
        }

        // Cost per km
        if (state.summary.totalDistanceKm > 0.5) {
            val costPerKm = state.summary.totalCost / state.summary.totalDistanceKm
            tvCostPerKm.text = String.format(Locale.US, "%.2f", costPerKm)
        } else {
            tvCostPerKm.text = "--"
        }
        val currency = state.config?.currency ?: "₺"
        tvTotalTripCostStats.text = "Total cost: " + String.format(Locale.US, "%.2f %s", state.summary.totalCost, currency)

        // DNA radar & progress
        radarDnaChart.setScores(dna)
        if (dna != null) {
            progressAnticipation.progress = dna.anticipation
            tvScoreAnticipation.text = dna.anticipation.toString()
            progressSmoothness.progress = dna.smoothness
            tvScoreSmoothness.text = dna.smoothness.toString()
            progressSpeedDisc.progress = dna.speedDiscipline
            tvScoreSpeedDisc.text = dna.speedDiscipline.toString()
            progressEfficiency.progress = dna.efficiency
            tvScoreEfficiency.text = dna.efficiency.toString()
            progressConsistency.progress = dna.consistency
            tvScoreConsistency.text = dna.consistency.toString()
        } else {
            progressAnticipation.progress = 0
            tvScoreAnticipation.text = "--"
            progressSmoothness.progress = 0
            tvScoreSmoothness.text = "--"
            progressSpeedDisc.progress = 0
            tvScoreSpeedDisc.text = "--"
            progressEfficiency.progress = 0
            tvScoreEfficiency.text = "--"
            progressConsistency.progress = 0
            tvScoreConsistency.text = "--"
        }
    }

    private fun renderStorageTab(state: TripsUiState) {
        val cfg = state.config
        val st = state.storage

        isProgrammaticChange = true
        if (cfg != null) {
            switchTripAnalytics.isChecked = cfg.enabled
            etTripElectricityRate.setText(String.format(Locale.US, "%.2f", cfg.electricityRate))
            val currIndex = supportedCurrencies.indexOf(cfg.currency)
            if (currIndex >= 0) spinnerTripCurrency.setSelection(currIndex)

            if (cfg.distanceUnit == "mi") {
                toggleDistanceUnit.check(R.id.btnUnitMiles)
            } else {
                toggleDistanceUnit.check(R.id.btnUnitKm)
            }
        }

        if (st != null) {
            when (st.storageType) {
                "SD_CARD" -> toggleStorageLocation.check(R.id.btnStorageSd)
                "USB" -> toggleStorageLocation.check(R.id.btnStorageUsb)
                else -> toggleStorageLocation.check(R.id.btnStorageInternal)
            }
            btnStorageSd.isEnabled = st.sdCardAvailable
            btnStorageUsb.isEnabled = st.usbAvailable

            tvStorageUsageVal.text = String.format(Locale.US, "%.1f %s / %d MB", st.usedMb, st.usedUnit, st.limitMb)
            val usagePct = if (st.limitMb > 0) ((st.usedMb / st.limitMb) * 100).toInt().coerceIn(0, 100) else 0
            progressStorageUsage.progress = usagePct
        }

        if (state.isRecovering) {
            btnRecoverTrips.isEnabled = false
            tvRecoveryStatus.text = state.recoveryMessage ?: "Scanning..."
        } else {
            btnRecoverTrips.isEnabled = true
            if (state.recoveryMessage != null) {
                tvRecoveryStatus.text = state.recoveryMessage
            }
        }
        isProgrammaticChange = false
    }

    private fun renderDetail(trip: TripRecordItem, samples: List<TelemetrySampleItem>) {
        val dateFormat = SimpleDateFormat("EEEE, dd MMM • HH:mm", Locale.getDefault())
        val dateStr = if (trip.startTime > 0) dateFormat.format(Date(trip.startTime)) else "--"
        tvDetailTitle.text = String.format(Locale.US, "Trip Details • %.1f km", trip.distanceKm)
        tvDetailSubtitle.text = dateStr

        tvDetailDistance.text = String.format(Locale.US, "%.1f km", trip.distanceKm)

        val durationMinutes = trip.durationSeconds / 60
        tvDetailDuration.text = if (durationMinutes >= 60) {
            val hours = durationMinutes / 60
            val mins = durationMinutes % 60
            "${hours}h ${mins}m"
        } else {
            "$durationMinutes min"
        }

        tvDetailEnergy.text = String.format(Locale.US, "%.1f kWh", trip.energyUsedKwh)
        tvDetailConsumption.text = String.format(Locale.US, "%.1f kWh/100km", trip.consumptionKwhPer100Km)
        tvDetailAvgSpeed.text = String.format(Locale.US, "%.0f km/h", trip.avgSpeedKmh)
        tvDetailMaxSpeed.text = "${trip.maxSpeedKmh} km/h"

        val socDeltaStr = if (trip.socStart > 0 && trip.socEnd > 0) {
            String.format(Locale.US, "%.0f%% → %.0f%% (%.0f%%)", trip.socStart, trip.socEnd, trip.socDelta)
        } else "--"
        tvDetailSocUsed.text = socDeltaStr

        val currency = trip.currency.ifEmpty { "₺" }
        tvDetailCost.text = String.format(Locale.US, "%.2f %s", trip.tripCost, currency)

        tvDetailElevation.text = String.format(Locale.US, "↑ %.0fm  ↓ %.0fm", trip.elevationGainM, trip.elevationLossM)
        tvDetailTemp.text = if (trip.extTempC != 0) "${trip.extTempC} °C" else "--"
        tvDetailOverallScore.text = if (trip.overallScore > 0) "★ ${trip.overallScore}" else "--"

        val profileParts = mutableListOf<String>()
        if (trip.kinematicState.isNotEmpty()) profileParts.add(trip.kinematicState)
        if (trip.gradientProfile.isNotEmpty()) profileParts.add(trip.gradientProfile)
        tvDetailProfile.text = if (profileParts.isNotEmpty()) profileParts.joinToString(" • ") else "STANDARD"

        // Set samples to charts
        chartTimeline.setSamples(samples)

        // Compute speed distribution
        if (samples.isNotEmpty()) {
            val lowCount = samples.count { it.speedKmh < 40 }
            val normalCount = samples.count { it.speedKmh in 40..80 }
            val highCount = samples.count { it.speedKmh > 80 }
            histogramSpeed.setDistribution(lowCount.toFloat(), normalCount.toFloat(), highCount.toFloat())
        } else {
            histogramSpeed.setDistribution(0f, 0f, 0f)
        }
    }

    private fun showDeleteConfirmDialog(trip: TripRecordItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_trip, null)
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.common_delete) { _, _ ->
                viewModel.deleteTrip(trip.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
