package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Keeps the ledger's file IO off the caller's thread for UI callers.
 * Unlocking calls [DemoFinanceRepository] directly: it already holds the session lock,
 * so moving to another thread here would wait on that lock forever.
 */
internal class IoFinanceRepository(
    private val finance: DemoFinanceRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : CreditRepository by finance, PaymentRepository by finance {
    override val isConfigured: Boolean get() = finance.isConfigured
    override fun activeSessionKey(): String? = finance.activeSessionKey()

    override suspend fun getTransactions(): List<CreditTransaction> = withContext(io) { finance.getTransactions() }
    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = withContext(io) { finance.spend(transactionId, credits) }
    override suspend fun getPackages(): List<CreditOffer> = withContext(io) { finance.getPackages() }
    override suspend fun createPayment(paymentId: String, packageId: String): PaymentRecord = withContext(io) { finance.createPayment(paymentId, packageId) }
    override fun observePayment(paymentId: String): Flow<PaymentRecord> = finance.observePayment(paymentId).flowOn(io)
    override suspend fun cancelPayment(paymentId: String): PaymentRecord = withContext(io) { finance.cancelPayment(paymentId) }
    override suspend fun get(paymentId: String): PaymentRecord? = withContext(io) { finance.get(paymentId) }
}
