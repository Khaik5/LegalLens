package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftListUiEffect {
    data class OpenDraft(val draftId: String, val contractId: String?) : ChatbotDraftListUiEffect
}
