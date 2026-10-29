package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface ChatbotDraftAiReviewUiEvent {
    data object AcceptClicked : ChatbotDraftAiReviewUiEvent
    data object RejectClicked : ChatbotDraftAiReviewUiEvent
}
