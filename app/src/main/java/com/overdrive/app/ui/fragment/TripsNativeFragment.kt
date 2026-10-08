package com.overdrive.app.ui.fragment

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.overdrive.app.R
import com.overdrive.app.navmap.nav.MapNetworking
import com.overdrive.app.ui.trips.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
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

    // Timeline Scrubber Card Views
    private lateinit var cardTimelineSlider: MaterialCardView
    private lateinit var tvSliderSpeed: TextView
    private lateinit var tvSliderSpeedUnit: TextView
    private lateinit var tvSliderAccel: TextView
    private lateinit var tvSliderBrake: TextView
    private lateinit var tvSliderSoc: TextView
    private lateinit var sbTimeline: SeekBar
    private lateinit var tvSliderStartTime: TextView
    private lateinit var tvSliderCurrentTime: TextView
    private lateinit var tvSliderEndTime: TextView

    // Route Map Views
    private lateinit var cardRouteMap: MaterialCardView
    private lateinit var mapViewTripRoute: MapView
    private lateinit var btnMapFitRoute: MaterialCardView
    private lateinit var btnMapFocusVehicle: MaterialCardView
    private lateinit var btnMapZoomIn: MaterialCardView
    private lateinit var btnMapZoomOut: MaterialCardView
    private var tripMap: MapLibreMap? = null

    // Pedal breakdown row
    private lateinit var tvTlAccelPct: TextView
    private lateinit var tvTlCoastPct: TextView
    private lateinit var tvTlBrakePct: TextView

    // Driving DNA Breakdown Card Views
    private lateinit var cardDnaBreakdown: MaterialCardView
    private lateinit var rowDnaAnticipation: View
    private lateinit var pbDetailAnticipation: ProgressBar
    private lateinit var tvDetailScoreAnticipation: TextView
    private lateinit var layoutAnticipationCoaching: View

    private lateinit var rowDnaSmoothness: View
    private lateinit var pbDetailSmoothness: ProgressBar
    private lateinit var tvDetailScoreSmoothness: TextView
    private lateinit var layoutSmoothnessCoaching: View

    private lateinit var rowDnaSpeedDisc: View
    private lateinit var pbDetailSpeedDisc: ProgressBar
    private lateinit var tvDetailScoreSpeedDisc: TextView
    private lateinit var layoutSpeedDiscCoaching: View

    private lateinit var rowDnaEfficiency: View
    private lateinit var pbDetailEfficiency: ProgressBar
    private lateinit var tvDetailScoreEfficiency: TextView
    private lateinit var layoutEfficiencyCoaching: View

    private lateinit var rowDnaConsistency: View
    private lateinit var pbDetailConsistency: ProgressBar
    private lateinit var tvDetailScoreConsistency: TextView
    private lateinit var layoutConsistencyCoaching: View

    private var activeTrip: TripRecordItem? = null
    private var activeSamples: List<TelemetrySampleItem> = emptyList()

    private val supportedCurrencies = listOf("₺", "$", "€", "£", "₹", "¥")
    private var isProgrammaticChange = false

    companion object {
        private const val ROUTE_SOURCE_ID = "trip_route_source"
        private const val ROUTE_CASING_LAYER_ID = "trip_route_casing"
        private const val ROUTE_LAYER_ID = "trip_route_layer"
        private const val MARKER_SOURCE_ID = "trip_marker_source"
        private const val MARKER_LAYER_ID = "trip_marker_layer"
        private const val CAR_SOURCE_ID = "trip_car_source"
        private const val CAR_LAYER_ID = "trip_car_layer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            MapLibre.getInstance(requireContext())
            MapNetworking.installMapLibreHttpClient()
        } catch (e: Exception) {
            android.util.Log.e("TripsNativeFragment", "Failed to init MapLibre runtime", e)
        }
    }

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
        setupMap(savedInstanceState)
        setupListeners()
        setupAdapter()
        setupBackPressHandling()
        observeState()
    }

    override fun onStart() {
        super.onStart()
        mapViewTripRoute.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapViewTripRoute.onResume()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        mapViewTripRoute.onPause()
        viewModel.stopPolling()
    }

    override fun onStop() {
        super.onStop()
        mapViewTripRoute.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapViewTripRoute.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapViewTripRoute.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tripMap = null
        mapViewTripRoute.onDestroy()
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

        // Stats tab
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

        // Storage tab
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

        // Detail Drill-in Views
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

        // Timeline Scrubber
        cardTimelineSlider = v.findViewById(R.id.cardTimelineSlider)
        tvSliderSpeed = v.findViewById(R.id.tvSliderSpeed)
        tvSliderSpeedUnit = v.findViewById(R.id.tvSliderSpeedUnit)
        tvSliderAccel = v.findViewById(R.id.tvSliderAccel)
        tvSliderBrake = v.findViewById(R.id.tvSliderBrake)
        tvSliderSoc = v.findViewById(R.id.tvSliderSoc)
        sbTimeline = v.findViewById(R.id.sbTimeline)
        tvSliderStartTime = v.findViewById(R.id.tvSliderStartTime)
        tvSliderCurrentTime = v.findViewById(R.id.tvSliderCurrentTime)
        tvSliderEndTime = v.findViewById(R.id.tvSliderEndTime)

        // Route Map
        cardRouteMap = v.findViewById(R.id.cardRouteMap)
        mapViewTripRoute = v.findViewById(R.id.mapViewTripRoute)
        btnMapFitRoute = v.findViewById(R.id.btnMapFitRoute)
        btnMapFocusVehicle = v.findViewById(R.id.btnMapFocusVehicle)
        btnMapZoomIn = v.findViewById(R.id.btnMapZoomIn)
        btnMapZoomOut = v.findViewById(R.id.btnMapZoomOut)

        // Pedal breakdown
        tvTlAccelPct = v.findViewById(R.id.tvTlAccelPct)
        tvTlCoastPct = v.findViewById(R.id.tvTlCoastPct)
        tvTlBrakePct = v.findViewById(R.id.tvTlBrakePct)

        // Driving DNA Breakdown Card
        cardDnaBreakdown = v.findViewById(R.id.cardDnaBreakdown)
        rowDnaAnticipation = v.findViewById(R.id.rowDnaAnticipation)
        pbDetailAnticipation = v.findViewById(R.id.pbDetailAnticipation)
        tvDetailScoreAnticipation = v.findViewById(R.id.tvDetailScoreAnticipation)
        layoutAnticipationCoaching = v.findViewById(R.id.layoutAnticipationCoaching)

        rowDnaSmoothness = v.findViewById(R.id.rowDnaSmoothness)
        pbDetailSmoothness = v.findViewById(R.id.pbDetailSmoothness)
        tvDetailScoreSmoothness = v.findViewById(R.id.tvDetailScoreSmoothness)
        layoutSmoothnessCoaching = v.findViewById(R.id.layoutSmoothnessCoaching)

        rowDnaSpeedDisc = v.findViewById(R.id.rowDnaSpeedDisc)
        pbDetailSpeedDisc = v.findViewById(R.id.pbDetailSpeedDisc)
        tvDetailScoreSpeedDisc = v.findViewById(R.id.tvDetailScoreSpeedDisc)
        layoutSpeedDiscCoaching = v.findViewById(R.id.layoutSpeedDiscCoaching)

        rowDnaEfficiency = v.findViewById(R.id.rowDnaEfficiency)
        pbDetailEfficiency = v.findViewById(R.id.pbDetailEfficiency)
        tvDetailScoreEfficiency = v.findViewById(R.id.tvDetailScoreEfficiency)
        layoutEfficiencyCoaching = v.findViewById(R.id.layoutEfficiencyCoaching)

        rowDnaConsistency = v.findViewById(R.id.rowDnaConsistency)
        pbDetailConsistency = v.findViewById(R.id.pbDetailConsistency)
        tvDetailScoreConsistency = v.findViewById(R.id.tvDetailScoreConsistency)
        layoutConsistencyCoaching = v.findViewById(R.id.layoutConsistencyCoaching)

        // Setup currency spinner
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, supportedCurrencies)
        spinnerTripCurrency.adapter = adapter
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        mapViewTripRoute.onCreate(savedInstanceState)
        var startX = 0f
        var startY = 0f
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        mapViewTripRoute.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount > 1) {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    } else {
                        val dx = Math.abs(event.x - startX)
                        val dy = Math.abs(event.y - startY)
                        if (dy > touchSlop && dy > dx * 1.2f) {
                            v.parent.requestDisallowInterceptTouchEvent(false)
                        } else if (dx > touchSlop) {
                            v.parent.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        mapViewTripRoute.getMapAsync { map ->
            tripMap = map
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isLogoEnabled = false
            loadMapStyle { style ->
                setupMapLayers(style)
                plotTripRoute()
            }
        }
    }

    private fun loadMapStyle(onStyleLoaded: (Style) -> Unit) {
        val mlMap = tripMap ?: return
        val isNight = isNightMode()
        val assetPath = if (isNight) "maps/dark_style.json" else "maps/liberty_style.json"
        val json = try {
            requireContext().assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
                .takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            null
        }

        val builder = if (json != null) {
            Style.Builder().fromJson(json)
        } else {
            val url = if (isNight) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/liberty"
            Style.Builder().fromUri(url)
        }

        mlMap.setStyle(builder) { style ->
            onStyleLoaded(style)
        }
    }

    private fun setupMapLayers(style: Style) {
        // Register marker icons
        style.addImage("marker_start", createCircleMarkerBitmap(Color.parseColor("#22C55E"), "S"))
        style.addImage("marker_end", createCircleMarkerBitmap(Color.parseColor("#EF4444"), "E"))
        style.addImage("marker_car", createCarMarkerBitmap())

        // 1. Route line sources and layers
        if (style.getSource(ROUTE_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE_ID))

            // Casing layer
            style.addLayer(
                LineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    PropertyFactory.lineColor(Color.parseColor("#4D000000")),
                    PropertyFactory.lineWidth(8f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
            )

            // Main speed-colored layer
            style.addLayer(
                LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                    PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
                    PropertyFactory.lineWidth(5.5f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
            )
        }

        // 2. Start & End Markers
        if (style.getSource(MARKER_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(MARKER_SOURCE_ID))
            style.addLayer(
                SymbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
                    PropertyFactory.iconImage(Expression.get("icon")),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
            )
        }

        // 3. Vehicle Position / Heading Marker
        if (style.getSource(CAR_SOURCE_ID) == null) {
            style.addSource(GeoJsonSource(CAR_SOURCE_ID))
            style.addLayer(
                SymbolLayer(CAR_LAYER_ID, CAR_SOURCE_ID).withProperties(
                    PropertyFactory.iconImage("marker_car"),
                    PropertyFactory.iconRotate(Expression.toNumber(Expression.get("heading"))),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
            )
        }
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

        // Timeline SeekBar scrubbing
        sbTimeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateScrubPosition(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Chart timeline direct scrubbing
        chartTimeline.onScrubListener = { idx ->
            sbTimeline.progress = idx
        }

        // DNA Coaching rows toggle
        rowDnaAnticipation.setOnClickListener { toggleCoaching(layoutAnticipationCoaching) }
        rowDnaSmoothness.setOnClickListener { toggleCoaching(layoutSmoothnessCoaching) }
        rowDnaSpeedDisc.setOnClickListener { toggleCoaching(layoutSpeedDiscCoaching) }
        rowDnaEfficiency.setOnClickListener { toggleCoaching(layoutEfficiencyCoaching) }
        rowDnaConsistency.setOnClickListener { toggleCoaching(layoutConsistencyCoaching) }

        // Floating Map Controls
        btnMapFitRoute.setOnClickListener {
            fitRouteBounds(animate = true)
        }
        btnMapFocusVehicle.setOnClickListener {
            focusVehicleMarker(animate = true)
        }
        btnMapZoomIn.setOnClickListener {
            tripMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        btnMapZoomOut.setOnClickListener {
            tripMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
    }

    private fun toggleCoaching(v: View) {
        v.visibility = if (v.visibility == View.VISIBLE) View.GONE else View.VISIBLE
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
        activeTrip = trip
        activeSamples = samples

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

        // 1. Set samples to chart
        chartTimeline.setSamples(samples, trip.socStart, trip.socEnd)

        // 2. Compute pedal breakdown percentages
        if (samples.isNotEmpty()) {
            var accelCount = 0
            var brakeCount = 0
            var coastCount = 0
            for (s in samples) {
                if (s.brakePedalPercent > 0) brakeCount++
                else if (s.accelPedalPercent > 0) accelCount++
                else coastCount++
            }
            val total = accelCount + brakeCount + coastCount
            val accelPct = if (total > 0) Math.round((accelCount.toFloat() / total) * 100) else 0
            val coastPct = if (total > 0) Math.round((coastCount.toFloat() / total) * 100) else 0
            val brakePct = if (total > 0) Math.round((brakeCount.toFloat() / total) * 100) else 0

            tvTlAccelPct.text = "$accelPct%"
            tvTlCoastPct.text = "$coastPct%"
            tvTlBrakePct.text = "$brakePct%"
        } else {
            tvTlAccelPct.text = "--%"
            tvTlCoastPct.text = "--%"
            tvTlBrakePct.text = "--%"
        }

        // 3. Compute speed distribution
        if (samples.isNotEmpty()) {
            val lowCount = samples.count { it.speedKmh < 40 }
            val normalCount = samples.count { it.speedKmh in 40..80 }
            val highCount = samples.count { it.speedKmh > 80 }
            histogramSpeed.setDistribution(lowCount.toFloat(), normalCount.toFloat(), highCount.toFloat())
        } else {
            histogramSpeed.setDistribution(0f, 0f, 0f)
        }

        // 4. Driving DNA Breakdown Card
        val hasDna = trip.overallScore > 0 || trip.anticipationScore > 0 ||
            trip.smoothnessScore > 0 || trip.speedDisciplineScore > 0 ||
            trip.efficiencyScore > 0 || trip.consistencyScore > 0

        if (hasDna) {
            cardDnaBreakdown.visibility = View.VISIBLE
            pbDetailAnticipation.progress = trip.anticipationScore
            tvDetailScoreAnticipation.text = if (trip.anticipationScore > 0) trip.anticipationScore.toString() else "--"

            pbDetailSmoothness.progress = trip.smoothnessScore
            tvDetailScoreSmoothness.text = if (trip.smoothnessScore > 0) trip.smoothnessScore.toString() else "--"

            pbDetailSpeedDisc.progress = trip.speedDisciplineScore
            tvDetailScoreSpeedDisc.text = if (trip.speedDisciplineScore > 0) trip.speedDisciplineScore.toString() else "--"

            pbDetailEfficiency.progress = trip.efficiencyScore
            tvDetailScoreEfficiency.text = if (trip.efficiencyScore > 0) trip.efficiencyScore.toString() else "--"

            pbDetailConsistency.progress = trip.consistencyScore
            tvDetailScoreConsistency.text = if (trip.consistencyScore > 0) trip.consistencyScore.toString() else "--"
        } else {
            cardDnaBreakdown.visibility = View.GONE
        }

        // 5. Timeline Scrubber
        if (samples.size >= 2) {
            cardTimelineSlider.visibility = View.VISIBLE
            sbTimeline.max = samples.size - 1
            sbTimeline.progress = 0
            tvSliderStartTime.text = "0:00"

            val durationSec = ((samples.last().timestampMs - samples.first().timestampMs) / 1000).coerceAtLeast(0L)
            val mins = durationSec / 60
            val secs = durationSec % 60
            tvSliderEndTime.text = String.format(Locale.US, "%d:%02d", mins, secs)

            updateScrubPosition(0)
        } else {
            cardTimelineSlider.visibility = View.GONE
        }

        // 6. Plot Route on Map
        plotTripRoute()
    }

    private fun updateScrubPosition(idx: Int) {
        if (activeSamples.isEmpty() || idx !in activeSamples.indices) return
        val s = activeSamples[idx]

        // 1. Text HUD stats
        val isMiles = (viewModel.uiState.value.config?.distanceUnit == "mi")
        val displaySpeed = if (isMiles) (s.speedKmh * 0.621371).toInt() else s.speedKmh
        tvSliderSpeed.text = displaySpeed.toString()
        tvSliderSpeedUnit.text = if (isMiles) "mph" else "km/h"
        tvSliderAccel.text = "${s.accelPedalPercent}%"
        tvSliderBrake.text = "${s.brakePedalPercent}%"

        // SoC interpolation
        val trip = activeTrip
        if (trip != null && trip.socStart > 0 && trip.socEnd > 0) {
            val total = (activeSamples.size - 1).coerceAtLeast(1)
            val soc = trip.socStart + (trip.socEnd - trip.socStart) * (idx.toDouble() / total)
            tvSliderSoc.text = String.format(Locale.US, "%.1f%%", soc)
        } else {
            tvSliderSoc.text = "--%"
        }

        // Elapsed time
        val startMs = activeSamples.first().timestampMs
        val elapsedSec = ((s.timestampMs - startMs) / 1000).coerceAtLeast(0L)
        val mins = elapsedSec / 60
        val secs = elapsedSec % 60
        tvSliderCurrentTime.text = String.format(Locale.US, "%d:%02d", mins, secs)

        // 2. Chart timeline scrubber
        chartTimeline.setScrubberIndex(idx)

        // 3. Move vehicle marker on Map
        val ptSample = if (s.lat != 0.0 && s.lon != 0.0 && s.lat.isFinite() && s.lon.isFinite()) {
            s
        } else {
            activeSamples.minByOrNull { sample ->
                if (sample.lat != 0.0 && sample.lon != 0.0 && sample.lat.isFinite() && sample.lon.isFinite()) {
                    Math.abs(sample.timestampMs - s.timestampMs)
                } else {
                    Long.MAX_VALUE
                }
            }
        }
        if (ptSample != null && ptSample.lat != 0.0 && ptSample.lon != 0.0) {
            val carPt = Point.fromLngLat(ptSample.lon, ptSample.lat)
            val heading = computeSmoothedHeading(activeSamples, idx) ?: 0f
            val carFeature = Feature.fromGeometry(carPt).apply {
                addNumberProperty("heading", heading)
            }
            tripMap?.style?.getSourceAs<GeoJsonSource>(CAR_SOURCE_ID)?.setGeoJson(
                FeatureCollection.fromFeatures(listOf(carFeature))
            )
        }
    }

    private fun plotTripRoute() {
        val map = tripMap ?: return
        val style = map.style ?: return
        val samples = activeSamples
        val gpsPoints = samples.filter { it.lat != 0.0 && it.lon != 0.0 && it.lat.isFinite() && it.lon.isFinite() }

        val routeSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
        val markerSource = style.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID)
        val carSource = style.getSourceAs<GeoJsonSource>(CAR_SOURCE_ID)

        if (gpsPoints.size < 2) {
            routeSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            markerSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            carSource?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }

        // 1. Build speed-banded route LineStrings
        val features = mutableListOf<Feature>()
        var runStart = 0
        var runColor = getSpeedColor(gpsPoints[0].speedKmh)

        for (i in 1..gpsPoints.size) {
            val segColor = if (i < gpsPoints.size) getSpeedColor(gpsPoints[i].speedKmh) else null
            if (i == gpsPoints.size || segColor != runColor) {
                val segmentPoints = gpsPoints.subList(runStart, i).map {
                    Point.fromLngLat(it.lon, it.lat)
                }
                if (segmentPoints.size >= 2) {
                    val line = LineString.fromLngLats(segmentPoints)
                    val feat = Feature.fromGeometry(line).apply {
                        addStringProperty("color", runColor)
                    }
                    features.add(feat)
                }
                runStart = (i - 1).coerceAtLeast(0)
                if (segColor != null) runColor = segColor
            }
        }
        routeSource?.setGeoJson(FeatureCollection.fromFeatures(features))

        // 2. Start & End Markers
        val markerFeatures = mutableListOf<Feature>()
        val startPt = Point.fromLngLat(gpsPoints.first().lon, gpsPoints.first().lat)
        val endPt = Point.fromLngLat(gpsPoints.last().lon, gpsPoints.last().lat)
        markerFeatures.add(Feature.fromGeometry(startPt).apply { addStringProperty("icon", "marker_start") })
        markerFeatures.add(Feature.fromGeometry(endPt).apply { addStringProperty("icon", "marker_end") })
        markerSource?.setGeoJson(FeatureCollection.fromFeatures(markerFeatures))

        // 3. Initial Car Marker at start
        val heading = computeSmoothedHeading(gpsPoints, 0) ?: 0f
        val carFeature = Feature.fromGeometry(startPt).apply {
            addNumberProperty("heading", heading)
        }
        carSource?.setGeoJson(FeatureCollection.fromFeatures(listOf(carFeature)))

        // 4. Fit Camera Bounds
        fitRouteBounds(animate = false)
    }

    private fun fitRouteBounds(animate: Boolean) {
        val map = tripMap ?: return
        val gpsPoints = activeSamples.filter { it.lat != 0.0 && it.lon != 0.0 && it.lat.isFinite() && it.lon.isFinite() }
        if (gpsPoints.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()
        for (pt in gpsPoints) {
            boundsBuilder.include(LatLng(pt.lat, pt.lon))
        }
        val bounds = boundsBuilder.build()
        val update = if (bounds.latitudeSpan > 0.0001 || bounds.longitudeSpan > 0.0001) {
            CameraUpdateFactory.newLatLngBounds(bounds, 60)
        } else {
            CameraUpdateFactory.newLatLngZoom(LatLng(gpsPoints[0].lat, gpsPoints[0].lon), 15.0)
        }

        if (animate) {
            map.animateCamera(update, 600, null)
        } else {
            map.easeCamera(update, 500)
        }
    }

    private fun focusVehicleMarker(animate: Boolean) {
        val map = tripMap ?: return
        if (activeSamples.isEmpty()) return
        val idx = sbTimeline.progress.coerceIn(activeSamples.indices)
        val s = activeSamples[idx]

        val validSample = if (s.lat != 0.0 && s.lon != 0.0 && s.lat.isFinite() && s.lon.isFinite()) {
            s
        } else {
            activeSamples.minByOrNull { sample ->
                if (sample.lat != 0.0 && sample.lon != 0.0 && sample.lat.isFinite() && sample.lon.isFinite()) {
                    Math.abs(sample.timestampMs - s.timestampMs)
                } else {
                    Long.MAX_VALUE
                }
            }
        } ?: return

        val target = LatLng(validSample.lat, validSample.lon)
        val currentZoom = map.cameraPosition.zoom
        val targetZoom = if (currentZoom < 16.0) 16.5 else currentZoom
        val update = CameraUpdateFactory.newLatLngZoom(target, targetZoom)

        if (animate) {
            map.animateCamera(update, 600, null)
        } else {
            map.easeCamera(update, 500)
        }
    }

    private fun getSpeedColor(speed: Int): String {
        return when {
            speed < 40 -> "#22C55E" // green
            speed <= 80 -> "#EAB308" // yellow
            else -> "#EF4444" // red
        }
    }

    private fun computeSmoothedHeading(samples: List<TelemetrySampleItem>, idx: Int): Float? {
        if (samples.size < 2 || idx !in samples.indices) return null
        var sumSin = 0.0
        var sumCos = 0.0
        var pairs = 0
        val minDelta = 3e-5
        val window = 5
        val lo = (idx - window).coerceAtLeast(0)
        val hi = (idx + window).coerceAtMost(samples.size - 1)
        for (i in lo until hi) {
            val a = samples[i]
            val b = samples[i + 1]
            if (a.lat == 0.0 || a.lon == 0.0 || b.lat == 0.0 || b.lon == 0.0) continue
            if (Math.abs(b.lat - a.lat) < minDelta && Math.abs(b.lon - a.lon) < minDelta) continue
            val dLon = Math.toRadians(b.lon - a.lon)
            val lat1 = Math.toRadians(a.lat)
            val lat2 = Math.toRadians(b.lat)
            val y = Math.sin(dLon) * Math.cos(lat2)
            val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
            val bearingRad = Math.atan2(y, x)
            sumSin += Math.sin(bearingRad)
            sumCos += Math.cos(bearingRad)
            pairs++
        }
        if (pairs == 0) {
            if (idx < samples.size - 1) {
                val a = samples[idx]
                val b = samples[idx + 1]
                val dLon = Math.toRadians(b.lon - a.lon)
                val lat1 = Math.toRadians(a.lat)
                val lat2 = Math.toRadians(b.lat)
                val y = Math.sin(dLon) * Math.cos(lat2)
                val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
                return Math.toDegrees(Math.atan2(y, x)).toFloat()
            }
            return null
        }
        return Math.toDegrees(Math.atan2(sumSin / pairs, sumCos / pairs)).toFloat()
    }

    private fun createCircleMarkerBitmap(color: Int, text: String): Bitmap {
        val size = 56
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, strokePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, size / 2f, size / 2f - yOffset, textPaint)
        return bitmap
    }

    private fun createCarMarkerBitmap(): Bitmap {
        val src = try {
            BitmapFactory.decodeResource(resources, R.drawable.car_top_view)
                ?: BitmapFactory.decodeResource(resources, R.drawable.car_icon_map)
        } catch (e: Exception) {
            null
        } ?: return createFallbackCarMarkerBitmap()

        val density = resources.displayMetrics.density
        val targetW = (28 * density).toInt().coerceIn(24, 72)
        val targetH = (targetW * (src.height.toFloat() / src.width.toFloat())).toInt()

        val padding = (4 * density).toInt()
        val totalW = targetW + padding * 2
        val totalH = targetH + padding * 2

        val result = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Drop shadow matching the Trips (Eski) web style
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 0, 0, 0)
            maskFilter = BlurMaskFilter(padding.toFloat(), BlurMaskFilter.Blur.NORMAL)
        }
        val carRect = RectF(
            padding.toFloat(),
            padding.toFloat(),
            (padding + targetW).toFloat(),
            (padding + targetH).toFloat()
        )
        canvas.drawRoundRect(carRect, targetW * 0.2f, targetW * 0.2f, shadowPaint)

        // Draw car icon
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val srcRect = Rect(0, 0, src.width, src.height)
        canvas.drawBitmap(src, srcRect, carRect, paint)

        return result
    }

    private fun createFallbackCarMarkerBitmap(): Bitmap {
        val w = 40
        val h = 64
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00D4AA")
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val path = Path().apply {
            moveTo(w / 2f, 2f)
            lineTo(w - 4f, 18f)
            lineTo(w - 4f, h - 6f)
            quadTo(w / 2f, h - 2f, 4f, h - 6f)
            lineTo(4f, 18f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.drawPath(path, strokePaint)

        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#80FFFFFF")
            style = Paint.Style.FILL
        }
        val glassPath = Path().apply {
            moveTo(w / 2f, 10f)
            lineTo(w - 10f, 22f)
            lineTo(10f, 22f)
            close()
        }
        canvas.drawPath(glassPath, glassPaint)
        return bitmap
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
