package com.example.lagallens.presentation.feature.creditstore.flow.contract

import com.example.lagallens.domain.model.payment.BankTransfer

data class CreditFlowUiState(
    val isPaymentRequesting: Boolean = false,
    val isBackendUnavailable: Boolean = false,
    /** Set once the backend gave transfer instructions for the pending payment. */
    val bankTransfer: BankTransfer? = null,
    /** The payment exists but the transfer code could not be loaded. */
    val isBankTransferUnavailable: Boolean = false
)
