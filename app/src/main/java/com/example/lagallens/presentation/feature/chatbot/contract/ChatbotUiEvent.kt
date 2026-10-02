package com.example.lagallens.presentation.feature.chatbot.contract

sealed interface ChatbotUiEvent {
    data class ScreenOpened(
        val contractName: String,
        val contractType: String,
        val initialGreeting: String,
        val initialQuestion: String,
        val initialAnswer: String,
        val localResponse: String
    ) : ChatbotUiEvent

    data class MessageChanged(val message: String) : ChatbotUiEvent
    data object SendClicked : ChatbotUiEvent
    data object AttachmentClicked : ChatbotUiEvent
    data object BackClicked : ChatbotUiEvent
}
