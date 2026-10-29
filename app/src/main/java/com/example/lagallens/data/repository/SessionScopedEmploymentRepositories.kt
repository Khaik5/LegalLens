package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.util.concurrent.ConcurrentHashMap

internal class SessionScopedEmploymentRepositories(context: Context, private val auth: AuthLocalDataSource = AuthLocalDataSource(context.applicationContext)) {
    private val app = context.applicationContext
    private val stores = ConcurrentHashMap<String, EmploymentDemoDataSource>()
    private fun store(id: String) = stores.computeIfAbsent(id) {
        EmploymentDemoDataSource(app, AccountStorageName.forUser("contract_demo_data", it))
    }
    private fun <T> execute(block: (EmploymentDemoDataSource) -> T): T {
        val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
        return auth.withSession(scope) { block(store(scope.userId)) }
    }

    val contracts: ContractRepository = object : ContractRepository {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeContracts(): Flow<List<EmploymentContract>> = auth.observeSessionScope().flatMapLatest { scope ->
            flow {
                emit(emptyList())
                if (scope != null) {
                    val source = try { auth.withSession(scope) { store(scope.userId) } }
                    catch (_: IllegalStateException) { return@flow }
                    emitAll(flow {
                        source.contracts.collect { values ->
                            if (auth.currentSessionScope() == scope) emit(values)
                        }
                    })
                } else emitAll(emptyFlow())
            }
        }
        override suspend fun getContract(contractId: String) = execute { it.getContract(contractId) }
        override suspend fun createContract(contract: EmploymentContract) = execute { it.createContract(contract) }
        override suspend fun updateContract(contract: EmploymentContract) = execute { it.updateContract(contract) }
        override suspend fun renameContract(contractId: String, title: String) = execute { it.renameContract(contractId, title) }
        override suspend fun deleteContract(contractId: String) = execute { it.deleteContract(contractId) }
        override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? {
            val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
            val source = auth.withSession(scope) { store(scope.userId) }
            val result = FakeContractRepository(source).compareContracts(firstId, secondId)
            return auth.withSession(scope) { result }
        }
    }
    val documents: DocumentRepository = object : DocumentRepository {
        override suspend fun getDocument(documentId: String) = execute { it.getDocument(documentId) }
        override suspend fun saveDocument(document: EmploymentDocument) = execute { it.saveDocument(document) }
        override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String) = execute { it.updateDocumentContent(documentId, contractId, content) }
        override suspend fun deleteDocument(documentId: String) = execute { it.deleteContractForDocument(documentId) }
    }
}
