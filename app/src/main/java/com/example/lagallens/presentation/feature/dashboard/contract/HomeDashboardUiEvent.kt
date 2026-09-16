package com.example.lagallens.presentation.feature.dashboard.contract

import androidx.annotation.StringRes

sealed interface HomeDashboardUiEvent {
    data class ShortcutClicked(@StringRes val titleRes: Int) : HomeDashboardUiEvent
    data object ViewAllContractsClicked : HomeDashboardUiEvent
    data object ContractClicked : HomeDashboardUiEvent
}
