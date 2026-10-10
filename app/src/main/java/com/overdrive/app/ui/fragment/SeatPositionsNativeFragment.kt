package com.overdrive.app.ui.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.overdrive.app.R
import com.overdrive.app.byd.light.LightConstants
import com.overdrive.app.ui.seatpositions.AmbientColourAdapter
import com.overdrive.app.ui.seatpositions.SeatGeometryHelper
import com.overdrive.app.ui.seatpositions.SeatPosition
import com.overdrive.app.ui.seatpositions.SeatPositionAdapter
import com.overdrive.app.ui.seatpositions.SeatPositionsViewModel
import kotlinx.coroutines.launch

class SeatPositionsNativeFragment : Fragment() {

    private val viewModel: SeatPositionsViewModel by viewModels()
    private lateinit var adapter: SeatPositionAdapter

    // Views
    private var layoutCurrentGlyph: View? = null
    private var ivCurrentGlyph: ImageView? = null
    private var tvCurrentMatch: TextView? = null
    private var btnToggleDetails: MaterialButton? = null
    private var layoutAxesDetails: LinearLayout? = null
    private var tvSeatAxesReadout: TextView? = null
    private var tvMirrorAxesReadout: TextView? = null
    private var btnSaveNew: MaterialButton? = null
    private var viewStatusDot: View? = null
    private var tvStatusText: TextView? = null
    private var cardEmptyState: MaterialCardView? = null
    private var rvSavedPositions: RecyclerView? = null
    private var pbLoading: ProgressBar? = null

    private var detailsExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_seat_positions_native, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupListeners()
        observeState()
    }

    private fun bindViews(view: View) {
        layoutCurrentGlyph = view.findViewById(R.id.layoutCurrentGlyph)
        ivCurrentGlyph = view.findViewById(R.id.ivCurrentGlyph)
        tvCurrentMatch = view.findViewById(R.id.tvCurrentMatch)
        btnToggleDetails = view.findViewById(R.id.btnToggleDetails)
        layoutAxesDetails = view.findViewById(R.id.layoutAxesDetails)
        tvSeatAxesReadout = view.findViewById(R.id.tvSeatAxesReadout)
        tvMirrorAxesReadout = view.findViewById(R.id.tvMirrorAxesReadout)
        btnSaveNew = view.findViewById(R.id.btnSaveNew)
        viewStatusDot = view.findViewById(R.id.viewStatusDot)
        tvStatusText = view.findViewById(R.id.tvStatusText)
        cardEmptyState = view.findViewById(R.id.cardEmptyState)
        rvSavedPositions = view.findViewById(R.id.rvSavedPositions)
        pbLoading = view.findViewById(R.id.pbLoading)
    }

    private fun setupRecyclerView() {
        val rv = rvSavedPositions ?: return
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = SeatPositionAdapter(
            context = requireContext(),
            onApplyClick = { pos -> handleApply(pos) },
            onSaveOverClick = { pos -> showSaveOverDialog(pos) },
            onRenameClick = { pos -> showRenameDialog(pos) },
            onSetColourClick = { pos -> showSetColourDialog(pos) },
            onSetAliasClick = { pos -> showSetAliasDialog(pos) },
            onDeleteClick = { pos -> showDeleteDialog(pos) }
        )
        rv.adapter = adapter
    }

    private fun setupListeners() {
        btnToggleDetails?.setOnClickListener {
            detailsExpanded = !detailsExpanded
            updateDetailsExpansion()
        }

        btnSaveNew?.setOnClickListener {
            showSaveNewDialog()
        }
    }

    private fun updateDetailsExpansion() {
        layoutAxesDetails?.visibility = if (detailsExpanded) View.VISIBLE else View.GONE
        btnToggleDetails?.text = getString(
            if (detailsExpanded) R.string.seatpos_hide_details else R.string.seatpos_show_details
        )
        btnToggleDetails?.setIconResource(
            if (detailsExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopPolling()
    }

    private fun renderState(state: com.overdrive.app.ui.seatpositions.SeatPositionsState) {
        val ctx = context ?: return

        // 1. Current Match / Eyebrow Title & Glyph & Details visibility
        if (state.currentAxes == null) {
            layoutCurrentGlyph?.visibility = View.GONE
            btnToggleDetails?.visibility = View.GONE
            layoutAxesDetails?.visibility = View.GONE
            tvCurrentMatch?.text = getString(R.string.seatpos_current_unavailable)
            tvCurrentMatch?.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        } else {
            layoutCurrentGlyph?.visibility = View.VISIBLE
            btnToggleDetails?.visibility = View.VISIBLE
            layoutAxesDetails?.visibility = if (detailsExpanded) View.VISIBLE else View.GONE
            if (state.matchedPosition != null) {
                tvCurrentMatch?.text = getString(R.string.seatpos_matches, state.matchedPosition.displayName)
                tvCurrentMatch?.setTextColor(ContextCompat.getColor(ctx, R.color.brand_primary))
            } else {
                tvCurrentMatch?.text = getString(R.string.seatpos_not_saved)
                tvCurrentMatch?.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            }
        }

        // 2. Axes Details Readout
        renderAxesReadout(state.currentAxes)

        // 3. Save as New Button State
        btnSaveNew?.isEnabled = state.gate.canSave

        // 4. Gate Status Footer
        if (!state.gate.acc) {
            viewStatusDot?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F"))
            tvStatusText?.text = getString(R.string.seatpos_gate_acc_off)
        } else if (state.gate.movementBlocked || state.gate.positioningBlocked) {
            viewStatusDot?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F57C00"))
            tvStatusText?.text = state.gate.movementBlockReason ?: getString(R.string.seatpos_gate_moving)
        } else if (!state.gate.modelConfirmed && !state.gate.modelAcknowledged) {
            viewStatusDot?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F57C00"))
            tvStatusText?.text = getString(R.string.seatpos_unconfirmed_note)
        } else {
            viewStatusDot?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            tvStatusText?.text = getString(R.string.seatpos_gate_ready)
        }

        // 5. Positions List / Empty State
        if (state.positions.isEmpty() && !state.isLoading) {
            cardEmptyState?.visibility = View.VISIBLE
            rvSavedPositions?.visibility = View.GONE
        } else {
            cardEmptyState?.visibility = View.GONE
            rvSavedPositions?.visibility = View.VISIBLE
            adapter.activePositionId = state.matchedPosition?.id
            adapter.applyingPositionId = state.applyingPositionId
            adapter.automations = state.automations
            adapter.canApply = state.gate.canApply
            adapter.submitList(state.positions)
        }

        // 6. Loading
        pbLoading?.visibility = if (state.isLoading && state.positions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderAxesReadout(axes: Map<String, Double>?) {
        if (axes == null) {
            tvSeatAxesReadout?.text = "—"
            tvMirrorAxesReadout?.text = "—"
            return
        }

        val seatParts = mutableListOf<String>()
        val mirrorParts = mutableListOf<String>()

        for (axis in SeatGeometryHelper.AXES) {
            val v = axes[axis.key]
            if (SeatGeometryHelper.isValidValue(v)) {
                val label = if (isTurkish()) axis.labelTr else axis.labelEn
                val text = "$label: ${String.format("%.1f", v)}"
                if (axis.group == "seat") {
                    seatParts.add(text)
                } else {
                    mirrorParts.add(text)
                }
            }
        }

        tvSeatAxesReadout?.text = if (seatParts.isNotEmpty()) seatParts.joinToString(" · ") else "—"
        tvMirrorAxesReadout?.text = if (mirrorParts.isNotEmpty()) mirrorParts.joinToString(" · ") else "—"
    }

    private fun isTurkish(): Boolean {
        return resources.configuration.locales.get(0).language.startsWith("tr")
    }

    // ================= ACTIONS & DIALOGS =================

    private fun handleApply(position: SeatPosition) {
        val state = viewModel.state.value
        if (!state.gate.modelConfirmed && !state.gate.modelAcknowledged) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.seatpos_unconfirmed_title)
                .setMessage(R.string.seatpos_unconfirmed_body)
                .setNegativeButton(R.string.common_cancel, null)
                .setPositiveButton(R.string.seatpos_unconfirmed_confirm) { _, _ ->
                    executeApply(position, ackModel = true)
                }
                .show()
        } else {
            executeApply(position, ackModel = false)
        }
    }

    private fun executeApply(position: SeatPosition, ackModel: Boolean) {
        viewModel.applyPosition(position, ackModel) { success, error ->
            val msg = if (success) {
                getString(R.string.seatpos_applied_toast, position.displayName)
            } else {
                error ?: getString(R.string.seatpos_apply_failed)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaveNewDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_seat_position, null)
        val etName = dialogView.findViewById<EditText>(R.id.etPositionName)
        val rbAll = dialogView.findViewById<RadioButton>(R.id.rbScopeAll)
        val rbGeo = dialogView.findViewById<RadioButton>(R.id.rbScopeGeometry)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.seatpos_save_new_title)
            .setView(dialogView)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.seatpos_save) { _, _ ->
                val name = etName.text.toString().trim().ifEmpty { getString(R.string.seatpos_default_name) }
                val parts = when {
                    rbGeo.isChecked -> "geometry"
                    rbAll.isChecked -> "all"
                    else -> "ambient"
                }
                viewModel.createPosition(name, parts) { success, error ->
                    val msg = if (success) {
                        getString(R.string.seatpos_saved_toast, name)
                    } else {
                        error ?: getString(R.string.seatpos_save_failed)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showSaveOverDialog(position: SeatPosition) {
        val uses = viewModel.state.value.automations[position.id]
        val body = getString(R.string.seatpos_overwrite_body) +
                if (uses != null) " " + getString(R.string.seatpos_overwrite_used) else ""

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.seatpos_overwrite_title, position.displayName))
            .setMessage(body)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.seatpos_save) { _, _ ->
                viewModel.saveOverPosition(position, "all") { success, error ->
                    val msg = if (success) {
                        getString(R.string.seatpos_saved_toast, position.displayName)
                    } else {
                        error ?: getString(R.string.seatpos_save_failed)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showRenameDialog(position: SeatPosition) {
        val input = EditText(requireContext()).apply {
            setText(position.name)
            setSingleLine()
            setPadding(48, 32, 48, 16)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.seatpos_rename_title)
            .setView(input)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.seatpos_save) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.renamePosition(position, newName) { success, error ->
                        val msg = if (success) {
                            getString(R.string.seatpos_saved_toast, newName)
                        } else {
                            error ?: getString(R.string.seatpos_rename_failed)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showSetAliasDialog(position: SeatPosition) {
        val options = arrayOf(
            getString(R.string.seatpos_alias_car_1),
            getString(R.string.seatpos_alias_car_2),
            getString(R.string.seatpos_alias_car_3),
            getString(R.string.seatpos_clear_alias)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.seatpos_set_alias)
            .setItems(options) { _, which ->
                val alias = when (which) {
                    0 -> "Pos 1"
                    1 -> "Pos 2"
                    2 -> "Pos 3"
                    else -> null
                }
                viewModel.setAlias(position, alias) { success, error ->
                    val msg = if (success) {
                        getString(R.string.seatpos_saved_toast, position.displayName)
                    } else {
                        error ?: getString(R.string.seatpos_alias_failed)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showSetColourDialog(position: SeatPosition) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ambient_colour_picker, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rvColourPalette)
        rv.layoutManager = GridLayoutManager(requireContext(), 6)

        val palette = viewModel.state.value.palette.takeIf { it.isNotEmpty() }
            ?: LightConstants.AMBIENT_COLOURS.toList()
        var selectedIdx = position.ambientColour ?: 1

        val colourAdapter = AmbientColourAdapter(
            context = requireContext(),
            colours = palette,
            selectedIndex = selectedIdx,
            onColourSelected = { idx -> selectedIdx = idx }
        )
        rv.adapter = colourAdapter

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.seatpos_colour_title)
            .setView(dialogView)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.seatpos_save) { _, _ ->
                viewModel.setAmbientColour(position, selectedIdx) { success, error ->
                    val msg = if (success) {
                        getString(R.string.seatpos_saved_toast, position.displayName)
                    } else {
                        error ?: getString(R.string.seatpos_colour_failed)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDeleteDialog(position: SeatPosition) {
        val uses = viewModel.state.value.automations[position.id]
        val message = if (uses != null) {
            getString(R.string.seatpos_delete_used)
        } else {
            getString(R.string.seatpos_delete_unused)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.seatpos_delete_title, position.displayName))
            .setMessage(message)
            .setNegativeButton(R.string.common_cancel, null)
            .setPositiveButton(R.string.seatpos_delete) { _, _ ->
                viewModel.deletePosition(position) { success, error ->
                    val msg = if (success) {
                        getString(R.string.seatpos_saved_toast, position.displayName)
                    } else {
                        error ?: getString(R.string.seatpos_delete_failed)
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}
