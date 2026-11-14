package com.example.lagallens.data.model.response

import com.google.gson.JsonElement

data class ConversationResponse(
    val id: Long,
    val contractId: Long?,
    val title: String?,
    val createdAt: String?,
    val updatedAt: String?
)

/**
 * One stored message. A USER message carries the state of the answer request it started;
 * [result] is the stored answer JSON once that request has COMPLETED.
 */
data class ChatMessageResponse(
    val id: Long,
    val senderType: String,
    val content: String?,
    val createdAt: String?,
    val requestStatus: String?,
    val errorCode: String?,
    val clientMessageId: String?,
    val result: JsonElement?
)

/** Returned when a question is queued or retried; [id] is the request to retry. */
data class ChatRequestResponse(
    val id: Long,
    val messageId: Long?,
    val status: String,
    val result: JsonElement?
)

data class ChatAnswerResponse(
    val answer: String?,
    val noEvidence: Boolean,
    val citations: List<ChatCitationResponse>?
)

data class ChatCitationResponse(
    val articleId: Long,
    val articleVersion: Long?,
    val documentVersion: Long?,
    val excerpt: String?
)

data class LegalArticleResponse(
    val id: Long,
    val articleNumber: String?,
    val title: String?
)
