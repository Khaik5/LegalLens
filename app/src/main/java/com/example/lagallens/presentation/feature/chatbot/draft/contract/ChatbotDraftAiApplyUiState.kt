package com.example.lagallens.presentation.feature.chatbot.draft.contract

enum class ChatbotDraftAiApplyStatus { APPLYING, ERROR, SUCCESS }

data class ChatbotDraftAiApplyUiState(
    val status: ChatbotDraftAiApplyStatus = ChatbotDraftAiApplyStatus.APPLYING,
    val draftId: String? = null,
    /** What went wrong, when the backend said; null keeps the screen's generic text. */
    val errorMessageRes: Int? = null
)
