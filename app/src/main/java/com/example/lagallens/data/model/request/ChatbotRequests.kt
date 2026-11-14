package com.example.lagallens.data.model.request

data class CreateConversationRequest(
    val contractId: Long?,
    val title: String?
)

/** [clientMessageId] makes a resend of the same question return the existing request instead of a duplicate. */
data class SendChatMessageRequest(
    val content: String,
    val clientMessageId: String
)
