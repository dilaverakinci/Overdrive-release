package com.overdrive.app.ui.fragment

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.overdrive.app.R
import com.overdrive.app.ui.vehicle.TyreData
import com.overdrive.app.ui.vehicle.VehicleArt
import com.overdrive.app.ui.vehicle.VehicleCategoryTab
import com.overdrive.app.ui.vehicle.VehicleState
import com.overdrive.app.ui.vehicle.VehicleViewModel
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Pure native Vehicle Control fragment.
 * Complete 1:1 visual and functional parity with legacy vehicle-control.html,
 * featuring 60 FPS Material 3 controls, 10-category action dock, and 4-corner TPMS strip.
 */
class VehicleNativeFragment : Fragment() {

    private val viewModel: VehicleViewModel by viewModels()

    // Top Status Bar
    private lateinit var pillLockStatus: LinearLayout
    private lateinit var dotLockStatus: View
    private lateinit var tvLockStatus: TextView

    private lateinit var pillModeToggle: LinearLayout
    private lateinit var tvModeLabel: TextView
    private var is3DMode = false

    private lateinit var pillCloudStatus: LinearLayout
    private lateinit var dotCloudStatus: View
    private lateinit var tvCloudStatus: TextView

    // Center Viewport
    private lateinit var ivVehicleArt: ImageView
    private lateinit var tvDoorWarningBadge: TextView

    // TPMS Strip
    private lateinit var cardTyreFL: MaterialCardView
    private lateinit var dotTyreFL: View
    private lateinit var tvTyreStateFL: TextView
    private lateinit var tvTyrePsiFL: TextView
    private lateinit var tvTyreSubFL: TextView

    private lateinit var cardTyreFR: MaterialCardView
    private lateinit var dotTyreFR: View
    private lateinit var tvTyreStateFR: TextView
    private lateinit var tvTyrePsiFR: TextView
    private lateinit var tvTyreSubFR: TextView

    private lateinit var cardTyreRL: MaterialCardView
    private lateinit var dotTyreRL: View
    private lateinit var tvTyreStateRL: TextView
    private lateinit var tvTyrePsiRL: TextView
    private lateinit var tvTyreSubRL: TextView

    private lateinit var cardTyreRR: MaterialCardView
    private lateinit var dotTyreRR: View
    private lateinit var tvTyreStateRR: TextView
    private lateinit var tvTyrePsiRR: TextView
    private lateinit var tvTyreSubRR: TextView

    // Expandable Actions Panel
    private lateinit var layoutExpandablePanel: MaterialCardView
    private lateinit var panelSecurity: LinearLayout
    private lateinit var panelTrunk: LinearLayout
    private lateinit var panelClimate: LinearLayout
    private lateinit var panelSeats: LinearLayout
    private lateinit var panelWindows: LinearLayout
    private lateinit var panelLights: LinearLayout
    private lateinit var panelAdas: LinearLayout
    private lateinit var panelCharging: LinearLayout
    private lateinit var panelSound: LinearLayout
    private lateinit var panelSystem: LinearLayout

    // Panel Action Controls
    private lateinit var btnLock: MaterialButton
    private lateinit var btnUnlock: MaterialButton
    private lateinit var btnFlash: MaterialButton
    private lateinit var btnFindCar: MaterialButton

    private lateinit var btnTrunkOpen: MaterialButton
    private lateinit var btnTrunkClose: MaterialButton

    private lateinit var btnAcOn: MaterialButton
    private lateinit var btnAcOff: MaterialButton
    private lateinit var btnTempMinus: ImageButton
    private lateinit var tvTargetTemp: TextView
    private lateinit var btnTempPlus: ImageButton
    private lateinit var switchBatteryHeat: MaterialSwitch

    private lateinit var btnDriverSeatHeat: MaterialButton
    private lateinit var btnDriverSeatCool: MaterialButton
    private lateinit var switchSteeringHeat: MaterialSwitch

    private lateinit var btnVentAll: MaterialButton
    private lateinit var btnCloseAll: MaterialButton
    private lateinit var btnOpenAll: MaterialButton

    private lateinit var switchDaytimeLight: MaterialSwitch
    private lateinit var btnAmbientCyan: MaterialButton
    private lateinit var btnAmbientBlue: MaterialButton
    private lateinit var btnAmbientPurple: MaterialButton

    private lateinit var switchSpeedLimitWarning: MaterialSwitch
    private lateinit var switchChildPresence: MaterialSwitch

    private lateinit var btnStartCharging: MaterialButton
    private lateinit var btnCap80: MaterialButton
    private lateinit var btnCap90: MaterialButton
    private lateinit var btnCap100: MaterialButton

    private lateinit var btnAvasTone1: MaterialButton
    private lateinit var btnAvasTone2: MaterialButton
    private lateinit var btnAvasStop: MaterialButton

    private lateinit var btnIviReboot: MaterialButton

    // Bottom Dock (10 tabs)
    private lateinit var tabSecurity: LinearLayout
    private lateinit var dotTabSecurity: View
    private lateinit var ivTabSecurity: ImageView
    private lateinit var tvTabSecurity: TextView

    private lateinit var tabTrunk: LinearLayout
    private lateinit var dotTabTrunk: View
    private lateinit var ivTabTrunk: ImageView
    private lateinit var tvTabTrunk: TextView

    private lateinit var tabClimate: LinearLayout
    private lateinit var dotTabClimate: View
    private lateinit var ivTabClimate: ImageView
    private lateinit var tvTabClimate: TextView

    private lateinit var tabSeats: LinearLayout
    private lateinit var dotTabSeats: View
    private lateinit var ivTabSeats: ImageView
    private lateinit var tvTabSeats: TextView

    private lateinit var tabWindows: LinearLayout
    private lateinit var dotTabWindows: View
    private lateinit var ivTabWindows: ImageView
    private lateinit var tvTabWindows: TextView

    private lateinit var tabLights: LinearLayout
    private lateinit var dotTabLights: View
    private lateinit var ivTabLights: ImageView
    private lateinit var tvTabLights: TextView

    private lateinit var tabAdas: LinearLayout
    private lateinit var dotTabAdas: View
    private lateinit var ivTabAdas: ImageView
    private lateinit var tvTabAdas: TextView

    private lateinit var tabCharging: LinearLayout
    private lateinit var dotTabCharging: View
    private lateinit var ivTabCharging: ImageView
    private lateinit var tvTabCharging: TextView

    private lateinit var tabSound: LinearLayout
    private lateinit var dotTabSound: View
    private lateinit var ivTabSound: ImageView
    private lateinit var tvTabSound: TextView

    private lateinit var tabSystem: LinearLayout
    private lateinit var dotTabSystem: View
    private lateinit var ivTabSystem: ImageView
    private lateinit var tvTabSystem: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vehicle_native, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startPolling()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPolling()
    }

    private fun bindViews(root: View) {
        // Top Status Bar
        pillLockStatus = root.findViewById(R.id.pillLockStatus)
        dotLockStatus = root.findViewById(R.id.dotLockStatus)
        tvLockStatus = root.findViewById(R.id.tvLockStatus)

        pillModeToggle = root.findViewById(R.id.pillModeToggle)
        tvModeLabel = root.findViewById(R.id.tvModeLabel)

        pillCloudStatus = root.findViewById(R.id.pillCloudStatus)
        dotCloudStatus = root.findViewById(R.id.dotCloudStatus)
        tvCloudStatus = root.findViewById(R.id.tvCloudStatus)

        // Center Viewport
        ivVehicleArt = root.findViewById(R.id.ivVehicleArt)
        tvDoorWarningBadge = root.findViewById(R.id.tvDoorWarningBadge)

        // TPMS Strip
        cardTyreFL = root.findViewById(R.id.cardTyreFL)
        dotTyreFL = root.findViewById(R.id.dotTyreFL)
        tvTyreStateFL = root.findViewById(R.id.tvTyreStateFL)
        tvTyrePsiFL = root.findViewById(R.id.tvTyrePsiFL)
        tvTyreSubFL = root.findViewById(R.id.tvTyreSubFL)

        cardTyreFR = root.findViewById(R.id.cardTyreFR)
        dotTyreFR = root.findViewById(R.id.dotTyreFR)
        tvTyreStateFR = root.findViewById(R.id.tvTyreStateFR)
        tvTyrePsiFR = root.findViewById(R.id.tvTyrePsiFR)
        tvTyreSubFR = root.findViewById(R.id.tvTyreSubFR)

        cardTyreRL = root.findViewById(R.id.cardTyreRL)
        dotTyreRL = root.findViewById(R.id.dotTyreRL)
        tvTyreStateRL = root.findViewById(R.id.tvTyreStateRL)
        tvTyrePsiRL = root.findViewById(R.id.tvTyrePsiRL)
        tvTyreSubRL = root.findViewById(R.id.tvTyreSubRL)

        cardTyreRR = root.findViewById(R.id.cardTyreRR)
        dotTyreRR = root.findViewById(R.id.dotTyreRR)
        tvTyreStateRR = root.findViewById(R.id.tvTyreStateRR)
        tvTyrePsiRR = root.findViewById(R.id.tvTyrePsiRR)
        tvTyreSubRR = root.findViewById(R.id.tvTyreSubRR)

        // Expandable Panel
        layoutExpandablePanel = root.findViewById(R.id.layoutExpandablePanel)
        panelSecurity = root.findViewById(R.id.panelSecurity)
        panelTrunk = root.findViewById(R.id.panelTrunk)
        panelClimate = root.findViewById(R.id.panelClimate)
        panelSeats = root.findViewById(R.id.panelSeats)
        panelWindows = root.findViewById(R.id.panelWindows)
        panelLights = root.findViewById(R.id.panelLights)
        panelAdas = root.findViewById(R.id.panelAdas)
        panelCharging = root.findViewById(R.id.panelCharging)
        panelSound = root.findViewById(R.id.panelSound)
        panelSystem = root.findViewById(R.id.panelSystem)

        // Panel Actions
        btnLock = root.findViewById(R.id.btnLock)
        btnUnlock = root.findViewById(R.id.btnUnlock)
        btnFlash = root.findViewById(R.id.btnFlash)
        btnFindCar = root.findViewById(R.id.btnFindCar)

        btnTrunkOpen = root.findViewById(R.id.btnTrunkOpen)
        btnTrunkClose = root.findViewById(R.id.btnTrunkClose)

        btnAcOn = root.findViewById(R.id.btnAcOn)
        btnAcOff = root.findViewById(R.id.btnAcOff)
        btnTempMinus = root.findViewById(R.id.btnTempMinus)
        tvTargetTemp = root.findViewById(R.id.tvTargetTemp)
        btnTempPlus = root.findViewById(R.id.btnTempPlus)
        switchBatteryHeat = root.findViewById(R.id.switchBatteryHeat)

        btnDriverSeatHeat = root.findViewById(R.id.btnDriverSeatHeat)
        btnDriverSeatCool = root.findViewById(R.id.btnDriverSeatCool)
        switchSteeringHeat = root.findViewById(R.id.switchSteeringHeat)

        btnVentAll = root.findViewById(R.id.btnVentAll)
        btnCloseAll = root.findViewById(R.id.btnCloseAll)
        btnOpenAll = root.findViewById(R.id.btnOpenAll)

        switchDaytimeLight = root.findViewById(R.id.switchDaytimeLight)
        btnAmbientCyan = root.findViewById(R.id.btnAmbientCyan)
        btnAmbientBlue = root.findViewById(R.id.btnAmbientBlue)
        btnAmbientPurple = root.findViewById(R.id.btnAmbientPurple)

        switchSpeedLimitWarning = root.findViewById(R.id.switchSpeedLimitWarning)
        switchChildPresence = root.findViewById(R.id.switchChildPresence)

        btnStartCharging = root.findViewById(R.id.btnStartCharging)
        btnCap80 = root.findViewById(R.id.btnCap80)
        btnCap90 = root.findViewById(R.id.btnCap90)
        btnCap100 = root.findViewById(R.id.btnCap100)

        btnAvasTone1 = root.findViewById(R.id.btnAvasTone1)
        btnAvasTone2 = root.findViewById(R.id.btnAvasTone2)
        btnAvasStop = root.findViewById(R.id.btnAvasStop)

        btnIviReboot = root.findViewById(R.id.btnIviReboot)

        // Bottom Dock (10 tabs)
        tabSecurity = root.findViewById(R.id.tabSecurity)
        dotTabSecurity = root.findViewById(R.id.dotTabSecurity)
        ivTabSecurity = root.findViewById(R.id.ivTabSecurity)
        tvTabSecurity = root.findViewById(R.id.tvTabSecurity)

        tabTrunk = root.findViewById(R.id.tabTrunk)
        dotTabTrunk = root.findViewById(R.id.dotTabTrunk)
        ivTabTrunk = root.findViewById(R.id.ivTabTrunk)
        tvTabTrunk = root.findViewById(R.id.tvTabTrunk)

        tabClimate = root.findViewById(R.id.tabClimate)
        dotTabClimate = root.findViewById(R.id.dotTabClimate)
        ivTabClimate = root.findViewById(R.id.ivTabClimate)
        tvTabClimate = root.findViewById(R.id.tvTabClimate)

        tabSeats = root.findViewById(R.id.tabSeats)
        dotTabSeats = root.findViewById(R.id.dotTabSeats)
        ivTabSeats = root.findViewById(R.id.ivTabSeats)
        tvTabSeats = root.findViewById(R.id.tvTabSeats)

        tabWindows = root.findViewById(R.id.tabWindows)
        dotTabWindows = root.findViewById(R.id.dotTabWindows)
        ivTabWindows = root.findViewById(R.id.ivTabWindows)
        tvTabWindows = root.findViewById(R.id.tvTabWindows)

        tabLights = root.findViewById(R.id.tabLights)
        dotTabLights = root.findViewById(R.id.dotTabLights)
        ivTabLights = root.findViewById(R.id.ivTabLights)
        tvTabLights = root.findViewById(R.id.tvTabLights)

        tabAdas = root.findViewById(R.id.tabAdas)
        dotTabAdas = root.findViewById(R.id.dotTabAdas)
        ivTabAdas = root.findViewById(R.id.ivTabAdas)
        tvTabAdas = root.findViewById(R.id.tvTabAdas)

        tabCharging = root.findViewById(R.id.tabCharging)
        dotTabCharging = root.findViewById(R.id.dotTabCharging)
        ivTabCharging = root.findViewById(R.id.ivTabCharging)
        tvTabCharging = root.findViewById(R.id.tvTabCharging)

        tabSound = root.findViewById(R.id.tabSound)
        dotTabSound = root.findViewById(R.id.dotTabSound)
        ivTabSound = root.findViewById(R.id.ivTabSound)
        tvTabSound = root.findViewById(R.id.tvTabSound)

        tabSystem = root.findViewById(R.id.tabSystem)
        dotTabSystem = root.findViewById(R.id.dotTabSystem)
        ivTabSystem = root.findViewById(R.id.ivTabSystem)
        tvTabSystem = root.findViewById(R.id.tvTabSystem)
    }

    private fun setupListeners() {
        // Mode toggle
        pillModeToggle.setOnClickListener {
            is3DMode = !is3DMode
            tvModeLabel.text = if (is3DMode) getString(R.string.vc_mode_3d) else getString(R.string.vc_mode_2d)
        }

        // Dock Tab toggles
        tabSecurity.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.SECURITY) }
        tabTrunk.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.TRUNK) }
        tabClimate.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.CLIMATE) }
        tabSeats.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.SEATS) }
        tabWindows.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.WINDOWS) }
        tabLights.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.LIGHTS) }
        tabAdas.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.ADAS) }
        tabCharging.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.CHARGING) }
        tabSound.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.SOUND) }
        tabSystem.setOnClickListener { viewModel.toggleTab(VehicleCategoryTab.SYSTEM) }

        // Action Buttons: Security
        btnLock.setOnClickListener { viewModel.lock { showToast(it) } }
        btnUnlock.setOnClickListener { viewModel.unlock { showToast(it) } }
        btnFlash.setOnClickListener { viewModel.flash { showToast(it) } }
        btnFindCar.setOnClickListener { viewModel.findCar { showToast(it) } }

        // Action Buttons: Trunk
        btnTrunkOpen.setOnClickListener { viewModel.openTrunk { showToast(it) } }
        btnTrunkClose.setOnClickListener { viewModel.closeTrunk { showToast(it) } }

        // Action Buttons: Climate
        btnAcOn.setOnClickListener { viewModel.setAc(true) { showToast(it) } }
        btnAcOff.setOnClickListener { viewModel.setAc(false) { showToast(it) } }
        btnTempMinus.setOnClickListener { viewModel.adjustTargetTemp(-0.5) { showToast(it) } }
        btnTempPlus.setOnClickListener { viewModel.adjustTargetTemp(0.5) { showToast(it) } }
        switchBatteryHeat.setOnCheckedChangeListener { _, isChecked ->
            if (switchBatteryHeat.isPressed) {
                viewModel.setBatteryHeat(isChecked) { showToast(it) }
            }
        }

        // Action Buttons: Seats
        btnDriverSeatHeat.setOnClickListener {
            val cur = viewModel.state.value.seats.driverHeat
            val next = (cur + 1) % 4
            viewModel.setDriverSeatHeat(next) { showToast(it) }
        }
        btnDriverSeatCool.setOnClickListener {
            val cur = viewModel.state.value.seats.driverCool
            val next = (cur + 1) % 4
            viewModel.setDriverSeatCool(next) { showToast(it) }
        }
        switchSteeringHeat.setOnCheckedChangeListener { _, isChecked ->
            if (switchSteeringHeat.isPressed) {
                viewModel.setSteeringHeat(isChecked) { showToast(it) }
            }
        }

        // Action Buttons: Windows
        btnVentAll.setOnClickListener { viewModel.ventAllWindows { showToast(it) } }
        btnCloseAll.setOnClickListener { viewModel.closeAllWindows { showToast(it) } }
        btnOpenAll.setOnClickListener { viewModel.openAllWindows { showToast(it) } }

        // Action Buttons: Lights
        switchDaytimeLight.setOnCheckedChangeListener { _, isChecked ->
            if (switchDaytimeLight.isPressed) {
                viewModel.setDaytimeLights(isChecked) { showToast(it) }
            }
        }
        btnAmbientCyan.setOnClickListener { viewModel.setAmbientLights(0) { showToast(it) } }
        btnAmbientBlue.setOnClickListener { viewModel.setAmbientLights(1) { showToast(it) } }
        btnAmbientPurple.setOnClickListener { viewModel.setAmbientLights(2) { showToast(it) } }

        // Action Buttons: ADAS
        switchSpeedLimitWarning.setOnCheckedChangeListener { _, isChecked ->
            if (switchSpeedLimitWarning.isPressed) {
                viewModel.setSpeedLimitWarning(isChecked) { showToast(it) }
            }
        }
        switchChildPresence.setOnCheckedChangeListener { _, isChecked ->
            if (switchChildPresence.isPressed) {
                viewModel.setChildPresence(isChecked) { showToast(it) }
            }
        }

        // Action Buttons: Charging
        btnStartCharging.setOnClickListener { viewModel.startCharging { showToast(it) } }
        btnCap80.setOnClickListener { viewModel.setChargeCap(80) { showToast(it) } }
        btnCap90.setOnClickListener { viewModel.setChargeCap(90) { showToast(it) } }
        btnCap100.setOnClickListener { viewModel.setChargeCap(100) { showToast(it) } }

        // Action Buttons: Sound
        btnAvasTone1.setOnClickListener { viewModel.setAvasTone(1) { showToast(it) } }
        btnAvasTone2.setOnClickListener { viewModel.setAvasTone(2) { showToast(it) } }
        btnAvasStop.setOnClickListener { viewModel.stopAvasTone { showToast(it) } }

        // Action Buttons: System
        btnIviReboot.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.vc_action_ivi_reboot)
                .setMessage(R.string.vc_action_ivi_reboot_confirm)
                .setPositiveButton(R.string.vc_action_reboot) { _, _ ->
                    viewModel.rebootIvi { showToast(it) }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.selectedTab.collect { tab ->
                        renderSelectedTab(tab)
                    }
                }
            }
        }
    }

    private fun renderState(state: VehicleState) {
        val context = context ?: return

        // 1. Lock Status Pill
        when (state.overallLockState) {
            1 -> {
                dotLockStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_success))
                tvLockStatus.text = getString(R.string.vc_status_locked)
            }
            2 -> {
                dotLockStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_warning))
                tvLockStatus.text = getString(R.string.vc_status_unlocked)
            }
            else -> {
                dotLockStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_muted))
                tvLockStatus.text = getString(R.string.vc_status_no_data)
            }
        }

        // 2. Cloud Status Pill
        if (state.cloudConnected) {
            dotCloudStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_success))
            tvCloudStatus.text = getString(R.string.vc_cloud_connected)
        } else {
            dotCloudStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_muted))
            tvCloudStatus.text = getString(R.string.vc_cloud_not_connected)
        }

        // 3. Vehicle Hero Art
        ivVehicleArt.setImageResource(VehicleArt.drawableFor(state.modelId))

        // 4. Open Doors / Trunk Warning Badge
        val openDoors = state.doorOpenStates.values.count { it }
        val isTrunkOpen = state.trunkOpen
        if (openDoors > 0 || isTrunkOpen) {
            tvDoorWarningBadge.visibility = View.VISIBLE
            val doorText = when {
                openDoors > 1 -> getString(R.string.vc_doors_open, openDoors)
                openDoors == 1 -> getString(R.string.vc_door_open)
                else -> null
            }
            val trunkText = if (isTrunkOpen) getString(R.string.vc_trunk_is_open) else null
            tvDoorWarningBadge.text = listOfNotNull(doorText, trunkText).joinToString(" · ")
        } else {
            tvDoorWarningBadge.visibility = View.GONE
        }

        // 5. TPMS Strip
        renderTyre(state.tyres["fl"], dotTyreFL, tvTyreStateFL, tvTyrePsiFL, tvTyreSubFL)
        renderTyre(state.tyres["fr"], dotTyreFR, tvTyreStateFR, tvTyrePsiFR, tvTyreSubFR)
        renderTyre(state.tyres["rl"], dotTyreRL, tvTyreStateRL, tvTyrePsiRL, tvTyreSubRL)
        renderTyre(state.tyres["rr"], dotTyreRR, tvTyreStateRR, tvTyrePsiRR, tvTyreSubRR)

        // 6. Climate State
        tvTargetTemp.text = String.format(Locale.US, "%.1f°C", state.climate.targetTempC)
        switchBatteryHeat.isChecked = state.climate.batteryHeat

        // 7. Seats State
        val heatLabel = when (state.seats.driverHeat) {
            0 -> getString(R.string.vc_action_seat_off)
            else -> getString(R.string.vc_action_seat_level, state.seats.driverHeat)
        }
        btnDriverSeatHeat.text = "${getString(R.string.vc_action_driver_seat)} ${getString(R.string.vc_action_seat_heat)}: $heatLabel"

        val coolLabel = when (state.seats.driverCool) {
            0 -> getString(R.string.vc_action_seat_off)
            else -> getString(R.string.vc_action_seat_level, state.seats.driverCool)
        }
        btnDriverSeatCool.text = "${getString(R.string.vc_action_driver_seat)} ${getString(R.string.vc_action_seat_cool)}: $coolLabel"

        switchSteeringHeat.isChecked = state.seats.steeringHeat

        // 8. Lights State
        switchDaytimeLight.isChecked = state.lights.daytimeLight

        // 9. ADAS State
        switchSpeedLimitWarning.isChecked = state.adas.speedLimitWarning
        switchChildPresence.isChecked = state.adas.childPresenceDetection
    }

    private fun renderTyre(
        tyre: TyreData?,
        dot: View,
        tvState: TextView,
        tvPsi: TextView,
        tvSub: TextView
    ) {
        val context = context ?: return
        if (tyre != null && tyre.available) {
            val isNormal = tyre.status == "normal"
            val dotColor = if (isNormal) R.color.status_success else R.color.status_warning
            dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, dotColor))
            tvState.text = if (isNormal) "OK" else "WARN"
            tvState.setTextColor(ContextCompat.getColor(context, dotColor))

            tvPsi.text = tyre.psi?.let { String.format(Locale.US, "%.1f PSI", it) } ?: "— PSI"
            tvSub.text = "${tyre.kPa ?: "—"} kPa · ${tyre.tempC ?: "—"} °C"
        } else {
            dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_muted))
            tvState.text = "—"
            tvState.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            tvPsi.text = "— PSI"
            tvSub.text = "— kPa · — °C"
        }
    }

    private fun renderSelectedTab(selectedTab: VehicleCategoryTab?) {
        // Toggle Panel Visibility
        layoutExpandablePanel.visibility = if (selectedTab != null) View.VISIBLE else View.GONE

        panelSecurity.visibility = if (selectedTab == VehicleCategoryTab.SECURITY) View.VISIBLE else View.GONE
        panelTrunk.visibility = if (selectedTab == VehicleCategoryTab.TRUNK) View.VISIBLE else View.GONE
        panelClimate.visibility = if (selectedTab == VehicleCategoryTab.CLIMATE) View.VISIBLE else View.GONE
        panelSeats.visibility = if (selectedTab == VehicleCategoryTab.SEATS) View.VISIBLE else View.GONE
        panelWindows.visibility = if (selectedTab == VehicleCategoryTab.WINDOWS) View.VISIBLE else View.GONE
        panelLights.visibility = if (selectedTab == VehicleCategoryTab.LIGHTS) View.VISIBLE else View.GONE
        panelAdas.visibility = if (selectedTab == VehicleCategoryTab.ADAS) View.VISIBLE else View.GONE
        panelCharging.visibility = if (selectedTab == VehicleCategoryTab.CHARGING) View.VISIBLE else View.GONE
        panelSound.visibility = if (selectedTab == VehicleCategoryTab.SOUND) View.VISIBLE else View.GONE
        panelSystem.visibility = if (selectedTab == VehicleCategoryTab.SYSTEM) View.VISIBLE else View.GONE

        // Update Dock Tabs (10 tabs)
        val tabEntries = listOf(
            Triple(VehicleCategoryTab.SECURITY, tabSecurity, Triple(dotTabSecurity, ivTabSecurity, tvTabSecurity)),
            Triple(VehicleCategoryTab.TRUNK, tabTrunk, Triple(dotTabTrunk, ivTabTrunk, tvTabTrunk)),
            Triple(VehicleCategoryTab.CLIMATE, tabClimate, Triple(dotTabClimate, ivTabClimate, tvTabClimate)),
            Triple(VehicleCategoryTab.SEATS, tabSeats, Triple(dotTabSeats, ivTabSeats, tvTabSeats)),
            Triple(VehicleCategoryTab.WINDOWS, tabWindows, Triple(dotTabWindows, ivTabWindows, tvTabWindows)),
            Triple(VehicleCategoryTab.LIGHTS, tabLights, Triple(dotTabLights, ivTabLights, tvTabLights)),
            Triple(VehicleCategoryTab.ADAS, tabAdas, Triple(dotTabAdas, ivTabAdas, tvTabAdas)),
            Triple(VehicleCategoryTab.CHARGING, tabCharging, Triple(dotTabCharging, ivTabCharging, tvTabCharging)),
            Triple(VehicleCategoryTab.SOUND, tabSound, Triple(dotTabSound, ivTabSound, tvTabSound)),
            Triple(VehicleCategoryTab.SYSTEM, tabSystem, Triple(dotTabSystem, ivTabSystem, tvTabSystem))
        )

        val context = context ?: return
        val activeColor = ContextCompat.getColor(context, R.color.brand_primary)
        val normalColor = ContextCompat.getColor(context, R.color.text_muted)

        for ((tabEnum, container, views) in tabEntries) {
            val isSelected = tabEnum == selectedTab
            val (dot, icon, text) = views

            container.setBackgroundResource(if (isSelected) R.drawable.bg_vc_tab_active else android.R.color.transparent)
            dot.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            icon.imageTintList = ColorStateList.valueOf(if (isSelected) activeColor else normalColor)
            text.setTextColor(if (isSelected) activeColor else normalColor)
            text.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun showToast(success: Boolean) {
        if (!isAdded) return
        val msg = if (success) getString(R.string.vc_cmd_sent) else getString(R.string.vc_cmd_failed)
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
