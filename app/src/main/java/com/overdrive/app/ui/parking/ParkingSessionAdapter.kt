package com.overdrive.app.ui.parking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.overdrive.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ParkingListItem {
    data class DayHeader(val dayText: String) : ParkingListItem()
    data class SessionCard(val session: ParkingSession) : ParkingListItem()
}

class ParkingSessionAdapter(
    private val onSessionClick: (ParkingSession) -> Unit
) : ListAdapter<ParkingListItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_CARD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ParkingListItem.DayHeader -> VIEW_TYPE_HEADER
            is ParkingListItem.SessionCard -> VIEW_TYPE_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_parking_day_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_parking_session, parent, false)
            SessionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ParkingListItem.DayHeader -> (holder as HeaderViewHolder).bind(item.dayText)
            is ParkingListItem.SessionCard -> (holder as SessionViewHolder).bind(item.session)
        }
    }

    fun submitSessions(sessions: List<ParkingSession>) {
        if (sessions.isEmpty()) {
            submitList(emptyList())
            return
        }

        val items = mutableListOf<ParkingListItem>()
        val dayFormat = SimpleDateFormat("EEEE, d MMMM", Locale("tr"))
        var lastDay = ""

        sessions.forEach { s ->
            val day = dayFormat.format(Date(s.start)).uppercase()
            if (day != lastDay) {
                items.add(ParkingListItem.DayHeader(day))
                lastDay = day
            }
            items.add(ParkingListItem.SessionCard(s))
        }
        submitList(items)
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvParkingDayHeader: TextView = itemView.findViewById(R.id.tvParkingDayHeader)

        fun bind(dayText: String) {
            tvParkingDayHeader.text = dayText
        }
    }

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSessionTime: TextView = itemView.findViewById(R.id.tvSessionTime)
        private val tvSessionLiveTag: TextView = itemView.findViewById(R.id.tvSessionLiveTag)
        private val tvSentryEventsBadge: TextView = itemView.findViewById(R.id.tvSentryEventsBadge)
        private val tvSessionPlace: TextView = itemView.findViewById(R.id.tvSessionPlace)

        private val tvSessionDuration: TextView = itemView.findViewById(R.id.tvSessionDuration)
        private val layoutSessionEnergy: View = itemView.findViewById(R.id.layoutSessionEnergy)
        private val tvSessionEnergy: TextView = itemView.findViewById(R.id.tvSessionEnergy)

        private val layoutSessionSignage: View = itemView.findViewById(R.id.layoutSessionSignage)
        private val tvSessionSignage: TextView = itemView.findViewById(R.id.tvSessionSignage)

        private val tvSessionNeighbours: TextView = itemView.findViewById(R.id.tvSessionNeighbours)
        private val ivNeighboursIcon: ImageView = itemView.findViewById(R.id.ivNeighboursIcon)

        private val tvSessionGps: TextView = itemView.findViewById(R.id.tvSessionGps)
        private val tvSessionSentry: TextView = itemView.findViewById(R.id.tvSessionSentry)

        fun bind(session: ParkingSession) {
            val context = itemView.context

            // Time
            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvSessionTime.text = timeSdf.format(Date(session.start))

            // Live tag
            tvSessionLiveTag.visibility = if (session.isLive) View.VISIBLE else View.GONE

            // Sentry events badge
            tvSentryEventsBadge.text = session.eventsCount.toString()
            if (session.eventsCount == 0) {
                tvSentryEventsBadge.setBackgroundResource(R.drawable.bg_parking_badge_none)
                tvSentryEventsBadge.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            } else {
                tvSentryEventsBadge.setBackgroundResource(R.drawable.bg_parking_badge_active)
                tvSentryEventsBadge.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
            }

            // Place & level text
            val placeText = if (!session.signageLabel.isNullOrBlank()) {
                "${session.place} · ${session.signageLabel}"
            } else {
                session.place
            }
            tvSessionPlace.text = placeText

            // Duration
            val mins = session.durationMs / 60000
            val hours = mins / 60
            val remMins = mins % 60
            tvSessionDuration.text = if (hours > 0) "${hours}s ${remMins}dk" else "${remMins}dk"

            // Energy / SOC
            if (session.energyUsedKwh != null) {
                layoutSessionEnergy.visibility = View.VISIBLE
                val formatted = String.format(Locale.getDefault(), "%.2f kWh", session.energyUsedKwh)
                val prefix = if (session.energyUsedKwh > 0) "+" else ""
                tvSessionEnergy.text = "$prefix$formatted"
            } else if (session.socDelta != null) {
                layoutSessionEnergy.visibility = View.VISIBLE
                val prefix = if (session.socDelta > 0) "+" else ""
                tvSessionEnergy.text = String.format(Locale.getDefault(), "%s%.1f%% SoC", prefix, session.socDelta)
            } else {
                layoutSessionEnergy.visibility = View.GONE
            }

            // Signage capsule
            if (!session.signageLabel.isNullOrBlank()) {
                layoutSessionSignage.visibility = View.VISIBLE
                tvSessionSignage.text = session.signageLabel
            } else {
                layoutSessionSignage.visibility = View.GONE
            }

            // Neighbours capsule
            if (session.neighboursCount > 0) {
                tvSessionNeighbours.text = "${session.neighboursCount} komşu"
                tvSessionNeighbours.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                ivNeighboursIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.brand_primary)
            } else {
                tvSessionNeighbours.text = "0 komşu"
                tvSessionNeighbours.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                ivNeighboursIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.text_muted)
            }

            // GPS Quality
            val gpsLabel = when (session.gpsQuality.uppercase()) {
                "FRESH" -> "GPS güncel"
                "RECENT" -> "GPS yakın"
                "STALE" -> "GPS eski (kapalı otopark?)"
                else -> "GPS yok"
            }
            tvSessionGps.text = gpsLabel

            // Sentry State
            val sentryLabel = when (session.sentryState.lowercase()) {
                "armed" -> "Nöbetçi devrede"
                "lock_wait" -> "Kilit bekleniyor"
                "pipeline_down" -> "Kamera kapalı"
                "suppressed_safe_zone" -> "Güvenli bölge"
                "surveillance_off" -> "Nöbetçi kapalı"
                else -> "Nöbetçi bilinmiyor"
            }
            tvSessionSentry.text = sentryLabel

            itemView.setOnClickListener { onSessionClick(session) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ParkingListItem>() {
        override fun areItemsTheSame(oldItem: ParkingListItem, newItem: ParkingListItem): Boolean {
            return when {
                oldItem is ParkingListItem.DayHeader && newItem is ParkingListItem.DayHeader ->
                    oldItem.dayText == newItem.dayText
                oldItem is ParkingListItem.SessionCard && newItem is ParkingListItem.SessionCard ->
                    oldItem.session.id == newItem.session.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ParkingListItem, newItem: ParkingListItem): Boolean {
            return oldItem == newItem
        }
    }
}
