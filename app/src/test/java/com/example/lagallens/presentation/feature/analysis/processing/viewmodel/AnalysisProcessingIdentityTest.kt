package com.example.lagallens.presentation.feature.analysis.processing.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.presentation.feature.analysis.processing.contract.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AnalysisProcessingIdentityTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private class Repo : AnalysisIdentityRepository {
        var session = "a:1"
        var calls = 0
        var fail = false
        override fun sessionKey() = session
        override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String): AnalysisIdentity {
            calls++
            check(!fail)
            return AnalysisIdentity("result", operationId, contractId, documentId)
        }
        override suspend fun get(resultId: String, sessionKey: String): AnalysisIdentity? = null
        override suspend fun latest(contractId: String, sessionKey: String): AnalysisIdentity? = null
    }
    private fun event() = AnalysisProcessingUiEvent.ScreenOpened("same", "demo", "op", "contract", "document", "a:1")
    @Test fun createsOnceAndReturnsRepositoryId() = runTest {
        val repo = Repo(); val vm = AnalysisProcessingViewModel(repo, com.example.lagallens.data.repository.FakeAnalysisRepository(repo))
        vm.onEvent(event()); vm.onEvent(event()); testScheduler.advanceUntilIdle()
        assertEquals(1, repo.calls)
        val completion = AnalysisProcessingUiEffect.NavigateToAnalysisResult("same", "demo", "result", "a:1")
        assertEquals(completion, vm.uiState.value.completion)
        vm.onEvent(event()) // The recreated Activity sends ScreenOpened again.
        assertEquals(completion, vm.uiState.value.completion)
        assertEquals(1, repo.calls)
        vm.consumeCompletion(completion)
        assertNull(vm.uiState.value.completion)
        vm.onEvent(event())
        assertNull(vm.uiState.value.completion)
        assertEquals(1, repo.calls)
    }
    @Test fun failedWriteRoutesToFailure() = runTest {
        val repo = Repo().apply { fail = true }; val vm = AnalysisProcessingViewModel(repo, com.example.lagallens.data.repository.FakeAnalysisRepository(repo))
        vm.onEvent(event()); testScheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.completion is AnalysisProcessingUiEffect.NavigateToAnalysisFailure)
    }
    @Test fun sessionChangePreventsCreationAndNavigation() = runTest {
        val repo = Repo(); val vm = AnalysisProcessingViewModel(repo, com.example.lagallens.data.repository.FakeAnalysisRepository(repo))
        vm.onEvent(event()); repo.session = "b:2"; testScheduler.advanceUntilIdle()
        assertEquals(0, repo.calls); assertNull(vm.uiState.value.completion)
    }

    @Test fun invalidationClearsAnUnconsumedCompletion() = runTest {
        val repo = Repo(); val vm = AnalysisProcessingViewModel(repo, com.example.lagallens.data.repository.FakeAnalysisRepository(repo))
        vm.onEvent(event()); testScheduler.advanceUntilIdle()
        assertNotNull(vm.uiState.value.completion)
        repo.session = "b:2"; vm.invalidateSession()
        assertNull(vm.uiState.value.completion)
        assertEquals("", vm.uiState.value.documentName)
    }
}
