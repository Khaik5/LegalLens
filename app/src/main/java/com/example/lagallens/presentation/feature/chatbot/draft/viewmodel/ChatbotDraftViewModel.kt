package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotDraftViewModel(application: Application) : AndroidViewModel(application) {
    private val state = MutableStateFlow(ChatbotDraftUiState())
    val uiState = state.asStateFlow()
    private val repository get() = getApplication<LegalLensApplication>().draftRepository

    fun onEvent(event: ChatbotDraftUiEvent) {
        when (event) {
            is ChatbotDraftUiEvent.Opened -> load(event)
            is ChatbotDraftUiEvent.DraftLoaded -> state.update {
                it.copy(draftId = event.draftId, contractId = event.contractId, title = event.title, content = event.content, pageCount = 1, currentPage = 0, isLoading = false)
            }
            ChatbotDraftUiEvent.LoadFailed -> state.update { it.copy(isLoading = false) }
            is ChatbotDraftUiEvent.PageSelected -> state.update {
                it.copy(currentPage = event.page.coerceIn(0, it.pageCount - 1))
            }
            ChatbotDraftUiEvent.PreviousClicked -> state.update {
                it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0))
            }
            ChatbotDraftUiEvent.NextClicked -> state.update {
                it.copy(currentPage = (it.currentPage + 1).coerceAtMost(it.pageCount - 1))
            }
            ChatbotDraftUiEvent.ZoomInClicked -> state.update {
                it.copy(zoomPercent = (it.zoomPercent + 25).coerceAtMost(150))
            }
            ChatbotDraftUiEvent.ZoomOutClicked -> state.update {
                it.copy(zoomPercent = (it.zoomPercent - 25).coerceAtLeast(75))
            }
        }
    }

    private fun load(event: ChatbotDraftUiEvent.Opened) {
        val stage = DraftStage.fromLegacyKey(event.version) ?: DraftStage.ORIGINAL
        state.update { it.copy(version = event.version, contractId = event.contractId, isLoading = true) }
        viewModelScope.launch {
            try {
                val draft = withContext(Dispatchers.IO) {
                    repository.getDraft(event.draftId ?: event.version) ?: if (stage == DraftStage.ORIGINAL) {
                        repository.saveDraft(stage, event.initialTitle, event.initialContent, event.contractId)
                    } else null
                }
                if (draft != null) onEvent(ChatbotDraftUiEvent.DraftLoaded(draft.draftId, draft.contractId, draft.title, draft.content))
                else state.update { it.copy(isLoading = false) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                state.update { it.copy(isLoading = false) }
            }
        }
    }
}
