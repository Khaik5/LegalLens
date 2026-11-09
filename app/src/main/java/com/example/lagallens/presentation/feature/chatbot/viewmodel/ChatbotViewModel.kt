package com.example.lagallens.presentation.feature.chatbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.common.utils.toFailureMessage
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
import com.example.lagallens.domain.model.chatbot.ChatbotAttachment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart

class ChatbotViewModel(
    private val openConversation: OpenChatbotConversationUseCase,
    private val sendMessage: SendChatbotMessageUseCase,
    private val retryMessage: RetryChatbotMessageUseCase,
    private val draftAi: () -> com.example.lagallens.domain.repository.DraftAiRepository
) : ViewModel() {
    private val ownerSession = openConversation.currentSessionKey()
    private val sessionJobs = mutableSetOf<Job>()
    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ChatbotUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var conversationId: String? = null
    private var activeContractId: String? = null
    private var nextMessageId = INITIAL_MESSAGE_ID
    private var hasOpened = false
    private val failedQuestions = mutableMapOf<Long, String>()
    private val failedAttachments = mutableMapOf<Long, ChatbotAttachment>()
    private var newChatSuggestions: List<String> = emptyList()
    private var draftTransition: Job? = null
    private var draftGeneration: Job? = null
    private var previewDraftError = false
    private var generatedDraftId: String? = null

    init {
        viewModelScope.launch {
            openConversation.observeSessionKeys().collect { session ->
                if (session != ownerSession) invalidateSession()
            }
        }
    }

    private fun invalidateSession() {
        synchronized(sessionJobs) { sessionJobs.toList() }.forEach { it.cancel() }
        draftTransition?.cancel()
        draftGeneration?.cancel()
        conversationId = null
        activeContractId = null
        failedQuestions.clear()
        newChatSuggestions = emptyList()
        _uiState.value = ChatbotUiState()
    }

    private fun launchForSession(block: suspend CoroutineScope.() -> Unit): Job {
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            if (openConversation.currentSessionKey() == ownerSession) block()
        }
        synchronized(sessionJobs) { sessionJobs.add(job) }
        job.invokeOnCompletion { synchronized(sessionJobs) { sessionJobs.remove(job) } }
        job.start()
        return job
    }

    fun onEvent(event: ChatbotUiEvent) {
        if (openConversation.currentSessionKey() != ownerSession) { invalidateSession(); return }
        when (event) {
            is ChatbotUiEvent.ScreenOpened -> openScreen(event)
            is ChatbotUiEvent.MessageChanged -> _uiState.update { it.copy(messageDraft = event.message) }
            ChatbotUiEvent.SendClicked -> sendMessage()
            is ChatbotUiEvent.NewDraftClicked -> startDraftConversation(event.request, event.clarification)
            is ChatbotUiEvent.DraftDetailsSubmitted -> sendDraftClarification(event.message, event.acknowledgement)
            ChatbotUiEvent.ReviewDraftClicked -> _uiState.update { it.copy(isDraftReviewVisible = true) }
            ChatbotUiEvent.CreateDraftClicked -> if (_uiState.value.isDraftReviewVisible) createDraft()
            ChatbotUiEvent.RetryDraftCreationClicked -> if (_uiState.value.draftGenerationStatus == DraftGenerationStatus.ERROR) createDraft()
            ChatbotUiEvent.ViewDraftClicked -> if (_uiState.value.draftGenerationStatus == DraftGenerationStatus.SUCCESS) emitEffect(ChatbotUiEffect.ShowDraftPreview(generatedDraftId))
            ChatbotUiEvent.EditDraftClicked -> _uiState.update {
                it.copy(isDraftReviewVisible = false, isDraftReviewReady = false, draftGenerationStatus = DraftGenerationStatus.NONE)
            }
            is ChatbotUiEvent.SuggestionClicked -> sendMessage(event.suggestion)
            is ChatbotUiEvent.CitationClicked -> emitEffect(
                event.referenceId?.let(ChatbotUiEffect::NavigateToLegalReference) ?: ChatbotUiEffect.ShowMessage(R.string.analysis_citation_unavailable)
            )
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
        launchForSession {
            val conversation = try {
                openConversation.openForSession(ownerSession, event.contractName, event.contractType, event.contractId, event.conversationId)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
                _uiState.update {
                    it.copy(contractName = event.contractName, contractType = event.contractType, isConversationReady = false, isReplyPending = false, isNewConversation = true)
                }
                emitEffect(ChatbotUiEffect.ShowMessage(failure.toFailureMessage()))
                return@launchForSession
            }
            if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
            conversationId = conversation.id
            activeContractId = event.contractId
            newChatSuggestions = conversation.newConversationSuggestions
            val storedMessages = try {
                if (event.conversationId != null) openConversation.messagesForSession(ownerSession, conversation.id) else emptyList()
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
                _uiState.update { it.copy(contractName = event.contractName, contractType = event.contractType, isConversationReady = false, isReplyPending = false) }
                emitEffect(ChatbotUiEffect.ShowMessage(R.string.chatbot_history_load_failed))
                return@launchForSession
            }
            if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
            val restoredMessages = storedMessages.mapNotNull { it.toMessage() }
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
        val attachment = _uiState.value.attachmentUri?.let { uri -> ChatbotAttachment(uri, _uiState.value.attachmentName.orEmpty()) }
        val userMessage = ChatbotMessage(
            id = nextMessageId++,
            author = ChatbotMessageAuthor.USER,
            content = if (attachment == null) message else "$message\n\n\uD83D\uDCCE ${attachment.name}"
        )
        _uiState.update { state ->
            state.copy(
                messageDraft = "",
                attachmentUri = null,
                attachmentName = null,
                suggestions = emptyList(),
                isReplyPending = true,
                isNewConversation = false,
                messages = state.messages + userMessage
            )
        }
        persistTranscript()
        launchForSession {
            try {
                val reply = sendMessage.forSession(ownerSession, activeConversationId, message, attachment)
                if (openConversation.currentSessionKey() == ownerSession) appendReply(message, reply, attachment)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
                appendReply(message, ChatbotReply.RetryableError("", ""), attachment)
                _uiState.update { state -> state.copy(messages = state.messages.map { item ->
                    if (item.id == state.messages.lastOrNull()?.id) item.copy(error = item.error?.copy(titleRes = failure.toFailureMessage())) else item
                }) }
                emitEffect(ChatbotUiEffect.ShowMessage(failure.toFailureMessage()))
            }
        }
    }

    private fun appendReply(question: String, reply: ChatbotReply, attachment: ChatbotAttachment? = null) {
        val assistantMessage = reply.toMessage(nextMessageId++)
        if (reply is ChatbotReply.RetryableError) {
            failedQuestions[assistantMessage.id] = question
            attachment?.let { failedAttachments[assistantMessage.id] = it }
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
        launchForSession {
            runCatching { openConversation.openForSession(ownerSession, contractName, contractType, contractId) }
                .onSuccess { conversation ->
                    if (openConversation.currentSessionKey() != ownerSession) return@onSuccess
                    conversationId = conversation.id
                    newChatSuggestions = conversation.newConversationSuggestions
                    _uiState.update { it.copy(isConversationReady = true, suggestions = conversation.newConversationSuggestions) }
                }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    if (openConversation.currentSessionKey() == ownerSession) emitEffect(ChatbotUiEffect.ShowMessage(failure.toFailureMessage()))
                }
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
            draftTransition = launchForSession {
                delay(DRAFT_TRANSITION_DELAY_MS)
                if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
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
                messages = it.messages.filterNot { message -> message.id == request.id || message.isDraftLoading || message.isDraftAction } + request + clarification,
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
        val request = com.example.lagallens.domain.repository.DraftGenerationRequest(
            conversationId,
            activeContractId,
            _uiState.value.messages.filter { it.author == ChatbotMessageAuthor.USER }.map { it.content }
        )
        draftGeneration = launchForSession {
            val draft = try {
                if (previewDraftError) { delay(DRAFT_ERROR_PREVIEW_DELAY_MS); null } else draftAi().generateDraft(request)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                null
            }
            if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
            previewDraftError = false
            generatedDraftId = draft?.draftId
            _uiState.update {
                it.copy(draftGenerationStatus = if (draft == null) DraftGenerationStatus.ERROR else DraftGenerationStatus.SUCCESS, isReplyPending = false)
            }
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
        launchForSession {
            val retriedMessage = try {
                retryMessage.forSession(ownerSession, activeConversationId, question, failedAttachments[messageId]).toMessage(messageId)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
                _uiState.update { it.copy(isReplyPending = false) }
                emitEffect(ChatbotUiEffect.ShowMessage(failure.toFailureMessage()))
                return@launchForSession
            }
            if (openConversation.currentSessionKey() != ownerSession) return@launchForSession
            if (retriedMessage.error == null) {
                failedQuestions.remove(messageId)
                failedAttachments.remove(messageId)
            }
            _uiState.update { state ->
                val errorIndex = state.messages.indexOfFirst { message -> message.id == messageId && message.error != null }
                if (errorIndex == -1) return@update state.copy(isReplyPending = false)
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
        val contractId = activeContractId
        // A failed reply has no text but must survive reopening, so its Retry keeps working.
        val stored = state.messages.filter { (it.content.isNotBlank() || it.error != null) && !it.isDraftLoading && !it.isDraftAction }
            .map { it.toTranscript() }
        launchForSession {
            try {
                openConversation.saveForSession(ownerSession, id, contractId, state.contractName, state.contractType, stored)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (failure: Exception) {
                emitEffect(ChatbotUiEffect.ShowMessage(R.string.chatbot_save_failed))
            }
        }
    }

    private fun ChatbotMessage.toTranscript() = ChatbotTranscriptMessage(
        messageId = id.toString(),
        isUser = author == ChatbotMessageAuthor.USER,
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
        citation = citation?.let { com.example.lagallens.domain.model.chatbot.ChatbotCitation(it.title, it.description, it.actionLabel, it.referenceId) },
        referenceSectionTitle = referenceSectionTitle,
        references = references.map { com.example.lagallens.domain.model.chatbot.ChatbotReference(it.title, it.excerpt) },
        noEvidenceTitle = noEvidence?.title,
        errorRetryLabel = error?.retryLabel,
        retryQuestion = failedQuestions[id],
        showDraftNotice = showDraftNotice
    )

    private fun ChatbotTranscriptMessage.toMessage(): ChatbotMessage? {
        val id = messageId.toLongOrNull() ?: return null
        retryQuestion?.let { failedQuestions[id] = it }
        return ChatbotMessage(
            id = id,
            author = if (isUser) ChatbotMessageAuthor.USER else ChatbotMessageAuthor.ASSISTANT,
            content = content,
            citation = citation?.let { ChatbotCitation(it.title, it.description, it.actionLabel, it.referenceId) },
            referenceSectionTitle = referenceSectionTitle,
            references = references.map { ChatbotReference(it.title, it.excerpt) },
            noEvidence = noEvidenceTitle?.let { ChatbotNoEvidence(it, content) },
            error = errorRetryLabel?.let { ChatbotError(content, it) },
            showDraftNotice = showDraftNotice,
            createdAtEpochMillis = createdAtEpochMillis
        )
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
            citation = ChatbotCitation(citation.title, citation.description, citation.actionLabel, citation.referenceId)
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
        launchForSession { _uiEffect.send(effect) }
    }

    class Factory(
        private val openConversation: OpenChatbotConversationUseCase,
        private val sendMessage: SendChatbotMessageUseCase,
        private val retryMessage: RetryChatbotMessageUseCase,
        private val draftAi: () -> com.example.lagallens.domain.repository.DraftAiRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatbotViewModel::class.java))
            return ChatbotViewModel(openConversation, sendMessage, retryMessage, draftAi) as T
        }
    }

    private companion object {
        const val INITIAL_MESSAGE_ID = 1L
        const val DRAFT_TRANSITION_DELAY_MS = 1500L
        const val DRAFT_ERROR_PREVIEW_DELAY_MS = 1800L
    }
}
