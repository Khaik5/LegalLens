package com.example.lagallens.presentation.feature.chatbot.contract

sealed interface ChatbotUiEvent {
    data class ScreenOpened(
        val contractName: String,
        val contractType: String,
        val initialGreeting: String,
        val initialSuggestions: List<String>,
        val newChatSuggestions: List<String>,
        val localResponse: String,
        val citationQuestion: String,
        val citationResponse: String,
        val citation: ChatbotCitation,
        val groundedQuestion: String,
        val groundedResponse: String,
        val groundedReferenceSectionTitle: String,
        val groundedReferences: List<ChatbotReference>,
        val noEvidenceQuestion: String,
        val noEvidence: ChatbotNoEvidence,
        val errorQuestion: String,
        val error: ChatbotError
    ) : ChatbotUiEvent

    data class MessageChanged(val message: String) : ChatbotUiEvent
    data class SuggestionClicked(val suggestion: String) : ChatbotUiEvent
    data object SendClicked : ChatbotUiEvent
    data object CitationClicked : ChatbotUiEvent
    data class ErrorRetryClicked(val messageId: Long) : ChatbotUiEvent
    data object AttachmentClicked : ChatbotUiEvent
    data object DrawerOpened : ChatbotUiEvent
    data object DrawerDismissed : ChatbotUiEvent
    data object NewChatClicked : ChatbotUiEvent
    data object HistoryClicked : ChatbotUiEvent
    data object HelpClicked : ChatbotUiEvent
}
