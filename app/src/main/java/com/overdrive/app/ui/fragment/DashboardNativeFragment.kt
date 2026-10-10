package com.overdrive.app.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.overdrive.app.R
import com.overdrive.app.auth.AuthManager
import com.overdrive.app.client.CameraDaemonClient
import com.overdrive.app.ui.cockpit.CockpitViewModel
import com.overdrive.app.ui.dashboard.DashboardAiInsight
import com.overdrive.app.ui.dashboard.DashboardInsight
import com.overdrive.app.ui.dashboard.DashboardInsightProvider
import com.overdrive.app.ui.dashboard.DashboardUiState
import com.overdrive.app.ui.model.DaemonState
import com.overdrive.app.ui.model.DaemonStatus
import com.overdrive.app.ui.model.DaemonType
import com.overdrive.app.ui.model.localizedName
import com.overdrive.app.ui.util.QrCodeGenerator
import com.overdrive.app.ui.vehicle.VehicleArt
import com.overdrive.app.ui.viewmodel.DaemonsViewModel
import com.overdrive.app.ui.viewmodel.MainViewModel
import com.overdrive.app.ui.viewmodel.RecordingViewModel
import com.overdrive.app.ui.widget.AppToast
import com.overdrive.app.ui.widget.Skeleton
import com.overdrive.app.util.DaemonHttpClient
import com.overdrive.app.util.DeviceIdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Pure native, zero-WebView cockpit dashboard fragment.
 * Driven strictly by CockpitViewModel, StateFlow, and Coroutines on Dispatchers.IO.
 * Designed for responsive in-car operation across all head unit tiers (Snapdragon 625/665, 3-4 GB RAM).
 */
class DashboardNativeFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    private val daemonsViewModel: DaemonsViewModel by activityViewModels()
    private val recordingViewModel: RecordingViewModel by activityViewModels()
    private val cockpitViewModel: CockpitViewModel by viewModels()

    // Hero
    private lateinit var heroGreeting: TextView
    private lateinit var heroSubtitle: TextView
    private lateinit var heroChipTunnel: Chip
    private lateinit var heroChipServices: Chip
    private lateinit var heroChipRecording: Chip
    private lateinit var vehicleSocValue: TextView
    private lateinit var vehicleRangeValue: TextView

    // Optional GenAI card
    private lateinit var aiInsightCard: MaterialCardView
    private lateinit var aiInsightTitle: TextView
    private lateinit var aiInsightText: TextView
    private lateinit var aiInsightMeta: TextView
    private lateinit var aiInsightExpand: ImageView
    private var aiInsightExpanded = false

    // Charging block
    private lateinit var chargingCard: MaterialCardView
    private lateinit var chargingStateValue: TextView
    private lateinit var chargingPowerGroup: View
    private lateinit var chargingPowerValue: TextView
    private lateinit var chargingEtaGroup: View
    private lateinit var chargingEtaValue: TextView
    private lateinit var chargingSessionGroup: View
    private lateinit var chargingSessionValue: TextView

    // Recordings and storage
    private lateinit var metricRecordings: MaterialCardView
    private lateinit var metricRecordingsValue: TextView
    private lateinit var metricStorageValue: TextView
    private lateinit var recordingStorageProgress:
        com.google.android.material.progressindicator.LinearProgressIndicator

    private class ActivityRowViews(
        val container: View,
        val icon: ImageView,
        val text: TextView,
    )

    private var activityRows: List<ActivityRowViews> = emptyList()

    // Tunnel / Connect
    private lateinit var metricTunnel: MaterialCardView
    private lateinit var metricTunnelValue: TextView
    private lateinit var tunnelStateDot: View
    private lateinit var cardDaemons: MaterialCardView
    private lateinit var tvDaemonsStatus: TextView

    private lateinit var ivQrCode: ImageView
    private lateinit var qrContainer: FrameLayout
    private lateinit var tvQrPlaceholder: TextView
    private lateinit var tvUrl: TextView
    private lateinit var tvDeviceId: TextView
    private lateinit var chipGroupTunnels: ChipGroup
    private lateinit var remoteDetails: View
    private lateinit var btnExpandRemote: ImageView
    private var selectedTunnel: DaemonType? = null
    private var lastRenderedQrUrl: String? = null
    private var hasRenderedQrForView = false

    // Auth
    private lateinit var tvDeviceToken: TextView
    private lateinit var btnToggleToken: ImageView
    private lateinit var btnCopyToken: ImageView
    private lateinit var btnRegenerateToken: MaterialButton
    private var isTokenVisible = false

    private lateinit var quickLive: MaterialCardView
    private var metricVehicle: View? = null
    private var metricVehicleValue: TextView? = null

    private var heroSocProgress:
        com.google.android.material.progressindicator.LinearProgressIndicator? = null
    private var vehicleRangeLabel: TextView? = null
    private var heroRangeBreakdown: TextView? = null
    private var vehicleHalRangeColumn: View? = null
    private var vehicleHalRangeValue: TextView? = null
    private var vehicleArt: ImageView? = null
    private var quickTrips: View? = null
    private var quickVehicleControl: View? = null

    private var appToast: AppToast? = null
    private var skeleton: Skeleton? = null
    private var insightsProvider: DashboardInsightProvider? = null
    private var firstVisitCount: Int = -1

    companion object {
        private const val LOW_SOC_THRESHOLD_PERCENT = 20.0
        private const val WELCOME_INSIGHT_PRIORITY = 100
        private const val AI_INSIGHT_PREVIEW_LINES = 5
        private const val STATE_REMOTE_EXPANDED = "dashboard.remote_expanded"
        private const val STATE_AI_INSIGHT_EXPANDED = "dashboard.ai_insight_expanded"
        private const val STATE_SELECTED_TUNNEL = "dashboard.selected_tunnel"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        appToast = AppToast(view)
        bindSkeletons(view)
        wireClicks()
        observeSharedViewModels()
        observeCockpitUiState()

        val remoteExpanded = savedInstanceState?.getBoolean(STATE_REMOTE_EXPANDED, false) ?: false
        cockpitViewModel.setRemoteExpanded(remoteExpanded)

        selectedTunnel = savedInstanceState
            ?.getString(STATE_SELECTED_TUNNEL)
            ?.let { runCatching { DaemonType.valueOf(it) }.getOrNull() }
        aiInsightExpanded = savedInstanceState
            ?.getBoolean(STATE_AI_INSIGHT_EXPANDED, false) == true

        tvDeviceId.text = DeviceIdGenerator.generateDeviceId(requireContext())
        loadAuthState()

        if (insightsProvider == null) {
            val provider = DashboardInsightProvider(requireContext().applicationContext)
            insightsProvider = provider
            firstVisitCount = provider.recordDashboardVisit()
        }
    }

    override fun onResume() {
        super.onResume()
        cockpitViewModel.startPolling()
        recordingViewModel.updateStorageInfo()
        refreshVehicleTile()
        rebuildInsightsAsync()
        loadAuthState()
    }

    override fun onPause() {
        cockpitViewModel.stopPolling()
        super.onPause()
    }

    override fun onDestroyView() {
        appToast?.cancel()
        appToast = null
        skeleton?.cancel()
        skeleton = null
        if (::ivQrCode.isInitialized) ivQrCode.setImageDrawable(null)
        lastRenderedQrUrl = null
        hasRenderedQrForView = false
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_REMOTE_EXPANDED, cockpitViewModel.uiState.value.remoteExpanded)
        outState.putBoolean(STATE_AI_INSIGHT_EXPANDED, aiInsightExpanded)
        selectedTunnel?.let { outState.putString(STATE_SELECTED_TUNNEL, it.name) }
        super.onSaveInstanceState(outState)
    }

    private fun bindSkeletons(view: View) {
        val sk = Skeleton(view)
        this.skeleton = sk
        sk.bind(R.id.vehicleSocSkeleton, vehicleSocValue)
        sk.bind(R.id.vehicleRangeSkeleton, vehicleRangeValue)
        sk.bind(R.id.metricRecordingsSkeleton, metricRecordingsValue)
        sk.bind(R.id.metricStorageSkeleton, metricStorageValue)
        activityRows.firstOrNull()?.let {
            sk.bind(R.id.activityRow1Skeleton, it.icon, it.text)
        }
    }

    private fun bindViews(view: View) {
        heroGreeting = view.findViewById(R.id.heroGreeting)
        heroSubtitle = view.findViewById(R.id.heroSubtitle)
        heroChipTunnel = view.findViewById(R.id.heroChipTunnel)
        heroChipServices = view.findViewById(R.id.heroChipServices)
        heroChipRecording = view.findViewById(R.id.heroChipRecording)
        vehicleSocValue = view.findViewById(R.id.vehicleSocValue)
        vehicleRangeValue = view.findViewById(R.id.vehicleRangeValue)
        aiInsightCard = view.findViewById(R.id.aiInsightCard)
        aiInsightTitle = view.findViewById(R.id.aiInsightTitle)
        aiInsightText = view.findViewById(R.id.aiInsightText)
        aiInsightMeta = view.findViewById(R.id.aiInsightMeta)
        aiInsightExpand = view.findViewById(R.id.aiInsightExpand)

        chargingCard = view.findViewById(R.id.chargingCard)
        chargingStateValue = view.findViewById(R.id.chargingStateValue)
        chargingPowerGroup = view.findViewById(R.id.chargingPowerGroup)
        chargingPowerValue = view.findViewById(R.id.chargingPowerValue)
        chargingEtaGroup = view.findViewById(R.id.chargingEtaGroup)
        chargingEtaValue = view.findViewById(R.id.chargingEtaValue)
        chargingSessionGroup = view.findViewById(R.id.chargingSessionGroup)
        chargingSessionValue = view.findViewById(R.id.chargingSessionValue)

        metricRecordings = view.findViewById(R.id.metricRecordings)
        metricRecordingsValue = view.findViewById(R.id.metricRecordingsValue)
        metricStorageValue = view.findViewById(R.id.metricStorageValue)
        recordingStorageProgress = view.findViewById(R.id.recordingStorageProgress)
        activityRows = listOf(
            Triple(R.id.activityItem1, R.id.activityIcon1, R.id.activityRow1),
            Triple(R.id.activityItem2, R.id.activityIcon2, R.id.activityRow2),
            Triple(R.id.activityItem3, R.id.activityIcon3, R.id.activityRow3),
        ).map { (containerId, iconId, textId) ->
            ActivityRowViews(
                view.findViewById(containerId),
                view.findViewById(iconId),
                view.findViewById(textId),
            )
        }
        metricTunnel = view.findViewById(R.id.metricTunnel)
        metricTunnelValue = view.findViewById(R.id.metricTunnelValue)
        tunnelStateDot = view.findViewById(R.id.tunnelStateDot)
        cardDaemons = view.findViewById(R.id.cardDaemons)
        tvDaemonsStatus = view.findViewById(R.id.tvDaemonsStatus)

        ivQrCode = view.findViewById(R.id.ivQrCode)
        qrContainer = view.findViewById(R.id.qrContainer)
        tvQrPlaceholder = view.findViewById(R.id.tvQrPlaceholder)
        tvUrl = view.findViewById(R.id.tvUrl)
        tvDeviceId = view.findViewById(R.id.tvDeviceId)
        chipGroupTunnels = view.findViewById(R.id.chipGroupTunnels)
        remoteDetails = view.findViewById(R.id.remoteDetails)
        btnExpandRemote = view.findViewById(R.id.btnExpandRemote)

        tvDeviceToken = view.findViewById(R.id.tvDeviceToken)
        btnToggleToken = view.findViewById(R.id.btnToggleToken)
        btnCopyToken = view.findViewById(R.id.btnCopyToken)
        btnRegenerateToken = view.findViewById(R.id.btnRegenerateToken)

        quickLive = view.findViewById(R.id.quickLive)
        quickTrips = view.findViewById(R.id.quickTrips)
        quickVehicleControl = view.findViewById(R.id.quickVehicleControl)
        heroSocProgress = view.findViewById(R.id.heroSocProgress)
        vehicleRangeLabel = view.findViewById(R.id.vehicleRangeLabel)
        heroRangeBreakdown = view.findViewById(R.id.heroRangeBreakdown)
        vehicleHalRangeColumn = view.findViewById(R.id.vehicleHalRangeColumn)
        vehicleHalRangeValue = view.findViewById(R.id.vehicleHalRangeValue)
        vehicleArt = view.findViewById(R.id.vehicleArt)

        metricVehicle = view.findViewById(R.id.metricVehicle)
        metricVehicleValue = view.findViewById(R.id.metricVehicleValue)
    }

    private fun wireClicks() {
        val fadeThrough = com.overdrive.app.ui.util.NavOptionsExt.m3FadeThrough()
        metricRecordings.setOnClickListener {
            findNavController().navigate(R.id.recordingsFragment, null, fadeThrough)
        }
        cardDaemons.setOnClickListener {
            findNavController().navigate(R.id.daemonsFragment, null, fadeThrough)
        }
        quickLive.setOnClickListener {
            findNavController().navigate(R.id.liveViewFragment, null, fadeThrough)
        }
        quickTrips?.setOnClickListener {
            findNavController().navigate(R.id.tripsFragment, null, fadeThrough)
        }
        quickVehicleControl?.setOnClickListener {
            findNavController().navigate(R.id.vehicleControlFragment, null, fadeThrough)
        }
        aiInsightCard.setOnClickListener {
            aiInsightExpanded = !aiInsightExpanded
            renderAiInsightExpansion()
        }
        metricTunnel.setOnClickListener {
            val nextState = !cockpitViewModel.uiState.value.remoteExpanded
            cockpitViewModel.setRemoteExpanded(nextState)
        }
        metricVehicle?.setOnClickListener { showVehicleCapacityDialog() }

        btnToggleToken.setOnClickListener { toggleTokenVisibility() }
        btnCopyToken.setOnClickListener { copyTokenToClipboard() }
        btnRegenerateToken.setOnClickListener { showRegenerateConfirmation() }
    }

    private fun observeSharedViewModels() {
        daemonsViewModel.daemonStates.observe(viewLifecycleOwner) { states ->
            val running = states.values.count { it.status == DaemonStatus.RUNNING }
            val total = states.size
            tvDaemonsStatus.text = getString(R.string.dashboard_daemons_running, running, total)
            rebuildTunnelChips()
            updateTunnelTile()
            refreshHeroChips()
        }

        val rebuild = Observer<String?> { _ ->
            rebuildTunnelChips()
            updateTunnelTile()
            refreshHeroChips()
        }
        daemonsViewModel.cloudflaredController.tunnelUrl.observe(viewLifecycleOwner, rebuild)
        daemonsViewModel.zrokController.tunnelUrl.observe(viewLifecycleOwner, rebuild)
        daemonsViewModel.tailscaleController.tunnelUrl.observe(viewLifecycleOwner, rebuild)

        recordingViewModel.isRecording.observe(viewLifecycleOwner) { _ ->
            renderRecordingsState(cockpitViewModel.uiState.value.recordings)
            refreshHeroChips()
        }
        recordingViewModel.storageInfo.observe(viewLifecycleOwner) { info ->
            val summary = info?.takeIf { it.totalBytes > 0L }?.let {
                DashboardUiState.StorageSummary(
                    usedBytes = it.usedBytes.coerceAtLeast(0L),
                    availableBytes = it.availableBytes.coerceAtLeast(0L),
                    totalBytes = it.totalBytes,
                )
            }
            cockpitViewModel.updateStorageSummary(summary)
        }
    }

    private fun observeCockpitUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cockpitViewModel.uiState.collect { state ->
                    renderVehicleState(state.vehicle)
                    renderRecordingsState(state.recordings)
                    renderActivityState(state.activity)
                    renderRemoteExpansion(state.remoteExpanded)
                }
            }
        }
    }

    private fun refreshHeroChips() {
        heroChipTunnel.text = metricTunnelValue.text
        heroChipServices.text = tvDaemonsStatus.text
        val recording = recordingViewModel.isRecording.value == true
        heroChipRecording.text = if (recording) {
            getString(R.string.dashboard_chip_recording_active)
        } else {
            getString(R.string.dashboard_chip_recording_idle)
        }

        tintStatusChip(
            heroChipTunnel,
            if (collectAvailableTunnels().isEmpty()) StatusTone.DOWN else StatusTone.LIVE
        )
        tintStatusChip(
            heroChipServices,
            when (computeCoreHealth(daemonsViewModel.daemonStates.value)) {
                CoreHealth.OK -> StatusTone.LIVE
                CoreHealth.ALERT -> StatusTone.DOWN
                CoreHealth.UNKNOWN -> StatusTone.IDLE
            }
        )
        tintStatusChip(heroChipRecording, if (recording) StatusTone.LIVE else StatusTone.IDLE)
    }

    private enum class StatusTone { LIVE, IDLE, DOWN }

    private fun tintStatusChip(chip: Chip, tone: StatusTone) {
        val ctx = context ?: return
        val (fill, label) = when (tone) {
            StatusTone.LIVE ->
                R.color.overdrive_status_success_container to R.color.overdrive_status_success
            StatusTone.IDLE ->
                R.color.overdrive_status_warning_container to R.color.overdrive_status_warning
            StatusTone.DOWN ->
                R.color.overdrive_status_danger_container to R.color.overdrive_status_danger
        }
        chip.chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(ctx, fill))
        chip.setTextColor(ContextCompat.getColor(ctx, label))
    }

    private fun renderRecordingsState(recordingState: DashboardUiState.RecordingState) {
        if (!::metricRecordingsValue.isInitialized) return
        val unavailable = recordingState == DashboardUiState.RecordingState.Unavailable
        if (unavailable || recordingState is DashboardUiState.RecordingState.Ready) {
            skeleton?.markLoaded(R.id.metricRecordingsSkeleton)
            skeleton?.markLoaded(R.id.metricStorageSkeleton)
        }
        when (recordingState) {
            DashboardUiState.RecordingState.Loading -> {
                metricRecordingsValue.setText(R.string.dashboard_metric_value_pending)
                metricStorageValue.setText(R.string.dashboard_metric_value_pending)
                recordingStorageProgress.visibility = View.INVISIBLE
            }
            DashboardUiState.RecordingState.Unavailable -> {
                metricRecordingsValue.setText(R.string.dashboard_metric_value_pending)
                metricStorageValue.setText(R.string.dashboard_modern_recordings_unavailable)
                recordingStorageProgress.visibility = View.INVISIBLE
            }
            is DashboardUiState.RecordingState.Ready -> {
                val count = recordingState.todayClipCount
                metricRecordingsValue.text = when {
                    count == null -> getString(R.string.dashboard_metric_value_pending)
                    recordingViewModel.isRecording.value == true ->
                        getString(R.string.dashboard_recordings_value_live, count)
                    else -> getString(R.string.dashboard_modern_clips_today, count)
                }

                val storage = recordingState.storage
                if (storage == null) {
                    metricStorageValue.setText(R.string.dashboard_modern_storage_unavailable)
                    recordingStorageProgress.visibility = View.INVISIBLE
                } else {
                    val ctx = context ?: return
                    metricStorageValue.text = getString(
                        R.string.dashboard_modern_storage_summary,
                        Formatter.formatShortFileSize(ctx, storage.usedBytes),
                        Formatter.formatShortFileSize(ctx, storage.availableBytes),
                    )
                    recordingStorageProgress.progress = storage.usagePercent
                    recordingStorageProgress.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun renderVehicleState(vehicleState: DashboardUiState.VehicleState) {
        if (!::vehicleSocValue.isInitialized) return
        if (vehicleState !is DashboardUiState.VehicleState.Loading) {
            skeleton?.markLoaded(R.id.vehicleSocSkeleton)
            skeleton?.markLoaded(R.id.vehicleRangeSkeleton)
        }
        when (vehicleState) {
            DashboardUiState.VehicleState.Loading -> {
                heroGreeting.setText(R.string.dashboard_modern_vehicle_status)
                heroSubtitle.setText(R.string.dashboard_modern_updating)
                vehicleSocValue.setText(R.string.dashboard_metric_value_pending)
                vehicleRangeValue.setText(R.string.dashboard_metric_value_pending)
                vehicleRangeLabel?.setText(R.string.dashboard_modern_range)
                setHalRangeColumnVisible(false)
                chargingCard.visibility = View.GONE
                renderSocGauge(null)
                renderRangeBreakdown(null)
            }
            is DashboardUiState.VehicleState.Unavailable -> {
                heroGreeting.setText(R.string.dashboard_modern_vehicle_status)
                heroSubtitle.setText(R.string.dashboard_modern_vehicle_unavailable)
                vehicleSocValue.setText(R.string.dashboard_metric_value_pending)
                vehicleRangeValue.setText(R.string.dashboard_metric_value_pending)
                vehicleRangeLabel?.setText(R.string.dashboard_modern_range)
                setHalRangeColumnVisible(false)
                chargingCard.visibility = View.GONE
                renderSocGauge(null)
                renderRangeBreakdown(null)
            }
            is DashboardUiState.VehicleState.Ready -> {
                val snapshot = vehicleState.snapshot
                heroGreeting.setText(R.string.dashboard_modern_vehicle_status)
                heroSubtitle.text = when {
                    snapshot.charging?.fault == true ->
                        getString(R.string.dashboard_modern_charge_fault)
                    snapshot.charging?.full == true ->
                        getString(R.string.dashboard_modern_charge_complete)
                    snapshot.charging?.charging == true ->
                        getString(R.string.dashboard_modern_charging)
                    else -> getString(R.string.dashboard_modern_vehicle_connected)
                }
                vehicleSocValue.text = snapshot.socPercent?.let {
                    getString(R.string.dashboard_modern_percent, it)
                } ?: getString(R.string.dashboard_metric_value_pending)

                val personalized = snapshot.rangeDetails?.personalized
                vehicleRangeValue.text = (personalized ?: snapshot.range)?.let {
                    getString(R.string.dashboard_modern_distance, it.value, it.unit.label)
                } ?: getString(R.string.dashboard_metric_value_pending)
                vehicleRangeLabel?.setText(
                    if (personalized != null) {
                        R.string.dashboard_modern_personalized_range
                    } else {
                        R.string.dashboard_modern_range
                    }
                )
                val halRange = snapshot.range
                val showHalColumn = personalized != null && halRange != null
                if (showHalColumn && halRange != null) {
                    vehicleHalRangeValue?.text = getString(
                        R.string.dashboard_modern_distance, halRange.value, halRange.unit.label
                    )
                }
                setHalRangeColumnVisible(showHalColumn)
                renderSocGauge(snapshot.socPercent)
                renderRangeBreakdown(snapshot.rangeDetails)
                renderCharging(snapshot.charging)
            }
        }
    }

    private fun setHalRangeColumnVisible(visible: Boolean) {
        vehicleHalRangeColumn?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun renderRangeBreakdown(
        details: com.overdrive.app.ui.dashboard.DashboardRangeDetails?,
    ) {
        val view = heroRangeBreakdown ?: return
        if (details == null || !details.isPhev ||
            (details.evLeg == null && details.fuelLeg == null)
        ) {
            view.visibility = View.GONE
            return
        }
        val parts = mutableListOf<String>()
        details.evLeg?.let {
            parts += getString(
                R.string.dashboard_modern_breakdown_ev, it.value, it.unit.label
            )
        }
        details.fuelLeg?.let { leg ->
            parts += details.fuelPercent?.let { pct ->
                getString(
                    R.string.dashboard_modern_breakdown_fuel_pct,
                    leg.value, leg.unit.label, pct,
                )
            } ?: getString(
                R.string.dashboard_modern_breakdown_fuel, leg.value, leg.unit.label
            )
        }
        view.text = parts.joinToString(separator = " · ")
        view.visibility = View.VISIBLE
    }

    private fun renderSocGauge(socPercent: Double?) {
        val gauge = heroSocProgress ?: return
        gauge.visibility = View.VISIBLE
        if (socPercent == null) {
            gauge.setProgressCompat(0, false)
            return
        }
        val clamped = socPercent.coerceIn(0.0, 100.0).toInt()
        gauge.setProgressCompat(clamped, true)
        val colorAttr = if (clamped <= LOW_SOC_THRESHOLD_PERCENT) {
            androidx.appcompat.R.attr.colorError
        } else {
            androidx.appcompat.R.attr.colorPrimary
        }
        gauge.setIndicatorColor(
            com.google.android.material.color.MaterialColors.getColor(gauge, colorAttr)
        )
    }

    private fun renderCharging(charging: com.overdrive.app.ui.dashboard.DashboardChargingSnapshot?) {
        if (charging == null) {
            chargingCard.visibility = View.GONE
            return
        }
        chargingCard.visibility = View.VISIBLE
        chargingStateValue.text = when {
            charging.fault -> getString(R.string.dashboard_modern_charge_fault)
            charging.full -> getString(R.string.dashboard_modern_charge_complete)
            charging.charging -> getString(R.string.dashboard_modern_charging)
            else -> charging.stateName ?: getString(R.string.dashboard_modern_plugged_in)
        }
        renderOptionalMetric(
            chargingPowerGroup,
            chargingPowerValue,
            charging.powerKw?.let {
                getString(
                    if (charging.powerEstimated) {
                        R.string.dashboard_modern_charge_power_estimated
                    } else {
                        R.string.dashboard_modern_charge_power
                    },
                    it,
                )
            },
        )
        renderOptionalMetric(
            chargingEtaGroup,
            chargingEtaValue,
            charging.timeToFullMinutes?.let(::formatChargeDuration),
        )
        renderOptionalMetric(
            chargingSessionGroup,
            chargingSessionValue,
            charging.sessionKwh?.let {
                val rendered = getString(R.string.dashboard_modern_charge_session, it)
                if (charging.sessionEnergyEstimated || charging.sessionEnergyIncomplete) {
                    "~$rendered"
                } else {
                    rendered
                }
            },
        )
        normalizeChargingMetricMargins()
    }

    private fun renderOptionalMetric(group: View, valueView: TextView, value: String?) {
        group.visibility = if (value == null) View.GONE else View.VISIBLE
        if (value != null) valueView.text = value
    }

    private fun normalizeChargingMetricMargins() {
        val gap = resources.getDimensionPixelSize(R.dimen.dashboard_modern_metric_gap)
        var visibleIndex = 0
        listOf(chargingPowerGroup, chargingEtaGroup, chargingSessionGroup).forEach { group ->
            if (group.visibility != View.VISIBLE) return@forEach
            val params = group.layoutParams as? ViewGroup.MarginLayoutParams ?: return@forEach
            val margin = if (visibleIndex == 0) 0 else gap
            if (params.marginStart != margin) {
                params.marginStart = margin
                group.layoutParams = params
            }
            visibleIndex += 1
        }
    }

    private fun formatChargeDuration(minutes: Int): String {
        val hours = minutes / 60
        val remaining = minutes % 60
        return if (hours == 0) {
            getString(R.string.dashboard_modern_minutes, minutes)
        } else {
            getString(R.string.dashboard_modern_hours_minutes, hours, remaining)
        }
    }

    private enum class CoreHealth { UNKNOWN, OK, ALERT }

    private fun computeCoreHealth(states: Map<DaemonType, DaemonState>?): CoreHealth {
        if (states.isNullOrEmpty()) return CoreHealth.UNKNOWN
        val core = setOf(
            DaemonType.CAMERA_DAEMON,
            DaemonType.SENTRY_DAEMON,
            DaemonType.ACC_SENTRY_DAEMON
        )
        var sawCore = false
        for ((type, state) in states) {
            if (type !in core) continue
            sawCore = true
            if (state.status == DaemonStatus.STOPPED || state.status == DaemonStatus.ERROR) {
                return CoreHealth.ALERT
            }
        }
        return if (sawCore) CoreHealth.OK else CoreHealth.UNKNOWN
    }

    private fun renderActivityState(activityState: DashboardUiState.ActivityState) {
        if (activityRows.isEmpty()) return
        if (activityState == DashboardUiState.ActivityState.Loading &&
            skeleton?.isLoaded(R.id.activityRow1Skeleton) != true
        ) {
            return
        }
        skeleton?.markLoaded(R.id.activityRow1Skeleton)
        val rows = when (activityState) {
            DashboardUiState.ActivityState.Loading -> listOf(
                DashboardUiState.ActivityRow(getString(R.string.dashboard_modern_activity_loading))
            )
            DashboardUiState.ActivityState.Unavailable -> listOf(
                DashboardUiState.ActivityRow(getString(R.string.dashboard_modern_activity_unavailable))
            )
            is DashboardUiState.ActivityState.Ready -> activityState.rows.ifEmpty {
                listOf(
                    DashboardUiState.ActivityRow(getString(R.string.dashboard_modern_no_activity))
                )
            }
        }
        activityRows.forEachIndexed { index, views ->
            val row = rows.getOrNull(index)
            views.container.visibility = if (row == null) View.GONE else View.VISIBLE
            if (row == null) return@forEachIndexed
            views.text.text = row.text
            val placeholder = row.icon == 0
            val textLp = views.text.layoutParams as ViewGroup.MarginLayoutParams
            if (placeholder) {
                views.icon.visibility = View.GONE
                textLp.marginStart = 0
            } else {
                views.icon.setImageResource(row.icon)
                views.icon.visibility = View.VISIBLE
                textLp.marginStart = views.text.resources.getDimensionPixelSize(R.dimen.dashboard_modern_gap)
            }
            views.text.layoutParams = textLp
            val textAttr = if (placeholder) {
                com.google.android.material.R.attr.colorOnSurfaceVariant
            } else {
                com.google.android.material.R.attr.colorOnSurface
            }
            views.text.setTextColor(
                com.google.android.material.color.MaterialColors.getColor(views.text, textAttr)
            )
        }
    }

    private fun updateTunnelTile() {
        val states = daemonsViewModel.daemonStates.value
        val display = com.overdrive.app.ui.model.TunnelDisplayPolicy.resolve(
            daemonsViewModel.zrokController.tunnelUrl.value,
            daemonsViewModel.cloudflaredController.tunnelUrl.value,
            daemonsViewModel.tailscaleController.tunnelUrl.value,
            states?.get(DaemonType.ZROK_TUNNEL)?.status,
            states?.get(DaemonType.CLOUDFLARED_TUNNEL)?.status,
            states?.get(DaemonType.TAILSCALE_TUNNEL)?.status,
        )

        when (display.kind) {
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.ONLINE -> {
                metricTunnelValue.text = getString(R.string.dashboard_tunnel_online)
                tunnelStateDot.setBackgroundResource(R.drawable.status_dot_online)
            }
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.STARTING_ZROK,
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.STARTING_CLOUDFLARED,
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.STARTING_TAILSCALE,
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.WAITING_FOR_URL -> {
                metricTunnelValue.text = getString(R.string.dashboard_tunnel_connecting)
                tunnelStateDot.setBackgroundResource(R.drawable.status_dot_starting)
            }
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.STOPPING -> {
                metricTunnelValue.text = getString(R.string.dashboard_tunnel_tile_stopping)
                tunnelStateDot.setBackgroundResource(R.drawable.status_dot_starting)
            }
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.FAILED -> {
                metricTunnelValue.text = getString(R.string.dashboard_tunnel_tile_failed)
                tunnelStateDot.setBackgroundResource(R.drawable.status_dot_offline)
            }
            com.overdrive.app.ui.model.TunnelDisplayPolicy.Kind.HIDDEN -> {
                metricTunnelValue.text = getString(R.string.dashboard_tunnel_offline)
                tunnelStateDot.setBackgroundResource(R.drawable.status_dot_offline)
            }
        }
    }

    private fun rebuildTunnelChips() {
        val available = collectAvailableTunnels()

        if (available.isEmpty()) {
            chipGroupTunnels.removeAllViews()
            chipGroupTunnels.visibility = View.GONE
            selectedTunnel = null
            renderQr(null)
            return
        }

        val newSelection = selectedTunnel?.takeIf { prev -> available.any { it.first == prev } }
            ?: available.first().first
        selectedTunnel = newSelection

        val currentTags = (0 until chipGroupTunnels.childCount)
            .map { (chipGroupTunnels.getChildAt(it) as Chip).tag as DaemonType }
        val newTags = available.map { it.first }
        if (currentTags != newTags) {
            chipGroupTunnels.setOnCheckedStateChangeListener(null)
            chipGroupTunnels.removeAllViews()
            available.forEach { (type, _) ->
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    tag = type
                    text = labelFor(type)
                    isCheckable = true
                    isCheckedIconVisible = false
                }
                chipGroupTunnels.addView(chip)
            }
            chipGroupTunnels.setOnCheckedStateChangeListener { group, ids ->
                val checkedId = ids.firstOrNull() ?: return@setOnCheckedStateChangeListener
                val chip = group.findViewById<Chip>(checkedId) ?: return@setOnCheckedStateChangeListener
                val type = chip.tag as? DaemonType ?: return@setOnCheckedStateChangeListener
                if (type != selectedTunnel) {
                    selectedTunnel = type
                    renderQr(urlFor(type))
                }
            }
        }

        for (i in 0 until chipGroupTunnels.childCount) {
            val chip = chipGroupTunnels.getChildAt(i) as Chip
            chip.isChecked = (chip.tag as DaemonType) == newSelection
        }

        chipGroupTunnels.visibility = if (available.size > 1) View.VISIBLE else View.GONE
        renderQr(urlFor(newSelection))
    }

    private fun collectAvailableTunnels(): List<Pair<DaemonType, String>> {
        val list = mutableListOf<Pair<DaemonType, String>>()
        val states = daemonsViewModel.daemonStates.value
        daemonsViewModel.cloudflaredController.tunnelUrl.value
            ?.takeIf {
                com.overdrive.app.ui.model.TunnelDisplayPolicy.isActiveUrl(
                    it, states?.get(DaemonType.CLOUDFLARED_TUNNEL)?.status)
            }
            ?.let { list.add(DaemonType.CLOUDFLARED_TUNNEL to it) }
        daemonsViewModel.zrokController.tunnelUrl.value
            ?.takeIf {
                com.overdrive.app.ui.model.TunnelDisplayPolicy.isActiveUrl(
                    it, states?.get(DaemonType.ZROK_TUNNEL)?.status)
            }
            ?.let { list.add(DaemonType.ZROK_TUNNEL to it) }
        daemonsViewModel.tailscaleController.tunnelUrl.value
            ?.takeIf {
                com.overdrive.app.ui.model.TunnelDisplayPolicy.isActiveUrl(
                    it, states?.get(DaemonType.TAILSCALE_TUNNEL)?.status)
            }
            ?.let { list.add(DaemonType.TAILSCALE_TUNNEL to it) }
        return list
    }

    private fun urlFor(type: DaemonType): String? = when (type) {
        DaemonType.CLOUDFLARED_TUNNEL -> daemonsViewModel.cloudflaredController.tunnelUrl.value
        DaemonType.ZROK_TUNNEL -> daemonsViewModel.zrokController.tunnelUrl.value
        DaemonType.TAILSCALE_TUNNEL -> daemonsViewModel.tailscaleController.tunnelUrl.value
        else -> null
    }

    private fun labelFor(type: DaemonType): String = when (type) {
        DaemonType.CLOUDFLARED_TUNNEL -> getString(R.string.tunnel_label_cloudflared)
        DaemonType.ZROK_TUNNEL -> getString(R.string.tunnel_label_zrok)
        DaemonType.TAILSCALE_TUNNEL -> getString(R.string.tunnel_label_tailscale)
        else -> type.localizedName(requireContext())
    }

    private fun renderQr(url: String?) {
        if (!cockpitViewModel.uiState.value.remoteExpanded) return

        if (url.isNullOrEmpty()) {
            showPlaceholder()
            lastRenderedQrUrl = null
            hasRenderedQrForView = false
            return
        }
        if (hasRenderedQrForView && url == lastRenderedQrUrl) return
        try {
            val qrBitmap = QrCodeGenerator.generate(url, 400)
            if (qrBitmap != null) {
                ivQrCode.setImageBitmap(qrBitmap)
                qrContainer.visibility = View.VISIBLE
                ivQrCode.visibility = View.VISIBLE
                tvQrPlaceholder.visibility = View.GONE
                tvUrl.text = url
                tvUrl.visibility = View.VISIBLE
                lastRenderedQrUrl = url
                hasRenderedQrForView = true
            } else {
                showPlaceholder()
            }
        } catch (_: Exception) {
            showPlaceholder()
        }
    }

    private fun showPlaceholder() {
        ivQrCode.setImageDrawable(null)
        ivQrCode.visibility = View.GONE
        qrContainer.visibility = View.GONE
        tvQrPlaceholder.visibility = View.VISIBLE
        tvQrPlaceholder.text = getTunnelPlaceholderText()
        tvUrl.visibility = View.GONE
    }

    private fun renderRemoteExpansion(expanded: Boolean) {
        if (!::remoteDetails.isInitialized) return
        remoteDetails.visibility = if (expanded) View.VISIBLE else View.GONE
        btnExpandRemote.rotation = if (expanded) 180f else 0f
        metricTunnel.contentDescription = getString(
            if (expanded) {
                R.string.dashboard_modern_collapse_remote
            } else {
                R.string.dashboard_modern_expand_remote
            }
        )
        if (expanded) {
            renderQr(selectedTunnel?.let(::urlFor))
        }
    }

    private fun getTunnelPlaceholderText(): String {
        val states = daemonsViewModel.daemonStates.value ?: return getString(R.string.dashboard_no_tunnel)
        val cfState = states[DaemonType.CLOUDFLARED_TUNNEL]
        val zrokState = states[DaemonType.ZROK_TUNNEL]
        val tailscaleState = states[DaemonType.TAILSCALE_TUNNEL]
        return when {
            zrokState?.status == DaemonStatus.STARTING -> getString(R.string.dashboard_starting_zrok)
            cfState?.status == DaemonStatus.STARTING -> getString(R.string.dashboard_starting_cloudflared)
            tailscaleState?.status == DaemonStatus.STARTING -> getString(R.string.dashboard_starting_tailscale)
            zrokState?.status == DaemonStatus.RUNNING -> getString(R.string.dashboard_waiting_url)
            cfState?.status == DaemonStatus.RUNNING -> getString(R.string.dashboard_waiting_url)
            tailscaleState?.status == DaemonStatus.RUNNING -> getString(R.string.dashboard_waiting_url)
            else -> getString(R.string.dashboard_no_tunnel)
        }
    }

    private fun loadAuthState() {
        try {
            val state = AuthManager.getState() ?: AuthManager.initialize()
            if (state != null) {
                updateTokenDisplay(state.secret)
            } else {
                tvDeviceToken.text = getString(R.string.dashboard_token_masked)
            }
        } catch (_: Exception) {
            tvDeviceToken.text = getString(R.string.dashboard_token_masked)
        }
    }

    private fun updateTokenDisplay(secret: String) {
        tvDeviceToken.text = if (isTokenVisible) secret else getString(R.string.dashboard_token_masked)
    }

    private fun toggleTokenVisibility() {
        isTokenVisible = !isTokenVisible
        AuthManager.getState()?.let { updateTokenDisplay(it.secret) }
        btnToggleToken.setImageResource(
            if (isTokenVisible) android.R.drawable.ic_menu_close_clear_cancel
            else android.R.drawable.ic_menu_view
        )
    }

    private fun copyTokenToClipboard() {
        val state = AuthManager.getState() ?: return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.clip_label_access_code), state.secret)
        clipboard.setPrimaryClip(clip)
        appToast?.show(getString(R.string.toast_access_code_copied), AppToast.Kind.SUCCESS)
    }

    private fun showRegenerateConfirmation() {
        MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Overdrive_M3_Dialog)
            .setIcon(R.drawable.ic_warning)
            .setTitle(getString(R.string.dialog_regenerate_token_title))
            .setMessage(getString(R.string.dialog_regenerate_token_message))
            .setPositiveButton(getString(R.string.dialog_regenerate)) { _, _ -> regenerateToken() }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun regenerateToken() {
        val newToken = AuthManager.regenerateToken()
        val ctx = context?.applicationContext ?: return
        if (newToken == null) {
            appToast?.show(ctx.getString(R.string.toast_token_regenerated_restart), AppToast.Kind.WARNING)
            loadAuthState()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val outcome = try {
                val client = CameraDaemonClient()
                if (client.connect()) {
                    val ok = client.invalidateAuthCacheSync()
                    client.disconnect()
                    if (ok) R.string.toast_token_regenerated_logged_out to AppToast.Kind.SUCCESS
                    else R.string.toast_token_regenerated_restart to AppToast.Kind.WARNING
                } else {
                    R.string.toast_token_regenerated_no_notify to AppToast.Kind.WARNING
                }
            } catch (_: Exception) {
                R.string.toast_token_regenerated to AppToast.Kind.SUCCESS
            }
            withContext(Dispatchers.Main) {
                if (isAdded) appToast?.show(ctx.getString(outcome.first), outcome.second)
            }
        }
        loadAuthState()
    }

    private fun refreshVehicleTile() {
        if (metricVehicleValue == null) return
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var nominalKwh = 0.0
            var modelId: String? = null
            try {
                val conn = DaemonHttpClient.open("/api/performance/soh/nominal", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    if (!json.isNull("nominalKwh")) {
                        nominalKwh = json.optDouble("nominalKwh", 0.0)
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            try {
                val conn = DaemonHttpClient.open("/api/models/selected", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val m = when {
                        json.has("selectedModelId") && !json.isNull("selectedModelId") ->
                            json.optString("selectedModelId", "")
                        json.has("modelSource") && json.optString("modelSource", "unset") == "unset" -> ""
                        else -> json.optString("modelId", "")
                    }
                    if (m.isNotEmpty() && !m.equals("null", ignoreCase = true)) modelId = m
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            withContext(Dispatchers.Main) {
                if (!isAdded || view == null) return@withContext
                val tile = metricVehicleValue ?: return@withContext
                if (nominalKwh > 0) {
                    tile.text = if (modelId != null) {
                        getString(R.string.dashboard_vehicle_summary, nominalKwh, modelDisplayName(modelId))
                    } else {
                        String.format("%.1f kWh", nominalKwh)
                    }
                } else {
                    tile.text = getString(R.string.dashboard_vehicle_tap_to_set)
                }
                vehicleArt?.setImageResource(VehicleArt.drawableFor(modelId))
            }
        }
    }

    private fun modelDisplayName(modelId: String?): String {
        val normalized = modelId?.lowercase(java.util.Locale.US)?.filter(Char::isLetterOrDigit)
        if (normalized.isNullOrEmpty() || normalized == "null") return "—"
        return when (normalized) {
            "seal" -> "BYD Seal"
            "sealion7" -> "BYD Sealion 7"
            "sealion6" -> "BYD Sealion 6"
            "shark" -> "BYD Shark"
            "sealu" -> "BYD Seal U"
            "sealudmi" -> "BYD Seal U DM-i"
            "dolphin" -> "BYD Dolphin"
            "atto3" -> "BYD Atto 3"
            "atto3evo" -> "BYD Atto 3 Evo"
            "atto2" -> "BYD Atto 2"
            "atto1" -> "BYD Atto 1"
            "han" -> "BYD Han"
            "tang" -> "BYD Tang"
            "song" -> "BYD Song"
            "qin" -> "BYD Qin"
            "m6" -> "BYD M6"
            "seagull" -> getString(R.string.vehicle_model_seagull)
            "destroyer", "destroyer05" -> "BYD Destroyer 05"
            else -> modelId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
        }
    }

    fun showVehicleCapacityDialog(onFinished: (() -> Unit)? = null): Boolean {
        val ctx = context ?: return false
        val dialogView = layoutInflater.inflate(R.layout.dialog_vehicle_capacity, null, false)

        val summaryDetection = dialogView.findViewById<TextView>(R.id.vehicleSummaryDetection)
        val summaryCapacity = dialogView.findViewById<TextView>(R.id.vehicleSummaryCapacity)
        val summarySoh = dialogView.findViewById<TextView>(R.id.vehicleSummarySoh)
        val summaryModel = dialogView.findViewById<TextView>(R.id.vehicleSummaryModel)
        val capInput = dialogView.findViewById<
            com.google.android.material.textfield.TextInputEditText>(R.id.vehicleCapacityInput)
        val capLayout = dialogView.findViewById<
            com.google.android.material.textfield.TextInputLayout>(R.id.vehicleCapacityLayout)
        val modelDropdown = dialogView.findViewById<
            com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.vehicleModelDropdown)
        val resetButton = dialogView.findViewById<
            MaterialButton>(R.id.vehicleResetAuto)

        var resetEligible = false
        var saveInFlight = false
        resetButton.isEnabled = false

        capInput.doAfterTextChanged { capLayout.error = null }

        val pendingValue = getString(R.string.vehicle_dialog_value_pending)
        summaryDetection.text = getString(R.string.vehicle_dialog_detection, pendingValue)
        summaryCapacity.text = pendingValue
        summarySoh.text = pendingValue
        summaryModel.text = pendingValue

        data class ModelEntry(val id: String, val title: String, val nominalKwh: Double)
        val modelEntries = mutableListOf<ModelEntry>()
        var selectedModelId: String? = null
        var modelSelectionChanged = false
        var initialModelId: String? = null
        modelDropdown.setOnItemClickListener { _, _, position, _ ->
            if (position in modelEntries.indices) {
                val entry = modelEntries[position]
                selectedModelId = entry.id
                modelSelectionChanged = true
                if (entry.nominalKwh > 0) {
                    capInput.setText(String.format("%.1f", entry.nominalKwh))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var initialKwh = 0.0
            val loadedModelEntries = mutableListOf<ModelEntry>()
            var nominalKwh = 0.0
            var nominalSource = "unset"
            var displaySoh = -1.0
            var displaySource = "unavailable"
            var statusModelId: String? = null

            try {
                val conn = DaemonHttpClient.open("/api/performance/soh/nominal", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    if (!json.isNull("nominalKwh")) initialKwh = json.optDouble("nominalKwh", 0.0)
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            try {
                val conn = DaemonHttpClient.open("/api/performance/soh", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    nominalKwh = json.optDouble("nominalCapacityKwh", 0.0)
                    nominalSource = json.optString("nominalSource", "unset")
                    displaySoh = json.optDouble("displaySoh", -1.0)
                    displaySource = json.optString("displaySource", "unavailable")
                    if (!json.isNull("modelId")) {
                        val mid = json.optString("modelId", "").ifEmpty { null }
                        if (mid != null && !mid.equals("null", ignoreCase = true)) {
                            statusModelId = mid
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            try {
                val conn = DaemonHttpClient.open("/api/models/manifest", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val models = json.optJSONArray("models")
                    if (models != null) {
                        for (i in 0 until models.length()) {
                            val m = models.optJSONObject(i) ?: continue
                            val id = m.optString("id", "")
                            val canonicalTitle = when {
                                m.optString("name", "").isNotEmpty() -> m.optString("name")
                                m.optString("title", "").isNotEmpty() -> m.optString("title")
                                else -> id
                            }
                            val title = if (id.equals("seagull", ignoreCase = true)) {
                                ctx.getString(R.string.vehicle_model_seagull)
                            } else {
                                canonicalTitle
                            }
                            val kwh = m.optDouble("nominalKwh", 0.0)
                            if (id.isNotEmpty()) loadedModelEntries.add(ModelEntry(id, title, kwh))
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            try {
                val conn = DaemonHttpClient.open("/api/models/selected", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val m = when {
                        json.has("selectedModelId") && !json.isNull("selectedModelId") ->
                            json.optString("selectedModelId", "")
                        json.has("modelSource") && json.optString("modelSource", "unset") == "unset" -> ""
                        else -> json.optString("modelId", "")
                    }
                    if (m.isNotEmpty() && !m.equals("null", ignoreCase = true)) initialModelId = m
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            withContext(Dispatchers.Main) {
                if (!isAdded || view == null) return@withContext
                modelEntries.clear()
                modelEntries.addAll(loadedModelEntries)
                val titles = modelEntries.map { it.title }.toTypedArray()
                val adapter = android.widget.ArrayAdapter(
                    ctx,
                    com.google.android.material.R.layout.m3_auto_complete_simple_item,
                    titles
                )
                modelDropdown.setAdapter(adapter)

                val activeModelId = initialModelId ?: statusModelId
                if (activeModelId != null) {
                    val activeEntry = modelEntries.firstOrNull { it.id.equals(activeModelId, ignoreCase = true) }
                    if (activeEntry != null) {
                        modelDropdown.setText(activeEntry.title, false)
                        selectedModelId = activeEntry.id
                    }
                }

                if (initialKwh > 0) {
                    capInput.setText(String.format("%.1f", initialKwh))
                }

                val manual = nominalSource == "user" || initialModelId != null || initialKwh > 0
                summaryDetection.text = getString(
                    R.string.vehicle_dialog_detection,
                    getString(
                        if (manual) {
                            R.string.vehicle_dialog_detection_manual
                        } else {
                            R.string.vehicle_dialog_detection_auto
                        }
                    )
                )
                resetEligible = manual
                resetButton.isEnabled = manual && !saveInFlight

                summaryCapacity.text = if (nominalKwh > 0) String.format("%.1f kWh", nominalKwh) else getString(R.string.soh_dialog_capacity_not_detected)
                summarySoh.text = if (displaySoh > 0) String.format("%.1f%%", displaySoh) else getString(R.string.vehicle_dialog_soh_unavailable).replaceFirstChar { it.uppercase() }
                summaryModel.text = if (activeModelId != null) modelDisplayName(activeModelId) else getString(R.string.soh_dialog_model_not_selected)
            }
        }

        val dialog = MaterialAlertDialogBuilder(ctx, R.style.Theme_Overdrive_M3_Dialog)
            .setTitle(getString(R.string.vehicle_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.vehicle_dialog_save), null)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .create()

        resetButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                postNominal(null, clearModelSelection = true)
                withContext(Dispatchers.Main) {
                    refreshVehicleTile()
                    cockpitViewModel.refreshVehicleStatus(showLoading = false)
                    dialog.dismiss()
                }
            }
        }

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE)
            fun setSaving(saving: Boolean) {
                saveInFlight = saving
                saveButton.isEnabled = !saving
                resetButton.isEnabled = !saving && resetEligible
                cancelButton.isEnabled = !saving
                dialog.setCancelable(!saving)
                dialog.setCanceledOnTouchOutside(!saving)
            }
            saveButton.setOnClickListener {
                val raw = capInput.text?.toString()?.trim().orEmpty()
                val kwh = raw.toDoubleOrNull()
                if (kwh == null || !kwh.isFinite() || kwh < 5.0 || kwh > 120.0) {
                    capLayout.error = getString(R.string.vehicle_dialog_invalid_capacity)
                    return@setOnClickListener
                }
                setSaving(true)
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val modelToSave = if (modelSelectionChanged) selectedModelId else initialModelId
                    val error = postNominalAndModel(kwh, modelToSave)
                    withContext(Dispatchers.Main) {
                        if (error == null) {
                            refreshVehicleTile()
                            cockpitViewModel.refreshVehicleStatus(showLoading = false)
                            dialog.dismiss()
                        } else {
                            setSaving(false)
                            appToast?.show(
                                getString(R.string.toast_failed_with_message, error),
                                AppToast.Kind.ERROR,
                            )
                        }
                    }
                }
            }
            cancelButton.setOnClickListener { dialog.dismiss() }
        }

        if (onFinished != null) {
            dialog.setOnDismissListener { onFinished() }
        }
        dialog.show()
        return true
    }

    private suspend fun postNominal(kwh: Double?, clearModelSelection: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val conn = DaemonHttpClient.open("/api/performance/soh/nominal", "POST", 3000, 5000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val body = if (kwh == null) "{\"nominalKwh\":null}" else "{\"nominalKwh\":$kwh}"
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Throwable) {}

        if (clearModelSelection) {
            try {
                val conn = DaemonHttpClient.open("/api/models/selected", "POST", 3000, 5000)
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use {
                    it.write("{\"clearModelSelection\":true}".toByteArray())
                }
                conn.responseCode
                conn.disconnect()
            } catch (_: Throwable) {}
        }
    }

    private suspend fun postNominalAndModel(kwh: Double, modelId: String?): String? = withContext(Dispatchers.IO) {
        if (!modelId.isNullOrEmpty()) {
            postJsonResult(
                "/api/models/selected",
                JSONObject().put("modelId", modelId).put("nominalKwh", kwh),
                "ok",
            )
        } else {
            postJsonResult(
                "/api/performance/soh/nominal",
                JSONObject().put("nominalKwh", kwh),
                "success",
            )
        }
    }

    private fun postJsonResult(path: String, body: JSONObject, successKey: String): String? {
        var conn: java.net.HttpURLConnection? = null
        return try {
            conn = DaemonHttpClient.open(path, "POST", 3000, 5000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val response = responseBody.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
            if (status in 200..299 && response?.optBoolean(successKey, false) == true) {
                null
            } else {
                response?.optString("error", "")?.takeIf { it.isNotBlank() } ?: "HTTP $status"
            }
        } catch (t: Throwable) {
            t.message?.takeIf { it.isNotBlank() } ?: "Network error"
        } finally {
            conn?.disconnect()
        }
    }

    private fun rebuildInsightsAsync() {
        val provider = insightsProvider ?: return
        val visitCount = if (firstVisitCount >= 0) firstVisitCount else 0
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val built = try {
                provider.build(visitCount)
            } catch (_: Throwable) {
                null
            }
            val aiInsight = try {
                provider.latestAiInsight()
            } catch (_: Throwable) {
                null
            }
            withContext(Dispatchers.Main) {
                if (!isAdded || view == null) return@withContext
                val rows = built
                    ?.asSequence()
                    ?.filter { it.priority < WELCOME_INSIGHT_PRIORITY }
                    ?.map { DashboardUiState.ActivityRow(it.text, it.icon) }
                    ?.toList()
                cockpitViewModel.setActivityRows(rows)
                renderAiInsight(aiInsight)
            }
        }
    }

    private fun renderAiInsight(insight: DashboardAiInsight?) {
        if (!::aiInsightCard.isInitialized) return
        if (insight == null) {
            aiInsightExpanded = false
            aiInsightCard.visibility = View.GONE
            return
        }
        aiInsightTitle.text = insight.title
        aiInsightText.text = insight.text
        val relative = DateUtils.getRelativeTimeSpanString(
            insight.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
        aiInsightMeta.text = listOf(relative, insight.model)
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
        renderAiInsightExpansion()
        aiInsightCard.visibility = View.VISIBLE
    }

    private fun renderAiInsightExpansion() {
        if (!::aiInsightCard.isInitialized) return
        aiInsightText.maxLines =
            if (aiInsightExpanded) Int.MAX_VALUE else AI_INSIGHT_PREVIEW_LINES
        aiInsightText.ellipsize =
            if (aiInsightExpanded) null else TextUtils.TruncateAt.END
        aiInsightExpand.rotation = if (aiInsightExpanded) 180f else 0f
        aiInsightCard.contentDescription = getString(
            if (aiInsightExpanded) {
                R.string.dashboard_ai_insight_collapse
            } else {
                R.string.dashboard_ai_insight_open
            }
        )
    }
}
