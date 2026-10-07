package com.overdrive.app.ui.parking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.overdrive.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParkingSessionAdapter(
    private val onSessionClick: (ParkingSession) -> Unit
) : ListAdapter<ParkingSession, ParkingSessionAdapter.SessionViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_parking_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSessionTime: TextView = itemView.findViewById(R.id.tvSessionTime)
        private val tvSessionLiveTag: TextView = itemView.findViewById(R.id.tvSessionLiveTag)
        private val tvSentryEventsBadge: TextView = itemView.findViewById(R.id.tvSentryEventsBadge)
        private val tvSessionPlace: TextView = itemView.findViewById(R.id.tvSessionPlace)
        private val tvSignageLevel: TextView = itemView.findViewById(R.id.tvSignageLevel)
        private val tvSessionDuration: TextView = itemView.findViewById(R.id.tvSessionDuration)
        private val tvSessionEnergy: TextView = itemView.findViewById(R.id.tvSessionEnergy)
        private val layoutSessionEnergy: View = itemView.findViewById(R.id.layoutSessionEnergy)
        private val tvSessionNeighbours: TextView = itemView.findViewById(R.id.tvSessionNeighbours)
        private val layoutSessionNeighbours: View = itemView.findViewById(R.id.layoutSessionNeighbours)

        fun bind(session: ParkingSession) {
            val context = itemView.context

            // Time & Date
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            tvSessionTime.text = sdf.format(Date(session.start))

            // Live tag
            tvSessionLiveTag.visibility = if (session.isLive) View.VISIBLE else View.GONE

            // Sentry events badge
            tvSentryEventsBadge.text = session.eventsCount.toString()
            when {
                session.eventsCount == 0 -> {
                    tvSentryEventsBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
                    tvSentryEventsBadge.backgroundTintList = ContextCompat.getColorStateList(context, R.color.brand_cyan_glow)
                }
                session.eventsCount in 1..3 -> {
                    tvSentryEventsBadge.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                    tvSentryEventsBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(0x33F59E0B)
                }
                else -> {
                    tvSentryEventsBadge.setTextColor(ContextCompat.getColor(context, R.color.status_error))
                    tvSentryEventsBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(0x33EF4444)
                }
            }

            // Place & Signage Level
            tvSessionPlace.text = session.place
            if (!session.signageLabel.isNullOrBlank()) {
                tvSignageLevel.visibility = View.VISIBLE
                tvSignageLevel.text = session.signageLabel
            } else {
                tvSignageLevel.visibility = View.GONE
            }

            // Duration
            val mins = session.durationMs / 60000
            val hours = mins / 60
            val remMins = mins % 60
            tvSessionDuration.text = if (hours > 0) "${hours}s ${remMins}dk" else "${remMins}dk"

            // Energy / SOC
            if (session.energyUsedKwh != null && session.energyUsedKwh > 0.0) {
                layoutSessionEnergy.visibility = View.VISIBLE
                tvSessionEnergy.text = String.format(Locale.getDefault(), "%.1f kWh", session.energyUsedKwh)
            } else if (session.socStart != null && session.socEnd != null) {
                val diff = session.socEnd - session.socStart
                layoutSessionEnergy.visibility = View.VISIBLE
                tvSessionEnergy.text = if (diff >= 0) "+$diff%" else "$diff%"
            } else {
                layoutSessionEnergy.visibility = View.GONE
            }

            // Neighbours
            if (session.neighboursCount > 0) {
                layoutSessionNeighbours.visibility = View.VISIBLE
                tvSessionNeighbours.text = "${session.neighboursCount} Komşu"
            } else {
                layoutSessionNeighbours.visibility = View.GONE
            }

            itemView.setOnClickListener { onSessionClick(session) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ParkingSession>() {
        override fun areItemsTheSame(oldItem: ParkingSession, newItem: ParkingSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ParkingSession, newItem: ParkingSession): Boolean {
            return oldItem == newItem
        }
    }
}
