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
import com.example.lagallens.BuildConfig
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotDraftRegenerationBinding
import com.example.lagallens.presentation.common.utils.applySystemBarInsets
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationUiState
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import com.example.lagallens.presentation.feature.chatbot.draft.viewmodel.ChatbotDraftRegenerationViewModel
import kotlinx.coroutines.launch

class ChatbotDraftRegenerationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotDraftRegenerationBinding
    private val viewModel by viewModels<ChatbotDraftRegenerationViewModel>()
    private var openingDraft = false
    private val sourceDraftId by lazy { intent.getStringExtra(EXTRA_SOURCE_DRAFT_ID) }
    private val contractId by lazy { intent.getStringExtra(EXTRA_CONTRACT_ID) }
    private val sourceVersion by lazy {
        intent.getStringExtra(EXTRA_SOURCE_VERSION) ?: ChatbotDraftUiState.DRAFT_VERSION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityChatbotDraftRegenerationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        bindActions()
        viewModel.onEvent(
            ChatbotDraftRegenerationUiEvent.Opened(
                previewFailure = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_FAILURE, false),
                sourceVersion = sourceVersion,
                sourceDraftId = sourceDraftId,
                contractId = contractId
            )
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun bindActions() = with(binding) {
        draftRegenerationBack.setOnClickListener { finish() }
        draftRegenerationRetry.setOnClickListener { viewModel.onEvent(ChatbotDraftRegenerationUiEvent.RetryClicked) }
        draftRegenerationReturn.setOnClickListener { finish() }
        draftRegenerationView.setOnClickListener {
            if (openingDraft) return@setOnClickListener
            openingDraft = true
            val draftId = viewModel.uiState.value.draftId
            if (draftId == null) {
                openingDraft = false
                return@setOnClickListener
            }
            startActivity(ChatbotDraftPreviewActivity.createIntent(this@ChatbotDraftRegenerationActivity, ChatbotDraftUiState.REGENERATED_VERSION, draftId = draftId))
            finish()
        }
    }

    private fun render(state: ChatbotDraftRegenerationUiState) = with(binding) {
        val isLoading = state.status == ChatbotDraftRegenerationStatus.LOADING
        val isError = state.status == ChatbotDraftRegenerationStatus.ERROR
        val isSuccess = state.status == ChatbotDraftRegenerationStatus.SUCCESS
        draftRegenerationTitle.text = getString(R.string.chatbot_draft_regeneration_title)
        draftRegenerationSourceName.text = getString(R.string.chatbot_draft_document_name, sourceVersion)
        draftRegenerationSourceMetadata.text = getString(R.string.chatbot_draft_regeneration_source_metadata)
        draftRegenerationSourceBadge.text = getString(R.string.chatbot_draft_preserved_badge, sourceVersion)
        draftRegenerationLoading.isVisible = isLoading
        draftRegenerationError.isVisible = isError
        draftRegenerationSuccess.isVisible = isSuccess
        draftRegenerationRetry.isVisible = isError
        draftRegenerationView.isVisible = isSuccess
        draftRegenerationPendingAction.isVisible = isLoading
    }

    companion object {
        private const val EXTRA_SOURCE_VERSION = "chatbot_draft_source_version"
        private const val EXTRA_SOURCE_DRAFT_ID = "chatbot_draft_source_draft_id"
        private const val EXTRA_CONTRACT_ID = "chatbot_draft_contract_id"
        const val EXTRA_PREVIEW_FAILURE = "chatbot_draft_preview_failure"

        fun createIntent(context: Context, sourceVersion: String = ChatbotDraftUiState.DRAFT_VERSION, previewFailure: Boolean = false, sourceDraftId: String? = null, contractId: String? = null): Intent =
            Intent(context, ChatbotDraftRegenerationActivity::class.java)
                .putExtra(EXTRA_SOURCE_VERSION, sourceVersion)
                .putExtra(EXTRA_PREVIEW_FAILURE, previewFailure)
                .putExtra(EXTRA_SOURCE_DRAFT_ID, sourceDraftId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
    }
}
