package com.overdrive.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.overdrive.app.R
import com.overdrive.app.auth.AuthManager
import com.overdrive.app.config.UnifiedConfigManager
import com.overdrive.app.config.VehicleModelSelection
import com.overdrive.app.ui.component.OverdriveComposeContainer
import com.overdrive.app.ui.model.DaemonStatus
import com.overdrive.app.ui.theme.OverdriveTheme
import com.overdrive.app.ui.util.QrCodeGenerator
import com.overdrive.app.ui.vehicle.VehicleArt
import com.overdrive.app.ui.vehicle.VehicleTopDownArt
import com.overdrive.app.ui.viewmodel.DaemonsViewModel
import com.overdrive.app.ui.viewmodel.MainViewModel
import com.overdrive.app.ui.viewmodel.RecordingViewModel
import com.overdrive.app.util.DaemonHttpClient
import com.overdrive.app.util.DeviceIdGenerator
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Modern 100% Jetpack Compose Native implementation of Dashboard Fragment.
 * Backed by identical ViewModel contracts, daemon polling, and vehicle configuration.
 */
class DashboardComposeFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    private val daemonsViewModel: DaemonsViewModel by activityViewModels()
    private val recordingViewModel: RecordingViewModel by activityViewModels()
    private val dashboardViewModel: DashboardViewModel by viewModels()

    private var uiState by mutableStateOf(DashboardUiState())
    private var heroState by mutableStateOf(DashboardHeroState())
    private var remoteState by mutableStateOf(DashboardRemoteState())

    private var metricsExecutor: ExecutorService? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val statusRefreshRunnable = Runnable {
        refreshVehicleStatus()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return OverdriveComposeContainer(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                OverdriveTheme {
                    DashboardScreen(
                        uiState = uiState,
                        heroState = heroState,
                        remoteState = remoteState,
                        onVehicleCardClick = {
                            showVehicleCapacityDialog()
                        },
                        onRecordingsClick = {
                            findNavController().navigate(R.id.recordingsFragment)
                        },
                        onLiveClick = {
                            findNavController().navigate(R.id.liveViewFragment)
                        },
                        onDaemonsClick = {
                            findNavController().navigate(R.id.daemonsFragment)
                        },
                        onTripsClick = {
                            findNavController().navigate(R.id.tripsFragment)
                        },
                        onVehicleControlClick = {
                            findNavController().navigate(R.id.vehicleControlFragment)
                        },
                        onToggleRemoteExpanded = {
                            remoteState = remoteState.copy(isExpanded = !remoteState.isExpanded)
                        },
                        onToggleTokenMask = {
                            remoteState = remoteState.copy(isTokenMasked = !remoteState.isTokenMasked)
                        },
                        onCopyToken = {
                            copyToClipboard(getString(R.string.clip_label_access_code), remoteState.deviceToken)
                            Toast.makeText(requireContext(), R.string.toast_access_code_copied, Toast.LENGTH_SHORT).show()
                        },
                        onCopyUrl = { url ->
                            copyToClipboard(getString(R.string.dashboard_metric_tunnel), url)
                            Toast.makeText(requireContext(), R.string.toast_url_copied_short, Toast.LENGTH_SHORT).show()
                        },
                        onRegenerateToken = {
                            val newToken = AuthManager.regenerateToken() ?: ""
                            remoteState = remoteState.copy(deviceToken = newToken)
                            Toast.makeText(requireContext(), R.string.toast_token_regenerated, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDeviceIdAndAuth()
        observeViewModels()
    }

    override fun onResume() {
        super.onResume()
        refreshVehicleStatus()
        refreshVehicleTile()
        recordingViewModel.updateStorageInfo()
        rebuildInsightsAsync()
    }

    override fun onPause() {
        mainHandler.removeCallbacks(statusRefreshRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        mainHandler.removeCallbacks(statusRefreshRunnable)
        metricsExecutor?.shutdownNow()
        metricsExecutor = null
        super.onDestroyView()
    }

    private fun initDeviceIdAndAuth() {
        val deviceId = DeviceIdGenerator.generateDeviceId(requireContext())
        val token = AuthManager.getState()?.deviceSecret ?: AuthManager.initialize()?.deviceSecret ?: ""
        remoteState = remoteState.copy(
            deviceId = deviceId,
            deviceToken = token
        )
    }

    private fun observeViewModels() {
        // Observe Real-time Reactive Vehicle Telemetry via StateFlow (0ms internal latency)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                dashboardViewModel.vehicleSnapshot.collect { snapshot ->
                    if (snapshot != null) {
                        uiState = DashboardStateReducer.status(
                            uiState,
                            DashboardStatusResult.Available(snapshot)
                        )
                    }
                }
            }
        }

        // Observe Daemons
        daemonsViewModel.daemonStates.observe(viewLifecycleOwner) { states ->
            val running = states.values.count { it.status == DaemonStatus.RUNNING }
            val total = states.size
            val daemonsText = getString(R.string.dashboard_daemons_running, running, total)
            val subText = if (uiState.vehicle !is DashboardUiState.VehicleState.Ready) {
                daemonsText
            } else {
                getString(R.string.dashboard_modern_vehicle_connected)
            }
            heroState = heroState.copy(
                greeting = getString(R.string.dashboard_modern_vehicle_status),
                subtitle = subText,
                daemonsChipText = daemonsText,
                areDaemonsRunning = running > 0
            )
        }

        // Observe Tunnels
        val updateTunnel: (String?) -> Unit = { _ ->
            val activeUrl = daemonsViewModel.zrokController.tunnelUrl.value
                ?: daemonsViewModel.cloudflaredController.tunnelUrl.value
                ?: daemonsViewModel.tailscaleController.tunnelUrl.value

            val isOnline = !activeUrl.isNullOrEmpty()
            val statusText = if (isOnline) activeUrl!! else getString(R.string.dashboard_tunnel_offline)

            val qrBitmap = if (isOnline) {
                QrCodeGenerator.generate(activeUrl!!, 256)
            } else {
                null
            }

            remoteState = remoteState.copy(
                isOnline = isOnline,
                statusText = statusText,
                activeUrl = activeUrl,
                qrBitmap = qrBitmap
            )

            heroState = heroState.copy(
                tunnelChipText = if (isOnline) getString(R.string.dashboard_tunnel_online) else getString(R.string.dashboard_tunnel_offline),
                isTunnelOnline = isOnline
            )
        }

        daemonsViewModel.zrokController.tunnelUrl.observe(viewLifecycleOwner, updateTunnel)
        daemonsViewModel.cloudflaredController.tunnelUrl.observe(viewLifecycleOwner, updateTunnel)
        daemonsViewModel.tailscaleController.tunnelUrl.observe(viewLifecycleOwner, updateTunnel)

        // Observe Recordings
        recordingViewModel.isRecording.observe(viewLifecycleOwner) { isRec ->
            heroState = heroState.copy(
                isRecordingActive = isRec == true,
                recordingChipText = if (isRec == true) {
                    getString(R.string.dashboard_chip_recording_active)
                } else {
                    getString(R.string.dashboard_chip_recording_idle)
                }
            )
        }

        recordingViewModel.storageInfo.observe(viewLifecycleOwner) { info ->
            val summary = info?.takeIf { it.totalBytes > 0L }?.let {
                DashboardUiState.StorageSummary(
                    usedBytes = it.usedBytes.coerceAtLeast(0L),
                    availableBytes = it.availableBytes.coerceAtLeast(0L),
                    totalBytes = it.totalBytes
                )
            }
            uiState = uiState.copy(
                recordings = DashboardUiState.RecordingState.Ready(
                    todayClipCount = null,
                    storage = summary
                )
            )
        }
    }

    private fun refreshVehicleStatus() {
        dashboardViewModel.refresh()
        val executor = metricsExecutor ?: Executors.newSingleThreadExecutor().also { metricsExecutor = it }
        executor.execute {
            var conn: java.net.HttpURLConnection? = null
            val result = try {
                conn = DaemonHttpClient.open("/status", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    DashboardStatusParser.parse(body)
                } else {
                    DashboardStatusResult.Unavailable(DashboardStatusResult.Reason.SERVICE_UNAVAILABLE)
                }
            } catch (_: Throwable) {
                DashboardStatusResult.Unavailable(DashboardStatusResult.Reason.SERVICE_UNAVAILABLE)
            } finally {
                try { conn?.disconnect() } catch (_: Throwable) {}
            }
            mainHandler.post {
                if (!isAdded || view == null) return@post
                uiState = DashboardStateReducer.status(uiState, result)
                if (isResumed) {
                    mainHandler.removeCallbacks(statusRefreshRunnable)
                    mainHandler.postDelayed(statusRefreshRunnable, 5000L)
                }
            }
        }
    }

    internal fun refreshVehicleTile() {
        val executor = metricsExecutor ?: Executors.newSingleThreadExecutor().also { metricsExecutor = it }
        executor.execute {
            var nominalKwh = 0.0
            var modelId: String? = null
            try {
                val conn = DaemonHttpClient.open("/api/performance/soh/nominal", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(body)
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
                    val json = org.json.JSONObject(body)
                    val m = when {
                        json.has("selectedModelId") && !json.isNull("selectedModelId") ->
                            json.optString("selectedModelId", "")
                        json.has("modelSource") && json.optString("modelSource", "unset") == "unset" -> ""
                        else -> json.optString("modelId", "")
                    }
                    if (m.isNotEmpty()) modelId = m
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            if (modelId == null) {
                try {
                    val prefs = context?.getSharedPreferences("overdrive_vehicle", Context.MODE_PRIVATE)
                    val pId = prefs?.getString("selected_model_id", null)
                    if (!pId.isNullOrEmpty()) {
                        modelId = pId
                        if (nominalKwh <= 0.0) nominalKwh = prefs.getFloat("nominal_kwh", 0f).toDouble()
                    }
                } catch (_: Throwable) {}
            }
            if (modelId == null) {
                try {
                    val v = UnifiedConfigManager.getVehicle()
                    val m = v.optString("modelId", "")
                    val src = v.optString("modelSource", "")
                    if (m.isNotEmpty() && src != "unset") {
                        modelId = m
                        if (nominalKwh <= 0.0) nominalKwh = v.optDouble("nominalKwh", 0.0)
                    }
                } catch (_: Throwable) {}
            }

            val finalNominalKwh = nominalKwh
            val finalModelId = modelId

            mainHandler.post {
                if (!isAdded || view == null) return@post
                val vehicleText = if (finalNominalKwh > 0) {
                    if (finalModelId != null) {
                        getString(R.string.dashboard_vehicle_summary, finalNominalKwh, modelDisplayName(finalModelId))
                    } else {
                        String.format("%.1f kWh", finalNominalKwh)
                    }
                } else if (finalModelId != null) {
                    modelDisplayName(finalModelId)
                } else {
                    getString(R.string.dashboard_vehicle_tap_to_set)
                }

                heroState = heroState.copy(
                    vehicleModel = vehicleText,
                    modelId = finalModelId
                )
            }
        }
    }

    private fun rebuildInsightsAsync() {
        val executor = metricsExecutor ?: Executors.newSingleThreadExecutor().also { metricsExecutor = it }
        executor.execute {
            val provider = DashboardInsightProvider(requireContext().applicationContext)
            val built = try {
                provider.build(0)
            } catch (_: Throwable) {
                null
            }
            mainHandler.post {
                if (!isAdded || view == null) return@post
                val rows = built
                    ?.asSequence()
                    ?.filter { it.priority < 100 }
                    ?.map { DashboardUiState.ActivityRow(it.text, it.icon) }
                    ?.toList()
                uiState = DashboardStateReducer.activity(uiState, rows)
            }
        }
    }

    /**
     * Show vehicle configuration dialog for vehicle model and battery pack selection.
     */
    internal fun showVehicleCapacityDialog(onFinished: (() -> Unit)? = null): Boolean {
        val ctx = context ?: return false

        val dialogView = layoutInflater.inflate(R.layout.dialog_vehicle_capacity, null, false)

        val summaryDetection = dialogView.findViewById<TextView>(R.id.vehicleSummaryDetection)
        val summaryCapacity = dialogView.findViewById<TextView>(R.id.vehicleSummaryCapacity)
        val summarySoh = dialogView.findViewById<TextView>(R.id.vehicleSummarySoh)
        val summaryEffective = dialogView.findViewById<TextView>(R.id.vehicleSummaryEffective)
        val summaryModel = dialogView.findViewById<TextView>(R.id.vehicleSummaryModel)
        val summaryCalibration = dialogView.findViewById<TextView>(R.id.vehicleSummaryCalibration)
        val capInput = dialogView.findViewById<
            com.google.android.material.textfield.TextInputEditText>(R.id.vehicleCapacityInput)
        val capLayout = dialogView.findViewById<
            com.google.android.material.textfield.TextInputLayout>(R.id.vehicleCapacityLayout)
        val modelDropdown = dialogView.findViewById<
            com.google.android.material.textfield.MaterialAutoCompleteTextView>(
            R.id.vehicleModelDropdown)
        val resetButton = dialogView.findViewById<
            com.google.android.material.button.MaterialButton>(R.id.vehicleResetAuto)

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
        modelDropdown.setOnItemClickListener { _, _, position, _ ->
            if (position in modelEntries.indices) {
                val entry = modelEntries[position]
                selectedModelId = entry.id
                modelSelectionChanged = true
                if (entry.nominalKwh > 0) {
                    capInput.setText(String.format(Locale.US, "%.1f", entry.nominalKwh))
                }
            }
        }

        val executor = metricsExecutor ?: Executors.newSingleThreadExecutor().also { metricsExecutor = it }
        executor.execute {
            var initialKwh = 0.0
            val modelIds = mutableListOf<ModelEntry>()
            var initialModelId: String? = null

            var nominalKwh = 0.0
            var nominalSource = "unset"
            var displaySoh = -1.0
            var displaySource = "unavailable"
            var estimatedKwh = 0.0
            var statusModelId: String? = null
            var calSoh = 0.0
            var calTs = 0L

            try {
                val conn = DaemonHttpClient.open("/api/performance/soh/nominal", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(body)
                    if (!json.isNull("nominalKwh")) initialKwh = json.optDouble("nominalKwh", 0.0)
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            try {
                val conn = DaemonHttpClient.open("/api/performance/soh", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(body)
                    nominalKwh = json.optDouble("nominalCapacityKwh", 0.0)
                    nominalSource = json.optString("nominalSource", "unset")
                    displaySoh = json.optDouble("displaySoh", -1.0)
                    displaySource = json.optString("displaySource", "unavailable")
                    val est = json.optDouble("estimatedCapacityKwh", -1.0)
                    if (est > 0) estimatedKwh = est
                    if (!json.isNull("modelId")) {
                        statusModelId = json.optString("modelId", "").ifEmpty { null }
                    }
                    val calObj = json.optJSONObject("calibration")
                    if (calObj != null) {
                        calSoh = calObj.optDouble("soh", -1.0)
                        calTs = calObj.optLong("timestampMs", 0L)
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            try {
                val conn = DaemonHttpClient.open("/api/models/manifest", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(body)
                    val arr = json.optJSONArray("models")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val m = arr.getJSONObject(i)
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
                            if (id.isNotEmpty()) modelIds.add(ModelEntry(id, title, kwh))
                        }
                    }
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            if (modelIds.isEmpty()) {
                try {
                    val body = ctx.assets.open("web/shared/models/manifest.json")
                        .bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(body)
                    val arr = json.optJSONArray("models")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val m = arr.getJSONObject(i)
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
                            if (id.isNotEmpty()) modelIds.add(ModelEntry(id, title, kwh))
                        }
                    }
                } catch (_: Throwable) {}
            }

            try {
                val conn = DaemonHttpClient.open("/api/models/selected", "GET", 2000, 3000)
                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(body)
                    val m = when {
                        json.has("selectedModelId") && !json.isNull("selectedModelId") ->
                            json.optString("selectedModelId", "")
                        json.has("modelSource") && json.optString("modelSource", "unset") == "unset" -> ""
                        else -> json.optString("modelId", "")
                    }
                    if (m.isNotEmpty()) initialModelId = m
                }
                conn.disconnect()
            } catch (_: Throwable) {}

            if (initialModelId == null) {
                try {
                    val prefs = ctx.getSharedPreferences("overdrive_vehicle", Context.MODE_PRIVATE)
                    val pId = prefs?.getString("selected_model_id", null)
                    if (!pId.isNullOrEmpty()) {
                        initialModelId = pId
                        if (initialKwh <= 0.0) initialKwh = prefs.getFloat("nominal_kwh", 0f).toDouble()
                    }
                } catch (_: Throwable) {}
            }
            if (initialModelId == null) {
                try {
                    val v = UnifiedConfigManager.getVehicle()
                    val m = v.optString("modelId", "")
                    val src = v.optString("modelSource", "")
                    if (m.isNotEmpty() && src != "unset") {
                        initialModelId = m
                        if (initialKwh <= 0.0) initialKwh = v.optDouble("nominalKwh", 0.0)
                    }
                } catch (_: Throwable) {}
            }

            if (nominalKwh <= 0.0 && initialKwh > 0.0) {
                nominalKwh = initialKwh
                nominalSource = "user"
            }

            val finalNominalKwh = nominalKwh
            val finalNominalSource = nominalSource
            val finalDisplaySoh = displaySoh
            val finalDisplaySource = displaySource
            val finalEstimatedKwh = estimatedKwh
            val finalStatusModelId = statusModelId ?: initialModelId
            val finalCalSoh = calSoh
            val finalCalTs = calTs

            mainHandler.post {
                if (!isAdded || view == null) return@post

                if (initialKwh > 0) capInput.setText(String.format(Locale.US, "%.1f", initialKwh))

                modelEntries.clear()
                modelEntries.addAll(modelIds)
                val titles = modelIds.map { it.title }
                val adapter = android.widget.ArrayAdapter(
                    ctx,
                    com.google.android.material.R.layout.m3_auto_complete_simple_item,
                    titles
                )
                modelDropdown.setAdapter(adapter)
                if (initialModelId != null) {
                    val idx = modelIds.indexOfFirst { it.id == initialModelId }
                    if (idx >= 0) {
                        modelDropdown.setText(titles[idx], false)
                        selectedModelId = initialModelId
                    }
                }

                val manual = finalNominalSource == "user" || initialModelId != null
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

                summaryCapacity.text = if (finalNominalKwh > 0) {
                    String.format(Locale.US, "%.1f kWh", finalNominalKwh)
                } else {
                    getString(R.string.soh_dialog_capacity_not_detected)
                }

                summarySoh.text = when {
                    finalDisplaySoh > 0 && finalDisplaySource == "oem" ->
                        String.format(Locale.US, "%.1f%% (vehicle)", finalDisplaySoh)
                    finalDisplaySoh > 0 && finalDisplaySource == "live" ->
                        String.format(Locale.US, "%.1f%% (live)", finalDisplaySoh)
                    finalDisplaySoh > 0 && finalDisplaySource == "calibration" ->
                        String.format(Locale.US, "%.1f%% (from last charge)", finalDisplaySoh)
                    finalDisplaySoh > 0 -> String.format(Locale.US, "%.1f%%", finalDisplaySoh)
                    else -> getString(R.string.vehicle_dialog_soh_unavailable)
                        .replaceFirstChar { it.uppercase() }
                }

                if (finalEstimatedKwh > 0) {
                    summaryEffective.text = getString(
                        R.string.vehicle_dialog_summary_effective, finalEstimatedKwh
                    )
                    summaryEffective.visibility = View.VISIBLE
                }

                summaryModel.text = if (finalStatusModelId != null) {
                    modelDisplayName(finalStatusModelId)
                } else {
                    getString(R.string.soh_dialog_model_not_selected)
                }

                if (finalCalSoh > 0 && finalCalTs > 0) {
                    val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(java.util.Date(finalCalTs))
                    summaryCalibration.text = getString(
                        R.string.vehicle_dialog_summary_calibration, finalCalSoh, date
                    )
                    summaryCalibration.visibility = View.VISIBLE
                }
            }
        }

        var completionDeferred = false
        var completionSent = false
        fun finishOnce() {
            if (!completionSent) {
                completionSent = true
                onFinished?.invoke()
            }
        }

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            ctx, R.style.Theme_Overdrive_M3_Dialog
        )
            .setTitle(getString(R.string.vehicle_dialog_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.vehicle_dialog_save), null)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        resetButton.setOnClickListener {
            completionDeferred = true
            postNominal(
                kwh = null,
                clearModelSelection = true,
                onComplete = { finishOnce() },
            )
            dialog.dismiss()
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
                dialog.setCanceledOnTouchOutside(false)
            }

            saveButton.setOnClickListener {
                val raw = capInput.text?.toString()?.trim().orEmpty()
                val kwh = raw.toDoubleOrNull()
                if (kwh == null || !kwh.isFinite() || kwh < 5.0 || kwh > 120.0) {
                    capLayout.error = getString(R.string.vehicle_dialog_invalid_capacity)
                    return@setOnClickListener
                }
                completionDeferred = true
                setSaving(true)
                postNominalAndModel(
                    kwh,
                    selectedModelId.takeIf { modelSelectionChanged } ?: selectedModelId,
                ) { _ ->
                    finishOnce()
                    dialog.dismiss()
                }
            }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.setOnDismissListener {
            if (!completionDeferred) finishOnce()
        }

        dialog.show()
        return true
    }

    private fun postNominal(
        kwh: Double?,
        clearModelSelection: Boolean = false,
        onComplete: (() -> Unit)? = null,
    ) {
        val executor = metricsExecutor ?: Executors.newSingleThreadExecutor().also { metricsExecutor = it }
        executor.execute {
            try {
                val vehicle = UnifiedConfigManager.getVehicle()
                if (clearModelSelection) {
                    vehicle.put("modelSource", VehicleModelSelection.SOURCE_UNSET)
                }
                if (kwh == null) {
                    vehicle.remove("nominalKwh")
                } else {
                    vehicle.put("nominalKwh", kwh)
                }
                UnifiedConfigManager.setVehicle(vehicle)
            } catch (_: Throwable) {}

            try {
                val prefs = context?.getSharedPreferences("overdrive_vehicle", Context.MODE_PRIVATE)
                prefs?.edit()?.apply {
                    if (clearModelSelection) remove("selected_model_id")
                    if (kwh == null) remove("nominal_kwh") else putFloat("nominal_kwh", kwh.toFloat())
                    apply()
                }
            } catch (_: Throwable) {}

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

            mainHandler.post {
                refreshVehicleTile()
                onComplete?.invoke()
            }
        }
    }

    private fun postNominalAndModel(
        kwh: Double,
        modelId: String?,
        onComplete: ((String?) -> Unit)? = null,
    ) {
        val executor = metricsExecutor ?: Executors.newSingleThreadExecutor().also { metricsExecutor = it }
        executor.execute {
            try {
                val vehicle = UnifiedConfigManager.getVehicle()
                if (!modelId.isNullOrEmpty()) {
                    vehicle.put("modelId", modelId)
                    vehicle.put("modelSource", VehicleModelSelection.SOURCE_USER)
                }
                vehicle.put("nominalKwh", kwh)
                UnifiedConfigManager.setVehicle(vehicle)
            } catch (_: Throwable) {}

            try {
                val prefs = context?.getSharedPreferences("overdrive_vehicle", Context.MODE_PRIVATE)
                prefs?.edit()?.apply {
                    if (!modelId.isNullOrEmpty()) putString("selected_model_id", modelId)
                    putFloat("nominal_kwh", kwh.toFloat())
                    apply()
                }
            } catch (_: Throwable) {}

            if (!modelId.isNullOrEmpty()) {
                postJsonResult(
                    "/api/models/selected",
                    org.json.JSONObject()
                        .put("modelId", modelId)
                        .put("nominalKwh", kwh),
                )
            } else {
                postJsonResult(
                    "/api/performance/soh/nominal",
                    org.json.JSONObject().put("nominalKwh", kwh),
                )
            }

            mainHandler.post {
                refreshVehicleTile()
                onComplete?.invoke(null)
            }
        }
    }

    private fun postJsonResult(path: String, body: org.json.JSONObject): String? {
        var conn: java.net.HttpURLConnection? = null
        return try {
            conn = DaemonHttpClient.open(path, "POST", 3000, 5000)
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val status = conn.responseCode
            if (status in 200..299) null else "HTTP $status"
        } catch (e: Throwable) {
            e.message
        } finally {
            try { conn?.disconnect() } catch (_: Throwable) {}
        }
    }

    private fun modelDisplayName(modelId: String?): String {
        if (modelId.isNullOrEmpty()) return "—"
        return VehicleTopDownArt.displayNameFor(modelId, context)
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}
