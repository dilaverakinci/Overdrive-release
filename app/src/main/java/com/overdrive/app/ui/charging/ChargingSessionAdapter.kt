package com.overdrive.app.ui.charging

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.overdrive.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChargingSessionAdapter(
    private val onSessionClick: (ChargingSession) -> Unit,
    private val onDeleteClick: (ChargingSession) -> Unit
) : ListAdapter<ChargingSession, ChargingSessionAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    object DiffCallback : DiffUtil.ItemCallback<ChargingSession>() {
        override fun areItemsTheSame(oldItem: ChargingSession, newItem: ChargingSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChargingSession, newItem: ChargingSession): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardChargingSession)
        val iconContainer: FrameLayout = itemView.findViewById(R.id.iconContainer)
        val ivSessionType: ImageView = itemView.findViewById(R.id.ivSessionType)
        val tvSessionType: TextView = itemView.findViewById(R.id.tvSessionType)
        val tvSessionDate: TextView = itemView.findViewById(R.id.tvSessionDate)
        val tvLivePill: TextView = itemView.findViewById(R.id.tvLivePill)
        val tvSessionEnergy: TextView = itemView.findViewById(R.id.tvSessionEnergy)
        val tvSessionCost: TextView = itemView.findViewById(R.id.tvSessionCost)
        val tvSessionSoc: TextView = itemView.findViewById(R.id.tvSessionSoc)
        val tvSessionDuration: TextView = itemView.findViewById(R.id.tvSessionDuration)
        val tvSessionLocation: TextView = itemView.findViewById(R.id.tvSessionLocation)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_charging_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = getItem(position)
        val ctx = holder.itemView.context

        // DC vs AC styling
        val isDc = session.isDc == true
        val avgPower = session.avgPower ?: 0.0
        val typeLabel = when {
            isDc -> ctx.getString(R.string.charge_type_dc)
            avgPower >= 11.0 -> ctx.getString(R.string.charge_type_fast)
            else -> ctx.getString(R.string.charge_type_slow)
        }
        holder.tvSessionType.text = typeLabel

        if (isDc) {
            val dcColor = Color.parseColor("#2196F3")
            holder.ivSessionType.imageTintList = ColorStateList.valueOf(dcColor)
            holder.iconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#252196F3"))
        } else {
            val acColor = Color.parseColor("#00D4AA")
            holder.ivSessionType.imageTintList = ColorStateList.valueOf(acColor)
            holder.iconContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2500D4AA"))
        }

        // Date / Time
        if (session.startTime > 0) {
            holder.tvSessionDate.text = dateFormat.format(Date(session.startTime))
        } else {
            holder.tvSessionDate.text = "--"
        }

        // Live badge
        holder.tvLivePill.visibility = if (session.inProgress && session.chargingNow) View.VISIBLE else View.GONE

        // Energy
        val energy = session.energyAdded
        val prefix = if (session.isEstimated) "~" else ""
        holder.tvSessionEnergy.text = if (energy != null && energy > 0) {
            "$prefix${String.format(Locale.US, "%.1f", energy)} kWh"
        } else {
            "-- kWh"
        }

        // Cost
        val cost = session.cost
        val currency = session.currency
        holder.tvSessionCost.text = if (cost != null && cost >= 0) {
            "${String.format(Locale.US, "%.2f", cost)} $currency".trim()
        } else {
            "--"
        }

        // SoC
        val startSoc = session.startSoc
        val endSoc = session.endSoc
        if (startSoc != null || endSoc != null) {
            val startText = startSoc?.let { "${Math.round(it)}%" } ?: "--"
            val endText = endSoc?.let { "${Math.round(it)}%" } ?: "--"
            holder.tvSessionSoc.text = "$startText → $endText"
            holder.tvSessionSoc.visibility = View.VISIBLE
        } else {
            holder.tvSessionSoc.visibility = View.GONE
        }

        // Duration
        val dur = session.durationMinutes
        holder.tvSessionDuration.text = if (dur != null && dur > 0) {
            "$dur min"
        } else if (session.inProgress) {
            ctx.getString(R.string.charge_in_progress)
        } else {
            "--"
        }

        // Location
        val place = session.placeLabel
        if (!place.isNullOrEmpty()) {
            holder.tvSessionLocation.text = place
            holder.tvSessionLocation.visibility = View.VISIBLE
        } else {
            holder.tvSessionLocation.visibility = View.GONE
        }

        // Actions
        holder.btnDelete.visibility = if (session.inProgress) View.GONE else View.VISIBLE
        holder.btnDelete.setOnClickListener { onDeleteClick(session) }
        holder.card.setOnClickListener { onSessionClick(session) }
    }
}
