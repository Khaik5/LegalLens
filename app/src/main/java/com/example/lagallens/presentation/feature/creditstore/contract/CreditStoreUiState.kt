package com.example.lagallens.presentation.feature.creditstore.contract

data class CreditStoreUiState(
    val balanceCredits: Int = 5,
    val isBackendUnavailable: Boolean = false,
    val isBalanceUnavailable: Boolean = false,
    val isLoading: Boolean = true,
    val packages: List<CreditPackage> = emptyList()
)

data class CreditPackage(
    val id: String,
    val credits: Int,
    val priceVnd: Long,
    val title: String,
    val description: String,
    val isSelected: Boolean = false
)
