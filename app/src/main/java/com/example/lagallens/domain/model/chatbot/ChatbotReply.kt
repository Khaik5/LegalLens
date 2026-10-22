package com.example.lagallens.domain.model.chatbot

sealed interface ChatbotReply {
    data class Answer(val content: String) : ChatbotReply

    data class CitationAnswer(
        val content: String,
        val citation: ChatbotCitation
    ) : ChatbotReply

    data class GroundedAnswer(
        val content: String,
        val referenceSectionTitle: String,
        val references: List<ChatbotReference>
    ) : ChatbotReply

    data class NoEvidenceAnswer(
        val title: String,
        val content: String
    ) : ChatbotReply

    data class RetryableError(
        val title: String,
        val retryLabel: String
    ) : ChatbotReply
}

data class ChatbotCitation(
    val title: String,
    val description: String,
    val actionLabel: String
)

data class ChatbotReference(
    val title: String,
    val excerpt: String
)
