package com.example.lagallens.presentation.feature.dashboard.contract

import androidx.annotation.StringRes
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem

data class HomeDashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val totalContracts: Int = 0,
    val analyzedContracts: Int = 0,
    val reviewContracts: Int = 0,
    val recentContracts: List<DashboardRecentContract> = emptyList(),
    @param:StringRes val errorMessageRes: Int? = null
)

data class DashboardRecentContract(
    val contract: ContractListItem,
    val updatedAtMillis: Long
)
