package com.example.lagallens.presentation.feature.contracts.detail.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEffect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContractDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadContractUsesTheRequestedKnownContract() {
        val viewModel = ContractDetailViewModel(repository())
        viewModel.loadContract("collaborator")
        assertEquals(R.string.contract_demo_labor_fixed_term, viewModel.uiState.value.titleRes)
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
