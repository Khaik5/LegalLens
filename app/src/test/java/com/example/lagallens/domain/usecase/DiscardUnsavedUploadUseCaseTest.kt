package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.UnsavedUploadContracts
import com.example.lagallens.domain.repository.ContractRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscardUnsavedUploadUseCaseTest {
    private class Contracts(var failure: Exception? = null) : ContractRepository {
        val deleted = mutableListOf<String>()
        override fun observeContracts(): Flow<List<EmploymentContract>> = emptyFlow()
        override suspend fun getContract(contractId: String): EmploymentContract? = null
        override suspend fun createContract(contract: EmploymentContract) = contract
        override suspend fun updateContract(contract: EmploymentContract) = true
        override suspend fun renameContract(contractId: String, title: String) = true
        override suspend fun deleteContract(contractId: String): Boolean {
            failure?.let { throw it }
            deleted += contractId
            return true
        }
        override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = null
    }

    @Test fun anUnsavedUploadContractIsDeletedOnce() = runTest {
        val unsaved = UnsavedUploadContracts().apply { markUnsaved("7") }
        val contracts = Contracts()
        val discard = DiscardUnsavedUploadUseCase(unsaved) { contracts }

        assertTrue(discard("7"))
        assertFalse(discard("7"))

        assertEquals(listOf("7"), contracts.deleted)
    }

    @Test fun aContractTheUploadDidNotCreateIsNeverDeleted() = runTest {
        val contracts = Contracts()
        val discard = DiscardUnsavedUploadUseCase(UnsavedUploadContracts()) { contracts }

        assertFalse(discard("7"))
        assertFalse(discard(null))
        assertFalse(discard(""))

        assertTrue(contracts.deleted.isEmpty())
    }

    @Test fun aFailedDeletionIsReportedNotThrown() = runTest {
        val unsaved = UnsavedUploadContracts().apply { markUnsaved("7") }
        val discard = DiscardUnsavedUploadUseCase(unsaved) { Contracts(failure = java.io.IOException("offline")) }

        assertFalse(discard("7"))
    }
}
