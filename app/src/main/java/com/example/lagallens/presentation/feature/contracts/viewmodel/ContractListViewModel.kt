package com.example.lagallens.presentation.feature.contracts.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilterSelection
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEffect
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEvent
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiState
import com.example.lagallens.presentation.feature.contracts.contract.ContractProcessingStatus
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import com.example.lagallens.presentation.feature.contracts.contract.ContractStatusFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ContractListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ContractListUiEvent) {
        when (event) {
            is ContractListUiEvent.QueryChanged -> updateContracts(query = event.query)
            is ContractListUiEvent.FilterSelected -> updateContracts(filter = event.filter)
            ContractListUiEvent.FilterClicked -> showFilterBottomSheet()
            is ContractListUiEvent.FiltersApplied -> updateContracts(filterSelection = event.filterSelection)
            is ContractListUiEvent.ContractClicked -> navigateToDetail(event.contract.id)
        }
    }

    private fun updateContracts(
        query: String = _uiState.value.query,
        filter: ContractFilter = _uiState.value.selectedFilter,
        filterSelection: ContractFilterSelection = _uiState.value.filterSelection
    ) {
        _uiState.update {
            it.copy(
                query = query,
                selectedFilter = filter,
                filterSelection = filterSelection,
                contracts = ContractListItem.samples.filter { item ->
                    item.matches(filter) && item.matches(filterSelection) && item.matches(query)
                }
            )
        }
    }

    private fun ContractListItem.matches(filter: ContractFilter): Boolean = when (filter) {
        ContractFilter.ALL -> true
        ContractFilter.ANALYZED -> processingStatus == ContractProcessingStatus.COMPLETE
        ContractFilter.PROCESSING -> processingStatus == ContractProcessingStatus.PROCESSING
        ContractFilter.HIGH_RISK -> riskLevel == ContractRiskLevel.HIGH
    }

    private fun ContractListItem.matches(filterSelection: ContractFilterSelection): Boolean {
        val typeMatches = filterSelection.contractTypes.isEmpty() || contractType in filterSelection.contractTypes
        val statusMatches = when (filterSelection.status) {
            ContractStatusFilter.ALL -> true
            ContractStatusFilter.COMPLETE -> processingStatus == ContractProcessingStatus.COMPLETE
            ContractStatusFilter.PROCESSING -> processingStatus == ContractProcessingStatus.PROCESSING
            ContractStatusFilter.DRAFT -> processingStatus == ContractProcessingStatus.DRAFT
        }
        val riskMatches = filterSelection.riskLevel == null || riskLevel == filterSelection.riskLevel
        return typeMatches && statusMatches && riskMatches
    }

    private fun ContractListItem.matches(query: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        return normalizedQuery.isBlank() || getApplication<Application>()
            .getString(titleRes)
            .lowercase()
            .contains(normalizedQuery)
    }

    private fun navigateToDetail(contractId: String) {
        viewModelScope.launch {
            _uiEffect.emit(ContractListUiEffect.NavigateToDetail(contractId))
        }
    }

    private fun showFilterBottomSheet() {
        viewModelScope.launch {
            _uiEffect.emit(ContractListUiEffect.ShowFilterBottomSheet)
        }
    }
}
