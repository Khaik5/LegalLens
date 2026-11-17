package com.example.lagallens.presentation.feature.creditstore.payments.contract

import androidx.annotation.StringRes
import com.example.lagallens.domain.model.payment.PaymentHistoryEntry

data class PaymentHistoryUiState(
    val isLoading: Boolean = true,
    val entries: List<PaymentHistoryEntry> = emptyList(),
    @param:StringRes val errorMessageRes: Int? = null
)
