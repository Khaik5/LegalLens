package com.example.lagallens.presentation.feature.chatbot.history.contract

data class ChatbotHistoryConversation(
    val id: String,
    val contractName: String,
    val timestamp: String,
    val messageCount: String,
    val preview: String,
    val isDraftRequest: Boolean = false,
    val contractId: String? = null
)
