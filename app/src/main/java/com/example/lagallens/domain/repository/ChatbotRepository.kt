package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.chatbot.ChatbotAttachment
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ChatbotRepository {
    fun currentSessionKey(): String? = null
    fun observeSessionKeys(): Flow<String?> = flowOf(currentSessionKey())
    suspend fun conversationsForSession(sessionKey: String?): List<ChatbotTranscript> {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return getConversations()
    }
    suspend fun messagesForSession(sessionKey: String?, conversationId: String): List<ChatbotTranscriptMessage> {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return getMessages(conversationId)
    }
    suspend fun sendForSession(sessionKey: String?, conversationId: String, message: String, attachment: ChatbotAttachment? = null): ChatbotReply {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return sendMessage(conversationId, message)
    }
    suspend fun retryForSession(sessionKey: String?, conversationId: String, message: String, attachment: ChatbotAttachment? = null): ChatbotReply {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return retryMessage(conversationId, message)
    }
    suspend fun openForSession(sessionKey: String?, contractName: String, contractType: String, contractId: String?, conversationId: String?): ChatbotConversation {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return openConversation(contractName, contractType, contractId, conversationId)
    }
    /** Deletes one conversation for good. Returns true once it is gone, including when it was already gone. */
    suspend fun deleteConversationForSession(sessionKey: String?, conversationId: String): Boolean {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        deleteConversation(conversationId)
        return true
    }
    suspend fun saveForSession(sessionKey: String?, conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        saveMessages(conversationId, contractId, contractName, contractType, messages)
    }
    /**
     * The stored id of [conversationId]. A repository may open a new chat as a local placeholder that is only
     * stored with its first question; work that needs a stored conversation before that, such as drafting,
     * calls this first. Repositories that store conversations at once return the id unchanged.
     */
    suspend fun persistConversationForSession(sessionKey: String?, conversationId: String): String {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return conversationId
    }
    suspend fun openConversation(contractName: String, contractType: String, contractId: String? = null, conversationId: String? = null): ChatbotConversation

    suspend fun sendMessage(conversationId: String, message: String): ChatbotReply

    suspend fun retryMessage(conversationId: String, message: String): ChatbotReply

    suspend fun getConversations(): List<ChatbotTranscript> = emptyList()
    suspend fun getMessages(conversationId: String): List<ChatbotTranscriptMessage> = emptyList()
    suspend fun saveMessages(conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) = Unit
    fun deleteConversation(conversationId: String) = Unit
    /** The full text of a legal article cited in an answer; null when it is not available any more. */
    suspend fun legalArticleText(articleId: Long): com.example.lagallens.domain.model.chatbot.LegalArticleText? = null
    suspend fun deleteConversationsForContract(contractId: String) = Unit
    suspend fun renameContract(contractId: String, title: String) = Unit
    fun clearSessionData() = Unit
}
