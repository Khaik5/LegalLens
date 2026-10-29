package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.DemoSessionScope
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftRepository
import java.util.concurrent.ConcurrentHashMap

/** Captures the account at the start of each operation and rejects stale results. */
internal class SessionScopedDraftRepository(
    private val currentScope: () -> DemoSessionScope?,
    private val create: (String) -> DraftRepository
) : DraftRepository {
    private val repositories = ConcurrentHashMap<String, DraftRepository>()

    private suspend fun <T> withSession(action: suspend DraftRepository.() -> T): T {
        val scope = checkNotNull(currentScope()) { "No active DEMO session" }
        val repository = repositories.computeIfAbsent(scope.userId, create)
        check(currentScope() == scope) { "DEMO session changed" }
        val result = repository.action()
        check(currentScope() == scope) { "DEMO session changed" }
        return result
    }

    override suspend fun getDrafts(): List<Draft> = withSession { getDrafts() }
    override suspend fun getDraft(draftId: String): Draft? = withSession { getDraft(draftId) }
    override suspend fun getLatestDraft(stage: DraftStage): Draft? = withSession { getLatestDraft(stage) }
    override suspend fun saveDraft(stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?): Draft =
        withSession { saveDraft(stage, title, content, contractId, sourceDraftId) }
    override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage): Draft? =
        withSession { createCopy(sourceDraftId, title, stage) }
    override suspend fun updateDraft(draftId: String, title: String, content: String): Draft? =
        withSession { updateDraft(draftId, title, content) }
    override suspend fun deleteDraft(draftId: String): Boolean = withSession { deleteDraft(draftId) }
}
