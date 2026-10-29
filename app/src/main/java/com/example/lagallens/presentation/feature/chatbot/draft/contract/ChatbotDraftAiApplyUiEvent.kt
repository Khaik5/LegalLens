package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftAiApplyUiEvent {
    data class Opened(val previewFailure: Boolean, val sourceDraftId: String?, val contractId: String?) : ChatbotDraftAiApplyUiEvent
    data object RetryClicked : ChatbotDraftAiApplyUiEvent
}
