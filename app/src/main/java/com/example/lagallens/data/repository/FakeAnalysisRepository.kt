package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.model.processing.AnalysisJobState
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.domain.repository.AnalysisRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Development-only analysis summary source; replace with the backend analysis repository when approved. */
class FakeAnalysisRepository(
    private val identities: AnalysisIdentityRepository,
    private val unavailableText: (language: String) -> String = { "" },
    private val stageDurationMillis: Long = 900L
) : AnalysisRepository {
    /** DEMO only: the delays stand in for analysis stages, and the stored result is fixed sample content. */
    override fun process(operationId: String, contractId: String, documentId: String, sessionKey: String): Flow<AnalysisJobState> = flow {
        (3..6).forEach { stage ->
            delay(stageDurationMillis)
            emit(AnalysisJobState.Running(stage))
        }
        delay(stageDurationMillis)
        check(identities.sessionKey() == sessionKey) { "Session changed" }
        emit(AnalysisJobState.Completed(identities.create(operationId, contractId, documentId, sessionKey).resultId))
    }

    override suspend fun getOverview(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity): AnalysisOverview =
        checkNotNull(identity.overview) { "Analysis content unavailable" }.copy(
            severity = identity.findingIds.mapNotNull { id ->
                identity.snapshot?.get("risk:$id:severity")?.let { value ->
                    com.example.lagallens.domain.model.analysis.AnalysisSeverity.entries.find { it.name == value }
                }
            }.minByOrNull { it.ordinal }
        )

    override suspend fun getDetail(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, language: String) =
        com.example.lagallens.data.mapper.DemoAnalysisSnapshotMapper(language, unavailableText(language)).toDetail(identity)
}
