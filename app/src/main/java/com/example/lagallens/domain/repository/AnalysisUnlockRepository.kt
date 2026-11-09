package com.example.lagallens.domain.repository

sealed interface AnalysisUnlockOutcome {
    data class Unlocked(val balance: Int) : AnalysisUnlockOutcome
    data class Insufficient(val balance: Int) : AnalysisUnlockOutcome
    data object Unavailable : AnalysisUnlockOutcome
}

/** Entitlement for one result in the signed-in account; the cost is decided by the data source. */
data class AnalysisUnlockStatus(val isUnlocked: Boolean, val cost: Int)

interface AnalysisUnlockRepository {
    suspend fun status(sessionKey: String, resultId: String): AnalysisUnlockStatus
    suspend fun unlock(sessionKey: String, resultId: String): AnalysisUnlockOutcome
}
