package com.example.lagallens.data.model.response

import com.google.gson.JsonElement

data class ConversationResponse(
    val id: Long,
    val contractId: Long?,
    val title: String?,
    val createdAt: String?,
    val updatedAt: String?,
    /** Summary fields; absent on backends older than BE-04. */
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val messageCount: Int? = null,
    val requestStatus: String? = null
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
    val result: JsonElement?,
    /** The request this question started; absent on older backends. Lets a FAILED question be retried after a restart. */
    val requestId: Long? = null
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
    val title: String?,
    val content: String? = null
)
