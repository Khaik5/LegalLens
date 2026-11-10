package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.local.datastore.requireUserId
import com.example.lagallens.data.datasource.remote.ContractRemoteDataSource
import com.example.lagallens.data.mapper.toContract
import com.example.lagallens.data.model.response.ContractResponse
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.repository.ContractRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class RemoteContractRepository private constructor(
    private val tokens: AuthSessionStore,
    private val remote: ContractRemoteDataSource,
    private val listState: MutableStateFlow<ContractListState>,
    private val boundSession: String?
) : ContractRepository {
    constructor(tokens: AuthSessionStore, remote: ContractRemoteDataSource) :
        this(tokens, remote, MutableStateFlow(ContractListState()), null)

    override fun forSession(sessionKey: String): ContractRepository {
        check(boundSession == null || boundSession == sessionKey) { "Session changed" }
        return RemoteContractRepository(tokens, remote, listState, sessionKey)
    }

    override fun activeSessionKey(): String? = tokens.currentSessionScope()?.key
        ?.takeIf { boundSession == null || boundSession == it }

    override fun observeSessionKeys(): Flow<String?> = tokens.observeTokenChanges()
        .map { activeSessionKey() }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeContracts(): Flow<List<EmploymentContract>> = observeSessionKeys()
        .flatMapLatest { key ->
            if (key == null) flowOf(emptyList()) else flow {
                val scope = AuthSessionScope(key)
                val userId = tokens.requireUserId(scope)
                listState.map { it.revision }.distinctUntilChanged().collect { revision ->
                    val summaries = remote.list(scope, userId)
                    val contracts = summaries.map { it.toContract() }
                    if (activeSessionKey() == key && listState.value.revision == revision) {
                        val byId = summaries.associateBy { it.id }
                        listState.update { state ->
                            if (state.revision == revision) state.copy(sessionKey = key, summaries = byId) else state
                        }
                        emit(contracts)
                    }
                }
            }
        }
        .buffer(0)
        .flowOn(Dispatchers.IO)

    override suspend fun getContract(contractId: String): EmploymentContract? = inSession { scope ->
        val detail = remote.get(scope, contractId) ?: return@inSession null
        val summary = listState.value.takeIf { it.sessionKey == scope.key }?.summaries?.get(detail.id)
        detail.copy(
            latestRiskLevel = detail.latestRiskLevel ?: summary?.latestRiskLevel,
            latestAnalysisStatus = detail.latestAnalysisStatus ?: summary?.latestAnalysisStatus
        ).toContract()
    }

    override suspend fun createContract(contract: EmploymentContract): EmploymentContract = inSession { scope ->
        val created = remote.create(scope, contract.title.trim(), contract.description)
        listState.update { it.copy(revision = it.revision + 1) }
        created.toContract()
    }

    override suspend fun renameContract(contractId: String, title: String): Boolean = inSession { scope ->
        val name = title.trim()
        if (name.isEmpty()) return@inSession false
        val existing = remote.getRequired(scope, contractId)
        if (existing.title != name) {
            remote.rename(scope, contractId, name, existing.description)
            listState.update { it.copy(revision = it.revision + 1) }
        }
        true
    }

    override suspend fun deleteContract(contractId: String): Boolean = inSession { scope ->
        remote.delete(scope, contractId)
        listState.update { state ->
            val id = contractId.toLongOrNull()
            val summaries = if (state.sessionKey == scope.key && id != null) state.summaries - id else state.summaries
            state.copy(revision = state.revision + 1, summaries = summaries)
        }
        true
    }

    override suspend fun updateContract(contract: EmploymentContract): Boolean =
        throw BackendNotConfiguredException("Contract content")

    override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? =
        throw BackendNotConfiguredException("Contract comparison")

    private suspend fun <T> inSession(action: suspend (AuthSessionScope) -> T): T = withContext(Dispatchers.IO) {
        val key = checkNotNull(activeSessionKey()) { "No active session" }
        val result = action(AuthSessionScope(key))
        check(activeSessionKey() == key) { "Session changed" }
        result
    }

    private data class ContractListState(
        val revision: Long = 0L,
        val sessionKey: String? = null,
        val summaries: Map<Long, ContractResponse> = emptyMap()
    )
}
