package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftAiEditUiEvent {
    data class Opened(val scope: String, val request: String, val previewFailure: Boolean) : ChatbotDraftAiEditUiEvent
    data object RetryClicked : ChatbotDraftAiEditUiEvent
}
