package com.example.lagallens.presentation.feature.chatbot.history.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatbotHistorySessionTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private class Repository : ChatbotRepository {
        val session = MutableStateFlow<String?>("A:1")
        val gate = CompletableDeferred<Unit>()
        override fun currentSessionKey() = session.value
        override fun observeSessionKeys() = session
        override suspend fun getConversations(): List<ChatbotTranscript> {
            withContext(NonCancellable) { gate.await() }
            return listOf(ChatbotTranscript("a", "contract-a", "private", "lao động", 1, emptyList()))
        }
        override suspend fun openConversation(contractName: String, contractType: String, contractId: String?, conversationId: String?): ChatbotConversation = error("unused")
        override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply = error("unused")
        override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply = error("unused")
    }

    @Test fun delayedResultAndEffectFromPreviousGenerationAreDiscarded() = runTest {
        val repository = Repository()
        val vm = ChatbotHistoryViewModel(repository)
        val effects = mutableListOf<Any>()
        val collector = backgroundScope.launch { vm.uiEffect.collect { effects.add(it) } }
        vm.onEvent(ChatbotHistoryUiEvent.ScreenOpened())
        testScheduler.runCurrent()
        repository.session.value = null
        testScheduler.runCurrent()
        repository.session.value = "B:2"
        testScheduler.runCurrent()
        repository.session.value = "A:3"
        testScheduler.runCurrent()
        repository.gate.complete(Unit)
        testScheduler.runCurrent()
        vm.onEvent(ChatbotHistoryUiEvent.BackClicked)
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.conversations.isEmpty())
        assertTrue(effects.isEmpty())
        collector.cancel()
    }
}
