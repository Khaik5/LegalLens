package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.DraftRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.DraftApi
import com.example.lagallens.data.model.request.CreateDraftRequest
import com.example.lagallens.data.model.request.DraftAiJobRequest
import com.example.lagallens.data.model.request.UpdateDraftRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.DraftAiChangeResponse
import com.example.lagallens.data.model.response.DraftAiJobCreatedResponse
import com.example.lagallens.data.model.response.DraftAiJobResponse
import com.example.lagallens.data.model.response.DraftDetailResponse
import com.example.lagallens.data.model.response.DraftPageResponse
import com.example.lagallens.data.model.response.DraftSavedResponse
import com.example.lagallens.data.model.response.DraftSummaryResponse
import com.example.lagallens.data.model.response.DraftVersionListResponse
import com.example.lagallens.data.model.response.DraftVersionResponse
import com.example.lagallens.data.model.response.DraftVersionSummaryResponse
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftEditRequest
import com.example.lagallens.domain.repository.DraftGenerationRequest
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteDraftRepositoryTest {
    private val api = DraftFixture()
    private val marks = InMemoryMarks()
    private val session = RemoteRepositorySession()
    private val remote = DraftRemoteDataSource(lazyOf(api))
    private val drafts = RemoteDraftRepository(session, remote, marks)
    private val ai = RemoteDraftAiRepository(session, remote, drafts, marks, { "again" }, pollIntervalMillis = 1, maxPolls = 5)

    @Test fun savingFromASourceCreatesANewVersionAndKeepsTheStage() = runTest {
        api.versionsOf[10] = mutableListOf(1 to "D1 text", 2 to "D1 text")
        api.currentVersion[10] = 2

        val saved = ai.let { drafts.saveDraft(DraftStage.MANUAL_EDIT, "M1", "edited", sourceDraftId = "10:2", isSaved = true) }

        assertEquals("10:3", saved.draftId)
        assertEquals(DraftStage.MANUAL_EDIT, saved.stage)
        assertTrue(saved.isSaved)
        assertEquals(UpdateDraftRequest(2, "M1", "edited"), api.updates.single())
    }

    @Test fun anEarlierVersionStaysReadable() = runTest {
        api.versionsOf[10] = mutableListOf(1 to "original", 2 to "newer")
        api.currentVersion[10] = 2

        val original = drafts.getDraft("10:1")!!

        assertEquals("original", original.content)
        assertEquals(DraftStage.ORIGINAL, original.stage)
    }

    @Test fun aStaleEditReturnsNull() = runTest {
        api.versionsOf[10] = mutableListOf(1 to "a", 2 to "b")
        api.currentVersion[10] = 2
        api.updateFailure = HttpException(Response.error<Any>(409, "{}".toResponseBody()))

        assertNull(drafts.updateDraft("10:1", "t", "c"))
    }

    @Test fun aCopyOfTheCurrentVersionIsANewDraftWithTheGivenTitle() = runTest {
        api.versionsOf[10] = mutableListOf(1 to "text")
        api.currentVersion[10] = 1

        val copy = drafts.createCopy("10:1", "D3", DraftStage.COPY)!!

        assertEquals(listOf(10L), api.copied)
        assertEquals(DraftStage.COPY, copy.stage)
        assertEquals("D3", api.updates.single().title)
        assertEquals(2, copy.versionNo)
    }

    @Test fun theListDoesNotLoadTextAndLatestDoes() = runTest {
        api.versionsOf[10] = mutableListOf(1 to "text")
        api.currentVersion[10] = 1
        marks.put("42", 10, 1, DraftVersionMark(DraftStage.ORIGINAL, false))

        assertEquals("", drafts.getDrafts().single().content)
        assertEquals("text", drafts.getLatestDraft(DraftStage.ORIGINAL)!!.content)
    }

    @Test fun generatingWaitsForTheJobThenAppliesIt() = runTest {
        api.jobStatuses = mutableListOf("WAITING_PROVIDER", "COMPLETED")

        val draft = ai.generateDraft(DraftGenerationRequest("16", "27", listOf("Soạn hợp đồng")))

        assertEquals("GENERATE", api.jobRequests.single().action)
        assertEquals(16L, api.jobRequests.single().conversationId)
        assertEquals(DraftStage.ORIGINAL, draft.stage)
        assertEquals("generated", draft.content)
        assertEquals(listOf("job-1"), api.applied)
    }

    @Test fun aFailedGenerationIsReported() = runTest {
        api.jobStatuses = mutableListOf("FAILED")

        val failure = runCatching { ai.generateDraft(DraftGenerationRequest("16", null, listOf("x"))) }.exceptionOrNull()

        assertEquals(DraftAiJobFailedException::class, failure!!::class)
        assertTrue(api.applied.isEmpty())
    }

    @Test fun aJobThatNeverFinishesIsCancelled() = runTest {
        api.jobStatuses = mutableListOf("WAITING_PROVIDER")

        runCatching { ai.generateDraft(DraftGenerationRequest("16", null, listOf("x"))) }

        assertEquals(listOf("job-1"), api.cancelled)
    }

    @Test fun editSuggestionsUseTheCurrentVersionAndOnlyApplyOnRequest() = runTest {
        api.versionsOf[10] = mutableListOf(1 to "a", 2 to "b")
        api.currentVersion[10] = 2
        api.jobStatuses = mutableListOf("COMPLETED")
        api.changes = listOf(DraftAiChangeResponse("c1", "Điều 3", "old", "new", null))

        val proposal = ai.proposeEdit(DraftEditRequest("16", "27", "10:2", "Điều 3", "Thêm bảo mật"))

        assertEquals(2, api.jobRequests.single().baseVersion)
        assertEquals("Điều 3\nThêm bảo mật", api.jobRequests.single().instruction)
        assertEquals("job-1", proposal.proposalId)
        assertEquals("new", proposal.proposedText)
        assertTrue(api.applied.isEmpty())

        val applied = ai.applyEdit(proposal.proposalId, "10:2", "27")

        assertEquals(DraftStage.AI_PROPOSAL, applied.stage)
        assertFalse(applied.isSaved)
    }

    private class InMemoryMarks : DraftVersionMarks {
        private val values = mutableMapOf<String, DraftVersionMark>()
        override fun get(userId: String, draftId: Long, version: Int) = values["$userId:$draftId:$version"]
        override fun put(userId: String, draftId: Long, version: Int, mark: DraftVersionMark) { values["$userId:$draftId:$version"] = mark }
    }

    private class DraftFixture : DraftApi {
        val versionsOf = mutableMapOf<Long, MutableList<Pair<Int, String>>>()
        val currentVersion = mutableMapOf<Long, Int>()
        val updates = mutableListOf<UpdateDraftRequest>()
        val copied = mutableListOf<Long>()
        val jobRequests = mutableListOf<DraftAiJobRequest>()
        val applied = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        var updateFailure: Exception? = null
        var jobStatuses = mutableListOf("COMPLETED")
        var changes: List<DraftAiChangeResponse> = emptyList()
        private var polls = 0
        private var nextId = 50L

        private fun text(id: Long, version: Int) = versionsOf[id]?.firstOrNull { it.first == version }?.second
        private fun detail(id: Long) = DraftDetailResponse(
            id, currentVersion.getValue(id), 27, null, 16, "title $id", text(id, currentVersion.getValue(id)), null, null
        )

        override suspend fun list(scope: AuthSessionScope, page: Int, limit: Int) =
            ApiEnvelope(true, DraftPageResponse(versionsOf.keys.map { DraftSummaryResponse(it, currentVersion.getValue(it), 27, null, 16, "t", null, null) }, null, 1, limit))

        override suspend fun create(scope: AuthSessionScope, request: CreateDraftRequest): ApiEnvelope<DraftSavedResponse> {
            val id = nextId++
            versionsOf[id] = mutableListOf(1 to request.content)
            currentVersion[id] = 1
            return ApiEnvelope(true, DraftSavedResponse(id, 1))
        }

        override suspend fun get(scope: AuthSessionScope, draftId: Long) = ApiEnvelope(true, detail(draftId))

        override suspend fun update(scope: AuthSessionScope, draftId: Long, request: UpdateDraftRequest): ApiEnvelope<DraftSavedResponse> {
            updateFailure?.let { throw it }
            updates += request
            val next = request.expectedVersion + 1
            versionsOf.getOrPut(draftId) { mutableListOf() } += next to (request.content ?: text(draftId, request.expectedVersion).orEmpty())
            currentVersion[draftId] = next
            return ApiEnvelope(true, DraftSavedResponse(draftId, next))
        }

        override suspend fun delete(scope: AuthSessionScope, draftId: Long) = Response.success<Unit>(null)

        override suspend fun copy(scope: AuthSessionScope, draftId: Long): ApiEnvelope<DraftSavedResponse> {
            copied += draftId
            val id = nextId++
            versionsOf[id] = mutableListOf(1 to text(draftId, currentVersion.getValue(draftId)).orEmpty())
            currentVersion[id] = 1
            return ApiEnvelope(true, DraftSavedResponse(id, 1))
        }

        override suspend fun versions(scope: AuthSessionScope, draftId: Long, limit: Int) =
            ApiEnvelope(true, DraftVersionListResponse(versionsOf.getValue(draftId).map { DraftVersionSummaryResponse(it.first, "title $draftId", null) }))

        override suspend fun version(scope: AuthSessionScope, draftId: Long, version: Int) =
            ApiEnvelope(true, DraftVersionResponse(version, "title $draftId", text(draftId, version), null))

        override suspend fun createJob(scope: AuthSessionScope, idempotencyKey: String, request: DraftAiJobRequest): ApiEnvelope<DraftAiJobCreatedResponse> {
            jobRequests += request
            val draft = request.draftId ?: nextId++.also { versionsOf[it] = mutableListOf(1 to ""); currentVersion[it] = 1 }
            return ApiEnvelope(true, DraftAiJobCreatedResponse("job-1", draft, "WAITING_PROVIDER"))
        }

        override suspend fun job(scope: AuthSessionScope, jobId: String): ApiEnvelope<DraftAiJobResponse> {
            val status = jobStatuses[minOf(polls++, jobStatuses.lastIndex)]
            val draft = jobRequests.last().draftId ?: (nextId - 1)
            return ApiEnvelope(true, DraftAiJobResponse(jobId, draft, 1, jobRequests.last().action, status, "generated", null, null, changes))
        }

        override suspend fun apply(scope: AuthSessionScope, jobId: String): ApiEnvelope<DraftSavedResponse> {
            applied += jobId
            val request = jobRequests.last()
            val id = request.draftId ?: (nextId - 1)
            val next = currentVersion.getValue(id) + 1
            versionsOf.getValue(id) += next to "generated"
            currentVersion[id] = next
            return ApiEnvelope(true, DraftSavedResponse(id, next))
        }

        override suspend fun cancel(scope: AuthSessionScope, jobId: String): ApiEnvelope<DraftAiJobCreatedResponse> {
            cancelled += jobId
            return ApiEnvelope(true, DraftAiJobCreatedResponse(jobId, 0, "CANCELLED"))
        }
    }
}
