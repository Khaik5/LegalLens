package com.example.lagallens.presentation.feature.chatbot.contract

data class ChatbotUiState(
    val contractName: String = "",
    val contractType: String = "",
    val messageDraft: String = "",
    val messages: List<ChatbotMessage> = emptyList()
)
