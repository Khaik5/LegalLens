package com.example.lagallens.presentation.feature.chatbot.draft.contract

data class ChatbotDraftUiState(
    val version: String = DRAFT_VERSION,
    val currentPage: Int = 0,
    val pageCount: Int = 6,
    val zoomPercent: Int = 100,
    val draftId: String? = null,
    val contractId: String? = null,
    val title: String? = null,
    val content: String? = null,
    val isLoading: Boolean = false
) {
    companion object {
        const val DRAFT_VERSION = "D1"
        const val REGENERATED_VERSION = "D1-R"
        const val MANUAL_VERSION = "M1"
        const val APPROVED_AI_VERSION = "D2"
        const val COPY_VERSION = "D3"
    }
}
