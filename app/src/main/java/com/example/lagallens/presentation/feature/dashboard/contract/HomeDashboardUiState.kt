package com.example.lagallens.presentation.feature.dashboard.contract

import androidx.annotation.StringRes
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem

data class HomeDashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val photoUri: String? = null,
    val totalContracts: Int = 0,
    val analyzedContracts: Int = 0,
    val reviewContracts: Int = 0,
    val recentContracts: List<DashboardRecentContract> = emptyList(),
    val upcomingDates: List<DashboardUpcomingDate> = emptyList(),
    @param:StringRes val errorMessageRes: Int? = null
)

/** A contract ending within the coming weeks; [daysLeft] is 0 on the day it ends. */
data class DashboardUpcomingDate(
    val contractId: String,
    val title: String,
    val endDate: java.time.LocalDate,
    val daysLeft: Int
)

data class DashboardRecentContract(
    val contract: ContractListItem,
    val updatedAtMillis: Long
)
