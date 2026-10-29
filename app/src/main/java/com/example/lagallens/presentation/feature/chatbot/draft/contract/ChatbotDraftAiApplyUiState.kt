package com.example.lagallens.presentation.feature.chatbot.draft.contract

enum class ChatbotDraftAiApplyStatus { APPLYING, ERROR, SUCCESS }

data class ChatbotDraftAiApplyUiState(
    val status: ChatbotDraftAiApplyStatus = ChatbotDraftAiApplyStatus.APPLYING,
    val draftId: String? = null
)
