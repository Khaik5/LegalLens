package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivityChatbotDraftAiApplyBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.ChatbotDraftAiApplyViewModel
import kotlinx.coroutines.launch

class ChatbotDraftAiApplyActivity : DraftSessionActivity() {
    private lateinit var binding: ActivityChatbotDraftAiApplyBinding
    private val viewModel by viewModels<ChatbotDraftAiApplyViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        useEdgeToEdgeSystemBars()
        binding = ActivityChatbotDraftAiApplyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        bindActions()
        viewModel.onEvent(
            ChatbotDraftAiApplyUiEvent.Opened(
                intent.getBooleanExtra(EXTRA_PREVIEW_FAILURE, false),
                intent.getStringExtra(EXTRA_SOURCE_DRAFT_ID),
                intent.getStringExtra(EXTRA_CONTRACT_ID),
                intent.getStringExtra(EXTRA_PROPOSAL_ID).orEmpty()
            )
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun bindActions() = with(binding) {
        draftAiApplyBack.setOnClickListener { finish() }
        draftAiApplyRetry.setOnClickListener { viewModel.onEvent(ChatbotDraftAiApplyUiEvent.RetryClicked) }
        draftAiApplyReturn.setOnClickListener { finish() }
        draftAiApplyView.setOnClickListener {
            startActivity(AppliedDraftPreviewActivity.createIntent(this@ChatbotDraftAiApplyActivity, draftId = viewModel.uiState.value.draftId, contractId = intent.getStringExtra(EXTRA_CONTRACT_ID)))
        }
        draftAiApplyManage.setOnClickListener {
            startActivity(ChatbotDraftVersionsActivity.createIntent(this@ChatbotDraftAiApplyActivity, contractId = intent.getStringExtra(EXTRA_CONTRACT_ID)))
        }
    }

    private fun render(state: com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyUiState) = with(binding) {
        val applying = state.status == ChatbotDraftAiApplyStatus.APPLYING
        val error = state.status == ChatbotDraftAiApplyStatus.ERROR
        val success = state.status == ChatbotDraftAiApplyStatus.SUCCESS
        draftAiApplyApplying.isVisible = applying
        draftAiApplyError.isVisible = error
        draftAiApplySuccess.isVisible = success
        draftAiApplyPending.isVisible = applying
        draftAiApplyRetry.isVisible = error
        draftAiApplyReturn.isVisible = error
        draftAiApplyView.isVisible = success
        draftAiApplyManage.isVisible = success
    }

    companion object {
        private const val EXTRA_PREVIEW_FAILURE = "chatbot_draft_apply_preview_failure"
        private const val EXTRA_SOURCE_DRAFT_ID = "chatbot_draft_apply_source_draft_id"
        private const val EXTRA_CONTRACT_ID = "chatbot_draft_apply_contract_id"
        private const val EXTRA_PROPOSAL_ID = "chatbot_draft_apply_proposal_id"
        fun createIntent(context: Context, previewFailure: Boolean, sourceDraftId: String?, contractId: String?, proposalId: String) =
            Intent(context, ChatbotDraftAiApplyActivity::class.java)
                .putExtra(EXTRA_PREVIEW_FAILURE, previewFailure)
                .putExtra(EXTRA_SOURCE_DRAFT_ID, sourceDraftId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
                .putExtra(EXTRA_PROPOSAL_ID, proposalId)
            .also { DraftSessionGuard.stamp(context, it) }
    }
}
