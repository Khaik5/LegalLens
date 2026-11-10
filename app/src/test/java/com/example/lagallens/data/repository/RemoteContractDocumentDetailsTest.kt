package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.DocumentFiles
import com.example.lagallens.data.datasource.remote.ContractRemoteDataSource
import com.example.lagallens.data.datasource.remote.DocumentRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.ContractApi
import com.example.lagallens.data.datasource.remote.api.DocumentApi
import com.example.lagallens.data.model.request.CreateContractRequest
import com.example.lagallens.data.model.request.UpdateContractRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ContractPageResponse
import com.example.lagallens.data.model.response.ContractResponse
import com.example.lagallens.data.model.response.DocumentResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RemoteContractDocumentDetailsTest {
    private val documentsApi = DocumentsFixture()
    private val repository = RemoteContractRepository(
        RemoteRepositorySession(),
        ContractRemoteDataSource(lazyOf(ContractFixture())),
        DocumentRemoteDataSource(lazyOf(documentsApi))
    )

    private fun document(id: Long, fileName: String, fileType: String, uploadedAt: String) =
        DocumentResponse(id, 1, fileName, fileType, 100, uploadedAt)

    @Test fun detailShowsTheLatestDocumentFormat() = runTest {
        documentsApi.documents = listOf(
            document(1, "old.docx", "WORD", "2026-11-01T00:00:00.000Z"),
            document(2, "Hợp đồng.pdf", "PDF", "2026-11-10T00:00:00.000Z")
        )

        val detail = checkNotNull(repository.getContract("1"))

        assertEquals("2", detail.documentId)
        assertEquals("Hợp đồng.pdf", detail.fileName)
        assertEquals(DocumentFiles.PDF, detail.mimeType)
        assertNull(detail.pageCount)
        assertEquals("1", documentsApi.requestedContractId)
    }

    @Test fun pdfShowsThePageCountFromTheBackend() = runTest {
        documentsApi.documents = listOf(
            document(4, "Hợp đồng đã quét.pdf", "PDF", "2026-11-10T00:00:00.000Z")
                .copy(mimeType = DocumentFiles.PDF, pageCount = 5)
        )

        val detail = checkNotNull(repository.getContract("1"))

        assertEquals(5, detail.pageCount)
        assertEquals("Hợp đồng đã quét.pdf", detail.fileName)
    }

    @Test fun wordWithoutPageCountStaysUnknown() = runTest {
        documentsApi.documents = listOf(
            document(5, "Hợp đồng.doc", "WORD", "2026-11-10T00:00:00.000Z").copy(mimeType = DocumentFiles.DOC)
        )

        val detail = checkNotNull(repository.getContract("1"))

        assertEquals(DocumentFiles.DOC, detail.mimeType)
        assertNull(detail.pageCount)
    }

    @Test fun imageCountsAsOnePage() = runTest {
        documentsApi.documents = listOf(document(3, "scan.jpg", "IMAGE", "2026-11-10T00:00:00.000Z"))

        val detail = checkNotNull(repository.getContract("1"))

        assertEquals(DocumentFiles.JPEG, detail.mimeType)
        assertEquals(1, detail.pageCount)
    }

    @Test fun contractWithoutDocumentKeepsEmptyFileDetails() = runTest {
        val detail = checkNotNull(repository.getContract("1"))

        assertEquals("", detail.fileName)
        assertEquals("", detail.mimeType)
    }

    @Test fun documentListFailureStillShowsTheContract() = runTest {
        for (failure in listOf(IOException("offline"), HttpException(Response.error<Unit>(500, "".toResponseBody())))) {
            documentsApi.failure = failure
            val detail = checkNotNull(repository.getContract("1"))
            assertEquals("Employment agreement", detail.title)
            assertEquals("", detail.mimeType)
        }
    }

    private class ContractFixture : ContractApi {
        private val contract = ContractResponse(
            1, "Employment agreement", "DRAFT", null,
            "2026-11-10T00:00:00.000Z", "2026-11-10T00:00:00.000Z"
        )

        override suspend fun get(scope: AuthSessionScope, id: String) = ApiEnvelope(true, contract)
        override suspend fun list(scope: AuthSessionScope, userId: String, page: Int, limit: Int, sort: String) =
            ApiEnvelope(true, ContractPageResponse(listOf(contract), 1, limit))
        override suspend fun create(scope: AuthSessionScope, request: CreateContractRequest) = ApiEnvelope(true, contract)
        override suspend fun update(scope: AuthSessionScope, id: String, request: UpdateContractRequest) = ApiEnvelope(true, contract)
        override suspend fun delete(scope: AuthSessionScope, id: String): ApiEnvelope<Unit> = ApiEnvelope(true, null)
    }

    private class DocumentsFixture : DocumentApi {
        var documents = emptyList<DocumentResponse>()
        var failure: Exception? = null
        var requestedContractId: String? = null

        override suspend fun list(scope: AuthSessionScope, contractId: String): ApiEnvelope<List<DocumentResponse>> {
            requestedContractId = contractId
            failure?.let { throw it }
            return ApiEnvelope(true, documents)
        }

        override suspend fun upload(scope: AuthSessionScope, contractId: RequestBody, file: MultipartBody.Part) =
            error("Unexpected upload")
        override suspend fun get(scope: AuthSessionScope, id: String) = error("Unexpected get")
        override suspend fun delete(scope: AuthSessionScope, id: String) = error("Unexpected delete")
        override suspend fun preview(scope: AuthSessionScope, id: String): ResponseBody = error("Unexpected preview")
        override suspend fun download(scope: AuthSessionScope, id: String): ResponseBody = error("Unexpected download")
    }
}
