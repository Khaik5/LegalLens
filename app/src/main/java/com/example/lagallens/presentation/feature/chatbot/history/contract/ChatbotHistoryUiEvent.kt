package com.example.lagallens.presentation.feature.chatbot.history.contract

sealed interface ChatbotHistoryUiEvent {
    data class ScreenOpened(val conversations: List<ChatbotHistoryConversation> = emptyList()) : ChatbotHistoryUiEvent
    data object BackClicked : ChatbotHistoryUiEvent
    data object SearchClicked : ChatbotHistoryUiEvent
    data class QueryChanged(val query: String) : ChatbotHistoryUiEvent
    data class ConversationClicked(val conversation: ChatbotHistoryConversation) : ChatbotHistoryUiEvent
}
