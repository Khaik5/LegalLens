package com.example.lagallens.presentation.feature.chatbot.draft.contract

enum class ChatbotDraftListStatus { LOADING, EMPTY, ERROR, CONTENT }

/** One draft of the user, shown by its newest version. */
data class ChatbotDraftListItem(
    val draftId: String,
    val contractId: String?,
    val title: String,
    val versionCount: Int,
    val updatedAt: Long,
    val isSaved: Boolean
)

data class ChatbotDraftListUiState(
    val status: ChatbotDraftListStatus = ChatbotDraftListStatus.LOADING,
    val items: List<ChatbotDraftListItem> = emptyList()
)
