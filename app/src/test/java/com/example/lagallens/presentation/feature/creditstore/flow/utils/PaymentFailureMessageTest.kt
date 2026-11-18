package com.example.lagallens.presentation.feature.creditstore.flow.utils

import com.example.lagallens.R
import com.example.lagallens.domain.model.payment.PaymentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentFailureMessageTest {
    @Test fun failedPaymentShowsFailureEvenIfItsTransferHasExpired() {
        assertEquals(R.string.credit_flow_payment_failed, paymentFailureMessage(PaymentStatus.FAILED, 10, 20))
    }

    @Test fun unpaidTransferAtOrAfterExpiryShowsExpiredMessage() {
        for (status in listOf(null, PaymentStatus.PENDING, PaymentStatus.CANCELLED)) {
            assertEquals(R.string.credit_flow_payment_expired, paymentFailureMessage(status, 10, 10))
        }
    }

    @Test fun cancelledPaymentBeforeExpiryShowsCancellation() {
        assertEquals(R.string.credit_flow_payment_cancelled, paymentFailureMessage(PaymentStatus.CANCELLED, 30, 20))
        assertEquals(R.string.credit_flow_payment_cancelled, paymentFailureMessage(PaymentStatus.CANCELLED, null, 20))
    }

    @Test fun unknownFailureShowsGenericPaymentError() {
        assertEquals(R.string.credit_flow_payment_failed, paymentFailureMessage(null, null, 20))
        assertEquals(R.string.credit_flow_payment_failed, paymentFailureMessage(PaymentStatus.PENDING, 30, 20))
    }
}
