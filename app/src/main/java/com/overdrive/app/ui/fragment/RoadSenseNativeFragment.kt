package com.overdrive.app.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.overdrive.app.R
import com.overdrive.app.navmap.RoadSenseMapActivity
import com.overdrive.app.ui.roadsense.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RoadSenseNativeFragment : Fragment() {

    private val viewModel: RoadSenseViewModel by viewModels()

    // Header & Loading
    private lateinit var tvRoadSenseTitle: TextView
    private lateinit var tvRoadSenseSubtitle: TextView
    private lateinit var badgeRoadSenseTopStatus: TextView
    private lateinit var tvRoadSenseBanner: TextView
    private lateinit var progressRoadSenseLoading: ProgressBar

    // Tab Containers
    private lateinit var containerGeneral: NestedScrollView
    private lateinit var containerMap: NestedScrollView
    private lateinit var containerWarnings: NestedScrollView
    private lateinit var containerData: NestedScrollView
    private lateinit var containerBlindSpot: NestedScrollView

    // Bottom Navigation Bar
    private lateinit var tabRoadSenseGeneral: LinearLayout
    private lateinit var ivTabGeneral: ImageView
    private lateinit var tvTabGeneral: TextView

    private lateinit var tabRoadSenseMap: LinearLayout
    private lateinit var ivTabMap: ImageView
    private lateinit var tvTabMap: TextView

    private lateinit var tabRoadSenseWarnings: LinearLayout
    private lateinit var ivTabWarnings: ImageView
    private lateinit var tvTabWarnings: TextView

    private lateinit var tabRoadSenseData: LinearLayout
    private lateinit var ivTabData: ImageView
    private lateinit var tvTabData: TextView

    private lateinit var tabRoadSenseBlindSpot: LinearLayout
    private lateinit var ivTabBlindSpot: ImageView
    private lateinit var tvTabBlindSpot: TextView

    // General Tab Views
    private lateinit var badgeMasterStatus: TextView
    private lateinit var switchRoadSenseEnabled: MaterialSwitch
    private lateinit var tvDetectSensValue: TextView
    private lateinit var sliderDetectSens: Slider
    private lateinit var switchCalibrationMode: MaterialSwitch
    private lateinit var switchOverlayVisible: MaterialSwitch

    // Map Tab Views
    private lateinit var btnOpenHazardMap: MaterialButton
    private lateinit var badgeRoutingStatus: TextView
    private lateinit var etRoutingEndpoint: TextInputEditText
    private lateinit var etRoutingKey: TextInputEditText
    private lateinit var btnClearRouting: MaterialButton
    private lateinit var btnSaveRouting: MaterialButton
    private lateinit var badgeClusterStatus: TextView
    private lateinit var switchClusterProject: MaterialSwitch
    private lateinit var switchClusterAuto: MaterialSwitch
    private lateinit var spinnerClusterLayout: Spinner

    // Warnings Tab Views
    private lateinit var switchWarnEnabled: MaterialSwitch
    private lateinit var btnWarnVisual: MaterialButton
    private lateinit var btnWarnAudio: MaterialButton
    private lateinit var btnWarnBoth: MaterialButton
    private lateinit var spinnerWarnChannel: Spinner
    private lateinit var tvWarnVolumeVal: TextView
    private lateinit var sliderWarnVolume: Slider
    private lateinit var btnTestChime: MaterialButton
    private lateinit var tvWarnLeadVal: TextView
    private lateinit var sliderWarnLead: Slider
    private lateinit var tvWarnConfVal: TextView
    private lateinit var sliderWarnConf: Slider
    private lateinit var switchSevMinor: MaterialSwitch
    private lateinit var switchSevModerate: MaterialSwitch
    private lateinit var switchSevSevere: MaterialSwitch

    // Data Tab Views
    private lateinit var switchCrowdUpload: MaterialSwitch
    private lateinit var switchCrowdDownload: MaterialSwitch
    private lateinit var etSyncWorkerUrl: TextInputEditText
    private lateinit var btnSaveSyncWorker: MaterialButton
    private lateinit var btnDeleteLocal: MaterialButton
    private lateinit var btnDeleteCloud: MaterialButton

    // Blind Spot Tab Views
    private lateinit var switchBsEnabled: MaterialSwitch
    private lateinit var btnBsMergeBoth: MaterialButton
    private lateinit var btnBsMergeSide: MaterialButton
    private lateinit var btnBsMergeRear: MaterialButton
    private lateinit var layoutBsRectify: LinearLayout
    private lateinit var tvBsRectifyVal: TextView
    private lateinit var sliderBsRectify: Slider
    private lateinit var etBsMinSpeed: TextInputEditText
    private lateinit var etBsMaxSpeed: TextInputEditText
    private lateinit var btnApplySpeed: MaterialButton
    private lateinit var switchBsSuppressReverse: MaterialSwitch
    private lateinit var btnBsTargetHeadunit: MaterialButton
    private lateinit var btnBsTargetCluster: MaterialButton
    private lateinit var tvBsSizeVal: TextView
    private lateinit var sliderBsSize: Slider

    private lateinit var btnCornerLTl: MaterialButton
    private lateinit var btnCornerLTr: MaterialButton
    private lateinit var btnCornerLBl: MaterialButton
    private lateinit var btnCornerLBr: MaterialButton
    private lateinit var btnCornerLCenter: MaterialButton

    private lateinit var btnCornerRTl: MaterialButton
    private lateinit var btnCornerRTr: MaterialButton
    private lateinit var btnCornerRBl: MaterialButton
    private lateinit var btnCornerRBr: MaterialButton
    private lateinit var btnCornerRCenter: MaterialButton

    private lateinit var btnPreviewLeft: MaterialButton
    private lateinit var btnPreviewRight: MaterialButton
    private lateinit var btnPreviewHide: MaterialButton

    private lateinit var tvBsRearFovVal: TextView
    private lateinit var sliderBsRearFov: Slider
    private lateinit var tvBsSideFovVal: TextView
    private lateinit var sliderBsSideFov: Slider
    private lateinit var tvBsYawVal: TextView
    private lateinit var sliderBsYaw: Slider
    private lateinit var tvBsRollVal: TextView
    private lateinit var sliderBsRoll: Slider
    private lateinit var tvBsPitchVal: TextView
    private lateinit var sliderBsPitch: Slider
    private lateinit var tvBsFeatherVal: TextView
    private lateinit var sliderBsFeather: Slider

    private lateinit var btnResetAlignment: MaterialButton
    private lateinit var btnSaveAlignment: MaterialButton

    private var isUpdatingUi = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_roadsense_native, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun bindViews(root: View) {
        tvRoadSenseTitle = root.findViewById(R.id.tvRoadSenseTitle)
        tvRoadSenseSubtitle = root.findViewById(R.id.tvRoadSenseSubtitle)
        badgeRoadSenseTopStatus = root.findViewById(R.id.badgeRoadSenseTopStatus)
        tvRoadSenseBanner = root.findViewById(R.id.tvRoadSenseBanner)
        progressRoadSenseLoading = root.findViewById(R.id.progressRoadSenseLoading)

        containerGeneral = root.findViewById(R.id.containerGeneral)
        containerMap = root.findViewById(R.id.containerMap)
        containerWarnings = root.findViewById(R.id.containerWarnings)
        containerData = root.findViewById(R.id.containerData)
        containerBlindSpot = root.findViewById(R.id.containerBlindSpot)

        tabRoadSenseGeneral = root.findViewById(R.id.tabRoadSenseGeneral)
        ivTabGeneral = root.findViewById(R.id.ivTabGeneral)
        tvTabGeneral = root.findViewById(R.id.tvTabGeneral)

        tabRoadSenseMap = root.findViewById(R.id.tabRoadSenseMap)
        ivTabMap = root.findViewById(R.id.ivTabMap)
        tvTabMap = root.findViewById(R.id.tvTabMap)

        tabRoadSenseWarnings = root.findViewById(R.id.tabRoadSenseWarnings)
        ivTabWarnings = root.findViewById(R.id.ivTabWarnings)
        tvTabWarnings = root.findViewById(R.id.tvTabWarnings)

        tabRoadSenseData = root.findViewById(R.id.tabRoadSenseData)
        ivTabData = root.findViewById(R.id.ivTabData)
        tvTabData = root.findViewById(R.id.tvTabData)

        tabRoadSenseBlindSpot = root.findViewById(R.id.tabRoadSenseBlindSpot)
        ivTabBlindSpot = root.findViewById(R.id.ivTabBlindSpot)
        tvTabBlindSpot = root.findViewById(R.id.tvTabBlindSpot)

        // General
        badgeMasterStatus = root.findViewById(R.id.badgeMasterStatus)
        switchRoadSenseEnabled = root.findViewById(R.id.switchRoadSenseEnabled)
        tvDetectSensValue = root.findViewById(R.id.tvDetectSensValue)
        sliderDetectSens = root.findViewById(R.id.sliderDetectSens)
        switchCalibrationMode = root.findViewById(R.id.switchCalibrationMode)
        switchOverlayVisible = root.findViewById(R.id.switchOverlayVisible)

        // Map
        btnOpenHazardMap = root.findViewById(R.id.btnOpenHazardMap)
        badgeRoutingStatus = root.findViewById(R.id.badgeRoutingStatus)
        etRoutingEndpoint = root.findViewById(R.id.etRoutingEndpoint)
        etRoutingKey = root.findViewById(R.id.etRoutingKey)
        btnClearRouting = root.findViewById(R.id.btnClearRouting)
        btnSaveRouting = root.findViewById(R.id.btnSaveRouting)
        badgeClusterStatus = root.findViewById(R.id.badgeClusterStatus)
        switchClusterProject = root.findViewById(R.id.switchClusterProject)
        switchClusterAuto = root.findViewById(R.id.switchClusterAuto)
        spinnerClusterLayout = root.findViewById(R.id.spinnerClusterLayout)

        // Warnings
        switchWarnEnabled = root.findViewById(R.id.switchWarnEnabled)
        btnWarnVisual = root.findViewById(R.id.btnWarnVisual)
        btnWarnAudio = root.findViewById(R.id.btnWarnAudio)
        btnWarnBoth = root.findViewById(R.id.btnWarnBoth)
        spinnerWarnChannel = root.findViewById(R.id.spinnerWarnChannel)
        tvWarnVolumeVal = root.findViewById(R.id.tvWarnVolumeVal)
        sliderWarnVolume = root.findViewById(R.id.sliderWarnVolume)
        btnTestChime = root.findViewById(R.id.btnTestChime)
        tvWarnLeadVal = root.findViewById(R.id.tvWarnLeadVal)
        sliderWarnLead = root.findViewById(R.id.sliderWarnLead)
        tvWarnConfVal = root.findViewById(R.id.tvWarnConfVal)
        sliderWarnConf = root.findViewById(R.id.sliderWarnConf)
        switchSevMinor = root.findViewById(R.id.switchSevMinor)
        switchSevModerate = root.findViewById(R.id.switchSevModerate)
        switchSevSevere = root.findViewById(R.id.switchSevSevere)

        // Data
        switchCrowdUpload = root.findViewById(R.id.switchCrowdUpload)
        switchCrowdDownload = root.findViewById(R.id.switchCrowdDownload)
        etSyncWorkerUrl = root.findViewById(R.id.etSyncWorkerUrl)
        btnSaveSyncWorker = root.findViewById(R.id.btnSaveSyncWorker)
        btnDeleteLocal = root.findViewById(R.id.btnDeleteLocal)
        btnDeleteCloud = root.findViewById(R.id.btnDeleteCloud)

        // Blind Spot
        switchBsEnabled = root.findViewById(R.id.switchBsEnabled)
        btnBsMergeBoth = root.findViewById(R.id.btnBsMergeBoth)
        btnBsMergeSide = root.findViewById(R.id.btnBsMergeSide)
        btnBsMergeRear = root.findViewById(R.id.btnBsMergeRear)
        layoutBsRectify = root.findViewById(R.id.layoutBsRectify)
        tvBsRectifyVal = root.findViewById(R.id.tvBsRectifyVal)
        sliderBsRectify = root.findViewById(R.id.sliderBsRectify)
        etBsMinSpeed = root.findViewById(R.id.etBsMinSpeed)
        etBsMaxSpeed = root.findViewById(R.id.etBsMaxSpeed)
        btnApplySpeed = root.findViewById(R.id.btnApplySpeed)
        switchBsSuppressReverse = root.findViewById(R.id.switchBsSuppressReverse)
        btnBsTargetHeadunit = root.findViewById(R.id.btnBsTargetHeadunit)
        btnBsTargetCluster = root.findViewById(R.id.btnBsTargetCluster)
        tvBsSizeVal = root.findViewById(R.id.tvBsSizeVal)
        sliderBsSize = root.findViewById(R.id.sliderBsSize)

        btnCornerLTl = root.findViewById(R.id.btnCornerLTl)
        btnCornerLTr = root.findViewById(R.id.btnCornerLTr)
        btnCornerLBl = root.findViewById(R.id.btnCornerLBl)
        btnCornerLBr = root.findViewById(R.id.btnCornerLBr)
        btnCornerLCenter = root.findViewById(R.id.btnCornerLCenter)

        btnCornerRTl = root.findViewById(R.id.btnCornerRTl)
        btnCornerRTr = root.findViewById(R.id.btnCornerRTr)
        btnCornerRBl = root.findViewById(R.id.btnCornerRBl)
        btnCornerRBr = root.findViewById(R.id.btnCornerRBr)
        btnCornerRCenter = root.findViewById(R.id.btnCornerRCenter)

        btnPreviewLeft = root.findViewById(R.id.btnPreviewLeft)
        btnPreviewRight = root.findViewById(R.id.btnPreviewRight)
        btnPreviewHide = root.findViewById(R.id.btnPreviewHide)

        tvBsRearFovVal = root.findViewById(R.id.tvBsRearFovVal)
        sliderBsRearFov = root.findViewById(R.id.sliderBsRearFov)
        tvBsSideFovVal = root.findViewById(R.id.tvBsSideFovVal)
        sliderBsSideFov = root.findViewById(R.id.sliderBsSideFov)
        tvBsYawVal = root.findViewById(R.id.tvBsYawVal)
        sliderBsYaw = root.findViewById(R.id.sliderBsYaw)
        tvBsRollVal = root.findViewById(R.id.tvBsRollVal)
        sliderBsRoll = root.findViewById(R.id.sliderBsRoll)
        tvBsPitchVal = root.findViewById(R.id.tvBsPitchVal)
        sliderBsPitch = root.findViewById(R.id.sliderBsPitch)
        tvBsFeatherVal = root.findViewById(R.id.tvBsFeatherVal)
        sliderBsFeather = root.findViewById(R.id.sliderBsFeather)

        btnResetAlignment = root.findViewById(R.id.btnResetAlignment)
        btnSaveAlignment = root.findViewById(R.id.btnSaveAlignment)

        setupSpinners()
    }

    private fun setupSpinners() {
        val clusterProfiles = listOf("10.25\" (Varsayılan)", "12.3\"", "8.8\"")
        val clusterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, clusterProfiles)
        spinnerClusterLayout.adapter = clusterAdapter

        val channels = listOf("navigation (Navigasyon)", "media (Medya)", "voice (Sesli Komut)", "alarm (Alarm)")
        val channelAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, channels)
        spinnerWarnChannel.adapter = channelAdapter
    }

    private fun setupListeners() {
        // Bottom tab clicks
        tabRoadSenseGeneral.setOnClickListener { viewModel.selectTab(RoadSenseTab.GENERAL) }
        tabRoadSenseMap.setOnClickListener { viewModel.selectTab(RoadSenseTab.MAP) }
        tabRoadSenseWarnings.setOnClickListener { viewModel.selectTab(RoadSenseTab.WARNINGS) }
        tabRoadSenseData.setOnClickListener { viewModel.selectTab(RoadSenseTab.DATA) }
        tabRoadSenseBlindSpot.setOnClickListener { viewModel.selectTab(RoadSenseTab.BLIND_SPOT) }

        // General
        switchRoadSenseEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleRoadSenseEnabled(isChecked)
        }
        sliderDetectSens.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tvDetectSensValue.text = "${value.toInt()}%"
                viewModel.updateDetectionSensitivity(value.toInt())
            }
        }
        switchCalibrationMode.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleCalibrationMode(isChecked)
        }
        switchOverlayVisible.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleOverlayVisible(isChecked)
        }

        // Map
        btnOpenHazardMap.setOnClickListener {
            try {
                startActivity(Intent(requireContext(), RoadSenseMapActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Harita açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        btnSaveRouting.setOnClickListener {
            val endpoint = etRoutingEndpoint.text?.toString() ?: ""
            val key = etRoutingKey.text?.toString() ?: ""
            viewModel.saveRouting(endpoint, key)
        }
        btnClearRouting.setOnClickListener {
            etRoutingKey.setText("")
            viewModel.clearRouting()
        }
        switchClusterProject.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleClusterProject(isChecked)
        }
        switchClusterAuto.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleClusterAuto(isChecked)
        }
        spinnerClusterLayout.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingUi) {
                    val code = when (position) {
                        1 -> 30 // 12.3"
                        2 -> 29 // 8.8"
                        else -> 31 // 10.25"
                    }
                    viewModel.setClusterLayout(code)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Warnings
        switchWarnEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleWarnEnabled(isChecked)
        }
        btnWarnVisual.setOnClickListener { viewModel.setWarnMode(WarnMode.VISUAL) }
        btnWarnAudio.setOnClickListener { viewModel.setWarnMode(WarnMode.AUDIO) }
        btnWarnBoth.setOnClickListener { viewModel.setWarnMode(WarnMode.BOTH) }
        spinnerWarnChannel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isUpdatingUi) {
                    val ch = when (position) {
                        1 -> SoundChannel.MEDIA
                        2 -> SoundChannel.VOICE
                        3 -> SoundChannel.ALARM
                        else -> SoundChannel.NAVIGATION
                    }
                    viewModel.setWarnAudioChannel(ch)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        sliderWarnVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tvWarnVolumeVal.text = "${value.toInt()}%"
                viewModel.setWarnAudioVolume(value.toInt())
            }
        }
        btnTestChime.setOnClickListener { viewModel.testChime() }
        sliderWarnLead.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tvWarnLeadVal.text = "${value.toInt()}s"
                viewModel.setWarnLeadSeconds(value.toInt())
            }
        }
        sliderWarnConf.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tvWarnConfVal.text = "${value.toInt()}%"
                viewModel.setWarnConfidenceThreshold(value.toInt())
            }
        }
        switchSevMinor.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleSeverityMinor(isChecked)
        }
        switchSevModerate.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleSeverityModerate(isChecked)
        }
        switchSevSevere.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleSeveritySevere(isChecked)
        }

        // Data
        switchCrowdUpload.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleCrowdUpload(isChecked)
        }
        switchCrowdDownload.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleCrowdDownload(isChecked)
        }
        btnSaveSyncWorker.setOnClickListener {
            val url = etSyncWorkerUrl.text?.toString() ?: ""
            viewModel.setSyncWorkerUrl(url)
        }
        btnDeleteLocal.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.roadsense_delete_local_title)
                .setMessage(R.string.roadsense_confirm_delete_local)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.roadsense_delete_local_btn) { _, _ ->
                    viewModel.deleteLocalCalibrations()
                }
                .show()
        }
        btnDeleteCloud.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.roadsense_delete_cloud_title)
                .setMessage(R.string.roadsense_confirm_delete_cloud)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.roadsense_delete_cloud_btn) { _, _ ->
                    viewModel.deleteCloudCalibrations()
                }
                .show()
        }

        // Blind Spot
        switchBsEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleBlindSpotEnabled(isChecked)
        }
        btnBsMergeBoth.setOnClickListener { viewModel.setBsMergeMode(BsMergeMode.BOTH) }
        btnBsMergeSide.setOnClickListener { viewModel.setBsMergeMode(BsMergeMode.SIDE) }
        btnBsMergeRear.setOnClickListener { viewModel.setBsMergeMode(BsMergeMode.REAR) }
        sliderBsRectify.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tvBsRectifyVal.text = "${value.toInt()}"
                viewModel.setBsRectifyStrength(value.toInt())
            }
        }
        btnApplySpeed.setOnClickListener {
            val lo = etBsMinSpeed.text?.toString()?.toIntOrNull() ?: 0
            val hi = etBsMaxSpeed.text?.toString()?.toIntOrNull() ?: 0
            viewModel.setBsSpeedRange(lo, hi)
        }
        switchBsSuppressReverse.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingUi) viewModel.toggleBsSuppressReverse(isChecked)
        }
        btnBsTargetHeadunit.setOnClickListener { viewModel.setBsDisplayTarget(BsDisplayTarget.HEAD_UNIT) }
        btnBsTargetCluster.setOnClickListener { viewModel.setBsDisplayTarget(BsDisplayTarget.CLUSTER) }
        sliderBsSize.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                tvBsSizeVal.text = "${value.toInt()}%"
                viewModel.setBsSizePct(value.toInt())
            }
        }

        // Corner Left
        btnCornerLTl.setOnClickListener { viewModel.setBsCorner("left", "tl") }
        btnCornerLTr.setOnClickListener { viewModel.setBsCorner("left", "tr") }
        btnCornerLBl.setOnClickListener { viewModel.setBsCorner("left", "bl") }
        btnCornerLBr.setOnClickListener { viewModel.setBsCorner("left", "br") }
        btnCornerLCenter.setOnClickListener { viewModel.setBsCorner("left", "center") }

        // Corner Right
        btnCornerRTl.setOnClickListener { viewModel.setBsCorner("right", "tl") }
        btnCornerRTr.setOnClickListener { viewModel.setBsCorner("right", "tr") }
        btnCornerRBl.setOnClickListener { viewModel.setBsCorner("right", "bl") }
        btnCornerRBr.setOnClickListener { viewModel.setBsCorner("right", "br") }
        btnCornerRCenter.setOnClickListener { viewModel.setBsCorner("right", "center") }

        // Preview
        btnPreviewLeft.setOnClickListener { viewModel.setBsPreview(7, true) }
        btnPreviewRight.setOnClickListener { viewModel.setBsPreview(8, true) }
        btnPreviewHide.setOnClickListener { viewModel.setBsPreview(0, false) }

        // Alignment Sliders
        sliderBsRearFov.addOnChangeListener { _, value, _ -> tvBsRearFovVal.text = String.format("%.2f", value) }
        sliderBsSideFov.addOnChangeListener { _, value, _ -> tvBsSideFovVal.text = String.format("%.2f", value) }
        sliderBsYaw.addOnChangeListener { _, value, _ -> tvBsYawVal.text = String.format("%.2f", value) }
        sliderBsRoll.addOnChangeListener { _, value, _ -> tvBsRollVal.text = String.format("%.3f", value) }
        sliderBsPitch.addOnChangeListener { _, value, _ -> tvBsPitchVal.text = String.format("%.3f", value) }
        sliderBsFeather.addOnChangeListener { _, value, _ -> tvBsFeatherVal.text = String.format("%.2f", value) }

        btnSaveAlignment.setOnClickListener {
            viewModel.setBsAlignment(
                rearFov = sliderBsRearFov.value,
                sideFov = sliderBsSideFov.value,
                yaw = sliderBsYaw.value,
                roll = sliderBsRoll.value,
                pitch = sliderBsPitch.value,
                feather = sliderBsFeather.value,
                projExp = 1.0f,
                rearRoll = 0.0f,
                rearPitch = 0.0f
            )
        }
        btnResetAlignment.setOnClickListener { viewModel.resetBsAlignmentDefaults() }

        tvRoadSenseBanner.setOnClickListener { viewModel.dismissBanner() }
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

    private fun renderUi(state: RoadSenseUiState) {
        isUpdatingUi = true
        try {
            // Loading indicator
            progressRoadSenseLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Banner
            if (state.bannerMessage != null) {
                tvRoadSenseBanner.visibility = View.VISIBLE
                tvRoadSenseBanner.text = state.bannerMessage
                tvRoadSenseBanner.setBackgroundResource(
                    if (state.isBannerError) R.drawable.bg_status_badge_inactive
                    else R.drawable.bg_status_badge_active
                )
            } else {
                tvRoadSenseBanner.visibility = View.GONE
            }

            // Top Status Badge
            val masterOn = state.general.enabled
            badgeRoadSenseTopStatus.text = if (masterOn) getString(R.string.status_on) else getString(R.string.status_off)
            badgeRoadSenseTopStatus.setBackgroundResource(
                if (masterOn) R.drawable.bg_status_badge_active else R.drawable.bg_status_badge_inactive
            )
            badgeRoadSenseTopStatus.setTextColor(
                if (masterOn) ContextCompat.getColor(requireContext(), R.color.brand_primary)
                else ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )

            // Switch active tab view container
            containerGeneral.visibility = if (state.activeTab == RoadSenseTab.GENERAL) View.VISIBLE else View.GONE
            containerMap.visibility = if (state.activeTab == RoadSenseTab.MAP) View.VISIBLE else View.GONE
            containerWarnings.visibility = if (state.activeTab == RoadSenseTab.WARNINGS) View.VISIBLE else View.GONE
            containerData.visibility = if (state.activeTab == RoadSenseTab.DATA) View.VISIBLE else View.GONE
            containerBlindSpot.visibility = if (state.activeTab == RoadSenseTab.BLIND_SPOT) View.VISIBLE else View.GONE

            updateTabStyles(state.activeTab)

            // 1. General Tab
            badgeMasterStatus.text = if (masterOn) getString(R.string.status_on) else getString(R.string.status_off)
            badgeMasterStatus.setBackgroundResource(
                if (masterOn) R.drawable.bg_status_badge_active else R.drawable.bg_status_badge_inactive
            )
            badgeMasterStatus.setTextColor(
                if (masterOn) ContextCompat.getColor(requireContext(), R.color.brand_primary)
                else ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
            switchRoadSenseEnabled.isChecked = masterOn
            tvDetectSensValue.text = "${state.general.detectionSensitivityPct}%"
            sliderDetectSens.value = state.general.detectionSensitivityPct.toFloat()
            switchCalibrationMode.isChecked = state.general.calibrationMode
            switchOverlayVisible.isChecked = state.general.overlayVisible

            // 2. Map Tab
            val routingConfigured = state.map.routingConfigured
            badgeRoutingStatus.text = if (routingConfigured) getString(R.string.roadsense_routing_configured) else getString(R.string.roadsense_routing_not_configured)
            badgeRoutingStatus.setBackgroundResource(
                if (routingConfigured) R.drawable.bg_status_badge_active else R.drawable.bg_status_badge_inactive
            )
            badgeRoutingStatus.setTextColor(
                if (routingConfigured) ContextCompat.getColor(requireContext(), R.color.brand_primary)
                else ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
            if (etRoutingEndpoint.text.isNullOrEmpty()) {
                etRoutingEndpoint.setText(state.map.routingEndpoint)
            }

            val clusterProjecting = state.map.clusterProjecting
            badgeClusterStatus.text = if (clusterProjecting) getString(R.string.status_on) else getString(R.string.status_off)
            badgeClusterStatus.setBackgroundResource(
                if (clusterProjecting) R.drawable.bg_status_badge_active else R.drawable.bg_status_badge_inactive
            )
            badgeClusterStatus.setTextColor(
                if (clusterProjecting) ContextCompat.getColor(requireContext(), R.color.brand_primary)
                else ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
            switchClusterProject.isChecked = clusterProjecting
            switchClusterAuto.isChecked = state.map.autoProjectCluster
            val clusterSpinnerIdx = when (state.map.clusterLayout) {
                30 -> 1 // 12.3"
                29 -> 2 // 8.8"
                else -> 0 // 10.25"
            }
            if (spinnerClusterLayout.selectedItemPosition != clusterSpinnerIdx) {
                spinnerClusterLayout.setSelection(clusterSpinnerIdx)
            }

            // 3. Warnings Tab
            switchWarnEnabled.isChecked = state.warnings.warnEnabled
            highlightWarnMode(state.warnings.warnMode)
            val channelIdx = when (state.warnings.warnAudioChannel) {
                SoundChannel.MEDIA -> 1
                SoundChannel.VOICE -> 2
                SoundChannel.ALARM -> 3
                else -> 0
            }
            if (spinnerWarnChannel.selectedItemPosition != channelIdx) {
                spinnerWarnChannel.setSelection(channelIdx)
            }
            tvWarnVolumeVal.text = "${state.warnings.warnAudioVolume}%"
            sliderWarnVolume.value = state.warnings.warnAudioVolume.toFloat()
            tvWarnLeadVal.text = "${state.warnings.warnLeadSeconds}s"
            sliderWarnLead.value = state.warnings.warnLeadSeconds.toFloat()
            tvWarnConfVal.text = "${state.warnings.warnConfidenceThreshold}%"
            sliderWarnConf.value = state.warnings.warnConfidenceThreshold.toFloat()
            switchSevMinor.isChecked = state.warnings.severityMinor
            switchSevModerate.isChecked = state.warnings.severityModerate
            switchSevSevere.isChecked = state.warnings.severitySevere

            // 4. Data Tab
            switchCrowdUpload.isChecked = state.data.crowdUpload
            switchCrowdDownload.isChecked = state.data.crowdDownload
            if (etSyncWorkerUrl.text.isNullOrEmpty()) {
                etSyncWorkerUrl.setText(state.data.syncWorkerUrl)
            }

            // 5. Blind Spot Tab
            switchBsEnabled.isChecked = state.blindSpot.enabled
            highlightMergeMode(state.blindSpot.mergeMode)
            layoutBsRectify.visibility = if (state.blindSpot.mergeMode != BsMergeMode.BOTH) View.VISIBLE else View.GONE
            tvBsRectifyVal.text = "${state.blindSpot.rectifyStrength}"
            sliderBsRectify.value = state.blindSpot.rectifyStrength.toFloat()

            if (etBsMinSpeed.text.isNullOrEmpty() || etBsMinSpeed.text.toString() == "0") {
                etBsMinSpeed.setText("${state.blindSpot.minSpeedKmh}")
            }
            if (etBsMaxSpeed.text.isNullOrEmpty() || etBsMaxSpeed.text.toString() == "0") {
                etBsMaxSpeed.setText("${state.blindSpot.maxSpeedKmh}")
            }
            switchBsSuppressReverse.isChecked = state.blindSpot.suppressInReverse

            highlightTarget(state.blindSpot.target)
            tvBsSizeVal.text = "${state.blindSpot.sizePct}%"
            sliderBsSize.value = state.blindSpot.sizePct.toFloat()

            highlightCorner("left", state.blindSpot.cornerLeft)
            highlightCorner("right", state.blindSpot.cornerRight)

            // Alignment values
            tvBsRearFovVal.text = String.format("%.2f", state.blindSpot.rearFov)
            sliderBsRearFov.value = state.blindSpot.rearFov.coerceIn(1.0f, 2.2f)

            tvBsSideFovVal.text = String.format("%.2f", state.blindSpot.sideFov)
            sliderBsSideFov.value = state.blindSpot.sideFov.coerceIn(1.0f, 2.2f)

            tvBsYawVal.text = String.format("%.2f", state.blindSpot.yaw)
            sliderBsYaw.value = state.blindSpot.yaw.coerceIn(0.0f, 1.4f)

            tvBsRollVal.text = String.format("%.3f", state.blindSpot.roll)
            sliderBsRoll.value = state.blindSpot.roll.coerceIn(-0.4f, 0.4f)

            tvBsPitchVal.text = String.format("%.3f", state.blindSpot.pitch)
            sliderBsPitch.value = state.blindSpot.pitch.coerceIn(-0.4f, 0.4f)

            tvBsFeatherVal.text = String.format("%.2f", state.blindSpot.feather)
            sliderBsFeather.value = state.blindSpot.feather.coerceIn(0.0f, 1.0f)
        } finally {
            isUpdatingUi = false
        }
    }

    private fun updateTabStyles(activeTab: RoadSenseTab) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        val outlineColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        val onSurfaceColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val onSurfaceVariantColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

        fun styleTab(iv: ImageView, tv: TextView, isActive: Boolean) {
            iv.setColorFilter(if (isActive) primaryColor else outlineColor)
            tv.setTextColor(if (isActive) onSurfaceColor else onSurfaceVariantColor)
            tv.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        }

        styleTab(ivTabGeneral, tvTabGeneral, activeTab == RoadSenseTab.GENERAL)
        styleTab(ivTabMap, tvTabMap, activeTab == RoadSenseTab.MAP)
        styleTab(ivTabWarnings, tvTabWarnings, activeTab == RoadSenseTab.WARNINGS)
        styleTab(ivTabData, tvTabData, activeTab == RoadSenseTab.DATA)
        styleTab(ivTabBlindSpot, tvTabBlindSpot, activeTab == RoadSenseTab.BLIND_SPOT)
    }

    private fun highlightWarnMode(mode: WarnMode) {
        val primary = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        fun setBtn(btn: MaterialButton, active: Boolean) {
            if (active) {
                btn.setBackgroundColor(primary)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
        setBtn(btnWarnVisual, mode == WarnMode.VISUAL)
        setBtn(btnWarnAudio, mode == WarnMode.AUDIO)
        setBtn(btnWarnBoth, mode == WarnMode.BOTH)
    }

    private fun highlightMergeMode(mode: BsMergeMode) {
        val primary = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        fun setBtn(btn: MaterialButton, active: Boolean) {
            if (active) {
                btn.setBackgroundColor(primary)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
        setBtn(btnBsMergeBoth, mode == BsMergeMode.BOTH)
        setBtn(btnBsMergeSide, mode == BsMergeMode.SIDE)
        setBtn(btnBsMergeRear, mode == BsMergeMode.REAR)
    }

    private fun highlightTarget(target: BsDisplayTarget) {
        val primary = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        fun setBtn(btn: MaterialButton, active: Boolean) {
            if (active) {
                btn.setBackgroundColor(primary)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
        setBtn(btnBsTargetHeadunit, target == BsDisplayTarget.HEAD_UNIT)
        setBtn(btnBsTargetCluster, target == BsDisplayTarget.CLUSTER)
    }

    private fun highlightCorner(side: String, corner: String) {
        val primary = ContextCompat.getColor(requireContext(), R.color.brand_primary)
        val isLeft = side.equals("left", true)
        val buttons = if (isLeft) {
            mapOf("tl" to btnCornerLTl, "tr" to btnCornerLTr, "bl" to btnCornerLBl, "br" to btnCornerLBr, "center" to btnCornerLCenter)
        } else {
            mapOf("tl" to btnCornerRTl, "tr" to btnCornerRTr, "bl" to btnCornerRBl, "br" to btnCornerRBr, "center" to btnCornerRCenter)
        }

        buttons.forEach { (key, btn) ->
            val active = key.equals(corner, true)
            if (active) {
                btn.setBackgroundColor(primary)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
    }
}
