package com.example.lagallens.presentation.feature.chatbot.history.contract

sealed interface ChatbotHistoryUiEvent {
    data class ScreenOpened(val conversations: List<ChatbotHistoryConversation>) : ChatbotHistoryUiEvent
    data object BackClicked : ChatbotHistoryUiEvent
    data object SearchClicked : ChatbotHistoryUiEvent
}
