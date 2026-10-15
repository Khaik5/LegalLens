package com.example.lagallens.presentation.feature.chatbot.contract

data class ChatbotUiState(
    val contractName: String = "",
    val contractType: String = "",
    val messageDraft: String = "",
    val messages: List<ChatbotMessage> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isNewConversation: Boolean = false,
    val isDrawerVisible: Boolean = false
)
