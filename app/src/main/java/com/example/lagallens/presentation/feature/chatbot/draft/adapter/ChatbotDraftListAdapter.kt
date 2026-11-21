package com.example.lagallens.presentation.feature.chatbot.draft.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemChatbotDraftListBinding
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListItem

class ChatbotDraftListAdapter(
    private val onItemClick: (ChatbotDraftListItem) -> Unit
) : ListAdapter<ChatbotDraftListItem, ChatbotDraftListAdapter.DraftViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DraftViewHolder(
        ItemChatbotDraftListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) = holder.bind(getItem(position))

    inner class DraftViewHolder(private val binding: ItemChatbotDraftListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotDraftListItem) = with(binding) {
            val context = root.context
            draftListItemTitle.text = item.title.ifBlank { context.getString(R.string.draft_list_untitled) }
            val updated = DateUtils.formatDateTime(context, item.updatedAt, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME)
            val versions = context.resources.getQuantityString(R.plurals.draft_list_version_count, item.versionCount, item.versionCount)
            draftListItemMeta.text = context.getString(R.string.draft_list_item_meta, context.getString(R.string.draft_list_item_updated, updated), versions)
            draftListItemBadge.setText(if (item.isSaved) R.string.draft_list_saved else R.string.draft_list_unsaved)
            root.setOnClickListener { onItemClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatbotDraftListItem>() {
        override fun areItemsTheSame(oldItem: ChatbotDraftListItem, newItem: ChatbotDraftListItem) = oldItem.draftId == newItem.draftId
        override fun areContentsTheSame(oldItem: ChatbotDraftListItem, newItem: ChatbotDraftListItem) = oldItem == newItem
    }
}
