package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface CreditRepository {
    fun activeSessionKey(): String? = null
    fun observeSessionKeys(): Flow<String?> = flowOf(activeSessionKey())
    /** False means the REAL wallet API has not been configured; a zero balance is not authoritative. */
    val isConfigured: Boolean get() = true
    val balance: StateFlow<Int>
    suspend fun getTransactions(): List<CreditTransaction>
    suspend fun spend(transactionId: String, credits: Int): CreditReceipt
}
