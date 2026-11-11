package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.CreditRemoteDataSource
import com.example.lagallens.data.mapper.creditBalance
import com.example.lagallens.data.mapper.toCreditTransaction
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.repository.CreditRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Wallet balance and history come from the backend. Spending waits for the backend unlock API. */
class RemoteCreditRepository(
    private val tokens: AuthSessionStore,
    private val remote: CreditRemoteDataSource
) : CreditRepository {
    private val mutableBalance = MutableStateFlow(0)
    private val balanceLock = Mutex()
    private var balanceSession: String? = null

    override val balance = mutableBalance.asStateFlow()

    override fun activeSessionKey(): String? = tokens.currentSessionScope()?.key

    override suspend fun refresh() = inSession { scope ->
        balanceLock.withLock {
            // Never show the previous account's balance while the new one loads.
            if (balanceSession != scope.key) mutableBalance.value = 0
            val wallet = remote.wallet(scope)
            check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
            balanceSession = scope.key
            mutableBalance.value = creditBalance(wallet.balance)
        }
    }

    override suspend fun getTransactions(): List<CreditTransaction> = inSession { scope ->
        remote.recentTransactions(scope).map { it.toCreditTransaction() }
    }

    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt =
        throw BackendNotConfiguredException("Credit unlock")

    /** Runs against the current session and fails if the account changes meanwhile. */
    private suspend fun <T> inSession(action: suspend (AuthSessionScope) -> T): T = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val result = action(scope)
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        result
    }
}
