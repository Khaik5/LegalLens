package com.example.lagallens.presentation.feature.chatbot.contract

enum class ChatbotMessageAuthor {
    ASSISTANT,
    USER
}

data class ChatbotMessage(
    val id: Long,
    val author: ChatbotMessageAuthor,
    val content: String
)
