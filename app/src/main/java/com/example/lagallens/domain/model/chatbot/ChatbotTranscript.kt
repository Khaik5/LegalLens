package com.example.lagallens.domain.model.chatbot

data class ChatbotTranscriptMessage(
    val messageId: String,
    val isUser: Boolean,
    val content: String,
    val createdAtEpochMillis: Long
)

data class ChatbotTranscript(
    val conversationId: String,
    val contractId: String?,
    val contractName: String,
    val contractType: String,
    val updatedAtEpochMillis: Long,
    val messages: List<ChatbotTranscriptMessage>
)
