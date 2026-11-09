package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotDraftAiApplyViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val ownerSession = app.currentSessionKey()
    private val state = MutableStateFlow(ChatbotDraftAiApplyUiState())
    val uiState = state.asStateFlow()
    private var initialized = false
    private var previewFailure = false
    private var sourceDraftId: String? = null
    private var contractId: String? = null
    private var proposalId = ""

    init { viewModelScope.launch { app.observeSessionKeys().collect { if (it != ownerSession) {
        sourceDraftId = null
        contractId = null
        state.value = ChatbotDraftAiApplyUiState()
    } } } }

    fun onEvent(event: ChatbotDraftAiApplyUiEvent) {
        if (ownerSession != app.currentSessionKey()) return
        when (event) {
            is ChatbotDraftAiApplyUiEvent.Opened -> if (!initialized) {
                initialized = true
                previewFailure = event.previewFailure
                sourceDraftId = event.sourceDraftId
                contractId = event.contractId
                proposalId = event.proposalId
                applyProposal()
            }
            ChatbotDraftAiApplyUiEvent.RetryClicked -> if (state.value.status == ChatbotDraftAiApplyStatus.ERROR) {
                previewFailure = false
                state.update { it.copy(status = ChatbotDraftAiApplyStatus.APPLYING) }
                applyProposal()
            }
        }
    }

    private fun applyProposal() {
        val repository = try { app.draftAiRepository(ownerSession) } catch (_: Exception) { return }
        val capturedSourceId = sourceDraftId
        val capturedContractId = contractId
        val capturedProposalId = proposalId
        viewModelScope.launch {
            if (ownerSession != app.currentSessionKey()) return@launch
            val shouldFail = previewFailure
            previewFailure = false
            state.update { it.copy(status = ChatbotDraftAiApplyStatus.APPLYING) }
            try {
                if (shouldFail) error("Preview failure")
                val draft = repository.applyEdit(capturedProposalId, capturedSourceId, capturedContractId)
                if (ownerSession == app.currentSessionKey()) state.update { it.copy(status = ChatbotDraftAiApplyStatus.SUCCESS, draftId = draft.draftId) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                if (ownerSession == app.currentSessionKey()) state.update { it.copy(status = ChatbotDraftAiApplyStatus.ERROR) }
            }
        }
    }

}
