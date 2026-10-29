package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import kotlinx.coroutines.flow.Flow

interface ContractRepository {
    fun observeContracts(): Flow<List<EmploymentContract>>
    suspend fun getContract(contractId: String): EmploymentContract?
    suspend fun createContract(contract: EmploymentContract): EmploymentContract
    suspend fun updateContract(contract: EmploymentContract): Boolean
    suspend fun renameContract(contractId: String, title: String): Boolean
    suspend fun deleteContract(contractId: String): Boolean
    suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison?
}
