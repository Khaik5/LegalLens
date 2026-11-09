package com.example.lagallens.presentation.feature.contracts.filter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilterSelection
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import com.example.lagallens.presentation.feature.contracts.contract.ContractStatusFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractType
import com.example.lagallens.presentation.feature.contracts.filter.contract.ContractFilterUiEffect
import com.example.lagallens.presentation.feature.contracts.filter.contract.ContractFilterUiEvent
import com.example.lagallens.presentation.feature.contracts.filter.contract.ContractFilterUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractFilterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ContractFilterUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ContractFilterUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: ContractFilterUiEvent) {
        when (event) {
            is ContractFilterUiEvent.Initialized -> _uiState.update { if (it.isInitialized) it else it.copy(selection = event.selection, isInitialized = true) }
            is ContractFilterUiEvent.ContractTypeToggled -> toggleContractType(event.contractType)
            is ContractFilterUiEvent.StatusSelected -> selectStatus(event.status)
            is ContractFilterUiEvent.RiskLevelSelected -> selectRiskLevel(event.riskLevel)
            ContractFilterUiEvent.ResetClicked -> resetFilters()
            ContractFilterUiEvent.ApplyClicked -> applyFilters()
        }
    }

    private fun toggleContractType(contractType: ContractType) {
        _uiState.update { state ->
            val updatedTypes = state.selection.contractTypes.toMutableSet().apply {
                if (!add(contractType)) remove(contractType)
            }
            state.copy(selection = state.selection.copy(contractTypes = updatedTypes))
        }
    }

    private fun selectStatus(status: ContractStatusFilter) {
        _uiState.update { state ->
            state.copy(selection = state.selection.copy(status = status))
        }
    }

    private fun selectRiskLevel(riskLevel: ContractRiskLevel) {
        _uiState.update { state ->
            state.copy(selection = state.selection.copy(riskLevel = riskLevel.takeUnless { it == state.selection.riskLevel }))
        }
    }

    private fun resetFilters() {
        _uiState.update { it.copy(selection = ContractFilterSelection()) }
    }

    private fun applyFilters() {
        viewModelScope.launch {
            _uiEffect.send(ContractFilterUiEffect.ApplyFilters(_uiState.value.selection))
        }
    }
}
