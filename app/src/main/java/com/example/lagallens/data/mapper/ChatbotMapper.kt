package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.ChatAnswerResponse
import com.example.lagallens.data.model.response.ChatMessageResponse
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.time.Instant

private val gson = Gson()

/** The stored answer may arrive as a JSON object or as JSON text, depending on the database driver. */
fun JsonElement?.toChatAnswer(): ChatAnswerResponse? {
    if (this == null || isJsonNull) return null
    val json = if (isJsonPrimitive && asJsonPrimitive.isString) asString else toString()
    return runCatching { gson.fromJson(json, ChatAnswerResponse::class.java) }.getOrNull()
}

fun String?.toEpochMillisOrNull(): Long? = this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

val ChatMessageResponse.isUserMessage: Boolean get() = senderType.equals(SENDER_USER, ignoreCase = true)

const val SENDER_USER = "USER"
const val CHAT_REQUEST_COMPLETED = "COMPLETED"
const val CHAT_REQUEST_FAILED = "FAILED"
