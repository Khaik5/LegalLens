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
    suspend fun getContract(contractId: String): EmploymentContract?
    suspend fun createContract(contract: EmploymentContract): EmploymentContract
    suspend fun updateContract(contract: EmploymentContract): Boolean
    suspend fun renameContract(contractId: String, title: String): Boolean
    suspend fun deleteContract(contractId: String): Boolean
    suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison?
}
