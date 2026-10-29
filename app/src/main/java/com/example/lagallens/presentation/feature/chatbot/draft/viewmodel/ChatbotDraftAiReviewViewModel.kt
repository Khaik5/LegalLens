package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import androidx.lifecycle.ViewModel
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiReviewUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiReviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatbotDraftAiReviewViewModel : ViewModel() {
    private val state = MutableStateFlow(ChatbotDraftAiReviewUiState())
    val uiState = state.asStateFlow()

    fun onEvent(event: ChatbotDraftAiReviewUiEvent) {
        when (event) {
            ChatbotDraftAiReviewUiEvent.AcceptClicked -> state.update { it.copy(accepted = true) }
            ChatbotDraftAiReviewUiEvent.RejectClicked -> state.update { it.copy(accepted = false) }
        }
    }
}
