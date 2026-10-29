package com.example.lagallens.presentation.feature.chatbot.contract

enum class DraftGenerationStatus { NONE, LOADING, ERROR, SUCCESS }

data class ChatbotUiState(
    val contractName: String = "",
    val contractType: String = "",
    val messageDraft: String = "",
    val messages: List<ChatbotMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isConversationReady: Boolean = false,
    val isReplyPending: Boolean = false,
    val isNewConversation: Boolean = false,
    val isDraftConversation: Boolean = false,
    val isDraftPreparing: Boolean = false,
    val isDraftReviewReady: Boolean = false,
    val isDraftReviewVisible: Boolean = false,
    val draftGenerationStatus: DraftGenerationStatus = DraftGenerationStatus.NONE,
    val isDrawerVisible: Boolean = false,
    val attachmentUri: String? = null,
    val attachmentName: String? = null
)
