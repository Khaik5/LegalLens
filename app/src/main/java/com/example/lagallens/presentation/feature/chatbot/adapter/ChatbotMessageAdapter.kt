package com.example.lagallens.presentation.feature.chatbot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.databinding.ItemChatbotAssistantMessageBinding
import com.example.lagallens.databinding.ItemChatbotUserMessageBinding
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessage
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor

class ChatbotMessageAdapter : ListAdapter<ChatbotMessage, RecyclerView.ViewHolder>(DiffCallback) {
    override fun getItemViewType(position: Int): Int = when (getItem(position).author) {
        ChatbotMessageAuthor.ASSISTANT -> VIEW_TYPE_ASSISTANT
        ChatbotMessageAuthor.USER -> VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        VIEW_TYPE_ASSISTANT -> AssistantMessageViewHolder(
            ItemChatbotAssistantMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        else -> UserMessageViewHolder(
            ItemChatbotUserMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AssistantMessageViewHolder -> holder.bind(getItem(position))
            is UserMessageViewHolder -> holder.bind(getItem(position))
        }
    }

    private class AssistantMessageViewHolder(
        private val binding: ItemChatbotAssistantMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotMessage) {
            binding.chatbotAssistantMessage.text = item.content
        }
    }

    private class UserMessageViewHolder(
        private val binding: ItemChatbotUserMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotMessage) {
            binding.chatbotUserMessage.text = item.content
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatbotMessage>() {
        override fun areItemsTheSame(oldItem: ChatbotMessage, newItem: ChatbotMessage): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatbotMessage, newItem: ChatbotMessage): Boolean = oldItem == newItem
    }

    private companion object {
        const val VIEW_TYPE_ASSISTANT = 1
        const val VIEW_TYPE_USER = 2
    }
}
