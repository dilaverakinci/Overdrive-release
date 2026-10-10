package com.overdrive.app.ui.seatpositions

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.overdrive.app.R
import com.overdrive.app.byd.light.LightConstants

class SeatPositionAdapter(
    private val context: Context,
    private val onApplyClick: (SeatPosition) -> Unit,
    private val onSaveOverClick: (SeatPosition) -> Unit,
    private val onRenameClick: (SeatPosition) -> Unit,
    private val onSetColourClick: (SeatPosition) -> Unit,
    private val onSetAliasClick: (SeatPosition) -> Unit,
    private val onDeleteClick: (SeatPosition) -> Unit
) : ListAdapter<SeatPosition, SeatPositionAdapter.ViewHolder>(DiffCallback) {

    var activePositionId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var applyingPositionId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var automations: Map<String, String> = emptyMap()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var canApply: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    object DiffCallback : DiffUtil.ItemCallback<SeatPosition>() {
        override fun areItemsTheSame(oldItem: SeatPosition, newItem: SeatPosition): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SeatPosition, newItem: SeatPosition): Boolean =
            oldItem == newItem
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardSeatPosition)
        val tvName: TextView = view.findViewById(R.id.tvPositionName)
        val tvActiveBadge: TextView = view.findViewById(R.id.tvActiveBadge)
        val tvFromCarChip: TextView = view.findViewById(R.id.tvFromCarChip)
        val tvCarButtonChip: TextView = view.findViewById(R.id.tvCarButtonChip)
        val viewAmbientSwatch: View = view.findViewById(R.id.viewAmbientSwatch)
        val tvAutomationTag: TextView = view.findViewById(R.id.tvAutomationTag)
        val btnApply: MaterialButton = view.findViewById(R.id.btnApply)
        val pbApply: ProgressBar = view.findViewById(R.id.pbApply)
        val btnOverflow: ImageButton = view.findViewById(R.id.btnOverflow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seat_position, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val isApplied = (activePositionId == item.id)
        val isApplying = (applyingPositionId == item.id)

        holder.tvName.text = item.displayName

        // Active State styling
        if (isApplied) {
            holder.tvActiveBadge.visibility = View.VISIBLE
            holder.card.strokeColor = ContextCompat.getColor(context, R.color.brand_primary)
            holder.card.strokeWidth = dpToPx(2)
        } else {
            holder.tvActiveBadge.visibility = View.GONE
            holder.card.strokeColor = ContextCompat.getColor(context, R.color.card_border)
            holder.card.strokeWidth = dpToPx(1)
        }

        // From Car / Slot Chip
        if (item.isFromCar && item.slot != null) {
            holder.tvFromCarChip.visibility = View.VISIBLE
            holder.tvFromCarChip.text = context.getString(R.string.seatpos_from_car_slot, item.slot)
        } else {
            holder.tvFromCarChip.visibility = View.GONE
        }

        // Alias / Car Button Chip
        if (item.alias != null && item.alias.isNotBlank() && item.slot != null) {
            holder.tvCarButtonChip.visibility = View.VISIBLE
            holder.tvCarButtonChip.text = context.getString(R.string.seatpos_car_slot_label, item.slot)
        } else {
            holder.tvCarButtonChip.visibility = View.GONE
        }

        // Ambient Swatch
        val colourIdx = item.ambientColour
        if (colourIdx != null && colourIdx in 1..LightConstants.AMBIENT_COLOURS.size) {
            holder.viewAmbientSwatch.visibility = View.VISIBLE
            try {
                val hex = LightConstants.AMBIENT_COLOURS[colourIdx - 1]
                holder.viewAmbientSwatch.backgroundTintList = ColorStateList.valueOf(Color.parseColor(hex))
            } catch (_: Exception) {
                holder.viewAmbientSwatch.visibility = View.GONE
            }
        } else {
            holder.viewAmbientSwatch.visibility = View.GONE
        }

        // Automation Dependency
        val autoName = automations[item.id]
        if (autoName != null) {
            holder.tvAutomationTag.visibility = View.VISIBLE
            holder.tvAutomationTag.text = context.getString(R.string.seatpos_used_by_one)
        } else {
            holder.tvAutomationTag.visibility = View.GONE
        }

        // Apply Button state
        if (isApplying) {
            holder.btnApply.visibility = View.INVISIBLE
            holder.pbApply.visibility = View.VISIBLE
        } else {
            holder.btnApply.visibility = View.VISIBLE
            holder.pbApply.visibility = View.GONE
            holder.btnApply.isEnabled = canApply
            holder.btnApply.setOnClickListener { onApplyClick(item) }
        }

        // Overflow Menu
        holder.btnOverflow.setOnClickListener { v ->
            showOverflowMenu(v, item)
        }
    }

    private fun showOverflowMenu(anchor: View, item: SeatPosition) {
        val popup = PopupMenu(context, anchor)
        popup.menu.add(0, 1, 0, R.string.seatpos_save_here)
        popup.menu.add(0, 2, 1, R.string.seatpos_change_colour)
        popup.menu.add(0, 3, 2, R.string.seatpos_rename)
        popup.menu.add(0, 4, 3, R.string.seatpos_set_alias)
        popup.menu.add(0, 5, 4, R.string.seatpos_delete)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> { onSaveOverClick(item); true }
                2 -> { onSetColourClick(item); true }
                3 -> { onRenameClick(item); true }
                4 -> { onSetAliasClick(item); true }
                5 -> { onDeleteClick(item); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
