package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ContractRepository {
    fun forSession(sessionKey: String): ContractRepository = throw IllegalStateException("Session-bound contracts unavailable")
    fun activeSessionKey(): String? = null
    fun observeSessionKeys(): Flow<String?> = flowOf(activeSessionKey())
    fun observeContracts(): Flow<List<EmploymentContract>>

    /** Marks the cached list as outdated, e.g. after an analysis finished; the next observer reloads it. */
    fun invalidate() = Unit
    suspend fun getContract(contractId: String): EmploymentContract?
    suspend fun createContract(contract: EmploymentContract): EmploymentContract
    suspend fun updateContract(contract: EmploymentContract): Boolean
    suspend fun renameContract(contractId: String, title: String): Boolean
    suspend fun deleteContract(contractId: String): Boolean

    /**
     * Deletes the contract; without [confirmPaidResults] a contract with Credit-unlocked results throws
     * [com.example.lagallens.domain.model.ContractHasPaidResultsException] instead. Sources without paid results
     * simply delete.
     */
    suspend fun deleteContract(contractId: String, confirmPaidResults: Boolean): Boolean = deleteContract(contractId)
    suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison?
}
