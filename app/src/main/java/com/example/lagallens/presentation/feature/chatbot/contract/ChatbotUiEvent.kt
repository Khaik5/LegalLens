package com.example.lagallens.presentation.feature.chatbot.contract

sealed interface ChatbotUiEvent {
    data class ScreenOpened(
        val contractName: String,
        val contractType: String,
        val contractId: String? = null,
        val conversationId: String? = null,
        val historyPreview: String? = null,
        val startNewConversation: Boolean = false,
        val draftRequest: String? = null,
        val draftClarification: String? = null,
        val previewDraftError: Boolean = false
    ) : ChatbotUiEvent

    data class MessageChanged(val message: String) : ChatbotUiEvent
    data class SuggestionClicked(val suggestion: String) : ChatbotUiEvent
    data object SendClicked : ChatbotUiEvent
    data class NewDraftClicked(val request: String, val clarification: String) : ChatbotUiEvent
    data class DraftDetailsSubmitted(val message: String, val acknowledgement: String) : ChatbotUiEvent
    data object ReviewDraftClicked : ChatbotUiEvent
    data object CreateDraftClicked : ChatbotUiEvent
    data object RetryDraftCreationClicked : ChatbotUiEvent
    data object ViewDraftClicked : ChatbotUiEvent
    data object EditDraftClicked : ChatbotUiEvent
    data object CitationClicked : ChatbotUiEvent
    data class ErrorRetryClicked(val messageId: Long) : ChatbotUiEvent
    data object AttachmentClicked : ChatbotUiEvent
    data object AttachmentCameraClicked : ChatbotUiEvent
    data object AttachmentSavedClicked : ChatbotUiEvent
    data class AttachmentSelected(val uri: String, val name: String) : ChatbotUiEvent
    data object AttachmentRemoved : ChatbotUiEvent
    data object DrawerOpened : ChatbotUiEvent
    data object DrawerDismissed : ChatbotUiEvent
    data object NewChatClicked : ChatbotUiEvent
    data object HistoryClicked : ChatbotUiEvent
    data object HelpClicked : ChatbotUiEvent
}
