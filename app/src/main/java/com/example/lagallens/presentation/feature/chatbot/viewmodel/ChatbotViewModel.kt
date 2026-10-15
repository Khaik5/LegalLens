package com.example.lagallens.presentation.feature.chatbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotCitation
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotError
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessage
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotNoEvidence
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotReference
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEffect
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChatbotUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var nextMessageId = INITIAL_MESSAGE_ID
    private var localResponse = ""
    private var citationQuestion = ""
    private var citationResponse = ""
    private var citation: ChatbotCitation? = null
    private var groundedQuestion = ""
    private var groundedResponse = ""
    private var groundedReferenceSectionTitle = ""
    private var groundedReferences: List<ChatbotReference> = emptyList()
    private var noEvidenceQuestion = ""
    private var noEvidence: ChatbotNoEvidence? = null
    private var errorQuestion = ""
    private var error: ChatbotError? = null
    private var newChatSuggestions: List<String> = emptyList()
    private var hasOpened = false

    fun onEvent(event: ChatbotUiEvent) {
        when (event) {
            is ChatbotUiEvent.ScreenOpened -> openScreen(event)
            is ChatbotUiEvent.MessageChanged -> _uiState.update { it.copy(messageDraft = event.message) }
            ChatbotUiEvent.SendClicked -> sendMessage()
            is ChatbotUiEvent.SuggestionClicked -> sendMessage(event.suggestion)
            ChatbotUiEvent.CitationClicked -> emitEffect(ChatbotUiEffect.NavigateToLegalReference)
            is ChatbotUiEvent.ErrorRetryClicked -> retryMessage(event.messageId)
            ChatbotUiEvent.AttachmentClicked -> emitEffect(ChatbotUiEffect.ShowMessage(R.string.chatbot_attachment_unavailable))
            ChatbotUiEvent.DrawerOpened -> _uiState.update { it.copy(isDrawerVisible = true) }
            ChatbotUiEvent.DrawerDismissed -> _uiState.update { it.copy(isDrawerVisible = false) }
            ChatbotUiEvent.NewChatClicked -> resetConversation()
            ChatbotUiEvent.HistoryClicked -> openHistory()
            ChatbotUiEvent.HelpClicked -> showUnavailable(R.string.chatbot_help_unavailable)
        }
    }

    private fun openScreen(event: ChatbotUiEvent.ScreenOpened) {
        if (hasOpened) return
        hasOpened = true
        localResponse = event.localResponse
        citationQuestion = event.citationQuestion
        citationResponse = event.citationResponse
        citation = event.citation
        groundedQuestion = event.groundedQuestion
        groundedResponse = event.groundedResponse
        groundedReferenceSectionTitle = event.groundedReferenceSectionTitle
        groundedReferences = event.groundedReferences
        noEvidenceQuestion = event.noEvidenceQuestion
        noEvidence = event.noEvidence
        errorQuestion = event.errorQuestion
        error = event.error
        newChatSuggestions = event.newChatSuggestions
        _uiState.update {
            it.copy(
                contractName = event.contractName,
                contractType = event.contractType,
                messages = initialMessages(event.initialGreeting),
                suggestions = event.initialSuggestions
            )
        }
    }

    private fun sendMessage(message: String = _uiState.value.messageDraft.trim()) {
        if (message.isBlank()) return
        _uiState.update { state ->
            state.copy(
                messageDraft = "",
                suggestions = emptyList(),
                isNewConversation = false,
                messages = state.messages + ChatbotMessage(
                    id = nextMessageId++,
                    author = ChatbotMessageAuthor.USER,
                    content = message
                ) + assistantResponseFor(message)
            )
        }
    }

    private fun resetConversation() {
        _uiState.update {
            it.copy(
                messageDraft = "",
                messages = emptyList(),
                suggestions = newChatSuggestions,
                isNewConversation = true,
                isDrawerVisible = false
            )
        }
    }

    private fun showUnavailable(messageRes: Int) {
        _uiState.update { it.copy(isDrawerVisible = false) }
        emitEffect(ChatbotUiEffect.ShowMessage(messageRes))
    }

    private fun openHistory() {
        _uiState.update { it.copy(isDrawerVisible = false) }
        emitEffect(ChatbotUiEffect.NavigateToHistory)
    }

    private fun retryMessage(messageId: Long) {
        _uiState.update { state ->
            val errorIndex = state.messages.indexOfFirst { message -> message.id == messageId && message.error != null }
            if (errorIndex == -1) return@update state
            val retriedMessage = ChatbotMessage(
                id = messageId,
                author = ChatbotMessageAuthor.ASSISTANT,
                content = localResponse
            )
            state.copy(messages = state.messages.toMutableList().also { it[errorIndex] = retriedMessage })
        }
    }

    private fun assistantResponseFor(message: String): ChatbotMessage = when (message) {
        errorQuestion -> ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = error?.title.orEmpty(),
            error = error
        )
        noEvidenceQuestion -> ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = noEvidence?.message.orEmpty(),
            noEvidence = noEvidence
        )
        groundedQuestion -> ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = groundedResponse,
            referenceSectionTitle = groundedReferenceSectionTitle,
            references = groundedReferences
        )
        citationQuestion -> ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = citationResponse,
            citation = citation
        )
        else -> ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = localResponse
        )
    }

    private fun initialMessages(greeting: String): List<ChatbotMessage> = listOf(
        ChatbotMessage(INITIAL_MESSAGE_ID, ChatbotMessageAuthor.ASSISTANT, greeting)
    ).also {
        nextMessageId = INITIAL_MESSAGE_ID + it.size
    }

    private fun emitEffect(effect: ChatbotUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        const val INITIAL_MESSAGE_ID = 1L
    }
}
