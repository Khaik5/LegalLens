package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.model.chatbot.ChatbotAttachment
import com.example.lagallens.domain.model.chatbot.ChatbotCitation
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReference
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import com.example.lagallens.domain.repository.ChatbotRepository
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Local-only labor-contract chatbot. Stored transcripts are DEMO data and are never legal advice. */
class FakeChatbotRepository(context: Context? = null, private val sessionUserId: (() -> String?)? = null) : ChatbotRepository {
    private val appContext = context?.applicationContext
    override fun currentSessionKey(): String? {
        val context = appContext ?: return null
        if (sessionUserId != null) return sessionUserId.invoke()
        return AuthLocalDataSource(context).currentSessionScope()?.let { "${it.userId}:${it.generation}" }
    }
    override fun observeSessionKeys(): Flow<String?> {
        val context = appContext ?: return flowOf(null)
        if (sessionUserId != null) return flowOf(currentSessionKey())
        return AuthLocalDataSource(context).observeSessionScope().map { it?.let { scope -> "${scope.userId}:${scope.generation}" } }
    }
    private fun <T> inActiveSession(action: () -> T): T {
        val context = appContext ?: return action()
        // An injected account provider is used by isolated test fixtures.
        if (sessionUserId != null) return action()
        val auth = AuthLocalDataSource(context)
        val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
        return auth.withSession(scope, action)
    }
    private fun preferences(): android.content.SharedPreferences? {
        val context = appContext ?: return null
        val userId = if (sessionUserId != null) sessionUserId.invoke() else AuthLocalDataSource(context).currentSessionUserId()
        return context.getSharedPreferences(AccountStorageName.forUser(PREFERENCES_NAME, userId), Context.MODE_PRIVATE)
    }
    private val memory = linkedMapOf<String, ChatbotTranscript>()

    override suspend fun openForSession(sessionKey: String?, contractName: String, contractType: String, contractId: String?, conversationId: String?): ChatbotConversation = inActiveSession {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        openConversationLocal(contractName, contractType, contractId, conversationId)
    }
    override suspend fun saveForSession(sessionKey: String?, conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) = inActiveSession {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        saveMessagesLocal(conversationId, contractId, contractName, contractType, messages)
    }
    override suspend fun messagesForSession(sessionKey: String?, conversationId: String): List<ChatbotTranscriptMessage> = inActiveSession {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        readAll()[conversationId]?.messages.orEmpty()
    }
    override suspend fun sendForSession(sessionKey: String?, conversationId: String, message: String, attachment: ChatbotAttachment?): ChatbotReply = inActiveSession {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        sendMessageLocal(conversationId, message).withAttachmentNotice(attachment)
    }
    override suspend fun retryForSession(sessionKey: String?, conversationId: String, message: String, attachment: ChatbotAttachment?): ChatbotReply = inActiveSession {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        retryMessageLocal(conversationId).withAttachmentNotice(attachment)
    }

    /** No attachment is read in DEMO, and the reply says so instead of implying the file was analysed. */
    private fun ChatbotReply.withAttachmentNotice(attachment: ChatbotAttachment?): ChatbotReply =
        if (attachment != null && this is ChatbotReply.Answer) copy(content = content + "\n\n" + DEMO_ATTACHMENT_NOTICE) else this

    override suspend fun openConversation(
        contractName: String,
        contractType: String,
        contractId: String?,
        conversationId: String?
    ): ChatbotConversation = inActiveSession { openConversationLocal(contractName, contractType, contractId, conversationId) }

    private fun openConversationLocal(contractName: String, contractType: String, contractId: String?, conversationId: String?): ChatbotConversation {
        val id = conversationId?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
        val transcripts = readAll()
        if (id !in transcripts) {
            val greeting = ChatbotTranscriptMessage("1", false, DEMO_GREETING, System.currentTimeMillis())
            saveMessagesLocal(id, contractId, contractName, contractType, listOf(greeting))
        }
        return ChatbotConversation(id, DEMO_GREETING, INITIAL_SUGGESTIONS, NEW_CHAT_SUGGESTIONS)
    }

    override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply = inActiveSession { sendMessageLocal(conversationId, message) }
    private fun sendMessageLocal(conversationId: String, message: String): ChatbotReply {
        val normalized = message.trim()
        if (normalized.isBlank()) return ChatbotReply.Answer(DEMO_FALLBACK)
        val transcript = readAll()[conversationId]
            ?: return ChatbotReply.RetryableError("[DEMO] Hội thoại không còn khả dụng. Hãy tạo hội thoại mới.", "Thử lại")
        if (!transcript.isEmploymentContract()) {
            return ChatbotReply.NoEvidenceAnswer(
                title = "Chỉ hỗ trợ hợp đồng lao động",
                content = "Legal Lens hiện chỉ hỗ trợ ngữ cảnh hợp đồng lao động. Đây là thông báo DEMO, không phải tư vấn pháp lý."
            )
        }
        if (normalized == QA_RETRYABLE_ERROR_TRIGGER) return ChatbotReply.RetryableError("[DEMO] Mô phỏng lỗi gửi tin nhắn.", "Thử lại")
        return ChatbotReply.Answer(DEMO_RESPONSE)
    }

    override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply = inActiveSession { retryMessageLocal(conversationId) }
    private fun retryMessageLocal(conversationId: String): ChatbotReply {
        val transcript = readAll()[conversationId]
        return if (transcript == null) ChatbotReply.RetryableError("[DEMO] Hội thoại không còn khả dụng. Hãy tạo hội thoại mới.", "Thử lại")
        else if (!transcript.isEmploymentContract()) ChatbotReply.NoEvidenceAnswer("Chỉ hỗ trợ hợp đồng lao động", "Đây là thông báo DEMO, không phải tư vấn pháp lý.")
        else ChatbotReply.Answer(DEMO_RESPONSE)
    }

    private fun ChatbotTranscript.isEmploymentContract() = EMPLOYMENT_TYPE_LABELS.any { contractType.contains(it, ignoreCase = true) }

    override suspend fun getConversations(): List<ChatbotTranscript> = inActiveSession { readAll().values
        .filter { it.messages.size > 1 }
        .sortedByDescending { it.updatedAtEpochMillis } }

    override suspend fun conversationsForSession(sessionKey: String?): List<ChatbotTranscript> = inActiveSession {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        readAll().values.filter { it.messages.size > 1 }.sortedByDescending { it.updatedAtEpochMillis }
    }

    override suspend fun getMessages(conversationId: String): List<ChatbotTranscriptMessage> =
        inActiveSession { readAll()[conversationId]?.messages.orEmpty() }

    override suspend fun saveMessages(
        conversationId: String,
        contractId: String?,
        contractName: String,
        contractType: String,
        messages: List<ChatbotTranscriptMessage>
    ) = inActiveSession { saveMessagesLocal(conversationId, contractId, contractName, contractType, messages) }

    private fun saveMessagesLocal(conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) {
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
        writeAll(preferences, all)
    }

    private fun writeAll(preferences: android.content.SharedPreferences, all: Map<String, ChatbotTranscript>) {
        val json = JSONArray()
        all.values.forEach { item ->
            val messageArray = JSONArray()
            item.messages.forEach { message -> messageArray.put(message.toJson()) }
            json.put(JSONObject().put("id", item.conversationId).put("contractId", item.contractId)
                .put("contractName", item.contractName).put("contractType", item.contractType)
                .put("updatedAt", item.updatedAtEpochMillis).put("messages", messageArray))
        }
        check(preferences.edit().putString(KEY_TRANSCRIPTS, json.toString()).commit())
    }

    private fun ChatbotTranscriptMessage.toJson() = JSONObject().put("id", messageId).put("user", isUser)
        .put("content", content).put("createdAt", createdAtEpochMillis)
        .put("citation", citation?.let { JSONObject().put("title", it.title).put("description", it.description).put("action", it.actionLabel).put("referenceId", it.referenceId) })
        .put("referenceTitle", referenceSectionTitle)
        .put("references", JSONArray().apply { references.forEach { put(JSONObject().put("title", it.title).put("excerpt", it.excerpt)) } })
        .put("noEvidenceTitle", noEvidenceTitle).put("errorRetryLabel", errorRetryLabel)
        .put("retryQuestion", retryQuestion).put("draftNotice", showDraftNotice)

    private fun JSONObject.toTranscriptMessage(): ChatbotTranscriptMessage {
        val referenceRows = optJSONArray("references") ?: JSONArray()
        return ChatbotTranscriptMessage(
            messageId = optString("id"),
            isUser = optBoolean("user"),
            content = optString("content"),
            createdAtEpochMillis = optLong("createdAt"),
            citation = optJSONObject("citation")?.let {
                ChatbotCitation(it.optString("title"), it.optString("description"), it.optString("action"), it.optString("referenceId").takeIf(String::isNotBlank))
            },
            referenceSectionTitle = optString("referenceTitle").takeIf(String::isNotBlank),
            references = (0 until referenceRows.length()).map { index ->
                referenceRows.getJSONObject(index).let { ChatbotReference(it.optString("title"), it.optString("excerpt")) }
            },
            noEvidenceTitle = optString("noEvidenceTitle").takeIf(String::isNotBlank),
            errorRetryLabel = optString("errorRetryLabel").takeIf(String::isNotBlank),
            retryQuestion = optString("retryQuestion").takeIf(String::isNotBlank),
            showDraftNotice = optBoolean("draftNotice")
        )
    }

    override suspend fun deleteConversationsForContract(contractId: String) = inActiveSession {
        replaceAll { all -> all.filterValues { it.contractId != contractId } }
    }

    override suspend fun renameContract(contractId: String, title: String) = inActiveSession {
        replaceAll { all -> all.mapValues { (_, item) -> if (item.contractId == contractId) item.copy(contractName = title) else item } }
    }

    private fun replaceAll(transform: (Map<String, ChatbotTranscript>) -> Map<String, ChatbotTranscript>) {
        val preferences = preferences()
        val updated = transform(readAll(preferences))
        memory.clear()
        memory.putAll(updated)
        if (preferences != null) writeAll(preferences, updated)
    }

    override fun clearSessionData() {
        memory.clear()
    }

    override fun deleteConversation(conversationId: String) = inActiveSession {
        val preferences = preferences()
        val all = readAll(preferences).toMutableMap()
        all.remove(conversationId)
        memory.clear()
        memory.putAll(all)
        if (preferences == null) return@inActiveSession
        val previous = preferences.getString(KEY_TRANSCRIPTS, null)
        if (previous == null) return@inActiveSession
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
                    messageJson.getJSONObject(messageIndex).toTranscriptMessage()
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
        const val DEMO_ATTACHMENT_NOTICE = "[DEMO] Tệp đính kèm đã được nhận nhưng chưa được đọc nội dung."
        const val QA_RETRYABLE_ERROR_TRIGGER = "__demo_retryable_error__"
        val EMPLOYMENT_TYPE_LABELS = listOf("lao động", "employment")
        val INITIAL_SUGGESTIONS = listOf("Thời hạn hợp đồng lao động là bao lâu?", "Lương được ghi trong hợp đồng thế nào?", "Quyền lợi và nghĩa vụ của người lao động là gì?", "Cần lưu ý điều khoản nào?")
        val NEW_CHAT_SUGGESTIONS = listOf("Rà soát điều khoản chấm dứt hợp đồng", "Giải thích thời gian thử việc", "Tóm tắt quyền lợi trong hợp đồng")
    }
}
