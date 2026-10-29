package com.example.lagallens.presentation.feature.compare.select.contract

data class CompareContract(val id: String, val title: String, val subtitle: String)

data class CompareSelectUiState(
    val contracts: List<CompareContract> = emptyList(),
    val selectedIds: List<String> = emptyList(),
    val initialContractId: String? = null,
    val query: String = "",
    val isInitialized: Boolean = false
)
