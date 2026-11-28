package com.example.lagallens.presentation.feature.chatbot.history.adapter

import android.content.Context
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
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class ChatbotHistoryAdapter(
    private val onClick: (ChatbotHistoryConversation) -> Unit,
    private val onDelete: (ChatbotHistoryConversation) -> Unit = {}
) : ListAdapter<ChatbotHistoryConversation, ChatbotHistoryAdapter.HistoryViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder = HistoryViewHolder(
        ItemChatbotHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)
        // The list is newest first, so a day label opens each run of conversations from the same day.
        val day = item.updatedAtEpochMillis?.toLocalDate()
        val previousDay = if (position > 0) getItem(position - 1).updatedAtEpochMillis?.toLocalDate() else null
        val dayLabel = day?.takeIf { position == 0 || it != previousDay }?.let { dayLabel(holder.itemView.context, it, item.updatedAtEpochMillis) }
        holder.bind(item, dayLabel, onClick, onDelete)
    }

    override fun onCurrentListChanged(
        previousList: MutableList<ChatbotHistoryConversation>,
        currentList: MutableList<ChatbotHistoryConversation>
    ) {
        // A day label depends on the item above it, so removing or filtering one item can move the label.
        if (previousList.isNotEmpty()) notifyItemRangeChanged(0, currentList.size)
    }

    class HistoryViewHolder(
        private val binding: ItemChatbotHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init { binding.chatbotHistoryCard.bindPressFeedback() }
        fun bind(
            item: ChatbotHistoryConversation,
            dayLabel: String?,
            onClick: (ChatbotHistoryConversation) -> Unit,
            onDelete: (ChatbotHistoryConversation) -> Unit
        ) = with(binding) {
            chatbotHistoryDayLabel.text = dayLabel
            chatbotHistoryDayLabel.isVisible = dayLabel != null
            chatbotHistoryContractName.text = item.contractName
            chatbotHistoryTimestamp.text = item.timestamp
            chatbotHistoryMessageCount.text = item.messageCountQuantity?.let { count ->
                root.resources.getQuantityString(R.plurals.chatbot_history_message_count, count, count)
            } ?: item.messageCount
            chatbotHistoryPreview.text = item.preview
            chatbotHistoryContinue.isVisible = item.isDraftRequest
            chatbotHistoryIcon.isVisible = item.isDraftRequest
            chatbotHistoryMessageCount.isVisible = item.messageCountQuantity != null || item.messageCount.isNotBlank()
            chatbotHistoryCard.setBackgroundResource(if (item.isDraftRequest) R.drawable.bg_chatbot_draft_review else R.drawable.bg_chatbot_source_document)
            chatbotHistoryCard.setOnClickListener { onClick(item) }
            chatbotHistoryCard.setOnLongClickListener { onDelete(item); true }
            chatbotHistoryDelete.setOnClickListener { onDelete(item) }
            chatbotHistoryContinue.setOnClickListener { onClick(item) }
        }
    }

    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun dayLabel(context: Context, day: LocalDate, epochMillis: Long?): String {
        val today = LocalDate.now(ZoneId.systemDefault())
        return when (day) {
            today -> context.getString(R.string.chatbot_history_today)
            today.minusDays(1) -> context.getString(R.string.chatbot_history_yesterday)
            else -> DateFormat.getDateInstance(DateFormat.MEDIUM, context.resources.configuration.locales[0]).format(Date(epochMillis ?: 0L))
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
