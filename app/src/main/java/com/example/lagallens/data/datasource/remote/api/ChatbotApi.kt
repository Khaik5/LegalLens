package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.CreateConversationRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.request.SendChatMessageRequest
import com.example.lagallens.data.model.response.ChatMessageResponse
import com.example.lagallens.data.model.response.ChatRequestResponse
import com.example.lagallens.data.model.response.ConversationResponse
import com.example.lagallens.data.model.response.LegalArticleResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

/** Every response except a 204 is wrapped in the `{ success, data }` envelope. */
interface ChatbotApi {
    @POST("conversations")
    suspend fun create(@Tag scope: AuthSessionScope, @Body request: CreateConversationRequest): ApiEnvelope<ConversationResponse>

    @GET("conversations")
    suspend fun list(
        @Tag scope: AuthSessionScope,
        @Query("page") page: Int,
        @Query("limit") limit: Int = PAGE_SIZE
    ): ApiEnvelope<List<ConversationResponse>>

    /** Oldest first. */
    @GET("conversations/{id}/messages")
    suspend fun messages(
        @Tag scope: AuthSessionScope,
        @Path("id") conversationId: Long,
        @Query("page") page: Int,
        @Query("limit") limit: Int = PAGE_SIZE
    ): ApiEnvelope<List<ChatMessageResponse>>

    @POST("conversations/{id}/messages")
    suspend fun send(
        @Tag scope: AuthSessionScope,
        @Path("id") conversationId: Long,
        @Body request: SendChatMessageRequest
    ): ApiEnvelope<ChatRequestResponse>

    @POST("conversations/{id}/messages/{requestId}/retry")
    suspend fun retry(
        @Tag scope: AuthSessionScope,
        @Path("id") conversationId: Long,
        @Path("requestId") requestId: Long
    ): ApiEnvelope<ChatRequestResponse>

    /** 204 without a body. */
    @DELETE("conversations/{id}")
    suspend fun delete(@Tag scope: AuthSessionScope, @Path("id") conversationId: Long): Response<Unit>

    @GET("legal-articles/{id}")
    suspend fun legalArticle(@Tag scope: AuthSessionScope, @Path("id") articleId: Long): ApiEnvelope<LegalArticleResponse>

    companion object {
        const val PAGE_SIZE = 100
    }
}
