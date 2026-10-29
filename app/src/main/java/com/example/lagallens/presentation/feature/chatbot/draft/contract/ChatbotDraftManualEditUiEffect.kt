package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftManualEditUiEffect {
    data class Saved(val draftId: String) : ChatbotDraftManualEditUiEffect
    data object SaveFailed : ChatbotDraftManualEditUiEffect
}
