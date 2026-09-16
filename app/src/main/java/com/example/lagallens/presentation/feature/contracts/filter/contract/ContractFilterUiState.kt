package com.example.lagallens.presentation.feature.contracts.filter.contract

import com.example.lagallens.presentation.feature.contracts.contract.ContractFilterSelection
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import com.example.lagallens.presentation.feature.contracts.contract.ContractStatusFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractType

data class ContractFilterUiState(
    val selection: ContractFilterSelection = ContractFilterSelection(
        contractTypes = setOf(ContractType.SERVICE),
        status = ContractStatusFilter.COMPLETE,
        riskLevel = ContractRiskLevel.HIGH
    )
)
