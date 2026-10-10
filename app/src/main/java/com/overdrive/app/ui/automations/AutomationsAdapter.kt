package com.overdrive.app.ui.automations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.overdrive.app.R

class AutomationsAdapter(
    private val onToggleMode: (AutomationItem) -> Unit,
    private val onCycleMode: (AutomationItem) -> Unit,
    private val onTest: (AutomationItem) -> Unit,
    private val onDelete: (AutomationItem) -> Unit
) : ListAdapter<AutomationItem, AutomationsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_automation_rule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvAutomationName)
        private val tvModeBadge: TextView = itemView.findViewById(R.id.tvModeBadge)
        private val switchEnabled: MaterialSwitch = itemView.findViewById(R.id.switchAutomationEnabled)
        private val tvTrigger: TextView = itemView.findViewById(R.id.tvAutomationTrigger)
        private val layoutConditions: View = itemView.findViewById(R.id.layoutAutomationConditions)
        private val tvCondition: TextView = itemView.findViewById(R.id.tvAutomationCondition)
        private val tvConditionHeader: TextView = itemView.findViewById(R.id.tvConditionHeader)
        private val tvActions: TextView = itemView.findViewById(R.id.tvAutomationActions)
        private val layoutElse: View = itemView.findViewById(R.id.layoutAutomationElse)
        private val tvElseActions: TextView = itemView.findViewById(R.id.tvAutomationElseActions)
        private val tvStats: TextView = itemView.findViewById(R.id.tvAutomationStats)
        private val btnTest: MaterialButton = itemView.findViewById(R.id.btnTestAutomation)
        private val btnMode: MaterialButton = itemView.findViewById(R.id.btnCycleMode)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDeleteAutomation)

        fun bind(item: AutomationItem) {
            tvName.text = item.displayName

            // Mode Badge
            when (item.mode) {
                "automatic" -> {
                    tvModeBadge.text = itemView.context.getString(R.string.automation_mode_automatic)
                    tvModeBadge.setBackgroundResource(R.drawable.bg_status_badge_active)
                }
                "manual" -> {
                    tvModeBadge.text = itemView.context.getString(R.string.automation_mode_manual)
                    tvModeBadge.setBackgroundResource(R.drawable.bg_pill_state)
                }
                else -> {
                    tvModeBadge.text = itemView.context.getString(R.string.automation_mode_disabled)
                    tvModeBadge.setBackgroundResource(R.drawable.bg_status_badge_inactive)
                }
            }

            // Switch (temporarily detach listener to avoid trigger during bind)
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = item.isRunningEnabled
            switchEnabled.setOnCheckedChangeListener { _, _ ->
                onToggleMode(item)
            }

            // Trigger & Conditions
            tvTrigger.text = item.triggerSummary
            if (item.conditionSummary.isNotBlank()) {
                layoutConditions.visibility = View.VISIBLE
                tvConditionHeader.text = if (item.conditionLogic == "OR") "ANY" else "ALL"
                tvCondition.text = item.conditionSummary
            } else {
                layoutConditions.visibility = View.GONE
            }

            // Actions & Else
            tvActions.text = item.actionsSummary
            if (!item.elseActionsSummary.isNullOrBlank()) {
                layoutElse.visibility = View.VISIBLE
                tvElseActions.text = item.elseActionsSummary
            } else {
                layoutElse.visibility = View.GONE
            }

            // Stats
            val statsText = if (item.triggerCount > 0) {
                "Fired ${item.triggerCount} times"
            } else {
                "Never fired"
            }
            tvStats.text = statsText

            btnTest.setOnClickListener { onTest(item) }
            btnMode.setOnClickListener { onCycleMode(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<AutomationItem>() {
        override fun areItemsTheSame(oldItem: AutomationItem, newItem: AutomationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AutomationItem, newItem: AutomationItem): Boolean {
            return oldItem == newItem
        }
    }
}
