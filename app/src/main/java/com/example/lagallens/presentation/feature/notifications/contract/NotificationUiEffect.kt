package com.example.lagallens.presentation.feature.notifications.contract

import androidx.annotation.StringRes

sealed interface NotificationUiEffect {
    data class NavigateToContractDetail(val contractId: String) : NotificationUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : NotificationUiEffect
}
