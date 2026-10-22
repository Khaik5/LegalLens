package com.example.lagallens.presentation.feature.chatbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.usecase.OpenChatbotConversationUseCase
import com.example.lagallens.domain.usecase.RetryChatbotMessageUseCase
import com.example.lagallens.domain.usecase.SendChatbotMessageUseCase
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

class ChatbotViewModel(
    private val openConversation: OpenChatbotConversationUseCase,
    private val sendMessage: SendChatbotMessageUseCase,
    private val retryMessage: RetryChatbotMessageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChatbotUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var conversationId: String? = null
    private var nextMessageId = INITIAL_MESSAGE_ID
    private var hasOpened = false
    private val failedQuestions = mutableMapOf<Long, String>()
    private var newChatSuggestions: List<String> = emptyList()

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
        viewModelScope.launch {
            val conversation = openConversation(event.contractName, event.contractType)
            conversationId = conversation.id
            newChatSuggestions = conversation.newConversationSuggestions
            _uiState.update {
                it.copy(
                    contractName = event.contractName,
                    contractType = event.contractType,
                    messages = initialMessages(conversation.greeting),
                    suggestions = conversation.initialSuggestions,
                    isConversationReady = true
                )
            }
        }
    }

    private fun sendMessage(message: String = _uiState.value.messageDraft.trim()) {
        if (message.isBlank()) return
        val activeConversationId = conversationId ?: return
        if (_uiState.value.isReplyPending) return
        val userMessage = ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.USER,
            content = message
        )
        _uiState.update { state ->
            state.copy(
                messageDraft = "",
                suggestions = emptyList(),
                isReplyPending = true,
                isNewConversation = false,
                messages = state.messages + userMessage
            )
        }
        viewModelScope.launch {
            appendReply(message, sendMessage(activeConversationId, message))
        }
    }

    private fun appendReply(question: String, reply: ChatbotReply) {
        val assistantMessage = reply.toMessage(nextMessageId++)
        if (reply is ChatbotReply.RetryableError) {
            failedQuestions[assistantMessage.id] = question
        }
        _uiState.update { state ->
            state.copy(
                messages = state.messages + assistantMessage,
                isReplyPending = false
            )
        }
    }

    private fun resetConversation() {
        failedQuestions.clear()
        _uiState.update {
            it.copy(
                messageDraft = "",
                messages = emptyList(),
                suggestions = newChatSuggestions,
                isReplyPending = false,
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
        val activeConversationId = conversationId ?: return
        val question = failedQuestions[messageId] ?: return
        if (_uiState.value.isReplyPending) return
        _uiState.update { it.copy(isReplyPending = true) }
        viewModelScope.launch {
            val retriedMessage = retryMessage(activeConversationId, question).toMessage(messageId)
            failedQuestions.remove(messageId)
            _uiState.update { state ->
                val errorIndex = state.messages.indexOfFirst { message -> message.id == messageId && message.error != null }
                if (errorIndex == -1) return@update state
                state.copy(
                    messages = state.messages.toMutableList().also { it[errorIndex] = retriedMessage },
                    isReplyPending = false
                )
            }
        }
    }

    private fun ChatbotReply.toMessage(id: Long): ChatbotMessage = when (this) {
        is ChatbotReply.Answer -> ChatbotMessage(
            id = id,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = content
        )

        is ChatbotReply.CitationAnswer -> ChatbotMessage(
            id = id,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = content,
            citation = ChatbotCitation(citation.title, citation.description, citation.actionLabel)
        )

        is ChatbotReply.GroundedAnswer -> ChatbotMessage(
            id = id,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = content,
            referenceSectionTitle = referenceSectionTitle,
            references = references.map { ChatbotReference(it.title, it.excerpt) }
        )

        is ChatbotReply.NoEvidenceAnswer -> ChatbotMessage(
            id = id,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = content,
            noEvidence = ChatbotNoEvidence(title, content)
        )

        is ChatbotReply.RetryableError -> ChatbotMessage(
            id = id,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = title,
            error = ChatbotError(title, retryLabel)
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

    class Factory(
        private val openConversation: OpenChatbotConversationUseCase,
        private val sendMessage: SendChatbotMessageUseCase,
        private val retryMessage: RetryChatbotMessageUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatbotViewModel::class.java))
            return ChatbotViewModel(openConversation, sendMessage, retryMessage) as T
        }
    }

    private companion object {
        const val INITIAL_MESSAGE_ID = 1L
    }
}
