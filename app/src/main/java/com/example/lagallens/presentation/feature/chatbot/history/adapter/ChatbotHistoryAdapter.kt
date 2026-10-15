package com.example.lagallens.presentation.feature.chatbot.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemChatbotHistoryBinding
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryConversation

class ChatbotHistoryAdapter : ListAdapter<ChatbotHistoryConversation, ChatbotHistoryAdapter.HistoryViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder = HistoryViewHolder(
        ItemChatbotHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        private val binding: ItemChatbotHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotHistoryConversation) = with(binding) {
            chatbotHistoryContractName.text = item.contractName
            chatbotHistoryTimestamp.text = item.timestamp
            chatbotHistoryMessageCount.text = item.messageCount
            chatbotHistoryPreview.text = item.preview
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatbotHistoryConversation>() {
        override fun areItemsTheSame(
            oldItem: ChatbotHistoryConversation,
            newItem: ChatbotHistoryConversation
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ChatbotHistoryConversation,
            newItem: ChatbotHistoryConversation
        ): Boolean = oldItem == newItem
    }
}
