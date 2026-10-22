package com.example.lagallens.presentation.feature.dashboard.contract

import androidx.annotation.StringRes

sealed interface HomeDashboardUiEffect {
    data object NavigateToUploadSource : HomeDashboardUiEffect
    data object NavigateToCamera : HomeDashboardUiEffect
    data object NavigateToContracts : HomeDashboardUiEffect
    data class NavigateToContractDetail(val contractId: String) : HomeDashboardUiEffect
    data class ShowMessage(@StringRes val titleRes: Int) : HomeDashboardUiEffect
}
