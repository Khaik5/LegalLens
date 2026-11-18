package com.example.lagallens.data.repository

import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.AnalysisRemoteDataSource
import com.example.lagallens.data.mapper.creditBalance
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.AnalysisUnlockStatus
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** The backend decides the price and charges the wallet once per result. */
class RemoteAnalysisUnlockRepository(
    private val tokens: AuthSessionStore,
    private val remote: AnalysisRemoteDataSource
) : AnalysisUnlockRepository {
    override suspend fun status(sessionKey: String, resultId: String): AnalysisUnlockStatus {
        val state = remote.unlockStatus(scope(sessionKey), resultId)
        // A price of 1.5 Credits must not show as 1: the cost is rounded up, balances down (see creditBalance).
        return AnalysisUnlockStatus(isUnlocked = state.isUnlocked, cost = kotlin.math.ceil(state.costCredits).toInt())
    }

    override suspend fun unlock(sessionKey: String, resultId: String): AnalysisUnlockOutcome {
        val scope = scope(sessionKey)
        return try {
            val result = remote.unlock(scope, resultId)
            AnalysisUnlockOutcome.Unlocked(creditBalance(result.balanceAfter ?: remote.unlockStatus(scope, resultId).balance))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: HttpException) {
            val failure = ApiFailureMapper.map(error)
            when {
                // Already paid earlier: nothing is charged again.
                failure.details?.code == ALREADY_UNLOCKED -> AnalysisUnlockOutcome.Unlocked(balance(scope, resultId))
                failure.statusCode == 402 -> AnalysisUnlockOutcome.Insufficient(balance(scope, resultId))
                else -> AnalysisUnlockOutcome.Unavailable
            }
        } catch (_: IOException) {
            AnalysisUnlockOutcome.Unavailable
        }
    }

    private suspend fun balance(scope: AuthSessionScope, resultId: String): Int = creditBalance(remote.unlockStatus(scope, resultId).balance)

    private fun scope(sessionKey: String): AuthSessionScope {
        val current = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        check(current.key == sessionKey) { "Session changed" }
        return current
    }

    private companion object {
        const val ALREADY_UNLOCKED = "ANALYSIS_ALREADY_UNLOCKED"
    }
}
