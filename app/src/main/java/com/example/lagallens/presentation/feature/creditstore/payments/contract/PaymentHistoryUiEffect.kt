package com.example.lagallens.presentation.feature.creditstore.payments.contract

sealed interface PaymentHistoryUiEffect {
    data object NavigateBack : PaymentHistoryUiEffect
}
