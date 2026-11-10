package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.DocumentApi
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.DocumentResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class DocumentRemoteDataSourceTest {
    private val scope = AuthSessionScope("session")
    private val api = DocumentApiFixture()
    private val dataSource = DocumentRemoteDataSource(lazyOf(api))

    @Test fun missingDocumentReturnsNullInsteadOfFailing() = runTest {
        api.failure = httpError(404)

        assertNull(dataSource.get(scope, "9"))
        assertFalse(dataSource.delete(scope, "9"))
    }

    @Test fun otherHttpFailuresAreRethrown() = runTest {
        api.failure = httpError(403)
        try {
            dataSource.get(scope, "9")
            fail("Expected HttpException")
        } catch (error: HttpException) {
            assertEquals(403, error.code())
        }
    }

    @Test fun listUnwrapsEnvelope() = runTest {
        assertEquals(listOf(7L), dataSource.list(scope, "3").map { it.id })
        assertEquals("3", api.lastContractId)
    }

    private fun httpError(code: Int) = HttpException(Response.error<Unit>(code, "".toResponseBody()))

    private class DocumentApiFixture : DocumentApi {
        var failure: HttpException? = null
        var lastContractId: String? = null
        private val document = DocumentResponse(7, 3, "a.pdf", "PDF", 10, "2026-11-10T00:00:00.000Z")

        override suspend fun upload(scope: AuthSessionScope, contractId: RequestBody, file: MultipartBody.Part) =
            ApiEnvelope(true, document)

        override suspend fun list(scope: AuthSessionScope, contractId: String): ApiEnvelope<List<DocumentResponse>> {
            lastContractId = contractId
            return ApiEnvelope(true, listOf(document))
        }

        override suspend fun get(scope: AuthSessionScope, id: String): ApiEnvelope<DocumentResponse> {
            failure?.let { throw it }
            return ApiEnvelope(true, document)
        }

        override suspend fun delete(scope: AuthSessionScope, id: String): ApiEnvelope<Unit> {
            failure?.let { throw it }
            return ApiEnvelope(true, Unit)
        }

        override suspend fun preview(scope: AuthSessionScope, id: String): ResponseBody = "".toResponseBody()
        override suspend fun download(scope: AuthSessionScope, id: String): ResponseBody = "".toResponseBody()
    }
}
