package com.example.lagallens.presentation.feature.creditstore.flow.utils

import androidx.annotation.StringRes
import com.example.lagallens.R
import com.example.lagallens.domain.model.payment.PaymentStatus

@StringRes
internal fun paymentRecoveryMessage(status: PaymentStatus?): Int = when (status) {
    PaymentStatus.FAILED, PaymentStatus.CANCELLED -> paymentFailureMessage(status, null, 0)
    else -> R.string.credit_flow_payment_unconfirmed
}

@StringRes
internal fun paymentFailureMessage(status: PaymentStatus?, expiresAtMillis: Long?, nowMillis: Long): Int =
    when {
        status == PaymentStatus.FAILED -> R.string.credit_flow_payment_failed
        expiresAtMillis != null && nowMillis >= expiresAtMillis -> R.string.credit_flow_payment_expired
        status == PaymentStatus.CANCELLED -> R.string.credit_flow_payment_cancelled
        else -> R.string.credit_flow_payment_failed
    }
