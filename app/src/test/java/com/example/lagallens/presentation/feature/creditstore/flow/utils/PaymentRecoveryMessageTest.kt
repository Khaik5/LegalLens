package com.example.lagallens.presentation.feature.creditstore.flow.utils

import com.example.lagallens.R
import com.example.lagallens.domain.model.payment.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentRecoveryMessageTest {
    @Test fun offlineRecoveryWithoutARecordDoesNotClaimPaymentFailed() {
        assertEquals(R.string.credit_flow_payment_unconfirmed, paymentRecoveryMessage(null))
    }

    @Test fun pendingRecoveryDoesNotClaimPaymentFailed() {
        assertEquals(R.string.credit_flow_payment_unconfirmed, paymentRecoveryMessage(PaymentStatus.PENDING))
    }

    @Test fun confirmedFailuresAndCancellationsKeepTheirSpecificMessages() {
        assertEquals(R.string.credit_flow_payment_failed, paymentRecoveryMessage(PaymentStatus.FAILED))
        assertEquals(R.string.credit_flow_payment_cancelled, paymentRecoveryMessage(PaymentStatus.CANCELLED))
    }
}
