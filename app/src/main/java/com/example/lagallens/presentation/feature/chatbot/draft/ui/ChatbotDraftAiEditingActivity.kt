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
import com.example.lagallens.R
import com.example.lagallens.BuildConfig
import com.example.lagallens.databinding.ActivityChatbotDraftAiEditBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.ChatbotDraftAiEditViewModel
import kotlinx.coroutines.launch

class ChatbotDraftAiEditingActivity : DraftSessionActivity() {
    private lateinit var binding: ActivityChatbotDraftAiEditBinding
    private val viewModel by viewModels<ChatbotDraftAiEditViewModel> {
        val app = application as com.example.lagallens.LegalLensApplication
        ChatbotDraftAiEditViewModel.Factory(DraftSessionGuard.owner(intent), app::currentSessionKey, app.observeSessionKeys()) {
            app.draftAiRepository(DraftSessionGuard.owner(intent))
        }
    }
    private val scope by lazy { intent.getStringExtra(EXTRA_SCOPE).orEmpty() }
    private val request by lazy { intent.getStringExtra(EXTRA_REQUEST).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!guardDraftCreate()) return
        useEdgeToEdgeSystemBars()
        binding = ActivityChatbotDraftAiEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        bindActions()
        viewModel.onEvent(
            ChatbotDraftAiEditUiEvent.Opened(
                scope, request, intent.getBooleanExtra(EXTRA_FAILURE, false),
                intent.getStringExtra(EXTRA_SOURCE_DRAFT_ID), intent.getStringExtra(EXTRA_CONTRACT_ID), intent.getStringExtra(EXTRA_CONVERSATION_ID)
            )
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect { if (isDraftSessionValid()) render(it) } }
        }
    }

    private fun bindActions() = with(binding) {
        draftAiBack.setOnClickListener { finish() }
        draftAiReturn.setOnClickListener { finish() }
        draftAiRetry.setOnClickListener { viewModel.onEvent(ChatbotDraftAiEditUiEvent.RetryClicked) }
        draftAiEditRequest.setOnClickListener {
            if (!isDraftSessionValid()) return@setOnClickListener
            setResult(RESULT_EDIT_REQUEST, Intent().putExtra(EXTRA_SCOPE, scope).putExtra(EXTRA_REQUEST, request))
            finish()
        }
        draftAiReview.setOnClickListener {
            reviewLauncherStart()
        }
    }

    private fun reviewLauncherStart() {
        val proposal = viewModel.uiState.value.proposal ?: return
        startActivity(
            ChatbotDraftAiReviewActivity.createIntent(
                this,
                BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_APPLY_FAILURE, false),
                intent.getStringExtra(EXTRA_SOURCE_DRAFT_ID),
                intent.getStringExtra(EXTRA_CONTRACT_ID),
                proposal
            )
        )
    }

    private fun render(state: com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState) = with(binding) {
        val loading = state.status == ChatbotDraftAiEditStatus.LOADING
        val error = state.status == ChatbotDraftAiEditStatus.ERROR
        val ready = state.status == ChatbotDraftAiEditStatus.READY
        draftAiSourceName.text = getString(R.string.chatbot_draft_document_name, ChatbotDraftUiState.DRAFT_VERSION)
        draftAiRequestCard.text = state.request
        draftAiSourceBadge.text = getString(R.string.chatbot_draft_ai_source_badge, ChatbotDraftUiState.DRAFT_VERSION)
        draftAiScope.text = getString(if (state.scope == com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState.WHOLE_CONTRACT_SCOPE) R.string.chatbot_draft_ai_request_label_whole else R.string.chatbot_draft_ai_request_label_selected)
        draftAiLoading.isVisible = loading
        draftAiError.isVisible = error
        draftAiReady.isVisible = ready
        draftAiRetry.isVisible = error
        draftAiEditRequest.isVisible = error
        draftAiReview.isVisible = ready
        draftAiReturn.isVisible = error || ready
    }

    companion object {
        const val EXTRA_SCOPE = "chatbot_draft_ai_scope"
        const val EXTRA_REQUEST = "chatbot_draft_ai_request"
        private const val EXTRA_FAILURE = "chatbot_draft_ai_failure"
        private const val EXTRA_APPLY_FAILURE = "chatbot_draft_ai_apply_failure"
        private const val EXTRA_SOURCE_DRAFT_ID = "chatbot_draft_ai_source_draft_id"
        private const val EXTRA_CONTRACT_ID = "chatbot_draft_ai_contract_id"
        private const val EXTRA_CONVERSATION_ID = "chatbot_draft_ai_conversation_id"
        private const val RESULT_EDIT_REQUEST = 614
        fun createIntent(context: Context, scope: String, request: String, previewFailure: Boolean = false, previewApplyFailure: Boolean = false, sourceDraftId: String? = null, contractId: String? = null, conversationId: String? = null) =
            Intent(context, ChatbotDraftAiEditingActivity::class.java)
                .putExtra(EXTRA_SCOPE, scope)
                .putExtra(EXTRA_REQUEST, request)
                .putExtra(EXTRA_FAILURE, previewFailure)
                .putExtra(EXTRA_APPLY_FAILURE, previewApplyFailure)
                .putExtra(EXTRA_SOURCE_DRAFT_ID, sourceDraftId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
                .putExtra(EXTRA_CONVERSATION_ID, conversationId)
            .also { DraftSessionGuard.stamp(context, it) }
    }
}
