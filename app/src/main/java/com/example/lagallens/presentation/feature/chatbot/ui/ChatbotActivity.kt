package com.example.lagallens.presentation.feature.chatbot.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityChatbotBinding
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.ui.AnalysisLegalReferenceDetailActivity
import com.example.lagallens.presentation.feature.chatbot.adapter.ChatbotMessageAdapter
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEffect
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiState
import com.example.lagallens.presentation.feature.chatbot.history.ui.ChatbotHistoryActivity
import com.example.lagallens.presentation.feature.chatbot.viewmodel.ChatbotViewModel
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatbotBinding
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
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
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
        observeViewModel()
        viewModel.onEvent(
            ChatbotUiEvent.ScreenOpened(
                contractName = intent.getStringExtra(EXTRA_CONTRACT_NAME)
                    .orEmpty()
                    .ifBlank { getString(R.string.chatbot_default_contract_name) },
                contractType = intent.getStringExtra(EXTRA_CONTRACT_TYPE)
                    .orEmpty()
                    .ifBlank { getString(R.string.chatbot_default_contract_type) }
            )
        )
    }

    private fun bindViews() = with(binding) {
        chatbotMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = messageAdapter
        }
        chatbotBack.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.DrawerOpened) }
        chatbotMore.setOnClickListener {
            viewModel.onEvent(
                if (viewModel.uiState.value.isNewConversation) {
                    ChatbotUiEvent.NewChatClicked
                } else {
                    ChatbotUiEvent.AttachmentClicked
                }
            )
        }
        chatbotDrawerScrim.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.DrawerDismissed) }
        chatbotDrawerActiveChat.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.DrawerDismissed) }
        chatbotDrawerNewChat.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.NewChatClicked) }
        chatbotDrawerHistory.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.HistoryClicked) }
        chatbotDrawerHelp.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.HelpClicked) }
        chatbotAttach.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.AttachmentClicked) }
        chatbotSend.setOnClickListener { viewModel.onEvent(ChatbotUiEvent.SendClicked) }
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
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotNewSuggestionRisk.text.toString()))
        }
        chatbotNewSuggestionPayment.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotNewSuggestionPayment.text.toString()))
        }
        chatbotNewSuggestionPenalty.setOnClickListener {
            viewModel.onEvent(ChatbotUiEvent.SuggestionClicked(chatbotNewSuggestionPenalty.text.toString()))
        }
        chatbotInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                viewModel.onEvent(ChatbotUiEvent.SendClicked)
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

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: ChatbotUiState) = with(binding) {
        chatbotContractName.text = state.contractName
        chatbotContractType.text = state.contractType
        chatbotDrawerActiveChat.text = state.contractName
        chatbotInput.isEnabled = state.isConversationReady && !state.isReplyPending
        chatbotSend.isEnabled = state.isConversationReady && !state.isReplyPending
        chatbotDrawerOverlay.visibility = if (state.isDrawerVisible) android.view.View.VISIBLE else android.view.View.GONE
        chatbotEmptyState.visibility = if (state.isNewConversation) android.view.View.VISIBLE else android.view.View.GONE
        chatbotMessages.visibility = if (state.isNewConversation) android.view.View.GONE else android.view.View.VISIBLE
        if (chatbotInput.text.toString() != state.messageDraft) {
            chatbotInput.setText(state.messageDraft)
            chatbotInput.setSelection(state.messageDraft.length)
        }
        chatbotSuggestions.visibility = if (!state.isNewConversation && state.suggestions.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        chatbotSuggestionContractTerm.text = state.suggestions.getOrNull(0).orEmpty()
        chatbotSuggestionSalary.text = state.suggestions.getOrNull(1).orEmpty()
        chatbotSuggestionNotableTerms.text = state.suggestions.getOrNull(2).orEmpty()
        chatbotSuggestionRightsAndObligations.text = state.suggestions.getOrNull(3).orEmpty()
        chatbotNewSuggestionRisk.text = state.suggestions.getOrNull(0).orEmpty()
        chatbotNewSuggestionPayment.text = state.suggestions.getOrNull(1).orEmpty()
        chatbotNewSuggestionPenalty.text = state.suggestions.getOrNull(2).orEmpty()
        chatbotMore.setImageResource(
            if (state.isNewConversation) R.drawable.ic_chatbot_new_chat_add else R.drawable.ic_analysis_risk_overview_more
        )
        chatbotMore.contentDescription = getString(
            if (state.isNewConversation) {
                R.string.chatbot_new_state_add_content_description
            } else {
                R.string.chatbot_more_content_description
            }
        )
        messageAdapter.submitList(state.messages) {
            if (state.messages.isNotEmpty()) chatbotMessages.scrollToPosition(state.messages.lastIndex)
        }
    }

    private fun handleEffect(effect: ChatbotUiEffect) {
        when (effect) {
            ChatbotUiEffect.NavigateBack -> finish()
            ChatbotUiEffect.NavigateToHistory -> startActivity(ChatbotHistoryActivity.createIntent(this))
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

    override fun onDestroy() {
        binding.chatbotMessages.adapter = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (viewModel.uiState.value.isDrawerVisible) {
            viewModel.onEvent(ChatbotUiEvent.DrawerDismissed)
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val EXTRA_CONTRACT_NAME = "chatbot_contract_name"
        private const val EXTRA_CONTRACT_TYPE = "chatbot_contract_type"
        private const val CHATBOT_COMMERCIAL_LAW_REFERENCE_ID = "commercial_law_2005_article_301"

        fun createIntent(context: Context, contractName: String, contractType: String): Intent =
            Intent(context, ChatbotActivity::class.java)
                .putExtra(EXTRA_CONTRACT_NAME, contractName)
                .putExtra(EXTRA_CONTRACT_TYPE, contractType)
    }
}
