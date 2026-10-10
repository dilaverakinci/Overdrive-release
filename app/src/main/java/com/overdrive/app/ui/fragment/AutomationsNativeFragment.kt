package com.overdrive.app.ui.fragment

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.overdrive.app.R
import com.overdrive.app.ui.automations.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AutomationsNativeFragment : Fragment() {

    private val viewModel: AutomationsViewModel by viewModels()

    // Header views
    private lateinit var tvAutomationsTitle: TextView
    private lateinit var tvAutomationsSubtitle: TextView
    private lateinit var tvStatusBanner: TextView
    private lateinit var progressLoading: ProgressBar

    // Containers
    private lateinit var containerRules: NestedScrollView
    private lateinit var containerAdd: NestedScrollView
    private lateinit var containerGroups: NestedScrollView
    private lateinit var containerSafety: NestedScrollView
    private lateinit var containerCommunity: NestedScrollView

    // Bottom Navigation Bar
    private lateinit var tabBottomAutomations: LinearLayout
    private lateinit var ivBottomTabAutomations: ImageView
    private lateinit var tvBottomTabAutomations: TextView

    private lateinit var tabBottomAdd: LinearLayout
    private lateinit var ivBottomTabAdd: ImageView
    private lateinit var tvBottomTabAdd: TextView

    private lateinit var tabBottomGroups: LinearLayout
    private lateinit var ivBottomTabGroups: ImageView
    private lateinit var tvBottomTabGroups: TextView

    private lateinit var tabBottomSafety: LinearLayout
    private lateinit var ivBottomTabSafety: ImageView
    private lateinit var tvBottomTabSafety: TextView

    private lateinit var tabBottomCommunity: LinearLayout
    private lateinit var ivBottomTabCommunity: ImageView
    private lateinit var tvBottomTabCommunity: TextView

    // Automations Tab views
    private lateinit var tvRulesCount: TextView
    private lateinit var btnSortRules: MaterialButton
    private lateinit var btnAddAutomationTop: MaterialButton
    private lateinit var recyclerRules: RecyclerView
    private lateinit var layoutEmptyRules: View
    private lateinit var btnAddEmpty: MaterialButton
    private lateinit var rulesAdapter: AutomationsAdapter

    // Settings inside Automations Tab
    private lateinit var switchAllowShell: MaterialSwitch
    private lateinit var spinnerClusterSize: Spinner
    private lateinit var btnBackupExport: MaterialButton
    private lateinit var btnBackupImport: MaterialButton
    private lateinit var btnAudioUpload: MaterialButton

    // Add Tab views
    private lateinit var spinnerExecMode: Spinner
    private lateinit var etAutomationName: EditText
    private lateinit var spinnerTrigger: Spinner
    private lateinit var spinnerAction: Spinner
    private lateinit var btnCancelAdd: MaterialButton
    private lateinit var btnSaveAutomation: MaterialButton

    // Groups Tab views
    private lateinit var tvGroupsCount: TextView
    private lateinit var btnGroupsExport: MaterialButton
    private lateinit var btnGroupsImport: MaterialButton
    private lateinit var btnNewGroup: MaterialButton
    private lateinit var recyclerGroups: RecyclerView
    private lateinit var layoutEmptyGroups: View
    private lateinit var btnNewGroupEmpty: MaterialButton
    private lateinit var groupsAdapter: ActionGroupsAdapter

    // Safety Guards Tab views
    private lateinit var switchSafetyDoorLocks: MaterialSwitch
    private lateinit var switchSafetyTrunk: MaterialSwitch
    private lateinit var switchSafetyMirrorFold: MaterialSwitch
    private lateinit var switchSafetyPositioning: MaterialSwitch
    private lateinit var switchSafetyHeadlightOff: MaterialSwitch
    private lateinit var switchSafetyDisplayBrightness: MaterialSwitch
    private lateinit var switchSafetyDisplayPower: MaterialSwitch
    private lateinit var switchSafetyScreenMedia: MaterialSwitch

    private var isProgrammaticChange = false
    private var isClusterSpinnerProgrammatic = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_automations_native, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupBottomTabs()
        setupListeners()
        setupAdapters()
        setupSpinners()
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
        tvAutomationsTitle = v.findViewById(R.id.tvAutomationsTitle)
        tvAutomationsSubtitle = v.findViewById(R.id.tvAutomationsSubtitle)
        tvStatusBanner = v.findViewById(R.id.tvStatusBanner)
        progressLoading = v.findViewById(R.id.progressAutomationsLoading)

        containerRules = v.findViewById(R.id.containerRules)
        containerAdd = v.findViewById(R.id.containerAdd)
        containerGroups = v.findViewById(R.id.containerGroups)
        containerSafety = v.findViewById(R.id.containerSafety)
        containerCommunity = v.findViewById(R.id.containerCommunity)

        // Bottom Tabs
        tabBottomAutomations = v.findViewById(R.id.tabBottomAutomations)
        ivBottomTabAutomations = v.findViewById(R.id.ivBottomTabAutomations)
        tvBottomTabAutomations = v.findViewById(R.id.tvBottomTabAutomations)

        tabBottomAdd = v.findViewById(R.id.tabBottomAdd)
        ivBottomTabAdd = v.findViewById(R.id.ivBottomTabAdd)
        tvBottomTabAdd = v.findViewById(R.id.tvBottomTabAdd)

        tabBottomGroups = v.findViewById(R.id.tabBottomGroups)
        ivBottomTabGroups = v.findViewById(R.id.ivBottomTabGroups)
        tvBottomTabGroups = v.findViewById(R.id.tvBottomTabGroups)

        tabBottomSafety = v.findViewById(R.id.tabBottomSafety)
        ivBottomTabSafety = v.findViewById(R.id.ivBottomTabSafety)
        tvBottomTabSafety = v.findViewById(R.id.tvBottomTabSafety)

        tabBottomCommunity = v.findViewById(R.id.tabBottomCommunity)
        ivBottomTabCommunity = v.findViewById(R.id.ivBottomTabCommunity)
        tvBottomTabCommunity = v.findViewById(R.id.tvBottomTabCommunity)

        // Automations Tab views
        tvRulesCount = v.findViewById(R.id.tvRulesCount)
        btnSortRules = v.findViewById(R.id.btnSortRules)
        btnAddAutomationTop = v.findViewById(R.id.btnAddAutomationTop)
        recyclerRules = v.findViewById(R.id.recyclerRules)
        layoutEmptyRules = v.findViewById(R.id.layoutEmptyRules)
        btnAddEmpty = v.findViewById(R.id.btnAddEmpty)

        switchAllowShell = v.findViewById(R.id.switchAllowShell)
        spinnerClusterSize = v.findViewById(R.id.spinnerClusterSize)
        btnBackupExport = v.findViewById(R.id.btnBackupExport)
        btnBackupImport = v.findViewById(R.id.btnBackupImport)
        btnAudioUpload = v.findViewById(R.id.btnAudioUpload)

        // Add Tab views
        spinnerExecMode = v.findViewById(R.id.spinnerExecMode)
        etAutomationName = v.findViewById(R.id.etAutomationName)
        spinnerTrigger = v.findViewById(R.id.spinnerTrigger)
        spinnerAction = v.findViewById(R.id.spinnerAction)
        btnCancelAdd = v.findViewById(R.id.btnCancelAdd)
        btnSaveAutomation = v.findViewById(R.id.btnSaveAutomation)

        // Groups Tab views
        tvGroupsCount = v.findViewById(R.id.tvGroupsCount)
        btnGroupsExport = v.findViewById(R.id.btnGroupsExport)
        btnGroupsImport = v.findViewById(R.id.btnGroupsImport)
        btnNewGroup = v.findViewById(R.id.btnNewGroup)
        recyclerGroups = v.findViewById(R.id.recyclerGroups)
        layoutEmptyGroups = v.findViewById(R.id.layoutEmptyGroups)
        btnNewGroupEmpty = v.findViewById(R.id.btnNewGroupEmpty)

        // Safety Guards Tab views
        switchSafetyDoorLocks = v.findViewById(R.id.switchSafetyDoorLocks)
        switchSafetyTrunk = v.findViewById(R.id.switchSafetyTrunk)
        switchSafetyMirrorFold = v.findViewById(R.id.switchSafetyMirrorFold)
        switchSafetyPositioning = v.findViewById(R.id.switchSafetyPositioning)
        switchSafetyHeadlightOff = v.findViewById(R.id.switchSafetyHeadlightOff)
        switchSafetyDisplayBrightness = v.findViewById(R.id.switchSafetyDisplayBrightness)
        switchSafetyDisplayPower = v.findViewById(R.id.switchSafetyDisplayPower)
        switchSafetyScreenMedia = v.findViewById(R.id.switchSafetyScreenMedia)
    }

    private fun setupBottomTabs() {
        tabBottomAutomations.setOnClickListener { viewModel.selectTab(AutomationsTab.AUTOMATIONS) }
        tabBottomAdd.setOnClickListener { viewModel.selectTab(AutomationsTab.ADD) }
        tabBottomGroups.setOnClickListener { viewModel.selectTab(AutomationsTab.GROUPS) }
        tabBottomSafety.setOnClickListener { viewModel.selectTab(AutomationsTab.SAFETY) }
        tabBottomCommunity.setOnClickListener { viewModel.selectTab(AutomationsTab.COMMUNITY) }
    }

    private fun setupListeners() {
        btnAddAutomationTop.setOnClickListener { viewModel.selectTab(AutomationsTab.ADD) }
        btnAddEmpty.setOnClickListener { viewModel.selectTab(AutomationsTab.ADD) }
        btnCancelAdd.setOnClickListener { viewModel.selectTab(AutomationsTab.AUTOMATIONS) }

        btnSortRules.setOnClickListener { showSortMenu(it) }

        btnBackupExport.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.automation_backup_export_import), Toast.LENGTH_SHORT).show()
        }
        btnBackupImport.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.automation_backup_export_import), Toast.LENGTH_SHORT).show()
        }
        btnAudioUpload.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.automation_audio_uploaded), Toast.LENGTH_SHORT).show()
        }

        btnGroupsExport.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.automation_btn_export), Toast.LENGTH_SHORT).show()
        }
        btnGroupsImport.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.automation_btn_import), Toast.LENGTH_SHORT).show()
        }
        btnNewGroup.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.automation_groups_new), Toast.LENGTH_SHORT).show()
        }
        btnNewGroupEmpty.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.automation_groups_new), Toast.LENGTH_SHORT).show()
        }

        btnSaveAutomation.setOnClickListener {
            val name = etAutomationName.text.toString().trim()
            val mode = when (spinnerExecMode.selectedItemPosition) {
                0 -> "automatic"
                1 -> "manual"
                else -> "disabled"
            }
            val trigger = spinnerTrigger.selectedItem?.toString() ?: ""
            val action = spinnerAction.selectedItem?.toString() ?: ""

            viewModel.saveAutomation(name, mode, trigger, action) { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Automation saved successfully", Toast.LENGTH_SHORT).show()
                    viewModel.selectTab(AutomationsTab.AUTOMATIONS)
                } else {
                    Toast.makeText(requireContext(), "Saved automation locally", Toast.LENGTH_SHORT).show()
                    viewModel.selectTab(AutomationsTab.AUTOMATIONS)
                }
            }
        }

        switchSafetyDoorLocks.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("doorLocks", isChecked)
        }
        switchSafetyTrunk.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("trunk", isChecked)
        }
        switchSafetyMirrorFold.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("mirrorFold", isChecked)
        }
        switchSafetyPositioning.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("positioning", isChecked)
        }
        switchSafetyHeadlightOff.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("headlightOff", isChecked)
        }
        switchSafetyDisplayBrightness.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("displayBrightness", isChecked)
        }
        switchSafetyDisplayPower.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("displayPower", isChecked)
        }
        switchSafetyScreenMedia.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateSafetyGuard("screenMedia", isChecked)
        }

        switchAllowShell.setOnCheckedChangeListener { _, isChecked ->
            if (!isProgrammaticChange) viewModel.updateAllowShell(isChecked)
        }
    }

    private fun setupSpinners() {
        // Cluster size
        val clusterProfiles = listOf(
            getString(R.string.keymap_cluster_1025) to 31,
            getString(R.string.keymap_cluster_123) to 30,
            getString(R.string.keymap_cluster_88) to 29
        )
        val clusterAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            clusterProfiles.map { it.first }
        )
        spinnerClusterSize.adapter = clusterAdapter
        spinnerClusterSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isClusterSpinnerProgrammatic) return
                val selectedProfile = clusterProfiles[position].second
                viewModel.updateClusterSizeProfile(selectedProfile)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Exec mode
        val execModes = listOf("Automatic", "Manual Only", "Disabled")
        spinnerExecMode.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            execModes
        )

        // Triggers
        val triggers = listOf(
            "Vehicle Speed [speed]",
            "Gear Selection [gear]",
            "Battery SOC [soc]",
            "Doors State [doors]",
            "Turn Indicators [turn_signal]",
            "Headlights State [headlights]",
            "AC State [ac]",
            "Time of Day [time]"
        )
        spinnerTrigger.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            triggers
        )

        // Actions
        val actions = listOf(
            "Lock Vehicle Doors",
            "Unlock Vehicle Doors",
            "Flash Lights",
            "Sound Horn Alert",
            "Open Trunk / Tailgate",
            "Close Trunk / Tailgate",
            "Fold Exterior Mirrors",
            "Unfold Exterior Mirrors"
        )
        spinnerAction.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            actions
        )
    }

    private fun setupAdapters() {
        rulesAdapter = AutomationsAdapter(
            onToggleMode = { item -> viewModel.toggleAutomationMode(item) },
            onCycleMode = { item ->
                val nextMode = when (item.mode) {
                    "automatic" -> "manual"
                    "manual" -> "disabled"
                    else -> "automatic"
                }
                viewModel.setAutomationMode(item.id, nextMode)
            },
            onTest = { item -> viewModel.testAutomation(item.id, item.displayName) },
            onDelete = { item -> showDeleteRuleDialog(item) }
        )
        recyclerRules.layoutManager = LinearLayoutManager(requireContext())
        recyclerRules.adapter = rulesAdapter

        groupsAdapter = ActionGroupsAdapter(
            onRun = { group -> viewModel.runActionGroup(group) },
            onDelete = { group -> showDeleteGroupDialog(group) }
        )
        recyclerGroups.layoutManager = LinearLayoutManager(requireContext())
        recyclerGroups.adapter = groupsAdapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        renderUi(state)
                    }
                }
                launch {
                    viewModel.clusterSizeProfile.collectLatest { profile ->
                        isClusterSpinnerProgrammatic = true
                        val idx = when (profile) {
                            30 -> 1
                            29 -> 2
                            else -> 0
                        }
                        spinnerClusterSize.setSelection(idx)
                        isClusterSpinnerProgrammatic = false
                    }
                }
            }
        }
    }

    private fun renderUi(state: AutomationsUiState) {
        progressLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Status banner
        if (state.testRunStatus != null) {
            tvStatusBanner.text = state.testRunStatus
            tvStatusBanner.visibility = View.VISIBLE
        } else {
            tvStatusBanner.visibility = View.GONE
        }

        // Render Bottom Tabs & Containers
        renderTabs(state.activeTab)

        when (state.activeTab) {
            AutomationsTab.AUTOMATIONS -> {
                containerRules.visibility = View.VISIBLE
                containerAdd.visibility = View.GONE
                containerGroups.visibility = View.GONE
                containerSafety.visibility = View.GONE
                containerCommunity.visibility = View.GONE
                renderRulesTab(state)
            }
            AutomationsTab.ADD -> {
                containerRules.visibility = View.GONE
                containerAdd.visibility = View.VISIBLE
                containerGroups.visibility = View.GONE
                containerSafety.visibility = View.GONE
                containerCommunity.visibility = View.GONE
            }
            AutomationsTab.GROUPS -> {
                containerRules.visibility = View.GONE
                containerAdd.visibility = View.GONE
                containerGroups.visibility = View.VISIBLE
                containerSafety.visibility = View.GONE
                containerCommunity.visibility = View.GONE
                renderGroupsTab(state)
            }
            AutomationsTab.SAFETY -> {
                containerRules.visibility = View.GONE
                containerAdd.visibility = View.GONE
                containerGroups.visibility = View.GONE
                containerSafety.visibility = View.VISIBLE
                containerCommunity.visibility = View.GONE
                renderSafetyTab(state)
            }
            AutomationsTab.COMMUNITY -> {
                containerRules.visibility = View.GONE
                containerAdd.visibility = View.GONE
                containerGroups.visibility = View.GONE
                containerSafety.visibility = View.GONE
                containerCommunity.visibility = View.VISIBLE
            }
        }

        renderSettingsTab(state)

        state.errorMessage?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderTabs(activeTab: AutomationsTab) {
        val activeBg = R.drawable.bg_bottom_tab_active
        val activeColor = Color.parseColor("#004D40")
        val inactiveColor = Color.parseColor("#9E9E9E")

        fun applyTab(
            tabLayout: LinearLayout,
            iv: ImageView,
            tv: TextView,
            isActive: Boolean
        ) {
            if (isActive) {
                tabLayout.setBackgroundResource(activeBg)
                iv.setColorFilter(activeColor)
                tv.setTextColor(activeColor)
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tabLayout.setBackgroundColor(Color.TRANSPARENT)
                iv.setColorFilter(inactiveColor)
                tv.setTextColor(inactiveColor)
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }

        applyTab(tabBottomAutomations, ivBottomTabAutomations, tvBottomTabAutomations, activeTab == AutomationsTab.AUTOMATIONS)
        applyTab(tabBottomAdd, ivBottomTabAdd, tvBottomTabAdd, activeTab == AutomationsTab.ADD)
        applyTab(tabBottomGroups, ivBottomTabGroups, tvBottomTabGroups, activeTab == AutomationsTab.GROUPS)
        applyTab(tabBottomSafety, ivBottomTabSafety, tvBottomTabSafety, activeTab == AutomationsTab.SAFETY)
        applyTab(tabBottomCommunity, ivBottomTabCommunity, tvBottomTabCommunity, activeTab == AutomationsTab.COMMUNITY)
    }

    private fun renderRulesTab(state: AutomationsUiState) {
        val total = state.automations.size
        tvRulesCount.text = "$total Automations configured"

        btnSortRules.text = when (state.sortMode) {
            AutomationSortMode.DEFAULT -> "Sort: Default"
            AutomationSortMode.NAME_AZ -> "Sort: A-Z"
            AutomationSortMode.NAME_ZA -> "Sort: Z-A"
            AutomationSortMode.RECENT -> "Sort: Recent"
            AutomationSortMode.RUNS -> "Sort: Most Runs"
            AutomationSortMode.ENABLED_FIRST -> "Sort: Enabled First"
            AutomationSortMode.DISABLED_FIRST -> "Sort: Disabled First"
        }

        if (state.automations.isEmpty()) {
            recyclerRules.visibility = View.GONE
            layoutEmptyRules.visibility = View.VISIBLE
        } else {
            recyclerRules.visibility = View.VISIBLE
            layoutEmptyRules.visibility = View.GONE
            rulesAdapter.submitList(state.automations)
        }
    }

    private fun renderGroupsTab(state: AutomationsUiState) {
        val total = state.actionGroups.size
        tvGroupsCount.text = "$total Reusable Action Groups"

        if (state.actionGroups.isEmpty()) {
            recyclerGroups.visibility = View.GONE
            layoutEmptyGroups.visibility = View.VISIBLE
        } else {
            recyclerGroups.visibility = View.VISIBLE
            layoutEmptyGroups.visibility = View.GONE
            groupsAdapter.submitList(state.actionGroups)
        }
    }

    private fun renderSafetyTab(state: AutomationsUiState) {
        val s = state.settings
        isProgrammaticChange = true
        switchSafetyDoorLocks.isChecked = s.doorLocksGuard
        switchSafetyTrunk.isChecked = s.trunkGuard
        switchSafetyMirrorFold.isChecked = s.mirrorFoldGuard
        switchSafetyPositioning.isChecked = s.positioningGuard
        switchSafetyHeadlightOff.isChecked = s.headlightOffGuard
        switchSafetyDisplayBrightness.isChecked = s.displayBrightnessGuard
        switchSafetyDisplayPower.isChecked = s.displayPowerGuard
        switchSafetyScreenMedia.isChecked = s.screenMediaGuard
        isProgrammaticChange = false
    }

    private fun renderSettingsTab(state: AutomationsUiState) {
        isProgrammaticChange = true
        switchAllowShell.isChecked = state.settings.allowShell
        isProgrammaticChange = false
    }

    private fun showSortMenu(v: View) {
        val popup = PopupMenu(requireContext(), v)
        popup.menu.add(0, 1, 0, "Default")
        popup.menu.add(0, 2, 1, "Name (A to Z)")
        popup.menu.add(0, 3, 2, "Name (Z to A)")
        popup.menu.add(0, 4, 3, "Most Recently Triggered")
        popup.menu.add(0, 5, 4, "Most Trigger Count")
        popup.menu.add(0, 6, 5, "Enabled First")
        popup.menu.add(0, 7, 6, "Disabled First")

        popup.setOnMenuItemClickListener { item: MenuItem ->
            val mode = when (item.itemId) {
                1 -> AutomationSortMode.DEFAULT
                2 -> AutomationSortMode.NAME_AZ
                3 -> AutomationSortMode.NAME_ZA
                4 -> AutomationSortMode.RECENT
                5 -> AutomationSortMode.RUNS
                6 -> AutomationSortMode.ENABLED_FIRST
                7 -> AutomationSortMode.DISABLED_FIRST
                else -> AutomationSortMode.DEFAULT
            }
            viewModel.setSortMode(mode)
            true
        }
        popup.show()
    }

    private fun showDeleteRuleDialog(item: AutomationItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_automation, null)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDeleteDialogMessage)
        tvMessage.text = "\"${item.displayName}\" will be permanently deleted."

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton(R.string.common_delete) { _, _ ->
                viewModel.deleteAutomation(item.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteGroupDialog(group: ActionGroupItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.automation_dialog_delete_title)
            .setMessage("Are you sure you want to delete action group \"${group.name}\"?")
            .setPositiveButton(R.string.common_delete) { _, _ ->
                viewModel.deleteActionGroup(group.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
