package com.example.lagallens.presentation.feature.chatbot.draft.contract

enum class ChatbotDraftRegenerationStatus { LOADING, ERROR, SUCCESS }

data class ChatbotDraftRegenerationUiState(
    val status: ChatbotDraftRegenerationStatus = ChatbotDraftRegenerationStatus.LOADING,
    val isInitialized: Boolean = false,
    val draftId: String? = null
)
