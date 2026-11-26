package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.model.SaveDraftAsContractResult
import com.example.lagallens.domain.repository.DraftRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SavedDraftCopyDetailViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private class ContractRepository(var result: suspend (String) -> SaveDraftAsContractResult) : DraftRepository {
        val requested = mutableListOf<String>()
        override suspend fun saveAsContract(draftId: String): SaveDraftAsContractResult {
            requested += draftId
            return result(draftId)
        }
        override suspend fun getDrafts(): List<Draft> = emptyList()
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

    private fun viewModel(repository: ContractRepository, session: () -> String? = { "a:1" }) =
        SavedDraftCopyDetailViewModel("a:1", session) { repository }

    @Test fun savingMakesTheContractAndReportsIt() = runTest {
        val repository = ContractRepository { SaveDraftAsContractResult.Saved("42") }
        val vm = viewModel(repository)
        val effects = mutableListOf<SavedDraftCopyDetailUiEffect>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiEffect.collect { effects += it } }

        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        collector.cancel()

        assertEquals(listOf("5:3"), repository.requested)
        assertEquals(SaveAsContractStatus.SAVED, vm.uiState.value.saveAsContractStatus)
        assertEquals(listOf<SavedDraftCopyDetailUiEffect>(SavedDraftCopyDetailUiEffect.ShowMessage(R.string.draft_saved_contract_success)), effects)
    }

    @Test fun backAfterSavingGoesToTheContractListAndOtherwiseCloses() = runTest {
        val vm = viewModel(ContractRepository { SaveDraftAsContractResult.Saved("42") })
        val effects = mutableListOf<SavedDraftCopyDetailUiEffect>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiEffect.collect { effects += it } }

        vm.onEvent(SavedDraftCopyDetailUiEvent.BackClicked)
        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        vm.onEvent(SavedDraftCopyDetailUiEvent.BackClicked)
        advanceUntilIdle()
        collector.cancel()

        assertEquals(SavedDraftCopyDetailUiEffect.Close, effects.first())
        assertEquals(SavedDraftCopyDetailUiEffect.NavigateToContracts, effects.last())
    }

    @Test fun aFailureAllowsTryingAgain() = runTest {
        var failing = true
        val repository = ContractRepository { if (failing) error("offline") else SaveDraftAsContractResult.Saved("42") }
        val vm = viewModel(repository)
        val effects = mutableListOf<SavedDraftCopyDetailUiEffect>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiEffect.collect { effects += it } }

        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        assertEquals(SaveAsContractStatus.IDLE, vm.uiState.value.saveAsContractStatus)
        assertEquals(SavedDraftCopyDetailUiEffect.ShowMessage(R.string.draft_saved_contract_failed), effects.single())

        failing = false
        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        collector.cancel()
        assertEquals(SaveAsContractStatus.SAVED, vm.uiState.value.saveAsContractStatus)
        assertEquals(2, repository.requested.size)
    }


    @Test fun aSourceThatCannotSaveTurnsTheActionOffInsteadOfInvitingRetries() = runTest {
        val vm = viewModel(ContractRepository { SaveDraftAsContractResult.Unsupported })
        val effects = mutableListOf<SavedDraftCopyDetailUiEffect>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiEffect.collect { effects += it } }

        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        collector.cancel()

        assertEquals(SaveAsContractStatus.UNAVAILABLE, vm.uiState.value.saveAsContractStatus)
        assertEquals(listOf<SavedDraftCopyDetailUiEffect>(SavedDraftCopyDetailUiEffect.ShowMessage(R.string.draft_saved_contract_unavailable)), effects)
    }

    @Test fun aDraftTheBackendRefusesTurnsTheActionOffAndSaysWhy() = runTest {
        val repository = ContractRepository { SaveDraftAsContractResult.Rejected }
        val vm = viewModel(repository)
        val effects = mutableListOf<SavedDraftCopyDetailUiEffect>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiEffect.collect { effects += it } }

        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        collector.cancel()

        assertEquals(SaveAsContractStatus.UNAVAILABLE, vm.uiState.value.saveAsContractStatus)
        assertEquals(listOf<SavedDraftCopyDetailUiEffect>(SavedDraftCopyDetailUiEffect.ShowMessage(R.string.draft_saved_contract_rejected)), effects)
        assertEquals(1, repository.requested.size)
    }

    @Test fun tappingWhileSavingDoesNotSendASecondRequest() = runTest {
        val gate = CompletableDeferred<SaveDraftAsContractResult>()
        val repository = ContractRepository { gate.await() }
        val vm = viewModel(repository)

        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()
        assertEquals(SaveAsContractStatus.SAVING, vm.uiState.value.saveAsContractStatus)
        assertEquals(1, repository.requested.size)

        gate.complete(SaveDraftAsContractResult.Saved("42"))
        advanceUntilIdle()
        assertEquals(SaveAsContractStatus.SAVED, vm.uiState.value.saveAsContractStatus)
    }

    @Test fun anotherSessionNeitherSavesNorChangesTheState() = runTest {
        var current: String? = "a:1"
        val repository = ContractRepository { SaveDraftAsContractResult.Saved("42") }
        val vm = viewModel(repository) { current }
        current = "b:2"

        vm.onEvent(SavedDraftCopyDetailUiEvent.SaveAsContractClicked("5:3"))
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.requested)
        assertEquals(SaveAsContractStatus.IDLE, vm.uiState.value.saveAsContractStatus)
    }
}
