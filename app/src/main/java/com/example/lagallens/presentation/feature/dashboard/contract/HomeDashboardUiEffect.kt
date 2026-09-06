package com.example.lagallens.presentation.feature.dashboard.contract

import androidx.annotation.StringRes

sealed interface HomeDashboardUiEffect {
    data class ShowMessage(@StringRes val titleRes: Int) : HomeDashboardUiEffect
}
