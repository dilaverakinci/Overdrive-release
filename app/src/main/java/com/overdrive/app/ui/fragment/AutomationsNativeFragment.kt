package com.overdrive.app.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    private lateinit var layoutAutomationTabs: LinearLayout
    private lateinit var tabBtnRules: MaterialButton
    private lateinit var tabBtnGroups: MaterialButton
    private lateinit var tabBtnSafety: MaterialButton
    private lateinit var tabBtnSettings: MaterialButton

    private lateinit var tvStatusBanner: TextView
    private lateinit var progressLoading: ProgressBar

    // Containers
    private lateinit var containerRules: LinearLayout
    private lateinit var containerGroups: LinearLayout
    private lateinit var containerSafety: NestedScrollView
    private lateinit var containerSettings: NestedScrollView

    // Rules Tab views
    private lateinit var tvRulesCount: TextView
    private lateinit var btnSortRules: MaterialButton
    private lateinit var recyclerRules: RecyclerView
    private lateinit var layoutEmptyRules: LinearLayout
    private lateinit var rulesAdapter: AutomationsAdapter

    // Groups Tab views
    private lateinit var tvGroupsCount: TextView
    private lateinit var recyclerGroups: RecyclerView
    private lateinit var layoutEmptyGroups: LinearLayout
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

    // Settings Tab views
    private lateinit var switchAllowShell: MaterialSwitch

    private var isProgrammaticChange = false

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
        setupListeners()
        setupAdapters()
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
        layoutAutomationTabs = v.findViewById(R.id.layoutAutomationTabs)
        tabBtnRules = v.findViewById(R.id.tabBtnRules)
        tabBtnGroups = v.findViewById(R.id.tabBtnGroups)
        tabBtnSafety = v.findViewById(R.id.tabBtnSafety)
        tabBtnSettings = v.findViewById(R.id.tabBtnSettings)

        tvStatusBanner = v.findViewById(R.id.tvStatusBanner)
        progressLoading = v.findViewById(R.id.progressAutomationsLoading)

        containerRules = v.findViewById(R.id.containerRules)
        containerGroups = v.findViewById(R.id.containerGroups)
        containerSafety = v.findViewById(R.id.containerSafety)
        containerSettings = v.findViewById(R.id.containerSettings)

        tvRulesCount = v.findViewById(R.id.tvRulesCount)
        btnSortRules = v.findViewById(R.id.btnSortRules)
        recyclerRules = v.findViewById(R.id.recyclerRules)
        layoutEmptyRules = v.findViewById(R.id.layoutEmptyRules)

        tvGroupsCount = v.findViewById(R.id.tvGroupsCount)
        recyclerGroups = v.findViewById(R.id.recyclerGroups)
        layoutEmptyGroups = v.findViewById(R.id.layoutEmptyGroups)

        switchSafetyDoorLocks = v.findViewById(R.id.switchSafetyDoorLocks)
        switchSafetyTrunk = v.findViewById(R.id.switchSafetyTrunk)
        switchSafetyMirrorFold = v.findViewById(R.id.switchSafetyMirrorFold)
        switchSafetyPositioning = v.findViewById(R.id.switchSafetyPositioning)
        switchSafetyHeadlightOff = v.findViewById(R.id.switchSafetyHeadlightOff)
        switchSafetyDisplayBrightness = v.findViewById(R.id.switchSafetyDisplayBrightness)
        switchSafetyDisplayPower = v.findViewById(R.id.switchSafetyDisplayPower)
        switchSafetyScreenMedia = v.findViewById(R.id.switchSafetyScreenMedia)

        switchAllowShell = v.findViewById(R.id.switchAllowShell)
    }

    private fun setupListeners() {
        tabBtnRules.setOnClickListener { viewModel.selectTab(AutomationsTab.RULES) }
        tabBtnGroups.setOnClickListener { viewModel.selectTab(AutomationsTab.GROUPS) }
        tabBtnSafety.setOnClickListener { viewModel.selectTab(AutomationsTab.SAFETY) }
        tabBtnSettings.setOnClickListener { viewModel.selectTab(AutomationsTab.SETTINGS) }

        btnSortRules.setOnClickListener { showSortMenu(it) }

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
                viewModel.uiState.collectLatest { state ->
                    renderUi(state)
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

        // Render Tabs
        renderTabs(state.activeTab)

        when (state.activeTab) {
            AutomationsTab.RULES -> {
                containerRules.visibility = View.VISIBLE
                containerGroups.visibility = View.GONE
                containerSafety.visibility = View.GONE
                containerSettings.visibility = View.GONE
                renderRulesTab(state)
            }
            AutomationsTab.GROUPS -> {
                containerRules.visibility = View.GONE
                containerGroups.visibility = View.VISIBLE
                containerSafety.visibility = View.GONE
                containerSettings.visibility = View.GONE
                renderGroupsTab(state)
            }
            AutomationsTab.SAFETY -> {
                containerRules.visibility = View.GONE
                containerGroups.visibility = View.GONE
                containerSafety.visibility = View.VISIBLE
                containerSettings.visibility = View.GONE
                renderSafetyTab(state)
            }
            AutomationsTab.SETTINGS -> {
                containerRules.visibility = View.GONE
                containerGroups.visibility = View.GONE
                containerSafety.visibility = View.GONE
                containerSettings.visibility = View.VISIBLE
                renderSettingsTab(state)
            }
        }

        state.errorMessage?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderTabs(activeTab: AutomationsTab) {
        tabBtnRules.applyTabStyle(activeTab == AutomationsTab.RULES)
        tabBtnGroups.applyTabStyle(activeTab == AutomationsTab.GROUPS)
        tabBtnSafety.applyTabStyle(activeTab == AutomationsTab.SAFETY)
        tabBtnSettings.applyTabStyle(activeTab == AutomationsTab.SETTINGS)
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

    private fun MaterialButton.applyTabStyle(selected: Boolean) {
        if (selected) {
            setBackgroundColor(Color.parseColor("#BBD4CB"))
            setTextColor(Color.parseColor("#004D40"))
        } else {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.parseColor("#9E9E9E"))
        }
    }
}
