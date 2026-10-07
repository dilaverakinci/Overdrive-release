package com.overdrive.app.ui.integrations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.overdrive.app.R

class MqttConnectionsAdapter(
    private val onDeleteClicked: (MqttBrokerConfig) -> Unit
) : ListAdapter<MqttBrokerConfig, MqttConnectionsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mqtt_connection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dotStatus: View = itemView.findViewById(R.id.dotMqttConn)
        private val tvName: TextView = itemView.findViewById(R.id.tvMqttConnName)
        private val tvEndpoint: TextView = itemView.findViewById(R.id.tvMqttConnEndpoint)
        private val tvTopic: TextView = itemView.findViewById(R.id.tvMqttConnTopic)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteMqttConn)

        fun bind(item: MqttBrokerConfig) {
            tvName.text = item.name.ifBlank { "MQTT Broker" }
            tvEndpoint.text = item.displayEndpoint
            tvTopic.text = "Topic: ${item.topicPrefix}"

            dotStatus.setBackgroundResource(
                if (item.connected) R.drawable.status_dot_online
                else R.drawable.status_dot_offline
            )

            btnDelete.setOnClickListener { onDeleteClicked(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MqttBrokerConfig>() {
        override fun areItemsTheSame(oldItem: MqttBrokerConfig, newItem: MqttBrokerConfig): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MqttBrokerConfig, newItem: MqttBrokerConfig): Boolean =
            oldItem == newItem
    }
}
