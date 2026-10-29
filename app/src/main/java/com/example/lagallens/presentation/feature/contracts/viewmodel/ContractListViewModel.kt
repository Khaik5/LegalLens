package com.example.lagallens.presentation.feature.contracts.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
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
import kotlinx.coroutines.CancellationException

class ContractListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ContractListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                (application as LegalLensApplication).contractRepository.observeContracts().collect { contracts ->
                    sourceContracts = contracts.map(::toListItem)
                    updateContracts()
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                sourceContracts = emptyList()
                updateContracts()
            }
        }
    }

    private var sourceContracts: List<ContractListItem> = emptyList()

    private fun toListItem(contract: EmploymentContract): ContractListItem {
        val template = ContractListItem.allSamples.firstOrNull { it.id == contract.id }
            ?: ContractListItem.allSamples.first()
        val application = getApplication<Application>()
        val status = when (contract.status) {
            EmploymentContractStatus.COMPLETE -> ContractProcessingStatus.COMPLETE
            EmploymentContractStatus.PROCESSING -> ContractProcessingStatus.PROCESSING
            EmploymentContractStatus.DRAFT -> ContractProcessingStatus.DRAFT
        }
        val risk = when (contract.risk) {
            EmploymentContractRisk.LOW -> ContractRiskLevel.LOW
            EmploymentContractRisk.MEDIUM -> ContractRiskLevel.MEDIUM
            EmploymentContractRisk.HIGH -> ContractRiskLevel.HIGH
            EmploymentContractRisk.CRITICAL -> ContractRiskLevel.CRITICAL
            EmploymentContractRisk.NOT_ANALYZED -> ContractRiskLevel.NOT_ANALYZED
        }
        val statusRes = when (status) {
            ContractProcessingStatus.COMPLETE -> com.example.lagallens.R.string.contract_complete_status
            ContractProcessingStatus.PROCESSING -> com.example.lagallens.R.string.contract_processing_status
            ContractProcessingStatus.DRAFT -> com.example.lagallens.R.string.contract_draft_status
        }
        val riskRes = when (risk) {
            ContractRiskLevel.LOW -> com.example.lagallens.R.string.contract_low_risk
            ContractRiskLevel.MEDIUM -> com.example.lagallens.R.string.contract_medium_risk
            ContractRiskLevel.HIGH, ContractRiskLevel.CRITICAL -> com.example.lagallens.R.string.contract_high_risk
            ContractRiskLevel.NOT_ANALYZED -> com.example.lagallens.R.string.contract_not_analyzed_risk
        }
        val (statusBackgroundRes, statusTextColorRes) = when (status) {
            ContractProcessingStatus.COMPLETE -> com.example.lagallens.R.drawable.bg_contract_status_complete to com.example.lagallens.R.color.contract_complete
            ContractProcessingStatus.PROCESSING -> com.example.lagallens.R.drawable.bg_contract_status_processing to com.example.lagallens.R.color.contract_pending
            ContractProcessingStatus.DRAFT -> com.example.lagallens.R.drawable.bg_contract_status_draft to com.example.lagallens.R.color.legal_lens_auth_accent
        }
        val riskIconRes = when (risk) {
            ContractRiskLevel.HIGH, ContractRiskLevel.CRITICAL -> com.example.lagallens.R.drawable.ic_dashboard_risk_high
            ContractRiskLevel.NOT_ANALYZED -> com.example.lagallens.R.drawable.ic_contract_risk_pending
            else -> com.example.lagallens.R.drawable.ic_dashboard_risk_low
        }
        val riskTextColorRes = when (risk) {
            ContractRiskLevel.HIGH, ContractRiskLevel.CRITICAL -> com.example.lagallens.R.color.contract_risk_high
            ContractRiskLevel.NOT_ANALYZED -> com.example.lagallens.R.color.legal_lens_secondary_text
            else -> com.example.lagallens.R.color.contract_complete
        }
        return template.copy(
            id = contract.id,
            titleOverride = contract.title,
            subtitleOverride = application.getString(com.example.lagallens.R.string.contract_demo_labor_subtitle),
            documentId = contract.documentId,
            content = contract.content,
            contractType = com.example.lagallens.presentation.feature.contracts.contract.ContractType.EMPLOYMENT,
            processingStatus = status,
            riskLevel = risk,
            statusRes = statusRes,
            riskRes = riskRes,
            statusBackgroundRes = statusBackgroundRes,
            statusTextColorRes = statusTextColorRes,
            riskIconRes = riskIconRes,
            riskTextColorRes = riskTextColorRes
        )
    }

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
        val title = titleOverride ?: getApplication<Application>().getString(titleRes)
        return normalizedQuery.isBlank() || title.lowercase().contains(normalizedQuery)
    }

    private fun navigateToDetail(contractId: String) {
        val session = getApplication<LegalLensApplication>().currentDemoSessionKey()
        viewModelScope.launch {
            if (session == getApplication<LegalLensApplication>().currentDemoSessionKey()) _uiEffect.emit(ContractListUiEffect.NavigateToDetail(contractId))
        }
    }

    private fun showFilterBottomSheet() {
        viewModelScope.launch {
            _uiEffect.emit(ContractListUiEffect.ShowFilterBottomSheet)
        }
    }
}
