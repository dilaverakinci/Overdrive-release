package com.overdrive.app.ui.keymapping

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

class KeyBindingsAdapter(
    private val onToggleEnabled: (KeyBinding, Boolean) -> Unit,
    private val onTestClick: (KeyAction) -> Unit,
    private val onDeleteClick: (KeyBinding) -> Unit
) : ListAdapter<KeyBinding, KeyBindingsAdapter.BindingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_key_binding, parent, false)
        return BindingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BindingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvKeycodeBadge: TextView = itemView.findViewById(R.id.tvKeycodeBadge)
        private val tvPressTypeBadge: TextView = itemView.findViewById(R.id.tvPressTypeBadge)
        private val tvBlockNativeBadge: TextView = itemView.findViewById(R.id.tvBlockNativeBadge)
        private val tvKeyName: TextView = itemView.findViewById(R.id.tvKeyName)
        private val tvActionSummary: TextView = itemView.findViewById(R.id.tvActionSummary)
        private val switchBindingEnabled: MaterialSwitch = itemView.findViewById(R.id.switchBindingEnabled)
        private val btnTestBinding: MaterialButton = itemView.findViewById(R.id.btnTestBinding)
        private val btnDeleteBinding: MaterialButton = itemView.findViewById(R.id.btnDeleteBinding)

        fun bind(item: KeyBinding) {
            tvKeycodeBadge.text = "[${item.keycode}]"
            tvPressTypeBadge.text = item.pressType.uppercase()
            tvKeyName.text = item.displayKeyName
            tvActionSummary.text = item.displaySummary

            tvBlockNativeBadge.visibility = if (item.blockNativeSingle && item.pressType == "double") View.VISIBLE else View.GONE

            switchBindingEnabled.setOnCheckedChangeListener(null)
            switchBindingEnabled.isChecked = item.enabled
            switchBindingEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnabled(item, isChecked)
            }

            btnTestBinding.setOnClickListener {
                item.action?.let { act -> onTestClick(act) }
            }

            btnDeleteBinding.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<KeyBinding>() {
        override fun areItemsTheSame(oldItem: KeyBinding, newItem: KeyBinding): Boolean {
            return oldItem.keycode == newItem.keycode && oldItem.pressType == newItem.pressType
        }

        override fun areContentsTheSame(oldItem: KeyBinding, newItem: KeyBinding): Boolean {
            return oldItem == newItem
        }
    }
}
