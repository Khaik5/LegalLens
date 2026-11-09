package com.example.lagallens.presentation.feature.chatbot.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.usecase.OpenChatbotConversationUseCase
import com.example.lagallens.domain.usecase.RetryChatbotMessageUseCase
import com.example.lagallens.domain.usecase.SendChatbotMessageUseCase
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ChatbotSessionSafetyTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private class Repository : ChatbotRepository {
        val session = MutableStateFlow<String?>("A:1")
        var openGate: CompletableDeferred<Unit>? = null
        var saveGate: CompletableDeferred<Unit>? = null
        var retryGate: CompletableDeferred<Unit>? = null
        val saved = mutableListOf<String?>()
        override fun currentSessionKey() = session.value
        override fun observeSessionKeys() = session
        override suspend fun openConversation(contractName: String, contractType: String, contractId: String?, conversationId: String?): ChatbotConversation {
            openGate?.let { withContext(NonCancellable) { it.await() } }
            return ChatbotConversation("conversation", "DEMO", emptyList(), emptyList())
        }
        override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply = if (message == "fail") ChatbotReply.RetryableError("failed", "retry") else ChatbotReply.Answer("reply")
        override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply {
            retryGate?.let { withContext(NonCancellable) { it.await() } }
            return ChatbotReply.Answer("retry")
        }
        override suspend fun saveForSession(sessionKey: String?, conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) {
            saveGate?.let { withContext(NonCancellable) { it.await() } }
            check(session.value == sessionKey)
            saved.add(sessionKey)
        }
    }
    private fun viewModel(repo: Repository) = ChatbotViewModel(OpenChatbotConversationUseCase(repo), SendChatbotMessageUseCase(repo), RetryChatbotMessageUseCase(repo)) { com.example.lagallens.TestDraftAiRepository() }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun delayedOpenAfterSameAccountNewGenerationIsDiscarded() = runTest {
        val repo = Repository(); val gate = CompletableDeferred<Unit>(); repo.openGate = gate
        val vm = viewModel(repo)
        vm.onEvent(ChatbotUiEvent.ScreenOpened("A private", "Hợp đồng lao động"))
        repo.session.value = null; testScheduler.runCurrent()
        repo.session.value = "A:2"; testScheduler.runCurrent()
        gate.complete(Unit); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertFalse(vm.uiState.value.isConversationReady)
        vm.onEvent(ChatbotUiEvent.ScreenOpened("old intent", "Hợp đồng lao động"))
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun delayedTranscriptCannotWriteToNextAccount() = runTest {
        val repo = Repository(); val vm = viewModel(repo)
        vm.onEvent(ChatbotUiEvent.ScreenOpened("A private", "Hợp đồng lao động"))
        val gate = CompletableDeferred<Unit>(); repo.saveGate = gate
        vm.onEvent(ChatbotUiEvent.SuggestionClicked("private question"))
        repo.session.value = "B:2"; testScheduler.runCurrent()
        gate.complete(Unit); testScheduler.runCurrent()
        assertTrue(repo.saved.isEmpty())
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun retryResultFromOldGenerationIsDiscarded() = runTest {
        val repo = Repository(); val vm = viewModel(repo)
        vm.onEvent(ChatbotUiEvent.ScreenOpened("A", "Hợp đồng lao động"))
        vm.onEvent(ChatbotUiEvent.SuggestionClicked("fail"))
        val failed = vm.uiState.value.messages.last().id
        val priorWrites = repo.saved.size
        val gate = CompletableDeferred<Unit>(); repo.retryGate = gate
        vm.onEvent(ChatbotUiEvent.ErrorRetryClicked(failed))
        repo.session.value = "B:2"; testScheduler.runCurrent()
        repo.session.value = "A:3"; testScheduler.runCurrent()
        gate.complete(Unit); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertEquals(priorWrites, repo.saved.size)
    }
}
