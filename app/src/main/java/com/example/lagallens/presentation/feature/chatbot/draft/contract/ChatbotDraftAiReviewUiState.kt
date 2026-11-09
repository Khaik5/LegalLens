package com.example.lagallens.presentation.feature.chatbot.draft.contract

import com.example.lagallens.domain.repository.DraftEditProposal

data class ChatbotDraftAiReviewUiState(
    val proposal: DraftEditProposal? = null,
    val accepted: Boolean = true
)
