package com.example.lagallens.presentation.feature.contracts.detail.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractAssistantAction
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ContractDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun failedDeleteReportsAnErrorAndKeepsTheIdForRetry() = runTest {
        val base = repository()
        var attempts = 0
        val failing = object : ContractRepository by base {
            override suspend fun deleteContract(contractId: String): Boolean {
                if (++attempts == 1) throw IOException("offline")
                return base.deleteContract(contractId)
            }
        }
        val vm = ContractDetailViewModel(failing)
        vm.loadContract("collaborator")
        vm.onEvent(ContractDetailUiEvent.DeleteConfirmed)
        assertEquals(ContractDetailUiEffect.ShowMessage(R.string.contract_action_failed), vm.uiEffect.first())

        vm.onEvent(ContractDetailUiEvent.DeleteConfirmed)
        assertEquals(ContractDetailUiEffect.NavigateBack, vm.uiEffect.first())
        assertEquals(2, attempts)
    }

    @Test fun failedRenameKeepsTheIdForRetry() = runTest {
        val base = repository()
        var attempts = 0
        val vm = ContractDetailViewModel(object : ContractRepository by base {
            override suspend fun renameContract(contractId: String, title: String): Boolean {
                if (++attempts == 1) throw IOException("offline")
                return base.renameContract(contractId, title)
            }
        })
        vm.loadContract("collaborator")
        vm.onEvent(ContractDetailUiEvent.RenameConfirmed("New title"))
        assertEquals(ContractDetailUiEffect.ShowMessage(R.string.contract_action_failed), vm.uiEffect.first())

        vm.onEvent(ContractDetailUiEvent.RenameConfirmed("New title"))
        testScheduler.runCurrent()
        assertEquals("New title", vm.uiState.value.renamedTitle)
        assertEquals(2, attempts)
    }

    @Test fun transientLoadFailureAllowsReloadingTheSameId() = runTest {
        val base = repository()
        var attempts = 0
        val vm = ContractDetailViewModel(object : ContractRepository by base {
            override suspend fun getContract(contractId: String): EmploymentContract? {
                if (++attempts == 1) throw IOException("offline")
                return base.getContract(contractId)
            }
        })
        vm.loadContract("collaborator")
        assertEquals(ContractDetailUiEffect.ShowMessage(R.string.contract_action_failed), vm.uiEffect.first())

        vm.loadContract("collaborator")
        testScheduler.runCurrent()
        assertEquals("collaborator", vm.uiState.value.contractId)
        assertEquals(2, attempts)
    }

    @Test fun forbiddenRenameClearsTheContractAndNavigatesBack() = runTest {
        val base = repository()
        val vm = ContractDetailViewModel(object : ContractRepository by base {
            override suspend fun renameContract(contractId: String, title: String): Boolean =
                throw HttpException(Response.error<Unit>(403, "".toResponseBody()))
        })
        vm.loadContract("collaborator")
        vm.onEvent(ContractDetailUiEvent.RenameConfirmed("New title"))

        assertEquals(ContractDetailUiEffect.NavigateBack, vm.uiEffect.first())
        assertTrue(vm.uiState.value.contractId.isBlank())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun oldSessionResultDoesNotPopulateNewSession() = runTest {
        val sessions = MutableStateFlow<String?>("A:1")
        val gate = CompletableDeferred<Unit>()
        val base = repository()
        val scoped = object : ContractRepository by base {
            override fun activeSessionKey() = sessions.value
            override fun observeSessionKeys(): Flow<String?> = sessions
            override suspend fun getContract(contractId: String): EmploymentContract? {
                gate.await()
                return base.getContract("collaborator")?.copy(id = contractId)
            }
        }
        val vm = ContractDetailViewModel(scoped)
        vm.loadContract("private-a")
        sessions.value = "B:2"
        testScheduler.runCurrent()
        gate.complete(Unit)
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.contractId.isBlank())
        vm.loadContract("private-b")
        testScheduler.runCurrent()
        assertEquals("private-b", vm.uiState.value.contractId)
    }

    @Test
    fun loadContractUsesTheRequestedKnownContract() {
        val viewModel = ContractDetailViewModel(repository())
        viewModel.loadContract("collaborator")
        assertEquals("Hợp đồng lao động DEMO", viewModel.uiState.value.renamedTitle)
        assertEquals("document-collaborator", viewModel.uiState.value.documentId)
    }

    @Test
    fun loadContractWithUnknownIdNavigatesBackInsteadOfShowingAnotherContract() = runTest {
        val viewModel = ContractDetailViewModel(repository())
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }
        viewModel.loadContract("missing-contract")
        assertEquals(ContractDetailUiEffect.NavigateBack, effect.await())
    }

    @Test fun analyzeCarriesTheLoadedContractIdentity() = runTest {
        val vm = ContractDetailViewModel(repository())
        vm.loadContract("collaborator")
        testScheduler.runCurrent()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { vm.uiEffect.first() }
        vm.onEvent(ContractDetailUiEvent.AssistantActionClicked(ContractAssistantAction.ANALYZE))
        val route = effect.await() as ContractDetailUiEffect.NavigateToAnalysis
        assertEquals("collaborator", route.contractId)
        assertEquals("document-collaborator", route.documentId)
    }

    private fun repository() = object : ContractRepository {
        private val contract = EmploymentContract(
            "collaborator", "document-collaborator", "Hợp đồng lao động DEMO", "ĐIỀU 1. Công việc\n[Mẫu]", 1L, 1L
        )
        override fun observeContracts(): Flow<List<EmploymentContract>> = emptyFlow()
        override suspend fun getContract(contractId: String) = contract.takeIf { it.id == contractId }
        override suspend fun createContract(contract: EmploymentContract) = contract
        override suspend fun updateContract(contract: EmploymentContract) = contract.id == this.contract.id
        override suspend fun renameContract(contractId: String, title: String) = contractId == contract.id
        override suspend fun deleteContract(contractId: String) = contractId == contract.id
        override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = null
    }
}
