package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import kotlinx.coroutines.flow.StateFlow

interface CreditRepository {
    fun activeSessionKey(): String? = null
    /** False means the REAL wallet API has not been configured; a zero balance is not authoritative. */
    val isConfigured: Boolean get() = true
    val balance: StateFlow<Int>
    suspend fun getTransactions(): List<CreditTransaction>
    suspend fun addPurchasedCredits(paymentId: String, packageId: String, amountVnd: Long, credits: Int): CreditReceipt
    suspend fun spend(transactionId: String, credits: Int): CreditReceipt
}
