package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState
import com.example.lagallens.domain.repository.DraftEditRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChatbotDraftAiEditViewModel(
    private val ownerSession: String? = null,
    private val currentSession: () -> String? = { null },
    sessions: Flow<String?>? = null,
    private val draftAi: () -> com.example.lagallens.domain.repository.DraftAiRepository
) : ViewModel() {
    private val state = MutableStateFlow(ChatbotDraftAiEditUiState())
    val uiState = state.asStateFlow()
    private var previewFailure = false
    init { sessions?.let { source -> viewModelScope.launch { source.collect { if (it != ownerSession) {
        previewFailure = false
        state.value = ChatbotDraftAiEditUiState()
    } } } } }

    fun onEvent(event: ChatbotDraftAiEditUiEvent) {
        if (ownerSession != currentSession()) return
        when (event) {
            is ChatbotDraftAiEditUiEvent.Opened -> if (!state.value.isInitialized) {
                previewFailure = event.previewFailure
                state.update {
                    it.copy(
                        scope = event.scope, request = event.request, draftId = event.draftId, contractId = event.contractId,
                        conversationId = event.conversationId, status = ChatbotDraftAiEditStatus.LOADING, isInitialized = true
                    )
                }
                generate()
            }
            ChatbotDraftAiEditUiEvent.RetryClicked -> if (state.value.status == ChatbotDraftAiEditStatus.ERROR) {
                previewFailure = false
                state.update { it.copy(status = ChatbotDraftAiEditStatus.LOADING) }
                generate()
            }
        }
    }

    private fun generate() {
        val current = state.value
        val shouldFail = previewFailure
        previewFailure = false
        viewModelScope.launch {
            val proposal = try {
                if (shouldFail) null else draftAi().proposeEdit(
                    DraftEditRequest(current.conversationId, current.contractId, current.draftId, current.scope, current.request)
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            if (ownerSession != currentSession()) return@launch
            state.update { it.copy(proposal = proposal, status = if (proposal != null) ChatbotDraftAiEditStatus.READY else ChatbotDraftAiEditStatus.ERROR) }
        }
    }

    class Factory(
        private val owner: String?,
        private val current: () -> String?,
        private val sessions: Flow<String?>,
        private val draftAi: () -> com.example.lagallens.domain.repository.DraftAiRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatbotDraftAiEditViewModel(owner, current, sessions, draftAi) as T
    }
}
