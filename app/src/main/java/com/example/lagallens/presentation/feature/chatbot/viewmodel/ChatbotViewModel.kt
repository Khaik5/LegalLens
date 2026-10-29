package com.example.lagallens.presentation.feature.chatbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
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
import com.example.lagallens.presentation.feature.chatbot.contract.DraftGenerationStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException

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
    private var activeContractId: String? = null
    private var nextMessageId = INITIAL_MESSAGE_ID
    private var hasOpened = false
    private val failedQuestions = mutableMapOf<Long, String>()
    private var newChatSuggestions: List<String> = emptyList()
    private var draftTransition: Job? = null
    private var draftGeneration: Job? = null
    private var previewDraftError = false

    fun onEvent(event: ChatbotUiEvent) {
        when (event) {
            is ChatbotUiEvent.ScreenOpened -> openScreen(event)
            is ChatbotUiEvent.MessageChanged -> _uiState.update { it.copy(messageDraft = event.message) }
            ChatbotUiEvent.SendClicked -> sendMessage()
            is ChatbotUiEvent.NewDraftClicked -> startDraftConversation(event.request, event.clarification)
            is ChatbotUiEvent.DraftDetailsSubmitted -> sendDraftClarification(event.message, event.acknowledgement)
            ChatbotUiEvent.ReviewDraftClicked -> _uiState.update { it.copy(isDraftReviewVisible = true) }
            ChatbotUiEvent.CreateDraftClicked -> if (_uiState.value.isDraftReviewVisible) createDraft()
            ChatbotUiEvent.RetryDraftCreationClicked -> if (_uiState.value.draftGenerationStatus == DraftGenerationStatus.ERROR) createDraft()
            ChatbotUiEvent.ViewDraftClicked -> if (_uiState.value.draftGenerationStatus == DraftGenerationStatus.SUCCESS) emitEffect(ChatbotUiEffect.ShowDraftPreview)
            ChatbotUiEvent.EditDraftClicked -> _uiState.update {
                it.copy(isDraftReviewVisible = false, isDraftReviewReady = false, draftGenerationStatus = DraftGenerationStatus.NONE)
            }
            is ChatbotUiEvent.SuggestionClicked -> sendMessage(event.suggestion)
            ChatbotUiEvent.CitationClicked -> emitEffect(ChatbotUiEffect.NavigateToLegalReference)
            is ChatbotUiEvent.ErrorRetryClicked -> retryMessage(event.messageId)
            ChatbotUiEvent.AttachmentClicked -> emitEffect(ChatbotUiEffect.OpenAttachmentSheet)
            ChatbotUiEvent.AttachmentCameraClicked -> emitEffect(ChatbotUiEffect.OpenCamera)
            ChatbotUiEvent.AttachmentSavedClicked -> emitEffect(ChatbotUiEffect.ShowMessage(R.string.chatbot_attachment_saved_unavailable))
            is ChatbotUiEvent.AttachmentSelected -> _uiState.update { it.copy(attachmentUri = event.uri, attachmentName = event.name) }
            ChatbotUiEvent.AttachmentRemoved -> _uiState.update { it.copy(attachmentUri = null, attachmentName = null) }
            ChatbotUiEvent.DrawerOpened -> _uiState.update { it.copy(isDrawerVisible = true) }
            ChatbotUiEvent.DrawerDismissed -> _uiState.update { it.copy(isDrawerVisible = false) }
            ChatbotUiEvent.NewChatClicked -> resetConversation()
            ChatbotUiEvent.HistoryClicked -> openHistory()
            ChatbotUiEvent.HelpClicked -> {
                _uiState.update { it.copy(isDrawerVisible = false) }
                emitEffect(ChatbotUiEffect.NavigateToHelp)
            }
        }
    }

    private fun openScreen(event: ChatbotUiEvent.ScreenOpened) {
        if (hasOpened) return
        hasOpened = true
        previewDraftError = event.previewDraftError
        viewModelScope.launch {
            val conversation = try {
                openConversation(event.contractName, event.contractType, event.contractId, event.conversationId)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(contractName = event.contractName, contractType = event.contractType, isConversationReady = false, isReplyPending = false, isNewConversation = true)
                }
                emitEffect(ChatbotUiEffect.ShowMessage(R.string.auth_backend_not_configured))
                return@launch
            }
            conversationId = conversation.id
            activeContractId = event.contractId
            newChatSuggestions = conversation.newConversationSuggestions
            val storedMessages = if (event.conversationId != null) openConversation.messages(conversation.id) else emptyList()
            val restoredMessages = storedMessages.mapNotNull { stored ->
                stored.messageId.toLongOrNull()?.let { id ->
                    ChatbotMessage(id, if (stored.isUser) ChatbotMessageAuthor.USER else ChatbotMessageAuthor.ASSISTANT, stored.content)
                }
            }
            nextMessageId = (restoredMessages.maxOfOrNull(ChatbotMessage::id) ?: 0L) + 1L
            _uiState.update {
                it.copy(
                    contractName = event.contractName,
                    contractType = event.contractType,
                    messages = restoredMessages.ifEmpty { initialMessages(conversation.greeting) },
                    isConversationReady = true,
                    isNewConversation = event.startNewConversation,
                    isDraftConversation = false,
                    isDraftReviewReady = false,
                    isDraftReviewVisible = false,
                    suggestions = if (event.startNewConversation) newChatSuggestions else conversation.initialSuggestions
                )
            }
            if (!event.draftRequest.isNullOrBlank() && !event.draftClarification.isNullOrBlank()) {
                startDraftConversation(event.draftRequest, event.draftClarification)
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
        persistTranscript()
        viewModelScope.launch {
            try {
                appendReply(message, sendMessage(activeConversationId, message))
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                appendReply(message, ChatbotReply.RetryableError("Không thể gửi tin nhắn. Vui lòng thử lại.", "Thử lại"))
            }
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
        persistTranscript()
    }

    private fun resetConversation() {
        draftTransition?.cancel()
        draftGeneration?.cancel()
        failedQuestions.clear()
        val previous = _uiState.value
        val contractName = previous.contractName
        val contractType = previous.contractType
        val contractId = activeContractId
        conversationId = null
        _uiState.update {
            it.copy(
                messageDraft = "",
                messages = emptyList(),
                suggestions = newChatSuggestions,
                isReplyPending = false,
                isNewConversation = true,
                isDraftConversation = false,
                isDraftPreparing = false,
                isDraftReviewReady = false,
                isDraftReviewVisible = false,
                draftGenerationStatus = DraftGenerationStatus.NONE,
                isDrawerVisible = false,
                isConversationReady = false
            )
        }
        viewModelScope.launch {
            runCatching { openConversation(contractName, contractType, contractId) }
                .onSuccess { conversation ->
                    conversationId = conversation.id
                    newChatSuggestions = conversation.newConversationSuggestions
                    _uiState.update { it.copy(isConversationReady = true, suggestions = conversation.newConversationSuggestions) }
                }
                .onFailure { emitEffect(ChatbotUiEffect.ShowMessage(R.string.auth_backend_not_configured)) }
        }
    }

    private fun startDraftConversation(requestText: String, clarificationText: String) {
        val previous = _uiState.value
        if (!previous.isConversationReady || previous.isReplyPending) return
        val request = ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.USER,
            content = requestText
        )
        val clarification = ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = clarificationText
        )
        if (!previous.isNewConversation && previous.messages.size > 1 && !previous.isDraftConversation) {
            val action = ChatbotMessage(nextMessageId++, ChatbotMessageAuthor.ASSISTANT, "", isDraftAction = true)
            val loading = ChatbotMessage(nextMessageId++, ChatbotMessageAuthor.ASSISTANT, "", isDraftLoading = true)
            _uiState.update {
                it.copy(
                    messageDraft = "",
                    messages = it.messages + action + request + loading,
                    suggestions = emptyList(),
                    isNewConversation = false,
                    isDraftConversation = true,
                    isDraftPreparing = true,
                    isDraftReviewReady = false,
                    isDraftReviewVisible = false,
                    isReplyPending = true,
                    isDrawerVisible = false
                )
            }
            draftTransition = viewModelScope.launch {
                delay(DRAFT_TRANSITION_DELAY_MS)
                showDraftClarification(request, clarification)
            }
            return
        }
        showDraftClarification(request, clarification)
    }

    private fun showDraftClarification(request: ChatbotMessage, clarification: ChatbotMessage) {
        _uiState.update {
            it.copy(
                messageDraft = "",
                messages = listOf(request, clarification),
                suggestions = emptyList(),
                isNewConversation = false,
                isDraftConversation = true,
                isDraftPreparing = false,
                isDraftReviewReady = false,
                isDraftReviewVisible = false,
                isReplyPending = false,
                isDrawerVisible = false
            )
        }
        persistTranscript()
    }

    private fun sendDraftClarification(message: String, acknowledgement: String) {
        if (message.isBlank() || _uiState.value.isReplyPending || _uiState.value.draftGenerationStatus != DraftGenerationStatus.NONE) return
        val userMessage = ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.USER,
            content = message
        )
        val assistantMessage = ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.ASSISTANT,
            content = acknowledgement,
            showDraftNotice = true
        )
        _uiState.update { state ->
            state.copy(
                messageDraft = "",
                messages = state.messages + userMessage + assistantMessage,
                suggestions = emptyList(),
                isDraftReviewReady = true,
                isDraftReviewVisible = false,
                isReplyPending = false
            )
        }
        persistTranscript()
    }

    private fun createDraft() {
        if (_uiState.value.draftGenerationStatus == DraftGenerationStatus.LOADING) return
        draftGeneration?.cancel()
        _uiState.update { it.copy(isDraftReviewVisible = false, draftGenerationStatus = DraftGenerationStatus.LOADING, isReplyPending = true) }
        draftGeneration = viewModelScope.launch {
            delay(DRAFT_GENERATION_DELAY_MS)
            val result = if (previewDraftError) DraftGenerationStatus.ERROR else DraftGenerationStatus.SUCCESS
            previewDraftError = false
            _uiState.update { it.copy(draftGenerationStatus = result, isReplyPending = false) }
        }
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
            val retriedMessage = try {
                retryMessage(activeConversationId, question).toMessage(messageId)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                _uiState.update { it.copy(isReplyPending = false) }
                emitEffect(ChatbotUiEffect.ShowMessage(R.string.auth_backend_not_configured))
                return@launch
            }
            failedQuestions.remove(messageId)
            _uiState.update { state ->
                val errorIndex = state.messages.indexOfFirst { message -> message.id == messageId && message.error != null }
                if (errorIndex == -1) return@update state
                state.copy(
                    messages = state.messages.toMutableList().also { it[errorIndex] = retriedMessage },
                    isReplyPending = false
                )
            }
            persistTranscript()
        }
    }

    private fun persistTranscript() {
        val id = conversationId ?: return
        val state = _uiState.value
        val stored = state.messages.filter { it.content.isNotBlank() && !it.isDraftLoading && !it.isDraftAction }
            .map { message -> ChatbotTranscriptMessage(message.id.toString(), message.author == ChatbotMessageAuthor.USER, message.content, System.currentTimeMillis()) }
        viewModelScope.launch {
            runCatching { openConversation.save(id, activeContractId, state.contractName, state.contractType, stored) }
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
        const val DRAFT_TRANSITION_DELAY_MS = 1500L
        const val DRAFT_GENERATION_DELAY_MS = 1800L
    }
}
