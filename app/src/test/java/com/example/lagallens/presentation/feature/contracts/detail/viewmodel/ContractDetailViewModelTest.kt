package com.example.lagallens.presentation.feature.contracts.detail.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEffect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
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
        val viewModel = ContractDetailViewModel()

        viewModel.loadContract("collaborator")

        assertEquals(R.string.contract_collaborator, viewModel.uiState.value.titleRes)
    }

    @Test
    fun loadContractWithUnknownIdNavigatesBackInsteadOfShowingAnotherContract() = runTest {
        val viewModel = ContractDetailViewModel()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.loadContract("missing-contract")

        assertEquals(ContractDetailUiEffect.NavigateBack, effect.await())
    }
}
