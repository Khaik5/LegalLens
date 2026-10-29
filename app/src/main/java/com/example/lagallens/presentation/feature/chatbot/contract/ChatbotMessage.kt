package com.example.lagallens.presentation.feature.chatbot.contract

enum class ChatbotMessageAuthor {
    ASSISTANT,
    USER
}

data class ChatbotMessage(
    val id: Long,
    val author: ChatbotMessageAuthor,
    val content: String,
    val citation: ChatbotCitation? = null,
    val referenceSectionTitle: String? = null,
    val references: List<ChatbotReference> = emptyList(),
    val noEvidence: ChatbotNoEvidence? = null,
    val error: ChatbotError? = null,
    val showDraftNotice: Boolean = false,
    val isDraftAction: Boolean = false,
    val isDraftLoading: Boolean = false
)

data class ChatbotCitation(
    val title: String,
    val description: String,
    val actionLabel: String
)

data class ChatbotReference(
    val title: String,
    val excerpt: String
)

data class ChatbotNoEvidence(
    val title: String,
    val message: String
)

data class ChatbotError(
    val title: String,
    val retryLabel: String
)
