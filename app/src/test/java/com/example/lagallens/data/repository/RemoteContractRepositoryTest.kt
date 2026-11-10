package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.ContractRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.ContractApi
import com.example.lagallens.data.mapper.toContract
import com.example.lagallens.data.model.request.CreateContractRequest
import com.example.lagallens.data.model.request.UpdateContractRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ContractPageResponse
import com.example.lagallens.data.model.response.ContractResponse
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
    }
}
