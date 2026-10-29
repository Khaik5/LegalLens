package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftUiEvent {
    data class Opened(
        val version: String,
        val initialTitle: String,
        val initialContent: String,
        val contractId: String?,
        val draftId: String? = null
    ) : ChatbotDraftUiEvent
    data class DraftLoaded(val draftId: String, val contractId: String?, val title: String, val content: String) : ChatbotDraftUiEvent
    data object LoadFailed : ChatbotDraftUiEvent
    data class PageSelected(val page: Int) : ChatbotDraftUiEvent
    data object PreviousClicked : ChatbotDraftUiEvent
    data object NextClicked : ChatbotDraftUiEvent
    data object ZoomInClicked : ChatbotDraftUiEvent
    data object ZoomOutClicked : ChatbotDraftUiEvent
}
