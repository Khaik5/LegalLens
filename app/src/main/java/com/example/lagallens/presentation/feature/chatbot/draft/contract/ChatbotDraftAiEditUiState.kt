package com.example.lagallens.presentation.feature.chatbot.draft.contract

enum class ChatbotDraftAiEditStatus { LOADING, ERROR, READY }

data class ChatbotDraftAiEditUiState(
    val status: ChatbotDraftAiEditStatus = ChatbotDraftAiEditStatus.LOADING,
    val scope: String = SELECTED_CLAUSE_SCOPE,
    val request: String = "",
    val draftId: String? = null,
    val contractId: String? = null,
    val conversationId: String? = null,
    val proposal: com.example.lagallens.domain.repository.DraftEditProposal? = null,
    val isInitialized: Boolean = false,
    /** What went wrong, when the backend said; null keeps the screen's generic text. */
    val errorMessageRes: Int? = null
) {
    companion object {
        const val SELECTED_CLAUSE_SCOPE = "selected_clause"
        const val WHOLE_CONTRACT_SCOPE = "whole_contract"
    }
}
