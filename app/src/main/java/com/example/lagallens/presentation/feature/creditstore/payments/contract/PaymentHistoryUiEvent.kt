package com.example.lagallens.presentation.feature.creditstore.payments.contract

sealed interface PaymentHistoryUiEvent {
    data object ScreenOpened : PaymentHistoryUiEvent
    data object RetryClicked : PaymentHistoryUiEvent
    data object BackClicked : PaymentHistoryUiEvent
}
