package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftManualEditUiEvent {
    data class Opened(val body: String, val addition: String) : ChatbotDraftManualEditUiEvent
    data class BodyChanged(val body: String) : ChatbotDraftManualEditUiEvent
    data class AdditionChanged(val addition: String) : ChatbotDraftManualEditUiEvent
    data class SaveClicked(val sourceDraftId: String?, val contractId: String?) : ChatbotDraftManualEditUiEvent
}
