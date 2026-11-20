package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.repository.AnalysisContentUnavailableException
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.AnalysisUnlockStatus
import com.example.lagallens.domain.repository.ContractRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetLockedAnalysisResultsUseCaseTest {
    private val contracts = listOf(
        contract("locked-old", updated = 1L),
        contract("unlocked", updated = 2L),
        contract("not-analysed", updated = 3L),
        contract("unreadable", updated = 4L),
        contract("locked-new", updated = 5L)
    )
    private val latest = mapOf("locked-old" to "job-1", "unlocked" to "job-2", "locked-new" to "job-5")
    private val unlocked = setOf("job-2")
    private var unlockFailure: Exception? = null

    private val useCase = GetLockedAnalysisResultsUseCase(
        contractRepository(),
        object : AnalysisIdentityRepository {
            override fun sessionKey() = "session"
            override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String) = error("unused")
            override suspend fun get(resultId: String, sessionKey: String): AnalysisIdentity? = null
            override suspend fun latest(contractId: String, sessionKey: String): AnalysisIdentity? {
                if (contractId == "unreadable") throw AnalysisContentUnavailableException()
                return latest[contractId]?.let { AnalysisIdentity(it, "", contractId, "") }
            }
        },
        object : AnalysisUnlockRepository {
            override suspend fun status(sessionKey: String, resultId: String): AnalysisUnlockStatus {
                unlockFailure?.let { throw it }
                return AnalysisUnlockStatus(isUnlocked = resultId in unlocked, cost = 2)
            }
            override suspend fun unlock(sessionKey: String, resultId: String): AnalysisUnlockOutcome = AnalysisUnlockOutcome.Unavailable
        }
    )

    @Test fun listsOnlyFinishedResultsThatAreStillLockedNewestFirst() = runTest {
        val results = useCase("session")

        assertEquals(listOf("job-5", "job-1"), results.map { it.resultId })
        assertEquals(listOf("Hợp đồng locked-new", "Hợp đồng locked-old"), results.map { it.contractTitle })
        assertTrue(results.all { it.unlockCost == 2 })
    }

    @Test fun aFailedUnlockLookupIsReportedInsteadOfShowingAPartialList() = runTest {
        unlockFailure = IOException("offline")

        assertTrue(runCatching { useCase("session") }.exceptionOrNull() is IOException)
    }

    private fun contract(id: String, updated: Long) =
        EmploymentContract(id, "document-$id", "Hợp đồng $id", "", 1L, updated, fileName = "$id.docx")

    private fun contractRepository() = object : ContractRepository {
        override fun observeContracts(): Flow<List<EmploymentContract>> = flowOf(contracts)
        override suspend fun getContract(contractId: String) = contracts.firstOrNull { it.id == contractId }
        override suspend fun createContract(contract: EmploymentContract) = contract
        override suspend fun updateContract(contract: EmploymentContract) = false
        override suspend fun renameContract(contractId: String, title: String) = false
        override suspend fun deleteContract(contractId: String) = false
        override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = null
    }
}
