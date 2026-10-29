package com.example.lagallens.presentation.feature.chatbot.history.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemChatbotHistoryBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryConversation

class ChatbotHistoryAdapter(private val onClick: (ChatbotHistoryConversation) -> Unit) : ListAdapter<ChatbotHistoryConversation, ChatbotHistoryAdapter.HistoryViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder = HistoryViewHolder(
        ItemChatbotHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class HistoryViewHolder(
        private val binding: ItemChatbotHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init { binding.root.bindPressFeedback() }
        fun bind(item: ChatbotHistoryConversation, onClick: (ChatbotHistoryConversation) -> Unit) = with(binding) {
            chatbotHistoryContractName.text = item.contractName
            chatbotHistoryTimestamp.text = item.timestamp
            chatbotHistoryMessageCount.text = item.messageCount
            chatbotHistoryPreview.text = item.preview
            chatbotHistoryContinue.isVisible = item.isDraftRequest
            chatbotHistoryIcon.isVisible = item.isDraftRequest
            chatbotHistoryMessageCount.isVisible = item.messageCount.isNotBlank()
            root.setBackgroundResource(if (item.isDraftRequest) R.drawable.bg_chatbot_draft_review else R.drawable.bg_chatbot_source_document)
            root.setOnClickListener { onClick(item) }
            chatbotHistoryContinue.setOnClickListener { onClick(item) }
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
