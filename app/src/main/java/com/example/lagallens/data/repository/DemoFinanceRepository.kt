package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.DemoSessionScope
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class FinanceSnapshot(
    val schemaVersion: Int = 1,
    val balance: Int = 5,
    val payments: Map<String, PaymentRecord> = emptyMap(),
    val spends: Map<String, Int> = emptyMap(),
    val transactions: List<CreditTransaction> = emptyList()
)

internal interface FinanceStorage {
    fun read(userId: String): FinanceSnapshot?
    /** Replaces the whole snapshot atomically, or throws. */
    fun write(userId: String, snapshot: FinanceSnapshot)
}

/** One facade, no cached account ledger. Every mutation replaces one account snapshot. */
internal class DemoFinanceRepository(
    private val storage: FinanceStorage,
    private val currentScope: () -> DemoSessionScope?,
    private val inSession: (DemoSessionScope, () -> Unit) -> Unit,
    private val now: () -> Long = System::currentTimeMillis
) : CreditRepository, PaymentRepository {
    private val mutableBalance = MutableStateFlow(0)
    override val balance = mutableBalance.asStateFlow()
    override val isConfigured = true
    override fun activeSessionKey(): String? = currentScope()?.let { "${it.userId}:${it.generation}" }

    // The session lock is always acquired before this object's lock.
    private fun <T> execute(action: (String, FinanceSnapshot) -> T): T {
        val scope = checkNotNull(currentScope()) { "No active DEMO session" }
        var result: Result<T>? = null
        inSession(scope) {
            synchronized(this) {
                check(currentScope() == scope) { "DEMO session changed" }
                result = runCatching {
                    val state = storage.read(scope.userId) ?: FinanceSnapshot()
                    mutableBalance.value = state.balance
                    action(scope.userId, state)
                }
            }
        }
        return checkNotNull(result).getOrThrow()
    }

    fun refreshSession() {
        if (currentScope() == null) { mutableBalance.value = 0; return }
        try { execute { _, state -> mutableBalance.value = state.balance } }
        catch (_: Exception) { mutableBalance.value = 0 }
    }

    private fun commit(user: String, state: FinanceSnapshot) {
        storage.write(user, state)
        mutableBalance.value = state.balance
    }

    override suspend fun getTransactions(): List<CreditTransaction> = execute { _, state -> state.transactions.sortedByDescending { it.createdAtEpochMillis } }
    override suspend fun get(paymentId: String): PaymentRecord? = execute { _, state -> state.payments[paymentId] }

    override suspend fun createOrGet(paymentId: String, packageId: String, amountVnd: Long, credits: Int): PaymentRecord = execute { user, state ->
        require(paymentId.isNotBlank() && packageId.isNotBlank() && amountVnd >= 0 && credits > 0)
        state.payments[paymentId]?.also {
            require(it.packageId == packageId && it.amountVnd == amountVnd && it.credits == credits) { "Conflicting paymentId" }
        } ?: PaymentRecord(paymentId, packageId, amountVnd, credits, PaymentStatus.PENDING, now()).also {
            commit(user, state.copy(payments = state.payments + (paymentId to it)))
        }
    }

    override suspend fun updateStatus(paymentId: String, status: PaymentStatus): PaymentRecord = execute { user, state ->
        val payment = requireNotNull(state.payments[paymentId]) { "Unknown paymentId" }
        require(status != PaymentStatus.SUCCESS || payment.status == PaymentStatus.SUCCESS) { "Use completeDemoPayment" }
        if (payment.status != PaymentStatus.PENDING || payment.status == status) payment
        else payment.copy(status = status, updatedAtEpochMillis = now()).also {
            commit(user, state.copy(payments = state.payments + (paymentId to it)))
        }
    }

    private fun complete(user: String, state: FinanceSnapshot, paymentId: String): Pair<PaymentRecord, Boolean> {
        val payment = requireNotNull(state.payments[paymentId]) { "Unknown paymentId" }
        if (payment.status != PaymentStatus.PENDING) return payment to false
        val completed = payment.copy(status = PaymentStatus.SUCCESS, updatedAtEpochMillis = now())
        val transaction = CreditTransaction("payment:$paymentId", paymentId, payment.packageId, payment.amountVnd, payment.credits, "SUCCESS", now())
        commit(user, state.copy(balance = Math.addExact(state.balance, payment.credits),
            payments = state.payments + (paymentId to completed), transactions = state.transactions + transaction))
        return completed to true
    }
    override suspend fun completeDemoPayment(paymentId: String): PaymentRecord = execute { user, state -> complete(user, state, paymentId).first }

    override suspend fun addPurchasedCredits(paymentId: String, packageId: String, amountVnd: Long, credits: Int): CreditReceipt = execute { user, state ->
        val payment = requireNotNull(state.payments[paymentId]) { "Payment must exist before crediting" }
        require(payment.packageId == packageId && payment.amountVnd == amountVnd && payment.credits == credits) { "Conflicting paymentId" }
        val applied = complete(user, state, paymentId).second
        CreditReceipt(if (applied) Math.addExact(state.balance, credits) else state.balance, applied)
    }

    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = execute { user, state ->
        require(transactionId.isNotBlank() && credits > 0)
        state.spends[transactionId]?.let { require(it == credits) { "Conflicting unlock key" } }
        if (transactionId in state.spends || state.balance < credits) CreditReceipt(state.balance, false)
        else {
            val updated = state.copy(balance = state.balance - credits, spends = state.spends + (transactionId to credits),
                transactions = state.transactions + CreditTransaction("unlock:$transactionId", null, null, 0, -credits, "SUCCESS", now()))
            commit(user, updated)
            CreditReceipt(updated.balance, true)
        }
    }
}
