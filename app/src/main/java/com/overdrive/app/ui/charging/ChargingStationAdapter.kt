package com.overdrive.app.ui.charging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.overdrive.app.R
import java.util.Locale

class ChargingStationAdapter(
    private val onNavigateClick: (EvStationItem) -> Unit,
    private val onItemClick: (EvStationItem) -> Unit
) : ListAdapter<EvStationItem, ChargingStationAdapter.StationViewHolder>(StationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_charging_station, parent, false)
        return StationViewHolder(view, onNavigateClick, onItemClick)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class StationViewHolder(
        itemView: View,
        private val onNavigateClick: (EvStationItem) -> Unit,
        private val onItemClick: (EvStationItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvOperator: TextView = itemView.findViewById(R.id.tvStationOperator)
        private val tvPower: TextView = itemView.findViewById(R.id.tvStationPower)
        private val tvSockets: TextView = itemView.findViewById(R.id.tvStationSockets)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvStationDistance)
        private val tvName: TextView = itemView.findViewById(R.id.tvStationName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvStationAddress)
        private val tvDcPrice: TextView = itemView.findViewById(R.id.tvStationDcPrice)
        private val tvAcPrice: TextView = itemView.findViewById(R.id.tvStationAcPrice)
        private val btnNavigate: MaterialButton = itemView.findViewById(R.id.btnNavigateStation)

        fun bind(station: EvStationItem) {
            val context = itemView.context

            tvOperator.text = if (station.operator.isNotEmpty()) station.operator else "EV"
            
            val typeStr = if (station.chargingType.isNotEmpty()) station.chargingType else "AC"
            tvPower.text = "${Math.round(station.maxPowerKw)} kW $typeStr"

            if (station.socketCount > 0) {
                tvSockets.visibility = View.VISIBLE
                tvSockets.text = context.getString(R.string.charge_stations_sockets, station.socketCount)
            } else {
                tvSockets.visibility = View.GONE
            }

            // Distance format
            if (station.distanceMeters < 1000) {
                tvDistance.text = "${Math.round(station.distanceMeters)} m"
            } else {
                tvDistance.text = String.format(Locale.getDefault(), "%.1f km", station.distanceMeters / 1000.0)
            }

            tvName.text = if (station.name.isNotEmpty()) station.name else station.operator

            // Address format: District / City • Street
            val locParts = mutableListOf<String>()
            if (station.district.isNotEmpty() && station.city.isNotEmpty()) {
                locParts.add("${station.district} / ${station.city}")
            } else if (station.city.isNotEmpty()) {
                locParts.add(station.city)
            }
            if (station.address.isNotEmpty()) {
                locParts.add(station.address)
            }
            tvAddress.text = if (locParts.isNotEmpty()) locParts.joinToString(" • ") else "Konum bilgisi mevcut değil"

            // Prices
            if (station.dcPrice > 0) {
                tvDcPrice.visibility = View.VISIBLE
                tvDcPrice.text = String.format(Locale.getDefault(), "DC: %.2f ₺", station.dcPrice)
            } else {
                tvDcPrice.visibility = View.GONE
            }

            if (station.acPrice > 0) {
                tvAcPrice.visibility = View.VISIBLE
                tvAcPrice.text = String.format(Locale.getDefault(), "AC: %.2f ₺", station.acPrice)
            } else {
                tvAcPrice.visibility = View.GONE
            }

            // If both prices are 0/empty, show standard rate notice
            if (station.dcPrice <= 0 && station.acPrice <= 0) {
                tvAcPrice.visibility = View.VISIBLE
                tvAcPrice.text = "Tarife: Standart"
            }

            btnNavigate.setOnClickListener {
                onNavigateClick(station)
            }

            itemView.setOnClickListener {
                onItemClick(station)
            }
        }
    }

    private class StationDiffCallback : DiffUtil.ItemCallback<EvStationItem>() {
        override fun areItemsTheSame(oldItem: EvStationItem, newItem: EvStationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EvStationItem, newItem: EvStationItem): Boolean {
            return oldItem == newItem
        }
    }
}
