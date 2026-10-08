package com.overdrive.app.ui.fragment

import android.content.res.ColorStateList
import android.graphics.Color
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
import com.google.android.material.slider.Slider
import com.overdrive.app.R
import com.overdrive.app.byd.light.LightConstants
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

    private lateinit var pillModelPicker: LinearLayout
    private lateinit var tvModelName: TextView

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

    // Panel Action Controls: Security
    private lateinit var btnLock: MaterialButton
    private lateinit var btnUnlock: MaterialButton
    private lateinit var btnFlash: MaterialButton
    private lateinit var btnFindCar: MaterialButton

    // Panel Action Controls: Trunk
    private lateinit var btnTrunkOpen: MaterialButton
    private lateinit var btnTrunkClose: MaterialButton

    // Panel Action Controls: Climate
    private lateinit var btnAcOn: MaterialButton
    private lateinit var btnAcOff: MaterialButton
    private lateinit var btnTempMinus: ImageButton
    private lateinit var tvTargetTemp: TextView
    private lateinit var btnTempPlus: ImageButton
    private lateinit var btnFanMinus: ImageButton
    private lateinit var tvFanSpeed: TextView
    private lateinit var btnFanPlus: ImageButton
    private lateinit var switchBatteryHeat: MaterialSwitch

    // Panel Action Controls: Seats
    private lateinit var btnDriverSeatHeat: MaterialButton
    private lateinit var btnDriverSeatCool: MaterialButton
    private lateinit var btnPassengerSeatHeat: MaterialButton
    private lateinit var btnPassengerSeatCool: MaterialButton
    private lateinit var switchSteeringHeat: MaterialSwitch
    private lateinit var btnSeatMemory1: MaterialButton
    private lateinit var btnSeatMemory2: MaterialButton

    // Panel Action Controls: Windows
    private lateinit var btnWinAllVent: MaterialButton
    private lateinit var btnCloseAll: MaterialButton
    private lateinit var btnOpenAll: MaterialButton
    private val windowRows = mutableListOf<WindowRow>()

    // Panel Action Controls: Lights
    private lateinit var switchDaytimeLight: MaterialSwitch
    private lateinit var viewAmbientPreview: View
    private lateinit var tvAmbientColorValue: TextView
    private lateinit var sliderAmbientColor: Slider
    private var isDraggingAmbient = false

    // Panel Action Controls: ADAS
    private lateinit var switchSpeedLimitWarning: MaterialSwitch
    private lateinit var switchChildPresence: MaterialSwitch

    // Panel Action Controls: Charging
    private lateinit var btnStartCharging: MaterialButton
    private lateinit var btnCap80: MaterialButton
    private lateinit var btnCap90: MaterialButton
    private lateinit var btnCap100: MaterialButton
    private lateinit var btnCurrent6A: MaterialButton
    private lateinit var btnCurrent8A: MaterialButton
    private lateinit var btnCurrent10A: MaterialButton
    private lateinit var btnCurrent16A: MaterialButton
    private lateinit var btnCurrentMax: MaterialButton

    // Panel Action Controls: Sound
    private lateinit var btnAvasDingDong: MaterialButton
    private lateinit var btnAvasTripleBeep: MaterialButton
    private lateinit var btnAvasChime: MaterialButton
    private lateinit var btnAvasAlarm: MaterialButton
    private lateinit var btnAvasStop: MaterialButton
    private lateinit var switchEngineSound: MaterialSwitch
    private lateinit var btnEngineMinus: ImageButton
    private lateinit var tvEnginePreset: TextView
    private lateinit var btnEnginePlus: ImageButton
    private var currentEnginePreset = 1

    // Panel Action Controls: System
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

    private class WindowRow(
        val areaNum: Int,
        val areaKey: String,
        val nameResId: Int,
        container: View
    ) {
        val tvName: TextView = container.findViewById(R.id.tvWindowName)
        val tvPercent: TextView = container.findViewById(R.id.tvWindowPercent)
        val btn0: MaterialButton = container.findViewById(R.id.btnPreset0)
        val btn25: MaterialButton = container.findViewById(R.id.btnPreset25)
        val btn50: MaterialButton = container.findViewById(R.id.btnPreset50)
        val btn75: MaterialButton = container.findViewById(R.id.btnPreset75)
        val btn100: MaterialButton = container.findViewById(R.id.btnPreset100)
        val buttons = listOf(0 to btn0, 25 to btn25, 50 to btn50, 75 to btn75, 100 to btn100)
    }

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

        pillModelPicker = root.findViewById(R.id.pillModelPicker)
        tvModelName = root.findViewById(R.id.tvModelName)

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

        // Panel Actions: Security
        btnLock = root.findViewById(R.id.btnLock)
        btnUnlock = root.findViewById(R.id.btnUnlock)
        btnFlash = root.findViewById(R.id.btnFlash)
        btnFindCar = root.findViewById(R.id.btnFindCar)

        // Panel Actions: Trunk
        btnTrunkOpen = root.findViewById(R.id.btnTrunkOpen)
        btnTrunkClose = root.findViewById(R.id.btnTrunkClose)

        // Panel Actions: Climate
        btnAcOn = root.findViewById(R.id.btnAcOn)
        btnAcOff = root.findViewById(R.id.btnAcOff)
        btnTempMinus = root.findViewById(R.id.btnTempMinus)
        tvTargetTemp = root.findViewById(R.id.tvTargetTemp)
        btnTempPlus = root.findViewById(R.id.btnTempPlus)
        btnFanMinus = root.findViewById(R.id.btnFanMinus)
        tvFanSpeed = root.findViewById(R.id.tvFanSpeed)
        btnFanPlus = root.findViewById(R.id.btnFanPlus)
        switchBatteryHeat = root.findViewById(R.id.switchBatteryHeat)

        // Panel Actions: Seats
        btnDriverSeatHeat = root.findViewById(R.id.btnDriverSeatHeat)
        btnDriverSeatCool = root.findViewById(R.id.btnDriverSeatCool)
        btnPassengerSeatHeat = root.findViewById(R.id.btnPassengerSeatHeat)
        btnPassengerSeatCool = root.findViewById(R.id.btnPassengerSeatCool)
        switchSteeringHeat = root.findViewById(R.id.switchSteeringHeat)
        btnSeatMemory1 = root.findViewById(R.id.btnSeatMemory1)
        btnSeatMemory2 = root.findViewById(R.id.btnSeatMemory2)

        // Panel Actions: Windows
        btnWinAllVent = root.findViewById(R.id.btnWinAllVent)
        btnCloseAll = root.findViewById(R.id.btnCloseAll)
        btnOpenAll = root.findViewById(R.id.btnOpenAll)

        windowRows.clear()
        windowRows.add(WindowRow(1, "lf", R.string.vc_tpms_front_left, root.findViewById(R.id.rowWinLF)))
        windowRows.add(WindowRow(2, "rf", R.string.vc_tpms_front_right, root.findViewById(R.id.rowWinFR)))
        windowRows.add(WindowRow(3, "lr", R.string.vc_tpms_rear_left, root.findViewById(R.id.rowWinLR)))
        windowRows.add(WindowRow(4, "rr", R.string.vc_tpms_rear_right, root.findViewById(R.id.rowWinRR)))
        windowRows.add(WindowRow(5, "sunroof", R.string.vc_sunroof, root.findViewById(R.id.rowWinSunroof)))
        windowRows.add(WindowRow(6, "sunshade", R.string.vc_sunshade, root.findViewById(R.id.rowWinSunshade)))

        // Panel Actions: Lights
        switchDaytimeLight = root.findViewById(R.id.switchDaytimeLight)
        viewAmbientPreview = root.findViewById(R.id.viewAmbientPreview)
        tvAmbientColorValue = root.findViewById(R.id.tvAmbientColorValue)
        sliderAmbientColor = root.findViewById(R.id.sliderAmbientColor)

        // Panel Actions: ADAS
        switchSpeedLimitWarning = root.findViewById(R.id.switchSpeedLimitWarning)
        switchChildPresence = root.findViewById(R.id.switchChildPresence)

        // Panel Actions: Charging
        btnStartCharging = root.findViewById(R.id.btnStartCharging)
        btnCap80 = root.findViewById(R.id.btnCap80)
        btnCap90 = root.findViewById(R.id.btnCap90)
        btnCap100 = root.findViewById(R.id.btnCap100)
        btnCurrent6A = root.findViewById(R.id.btnCurrent6A)
        btnCurrent8A = root.findViewById(R.id.btnCurrent8A)
        btnCurrent10A = root.findViewById(R.id.btnCurrent10A)
        btnCurrent16A = root.findViewById(R.id.btnCurrent16A)
        btnCurrentMax = root.findViewById(R.id.btnCurrentMax)

        // Panel Actions: Sound
        btnAvasDingDong = root.findViewById(R.id.btnAvasDingDong)
        btnAvasTripleBeep = root.findViewById(R.id.btnAvasTripleBeep)
        btnAvasChime = root.findViewById(R.id.btnAvasChime)
        btnAvasAlarm = root.findViewById(R.id.btnAvasAlarm)
        btnAvasStop = root.findViewById(R.id.btnAvasStop)
        switchEngineSound = root.findViewById(R.id.switchEngineSound)
        btnEngineMinus = root.findViewById(R.id.btnEngineMinus)
        tvEnginePreset = root.findViewById(R.id.tvEnginePreset)
        btnEnginePlus = root.findViewById(R.id.btnEnginePlus)

        // Panel Actions: System
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
        // Mode toggle & model picker
        pillModelPicker.setOnClickListener { showModelSelectionDialog() }
        ivVehicleArt.setOnClickListener { showModelSelectionDialog() }
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
        btnFanMinus.setOnClickListener { viewModel.adjustFanSpeed(-1) { showToast(it) } }
        btnFanPlus.setOnClickListener { viewModel.adjustFanSpeed(1) { showToast(it) } }
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
        btnPassengerSeatHeat.setOnClickListener {
            val cur = viewModel.state.value.seats.passengerHeat
            val next = (cur + 1) % 4
            viewModel.setPassengerSeatHeat(next) { showToast(it) }
        }
        btnPassengerSeatCool.setOnClickListener {
            val cur = viewModel.state.value.seats.passengerCool
            val next = (cur + 1) % 4
            viewModel.setPassengerSeatCool(next) { showToast(it) }
        }
        switchSteeringHeat.setOnCheckedChangeListener { _, isChecked ->
            if (switchSteeringHeat.isPressed) {
                viewModel.setSteeringHeat(isChecked) { showToast(it) }
            }
        }
        btnSeatMemory1.setOnClickListener {
            viewModel.recallSeatPosition(1) { success ->
                if (isAdded) {
                    val msg = if (success) getString(R.string.vc_seat_pos_recalled, 1) else getString(R.string.vc_cmd_failed)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnSeatMemory1.setOnLongClickListener {
            viewModel.saveSeatPosition(1) { success ->
                if (isAdded) {
                    val msg = if (success) getString(R.string.vc_seat_pos_saved, 1) else getString(R.string.vc_cmd_failed)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
        btnSeatMemory2.setOnClickListener {
            viewModel.recallSeatPosition(2) { success ->
                if (isAdded) {
                    val msg = if (success) getString(R.string.vc_seat_pos_recalled, 2) else getString(R.string.vc_cmd_failed)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
        btnSeatMemory2.setOnLongClickListener {
            viewModel.saveSeatPosition(2) { success ->
                if (isAdded) {
                    val msg = if (success) getString(R.string.vc_seat_pos_saved, 2) else getString(R.string.vc_cmd_failed)
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            true
        }

        // Action Buttons: Windows
        btnWinAllVent.setOnClickListener { viewModel.ventAllWindows { showToast(it) } }
        btnCloseAll.setOnClickListener { viewModel.closeAllWindows { showToast(it) } }
        btnOpenAll.setOnClickListener { viewModel.openAllWindows { showToast(it) } }

        for (row in windowRows) {
            row.tvName.text = getString(row.nameResId)
            for ((pct, btn) in row.buttons) {
                btn.setOnClickListener {
                    viewModel.setWindowPosition(row.areaNum, pct) { showToast(it) }
                }
            }
        }

        // Action Buttons: Lights
        switchDaytimeLight.setOnCheckedChangeListener { _, isChecked ->
            if (switchDaytimeLight.isPressed) {
                viewModel.setDaytimeLights(isChecked) { showToast(it) }
            }
        }
        sliderAmbientColor.addOnChangeListener { _, value, fromUser ->
            val idx = value.toInt().coerceIn(1, 31)
            val hex = LightConstants.AMBIENT_COLOURS.getOrNull(idx - 1) ?: "#00AAFF"
            viewAmbientPreview.backgroundTintList = ColorStateList.valueOf(Color.parseColor(hex))
            tvAmbientColorValue.text = "Color $idx"
        }
        sliderAmbientColor.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isDraggingAmbient = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isDraggingAmbient = false
                val idx = slider.value.toInt().coerceIn(1, 31)
                viewModel.setAmbientLights(idx) { showToast(it) }
            }
        })

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

        btnCurrent6A.setOnClickListener { viewModel.setAcCurrentLimit(1) { showToast(it) } }
        btnCurrent8A.setOnClickListener { viewModel.setAcCurrentLimit(2) { showToast(it) } }
        btnCurrent10A.setOnClickListener { viewModel.setAcCurrentLimit(3) { showToast(it) } }
        btnCurrent16A.setOnClickListener { viewModel.setAcCurrentLimit(4) { showToast(it) } }
        btnCurrentMax.setOnClickListener { viewModel.setAcCurrentLimit(5) { showToast(it) } }

        // Action Buttons: Sound
        btnAvasDingDong.setOnClickListener { viewModel.setAvasTone(0) { showToast(it) } }
        btnAvasTripleBeep.setOnClickListener { viewModel.setAvasTone(2) { showToast(it) } }
        btnAvasChime.setOnClickListener { viewModel.setAvasTone(4) { showToast(it) } }
        btnAvasAlarm.setOnClickListener { viewModel.setAvasTone(6) { showToast(it) } }
        btnAvasStop.setOnClickListener { viewModel.stopAvasTone { showToast(it) } }

        tvEnginePreset.text = "$currentEnginePreset"
        btnEngineMinus.setOnClickListener {
            if (currentEnginePreset > 1) {
                currentEnginePreset--
                tvEnginePreset.text = "$currentEnginePreset"
                if (switchEngineSound.isChecked) {
                    viewModel.setEngineSound(true, currentEnginePreset) { showToast(it) }
                }
            }
        }
        btnEnginePlus.setOnClickListener {
            if (currentEnginePreset < 5) {
                currentEnginePreset++
                tvEnginePreset.text = "$currentEnginePreset"
                if (switchEngineSound.isChecked) {
                    viewModel.setEngineSound(true, currentEnginePreset) { showToast(it) }
                }
            }
        }
        switchEngineSound.setOnCheckedChangeListener { _, isChecked ->
            if (switchEngineSound.isPressed) {
                viewModel.setEngineSound(isChecked, currentEnginePreset) { showToast(it) }
            }
        }

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

        // 2b. Selected Model Name
        val modelDisplayName = when (state.modelId?.lowercase(Locale.US)?.filter(Char::isLetterOrDigit)) {
            null, "" -> getString(R.string.vc_model_unselected)
            "seal" -> "BYD Seal"
            "sealion7" -> "BYD Sealion 7"
            "shark" -> "BYD Shark"
            "sealu", "seal-u" -> "BYD Seal U"
            "sealudmi", "seal-u-dmi" -> "BYD Seal U DM-i"
            "dolphin" -> "BYD Dolphin"
            "atto3", "atto-3" -> "BYD Atto 3"
            "atto3evo", "atto3-evo" -> "BYD Atto 3 Evo"
            "atto2", "atto-2" -> "BYD Atto 2"
            "han" -> "BYD Han"
            "tang" -> "BYD Tang"
            "m6" -> "BYD M6"
            "seagull" -> "BYD Seagull"
            "destroyer", "destroyer05" -> "BYD Destroyer 05"
            else -> state.modelId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
        tvModelName.text = modelDisplayName

        // 3. Vehicle Hero Art (shows vehicle_fallback silhouette when unset/null)
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
        tvFanSpeed.text = "${getString(R.string.vc_fan_label)} ${state.climate.fanSpeed}"
        switchBatteryHeat.isChecked = state.climate.batteryHeat

        // 7. Seats State
        renderSeatButton(btnDriverSeatHeat, getString(R.string.vc_action_driver_seat), getString(R.string.vc_action_seat_heat), state.seats.driverHeat, isHeat = true)
        renderSeatButton(btnDriverSeatCool, getString(R.string.vc_action_driver_seat), getString(R.string.vc_action_seat_cool), state.seats.driverCool, isHeat = false)
        renderSeatButton(btnPassengerSeatHeat, getString(R.string.vc_action_passenger_seat), getString(R.string.vc_action_seat_heat), state.seats.passengerHeat, isHeat = true)
        renderSeatButton(btnPassengerSeatCool, getString(R.string.vc_action_passenger_seat), getString(R.string.vc_action_seat_cool), state.seats.passengerCool, isHeat = false)
        switchSteeringHeat.isChecked = state.seats.steeringHeat

        // 8. Windows State
        for (row in windowRows) {
            val currentPct = state.windowPercent[row.areaKey]
            if (currentPct != null && currentPct >= 0) {
                row.tvPercent.text = "$currentPct%"
                for ((pct, btn) in row.buttons) {
                    setButtonSelected(btn, currentPct == pct)
                }
            } else {
                row.tvPercent.text = "—%"
                for ((_, btn) in row.buttons) {
                    setButtonSelected(btn, false)
                }
            }
        }

        // 9. Lights State
        switchDaytimeLight.isChecked = state.lights.daytimeLight
        val colorIdx = state.lights.ambientColour.coerceIn(1, 31)
        if (!isDraggingAmbient) {
            sliderAmbientColor.value = colorIdx.toFloat()
            val hex = LightConstants.AMBIENT_COLOURS.getOrNull(colorIdx - 1) ?: "#00AAFF"
            viewAmbientPreview.backgroundTintList = ColorStateList.valueOf(Color.parseColor(hex))
            tvAmbientColorValue.text = "Color $colorIdx"
        }

        // 10. ADAS State
        switchSpeedLimitWarning.isChecked = state.adas.speedLimitWarning
        switchChildPresence.isChecked = state.adas.childPresenceDetection

        // 11. Charging State
        setButtonSelected(btnCap80, state.charging.chargeCapPercent == 80)
        setButtonSelected(btnCap90, state.charging.chargeCapPercent == 90)
        setButtonSelected(btnCap100, state.charging.chargeCapPercent == 100)

        setButtonSelected(btnCurrent6A, state.charging.acCurrentLimitState == 1)
        setButtonSelected(btnCurrent8A, state.charging.acCurrentLimitState == 2)
        setButtonSelected(btnCurrent10A, state.charging.acCurrentLimitState == 3)
        setButtonSelected(btnCurrent16A, state.charging.acCurrentLimitState == 4)
        setButtonSelected(btnCurrentMax, state.charging.acCurrentLimitState == 5)
    }

    private fun renderSeatButton(
        btn: MaterialButton,
        seatLabel: String,
        actionLabel: String,
        level: Int,
        isHeat: Boolean
    ) {
        val context = context ?: return
        val statusText = when (level) {
            0 -> getString(R.string.vc_action_seat_off)
            else -> getString(R.string.vc_action_seat_level, level)
        }
        btn.text = "$seatLabel $actionLabel: $statusText"

        if (level > 0) {
            val colorRes = if (isHeat) R.color.status_warning else R.color.status_info
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
            btn.setTextColor(ContextCompat.getColor(context, R.color.white))
            btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.bg_elevated))
            btn.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            btn.iconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_primary))
        }
    }

    private fun setButtonSelected(btn: MaterialButton, selected: Boolean) {
        val context = btn.context
        if (selected) {
            btn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary))
            btn.setTextColor(ContextCompat.getColor(context, R.color.white))
            btn.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary))
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            btn.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            btn.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.border_default))
        }
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

    private fun showModelSelectionDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val models = viewModel.getAvailableModels()
            val options = mutableListOf<Pair<String?, String>>()
            options.add(null to getString(R.string.vc_model_none))
            for (m in models) {
                options.add(m.first to m.second)
            }

            val titles = options.map { it.second }.toTypedArray()
            val currentModelId = viewModel.state.value.modelId
            var selectedIndex = options.indexOfFirst {
                if (currentModelId.isNullOrEmpty()) it.first == null
                else it.first?.equals(currentModelId, ignoreCase = true) == true
            }
            if (selectedIndex < 0) selectedIndex = 0

            MaterialAlertDialogBuilder(requireContext(), R.style.Theme_Overdrive_M3_Dialog)
                .setTitle(R.string.vc_model_select_title)
                .setSingleChoiceItems(titles, selectedIndex) { dialog, which ->
                    val chosen = options[which].first
                    viewModel.selectModel(chosen) { success ->
                        if (isAdded) {
                            val msg = if (success) {
                                if (chosen == null) getString(R.string.vc_model_cleared)
                                else getString(R.string.vc_model_selected_msg, options[which].second)
                            } else {
                                getString(R.string.vc_cmd_failed)
                            }
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
