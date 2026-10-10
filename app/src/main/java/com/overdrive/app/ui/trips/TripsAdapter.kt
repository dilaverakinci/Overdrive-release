package com.overdrive.app.ui.trips

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.overdrive.app.R
import java.text.SimpleDateFormat
import java.util.*

class TripsAdapter(
    private val onItemClick: (TripRecordItem) -> Unit,
    private val onDeleteClick: (TripRecordItem) -> Unit
) : ListAdapter<TripRecordItem, TripsAdapter.TripViewHolder>(TripDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip_record, parent, false)
        return TripViewHolder(view, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TripViewHolder(
        itemView: View,
        private val onItemClick: (TripRecordItem) -> Unit,
        private val onDeleteClick: (TripRecordItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTripDate: TextView = itemView.findViewById(R.id.tvTripDate)
        private val tvTripDuration: TextView = itemView.findViewById(R.id.tvTripDuration)
        private val tvKinematicTag: TextView = itemView.findViewById(R.id.tvKinematicTag)
        private val tvTripDistance: TextView = itemView.findViewById(R.id.tvTripDistance)
        private val tvTripEnergy: TextView = itemView.findViewById(R.id.tvTripEnergy)
        private val tvTripSpeed: TextView = itemView.findViewById(R.id.tvTripSpeed)
        private val tvTripCost: TextView = itemView.findViewById(R.id.tvTripCost)
        private val layoutScoreBadge: LinearLayout = itemView.findViewById(R.id.layoutScoreBadge)
        private val tvScoreStar: TextView = itemView.findViewById(R.id.tvScoreStar)
        private val tvScoreValue: TextView = itemView.findViewById(R.id.tvScoreValue)
        private val btnDeleteTrip: ImageButton = itemView.findViewById(R.id.btnDeleteTrip)

        private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

        fun bind(item: TripRecordItem) {
            val dateStr = if (item.startTime > 0) dateFormat.format(Date(item.startTime)) else "--"
            tvTripDate.text = dateStr

            val durationMinutes = item.durationSeconds / 60
            tvTripDuration.text = if (durationMinutes >= 60) {
                val hours = durationMinutes / 60
                val mins = durationMinutes % 60
                "• ${hours}h ${mins}m"
            } else {
                "• ${durationMinutes} min"
            }

            if (item.kinematicState.isNotEmpty()) {
                tvKinematicTag.visibility = View.VISIBLE
                tvKinematicTag.text = item.kinematicState.replace('_', ' ')
            } else {
                tvKinematicTag.visibility = View.GONE
            }

            tvTripDistance.text = String.format(Locale.US, "%.1f km", item.distanceKm)

            val energyStr = String.format(Locale.US, "%.1f kWh", item.energyUsedKwh)
            val consumptionStr = if (item.distanceKm > 0.5) {
                String.format(Locale.US, " (%.1f kWh/100km)", item.consumptionKwhPer100Km)
            } else ""
            tvTripEnergy.text = "$energyStr$consumptionStr"

            tvTripSpeed.text = String.format(Locale.US, "avg %.0f km/h", item.avgSpeedKmh)

            val currency = item.currency.ifEmpty { "₺" }
            tvTripCost.text = String.format(Locale.US, "%.2f %s", item.tripCost, currency)

            if (item.overallScore > 0) {
                layoutScoreBadge.visibility = View.VISIBLE
                tvScoreValue.text = item.overallScore.toString()
                val color = when {
                    item.overallScore >= 80 -> Color.parseColor("#00D4AA")
                    item.overallScore >= 60 -> Color.parseColor("#FFB020")
                    else -> Color.parseColor("#EF4444")
                }
                tvScoreStar.setTextColor(color)
                tvScoreValue.setTextColor(color)
            } else {
                layoutScoreBadge.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(item) }
            btnDeleteTrip.setOnClickListener { onDeleteClick(item) }
        }
    }

    class TripDiffCallback : DiffUtil.ItemCallback<TripRecordItem>() {
        override fun areItemsTheSame(oldItem: TripRecordItem, newItem: TripRecordItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TripRecordItem, newItem: TripRecordItem): Boolean =
            oldItem == newItem
    }
}
