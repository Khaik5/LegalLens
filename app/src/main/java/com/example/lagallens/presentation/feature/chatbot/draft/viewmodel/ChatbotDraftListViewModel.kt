package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListItem
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiEffect
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Lists the user's drafts so one can be opened again after leaving the chat that created it. */
class ChatbotDraftListViewModel(
    private val ownerSession: String?,
    private val currentSession: () -> String?,
    private val drafts: () -> DraftRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatbotDraftListUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChatbotDraftListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ChatbotDraftListUiEvent) {
        if (ownerSession != currentSession()) return
        when (event) {
            ChatbotDraftListUiEvent.Opened -> load()
            ChatbotDraftListUiEvent.RetryClicked -> load()
            is ChatbotDraftListUiEvent.ItemClicked ->
                viewModelScope.launch { _uiEffect.emit(ChatbotDraftListUiEffect.OpenDraft(event.item.draftId, event.item.contractId)) }
        }
    }

    private fun load() {
        // A refresh keeps the list on screen; only the first load shows the spinner.
        if (_uiState.value.items.isEmpty()) _uiState.update { it.copy(status = ChatbotDraftListStatus.LOADING) }
        viewModelScope.launch {
            val loaded = try {
                drafts().getDrafts()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            if (ownerSession != currentSession()) return@launch
            _uiState.update { current ->
                when {
                    loaded == null -> if (current.items.isEmpty()) current.copy(status = ChatbotDraftListStatus.ERROR) else current
                    else -> loaded.toItems().let { items ->
                        ChatbotDraftListUiState(if (items.isEmpty()) ChatbotDraftListStatus.EMPTY else ChatbotDraftListStatus.CONTENT, items)
                    }
                }
            }
        }
    }

    /** One row per draft: the newest version names it and sets its place in the list. */
    private fun List<Draft>.toItems(): List<ChatbotDraftListItem> =
        groupBy { it.lineageId }.values.map { versions ->
            val latest = versions.maxBy { it.versionNo }
            ChatbotDraftListItem(
                draftId = latest.draftId,
                contractId = latest.contractId,
                title = latest.title,
                versionCount = versions.size,
                updatedAt = latest.updatedAt,
                isSaved = versions.any { it.isSaved }
            )
        }.sortedByDescending { it.updatedAt }

    class Factory(
        private val owner: String?,
        private val current: () -> String?,
        private val drafts: () -> DraftRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatbotDraftListViewModel(owner, current, drafts) as T
    }
}
