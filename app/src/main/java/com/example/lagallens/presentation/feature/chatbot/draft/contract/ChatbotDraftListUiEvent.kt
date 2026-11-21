package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftListUiEvent {
    data object Opened : ChatbotDraftListUiEvent
    data object RetryClicked : ChatbotDraftListUiEvent
    data class ItemClicked(val item: ChatbotDraftListItem) : ChatbotDraftListUiEvent
}
