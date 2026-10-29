package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftRegenerationUiEvent {
    data class Opened(
        val previewFailure: Boolean,
        val sourceVersion: String,
        val sourceDraftId: String?,
        val contractId: String?
    ) : ChatbotDraftRegenerationUiEvent
    data object RetryClicked : ChatbotDraftRegenerationUiEvent
}
