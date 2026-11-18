package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.AnalysisRemoteDataSource
import com.example.lagallens.data.datasource.remote.OcrRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.AnalysisApi
import com.example.lagallens.data.datasource.remote.api.OcrApi
import com.example.lagallens.data.mapper.AnalysisObligationLabels
import com.example.lagallens.data.model.request.AnalysisJobRequest
import com.example.lagallens.data.model.request.OcrConfirmRequest
import com.example.lagallens.data.model.request.OcrCorrectionRequest
import com.example.lagallens.data.model.response.AnalysisAccessResponse
import com.example.lagallens.data.model.response.AnalysisCitationResponse
import com.example.lagallens.data.model.response.AnalysisFindingResponse
import com.example.lagallens.data.model.response.AnalysisJobCreatedResponse
import com.example.lagallens.data.model.response.AnalysisJobResponse
import com.example.lagallens.data.model.response.AnalysisLatestResponse
import com.example.lagallens.data.model.response.AnalysisObligationResponse
import com.example.lagallens.data.model.response.AnalysisOverviewResponse
import com.example.lagallens.data.model.response.AnalysisResultResponse
import com.example.lagallens.data.model.response.AnalysisSummaryResponse
import com.example.lagallens.data.model.response.AnalysisUnlockResponse
import com.example.lagallens.data.model.response.AnalysisUnlockStatusResponse
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.OcrConfirmResponse
import com.example.lagallens.data.model.response.OcrCorrectionResponse
import com.example.lagallens.data.model.response.OcrJobResponse
import com.example.lagallens.data.model.response.OcrResultResponse
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.model.analysis.AnalysisObligationEntry
import com.example.lagallens.domain.model.analysis.AnalysisObligationTab
import com.example.lagallens.domain.model.analysis.AnalysisPreview
import com.example.lagallens.domain.model.analysis.AnalysisSeverity
import com.example.lagallens.domain.model.processing.AnalysisJobState
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteAnalysisRepositoryTest {
    private val analysisApi = AnalysisFixture()
    private val ocrApi = OcrFixture()
    private val session = RemoteRepositorySession()
    private val analysisRemote = AnalysisRemoteDataSource(lazyOf(analysisApi))
    private val labels = AnalysisObligationLabels("Employer", "Employee", "Other")
    private val repository = RemoteAnalysisRepository(
        session, analysisRemote, OcrRemoteDataSource(lazyOf(ocrApi)), { labels }, pollIntervalMillis = 10, maxPolls = 5
    )

    private fun ocr(reviewStatus: String?, revisionId: String? = "rev-7") =
        OcrResultResponse("ocr-1", "COMPLETED", 100, null, false, 3, "text", 0.9, null, null, revisionId, reviewStatus)

    private fun job(status: String, progress: Int = 0) = AnalysisJobResponse("job-1", status, progress, null, false)

    @Test fun anApprovedRevisionIsNotConfirmedAgain() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = mutableListOf(job("COMPLETED"))

        val states = repository.process("op-1", "27", "41", "session").toList()

        assertEquals(AnalysisJobState.Completed("job-1"), states.last())
        assertTrue(ocrApi.confirmed.isEmpty())
        assertEquals(AnalysisJobRequest(27, 41, "rev-7"), analysisApi.created.single().second)
    }

    @Test fun aPendingRevisionIsConfirmedBeforeTheJobStarts() = runTest {
        ocrApi.latest = ocr("PENDING", revisionId = "rev-5")
        analysisApi.jobs = mutableListOf(job("COMPLETED"))

        repository.process("op-1", "27", "41", "session").toList()

        assertEquals(listOf(3), ocrApi.confirmed)
        assertEquals("rev-1", analysisApi.created.single().second.ocrRevisionId)
    }

    @Test fun theRevisionTheUserReviewedIsTheOneConfirmed() = runTest {
        val sharedOcr = OcrRemoteDataSource(lazyOf(ocrApi))
        val reviewing = RemoteAnalysisRepository(
            session, analysisRemote, sharedOcr, { labels }, pollIntervalMillis = 10, maxPolls = 5
        )
        ocrApi.latest = ocr("PENDING")
        sharedOcr.latestForView(com.example.lagallens.core.network.AuthSessionScope(session.key), "41")
        // Someone saved a newer revision after this user looked at revision 3.
        ocrApi.latest = ocr("PENDING").copy(revision = 4)
        analysisApi.jobs = mutableListOf(job("COMPLETED"))

        reviewing.process("op-1", "27", "41", "session").toList()

        assertEquals(listOf(3), ocrApi.confirmed)
    }

    @Test fun startingANewRecognitionForgetsTheRevisionShownBefore() = runTest {
        val sharedOcr = OcrRemoteDataSource(lazyOf(ocrApi))
        ocrApi.latest = ocr("PENDING")
        val scope = com.example.lagallens.core.network.AuthSessionScope("session")

        sharedOcr.latestForView(scope, "41")
        assertEquals(3, sharedOcr.viewedRevision(scope, "41"))
        sharedOcr.create(scope, "41", "key-1")

        assertNull(sharedOcr.viewedRevision(scope, "41"))
    }

    @Test fun theOperationIdIsTheIdempotencyKey() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = mutableListOf(job("COMPLETED"))

        repository.process("op-1", "27", "41", "session").toList()

        assertEquals("op-1", analysisApi.created.single().first)
    }

    @Test fun progressMovesTheTimelineForward() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = mutableListOf(job("PROCESSING", 40), job("PROCESSING", 80), job("COMPLETED", 100))

        val states = repository.process("op-1", "27", "41", "session").toList()

        assertEquals(
            listOf(AnalysisJobState.Running(3), AnalysisJobState.Running(4), AnalysisJobState.Running(5), AnalysisJobState.Completed("job-1")),
            states
        )
    }

    @Test fun aFailedJobIsReported() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = mutableListOf(job("PROCESSING"), job("FAILED"))

        assertEquals(AnalysisJobState.Failed, repository.process("op-1", "27", "41", "session").toList().last())
    }

    @Test fun aFailedJobIsRetriedWhenTheSameOperationStartsAgain() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.createdStatus = "FAILED"
        analysisApi.jobs = mutableListOf(job("PROCESSING"), job("COMPLETED"))

        val states = repository.process("op-1", "27", "41", "session").toList()

        assertEquals(listOf("job-1"), analysisApi.retried)
        assertEquals(AnalysisJobState.Completed("job-1"), states.last())
    }

    @Test fun aFailedJobThatCannotBeRetriedStaysFailed() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.createdStatus = "FAILED"
        analysisApi.retryable = false

        assertEquals(AnalysisJobState.Failed, repository.process("op-1", "27", "41", "session").toList().last())
    }

    @Test fun leavingTheScreenWhileWaitingCancelsTheJob() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = mutableListOf(job("PROCESSING", progress = 10), job("PROCESSING", progress = 90))

        // Stops collecting after the first stage, as a cleared ViewModel does.
        repository.process("op-1", "27", "41", "session").take(2).toList()

        assertEquals(listOf("job-1"), analysisApi.cancelled)
    }

    @Test fun aFinishedJobIsNotCancelled() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = mutableListOf(job("COMPLETED"))

        repository.process("op-1", "27", "41", "session").toList()

        assertTrue(analysisApi.cancelled.isEmpty())
    }

    @Test fun anUnconfiguredProviderIsReportedAsFailure() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.createFailure = httpError(503)

        assertEquals(AnalysisJobState.Failed, repository.process("op-1", "27", "41", "session").toList().last())
    }

    @Test fun missingOcrTextFailsWithoutCreatingAJob() = runTest {
        ocrApi.latest = null

        assertEquals(AnalysisJobState.Failed, repository.process("op-1", "27", "41", "session").toList().last())
        assertTrue(analysisApi.created.isEmpty())
    }

    @Test fun anotherSessionNeverStartsAJob() = runTest {
        ocrApi.latest = ocr("APPROVED")

        val states = repository.process("op-1", "27", "41", "someone-else").toList()

        assertEquals(listOf(AnalysisJobState.Failed), states)
        assertTrue(analysisApi.created.isEmpty())
    }

    @Test fun stopsWaitingAfterTheTimeLimit() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = MutableList(10) { job("PROCESSING") }

        assertEquals(AnalysisJobState.Failed, repository.process("op-1", "27", "41", "session").toList().last())
        // The backend allows one active job per contract, so a job left running would block the next analysis.
        assertEquals(listOf("job-1"), analysisApi.cancelled)
    }

    @Test fun aJobTheServerReportsAsFailedIsNotCancelledByTheClient() = runTest {
        ocrApi.latest = ocr("APPROVED")
        analysisApi.jobs = mutableListOf(job("PROCESSING"), job("FAILED"))

        assertEquals(AnalysisJobState.Failed, repository.process("op-1", "27", "41", "session").toList().last())
        assertTrue(analysisApi.cancelled.isEmpty())
    }

    @Test fun aLockedResultKeepsItsRealSizeSoTheScreensCountTheHiddenItems() = runTest {
        analysisApi.result = AnalysisResultResponse(
            "job-1", 27, 41,
            AnalysisOverviewResponse(62, "CRITICAL", 5, 5, 0),
            summarySections = List(2) { AnalysisSummaryResponse("s${it + 1}", "title", "text") },
            keyFacts = emptyList(), parties = emptyList(), obligations = emptyList(),
            findings = List(2) { AnalysisFindingResponse("f${it + 1}", "HIGH", null, "t", "d", null, null, null) },
            citations = emptyList(),
            access = AnalysisAccessResponse(false, "preview", mapOf("summarySections" to 3, "findings" to 3))
        )
        val identity = AnalysisIdentity("job-1", "", "27", "41")

        val detail = repository.getDetail(identity, "vi")
        val overview = repository.getOverview(identity)

        assertEquals(5, detail.findings.size)
        assertEquals(2, AnalysisPreview.visibleCount(detail.findings.size, isUnlocked = false))
        assertEquals(listOf("f1", "f2"), detail.findings.take(2).map { it.id })
        assertTrue(detail.findings.drop(2).all { it.title.isEmpty() && it.description.isEmpty() })
        assertEquals(5, detail.summary.size)
        assertEquals(AnalysisSeverity.HIGH, overview.severity)
        assertEquals(62, overview.score)
    }

    @Test fun citationsKeepTheirMetadataAndPadHiddenOnes() = runTest {
        analysisApi.result = AnalysisResultResponse(
            "job-1", 27, 41, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            citations = listOf(AnalysisCitationResponse("c1", 301, "Luật Thương mại 2005", "301", "Mức phạt", "Phạt vi phạm", "Không quá 8%", "2006-01-01", "ACTIVE")),
            access = AnalysisAccessResponse(false, "preview", mapOf("citations" to 2))
        )

        val citations = repository.getDetail(AnalysisIdentity("job-1", "", "27", "41"), "vi").citations

        assertEquals(3, citations.size)
        assertEquals("Luật Thương mại 2005", citations.first().documentTitle)
        assertEquals(301L, citations.first().legalArticleId)
        assertEquals("2006-01-01", citations.first().effectiveDate)
        assertTrue(citations.drop(1).all { it.excerpt.isEmpty() && it.documentTitle == null })
    }

    @Test fun obligationsAreGroupedByTabAndParty() = runTest {
        analysisApi.result = AnalysisResultResponse(
            "job-1", 27, 41, null, emptyList(), emptyList(), emptyList(),
            obligations = listOf(
                AnalysisObligationResponse("o1", "EMPLOYER", "RIGHT", "Right", "c1", "d", null),
                AnalysisObligationResponse("o2", "EMPLOYEE", "DUTY", "Duty", "c2", "d", null)
            ),
            findings = emptyList(), citations = emptyList(), access = null
        )

        val entries = repository.getDetail(AnalysisIdentity("job-1", "", "27", "41"), "vi").obligations

        val sections = entries.filterIsInstance<AnalysisObligationEntry.Section>()
        assertEquals(listOf(AnalysisObligationTab.RIGHTS, AnalysisObligationTab.DUTIES), sections.map { it.tab })
        assertEquals(listOf("Employer", "Employee"), sections.map { it.title })
        assertEquals(2, entries.filterIsInstance<AnalysisObligationEntry.Item>().size)
    }

    @Test fun onlyAFinishedAnalysisCanBeReopened() = runTest {
        val identities = RemoteAnalysisIdentityRepository(session, analysisRemote)
        analysisApi.latest = AnalysisLatestResponse("job-1", 27, 41, "FAILED", false)
        assertNull(identities.latest("27", "session"))

        analysisApi.latest = AnalysisLatestResponse("job-1", 27, 41, "COMPLETED", true)
        assertEquals("job-1", identities.latest("27", "session")?.resultId)

        analysisApi.latest = null
        assertNull(identities.latest("27", "session"))
    }

    @Test fun anUnknownResultIsUnavailable() = runTest {
        val identities = RemoteAnalysisIdentityRepository(session, analysisRemote)
        analysisApi.resultFailure = httpError(404)

        assertNull(identities.get("missing", "session"))
    }

    @Test fun unlockOutcomesFollowTheBackend() = runTest {
        val unlocks = RemoteAnalysisUnlockRepository(session, analysisRemote)
        analysisApi.unlockStatus = AnalysisUnlockStatusResponse(false, 2, 1)

        analysisApi.unlockFailure = httpError(402)
        assertEquals(AnalysisUnlockOutcome.Insufficient(1), unlocks.unlock("session", "job-1"))

        analysisApi.unlockFailure = httpError(409, """{"code":"ANALYSIS_ALREADY_UNLOCKED"}""")
        assertEquals(AnalysisUnlockOutcome.Unlocked(1), unlocks.unlock("session", "job-1"))

        analysisApi.unlockFailure = httpError(503)
        assertEquals(AnalysisUnlockOutcome.Unavailable, unlocks.unlock("session", "job-1"))

        analysisApi.unlockFailure = null
        assertEquals(AnalysisUnlockOutcome.Unlocked(8), unlocks.unlock("session", "job-1"))

        val status = unlocks.status("session", "job-1")
        assertEquals(2, status.cost)
        assertEquals(false, status.isUnlocked)
    }

    private fun httpError(code: Int, body: String = "{}") = HttpException(Response.error<Any>(code, body.toResponseBody()))

    private class AnalysisFixture : AnalysisApi {
        val created = mutableListOf<Pair<String, AnalysisJobRequest>>()
        var createFailure: Exception? = null
        var jobs = mutableListOf<AnalysisJobResponse>()
        var result: AnalysisResultResponse? = null
        var resultFailure: Exception? = null
        var latest: AnalysisLatestResponse? = null
        var unlockStatus = AnalysisUnlockStatusResponse(false, 2, 10)
        var unlockFailure: Exception? = null
        var createdStatus = "PENDING"
        var retryable = true
        val cancelled = mutableListOf<String>()
        val retried = mutableListOf<String>()
        private var reads = 0

        override suspend fun create(scope: AuthSessionScope, idempotencyKey: String, request: AnalysisJobRequest): ApiEnvelope<AnalysisJobCreatedResponse> {
            created += idempotencyKey to request
            createFailure?.let { throw it }
            return ApiEnvelope(true, AnalysisJobCreatedResponse("job-1", createdStatus))
        }

        override suspend fun job(scope: AuthSessionScope, jobId: String): ApiEnvelope<AnalysisJobResponse> =
            ApiEnvelope(true, jobs[minOf(reads++, jobs.lastIndex)])

        override suspend fun result(scope: AuthSessionScope, jobId: String, view: String?): ApiEnvelope<AnalysisResultResponse> {
            resultFailure?.let { throw it }
            return ApiEnvelope(true, checkNotNull(result))
        }

        override suspend fun unlockStatus(scope: AuthSessionScope, jobId: String) = ApiEnvelope(true, unlockStatus)

        override suspend fun unlock(scope: AuthSessionScope, jobId: String): ApiEnvelope<AnalysisUnlockResponse> {
            unlockFailure?.let { throw it }
            return ApiEnvelope(true, AnalysisUnlockResponse(true, 8))
        }

        override suspend fun cancel(scope: AuthSessionScope, jobId: String): Response<Unit> {
            cancelled += jobId
            return Response.success(Unit)
        }

        override suspend fun retry(scope: AuthSessionScope, jobId: String): Response<Unit> {
            retried += jobId
            return if (retryable) Response.success(Unit) else Response.error(409, "{}".toResponseBody())
        }

        override suspend fun latest(scope: AuthSessionScope, contractId: String): ApiEnvelope<AnalysisLatestResponse> =
            latest?.let { ApiEnvelope(true, it) } ?: throw HttpException(Response.error<Any>(404, "{}".toResponseBody()))
    }

    private class OcrFixture : OcrApi {
        override suspend fun cancel(scope: AuthSessionScope, documentId: String): Response<Unit> = Response.success(Unit)

        var latest: OcrResultResponse? = null
        val confirmed = mutableListOf<Int>()

        override suspend fun create(scope: AuthSessionScope, documentId: String, idempotencyKey: String) =
            ApiEnvelope(true, OcrJobResponse("ocr-1", "PENDING"))

        override suspend fun latest(scope: AuthSessionScope, documentId: String): ApiEnvelope<OcrResultResponse> =
            latest?.let { ApiEnvelope(true, it) } ?: throw HttpException(Response.error<Any>(404, "{}".toResponseBody()))

        override suspend fun confirm(scope: AuthSessionScope, documentId: String, request: OcrConfirmRequest): ApiEnvelope<OcrConfirmResponse> {
            confirmed += request.expectedRevision
            return ApiEnvelope(true, OcrConfirmResponse("rev-1", request.expectedRevision, "APPROVED"))
        }

        override suspend fun correct(scope: AuthSessionScope, documentId: String, request: OcrCorrectionRequest) =
            ApiEnvelope(true, OcrCorrectionResponse("ocr-1", request.expectedRevision + 1, request.text))
    }
}
