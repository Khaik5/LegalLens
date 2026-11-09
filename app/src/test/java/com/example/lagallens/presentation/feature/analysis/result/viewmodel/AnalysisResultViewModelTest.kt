package com.example.lagallens.presentation.feature.analysis.result.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.domain.usecase.GetAnalysisOverviewUseCase
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AnalysisResultViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private class Identities : AnalysisIdentityRepository {
        var requested = ""
        override fun sessionKey() = "account:session"
        override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String) = error("Unexpected create")
        override suspend fun get(resultId: String, sessionKey: String): AnalysisIdentity? {
            requested = resultId
            return if (resultId == "result") AnalysisIdentity(resultId, "op", "contract", "document") else null
        }
        override suspend fun latest(contractId: String, sessionKey: String): AnalysisIdentity? = null
    }
    @Test fun loadsOnlyTheRequestedResult() {
        val ids = Identities()
        var received: AnalysisIdentity? = null
        val source = object : com.example.lagallens.TestAnalysisRepository() {
            override suspend fun getOverview(identity: AnalysisIdentity): AnalysisOverview {
                received = identity
                return AnalysisOverview(65, 8, 3, 0)
            }
        }
        val vm = AnalysisResultViewModel(GetAnalysisOverviewUseCase(source), ids, com.example.lagallens.TestUnlockRepository())
        vm.onEvent(AnalysisResultUiEvent.ScreenOpened("file", "details", "result", "account:session"))
        assertEquals("result", ids.requested)
        assertEquals("result", received?.resultId)
        assertEquals(65, vm.uiState.value.score)
    }
    @Test fun lockedResultOpensSummaryPreview() = kotlinx.coroutines.test.runTest {
        val source = object : com.example.lagallens.TestAnalysisRepository() {
            override suspend fun getOverview(identity: AnalysisIdentity) = AnalysisOverview(65, 8, 3, 0)
        }
        val vm = AnalysisResultViewModel(GetAnalysisOverviewUseCase(source), Identities(), com.example.lagallens.TestUnlockRepository(unlocked = false, cost = 2))
        vm.onEvent(AnalysisResultUiEvent.ScreenOpened("file", "details", "result", "account:session"))
        val effect = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { vm.uiEffect.first() }
        vm.onEvent(AnalysisResultUiEvent.ItemClicked(com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultDestination.SUMMARY))
        assertTrue(effect.await() is com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEffect.NavigateToSummary)
    }

    @Test fun lockedResultAllowsLegalReferencesPreview() = kotlinx.coroutines.test.runTest {
        val source = object : com.example.lagallens.TestAnalysisRepository() {
            override suspend fun getOverview(identity: AnalysisIdentity) = AnalysisOverview(65, 8, 3, 0)
        }
        val vm = AnalysisResultViewModel(GetAnalysisOverviewUseCase(source), Identities(), com.example.lagallens.TestUnlockRepository(unlocked = false, cost = 2))
        vm.onEvent(AnalysisResultUiEvent.ScreenOpened("file", "details", "result", "account:session"))
        val effect = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { vm.uiEffect.first() }
        vm.onEvent(AnalysisResultUiEvent.ItemClicked(com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultDestination.LEGAL_REFERENCES))
        assertTrue(effect.await() is com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEffect.NavigateToLegalReferences)
    }

    @Test fun unlockedResultOpensPaidContent() = kotlinx.coroutines.test.runTest {
        val source = object : com.example.lagallens.TestAnalysisRepository() {
            override suspend fun getOverview(identity: AnalysisIdentity) = AnalysisOverview(65, 8, 3, 0)
        }
        val vm = AnalysisResultViewModel(GetAnalysisOverviewUseCase(source), Identities(), com.example.lagallens.TestUnlockRepository(unlocked = true))
        vm.onEvent(AnalysisResultUiEvent.ScreenOpened("file", "details", "result", "account:session"))
        val effect = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { vm.uiEffect.first() }
        vm.onEvent(AnalysisResultUiEvent.ItemClicked(com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultDestination.SUMMARY))
        assertTrue(effect.await() is com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEffect.NavigateToSummary)
    }

    @Test fun missingResultDoesNotShowFixture() {
        val ids = Identities()
        val vm = AnalysisResultViewModel(GetAnalysisOverviewUseCase(object : com.example.lagallens.TestAnalysisRepository() { override suspend fun getOverview(identity: AnalysisIdentity): AnalysisOverview = error("Must not load fixture") }), ids, com.example.lagallens.TestUnlockRepository())
        vm.onEvent(AnalysisResultUiEvent.ScreenOpened("file", "details", "missing", "account:session"))
        assertTrue(vm.uiState.value.items.isEmpty())
        assertNotNull(vm.uiState.value.errorMessageRes)
    }
    @org.junit.Test fun staleSessionCannotPublishLoadedResult() = kotlinx.coroutines.test.runTest {
        var session = "a:1"
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val ids = object : AnalysisIdentityRepository {
            override fun sessionKey() = session
            override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String): AnalysisIdentity = error("unexpected")
            override suspend fun get(resultId: String, sessionKey: String): AnalysisIdentity { gate.await(); return AnalysisIdentity(resultId, "op", "c", "d") }
            override suspend fun latest(contractId: String, sessionKey: String): AnalysisIdentity? = null
        }
        val source = object : com.example.lagallens.TestAnalysisRepository() { override suspend fun getOverview(identity: AnalysisIdentity) = AnalysisOverview(65, 8, 3, 0) }
        val vm = AnalysisResultViewModel(GetAnalysisOverviewUseCase(source), ids, com.example.lagallens.TestUnlockRepository())
        vm.onEvent(AnalysisResultUiEvent.ScreenOpened("private", "details", "result", "a:1"))
        testScheduler.runCurrent()
        session = "b:2"
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.items.isEmpty())
        assertEquals("", vm.uiState.value.documentName)
    }
}
