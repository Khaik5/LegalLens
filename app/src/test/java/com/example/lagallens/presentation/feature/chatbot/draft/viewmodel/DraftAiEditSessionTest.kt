package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiEditUiEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DraftAiEditSessionTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    @Test fun proposalRequestCarriesEveryIdAndTheProposalIsKept() = runTest {
        val session = MutableStateFlow<String?>("a:1")
        val ai = com.example.lagallens.TestDraftAiRepository()
        val vm = ChatbotDraftAiEditViewModel("a:1", { session.value }, session) { ai }
        vm.onEvent(ChatbotDraftAiEditUiEvent.Opened("scope", "Extend term", false, "draft-a", "contract-a", "conversation-a"))
        advanceUntilIdle()
        val request = ai.editRequests.single()
        org.junit.Assert.assertEquals("conversation-a", request.conversationId)
        org.junit.Assert.assertEquals("contract-a", request.contractId)
        org.junit.Assert.assertEquals("draft-a", request.draftId)
        org.junit.Assert.assertEquals("Extend term", request.instruction)
        org.junit.Assert.assertEquals("proposal-1", vm.uiState.value.proposal?.proposalId)
    }

    @Test fun delayedCallbackDoesNotRestoreOldRequestAfterSessionChange() = runTest {
        val session = MutableStateFlow<String?>("a:1")
        val vm = ChatbotDraftAiEditViewModel("a:1", { session.value }, session) { com.example.lagallens.TestDraftAiRepository() }
        vm.onEvent(ChatbotDraftAiEditUiEvent.Opened("scope", "A private", false))
        session.value = "b:2"
        session.value = "a:3"
        advanceUntilIdle()
        assertEquals("", vm.uiState.value.request)
        assertFalse(vm.uiState.value.isInitialized)
    }
}
