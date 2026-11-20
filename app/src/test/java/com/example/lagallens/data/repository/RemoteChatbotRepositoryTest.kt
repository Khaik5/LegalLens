package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.ChatbotRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.ChatbotApi
import com.example.lagallens.data.model.request.CreateConversationRequest
import com.example.lagallens.data.model.request.SendChatMessageRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ChatMessageResponse
import com.example.lagallens.data.model.response.ChatRequestResponse
import com.example.lagallens.data.model.response.ConversationResponse
import com.example.lagallens.data.model.response.LegalArticleResponse
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteChatbotRepositoryTest {
    private val api = ChatbotFixture()
    private val labels = RemoteChatbotLabels(
        greeting = "greeting",
        initialSuggestions = listOf("initial"),
        newConversationSuggestions = listOf("new"),
        pendingTitle = "pending",
        checkAgainLabel = "check again",
        failedTitle = "failed",
        retryLabel = "retry",
        noEvidenceTitle = "no evidence",
        referenceSectionTitle = { count -> "references:$count" },
        articleTitle = { number, title, id -> listOfNotNull(number, title).joinToString(" ").ifBlank { "article:$id" } }
    )
    private var nextClientId = 0
    private val repository = RemoteChatbotRepository(
        RemoteRepositorySession(),
        ChatbotRemoteDataSource(lazyOf(api)),
        labels = { labels },
        contractInfo = { id -> RemoteChatbotContractInfo("Contract $id", "Employment") },
        pollIntervalMillis = 1,
        maxPolls = 3,
        newClientMessageId = { "client-${++nextClientId}" }
    )

    @Test fun openingANewChatStoresNothingUntilTheFirstQuestion() = runTest {
        val conversation = repository.openConversation("HĐ lao động", "Employment", contractId = "12")

        assertTrue(api.created.isEmpty())
        assertTrue(repository.getMessages(conversation.id).isEmpty())
        assertEquals("greeting", conversation.greeting)
        assertEquals(listOf("initial"), conversation.initialSuggestions)
    }

    @Test fun theFirstQuestionCreatesTheConversationOnceAndLaterQuestionsReuseIt() = runTest {
        val conversation = repository.openConversation("HĐ lao động", "Employment", contractId = "12")

        repository.sendMessage(conversation.id, "Lương?")
        repository.sendMessage(conversation.id, "Thử việc?")

        assertEquals(CreateConversationRequest(12, "HĐ lao động"), api.created.single())
        assertEquals(listOf(5L, 5L), api.sentTo)
    }

    @Test fun draftingStoresANewChatAndGetsItsServerId() = runTest {
        val conversation = repository.openConversation("HĐ", "Employment", contractId = "12")

        val stored = repository.persistConversationForSession(repository.currentSessionKey(), conversation.id)

        assertEquals("5", stored)
        assertEquals(1, api.created.size)
        assertEquals("5", repository.persistConversationForSession(repository.currentSessionKey(), conversation.id))
        assertEquals(1, api.created.size)
    }

    @Test fun reusesAnUnusedConversationOfTheSameContractInsteadOfCreatingAnother() = runTest {
        api.conversations = listOf(ConversationResponse(9, 12, "HĐ", null, null, messageCount = 0))

        val conversation = repository.openConversation("HĐ", "Employment", contractId = "12")

        assertEquals("9", conversation.id)
        assertTrue(api.created.isEmpty())
    }

    @Test fun aContractWhoseChatsAlreadyHaveQuestionsStartsANewLocalChat() = runTest {
        api.conversations = listOf(ConversationResponse(9, 12, "HĐ", null, null, messageCount = 2))

        assertNotEquals("9", repository.openConversation("HĐ", "Employment", contractId = "12").id)
        assertTrue(api.created.isEmpty())
    }

    @Test fun doesNotReuseAnUnusedConversationOfAnotherContract() = runTest {
        api.conversations = listOf(ConversationResponse(9, 13, "Khác", null, null, messageCount = 0))

        assertNotEquals("9", repository.openConversation("HĐ", "Employment", contractId = "12").id)
        assertTrue(api.created.isEmpty())
    }

    @Test fun withoutASummaryOrAListTheChatStillStartsLocally() = runTest {
        api.conversations = listOf(ConversationResponse(9, 12, "HĐ", null, null))
        val withoutSummary = repository.openConversation("HĐ", "Employment", contractId = "12")
        api.listFailure = IllegalStateException("offline")
        val withoutList = repository.openConversation("HĐ", "Employment", contractId = "12")

        assertNotEquals("9", withoutSummary.id)
        assertNotEquals(withoutSummary.id, withoutList.id)
        assertTrue(api.created.isEmpty())
    }

    @Test fun aGeneralConversationWithoutAContractIsStoredWithItsFirstQuestion() = runTest {
        api.conversations = listOf(ConversationResponse(9, null, "Chung", null, null, messageCount = 0))

        val conversation = repository.openConversation("Chung", "", contractId = null)
        repository.sendMessage(conversation.id, "Xin chào")

        assertEquals(CreateConversationRequest(null, "Chung"), api.created.single())
    }

    @Test fun anAnswerThatArrivesAfterTheQuickPollsIsStillReturned() = runTest {
        val subject = RemoteChatbotRepository(
            RemoteRepositorySession(), ChatbotRemoteDataSource(lazyOf(api)), labels = { labels },
            contractInfo = { id -> RemoteChatbotContractInfo("Contract $id", "Employment") },
            pollIntervalMillis = 1,
            maxPolls = 14,
            newClientMessageId = { "client-1" }
        )
        api.onSend = { content ->
            api.messages = listOf(question(10, content, "WAITING_PROVIDER"))
            ChatRequestResponse(100, 10, "WAITING_PROVIDER", null)
        }
        api.onMessagesRead = { reads ->
            if (reads == 13) api.messages = listOf(
                question(10, "Lương?", "COMPLETED", """{"answer":"10 triệu.","noEvidence":false,"citations":[]}"""),
                answer(11, "10 triệu.")
            )
        }

        val reply = subject.sendMessage("5", "Lương?") as ChatbotReply.Answer

        assertEquals("10 triệu.", reply.content)
        assertEquals(13, api.messageReads)
    }

    @Test fun reopensAnExistingConversationWithoutCreatingOne() = runTest {
        val conversation = repository.openConversation("HĐ", "Employment", contractId = "12", conversationId = "9")

        assertEquals("9", conversation.id)
        assertTrue(api.created.isEmpty())
    }

    @Test fun sendsAQuestionAndReturnsTheGroundedAnswer() = runTest {
        api.onSend = { content ->
            api.messages = listOf(
                question(10, content, "COMPLETED", """{"answer":"Tối đa 60 ngày.","noEvidence":false,"citations":[{"articleId":3,"articleVersion":1,"documentVersion":1,"excerpt":"không quá 60 ngày"}]}"""),
                answer(11, "Tối đa 60 ngày.")
            )
            ChatRequestResponse(100, 10, "WAITING_PROVIDER", null)
        }

        val reply = repository.sendMessage("5", "Thử việc bao lâu?")

        val grounded = reply as ChatbotReply.GroundedAnswer
        assertEquals("Tối đa 60 ngày.", grounded.content)
        assertEquals("references:1", grounded.referenceSectionTitle)
        assertEquals("25 Thời gian thử việc", grounded.references.single().title)
        assertEquals("không quá 60 ngày", grounded.references.single().excerpt)
        assertEquals(SendChatMessageRequest("Thử việc bao lâu?", "client-1"), api.sent.single())
    }

    @Test fun readsAnAnswerStoredAsJsonText() = runTest {
        api.onSend = { content ->
            api.messages = listOf(
                question(10, content, "COMPLETED", "\"{\\\"answer\\\":null,\\\"noEvidence\\\":true,\\\"citations\\\":[]}\""),
                answer(11, "Không tìm thấy bằng chứng phù hợp để trả lời.")
            )
            ChatRequestResponse(100, 10, "WAITING_PROVIDER", null)
        }

        val reply = repository.sendMessage("5", "Có hỗ trợ đỗ xe không?") as ChatbotReply.NoEvidenceAnswer

        assertEquals("no evidence", reply.title)
        assertEquals("Không tìm thấy bằng chứng phù hợp để trả lời.", reply.content)
    }

    @Test fun anUnansweredQuestionBecomesACheckAgainCard() = runTest {
        api.onSend = { content ->
            api.messages = listOf(question(10, content, "WAITING_PROVIDER"))
            ChatRequestResponse(100, 10, "WAITING_PROVIDER", null)
        }

        val reply = repository.sendMessage("5", "Lương?") as ChatbotReply.RetryableError

        assertEquals("pending", reply.title)
        assertEquals("check again", reply.retryLabel)
        assertEquals(3, api.messageReads)
    }

    @Test fun checkingAgainReturnsTheAnswerWithoutSendingTheQuestionTwice() = runTest {
        api.messages = listOf(question(10, "Lương?", "COMPLETED", """{"answer":"10 triệu.","noEvidence":false,"citations":[]}"""), answer(11, "10 triệu."))

        val reply = repository.retryMessage("5", "Lương?") as ChatbotReply.Answer

        assertEquals("10 triệu.", reply.content)
        assertTrue(api.sent.isEmpty())
    }

    @Test fun retryingAFailedQuestionAsksTheServerToRetryIt() = runTest {
        api.onSend = { content ->
            api.messages = listOf(question(10, content, "FAILED"))
            ChatRequestResponse(100, 10, "WAITING_PROVIDER", null)
        }
        assertEquals("failed", (repository.sendMessage("5", "Lương?") as ChatbotReply.RetryableError).title)
        api.onRetry = { _ ->
            api.messages = listOf(question(10, "Lương?", "COMPLETED", """{"answer":"10 triệu.","noEvidence":false,"citations":[]}"""), answer(11, "10 triệu."))
            ChatRequestResponse(100, null, "WAITING_PROVIDER", null)
        }

        val reply = repository.retryMessage("5", "Lương?") as ChatbotReply.Answer

        assertEquals("10 triệu.", reply.content)
        assertEquals(listOf(100L), api.retried)
        assertEquals(1, api.sent.size)
    }

    @Test fun aFailedQuestionFromAnEarlierSessionIsSentAgain() = runTest {
        api.messages = listOf(question(10, "Lương?", "FAILED"))
        api.onSend = { _ -> ChatRequestResponse(101, 12, "FAILED", null) }

        repository.retryMessage("5", "Lương?")

        assertTrue(api.retried.isEmpty())
        assertEquals("Lương?", api.sent.single().content)
    }

    @Test fun rebuildsTheTranscriptWithAnswersAndPendingCards() = runTest {
        api.messages = listOf(
            question(10, "Thử việc?", "COMPLETED", """{"answer":"60 ngày.","noEvidence":false,"citations":[{"articleId":3,"excerpt":"60 ngày"}]}"""),
            answer(11, "60 ngày."),
            question(12, "Lương?", "WAITING_PROVIDER"),
            question(13, "Thưởng?", "FAILED")
        )

        val messages = repository.getMessages("5")

        assertEquals(listOf("10", "11", "12", "-12", "13", "-13"), messages.map { it.messageId })
        assertEquals("references:1", messages[1].referenceSectionTitle)
        assertEquals("pending", messages[3].content)
        assertEquals("check again", messages[3].errorRetryLabel)
        assertEquals("Lương?", messages[3].retryQuestion)
        assertEquals("retry", messages[5].errorRetryLabel)
        assertEquals("Thưởng?", messages[5].retryQuestion)
    }

    @Test fun historyUsesTheCurrentContractAndSkipsEmptyConversations() = runTest {
        api.conversations = listOf(
            ConversationResponse(5, 12, "Tên cũ", "2026-11-10T09:30:00.000Z", "2026-11-10T09:30:00.000Z"),
            ConversationResponse(6, null, null, null, null)
        )
        api.messagesByConversation = mapOf(5L to listOf(question(10, "Lương?", "WAITING_PROVIDER")))

        val history = repository.getConversations().single()

        assertEquals("5", history.conversationId)
        assertEquals("Contract 12", history.contractName)
        assertEquals("Employment", history.contractType)
        assertEquals(1_794_303_000_000L, history.updatedAtEpochMillis)
    }

    @Test fun historyUsesTheServerSummaryWithoutReadingMessages() = runTest {
        api.conversations = listOf(
            ConversationResponse(
                5, 12, "Tên cũ", "2026-11-10T09:00:00.000Z", "2026-11-10T09:00:00.000Z",
                lastMessage = "60 ngày.", lastMessageAt = "2026-11-10T09:30:00.000Z", messageCount = 2, requestStatus = "COMPLETED"
            ),
            ConversationResponse(6, null, "Trống", null, null, messageCount = 0)
        )

        val history = repository.getConversations().single()

        assertEquals(0, api.messageReads)
        assertEquals("5", history.conversationId)
        assertEquals("Contract 12", history.contractName)
        assertEquals(2, history.messageCount)
        assertEquals("60 ngày.", history.lastMessagePreview)
        assertEquals(1_794_303_000_000L, history.updatedAtEpochMillis)
    }

    @Test fun anArticleThatCannotBeLoadedKeepsAFallbackTitle() = runTest {
        api.missingArticles = true
        api.messages = listOf(
            question(10, "Thử việc?", "COMPLETED", """{"answer":"60 ngày.","noEvidence":false,"citations":[{"articleId":3,"excerpt":"60 ngày"}]}"""),
            answer(11, "60 ngày.")
        )

        val reference = repository.getMessages("5")[1].references.single()

        assertEquals("article:3", reference.title)
        assertNull(repository.getMessages("5")[1].noEvidenceTitle)
    }

    @Test fun cancellationDuringHistoryContractLookupIsPropagated() = runTest {
        api.conversations = listOf(ConversationResponse(5, 12, "Contract", null, null, messageCount = 1))
        val cancelled = kotlinx.coroutines.CancellationException("cancelled")
        val subject = RemoteChatbotRepository(
            RemoteRepositorySession(), ChatbotRemoteDataSource(lazyOf(api)), labels = { labels },
            contractInfo = { throw cancelled }
        )
        val result = runCatching { subject.getConversations() }
        assertTrue("Cancellation must escape the contract lookup: $result", result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
        assertEquals(cancelled.message, result.exceptionOrNull()?.message)
    }

    private fun question(id: Long, content: String, status: String?, result: String? = null) = ChatMessageResponse(
        id, "USER", content, "2026-11-10T09:30:00.000Z", status, null, "client", result?.let(JsonParser::parseString)
    )

    private fun answer(id: Long, content: String) =
        ChatMessageResponse(id, "AI", content, "2026-11-10T09:30:05.000Z", null, null, null, null)
}

private class ChatbotFixture : ChatbotApi {
    val created = mutableListOf<CreateConversationRequest>()
    val sent = mutableListOf<SendChatMessageRequest>()
    val sentTo = mutableListOf<Long>()
    val retried = mutableListOf<Long>()
    var conversations = emptyList<ConversationResponse>()
    var messages = emptyList<ChatMessageResponse>()
    var messagesByConversation: Map<Long, List<ChatMessageResponse>>? = null
    var messageReads = 0
    var missingArticles = false
    var listFailure: Exception? = null
    var onMessagesRead: (Int) -> Unit = {}
    var onSend: (String) -> ChatRequestResponse = { ChatRequestResponse(1, 1, "WAITING_PROVIDER", null) }
    var onRetry: (Long) -> ChatRequestResponse = { ChatRequestResponse(it, null, "WAITING_PROVIDER", null) }

    override suspend fun create(scope: AuthSessionScope, request: CreateConversationRequest): ApiEnvelope<ConversationResponse> {
        created += request
        return ApiEnvelope(true, ConversationResponse(5, request.contractId, request.title, null, null))
    }

    override suspend fun list(scope: AuthSessionScope, page: Int, limit: Int): ApiEnvelope<List<ConversationResponse>> {
        listFailure?.let { throw it }
        return ApiEnvelope(true, if (page == 1) conversations else emptyList())
    }

    override suspend fun messages(scope: AuthSessionScope, conversationId: Long, page: Int, limit: Int): ApiEnvelope<List<ChatMessageResponse>> {
        if (page > 1) return ApiEnvelope(true, emptyList())
        messageReads++
        onMessagesRead(messageReads)
        return ApiEnvelope(true, messagesByConversation?.get(conversationId).orEmpty().takeIf { messagesByConversation != null } ?: messages)
    }

    override suspend fun send(scope: AuthSessionScope, conversationId: Long, request: SendChatMessageRequest): ApiEnvelope<ChatRequestResponse> {
        sent += request
        sentTo += conversationId
        return ApiEnvelope(true, onSend(request.content))
    }

    override suspend fun retry(scope: AuthSessionScope, conversationId: Long, requestId: Long): ApiEnvelope<ChatRequestResponse> {
        retried += requestId
        return ApiEnvelope(true, onRetry(requestId))
    }

    override suspend fun delete(scope: AuthSessionScope, conversationId: Long): Response<Unit> = Response.success(Unit)

    override suspend fun legalArticle(scope: AuthSessionScope, articleId: Long): ApiEnvelope<LegalArticleResponse> {
        if (missingArticles) throw IllegalStateException("Published article not found")
        return ApiEnvelope(true, LegalArticleResponse(articleId, "25", "Thời gian thử việc"))
    }
}
