package com.example.lagallens.presentation.feature.chatbot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import com.example.lagallens.R
import com.example.lagallens.databinding.ItemChatbotAssistantMessageBinding
import com.example.lagallens.databinding.ItemChatbotAssistantCitationMessageBinding
import com.example.lagallens.databinding.ItemChatbotAssistantErrorMessageBinding
import com.example.lagallens.databinding.ItemChatbotAssistantGroundedMessageBinding
import com.example.lagallens.databinding.ItemChatbotAssistantNoEvidenceMessageBinding
import com.example.lagallens.databinding.ItemChatbotUserMessageBinding
import com.example.lagallens.databinding.ItemChatbotDraftActionBinding
import com.example.lagallens.databinding.ItemChatbotDraftLoadingBinding
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessage
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor

class ChatbotMessageAdapter(
    private val onCitationClicked: () -> Unit,
    private val onRetryClicked: (Long) -> Unit
) : ListAdapter<ChatbotMessage, RecyclerView.ViewHolder>(DiffCallback) {
    override fun getItemViewType(position: Int): Int = when {
        getItem(position).isDraftAction -> VIEW_TYPE_DRAFT_ACTION
        getItem(position).isDraftLoading -> VIEW_TYPE_DRAFT_LOADING
        getItem(position).error != null -> VIEW_TYPE_ASSISTANT_ERROR
        getItem(position).noEvidence != null -> VIEW_TYPE_ASSISTANT_NO_EVIDENCE
        getItem(position).references.isNotEmpty() -> VIEW_TYPE_ASSISTANT_GROUNDED
        getItem(position).author == ChatbotMessageAuthor.ASSISTANT && getItem(position).citation != null -> VIEW_TYPE_ASSISTANT_CITATION
        getItem(position).author == ChatbotMessageAuthor.ASSISTANT -> VIEW_TYPE_ASSISTANT
        else -> VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        VIEW_TYPE_DRAFT_ACTION -> StaticViewHolder(ItemChatbotDraftActionBinding.inflate(LayoutInflater.from(parent.context), parent, false).root)
        VIEW_TYPE_DRAFT_LOADING -> StaticViewHolder(ItemChatbotDraftLoadingBinding.inflate(LayoutInflater.from(parent.context), parent, false).root)
        VIEW_TYPE_ASSISTANT -> AssistantMessageViewHolder(
            ItemChatbotAssistantMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        VIEW_TYPE_ASSISTANT_CITATION -> AssistantCitationMessageViewHolder(
            ItemChatbotAssistantCitationMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onCitationClicked
        )
        VIEW_TYPE_ASSISTANT_GROUNDED -> AssistantGroundedMessageViewHolder(
            ItemChatbotAssistantGroundedMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        VIEW_TYPE_ASSISTANT_NO_EVIDENCE -> AssistantNoEvidenceMessageViewHolder(
            ItemChatbotAssistantNoEvidenceMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        VIEW_TYPE_ASSISTANT_ERROR -> AssistantErrorMessageViewHolder(
            ItemChatbotAssistantErrorMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onRetryClicked
        )
        else -> UserMessageViewHolder(
            ItemChatbotUserMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AssistantMessageViewHolder -> holder.bind(getItem(position))
            is AssistantCitationMessageViewHolder -> holder.bind(getItem(position))
            is AssistantGroundedMessageViewHolder -> holder.bind(getItem(position))
            is AssistantNoEvidenceMessageViewHolder -> holder.bind(getItem(position))
            is AssistantErrorMessageViewHolder -> holder.bind(getItem(position))
            is UserMessageViewHolder -> holder.bind(getItem(position))
        }
    }

    private class AssistantMessageViewHolder(
        private val binding: ItemChatbotAssistantMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotMessage) {
            binding.chatbotAssistantMessage.text = item.content
            binding.chatbotDraftNotice.isVisible = item.showDraftNotice
        }
    }

    private class StaticViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view)

    private class UserMessageViewHolder(
        private val binding: ItemChatbotUserMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotMessage) {
            binding.chatbotUserMessage.text = item.content
        }
    }

    private class AssistantCitationMessageViewHolder(
        private val binding: ItemChatbotAssistantCitationMessageBinding,
        onCitationClicked: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.chatbotCitationCard.setOnClickListener { onCitationClicked() }
        }

        fun bind(item: ChatbotMessage) = with(binding) {
            val citation = requireNotNull(item.citation)
            chatbotAssistantCitationMessage.text = item.content
            chatbotCitationTitle.text = citation.title
            chatbotCitationDescription.text = citation.description
            chatbotCitationAction.text = citation.actionLabel
        }
    }

    private class AssistantGroundedMessageViewHolder(
        private val binding: ItemChatbotAssistantGroundedMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotMessage) {
            val firstReference = item.references.firstOrNull() ?: return
            val secondReference = item.references.getOrNull(1)
            with(binding) {
                chatbotAssistantGroundedMessage.text = item.content
                chatbotReferenceSectionTitle.text = item.referenceSectionTitle
                val isSingleSource = item.referenceSectionTitle.isNullOrBlank()
                chatbotReferenceDivider.isVisible = !isSingleSource
                chatbotReferenceSectionTitle.isVisible = !isSingleSource
                chatbotReferenceFirstChevron.isVisible = !isSingleSource
                chatbotReferenceFirstTitle.setTextColor(ContextCompat.getColor(root.context, if (isSingleSource) R.color.legal_lens_auth_accent else R.color.legal_lens_primary))
                chatbotReferenceFirstTitle.text = firstReference.title
                chatbotReferenceFirstExcerpt.text = firstReference.excerpt
                chatbotReferenceSecondCard.visibility = if (secondReference == null) android.view.View.GONE else android.view.View.VISIBLE
                chatbotReferenceSecondTitle.text = secondReference?.title.orEmpty()
                chatbotReferenceSecondExcerpt.text = secondReference?.excerpt.orEmpty()
            }
        }
    }

    private class AssistantNoEvidenceMessageViewHolder(
        private val binding: ItemChatbotAssistantNoEvidenceMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotMessage) = with(binding) {
            val noEvidence = requireNotNull(item.noEvidence)
            chatbotNoEvidenceTitle.text = noEvidence.title
            chatbotNoEvidenceMessage.text = noEvidence.message
        }
    }

    private class AssistantErrorMessageViewHolder(
        private val binding: ItemChatbotAssistantErrorMessageBinding,
        private val onRetryClicked: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatbotMessage) = with(binding) {
            val error = requireNotNull(item.error)
            chatbotErrorTitle.text = error.title
            chatbotErrorRetryLabel.text = error.retryLabel
            chatbotErrorRetry.setOnClickListener { onRetryClicked(item.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatbotMessage>() {
        override fun areItemsTheSame(oldItem: ChatbotMessage, newItem: ChatbotMessage): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatbotMessage, newItem: ChatbotMessage): Boolean = oldItem == newItem
    }

    private companion object {
        const val VIEW_TYPE_ASSISTANT = 1
        const val VIEW_TYPE_USER = 2
        const val VIEW_TYPE_ASSISTANT_CITATION = 3
        const val VIEW_TYPE_ASSISTANT_GROUNDED = 4
        const val VIEW_TYPE_ASSISTANT_NO_EVIDENCE = 5
        const val VIEW_TYPE_ASSISTANT_ERROR = 6
        const val VIEW_TYPE_DRAFT_ACTION = 7
        const val VIEW_TYPE_DRAFT_LOADING = 8
    }
}
