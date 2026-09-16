package com.example.lagallens.presentation.feature.contracts.filter.contract

import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import com.example.lagallens.presentation.feature.contracts.contract.ContractStatusFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractType

sealed interface ContractFilterUiEvent {
    data class ContractTypeToggled(val contractType: ContractType) : ContractFilterUiEvent
    data class StatusSelected(val status: ContractStatusFilter) : ContractFilterUiEvent
    data class RiskLevelSelected(val riskLevel: ContractRiskLevel) : ContractFilterUiEvent
    data object ResetClicked : ContractFilterUiEvent
    data object ApplyClicked : ContractFilterUiEvent
}
