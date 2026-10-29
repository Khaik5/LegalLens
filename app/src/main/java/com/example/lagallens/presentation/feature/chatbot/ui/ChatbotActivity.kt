package com.example.lagallens.presentation.feature.chatbot.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.BuildConfig
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotBinding
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.ui.AnalysisLegalReferenceDetailActivity
import com.example.lagallens.presentation.feature.chatbot.adapter.ChatbotMessageAdapter
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEffect
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiState
import com.example.lagallens.presentation.feature.chatbot.contract.DraftGenerationStatus
import com.example.lagallens.presentation.feature.chatbot.history.ui.ChatbotHistoryActivity
import com.example.lagallens.presentation.feature.chatbot.help.ui.ChatbotHelpActivity
import com.example.lagallens.presentation.feature.chatbot.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.chatbot.draft.ui.ChatbotDraftPreviewActivity

class ChatbotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotBinding
    private var keyboardVisible = false
    private val helpLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val question = result.data?.getStringExtra(ChatbotHelpActivity.EXTRA_SELECTED_QUESTION)
        if (result.resultCode == RESULT_OK && !question.isNullOrBlank()) viewModel.onEvent(ChatbotUiEvent.MessageChanged(question))
    }
    private val attachmentPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = runCatching {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull() ?: uri.lastPathSegment.orEmpty()
            viewModel.onEvent(ChatbotUiEvent.AttachmentSelected(uri.toString(), name))
        }
    }
    private val viewModel by viewModels<ChatbotViewModel> {
        val app = application as LegalLensApplication
        ChatbotViewModel.Factory(
            openConversation = app.openChatbotConversationUseCase,
            sendMessage = app.sendChatbotMessageUseCase,
            retryMessage = app.retryChatbotMessageUseCase
        )
    }
    private val messageAdapter = ChatbotMessageAdapter(
        onCitationClicked = { viewModel.onEvent(ChatbotUiEvent.CitationClicked) },
        onRetryClicked = { messageId -> viewModel.onEvent(ChatbotUiEvent.ErrorRetryClicked(messageId)) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.chatbotRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val state = viewModel.uiState.value
            binding.chatbotSuggestions.isVisible = !keyboardVisible && !state.isNewConversation && !state.isDraftReviewVisible && state.draftGenerationStatus == DraftGenerationStatus.NONE && (state.suggestions.isNotEmpty() || state.isDraftReviewReady || (!state.isDraftConversation && state.messages.size > 1))
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.chatbotRoot)
        bindViews()
        onBackPressedDispatcher.addCallback(this, chatbotBackCallback)
        observeViewModel()
        viewModel.onEvent(
            ChatbotUiEvent.ScreenOpened(
                contractName = intent.getStringExtra(EXTRA_CONTRACT_NAME)
                    .orEmpty()
                    .ifBlank { getString(R.string.chatbot_default_contract_name) },
                contractType = intent.getStringExtra(EXTRA_CONTRACT_TYPE)
                    .orEmpty()
                    .ifBlank { getString(R.string.chatbot_default_contract_type) },
                contractId = intent.getStringExtra(EXTRA_CONTRACT_ID),
                conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID),
                historyPreview = intent.getStringExtra(EXTRA_HISTORY_PREVIEW),
                startNewConversation = intent.getBooleanExtra(EXTRA_START_NEW_CONVERSATION, false),
                draftRequest = if (intent.getBooleanExtra(EXTRA_CONTINUE_DRAFT, false)) getString(R.string.chatbot_draft_request) else null,
                draftClarification = if (intent.getBooleanExtra(EXTRA_CONTINUE_DRAFT, false)) getString(R.string.chatbot_draft_clarification) else null,
                previewDraftError = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_DRAFT_ERROR, false)
            )
        )
    }

    private fun bindViews() = with(binding) {
        listOf(chatbotBack, chatbotMore, chatbotAttach, chatbotSend, chatbotAttachmentChip,
            chatbotDrawerNewChat, chatbotDrawerHistory, chatbotDrawerHelp).forEach { it.bindPressFeedback() }
        chatbotMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = messageAdapter
        }
        chatbotBack.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.DrawerOpened) }
        chatbotMore.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.NewChatClicked) }
        chatbotDrawerScrim.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.DrawerDismissed) }
        chatbotDrawerActiveChat.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.DrawerDismissed) }
        chatbotDrawerNewChat.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.NewChatClicked) }
        chatbotDrawerHistory.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.HistoryClicked) }
        chatbotDrawerHelp.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.HelpClicked) }
        chatbotAttach.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.AttachmentClicked) }
        chatbotAttachmentChip.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.AttachmentRemoved) }
        chatbotSend.setOnClickListener { sendCurrentMessage() }
        chatbotDraftReviewAction.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.ReviewDraftClicked) }
        chatbotNewDraftFromDocument.setOnClickListener {
            viewModel.onEvent(
                ChatbotUiEvent.NewDraftClicked(
                    getString(R.string.chatbot_draft_request),
                    getString(R.string.chatbot_draft_clarification)
                )
            )
        }
        chatbotDraftReview.chatbotDraftCreate.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.CreateDraftClicked) }
        chatbotDraftReview.chatbotDraftEdit.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.EditDraftClicked) }
        chatbotDraftGeneration.chatbotGenerationRetry.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.RetryDraftCreationClicked) }
        chatbotDraftGeneration.chatbotGenerationEdit.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.EditDraftClicked) }
        chatbotDraftGeneration.chatbotGenerationView.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.ViewDraftClicked) }
        chatbotSuggestionContractTerm.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotSuggestionContractTerm.text.toString()))
        }
        chatbotSuggestionSalary.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotSuggestionSalary.text.toString()))
        }
        chatbotSuggestionNotableTerms.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotSuggestionNotableTerms.text.toString()))
        }
        chatbotSuggestionRightsAndObligations.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotSuggestionRightsAndObligations.text.toString()))
        }
        chatbotNewSuggestionRisk.setOnClickListener {
            viewModel.onEvent(
                ChatbotUiEvent.NewDraftClicked(
                    getString(R.string.chatbot_draft_request),
                    getString(R.string.chatbot_draft_clarification)
                )
            )
        }
        chatbotNewSuggestionPayment.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotNewSuggestionPayment.text.toString()))
        }
        chatbotNewSuggestionPenalty.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotNewSuggestionPenalty.text.toString()))
        }
        chatbotInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage()
                true
            } else {
                false
            }
        }
        chatbotInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onEvent(ChatbotUiEvent.MessageChanged(s?.toString().orEmpty()))
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun sendCurrentMessage() {
        val state = viewModel.uiState.value
        if (state.isDraftConversation) {
            viewModel.onEvent(
                ChatbotUiEvent.DraftDetailsSubmitted(
                    state.messageDraft,
                    getString(R.string.chatbot_draft_acknowledgement)
                )
            )
        } else {
            viewModel.onEvent(ChatbotUiEvent.SendClicked)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: ChatbotUiState) = with(binding) {
        val showGeneration = state.draftGenerationStatus != DraftGenerationStatus.NONE
        chatbotAttachmentChip.isVisible = state.attachmentName != null
        chatbotAttachmentChip.text = getString(R.string.chatbot_attachment_selected, state.attachmentName.orEmpty())
        chatbotAttachmentChip.contentDescription = getString(R.string.chatbot_attachment_remove, state.attachmentName.orEmpty())
        chatbotContractName.text = state.contractName
        chatbotContractType.text = if (state.isDraftConversation && !state.isDraftPreparing) getString(R.string.chatbot_draft_header_subtitle) else state.contractType
        chatbotDrawerActiveChat.text = state.contractName
        chatbotSourceDocument.chatbotSourceName.text = sourceDocumentName(state.contractName)
        chatbotDraftReview.chatbotDraftSourceName.text = sourceDocumentName(state.contractName)
        chatbotSourceDocument.root.isVisible = !showGeneration && !state.isDraftReviewVisible && (!state.isDraftConversation || !state.isDraftReviewReady)
        chatbotDraftSourceBadge.isVisible = !showGeneration && state.isDraftConversation && state.isDraftReviewReady && !state.isDraftReviewVisible
        chatbotInput.isEnabled = state.isConversationReady && !state.isReplyPending && !showGeneration
        chatbotSend.isEnabled = state.isConversationReady && !state.isReplyPending && !showGeneration
        chatbotDrawerOverlay.visibility = if (state.isDrawerVisible) android.view.View.VISIBLE else android.view.View.GONE
        chatbotEmptyState.isVisible = state.isNewConversation && !state.isDraftReviewVisible && !showGeneration
        chatbotMessages.isVisible = !state.isNewConversation && !state.isDraftReviewVisible && !showGeneration
        chatbotDraftReview.root.isVisible = state.isDraftReviewVisible
        chatbotDraftGeneration.root.isVisible = showGeneration
        chatbotDraftGeneration.chatbotGenerationLoading.isVisible = state.draftGenerationStatus == DraftGenerationStatus.LOADING
        chatbotDraftGeneration.chatbotGenerationError.isVisible = state.draftGenerationStatus == DraftGenerationStatus.ERROR
        chatbotDraftGeneration.chatbotGenerationSuccess.isVisible = state.draftGenerationStatus == DraftGenerationStatus.SUCCESS
        chatbotDraftGeneration.chatbotGenerationSource.text = getString(R.string.chatbot_generation_source, sourceDocumentName(state.contractName))
        chatbotDraftGeneration.chatbotGenerationResultDocument.chatbotSourceName.setText(R.string.chatbot_generation_result_name)
        chatbotDraftGeneration.chatbotGenerationResultDocument.chatbotSourceMetadata.setText(R.string.chatbot_generation_result_metadata)
        chatbotDraftGeneration.chatbotGenerationResultDocument.chatbotSourceBadge.setText(R.string.chatbot_generation_result_badge)
        if (chatbotInput.text.toString() != state.messageDraft) {
            chatbotInput.setText(state.messageDraft)
            chatbotInput.setSelection(state.messageDraft.length)
        }
        chatbotSuggestions.isVisible = !keyboardVisible && !state.isNewConversation && !state.isDraftReviewVisible && !showGeneration && (state.suggestions.isNotEmpty() || state.isDraftReviewReady || (!state.isDraftConversation && state.messages.size > 1))
        chatbotSuggestionContractTerm.text = state.suggestions.getOrNull(0).orEmpty()
        chatbotSuggestionSalary.text = state.suggestions.getOrNull(1).orEmpty()
        chatbotSuggestionNotableTerms.text = state.suggestions.getOrNull(2).orEmpty()
        chatbotSuggestionRightsAndObligations.text = state.suggestions.getOrNull(3).orEmpty()
        chatbotDraftReviewAction.isVisible = state.isDraftReviewReady
        val showNewDraftAction = !state.isNewConversation && !state.isDraftConversation && state.messages.size > 1 && !state.isDraftReviewReady
        chatbotSuggestionContractTerm.isVisible = !state.isDraftReviewReady && !showNewDraftAction
        chatbotSuggestionSalary.isVisible = !state.isDraftReviewReady && !showNewDraftAction
        chatbotSuggestionNotableTerms.isVisible = !state.isDraftReviewReady && !showNewDraftAction
        chatbotSuggestionRightsAndObligations.isVisible = !state.isDraftReviewReady && !showNewDraftAction
        chatbotNewDraftFromDocument.isVisible = showNewDraftAction
        chatbotNewSuggestionRisk.text = getString(R.string.chatbot_new_draft_from_document)
        chatbotNewSuggestionPayment.text = getString(R.string.chatbot_new_chat_term_question)
        chatbotNewSuggestionPenalty.text = getString(R.string.chatbot_new_chat_rights_question)
        chatbotMore.setImageResource(
            R.drawable.ic_chatbot_new_chat_add
        )
        chatbotMore.contentDescription = getString(
            R.string.chatbot_new_state_add_content_description
        )
        if (messageAdapter.currentList != state.messages) {
            messageAdapter.submitList(state.messages) {
                if (state.messages.isNotEmpty()) chatbotMessages.scrollToPosition(state.messages.lastIndex)
            }
        }
    }

    private fun handleEffect(effect: ChatbotUiEffect) {
        when (effect) {
            ChatbotUiEffect.ShowDraftPreview -> startActivity(
                ChatbotDraftPreviewActivity.createIntent(
                    this,
                    previewRegenerationError = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_PREVIEW_REGENERATION_ERROR, false),
                    contractId = intent.getStringExtra(EXTRA_CONTRACT_ID)
                )
            )
            ChatbotUiEffect.NavigateBack -> finish()
            ChatbotUiEffect.NavigateToHistory -> startActivity(ChatbotHistoryActivity.createIntent(this))
            ChatbotUiEffect.NavigateToHelp -> helpLauncher.launch(ChatbotHelpActivity.createIntent(this))
            ChatbotUiEffect.OpenAttachmentPicker -> attachmentPicker.launch(arrayOf("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*"))
            ChatbotUiEffect.OpenAttachmentSheet -> showAttachmentSheet()
            ChatbotUiEffect.OpenCamera -> startActivity(com.example.lagallens.presentation.feature.camera.permission.ui.CameraPermissionActivity.createIntent(this))
            ChatbotUiEffect.NavigateToLegalReference -> startActivity(
                AnalysisLegalReferenceDetailActivity.createIntent(
                    context = this,
                    referenceId = CHATBOT_COMMERCIAL_LAW_REFERENCE_ID,
                    documentName = binding.chatbotContractName.text.toString(),
                    documentMetadata = binding.chatbotContractType.text.toString()
                )
            )
            is ChatbotUiEffect.ShowMessage -> android.widget.Toast.makeText(this, effect.messageRes, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAttachmentSheet() {
        val dialog = BottomSheetDialog(this)
        val content = layoutInflater.inflate(R.layout.bottom_sheet_chatbot_attachment, null)
        content.findViewById<android.widget.TextView>(R.id.chatbotSourceName).text = sourceDocumentName(viewModel.uiState.value.contractName)
        content.findViewById<android.widget.TextView>(R.id.chatbotSourceMetadata).text = getString(R.string.chatbot_document_uploaded_read_only)
        content.findViewById<android.widget.TextView>(R.id.chatbotSourceBadge).text = getString(R.string.chatbot_document_original)
        content.findViewById<android.view.View>(R.id.chatbotAttachmentPdf).setOnClickListener {
            dialog.dismiss()
            attachmentPicker.launch(arrayOf("application/pdf"))
        }
        content.findViewById<android.view.View>(R.id.chatbotAttachmentCamera).setOnClickListener {
            dialog.dismiss()
            viewModel.onEvent(ChatbotUiEvent.AttachmentCameraClicked)
        }
        content.findViewById<android.view.View>(R.id.chatbotAttachmentSaved).setOnClickListener {
            dialog.dismiss()
            viewModel.onEvent(ChatbotUiEvent.AttachmentSavedClicked)
        }
        content.findViewById<android.view.View>(R.id.chatbotAttachmentCancel).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(content)
        dialog.show()
    }

    private fun sourceDocumentName(contractName: String): String = if (contractName == getString(R.string.chatbot_default_contract_name)) {
        getString(R.string.chatbot_default_source_name)
    } else {
        contractName
    }

    override fun onDestroy() {
        binding.chatbotMessages.adapter = null
        super.onDestroy()
    }

    private val chatbotBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.uiState.value.isDrawerVisible) {
                viewModel.onEvent(ChatbotUiEvent.DrawerDismissed)
                return
            }
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        private const val EXTRA_CONTRACT_NAME = "chatbot_contract_name"
        private const val EXTRA_CONTRACT_TYPE = "chatbot_contract_type"
        private const val EXTRA_CONTRACT_ID = "chatbot_contract_id"
        private const val EXTRA_CONVERSATION_ID = "chatbot_conversation_id"
        private const val EXTRA_HISTORY_PREVIEW = "chatbot_history_preview"
        private const val EXTRA_START_NEW_CONVERSATION = "chatbot_start_new_conversation"
        private const val EXTRA_CONTINUE_DRAFT = "chatbot_continue_draft"
        private const val EXTRA_PREVIEW_DRAFT_ERROR = "chatbot_preview_draft_error"
        private const val EXTRA_PREVIEW_REGENERATION_ERROR = "chatbot_preview_regeneration_error"
        private const val CHATBOT_COMMERCIAL_LAW_REFERENCE_ID = "commercial_law_2005_article_301"

        fun createIntent(context: Context, contractName: String, contractType: String, contractId: String? = null): Intent =
            Intent(context, ChatbotActivity::class.java)
                .putExtra(EXTRA_CONTRACT_NAME, contractName)
                .putExtra(EXTRA_CONTRACT_TYPE, contractType)
                .putExtra(EXTRA_CONTRACT_ID, contractId)

        fun createHistoryIntent(context: Context, contractName: String, conversationId: String, contractId: String?): Intent =
            createIntent(context, contractName, "Hợp đồng lao động", contractId).putExtra(EXTRA_CONVERSATION_ID, conversationId)

        fun createNewConversationIntent(
            context: Context,
            contractName: String,
            contractType: String,
            previewDraftError: Boolean = false,
            previewRegenerationError: Boolean = false,
            contractId: String? = null
        ): Intent =
            createIntent(context, contractName, contractType, contractId)
                .putExtra(EXTRA_START_NEW_CONVERSATION, true)
                .putExtra(EXTRA_PREVIEW_DRAFT_ERROR, previewDraftError)
                .putExtra(EXTRA_PREVIEW_REGENERATION_ERROR, previewRegenerationError)

        fun createDraftContinuationIntent(context: Context, contractName: String, contractType: String, contractId: String? = null): Intent =
            createIntent(context, contractName, contractType, contractId).putExtra(EXTRA_CONTINUE_DRAFT, true)
    }
}
