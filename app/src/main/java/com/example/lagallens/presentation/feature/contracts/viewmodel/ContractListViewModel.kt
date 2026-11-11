package com.example.lagallens.presentation.feature.contracts.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilterSelection
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEffect
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEvent
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiState
import com.example.lagallens.presentation.feature.contracts.contract.ContractProcessingStatus
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import com.example.lagallens.presentation.feature.contracts.contract.ContractStatusFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

import com.example.lagallens.presentation.feature.contracts.contract.labelRes
import com.example.lagallens.presentation.feature.contracts.contract.toContractListItem

class ContractListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ContractListUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var sourceContracts: List<ContractListItem> = emptyList()
    private var contractsJob: Job? = null

    init {
        observeContracts()
    }

    private fun observeContracts() {
        contractsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        contractsJob = viewModelScope.launch {
            try {
                getApplication<LegalLensApplication>().contractRepository.observeContracts().collect { contracts ->
                    sourceContracts = contracts.map(::toListItem)
                    updateContracts(isLoading = false, hasError = false)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                sourceContracts = emptyList()
                updateContracts(isLoading = false, hasError = true)
                _uiEffect.send(ContractListUiEffect.ShowMessage(R.string.dashboard_load_failed))
            }
        }
    }

    private fun toListItem(contract: EmploymentContract): ContractListItem =
        contract.toContractListItem(getApplication<Application>().getString(contract.type.labelRes()))

    fun onEvent(event: ContractListUiEvent) {
        when (event) {
            is ContractListUiEvent.QueryChanged -> updateContracts(query = event.query)
            is ContractListUiEvent.FilterSelected -> updateContracts(filter = event.filter)
            ContractListUiEvent.FilterClicked -> showFilterBottomSheet()
            ContractListUiEvent.RetryClicked -> if (_uiState.value.hasError) observeContracts()
            is ContractListUiEvent.FiltersApplied -> updateContracts(filterSelection = event.filterSelection)
            is ContractListUiEvent.ContractClicked -> navigateToDetail(event.contract.id)
        }
    }

    private fun updateContracts(
        query: String = _uiState.value.query,
        filter: ContractFilter = _uiState.value.selectedFilter,
        filterSelection: ContractFilterSelection = _uiState.value.filterSelection,
        isLoading: Boolean = _uiState.value.isLoading,
        hasError: Boolean = _uiState.value.hasError
    ) {
        _uiState.update {
            it.copy(
                query = query,
                selectedFilter = filter,
                filterSelection = filterSelection,
                isLoading = isLoading,
                hasError = hasError,
                contracts = sourceContracts.filter { item ->
                    item.matches(filter) && item.matches(filterSelection) && item.matches(query)
                }
            )
        }
    }

    private fun ContractListItem.matches(filter: ContractFilter): Boolean = when (filter) {
        ContractFilter.ALL -> true
        ContractFilter.ANALYZED -> processingStatus == ContractProcessingStatus.COMPLETE
        ContractFilter.PROCESSING -> processingStatus == ContractProcessingStatus.PROCESSING
        ContractFilter.HIGH_RISK -> riskLevel == ContractRiskLevel.HIGH || riskLevel == ContractRiskLevel.CRITICAL
    }

    private fun ContractListItem.matches(filterSelection: ContractFilterSelection): Boolean {
        val typeMatches = filterSelection.contractTypes.isEmpty() || contractType in filterSelection.contractTypes
        val statusMatches = when (filterSelection.status) {
            ContractStatusFilter.ALL -> true
            ContractStatusFilter.COMPLETE -> processingStatus == ContractProcessingStatus.COMPLETE
            ContractStatusFilter.PROCESSING -> processingStatus == ContractProcessingStatus.PROCESSING
            ContractStatusFilter.DRAFT -> processingStatus == ContractProcessingStatus.DRAFT
        }
        val riskMatches = filterSelection.riskLevel == null || riskLevel == filterSelection.riskLevel ||
            (filterSelection.riskLevel == ContractRiskLevel.HIGH && riskLevel == ContractRiskLevel.CRITICAL)
        return typeMatches && statusMatches && riskMatches
    }

    private fun ContractListItem.matches(query: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        val title = titleOverride ?: getApplication<Application>().getString(titleRes)
        return normalizedQuery.isBlank() || title.lowercase().contains(normalizedQuery)
    }

    private fun navigateToDetail(contractId: String) {
        val session = getApplication<LegalLensApplication>().currentSessionKey()
        viewModelScope.launch {
            if (session == getApplication<LegalLensApplication>().currentSessionKey()) _uiEffect.send(ContractListUiEffect.NavigateToDetail(contractId))
        }
    }

    private fun showFilterBottomSheet() {
        viewModelScope.launch {
            _uiEffect.send(ContractListUiEffect.ShowFilterBottomSheet)
        }
    }
}
