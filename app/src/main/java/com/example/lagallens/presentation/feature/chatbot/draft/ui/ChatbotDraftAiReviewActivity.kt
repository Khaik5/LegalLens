package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.example.lagallens.BuildConfig
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotDraftAiReviewBinding
import com.example.lagallens.domain.repository.DraftEditProposal
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiReviewUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.ChatbotDraftAiReviewViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ChatbotDraftAiReviewActivity : DraftSessionActivity() {
    private lateinit var binding: ActivityChatbotDraftAiReviewBinding
    private val viewModel by viewModels<ChatbotDraftAiReviewViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        val proposal = proposalFrom(intent) ?: run { finish(); return }
        useEdgeToEdgeSystemBars()
        binding = ActivityChatbotDraftAiReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        bindActions()
        viewModel.onEvent(ChatbotDraftAiReviewUiEvent.Opened(proposal))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun bindActions() = with(binding) {
        draftAiReviewBack.setOnClickListener { finish() }
        draftAiReviewAccept.setOnClickListener { viewModel.onEvent(ChatbotDraftAiReviewUiEvent.AcceptClicked) }
        draftAiReviewReject.setOnClickListener { viewModel.onEvent(ChatbotDraftAiReviewUiEvent.RejectClicked) }
        draftAiReviewContinue.setOnClickListener {
            val proposal = viewModel.uiState.value.proposal
            if (viewModel.uiState.value.accepted && proposal != null) {
                MaterialAlertDialogBuilder(this@ChatbotDraftAiReviewActivity)
                    .setTitle(R.string.chatbot_draft_ai_approval_title)
                    .setMessage(R.string.chatbot_draft_ai_approval_body)
                    .setNegativeButton(R.string.chatbot_draft_ai_cancel, null)
                    .setPositiveButton(R.string.chatbot_draft_ai_approval_confirm) { _, _ ->
                        startActivity(
                            ChatbotDraftAiApplyActivity.createIntent(
                                this@ChatbotDraftAiReviewActivity,
                                BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_FAILURE, false),
                                intent.getStringExtra(EXTRA_SOURCE_DRAFT_ID),
                                intent.getStringExtra(EXTRA_CONTRACT_ID),
                                proposal.proposalId
                            )
                        )
                    }
                    .show()
            }
        }
    }

    private fun render(state: com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiReviewUiState) = with(binding) {
        val accepted = state.accepted
        state.proposal?.let { proposal ->
            draftAiReviewClause.text = proposal.clauseTitle
            draftAiReviewBefore.text = proposal.originalText
            draftAiReviewAfter.text = proposal.proposedText
            draftAiReviewReason.text = proposal.reason
        }
        draftAiReviewCard.setBackgroundResource(if (accepted) R.drawable.bg_chatbot_ai_review_selected else R.drawable.bg_chatbot_source_document)
        draftAiReviewStatus.text = getString(if (accepted) R.string.chatbot_draft_ai_review_selected else R.string.chatbot_draft_ai_review_rejected)
        draftAiReviewStatus.setBackgroundResource(if (accepted) R.drawable.bg_chatbot_source_badge else R.drawable.bg_chatbot_new_state_suggestion)
        draftAiReviewStatus.setTextColor(getColor(if (accepted) R.color.legal_lens_auth_accent else R.color.legal_lens_secondary_text))
        draftAiReviewAccept.setText(getString(if (accepted) R.string.chatbot_draft_ai_review_accept_selected else R.string.chatbot_draft_ai_review_accept))
        draftAiReviewReject.setText(getString(if (accepted) R.string.chatbot_draft_ai_review_reject else R.string.chatbot_draft_ai_review_reject_selected))
        draftAiReviewAccept.setBackgroundResource(if (accepted) R.drawable.bg_chatbot_primary_action else R.drawable.bg_chatbot_new_state_suggestion)
        draftAiReviewReject.setBackgroundResource(if (accepted) R.drawable.bg_chatbot_new_state_suggestion else R.drawable.bg_chatbot_primary_action)
        draftAiReviewAccept.setTextColor(getColor(if (accepted) R.color.white else R.color.legal_lens_primary))
        draftAiReviewReject.setTextColor(getColor(if (accepted) R.color.legal_lens_primary else R.color.white))
        draftAiReviewNote.text = getString(if (accepted) R.string.chatbot_draft_ai_review_note_accepted else R.string.chatbot_draft_ai_review_note_rejected)
        draftAiReviewCount.text = getString(if (accepted) R.string.chatbot_draft_ai_review_count_accepted else R.string.chatbot_draft_ai_review_count_rejected)
        draftAiReviewContinue.isEnabled = accepted
        draftAiReviewContinue.alpha = if (accepted) 1f else 0.6f
    }

    companion object {
        private const val EXTRA_PREVIEW_FAILURE = "chatbot_draft_ai_review_apply_failure"
        private const val EXTRA_SOURCE_DRAFT_ID = "chatbot_draft_ai_review_source_draft_id"
        private const val EXTRA_CONTRACT_ID = "chatbot_draft_ai_review_contract_id"
        private const val EXTRA_PROPOSAL_ID = "chatbot_draft_ai_review_proposal_id"
        private const val EXTRA_CLAUSE_TITLE = "chatbot_draft_ai_review_clause_title"
        private const val EXTRA_ORIGINAL_TEXT = "chatbot_draft_ai_review_original_text"
        private const val EXTRA_PROPOSED_TEXT = "chatbot_draft_ai_review_proposed_text"
        private const val EXTRA_REASON = "chatbot_draft_ai_review_reason"

        private fun proposalFrom(intent: Intent): DraftEditProposal? {
            val proposalId = intent.getStringExtra(EXTRA_PROPOSAL_ID)?.takeIf(String::isNotBlank) ?: return null
            return DraftEditProposal(
                proposalId,
                intent.getStringExtra(EXTRA_CLAUSE_TITLE).orEmpty(),
                intent.getStringExtra(EXTRA_ORIGINAL_TEXT).orEmpty(),
                intent.getStringExtra(EXTRA_PROPOSED_TEXT).orEmpty(),
                intent.getStringExtra(EXTRA_REASON).orEmpty()
            )
        }

        fun createIntent(context: Context, previewFailure: Boolean, sourceDraftId: String?, contractId: String?, proposal: DraftEditProposal) = Intent(context, ChatbotDraftAiReviewActivity::class.java)
            .putExtra(EXTRA_PREVIEW_FAILURE, previewFailure)
            .putExtra(EXTRA_SOURCE_DRAFT_ID, sourceDraftId)
            .putExtra(EXTRA_CONTRACT_ID, contractId)
            .putExtra(EXTRA_PROPOSAL_ID, proposal.proposalId)
            .putExtra(EXTRA_CLAUSE_TITLE, proposal.clauseTitle)
            .putExtra(EXTRA_ORIGINAL_TEXT, proposal.originalText)
            .putExtra(EXTRA_PROPOSED_TEXT, proposal.proposedText)
            .putExtra(EXTRA_REASON, proposal.reason)
            .also { DraftSessionGuard.stamp(context, it) }
    }
}
