package com.example.lagallens.domain.model.credit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PurchaseBalanceTest {
    private fun transaction(paymentId: String?, delta: Int, createdAt: Long) =
        CreditTransaction("id-$createdAt", paymentId, null, 0, delta, "SUCCESS", createdAt)

    @Test fun laterSpendingDoesNotChangeTheReceipt() {
        val history = listOf(transaction("older", 15, 1), transaction("target", 5, 2), transaction(null, -2, 3))
        assertEquals(PurchaseBalance(20, 25), history.purchaseBalance("target", 23))
    }

    @Test fun newestPurchaseEndsAtTheCurrentBalance() {
        assertEquals(PurchaseBalance(5, 20), listOf(transaction("target", 15, 1)).purchaseBalance("target", 20))
    }

    @Test fun unknownPaymentHasNoBalance() {
        assertNull(listOf(transaction("other", 5, 1)).purchaseBalance("target", 5))
    }
}
