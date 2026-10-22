package com.example.lagallens.domain.model.chatbot

data class ChatbotConversation(
    val id: String,
    val greeting: String,
    val initialSuggestions: List<String>,
    val newConversationSuggestions: List<String>
)
