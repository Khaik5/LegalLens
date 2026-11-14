package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.ChatbotApi
import com.example.lagallens.data.model.request.CreateConversationRequest
import com.example.lagallens.data.model.request.SendChatMessageRequest
import com.example.lagallens.data.model.response.ChatMessageResponse
import com.example.lagallens.data.model.response.ConversationResponse
import retrofit2.HttpException

class ChatbotRemoteDataSource(private val api: Lazy<ChatbotApi>) {
    suspend fun create(scope: AuthSessionScope, contractId: Long?, title: String?) =
        api.value.create(scope, CreateConversationRequest(contractId, title)).requireData("Conversation")

    /** Reads every page; the backend returns the most recently updated conversation first. */
    suspend fun conversations(scope: AuthSessionScope): List<ConversationResponse> =
        readAll { page -> api.value.list(scope, page).requireData("Conversations") }.distinctBy { it.id }

    /** Reads every page, oldest message first. */
    suspend fun messages(scope: AuthSessionScope, conversationId: Long): List<ChatMessageResponse> =
        readAll { page -> api.value.messages(scope, conversationId, page).requireData("Chat messages") }.distinctBy { it.id }

    suspend fun send(scope: AuthSessionScope, conversationId: Long, content: String, clientMessageId: String) =
        api.value.send(scope, conversationId, SendChatMessageRequest(content, clientMessageId)).requireData("Chat request")

    suspend fun retry(scope: AuthSessionScope, conversationId: Long, requestId: Long) =
        api.value.retry(scope, conversationId, requestId).requireData("Chat request")

    suspend fun delete(scope: AuthSessionScope, conversationId: Long) {
        val response = api.value.delete(scope, conversationId)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun legalArticle(scope: AuthSessionScope, articleId: Long) = api.value.legalArticle(scope, articleId).requireData("Legal article")

    /** The list endpoints have no total, so a short page marks the end. */
    private suspend fun <T> readAll(load: suspend (page: Int) -> List<T>): List<T> {
        val items = mutableListOf<T>()
        var page = 1
        while (page <= MAX_PAGES) {
            val batch = load(page)
            items.addAll(batch)
            if (batch.size < ChatbotApi.PAGE_SIZE) break
            page++
        }
        return items
    }

    private companion object {
        const val MAX_PAGES = 50
    }
}
