package com.example.lagallens.presentation.feature.chatbot.history.contract

data class ChatbotHistoryUiState(
    val conversations: List<ChatbotHistoryConversation> = emptyList()
)
