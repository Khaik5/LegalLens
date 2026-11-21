package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiEffect
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftListUiEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatbotDraftListViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private fun version(draft: Long, version: Int, title: String, updatedAt: Long, saved: Boolean = false, contractId: String? = "7") = Draft(
        draftId = "$draft:$version", contractId = contractId, title = title, content = "", stage = DraftStage.ORIGINAL,
        versionNo = version, createdAt = 0L, updatedAt = updatedAt, lineageId = draft.toString(), isSaved = saved
    )

    private class ListRepository(var result: () -> List<Draft>) : DraftRepository {
        override suspend fun getDrafts() = result()
        override suspend fun getDraft(draftId: String): Draft? = null
        override suspend fun getLatestDraft(stage: DraftStage): Draft? = null
        override suspend fun saveDraft(
            stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?,
            conversationId: String?, isSaved: Boolean, isTitleGenerated: Boolean
        ): Draft = error("not used")
        override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage, isTitleGenerated: Boolean): Draft? = null
        override suspend fun updateDraft(draftId: String, title: String, content: String): Draft? = null
        override suspend fun deleteDraft(draftId: String) = false
    }

    private fun viewModel(repository: ListRepository, session: () -> String? = { "a:1" }) =
        ChatbotDraftListViewModel("a:1", session) { repository }

    @Test fun versionsOfOneDraftBecomeOneRowNamedByTheNewestVersion() = runTest {
        val repository = ListRepository {
            listOf(
                version(1, 1, "First", 100), version(1, 2, "First, edited", 300, saved = true),
                version(2, 1, "Other", 200)
            )
        }
        val vm = viewModel(repository)
        vm.onEvent(ChatbotDraftListUiEvent.Opened)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ChatbotDraftListStatus.CONTENT, state.status)
        assertEquals(listOf("First, edited", "Other"), state.items.map { it.title })
        assertEquals(listOf("1:2", "2:1"), state.items.map { it.draftId })
        assertEquals(2, state.items.first().versionCount)
        assertTrue(state.items.first().isSaved)
    }

    @Test fun noDraftsIsEmptyAndAFailedLoadIsAnErrorThatRetryRecovers() = runTest {
        var failing = true
        val repository = ListRepository { if (failing) error("offline") else emptyList() }
        val vm = viewModel(repository)
        vm.onEvent(ChatbotDraftListUiEvent.Opened)
        advanceUntilIdle()
        assertEquals(ChatbotDraftListStatus.ERROR, vm.uiState.value.status)

        failing = false
        vm.onEvent(ChatbotDraftListUiEvent.RetryClicked)
        advanceUntilIdle()
        assertEquals(ChatbotDraftListStatus.EMPTY, vm.uiState.value.status)
    }

    @Test fun aFailedRefreshKeepsTheListAlreadyShown() = runTest {
        var failing = false
        val repository = ListRepository { if (failing) error("offline") else listOf(version(1, 1, "Kept", 100)) }
        val vm = viewModel(repository)
        vm.onEvent(ChatbotDraftListUiEvent.Opened)
        advanceUntilIdle()
        failing = true
        vm.onEvent(ChatbotDraftListUiEvent.Opened)
        advanceUntilIdle()

        assertEquals(ChatbotDraftListStatus.CONTENT, vm.uiState.value.status)
        assertEquals(listOf("Kept"), vm.uiState.value.items.map { it.title })
    }

    @Test fun clickingARowOpensItsNewestVersion() = runTest {
        val repository = ListRepository { listOf(version(1, 1, "A", 100), version(1, 3, "A3", 300, contractId = "9")) }
        val vm = viewModel(repository)
        vm.onEvent(ChatbotDraftListUiEvent.Opened)
        advanceUntilIdle()

        val received = mutableListOf<ChatbotDraftListUiEffect>()
        val collector = launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) { vm.uiEffect.collect { received += it } }
        vm.onEvent(ChatbotDraftListUiEvent.ItemClicked(vm.uiState.value.items.single()))
        advanceUntilIdle()
        collector.cancel()

        assertEquals(listOf<ChatbotDraftListUiEffect>(ChatbotDraftListUiEffect.OpenDraft("1:3", "9")), received)
    }

    @Test fun aChangedSessionNeitherLoadsNorShowsAnotherUsersDrafts() = runTest {
        var current: String? = "a:1"
        val repository = ListRepository { listOf(version(1, 1, "Private", 100)) }
        val vm = viewModel(repository) { current }
        current = "b:2"
        vm.onEvent(ChatbotDraftListUiEvent.Opened)
        advanceUntilIdle()

        assertEquals(emptyList<Any>(), vm.uiState.value.items)
    }
}
