package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.analysis.LockedAnalysisResult
import com.example.lagallens.domain.repository.AnalysisContentUnavailableException
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.ContractRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * The analysis results the user can still unlock. The backend has no list of them, so each contract's latest
 * finished analysis is read with its unlock state; a contract whose result cannot be read is left out.
 */
class GetLockedAnalysisResultsUseCase(
    private val contracts: ContractRepository,
    private val identities: AnalysisIdentityRepository,
    private val unlocks: AnalysisUnlockRepository
) {
    fun sessionKey(): String? = identities.sessionKey()

    suspend operator fun invoke(sessionKey: String): List<LockedAnalysisResult> = coroutineScope {
        contracts.observeContracts().first().chunked(PARALLEL_LOOKUPS).flatMap { batch ->
            batch.map { contract ->
                async {
                    val latest = try {
                        identities.latest(contract.id, sessionKey)
                    } catch (_: AnalysisContentUnavailableException) {
                        null
                    } ?: return@async null
                    val status = unlocks.status(sessionKey, latest.resultId)
                    if (status.isUnlocked) null else LockedAnalysisResult(
                        contractId = contract.id,
                        contractTitle = contract.title,
                        fileName = contract.fileName,
                        resultId = latest.resultId,
                        unlockCost = status.cost,
                        updatedAtMillis = contract.updatedAtMillis
                    )
                }
            }.awaitAll().filterNotNull()
        }.sortedByDescending { it.updatedAtMillis }
    }

    private companion object {
        const val PARALLEL_LOOKUPS = 4
    }
}
