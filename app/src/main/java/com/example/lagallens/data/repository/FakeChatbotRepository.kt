package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import com.example.lagallens.domain.repository.ChatbotRepository
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Local-only labor-contract chatbot. Stored transcripts are DEMO data and are never legal advice. */
class FakeChatbotRepository(context: Context? = null, private val sessionUserId: (() -> String?)? = null) : ChatbotRepository {
    private val appContext = context?.applicationContext
    private fun preferences(): android.content.SharedPreferences? {
        val context = appContext ?: return null
        val userId = if (sessionUserId != null) sessionUserId.invoke() else AuthLocalDataSource(context).currentSessionUserId()
        return context.getSharedPreferences(AccountStorageName.forUser(PREFERENCES_NAME, userId), Context.MODE_PRIVATE)
    }
    private val memory = linkedMapOf<String, ChatbotTranscript>()

    override suspend fun openConversation(
        contractName: String,
        contractType: String,
        contractId: String?,
        conversationId: String?
    ): ChatbotConversation {
        val id = conversationId?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
        val transcripts = readAll()
        if (id !in transcripts) {
            val greeting = ChatbotTranscriptMessage("1", false, DEMO_GREETING, System.currentTimeMillis())
            saveMessages(id, contractId, contractName, contractType, listOf(greeting))
        }
        return ChatbotConversation(id, DEMO_GREETING, INITIAL_SUGGESTIONS, NEW_CHAT_SUGGESTIONS)
    }

    override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply {
        val normalized = message.trim()
        if (normalized.isBlank()) return ChatbotReply.Answer(DEMO_FALLBACK)
        val transcript = readAll()[conversationId]
            ?: return ChatbotReply.RetryableError("[DEMO] Hội thoại không còn khả dụng. Hãy tạo hội thoại mới.", "Thử lại")
        if (!transcript.contractType.contains("lao động", ignoreCase = true)) {
            return ChatbotReply.NoEvidenceAnswer(
                title = "Chỉ hỗ trợ hợp đồng lao động",
                content = "Legal Lens hiện chỉ hỗ trợ ngữ cảnh hợp đồng lao động. Đây là thông báo DEMO, không phải tư vấn pháp lý."
            )
        }
        if (normalized == QA_RETRYABLE_ERROR_TRIGGER) return ChatbotReply.RetryableError("[DEMO] Mô phỏng lỗi gửi tin nhắn.", "Thử lại")
        return ChatbotReply.Answer(DEMO_RESPONSE)
    }

    override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply = ChatbotReply.Answer(DEMO_RESPONSE)

    override suspend fun getConversations(): List<ChatbotTranscript> = readAll().values
        .filter { it.messages.size > 1 }
        .sortedByDescending { it.updatedAtEpochMillis }

    override suspend fun getMessages(conversationId: String): List<ChatbotTranscriptMessage> =
        readAll()[conversationId]?.messages.orEmpty()

    override suspend fun saveMessages(
        conversationId: String,
        contractId: String?,
        contractName: String,
        contractType: String,
        messages: List<ChatbotTranscriptMessage>
    ) {
        if (conversationId.isBlank()) return
        val preferences = preferences()
        val transcript = ChatbotTranscript(
            conversationId = conversationId,
            contractId = contractId,
            contractName = contractName,
            contractType = contractType,
            updatedAtEpochMillis = messages.lastOrNull()?.createdAtEpochMillis ?: System.currentTimeMillis(),
            messages = messages
        )
        val all = readAll(preferences).toMutableMap().apply { put(conversationId, transcript) }
        memory.clear()
        memory.putAll(all)
        if (preferences == null) return
        val json = JSONArray()
        all.values.forEach { item ->
            val messageArray = JSONArray()
            item.messages.forEach { message ->
                messageArray.put(JSONObject().put("id", message.messageId).put("user", message.isUser)
                    .put("content", message.content).put("createdAt", message.createdAtEpochMillis))
            }
            json.put(JSONObject().put("id", item.conversationId).put("contractId", item.contractId)
                .put("contractName", item.contractName).put("contractType", item.contractType)
                .put("updatedAt", item.updatedAtEpochMillis).put("messages", messageArray))
        }
        check(preferences.edit().putString(KEY_TRANSCRIPTS, json.toString()).commit())
    }

    override fun clearSessionData() {
        memory.clear()
    }

    override fun deleteConversation(conversationId: String) {
        val preferences = preferences()
        val all = readAll(preferences).toMutableMap()
        all.remove(conversationId)
        memory.clear()
        memory.putAll(all)
        if (preferences == null) return
        val previous = preferences.getString(KEY_TRANSCRIPTS, null)
        if (previous == null) return
        val filtered = JSONArray()
        val source = JSONArray(previous)
        for (index in 0 until source.length()) {
            val row = source.getJSONObject(index)
            if (row.optString("id") != conversationId) filtered.put(row)
        }
        check(preferences.edit().putString(KEY_TRANSCRIPTS, filtered.toString()).commit())
    }

    private fun readAll(preferences: android.content.SharedPreferences? = preferences()): Map<String, ChatbotTranscript> {
        if (preferences == null) return memory.toMap()
        val source = preferences.getString(KEY_TRANSCRIPTS, null) ?: return emptyMap()
        return runCatching {
            val result = linkedMapOf<String, ChatbotTranscript>()
            val json = JSONArray(source)
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                val messageJson = item.optJSONArray("messages") ?: JSONArray()
                val messages = (0 until messageJson.length()).map { messageIndex ->
                    val message = messageJson.getJSONObject(messageIndex)
                    ChatbotTranscriptMessage(
                        messageId = message.optString("id"),
                        isUser = message.optBoolean("user"),
                        content = message.optString("content"),
                        createdAtEpochMillis = message.optLong("createdAt")
                    )
                }
                val id = item.optString("id")
                if (id.isNotBlank()) result[id] = ChatbotTranscript(
                    id, item.optString("contractId").takeIf(String::isNotBlank),
                    item.optString("contractName"), item.optString("contractType"),
                    item.optLong("updatedAt"), messages
                )
            }
            memory.clear()
            memory.putAll(result)
            result
        }.getOrElse { throw IllegalStateException("Unable to read conversation data", it) }
    }

    private companion object {
        const val PREFERENCES_NAME = "chatbot_demo_transcripts"
        const val KEY_TRANSCRIPTS = "conversations_v1"
        const val DEMO_GREETING = "[DEMO] Tôi có thể minh họa các câu hỏi về hợp đồng lao động. Nội dung không phải tư vấn pháp lý."
        const val DEMO_FALLBACK = "[DEMO] Phản hồi minh họa, chưa được AI phân tích và không phải tư vấn pháp lý."
        const val DEMO_RESPONSE = "[DEMO] Tôi đã ghi nhận câu hỏi về hợp đồng lao động. Phản hồi này chỉ minh họa giao diện, chưa được phân tích bởi AI và không phải tư vấn pháp lý."
        const val QA_RETRYABLE_ERROR_TRIGGER = "__demo_retryable_error__"
        val INITIAL_SUGGESTIONS = listOf("Thời hạn hợp đồng lao động là bao lâu?", "Lương được ghi trong hợp đồng thế nào?", "Quyền lợi và nghĩa vụ của người lao động là gì?", "Cần lưu ý điều khoản nào?")
        val NEW_CHAT_SUGGESTIONS = listOf("Rà soát điều khoản chấm dứt hợp đồng", "Giải thích thời gian thử việc", "Tóm tắt quyền lợi trong hợp đồng")
    }
}
