package com.example.lagallens.domain.model.chatbot

data class ChatbotTranscriptMessage(
    val messageId: String,
    val isUser: Boolean,
    val content: String,
    val createdAtEpochMillis: Long,
    val citation: ChatbotCitation? = null,
    val referenceSectionTitle: String? = null,
    val references: List<ChatbotReference> = emptyList(),
    val noEvidenceTitle: String? = null,
    val errorRetryLabel: String? = null,
    val retryQuestion: String? = null,
    val showDraftNotice: Boolean = false
)

data class ChatbotTranscript(
    val conversationId: String,
    val contractId: String?,
    val contractName: String,
    val contractType: String,
    val updatedAtEpochMillis: Long,
    val messages: List<ChatbotTranscriptMessage>
)
