package com.overdrive.app.ui.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.overdrive.app.R
import com.overdrive.app.ui.keymapping.AppInfo
import com.overdrive.app.ui.keymapping.CuratedActionDef
import com.overdrive.app.ui.keymapping.KeyAction
import com.overdrive.app.ui.keymapping.KeyBinding
import com.overdrive.app.ui.keymapping.KeyBindingsAdapter
import com.overdrive.app.ui.keymapping.KeyMappingConstants
import com.overdrive.app.ui.keymapping.KeyMappingTab
import com.overdrive.app.ui.keymapping.KeyMappingViewModel
import com.overdrive.app.ui.keymapping.KnownButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeyMappingNativeFragment : Fragment() {

    private val viewModel: KeyMappingViewModel by viewModels()

    // Header & Bottom Tabs
    private lateinit var tvMasterStatusBadge: TextView
    private lateinit var tabBottomBindings: LinearLayout
    private lateinit var ivBottomTabBindings: ImageView
    private lateinit var tvBottomTabBindings: TextView
    private lateinit var tabBottomAdd: LinearLayout
    private lateinit var ivBottomTabAdd: ImageView
    private lateinit var tvBottomTabAdd: TextView

    // Cluster Size
    private lateinit var spinnerClusterSize: Spinner
    data class ClusterOption(val profile: Int, val labelRes: Int)
    private val clusterOptions = listOf(
        ClusterOption(31, R.string.keymap_cluster_1025),
        ClusterOption(30, R.string.keymap_cluster_123),
        ClusterOption(29, R.string.keymap_cluster_88)
    )

    // Tab Layouts
    private lateinit var layoutBindingsTab: NestedScrollView
    private lateinit var layoutAddTab: NestedScrollView

    // Bindings Tab Views
    private lateinit var cardA11yWarning: MaterialCardView
    private lateinit var btnOpenA11y: MaterialButton
    private lateinit var switchMasterEnable: MaterialSwitch
    private lateinit var sliderDoubleTap: Slider
    private lateinit var tvDoubleTapValue: TextView
    private lateinit var switchAllowAdvanced: MaterialSwitch
    private lateinit var tvBindingsCount: TextView
    private lateinit var btnAddBindingTop: MaterialButton
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var btnEmptyAdd: MaterialButton
    private lateinit var rvBindings: RecyclerView
    private lateinit var bindingsAdapter: KeyBindingsAdapter

    // Add Tab Views
    private lateinit var spinnerKnownButton: Spinner
    private lateinit var btnCaptureToggle: MaterialButton
    private lateinit var layoutCaptureBox: LinearLayout
    private lateinit var tvCaptureCode: TextView
    private lateinit var tvCaptureHint: TextView
    private lateinit var etManualKeycode: EditText
    private lateinit var spinnerPressType: Spinner
    private lateinit var layoutBlockNative: LinearLayout
    private lateinit var switchBlockNativeSingle: MaterialSwitch
    private lateinit var spinnerActionKind: Spinner
    private lateinit var layoutVehicleActionParams: LinearLayout
    private lateinit var spinnerCuratedAction: Spinner
    private lateinit var tvPayloadLabel: TextView
    private lateinit var spinnerPayload: Spinner
    private lateinit var layoutOpenAppParams: LinearLayout
    private lateinit var spinnerOpenApp: Spinner
    private lateinit var switchOpenAppSplit: MaterialSwitch
    private lateinit var layoutShellParams: LinearLayout
    private lateinit var etShellCmd: EditText
    private lateinit var btnAddStep: MaterialButton
    private lateinit var layoutSequenceSteps: LinearLayout
    private lateinit var btnSaveBinding: MaterialButton

    // Progress
    private lateinit var progressBarLoading: ProgressBar

    // Local form state for sequences
    private val sequenceSteps = mutableListOf<KeyAction>()
    private var isProgrammaticUpdate = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_key_mapping_native, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupTabs()
        setupRecycler()
        setupAddForm()
        observeState()
    }

    private fun initViews(v: View) {
        tvMasterStatusBadge = v.findViewById(R.id.tvMasterStatusBadge)
        tabBottomBindings = v.findViewById(R.id.tabBottomBindings)
        ivBottomTabBindings = v.findViewById(R.id.ivBottomTabBindings)
        tvBottomTabBindings = v.findViewById(R.id.tvBottomTabBindings)
        tabBottomAdd = v.findViewById(R.id.tabBottomAdd)
        ivBottomTabAdd = v.findViewById(R.id.ivBottomTabAdd)
        tvBottomTabAdd = v.findViewById(R.id.tvBottomTabAdd)
        spinnerClusterSize = v.findViewById(R.id.spinnerClusterSize)

        layoutBindingsTab = v.findViewById(R.id.layoutBindingsTab)
        layoutAddTab = v.findViewById(R.id.layoutAddTab)

        cardA11yWarning = v.findViewById(R.id.cardA11yWarning)
        btnOpenA11y = v.findViewById(R.id.btnOpenA11y)
        switchMasterEnable = v.findViewById(R.id.switchMasterEnable)
        sliderDoubleTap = v.findViewById(R.id.sliderDoubleTap)
        tvDoubleTapValue = v.findViewById(R.id.tvDoubleTapValue)
        switchAllowAdvanced = v.findViewById(R.id.switchAllowAdvanced)
        tvBindingsCount = v.findViewById(R.id.tvBindingsCount)
        btnAddBindingTop = v.findViewById(R.id.btnAddBindingTop)
        layoutEmptyState = v.findViewById(R.id.layoutEmptyState)
        btnEmptyAdd = v.findViewById(R.id.btnEmptyAdd)
        rvBindings = v.findViewById(R.id.rvBindings)

        spinnerKnownButton = v.findViewById(R.id.spinnerKnownButton)
        btnCaptureToggle = v.findViewById(R.id.btnCaptureToggle)
        layoutCaptureBox = v.findViewById(R.id.layoutCaptureBox)
        tvCaptureCode = v.findViewById(R.id.tvCaptureCode)
        tvCaptureHint = v.findViewById(R.id.tvCaptureHint)
        etManualKeycode = v.findViewById(R.id.etManualKeycode)
        spinnerPressType = v.findViewById(R.id.spinnerPressType)
        layoutBlockNative = v.findViewById(R.id.layoutBlockNative)
        switchBlockNativeSingle = v.findViewById(R.id.switchBlockNativeSingle)
        spinnerActionKind = v.findViewById(R.id.spinnerActionKind)
        layoutVehicleActionParams = v.findViewById(R.id.layoutVehicleActionParams)
        spinnerCuratedAction = v.findViewById(R.id.spinnerCuratedAction)
        tvPayloadLabel = v.findViewById(R.id.tvPayloadLabel)
        spinnerPayload = v.findViewById(R.id.spinnerPayload)
        layoutOpenAppParams = v.findViewById(R.id.layoutOpenAppParams)
        spinnerOpenApp = v.findViewById(R.id.spinnerOpenApp)
        switchOpenAppSplit = v.findViewById(R.id.switchOpenAppSplit)
        layoutShellParams = v.findViewById(R.id.layoutShellParams)
        etShellCmd = v.findViewById(R.id.etShellCmd)
        btnAddStep = v.findViewById(R.id.btnAddStep)
        layoutSequenceSteps = v.findViewById(R.id.layoutSequenceSteps)
        btnSaveBinding = v.findViewById(R.id.btnSaveBinding)

        progressBarLoading = v.findViewById(R.id.progressBarLoading)

        setupClusterSizeSpinner()

        // Accessibility Settings Button
        btnOpenA11y.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                showToast("Cannot open accessibility settings: ${e.message}")
            }
        }

        // Master Enable
        switchMasterEnable.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticUpdate) {
                viewModel.toggleMasterEnabled(isChecked)
            }
        }

        // Allow Advanced
        switchAllowAdvanced.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticUpdate) {
                viewModel.toggleAllowAdvanced(isChecked)
            }
        }

        // Double tap slider
        sliderDoubleTap.addOnChangeListener { _, value, fromUser ->
            val ms = value.toLong()
            tvDoubleTapValue.text = String.format("%.2fs", ms / 1000.0)
            if (fromUser) {
                viewModel.updateDoubleTapWindowMs(ms)
            }
        }

        btnAddBindingTop.setOnClickListener {
            viewModel.selectTab(KeyMappingTab.ADD)
        }
        btnEmptyAdd.setOnClickListener {
            viewModel.selectTab(KeyMappingTab.ADD)
        }
    }

    private fun setupClusterSizeSpinner() {
        val clusterAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            clusterOptions.map { getString(it.labelRes) }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerClusterSize.adapter = clusterAdapter
        spinnerClusterSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isProgrammaticUpdate && position in clusterOptions.indices) {
                    viewModel.updateClusterSizeProfile(clusterOptions[position].profile)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTabs() {
        tabBottomBindings.setOnClickListener {
            viewModel.selectTab(KeyMappingTab.BINDINGS)
        }
        tabBottomAdd.setOnClickListener {
            viewModel.selectTab(KeyMappingTab.ADD)
        }
    }

    private fun updateTabSelection(tab: KeyMappingTab) {
        val ctx = context ?: return
        val brandColor = ContextCompat.getColor(ctx, R.color.brand_primary)
        val mutedColor = Color.parseColor("#94A3B8")

        if (tab == KeyMappingTab.BINDINGS) {
            tabBottomBindings.setBackgroundResource(R.drawable.bg_bottom_tab_active)
            ivBottomTabBindings.imageTintList = ColorStateList.valueOf(brandColor)
            tvBottomTabBindings.setTextColor(brandColor)
            tvBottomTabBindings.setTypeface(null, Typeface.BOLD)

            tabBottomAdd.setBackgroundColor(Color.TRANSPARENT)
            ivBottomTabAdd.imageTintList = ColorStateList.valueOf(mutedColor)
            tvBottomTabAdd.setTextColor(mutedColor)
            tvBottomTabAdd.setTypeface(null, Typeface.NORMAL)

            layoutBindingsTab.visibility = View.VISIBLE
            layoutAddTab.visibility = View.GONE
        } else {
            tabBottomBindings.setBackgroundColor(Color.TRANSPARENT)
            ivBottomTabBindings.imageTintList = ColorStateList.valueOf(mutedColor)
            tvBottomTabBindings.setTextColor(mutedColor)
            tvBottomTabBindings.setTypeface(null, Typeface.NORMAL)

            tabBottomAdd.setBackgroundResource(R.drawable.bg_bottom_tab_active)
            ivBottomTabAdd.imageTintList = ColorStateList.valueOf(brandColor)
            tvBottomTabAdd.setTextColor(brandColor)
            tvBottomTabAdd.setTypeface(null, Typeface.BOLD)

            layoutBindingsTab.visibility = View.GONE
            layoutAddTab.visibility = View.VISIBLE
        }
    }

    private fun setupRecycler() {
        bindingsAdapter = KeyBindingsAdapter(
            onToggleEnabled = { binding, enabled ->
                viewModel.toggleBindingEnabled(binding, enabled)
            },
            onTestClick = { action ->
                viewModel.testAction(action)
            },
            onDeleteClick = { binding ->
                showDeleteDialog(binding)
            }
        )
        rvBindings.layoutManager = LinearLayoutManager(requireContext())
        rvBindings.adapter = bindingsAdapter
    }

    private fun setupAddForm() {
        // 1. Hardware Buttons
        val knownNames = KeyMappingConstants.KNOWN_BUTTONS.map { "[${it.code}] ${it.name}" } + listOf("Custom / Capture Button...")
        val buttonAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, knownNames)
        spinnerKnownButton.adapter = buttonAdapter

        spinnerKnownButton.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < KeyMappingConstants.KNOWN_BUTTONS.size) {
                    val known = KeyMappingConstants.KNOWN_BUTTONS[position]
                    viewModel.setManualKeycode(known.code)
                    etManualKeycode.setText(known.code.toString())
                    // Set allowed press types
                    updatePressTypeOptions(known.allowedPressTypes)
                } else {
                    // Custom
                    updatePressTypeOptions(listOf("single", "double", "long"))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Capture Button
        btnCaptureToggle.setOnClickListener {
            if (viewModel.isCapturing.value) {
                viewModel.stopCapture()
            } else {
                viewModel.startCapture()
            }
        }

        // 2. Press Types
        updatePressTypeOptions(listOf("single", "double", "long"))

        spinnerPressType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = spinnerPressType.selectedItem?.toString()?.lowercase() ?: ""
                layoutBlockNative.visibility = if (selected.contains("double")) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 3. Action Kind
        val actionKinds = listOf("Vehicle Action", "Open App", "Shell Command")
        val kindAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, actionKinds)
        spinnerActionKind.adapter = kindAdapter

        spinnerActionKind.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> { // Vehicle Action
                        layoutVehicleActionParams.visibility = View.VISIBLE
                        layoutOpenAppParams.visibility = View.GONE
                        layoutShellParams.visibility = View.GONE
                    }
                    1 -> { // Open App
                        layoutVehicleActionParams.visibility = View.GONE
                        layoutOpenAppParams.visibility = View.VISIBLE
                        layoutShellParams.visibility = View.GONE
                    }
                    2 -> { // Shell Command
                        layoutVehicleActionParams.visibility = View.GONE
                        layoutOpenAppParams.visibility = View.GONE
                        layoutShellParams.visibility = View.VISIBLE
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Curated actions
        val curatedNames = KeyMappingConstants.CURATED_ACTIONS.map { it.name }
        val curatedAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, curatedNames)
        spinnerCuratedAction.adapter = curatedAdapter

        spinnerCuratedAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val def = KeyMappingConstants.CURATED_ACTIONS.getOrNull(position)
                if (def != null && def.payloads.isNotEmpty()) {
                    tvPayloadLabel.visibility = View.VISIBLE
                    spinnerPayload.visibility = View.VISIBLE
                    val payloadLabels = def.payloads.map { it.second }
                    val payloadAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, payloadLabels)
                    spinnerPayload.adapter = payloadAdapter
                } else {
                    tvPayloadLabel.visibility = View.GONE
                    spinnerPayload.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Add Step to Sequence Button
        btnAddStep.setOnClickListener {
            val step = buildCurrentAction()
            if (step != null) {
                sequenceSteps.add(step)
                renderSequenceSteps()
                showToast("Step added: ${step.displaySummary}")
            }
        }

        // Save Binding Button
        btnSaveBinding.setOnClickListener {
            val keycodeStr = etManualKeycode.text.toString().trim()
            val keycode = keycodeStr.toIntOrNull()
            if (keycode == null || keycode <= 0) {
                showToast("Please select or capture a valid keycode")
                return@setOnClickListener
            }

            val pressType = when (spinnerPressType.selectedItemPosition) {
                1 -> "double"
                2 -> "long"
                else -> "single"
            }

            val action: KeyAction = if (sequenceSteps.isNotEmpty()) {
                val pendingAction = buildCurrentAction()
                val finalSteps = if (pendingAction != null) sequenceSteps + pendingAction else sequenceSteps
                if (finalSteps.size == 1) finalSteps[0]
                else KeyAction(kind = "sequence", steps = finalSteps)
            } else {
                val singleAction = buildCurrentAction()
                if (singleAction == null) {
                    showToast("Please choose an action for this binding")
                    return@setOnClickListener
                }
                singleAction
            }

            val binding = KeyBinding(
                keycode = keycode,
                pressType = pressType,
                enabled = true,
                blockNativeSingle = switchBlockNativeSingle.isChecked && pressType == "double",
                action = action
            )

            viewModel.saveBinding(binding) { success ->
                if (success) {
                    sequenceSteps.clear()
                    renderSequenceSteps()
                }
            }
        }
    }

    private fun updatePressTypeOptions(options: List<String>) {
        val readable = options.map {
            when (it) {
                "double" -> "Double Press"
                "long" -> "Long Press"
                else -> "Single Press"
            }
        }
        val ptAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, readable)
        spinnerPressType.adapter = ptAdapter
    }

    private fun buildCurrentAction(): KeyAction? {
        return when (spinnerActionKind.selectedItemPosition) {
            0 -> { // Vehicle Action
                val curPos = spinnerCuratedAction.selectedItemPosition
                val def = KeyMappingConstants.CURATED_ACTIONS.getOrNull(curPos) ?: return null
                val payloadVal = if (def.payloads.isNotEmpty()) {
                    val pPos = spinnerPayload.selectedItemPosition
                    def.payloads.getOrNull(pPos)?.first ?: ""
                } else null

                if (def.kind == "manualClip") {
                    KeyAction(kind = "manualClip", beforeSeconds = 30, afterSeconds = 0)
                } else if (def.kind == "api") {
                    val subPath = def.path?.replace("\${v}", payloadVal ?: "")
                    val subBody = def.body?.replace("\${v}", payloadVal ?: "")
                    KeyAction(
                        kind = "api",
                        id = def.id,
                        method = def.method,
                        path = subPath,
                        body = subBody,
                        payload = payloadVal
                    )
                } else if (def.kind == "vehicle") {
                    KeyAction(kind = "vehicle", action = def.key)
                } else {
                    KeyAction(kind = "catalog", key = def.key, sub = def.sub, payload = payloadVal)
                }
            }
            1 -> { // Open App
                val appList = viewModel.installedApps.value
                val pos = spinnerOpenApp.selectedItemPosition
                val app = appList.getOrNull(pos) ?: return null
                KeyAction(
                    kind = "openApp",
                    packageName = app.packageName,
                    label = app.label,
                    split = switchOpenAppSplit.isChecked
                )
            }
            2 -> { // Shell Command
                val cmd = etShellCmd.text.toString().trim()
                if (cmd.isBlank()) {
                    showToast("Please enter a shell command")
                    return null
                }
                KeyAction(kind = "shell", cmd = cmd)
            }
            else -> null
        }
    }

    private fun renderSequenceSteps() {
        layoutSequenceSteps.removeAllViews()
        if (sequenceSteps.isEmpty()) {
            return
        }
        sequenceSteps.forEachIndexed { index, step ->
            val stepView = TextView(requireContext()).apply {
                text = "${index + 1}. ${step.displaySummary}"
                textSize = 13f
                setPadding(12, 8, 12, 8)
                setBackgroundResource(R.drawable.bg_automation_logic_box)
            }
            layoutSequenceSteps.addView(stepView)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTab.collectLatest { tab ->
                updateTabSelection(tab)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.config.collectLatest { config ->
                if (config == null) return@collectLatest
                isProgrammaticUpdate = true
                switchMasterEnable.isChecked = config.enabled
                switchAllowAdvanced.isChecked = config.allowAdvanced
                sliderDoubleTap.value = config.doubleTapWindowMs.coerceIn(250L, 1500L).toFloat()
                tvDoubleTapValue.text = String.format("%.2fs", config.doubleTapWindowMs / 1000.0)

                tvMasterStatusBadge.text = if (config.enabled) "ACTIVE" else "OFF"
                tvMasterStatusBadge.setBackgroundResource(
                    if (config.enabled) R.drawable.bg_status_badge_active
                    else R.drawable.bg_keymap_pill
                )

                // Accessibility check: show warning card if not bound
                cardA11yWarning.visibility = if (config.enabled && !config.a11yBound) View.VISIBLE else View.GONE

                // Update Bindings List
                bindingsAdapter.submitList(config.bindings)
                tvBindingsCount.text = "${config.bindings.size} Key Bindings configured"
                layoutEmptyState.visibility = if (config.bindings.isEmpty()) View.VISIBLE else View.GONE
                rvBindings.visibility = if (config.bindings.isNotEmpty()) View.VISIBLE else View.GONE

                isProgrammaticUpdate = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                progressBarLoading.visibility = if (loading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.clusterSizeProfile.collectLatest { profile ->
                val index = clusterOptions.indexOfFirst { it.profile == profile }
                if (index >= 0 && spinnerClusterSize.selectedItemPosition != index) {
                    isProgrammaticUpdate = true
                    spinnerClusterSize.setSelection(index)
                    isProgrammaticUpdate = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.installedApps.collectLatest { apps ->
                val labels = apps.map { it.label }
                val appAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels)
                spinnerOpenApp.adapter = appAdapter
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCapturing.collectLatest { capturing ->
                if (capturing) {
                    btnCaptureToggle.text = "Stop Capture"
                    tvCaptureCode.text = "Listening..."
                    tvCaptureHint.text = "Press any steering wheel or dash button now"
                    layoutCaptureBox.visibility = View.VISIBLE
                } else {
                    btnCaptureToggle.text = "Capture Key"
                    val code = viewModel.capturedKeycode.value
                    if (code != null) {
                        tvCaptureCode.text = "Keycode #$code"
                        tvCaptureHint.text = "Key successfully captured!"
                        etManualKeycode.setText(code.toString())
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.capturedKeycode.collectLatest { code ->
                if (code != null) {
                    tvCaptureCode.text = "Keycode #$code"
                    etManualKeycode.setText(code.toString())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { msg ->
                if (!msg.isNullOrBlank()) showToast(msg)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.successMessage.collectLatest { msg ->
                if (!msg.isNullOrBlank()) showToast(msg)
            }
        }
    }

    private fun showDeleteDialog(binding: KeyBinding) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_binding, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmDelete)

        tvMessage.text = getString(R.string.keymap_delete_confirm_msg, binding.displayKeyName)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            viewModel.deleteBinding(binding)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showToast(msg: String) {
        view?.let {
            Snackbar.make(it, msg, Snackbar.LENGTH_SHORT).show()
        }
    }
}
