package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.DemoSessionScope
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SessionScopedDraftRepositoryTest {
    private class MemoryDrafts : DraftRepository {
        val data = mutableListOf<Draft>()
        var wait: CompletableDeferred<Unit>? = null
        override suspend fun getDrafts(): List<Draft> { wait?.await(); return data.toList() }
        override suspend fun getDraft(draftId: String) = data.find { it.draftId == draftId }
        override suspend fun getLatestDraft(stage: DraftStage) = data.lastOrNull { it.stage == stage }
        override suspend fun saveDraft(stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?): Draft {
            val source = data.find { it.draftId == sourceDraftId }
            val draft = Draft("id-${data.size}", contractId, title, content, stage, data.size + 1, 1L, 1L, source?.draftId, source?.lineageId ?: "lineage")
            data += draft
            return draft
        }
        override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage) = getDraft(sourceDraftId)?.let { saveDraft(stage, title, it.content, it.contractId, sourceDraftId) }
        override suspend fun updateDraft(draftId: String, title: String, content: String) = getDraft(draftId)?.let { saveDraft(it.stage, title, content, it.contractId, draftId) }
        override suspend fun deleteDraft(draftId: String) = data.removeAll { it.draftId == draftId }
    }

    @Test fun accountsDoNotShareDraftsAndLogoutKeepsThem() = runTest {
        var session: DemoSessionScope? = DemoSessionScope("a", "1")
        val repo = SessionScopedDraftRepository({ session }) { MemoryDrafts() }
        val first = repo.saveDraft(DraftStage.ORIGINAL, "A", "A private", null, null)
        val next = repo.saveDraft(DraftStage.MANUAL_EDIT, "A2", "edited", null, first.draftId)
        assertEquals(first.lineageId, next.lineageId)
        assertEquals(first.draftId, next.parentDraftId)
        session = DemoSessionScope("b", "2")
        assertTrue(repo.getDrafts().isEmpty())
        assertNull(repo.getDraft(first.draftId))
        session = null
        try { repo.getDrafts(); fail("missing session") } catch (_: IllegalStateException) { }
        session = DemoSessionScope("a", "3")
        assertEquals(listOf(first, next), repo.getDrafts())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun lateResultFromOldSessionIsRejected() = runTest {
        var session: DemoSessionScope? = DemoSessionScope("a", "1")
        val gate = CompletableDeferred<Unit>()
        val backing = MemoryDrafts().apply { wait = gate }
        val repo = SessionScopedDraftRepository({ session }) { backing }
        val result = async { runCatching { repo.getDrafts() } }
        runCurrent()
        session = DemoSessionScope("b", "2")
        gate.complete(Unit)
        assertTrue(result.await().exceptionOrNull() is IllegalStateException)
    }
}
