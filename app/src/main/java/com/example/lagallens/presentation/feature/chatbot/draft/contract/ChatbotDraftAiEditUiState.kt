package com.example.lagallens.presentation.feature.chatbot.draft.contract

enum class ChatbotDraftAiEditStatus { LOADING, ERROR, READY }

data class ChatbotDraftAiEditUiState(
    val status: ChatbotDraftAiEditStatus = ChatbotDraftAiEditStatus.LOADING,
    val scope: String = SELECTED_CLAUSE_SCOPE,
    val request: String = "",
    val isInitialized: Boolean = false
) {
    companion object {
        const val SELECTED_CLAUSE_SCOPE = "selected_clause"
        const val WHOLE_CONTRACT_SCOPE = "whole_contract"
    }
}
