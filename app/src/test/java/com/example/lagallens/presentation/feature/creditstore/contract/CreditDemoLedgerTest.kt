package com.example.lagallens.presentation.feature.creditstore.contract

import com.example.lagallens.data.datasource.local.datastore.CreditDemoLedger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditDemoLedgerTest {
    @Test fun duplicateDemoPurchaseDoesNotAddCreditsTwice() {
        val ledger = CreditDemoLedger(initialBalance = 5)

        val first = runBlocking { ledger.addPurchasedCredits("purchase-1", "package-5", 29000, 5) }
        val duplicate = runBlocking { ledger.addPurchasedCredits("purchase-1", "package-5", 29000, 5) }

        assertTrue(first.applied)
        assertEquals(10, first.balance)
        assertFalse(duplicate.applied)
        assertEquals(10, duplicate.balance)
        assertEquals(10, ledger.balance.value)
    }

    @Test fun spendIsIdempotentAndRejectsInsufficientBalance() = runBlocking {
        val ledger = CreditDemoLedger(initialBalance = 5)
        val first = ledger.spend("unlock-result-1", 2)
        val duplicate = ledger.spend("unlock-result-1", 2)
        val insufficient = ledger.spend("unlock-result-2", 10)

        assertTrue(first.applied)
        assertEquals(3, first.balance)
        assertFalse(duplicate.applied)
        assertEquals(3, duplicate.balance)
        assertFalse(insufficient.applied)
        assertEquals(3, ledger.balance.value)
    }
}
