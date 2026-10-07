package com.overdrive.app.ui.automations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.overdrive.app.R

class ActionGroupsAdapter(
    private val onRun: (ActionGroupItem) -> Unit,
    private val onDelete: (ActionGroupItem) -> Unit
) : ListAdapter<ActionGroupItem, ActionGroupsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_action_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvGroupCount)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvGroupActionsSummary)
        private val btnRun: MaterialButton = itemView.findViewById(R.id.btnRunGroup)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteGroup)

        fun bind(item: ActionGroupItem) {
            tvName.text = item.name
            tvCount.text = "${item.actionCount} actions"
            tvSummary.text = item.actionsSummary
            btnRun.setOnClickListener { onRun(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ActionGroupItem>() {
        override fun areItemsTheSame(oldItem: ActionGroupItem, newItem: ActionGroupItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActionGroupItem, newItem: ActionGroupItem): Boolean {
            return oldItem == newItem
        }
    }
}
