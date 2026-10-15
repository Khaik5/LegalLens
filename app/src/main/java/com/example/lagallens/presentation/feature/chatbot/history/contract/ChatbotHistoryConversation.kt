package com.example.lagallens.presentation.feature.chatbot.history.contract

data class ChatbotHistoryConversation(
    val id: Long,
    val contractName: String,
    val timestamp: String,
    val messageCount: String,
    val preview: String
)
