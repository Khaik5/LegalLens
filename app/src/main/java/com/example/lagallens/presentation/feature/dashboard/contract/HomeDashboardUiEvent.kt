package com.example.lagallens.presentation.feature.dashboard.contract

sealed interface HomeDashboardUiEvent {
    data object UploadClicked : HomeDashboardUiEvent
    data object CameraClicked : HomeDashboardUiEvent
    data object AnalyzeClicked : HomeDashboardUiEvent
    data object CompareClicked : HomeDashboardUiEvent
    data object ViewAllContractsClicked : HomeDashboardUiEvent
    data class ContractClicked(val contractId: String) : HomeDashboardUiEvent
}
