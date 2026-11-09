package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftManualEditUiEffect
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftManualEditUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftManualEditUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotDraftManualEditViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val ownerSession = app.currentSessionKey()
    fun isOwnerSession() = ownerSession == app.currentSessionKey()
    private val state = MutableStateFlow(ChatbotDraftManualEditUiState())
    val uiState = state.asStateFlow()
    private val effectChannel = Channel<ChatbotDraftManualEditUiEffect>(Channel.BUFFERED)
    val uiEffect = effectChannel.receiveAsFlow()
    init { viewModelScope.launch { app.observeSessionKeys().collect { if (it != ownerSession) {
        state.value = ChatbotDraftManualEditUiState()
        while (effectChannel.tryReceive().isSuccess) { }
    } } } }

    fun onEvent(event: ChatbotDraftManualEditUiEvent) {
        if (ownerSession != app.currentSessionKey()) return
        when (event) {
            is ChatbotDraftManualEditUiEvent.Opened -> if (!state.value.isDirty) {
                state.update { it.copy(body = event.body, addition = event.addition, isDirty = event.addition.isNotBlank()) }
            }
            is ChatbotDraftManualEditUiEvent.BodyChanged -> state.update {
                it.copy(body = event.body, isDirty = true)
            }
            is ChatbotDraftManualEditUiEvent.AdditionChanged -> state.update {
                it.copy(addition = event.addition, isDirty = true)
            }
            is ChatbotDraftManualEditUiEvent.SaveClicked -> save(event.sourceDraftId, event.contractId)
        }
    }

    private fun save(sourceDraftId: String?, contractId: String?) {
        val repository = try { app.draftRepository.forSession(ownerSession) } catch (_: Exception) { return }
        if (state.value.isSaving) return
        val content = listOf(state.value.body.trim(), state.value.addition.trim()).filter(String::isNotEmpty).joinToString("\n\n")
        if (content.isBlank()) {
            effectChannel.trySend(ChatbotDraftManualEditUiEffect.SaveFailed)
            return
        }
        state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val draft = repository.saveDraft(
                    stage = DraftStage.MANUAL_EDIT,
                    isTitleGenerated = true,
                    title = getApplication<Application>().getString(R.string.chatbot_draft_document_name, DraftStage.MANUAL_EDIT.legacyKey),
                    content = content,
                    contractId = contractId,
                    sourceDraftId = sourceDraftId ?: DraftStage.ORIGINAL.legacyKey
                )
                if (ownerSession == app.currentSessionKey()) {
                    state.update { it.copy(isSaving = false, isDirty = false) }
                    effectChannel.send(ChatbotDraftManualEditUiEffect.Saved(draft.draftId))
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                if (ownerSession == app.currentSessionKey()) {
                    state.update { it.copy(isSaving = false) }
                    effectChannel.send(ChatbotDraftManualEditUiEffect.SaveFailed)
                }
            }
        }
    }
}
