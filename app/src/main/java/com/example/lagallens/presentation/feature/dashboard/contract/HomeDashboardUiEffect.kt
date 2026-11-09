package com.example.lagallens.presentation.feature.dashboard.contract

sealed interface HomeDashboardUiEffect {
    data object NavigateToUploadSource : HomeDashboardUiEffect
    data object NavigateToCamera : HomeDashboardUiEffect
    data object NavigateToCompare : HomeDashboardUiEffect
    data object NavigateToContracts : HomeDashboardUiEffect
    data class NavigateToContractDetail(val contractId: String) : HomeDashboardUiEffect
}
