package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotDraftAiEditViewModel : ViewModel() {
    private val state = MutableStateFlow(ChatbotDraftAiEditUiState())
    val uiState = state.asStateFlow()
    private var previewFailure = false

    fun onEvent(event: ChatbotDraftAiEditUiEvent) {
        when (event) {
            is ChatbotDraftAiEditUiEvent.Opened -> if (!state.value.isInitialized) {
                previewFailure = event.previewFailure
                state.update {
                    it.copy(scope = event.scope, request = event.request, status = ChatbotDraftAiEditStatus.LOADING, isInitialized = true)
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
        viewModelScope.launch {
            delay(GENERATION_DELAY_MS)
            state.update {
                it.copy(status = if (previewFailure) ChatbotDraftAiEditStatus.ERROR else ChatbotDraftAiEditStatus.READY)
            }
            previewFailure = false
        }
    }

    private companion object {
        const val GENERATION_DELAY_MS = 1800L
    }
}
