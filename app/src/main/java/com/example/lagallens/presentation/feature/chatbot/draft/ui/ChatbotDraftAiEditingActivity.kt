package com.example.lagallens.presentation.feature.chatbot.draft.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
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

class ChatbotDraftAiEditingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotDraftAiEditBinding
    private val viewModel by viewModels<ChatbotDraftAiEditViewModel>()
    private val scope by lazy { intent.getStringExtra(EXTRA_SCOPE).orEmpty() }
    private val request by lazy { intent.getStringExtra(EXTRA_REQUEST).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        binding = ActivityChatbotDraftAiEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        bindActions()
        viewModel.onEvent(ChatbotDraftAiEditUiEvent.Opened(scope, request, intent.getBooleanExtra(EXTRA_FAILURE, false)))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.uiState.collect(::render) }
        }
    }

    private fun bindActions() = with(binding) {
        draftAiBack.setOnClickListener { finish() }
        draftAiReturn.setOnClickListener { finish() }
        draftAiRetry.setOnClickListener { viewModel.onEvent(ChatbotDraftAiEditUiEvent.RetryClicked) }
        draftAiEditRequest.setOnClickListener {
            setResult(RESULT_EDIT_REQUEST, Intent().putExtra(EXTRA_SCOPE, scope).putExtra(EXTRA_REQUEST, request))
            finish()
        }
        draftAiReview.setOnClickListener {
            reviewLauncherStart()
        }
    }

    private fun reviewLauncherStart() {
        startActivity(
            ChatbotDraftAiReviewActivity.createIntent(
                this,
                BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_APPLY_FAILURE, false),
                intent.getStringExtra(EXTRA_SOURCE_DRAFT_ID),
                intent.getStringExtra(EXTRA_CONTRACT_ID)
            )
        )
    }

    private fun render(state: com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState) = with(binding) {
        val loading = state.status == ChatbotDraftAiEditStatus.LOADING
        val error = state.status == ChatbotDraftAiEditStatus.ERROR
        val ready = state.status == ChatbotDraftAiEditStatus.READY
        draftAiSourceName.text = getString(R.string.chatbot_draft_document_name, ChatbotDraftUiState.DRAFT_VERSION)
        draftAiRequestCard.text = state.request
        draftAiScope.text = getString(if (state.scope == com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState.WHOLE_CONTRACT_SCOPE) R.string.chatbot_draft_ai_scope_whole else R.string.chatbot_draft_ai_scope_selected)
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
        private const val RESULT_EDIT_REQUEST = 614
        fun createIntent(context: Context, scope: String, request: String, previewFailure: Boolean = false, previewApplyFailure: Boolean = false, sourceDraftId: String? = null, contractId: String? = null) =
            Intent(context, ChatbotDraftAiEditingActivity::class.java)
                .putExtra(EXTRA_SCOPE, scope)
                .putExtra(EXTRA_REQUEST, request)
                .putExtra(EXTRA_FAILURE, previewFailure)
                .putExtra(EXTRA_APPLY_FAILURE, previewApplyFailure)
                .putExtra(EXTRA_SOURCE_DRAFT_ID, sourceDraftId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
    }
}
