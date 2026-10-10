package com.overdrive.app.ui.assistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.overdrive.app.R

class AssistantChatAdapter : ListAdapter<ChatMessage, AssistantChatAdapter.MessageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutUser: View = itemView.findViewById(R.id.layoutUserMessage)
        private val tvUser: TextView = itemView.findViewById(R.id.tvUserMessage)
        private val layoutAssistant: View = itemView.findViewById(R.id.layoutAssistantMessage)
        private val tvAssistant: TextView = itemView.findViewById(R.id.tvAssistantMessage)
        private val pbPending: ProgressBar = itemView.findViewById(R.id.pbAssistantPending)

        fun bind(message: ChatMessage) {
            if (message.isUser) {
                layoutUser.visibility = View.VISIBLE
                layoutAssistant.visibility = View.GONE
                tvUser.text = message.text
            } else {
                layoutUser.visibility = View.GONE
                layoutAssistant.visibility = View.VISIBLE
                if (message.isPending) {
                    tvAssistant.text = "Thinking…"
                    pbPending.visibility = View.VISIBLE
                } else {
                    tvAssistant.text = message.text
                    pbPending.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem == newItem
        }
    }
}
