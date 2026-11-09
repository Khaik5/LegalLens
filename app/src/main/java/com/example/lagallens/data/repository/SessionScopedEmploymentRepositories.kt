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
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

internal class SessionScopedEmploymentRepositories(context: Context, private val auth: AuthLocalDataSource = AuthLocalDataSource(context.applicationContext)) {
    private val app = context.applicationContext
    private val stores = ConcurrentHashMap<String, EmploymentDemoDataSource>()
    private fun store(id: String) = stores.computeIfAbsent(id) {
        EmploymentDemoDataSource(app, AccountStorageName.forUser("contract_demo_data", it))
    }
    private suspend fun <T> execute(block: (EmploymentDemoDataSource) -> T): T = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
        auth.withSession(scope) { block(store(scope.userId)) }
    }
    private suspend fun <T> executeForSession(key: String, block: (EmploymentDemoDataSource) -> T): T = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
        auth.withSession(scope) {
            check(key.isNotBlank() && key == "${scope.userId}:${scope.generation}") { "DEMO session changed" }
            block(store(scope.userId))
        }
    }

    val analysisIdentities: com.example.lagallens.domain.repository.AnalysisIdentityRepository =
        object : com.example.lagallens.domain.repository.AnalysisIdentityRepository {
            override fun sessionKey(): String? = auth.currentSessionScope()?.let { "${it.userId}:${it.generation}" }

            private suspend fun <T> inOwnerSession(key: String, action: (AnalysisIdentityStore) -> T): T = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
                auth.withSession(scope) {
                    check("${scope.userId}:${scope.generation}" == key) { "DEMO session changed" }
                    val source = store(scope.userId)
                    val directory = java.io.File(app.filesDir, "analysis_identity_v1")
                    val target = android.util.AtomicFile(java.io.File(directory, AccountStorageName.forUser("analysis", scope.userId)))
                    val identityStore = AnalysisIdentityStore(
                        read = {
                            val raw = try { target.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() } }
                            catch (failure: java.io.FileNotFoundException) {
                                val base = target.baseFile
                                if (base.exists() || java.io.File(base.path + ".bak").exists() || java.io.File(base.path + ".new").exists()) throw failure
                                null
                            }
                            if (raw == null) emptyList() else try { AnalysisIdentityCodec.decode(raw) }
                            catch (_: org.json.JSONException) { throw com.example.lagallens.domain.repository.AnalysisContentUnavailableException() }
                            catch (_: IllegalStateException) { throw com.example.lagallens.domain.repository.AnalysisContentUnavailableException() }
                        },
                        write = { records ->
                            val encoded = AnalysisIdentityCodec.encode(records)
                            check(AnalysisIdentityCodec.decode(encoded) == records) { "Invalid analysis snapshot" }
                            check(directory.isDirectory || directory.mkdirs()) { "Could not create analysis storage" }
                            val output = target.startWrite()
                            try {
                                output.write(encoded.toByteArray(Charsets.UTF_8))
                                output.fd.sync()
                                target.finishWrite(output)
                            } catch (failure: Throwable) {
                                target.failWrite(output)
                                throw failure
                            }
                        },
                        snapshot = { createDemoAnalysisSnapshot(app) },
                        ownerId = scope.userId,
                        linkedDocument = { contractId, documentId ->
                            val contract = source.getContract(contractId)
                            val document = source.getDocument(documentId)
                            contract != null && contract.documentId == documentId && document?.contractId == contractId
                        }
                    )
                    action(identityStore)
                }
            }
            override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String) =
                inOwnerSession(sessionKey) { it.create(operationId, contractId, documentId) }
            override suspend fun get(resultId: String, sessionKey: String) = inOwnerSession(sessionKey) { it.get(resultId) }
            override suspend fun latest(contractId: String, sessionKey: String) = inOwnerSession(sessionKey) { it.latest(contractId) }
        }

    val contracts: ContractRepository = object : ContractRepository {
        override fun forSession(sessionKey: String): ContractRepository = object : ContractRepository {
            override fun activeSessionKey() = sessionKey
            override fun observeContracts(): Flow<List<EmploymentContract>> = throw UnsupportedOperationException()
            override suspend fun getContract(contractId: String) = executeForSession(sessionKey) { it.getContract(contractId) }
            override suspend fun createContract(contract: EmploymentContract) = executeForSession(sessionKey) { it.createContract(contract) }
            override suspend fun updateContract(contract: EmploymentContract) = executeForSession(sessionKey) { it.updateContract(contract) }
            override suspend fun renameContract(contractId: String, title: String) = executeForSession(sessionKey) { it.renameContract(contractId, title) }
            override suspend fun deleteContract(contractId: String) = executeForSession(sessionKey) { it.deleteContract(contractId) }
            override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = throw UnsupportedOperationException()
        }
        override fun activeSessionKey(): String? = auth.currentSessionScope()?.let { "${it.userId}:${it.generation}" }
        override fun observeSessionKeys(): Flow<String?> = auth.observeSessionScope().map { it?.let { scope -> "${scope.userId}:${scope.generation}" } }
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeContracts(): Flow<List<EmploymentContract>> = auth.observeSessionScope().flatMapLatest { scope ->
            flow {
                emit(emptyList())
                if (scope != null) {
                    val source = try { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { auth.withSession(scope) { store(scope.userId) } } }
                    catch (_: IllegalStateException) { return@flow }
                    emitAll(flow {
                        kotlinx.coroutines.flow.combine(source.contracts,
                            com.example.lagallens.data.datasource.local.datastore.ProfileSettingsLocalDataSource(app).observeSettings()
                        ) { _, settings ->
                            val tag = if (settings.language == com.example.lagallens.domain.model.ProfileLanguage.ENGLISH) "en" else "vi"
                            val config = android.content.res.Configuration(app.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(tag)) }
                            val localized = app.createConfigurationContext(config)
                            source.localizedContracts { localized.getString(it) }
                        }.collect { values -> if (auth.currentSessionScope() == scope) emit(values) }
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
    private fun retainScan(document: EmploymentDocument): EmploymentDocument =
        com.example.lagallens.data.datasource.local.ScanDocumentStorage(app).retain(document, checkNotNull(auth.currentSessionScope()).userId)

    val documents: DocumentRepository = object : DocumentRepository {
        override fun forSession(sessionKey: String): DocumentRepository = object : DocumentRepository {
            override suspend fun getDocument(documentId: String) = executeForSession(sessionKey) { it.getDocument(documentId) }
            override suspend fun saveDocument(document: EmploymentDocument) = executeForSession(sessionKey) { it.saveDocument(retainScan(document)) }
            override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String) = executeForSession(sessionKey) { it.updateDocumentContent(documentId, contractId, content) }
            override suspend fun deleteDocument(documentId: String) = executeForSession(sessionKey) { it.deleteContractForDocument(documentId) }
        }
        override suspend fun getDocument(documentId: String) = execute { it.getDocument(documentId) }
        override suspend fun saveDocument(document: EmploymentDocument) = execute { it.saveDocument(retainScan(document)) }
        override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String) = execute { it.updateDocumentContent(documentId, contractId, content) }
        override suspend fun deleteDocument(documentId: String) = execute { it.deleteContractForDocument(documentId) }
    }
}
