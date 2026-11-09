package com.example.lagallens.presentation.feature.chatbot.draft.contract

import com.example.lagallens.domain.repository.DraftEditProposal

sealed interface ChatbotDraftAiReviewUiEvent {
    data class Opened(val proposal: DraftEditProposal) : ChatbotDraftAiReviewUiEvent
    data object AcceptClicked : ChatbotDraftAiReviewUiEvent
    data object RejectClicked : ChatbotDraftAiReviewUiEvent
}
