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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * waits a bounded time (about two minutes) for the answer; an unanswered question becomes a "check
 * again" card whose Retry polls again, and a failed one asks the server to retry it.
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
    /** Fallback for backends whose message list has no request ID: questions sent in this process. */
    private val requestIds = ConcurrentHashMap<Long, Long>()
    private val articleTitles = ConcurrentHashMap<Long, String>()
    /** New chats not yet on the server, by their placeholder id, and the server id each one got when created. */
    private val pendingConversations = ConcurrentHashMap<String, PendingConversation>()
    private val createdConversations = ConcurrentHashMap<String, Long>()
    private val creationLock = Mutex()

    private data class PendingConversation(val sessionKey: String, val contractId: Long?, val title: String?)

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
        // A new chat is only a local placeholder until its first question, so merely opening the screen never
        // leaves an empty conversation on the server. An empty one left by older app versions is reused first.
        val id = conversationId?.takeIf(String::isNotBlank)
            ?: unusedConversation(scope, contractId)
            ?: (PENDING_PREFIX + UUID.randomUUID()).also { key ->
                pendingConversations[key] = PendingConversation(
                    scope.key, contractId?.toLongOrNull(), contractName.trim().take(TITLE_MAX_LENGTH).ifBlank { null }
                )
            }
        val text = labels()
        ChatbotConversation(id, text.greeting, text.initialSuggestions, text.newConversationSuggestions)
    }

    override suspend fun persistConversationForSession(sessionKey: String?, conversationId: String): String = inSession { scope ->
        check(scope.key == sessionKey) { "Session changed" }
        serverConversationId(scope, conversationId).toString()
    }

    /** The server id of [conversationId]; a placeholder is created on the server the first time it is needed. */
    private suspend fun serverConversationId(scope: AuthSessionScope, conversationId: String): Long {
        conversationId.toLongOrNull()?.let { return it }
        createdConversations[conversationId]?.let { return it }
        return creationLock.withLock {
            createdConversations[conversationId]?.let { return@withLock it }
            val pending = requireNotNull(pendingConversations[conversationId]) { "Unknown conversation" }
            check(pending.sessionKey == scope.key) { "Session changed" }
            val id = remote.create(scope, pending.contractId, pending.title).id
            check(id > 0) { "Invalid conversation" }
            createdConversations[conversationId] = id
            pendingConversations.remove(conversationId)
            id
        }
    }

    /**
     * An unused conversation of the same contract, left by app versions that created one on every visit, is
     * reused instead of starting another. Without a summary (older backends) or when the lookup fails, the chat
     * starts as a local placeholder.
     */
    private suspend fun unusedConversation(scope: AuthSessionScope, contractId: String?): String? {
        val contract = contractId?.toLongOrNull() ?: return null
        return try {
            remote.recentConversations(scope)
                .firstOrNull { it.contractId == contract && it.messageCount == 0 }
                ?.id?.toString()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply = inSession { scope ->
        send(scope, serverConversationId(scope, conversationId), message)
    }

    override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply = inSession { scope ->
        val id = serverConversationId(scope, conversationId)
        val messages = remote.messages(scope, id)
        val question = messages.lastOrNull { it.isUserMessage && it.content == message }
        when (question?.requestStatus) {
            null -> send(scope, id, message)
            CHAT_REQUEST_COMPLETED -> replyFor(scope, question, messages)
            CHAT_REQUEST_FAILED -> {
                val requestId = question.requestId ?: requestIds[question.id] ?: return@inSession send(scope, id, message)
                remote.retry(scope, id, requestId)
                awaitReply(scope, id, question.id)
            }
            else -> awaitReply(scope, id, question.id)
        }
    }

    override suspend fun getMessages(conversationId: String): List<ChatbotTranscriptMessage> = inSession { scope ->
        val id = conversationId.toLongOrNull() ?: createdConversations[conversationId] ?: return@inSession emptyList()
        transcript(scope, remote.messages(scope, id))
    }

    override suspend fun getConversations(): List<ChatbotTranscript> = inSession { scope ->
        val conversations = remote.conversations(scope)
        val contracts = conversations.mapNotNull { it.contractId?.toString() }.distinct()
            .associateWith {
                try {
                    contractInfo(it)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
        // The server summary (count and last message) avoids reading every transcript.
        if (conversations.all { it.messageCount != null }) {
            return@inSession conversations.filter { (it.messageCount ?: 0) > 0 }.map { conversation ->
                val contractId = conversation.contractId?.toString()
                val contract = contractId?.let(contracts::get)
                ChatbotTranscript(
                    conversationId = conversation.id.toString(),
                    contractId = contractId,
                    contractName = contract?.name ?: conversation.title.orEmpty(),
                    contractType = contract?.typeLabel.orEmpty(),
                    updatedAtEpochMillis = conversation.lastMessageAt.toEpochMillisOrNull()
                        ?: conversation.updatedAt.toEpochMillisOrNull() ?: 0L,
                    messages = emptyList(),
                    messageCount = conversation.messageCount ?: 0,
                    lastMessagePreview = conversation.lastMessage.orEmpty()
                )
            }
        }
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
        pendingConversations.clear()
        createdConversations.clear()
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
            if (attempt > 0 || !firstPollImmediately) delay(pollDelayMillis(attempt))
            val messages = remote.messages(scope, conversationId)
            val question = messages.firstOrNull { it.id == questionId } ?: return failed()
            when (question.requestStatus) {
                CHAT_REQUEST_COMPLETED -> return replyFor(scope, question, messages)
                CHAT_REQUEST_FAILED -> return failed()
            }
        }
        return pending()
    }

    /** The first polls are quick for a fast answer; later ones are spaced out because the provider can take over a minute. */
    private fun pollDelayMillis(attempt: Int): Long =
        if (attempt < QUICK_POLLS) pollIntervalMillis else pollIntervalMillis * SLOW_POLL_FACTOR

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

    /** Runs in the current session and fails if the account changes meanwhile. */
    private suspend fun <T> inSession(action: suspend (AuthSessionScope) -> T): T = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val result = action(scope)
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        result
    }

    companion object {
        const val POLL_INTERVAL_MILLIS = 2_000L

        /** 10 quick polls of 2 s, then 25 of 4 s: about two minutes, longer than the slowest answer measured (89 s). */
        const val MAX_POLLS = 35
        private const val QUICK_POLLS = 10
        private const val SLOW_POLL_FACTOR = 2L
        private const val TITLE_MAX_LENGTH = 255
        private const val PENDING_PREFIX = "new-"
        private const val PARALLEL_LOADS = 4
    }
}
