package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.CreditDemoLedgerStore
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.repository.CreditRepository
import kotlinx.coroutines.flow.StateFlow

/** Repository adapter for the persistent DEMO ledger kept compatible with earlier saved balances. */
class FakeCreditRepository(context: Context) : CreditRepository {
    private val ledger = CreditDemoLedgerStore.also { it.initialize(context.applicationContext) }
    override val balance: StateFlow<Int> = ledger.balance
    override suspend fun getTransactions(): List<CreditTransaction> = ledger.getTransactions()
    override suspend fun addPurchasedCredits(paymentId: String, packageId: String, amountVnd: Long, credits: Int): CreditReceipt =
        ledger.addPurchasedCredits(paymentId, packageId, amountVnd, credits)
    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = ledger.spend(transactionId, credits)
}
