package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.ChatbotRemoteDataSource
import com.example.lagallens.data.mapper.CHAT_REQUEST_COMPLETED
import com.example.lagallens.data.mapper.CHAT_REQUEST_FAILED
import com.example.lagallens.data.mapper.isUserMessage
import com.example.lagallens.data.mapper.toChatAnswer
import com.example.lagallens.data.mapper.toEpochMillisOrNull
import com.example.lagallens.data.model.response.ChatAnswerResponse
import com.example.lagallens.data.model.response.ChatMessageResponse
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReference
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import com.example.lagallens.domain.repository.ChatbotRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Text the remote chatbot shows; resolved on each use so it follows the app language. */
data class RemoteChatbotLabels(
    val greeting: String,
    val initialSuggestions: List<String>,
    val newConversationSuggestions: List<String>,
    val pendingTitle: String,
    val checkAgainLabel: String,
    val failedTitle: String,
    val retryLabel: String,
    val noEvidenceTitle: String,
    val referenceSectionTitle: (count: Int) -> String,
    val articleTitle: (articleNumber: String?, title: String?, articleId: Long) -> String
)

data class RemoteChatbotContractInfo(val name: String, val typeLabel: String)

/**
 * Backend conversations. A question is queued on the server and answered by a worker, so sending
 * waits a bounded time for the answer; an unanswered question becomes a "check again" card whose
 * Retry polls again, and a failed one asks the server to retry it.
 */
class RemoteChatbotRepository(
    private val tokens: AuthSessionStore,
    private val remote: ChatbotRemoteDataSource,
    private val labels: () -> RemoteChatbotLabels,
    private val contractInfo: suspend (contractId: String) -> RemoteChatbotContractInfo?,
    private val pollIntervalMillis: Long = POLL_INTERVAL_MILLIS,
    private val maxPolls: Int = MAX_POLLS,
    private val newClientMessageId: () -> String = { UUID.randomUUID().toString() }
) : ChatbotRepository {
    /** Request IDs of questions sent in this process; the message list does not return them. */
    private val requestIds = ConcurrentHashMap<Long, Long>()
    private val articleTitles = ConcurrentHashMap<Long, String>()

    override fun currentSessionKey(): String? = tokens.currentSessionScope()?.key

    override fun observeSessionKeys(): Flow<String?> = tokens.observeTokenChanges()
        .map { currentSessionKey() }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    override suspend fun openConversation(
        contractName: String,
        contractType: String,
        contractId: String?,
        conversationId: String?
    ): ChatbotConversation = inSession { scope ->
        val id = conversationId?.takeIf(String::isNotBlank)
            ?: remote.create(scope, contractId?.toLongOrNull(), contractName.trim().take(TITLE_MAX_LENGTH).ifBlank { null }).id
                .also { check(it > 0) { "Invalid conversation" } }.toString()
        val text = labels()
        ChatbotConversation(id, text.greeting, text.initialSuggestions, text.newConversationSuggestions)
    }

    override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply = inSession { scope ->
        send(scope, conversationId.toConversationId(), message)
    }

    override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply = inSession { scope ->
        val id = conversationId.toConversationId()
        val messages = remote.messages(scope, id)
        val question = messages.lastOrNull { it.isUserMessage && it.content == message }
        when (question?.requestStatus) {
            null -> send(scope, id, message)
            CHAT_REQUEST_COMPLETED -> replyFor(scope, question, messages)
            CHAT_REQUEST_FAILED -> {
                val requestId = requestIds[question.id] ?: return@inSession send(scope, id, message)
                remote.retry(scope, id, requestId)
                awaitReply(scope, id, question.id)
            }
            else -> awaitReply(scope, id, question.id)
        }
    }

    override suspend fun getMessages(conversationId: String): List<ChatbotTranscriptMessage> = inSession { scope ->
        transcript(scope, remote.messages(scope, conversationId.toConversationId()))
    }

    override suspend fun getConversations(): List<ChatbotTranscript> = inSession { scope ->
        val conversations = remote.conversations(scope)
        val contracts = conversations.mapNotNull { it.contractId?.toString() }.distinct()
            .associateWith { runCatching { contractInfo(it) }.getOrNull() }
        coroutineScope {
            conversations.chunked(PARALLEL_LOADS).flatMap { batch ->
                batch.map { conversation ->
                    async {
                        val contractId = conversation.contractId?.toString()
                        val contract = contractId?.let(contracts::get)
                        val messages = transcript(scope, remote.messages(scope, conversation.id))
                        ChatbotTranscript(
                            conversationId = conversation.id.toString(),
                            contractId = contractId,
                            contractName = contract?.name ?: conversation.title.orEmpty(),
                            contractType = contract?.typeLabel.orEmpty(),
                            updatedAtEpochMillis = conversation.updatedAt.toEpochMillisOrNull()
                                ?: messages.lastOrNull()?.createdAtEpochMillis ?: 0L,
                            messages = messages
                        )
                    }
                }.awaitAll()
            }
        // Opening the chat screen creates a conversation; one without a question is not history.
        }.filter { it.messages.isNotEmpty() }
    }

    /** The server stores every question and answer itself. */
    override suspend fun saveMessages(
        conversationId: String,
        contractId: String?,
        contractName: String,
        contractType: String,
        messages: List<ChatbotTranscriptMessage>
    ) = Unit

    override fun clearSessionData() {
        requestIds.clear()
        articleTitles.clear()
    }

    private suspend fun send(scope: AuthSessionScope, conversationId: Long, message: String): ChatbotReply {
        val request = remote.send(scope, conversationId, message, newClientMessageId())
        val messageId = request.messageId
        if (messageId != null) requestIds[messageId] = request.id
        if (request.status == CHAT_REQUEST_FAILED) return failed()
        if (messageId == null) return pending()
        return awaitReply(scope, conversationId, messageId, firstPollImmediately = request.status == CHAT_REQUEST_COMPLETED)
    }

    private suspend fun awaitReply(
        scope: AuthSessionScope,
        conversationId: Long,
        questionId: Long,
        firstPollImmediately: Boolean = false
    ): ChatbotReply {
        repeat(maxPolls) { attempt ->
            if (attempt > 0 || !firstPollImmediately) delay(pollIntervalMillis)
            val messages = remote.messages(scope, conversationId)
            val question = messages.firstOrNull { it.id == questionId } ?: return failed()
            when (question.requestStatus) {
                CHAT_REQUEST_COMPLETED -> return replyFor(scope, question, messages)
                CHAT_REQUEST_FAILED -> return failed()
            }
        }
        return pending()
    }

    private suspend fun replyFor(scope: AuthSessionScope, question: ChatMessageResponse, messages: List<ChatMessageResponse>): ChatbotReply {
        val answerText = answerTextAfter(question, messages)
        val answer = question.result.toChatAnswer() ?: return ChatbotReply.Answer(answerText.orEmpty())
        val text = labels()
        if (answer.noEvidence) return ChatbotReply.NoEvidenceAnswer(text.noEvidenceTitle, answerText.orEmpty())
        val references = references(scope, answer)
        val content = answer.answer ?: answerText.orEmpty()
        return if (references.isEmpty()) ChatbotReply.Answer(content)
        else ChatbotReply.GroundedAnswer(content, text.referenceSectionTitle(references.size), references)
    }

    /** The worker stores the answer as the next AI message after the question. */
    private fun answerTextAfter(question: ChatMessageResponse, messages: List<ChatMessageResponse>): String? {
        val index = messages.indexOfFirst { it.id == question.id }
        if (index == -1) return null
        return messages.drop(index + 1).takeWhile { !it.isUserMessage }.firstOrNull()?.content
    }

    private suspend fun references(scope: AuthSessionScope, answer: ChatAnswerResponse): List<ChatbotReference> =
        answer.citations.orEmpty().map { citation ->
            ChatbotReference(articleTitle(scope, citation.articleId), citation.excerpt.orEmpty())
        }

    private suspend fun articleTitle(scope: AuthSessionScope, articleId: Long): String =
        articleTitles[articleId] ?: run {
            val article = runCatching { remote.legalArticle(scope, articleId) }
                .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }
                .getOrNull()
            val title = labels().articleTitle(article?.articleNumber, article?.title, articleId)
            if (article != null) articleTitles[articleId] = title
            title
        }

    /** Rebuilds the screen's messages: each question is followed by its answer, or by a card to retry or check again. */
    private suspend fun transcript(scope: AuthSessionScope, messages: List<ChatMessageResponse>): List<ChatbotTranscriptMessage> {
        val text = labels()
        val result = mutableListOf<ChatbotTranscriptMessage>()
        var lastQuestion: ChatMessageResponse? = null
        messages.forEachIndexed { index, message ->
            val createdAt = message.createdAt.toEpochMillisOrNull() ?: 0L
            if (message.isUserMessage) {
                lastQuestion = message
                result += ChatbotTranscriptMessage(message.id.toString(), true, message.content.orEmpty(), createdAt)
                val hasAnswer = messages.getOrNull(index + 1)?.isUserMessage == false
                if (message.requestStatus != null && message.requestStatus != CHAT_REQUEST_COMPLETED && !hasAnswer) {
                    val isFailed = message.requestStatus == CHAT_REQUEST_FAILED
                    result += ChatbotTranscriptMessage(
                        messageId = (-message.id).toString(),
                        isUser = false,
                        content = if (isFailed) text.failedTitle else text.pendingTitle,
                        createdAtEpochMillis = createdAt,
                        errorRetryLabel = if (isFailed) text.retryLabel else text.checkAgainLabel,
                        retryQuestion = message.content
                    )
                }
                return@forEachIndexed
            }
            val question = lastQuestion
            lastQuestion = null
            val reply = question?.takeIf { it.requestStatus == CHAT_REQUEST_COMPLETED }?.let { replyFor(scope, it, messages) }
            result += reply.toTranscript(message.id.toString(), message.content.orEmpty(), createdAt)
        }
        return result
    }

    private fun ChatbotReply?.toTranscript(id: String, storedText: String, createdAt: Long) = when (this) {
        is ChatbotReply.GroundedAnswer -> ChatbotTranscriptMessage(
            id, false, content, createdAt, referenceSectionTitle = referenceSectionTitle, references = references
        )
        is ChatbotReply.NoEvidenceAnswer -> ChatbotTranscriptMessage(id, false, content, createdAt, noEvidenceTitle = title)
        else -> ChatbotTranscriptMessage(id, false, storedText, createdAt)
    }

    private fun pending(): ChatbotReply = labels().let { ChatbotReply.RetryableError(it.pendingTitle, it.checkAgainLabel) }

    private fun failed(): ChatbotReply = labels().let { ChatbotReply.RetryableError(it.failedTitle, it.retryLabel) }

    private fun String.toConversationId(): Long = requireNotNull(toLongOrNull()) { "Unknown conversation" }

    /** Runs in the current session and fails if the account changes meanwhile. */
    private suspend fun <T> inSession(action: suspend (AuthSessionScope) -> T): T = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val result = action(scope)
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        result
    }

    companion object {
        const val POLL_INTERVAL_MILLIS = 2_000L
        const val MAX_POLLS = 10
        private const val TITLE_MAX_LENGTH = 255
        private const val PARALLEL_LOADS = 4
    }
}
