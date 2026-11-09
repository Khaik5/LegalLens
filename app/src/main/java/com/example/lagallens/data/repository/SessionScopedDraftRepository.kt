package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.DemoSessionScope
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftRepository

/** Captures the account at the start of each operation and rejects stale results. */
internal class SessionScopedDraftRepository(
    private val currentScope: () -> DemoSessionScope?,
    private val create: (DemoSessionScope) -> DraftRepository
) : DraftRepository {
    override fun forSession(sessionKey: String?): DraftRepository {
        val scope = checkNotNull(currentScope()) { "No active DEMO session" }
        check("${scope.userId}:${scope.generation}" == sessionKey) { "DEMO session changed" }
        return object : DraftRepository {
            private suspend fun <T> run(action: suspend DraftRepository.() -> T): T {
                check(currentScope() == scope) { "DEMO session changed" }
                val result = repository(scope).action()
                check(currentScope() == scope) { "DEMO session changed" }
                return result
            }
            override suspend fun getDrafts() = run { getDrafts() }
            override suspend fun getDraft(draftId: String) = run { getDraft(draftId) }
            override suspend fun getLatestDraft(stage: DraftStage) = run { getLatestDraft(stage) }
            override suspend fun saveDraft(stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?, conversationId: String?, isSaved: Boolean, isTitleGenerated: Boolean) = run { saveDraft(stage, title, content, contractId, sourceDraftId, conversationId, isSaved, isTitleGenerated) }
            override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage, isTitleGenerated: Boolean) = run { createCopy(sourceDraftId, title, stage, isTitleGenerated) }
            override suspend fun updateDraft(draftId: String, title: String, content: String) = run { updateDraft(draftId, title, content) }
            override suspend fun deleteDraft(draftId: String) = run { deleteDraft(draftId) }
        }
    }
    private var cachedScope: DemoSessionScope? = null
    private var cachedRepository: DraftRepository? = null

    @Synchronized
    private fun repository(scope: DemoSessionScope): DraftRepository {
        if (cachedScope != scope || cachedRepository == null) {
            cachedScope = scope
            cachedRepository = create(scope)
        }
        return checkNotNull(cachedRepository)
    }

    private suspend fun <T> withSession(action: suspend DraftRepository.() -> T): T {
        val scope = checkNotNull(currentScope()) { "No active DEMO session" }
        val repository = repository(scope)
        check(currentScope() == scope) { "DEMO session changed" }
        val result = repository.action()
        check(currentScope() == scope) { "DEMO session changed" }
        return result
    }

    override suspend fun getDrafts(): List<Draft> = withSession { getDrafts() }
    override suspend fun getDraft(draftId: String): Draft? = withSession { getDraft(draftId) }
    override suspend fun getLatestDraft(stage: DraftStage): Draft? = withSession { getLatestDraft(stage) }
    override suspend fun saveDraft(stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?, conversationId: String?, isSaved: Boolean, isTitleGenerated: Boolean): Draft =
        withSession { saveDraft(stage, title, content, contractId, sourceDraftId, conversationId, isSaved, isTitleGenerated) }
    override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage, isTitleGenerated: Boolean): Draft? =
        withSession { createCopy(sourceDraftId, title, stage, isTitleGenerated) }
    override suspend fun updateDraft(draftId: String, title: String, content: String): Draft? =
        withSession { updateDraft(draftId, title, content) }
    override suspend fun deleteDraft(draftId: String): Boolean = withSession { deleteDraft(draftId) }
}
