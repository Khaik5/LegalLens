package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotDraftRegenerationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val ownerSession = app.currentSessionKey()
    private val state = MutableStateFlow(ChatbotDraftRegenerationUiState())
    val uiState = state.asStateFlow()
    private var previewFailure = false
    private var sourceVersion = "D1"
    private var sourceDraftId: String? = null
    private var contractId: String? = null
    init { viewModelScope.launch { app.observeSessionKeys().collect { if (it != ownerSession) {
        sourceVersion = "D1"; sourceDraftId = null; contractId = null
        state.value = ChatbotDraftRegenerationUiState()
    } } } }

    fun onEvent(event: ChatbotDraftRegenerationUiEvent) {
        if (ownerSession != app.currentSessionKey()) return
        when (event) {
            is ChatbotDraftRegenerationUiEvent.Opened -> if (!state.value.isInitialized) {
                previewFailure = event.previewFailure
                sourceVersion = event.sourceVersion
                sourceDraftId = event.sourceDraftId
                contractId = event.contractId
                state.update { it.copy(isInitialized = true, status = ChatbotDraftRegenerationStatus.LOADING) }
                generate()
            }
            ChatbotDraftRegenerationUiEvent.RetryClicked -> if (state.value.status == ChatbotDraftRegenerationStatus.ERROR) {
                previewFailure = false
                state.update { it.copy(status = ChatbotDraftRegenerationStatus.LOADING) }
                generate()
            }
        }
    }

    private fun generate() {
        val repository = try { app.draftAiRepository(ownerSession) } catch (_: Exception) { return }
        val originalVersion = sourceVersion
        val originalDraftId = sourceDraftId
        val originalContractId = contractId
        viewModelScope.launch {
            val shouldFail = previewFailure
            previewFailure = false
            try {
                if (shouldFail) error("Preview failure")
                val draft = repository.regenerate(originalDraftId, originalVersion, originalContractId)
                if (ownerSession == app.currentSessionKey()) state.update { it.copy(status = ChatbotDraftRegenerationStatus.SUCCESS, draftId = draft.draftId) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                if (ownerSession == app.currentSessionKey()) state.update { it.copy(status = ChatbotDraftRegenerationStatus.ERROR) }
            }
        }
    }

}
