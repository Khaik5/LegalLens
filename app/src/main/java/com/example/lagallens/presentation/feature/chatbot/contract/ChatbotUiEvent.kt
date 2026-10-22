package com.example.lagallens.presentation.feature.chatbot.contract

sealed interface ChatbotUiEvent {
    data class ScreenOpened(
        val contractName: String,
        val contractType: String
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
