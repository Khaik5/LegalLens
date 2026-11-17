package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.ContractRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.ContractApi
import com.example.lagallens.data.mapper.toContract
import com.example.lagallens.data.model.request.CompareContractsRequest
import com.example.lagallens.data.model.request.CreateContractRequest
import com.example.lagallens.data.model.request.UpdateContractRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ComparedContractResponse
import com.example.lagallens.data.model.response.ComparisonOverviewResponse
import com.example.lagallens.data.model.response.ComparisonSummaryResponse
import com.example.lagallens.data.model.response.ContractComparisonResponse
import com.example.lagallens.data.model.response.ContractPageResponse
import com.example.lagallens.data.model.response.FieldDifferenceResponse
import com.example.lagallens.data.model.response.RiskComparisonResponse
import com.example.lagallens.data.model.response.RiskSnapshotResponse
import com.example.lagallens.data.model.response.TextDifferenceResponse
import com.example.lagallens.data.model.response.ContractResponse
import com.example.lagallens.domain.model.ContractChangeType
import com.example.lagallens.domain.model.ContractComparisonBlocker
import com.example.lagallens.domain.model.ContractComparisonSummary
import com.example.lagallens.domain.model.ContractComparisonUnavailableException
import com.example.lagallens.domain.model.ContractFieldValue
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteContractRepositoryTest {
    private val contract = ContractResponse(
        1, "Employment agreement", "ACTIVE", "Description",
        "2026-01-01T00:00:00.000Z", "2026-01-02T00:00:00.000Z", latestRiskLevel = "HIGH"
    )
    private val session = RemoteRepositorySession()
    private val api = ContractFixture()
    private val repository = RemoteContractRepository(session, ContractRemoteDataSource(lazyOf(api)))

    @Test fun readsAllPagesAndMapsSummaries() = runTest {
        api.total = 101
        val contracts = repository.observeContracts().first()

        assertEquals(listOf(1, 2), api.pages)
        assertEquals(listOf("1", "2"), contracts.map { it.id })
        assertEquals(EmploymentContractRisk.HIGH, contracts.first().risk)
    }

    @Test fun missingAndForbiddenDetailsReturnNull() = runTest {
        for (code in listOf(403, 404)) {
            api.getFailure = HttpException(Response.error<Unit>(code, "".toResponseBody()))
            assertNull(repository.getContract("1"))
        }
    }

    @Test fun detailUsesAnalysisSummaryFromTheCurrentSessionOnly() = runTest {
        api.current = contract.copy(latestAnalysisStatus = "PROCESSING")
        api.detailOverride = contract.copy(title = "Updated on server", latestRiskLevel = null)
        repository.observeContracts().first()

        val detail = checkNotNull(repository.forSession("session").getContract("1"))
        assertEquals("Updated on server", detail.title)
        assertEquals(EmploymentContractRisk.HIGH, detail.risk)
        assertEquals(EmploymentContractStatus.PROCESSING, detail.status)

        session.key = "new-session"
        val nextDetail = checkNotNull(repository.getContract("1"))
        assertEquals(EmploymentContractRisk.NOT_ANALYZED, nextDetail.risk)
        assertEquals(EmploymentContractStatus.COMPLETE, nextDetail.status)
    }

    @Test fun analysisFieldsFromDetailTakePrecedenceWhenPresent() = runTest {
        repository.observeContracts().first()
        api.detailOverride = contract.copy(latestRiskLevel = "LOW", latestAnalysisStatus = "PENDING")

        val detail = checkNotNull(repository.getContract("1"))
        assertEquals(EmploymentContractRisk.LOW, detail.risk)
        assertEquals(EmploymentContractStatus.PROCESSING, detail.status)
    }

    @Test fun renamePreservesDescriptionAndRefreshesCollectors() = runTest {
        val titles = Channel<String>(Channel.UNLIMITED)
        val collector = launch {
            repository.observeContracts().take(2).collect { titles.send(it.single().title) }
        }

        assertEquals("Employment agreement", titles.receive())
        assertTrue(repository.forSession("session").renameContract("1", " New title "))
        assertEquals("New title", titles.receive())
        assertEquals(UpdateContractRequest("New title", "Description"), api.lastUpdate)
        collector.join()
    }

    @Test fun createUsesServerIdAndOnlySendsContractMetadata() = runTest {
        val created = repository.createContract(contract.toContract().copy(id = "local", content = "Fixture OCR text"))

        assertEquals("2", created.id)
        assertEquals(CreateContractRequest("Employment agreement", "Description", "KHAC"), api.lastCreate)
    }

    @Test fun deleteAcceptsSuccessfulEnvelopeWithNullData() = runTest {
        assertTrue(repository.deleteContract("1"))
        assertEquals("1", api.deletedId)
    }

    @Test fun deletingAnAlreadyRemovedContractRefreshesCollectors() = runTest {
        val lists = Channel<List<String>>(Channel.UNLIMITED)
        val collector = launch {
            repository.observeContracts().take(2).collect { contracts -> lists.send(contracts.map { it.id }) }
        }
        assertEquals(listOf("1"), lists.receive())
        api.total = 0
        api.deleteFailure = HttpException(Response.error<Unit>(404, "".toResponseBody()))

        assertTrue(repository.deleteContract("1"))
        assertTrue(lists.receive().isEmpty())
        collector.join()
    }

    @Test fun boundRepositoryRejectsOperationsAfterSessionChanges() = runTest {
        val bound = repository.forSession("session")
        session.key = "new-session"

        assertTrue(runCatching { bound.getContract("1") }.exceptionOrNull() is IllegalStateException)
        assertTrue(api.detailIds.isEmpty())
    }

    private inner class ContractFixture : ContractApi {
        val pages = mutableListOf<Int>()
        val detailIds = mutableListOf<String>()
        var total = 1L
        var getFailure: HttpException? = null
        var deleteFailure: HttpException? = null
        var current = contract
        var detailOverride: ContractResponse? = null
        var lastUpdate: UpdateContractRequest? = null
        var lastCreate: CreateContractRequest? = null
        var deletedId: String? = null

        override suspend fun list(scope: AuthSessionScope, userId: String, page: Int, limit: Int, sort: String): ApiEnvelope<ContractPageResponse> {
            assertEquals("session", scope.key)
            assertEquals("42", userId)
            assertEquals(100, limit)
            assertEquals("updated_desc", sort)
            pages.add(page)
            val items = if (total == 0L) emptyList() else listOf(current.copy(id = page.toLong()))
            return ApiEnvelope(true, ContractPageResponse(items, total, limit))
        }

        override suspend fun get(scope: AuthSessionScope, id: String): ApiEnvelope<ContractResponse> {
            detailIds.add(id)
            getFailure?.let { throw it }
            return ApiEnvelope(true, detailOverride ?: current)
        }

        override suspend fun create(scope: AuthSessionScope, request: CreateContractRequest): ApiEnvelope<ContractResponse> {
            lastCreate = request
            return ApiEnvelope(true, current.copy(id = 2))
        }

        override suspend fun update(scope: AuthSessionScope, id: String, request: UpdateContractRequest): ApiEnvelope<ContractResponse> {
            lastUpdate = request
            current = current.copy(title = request.title)
            return ApiEnvelope(true, current)
        }

        override suspend fun delete(scope: AuthSessionScope, id: String): ApiEnvelope<Unit> {
            deletedId = id
            deleteFailure?.let { throw it }
            return ApiEnvelope(true, null)
        }

        val compared = mutableListOf<CompareContractsRequest>()
        var compareFailure: HttpException? = null
        var comparison = comparisonResponse()

        override suspend fun compare(scope: AuthSessionScope, request: CompareContractsRequest): ApiEnvelope<ContractComparisonResponse> {
            assertEquals("session", scope.key)
            compared += request
            compareFailure?.let { throw it }
            return ApiEnvelope(true, comparison)
        }
    }

    private fun comparisonResponse() = ContractComparisonResponse(
        first = ComparedContractResponse(1, 11, "Hợp đồng A", "a.pdf", "application/pdf", 3, 1_000L, 2_000L, "LOW"),
        second = ComparedContractResponse(2, 22, "Hợp đồng B", "b.pdf", "application/pdf", 4, 3_000L, 4_000L, "CRITICAL"),
        summary = ComparisonSummaryResponse(changedSections = 1, addedSections = 1, removedSections = 0, unchangedSections = 2),
        differences = listOf(
            TextDifferenceResponse("Điều 3. Tiền lương", "Lương 15 triệu", "Lương 18 triệu", "MODIFIED"),
            TextDifferenceResponse("Điều 9. Bảo mật", null, "Giữ bí mật", "ADDED"),
            TextDifferenceResponse("Điều 1. Thời hạn", "12 tháng", "12 tháng", "UNCHANGED")
        ),
        riskComparison = RiskComparisonResponse(RiskSnapshotResponse(20.0, "LOW"), RiskSnapshotResponse(88.5, "CRITICAL"), 68.5),
        overview = ComparisonOverviewResponse(listOf(
            FieldDifferenceResponse("basicSalary", "Mức lương cơ bản", "15000000.00", "18000000.00", "MODIFIED"),
            FieldDifferenceResponse("annualLeaveDays", "Ngày phép năm", null, 12.0, "ADDED"),
            FieldDifferenceResponse("socialInsurance", "Bảo hiểm xã hội", true, false, "MODIFIED")
        ))
    )

    private fun errorWithCode(status: Int, code: String) =
        HttpException(Response.error<Any>(status, """{"code":"$code","message":"x"}""".toResponseBody()))

    @Test fun comparingMapsTheBackendComparison() = runTest {
        val result = checkNotNull(repository.compareContracts("1", "2"))

        assertEquals(listOf(CompareContractsRequest(1, 2)), api.compared)
        assertEquals("Hợp đồng A", result.first.title)
        assertEquals(EmploymentContractRisk.CRITICAL, result.second.risk)
        assertEquals(ContractComparisonSummary(changedSections = 1, addedSections = 1, removedSections = 0, unchangedSections = 2), result.summary)
        assertEquals(
            listOf(ContractChangeType.MODIFIED, ContractChangeType.ADDED, ContractChangeType.UNCHANGED),
            result.differences.map { it.changeType }
        )
        assertEquals("", result.differences[1].firstText)
        assertEquals(68.5, checkNotNull(result.riskComparison).scoreDelta, 0.0)
        assertEquals(EmploymentContractRisk.CRITICAL, result.riskComparison?.second?.risk)
        // Whole numbers lose ".0"; booleans stay flags for the screen to word.
        assertEquals(listOf(ContractFieldValue.Text("15000000.00"), null, ContractFieldValue.Flag(true)), result.fieldDifferences.map { it.firstValue })
        assertEquals(listOf(ContractFieldValue.Text("18000000.00"), ContractFieldValue.Text("12"), ContractFieldValue.Flag(false)), result.fieldDifferences.map { it.secondValue })
    }

    @Test fun aComparisonWithoutRiskScoresHasNoRiskComparison() = runTest {
        api.comparison = comparisonResponse().copy(riskComparison = null)

        assertNull(repository.compareContracts("1", "2")?.riskComparison)
    }

    @Test fun comparingAContractWithItselfOrAnInvalidIdDoesNotCallTheBackend() = runTest {
        assertNull(repository.compareContracts("1", "1"))
        assertNull(repository.compareContracts("1", "abc"))

        assertTrue(api.compared.isEmpty())
    }

    @Test fun backendRefusalsBecomeReasonsTheScreenCanExplain() = runTest {
        val expected = mapOf(
            409 to "CONTRACT_DOCUMENT_REQUIRED" to ContractComparisonBlocker.DOCUMENT_MISSING,
            409 to "CONTRACT_OCR_NOT_READY" to ContractComparisonBlocker.OCR_NOT_READY,
            409 to "CONTRACT_DOCUMENT_UNAVAILABLE" to ContractComparisonBlocker.DOCUMENT_UNAVAILABLE,
            413 to "COMPARISON_CONTENT_TOO_LARGE" to ContractComparisonBlocker.TOO_LARGE,
            404 to "CONTRACT_NOT_FOUND" to ContractComparisonBlocker.NOT_FOUND
        )
        expected.forEach { (statusAndCode, blocker) ->
            api.compareFailure = errorWithCode(statusAndCode.first, statusAndCode.second)

            val failure = runCatching { repository.compareContracts("1", "2") }.exceptionOrNull()

            assertEquals(blocker, (failure as ContractComparisonUnavailableException).blocker)
        }
    }

    @Test fun anUnknownBackendErrorIsNotHiddenAsAReason() = runTest {
        api.compareFailure = errorWithCode(500, "SOMETHING_ELSE")

        assertTrue(runCatching { repository.compareContracts("1", "2") }.exceptionOrNull() is HttpException)
    }
}
