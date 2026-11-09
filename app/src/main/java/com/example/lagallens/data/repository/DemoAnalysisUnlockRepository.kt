package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.AnalysisUnlockStatus
import com.example.lagallens.domain.repository.CreditRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/** One-time unlock per account and result. The ledger is the only record of an unlock. */
internal class DemoAnalysisUnlockRepository(
    private val auth: AuthLocalDataSource,
    private val identities: AnalysisIdentityRepository,
    private val wallet: CreditRepository
) : AnalysisUnlockRepository {

    override suspend fun status(sessionKey: String, resultId: String): AnalysisUnlockStatus = withContext(Dispatchers.IO) {
        val scope = auth.currentSessionScope()
        if (scope == null || sessionKey != "${scope.userId}:${scope.generation}" || resultId.isBlank()) {
            return@withContext AnalysisUnlockStatus(isUnlocked = false, cost = UNLOCK_COST)
        }
        val identity = identities.get(resultId, sessionKey)
        auth.withSession(scope) {
            runBlocking {
                AnalysisUnlockStatus(isUnlocked = identity?.snapshot != null && hasUnlocked(resultId), cost = UNLOCK_COST)
            }
        }
    }

    override suspend fun unlock(sessionKey: String, resultId: String): AnalysisUnlockOutcome {
        val unavailable = AnalysisUnlockOutcome.Unavailable
        if (sessionKey.isBlank() || resultId.isBlank()) return unavailable
        return withContext(Dispatchers.IO) {
            val scope = auth.currentSessionScope() ?: return@withContext unavailable
            if (sessionKey != "${scope.userId}:${scope.generation}") return@withContext unavailable
            try {
                val identity = identities.get(resultId, sessionKey)
                if (identity?.snapshot == null || identity.resultId != resultId) return@withContext unavailable
                auth.withSession(scope) {
                    runBlocking {
                        if (hasUnlocked(resultId)) return@runBlocking AnalysisUnlockOutcome.Unlocked(wallet.balance.value)
                        val receipt = wallet.spend(spendKey(resultId), UNLOCK_COST)
                        if (receipt.applied) AnalysisUnlockOutcome.Unlocked(receipt.balance)
                        else AnalysisUnlockOutcome.Insufficient(receipt.balance)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                unavailable
            }
        }
    }

    private suspend fun hasUnlocked(resultId: String): Boolean =
        wallet.getTransactions().any { it.transactionId == "unlock:${spendKey(resultId)}" }

    private fun spendKey(resultId: String) = "unlock:$resultId"

    private companion object {
        const val UNLOCK_COST = 2
    }
}
