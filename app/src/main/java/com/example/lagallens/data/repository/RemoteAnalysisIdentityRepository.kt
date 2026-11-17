package com.example.lagallens.data.repository

import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.AnalysisRemoteDataSource
import com.example.lagallens.data.mapper.AnalysisResultMapper
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.repository.AnalysisContentUnavailableException
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import retrofit2.HttpException

/** The backend job ID is the result ID; the backend, not the device, owns which results exist. */
class RemoteAnalysisIdentityRepository(
    private val tokens: AuthSessionStore,
    private val remote: AnalysisRemoteDataSource
) : AnalysisIdentityRepository {
    override fun sessionKey(): String? = tokens.currentSessionScope()?.key

    /** Jobs are created by [RemoteAnalysisRepository.process], which also waits for them. */
    override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String): AnalysisIdentity =
        throw UnsupportedOperationException("Analysis jobs are created by the analysis repository")

    override suspend fun get(resultId: String, sessionKey: String): AnalysisIdentity? {
        val scope = scopeFor(sessionKey) ?: return null
        val result = try {
            remote.result(scope, resultId)
        } catch (error: HttpException) {
            val failure = ApiFailureMapper.map(error)
            // 404: not found or not owned; 409: the job has not produced a result.
            if (failure.statusCode == 404) return null
            if (failure.statusCode == 409) throw AnalysisContentUnavailableException()
            throw error
        }
        return AnalysisIdentity(
            resultId = resultId,
            operationId = "",
            contractId = result.contractId?.toString().orEmpty(),
            documentId = result.documentId?.toString().orEmpty(),
            findingIds = result.findings.orEmpty().mapNotNull { it.id },
            overview = AnalysisResultMapper.toOverview(result)
        )
    }

    /** Only a finished analysis can be reopened; a failed or running one means a new analysis is needed. */
    override suspend fun latest(contractId: String, sessionKey: String): AnalysisIdentity? {
        val scope = scopeFor(sessionKey) ?: return null
        val job = remote.latest(scope, contractId) ?: return null
        if (job.status != COMPLETED) return null
        return AnalysisIdentity(
            resultId = job.jobId,
            operationId = "",
            contractId = contractId,
            documentId = job.documentId?.toString().orEmpty()
        )
    }

    private fun scopeFor(sessionKey: String): AuthSessionScope? =
        sessionKey().takeIf { it == sessionKey }?.let(::AuthSessionScope)

    private companion object {
        const val COMPLETED = "COMPLETED"
    }
}
