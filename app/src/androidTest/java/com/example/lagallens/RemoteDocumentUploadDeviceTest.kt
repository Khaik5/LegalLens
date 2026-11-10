package com.example.lagallens

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.core.network.AuthHeaderInterceptor
import com.example.lagallens.data.datasource.local.datastore.AccessSession
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.EncryptedTokenStore
import com.example.lagallens.data.datasource.remote.ContractRemoteDataSource
import com.example.lagallens.data.datasource.remote.DocumentRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.ContractApi
import com.example.lagallens.data.datasource.remote.api.DocumentApi
import com.example.lagallens.data.repository.RemoteContractRepository
import com.example.lagallens.data.repository.RemoteDocumentUploadRepository
import com.example.lagallens.domain.model.processing.DocumentUploadRequest
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.model.processing.UploadFailure
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

@RunWith(AndroidJUnit4::class)
class RemoteDocumentUploadDeviceTest {
    @Test
    fun createsContractBeforeMultipartUploadAndRollsBackRejections() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tokens = object : AuthSessionStore by EncryptedTokenStore(context) {
            override fun accessSession() = AccessSession("test-session", "test-token")
        }
        val requests = mutableListOf<String>()
        var uploadStatus = 201
        val retrofit = Retrofit.Builder()
            .baseUrl("https://documents.example.invalid/")
            .client(OkHttpClient.Builder().addInterceptor(AuthHeaderInterceptor(tokens)).addInterceptor { chain ->
                val request = chain.request()
                assertEquals("Bearer test-token", request.header("Authorization"))
                val route = "${request.method} ${request.url.encodedPath}"
                requests.add(route)
                val response = when (route) {
                    "POST /contracts" -> {
                        val body = Buffer().also { checkNotNull(request.body).writeTo(it) }.readUtf8()
                        assertTrue(body.contains("\"title\":\"fixture\""))
                        201 to """{"success":true,"data":{"id":42,"title":"fixture","status":"DRAFT","createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}}"""
                    }
                    "POST /documents/upload" -> {
                        val body = Buffer().also { checkNotNull(request.body).writeTo(it) }.readUtf8()
                        assertTrue(body.contains("name=\"contractId\""))
                        assertTrue(body.contains("\r\n42\r\n"))
                        assertTrue(body.contains("name=\"file\"; filename=\"fixture.pdf\""))
                        assertTrue(body.contains("%PDF-"))
                        uploadStatus to if (uploadStatus == 201) """{"success":true,"data":{"id":8}}""" else """{"success":false}"""
                    }
                    "DELETE /contracts/42" -> 200 to """{"success":true,"data":null}"""
                    else -> error("Unexpected route")
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(response.first).message("Test response")
                    .body(response.second.toResponseBody("application/json".toMediaType())).build()
            }.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val contracts = RemoteContractRepository(tokens, ContractRemoteDataSource(lazy { retrofit.create(ContractApi::class.java) }))
        val repository = RemoteDocumentUploadRepository(
            context, tokens, contracts, DocumentRemoteDataSource(lazy { retrofit.create(DocumentApi::class.java) })
        )
        val input = File.createTempFile("upload-test-", ".pdf", context.cacheDir)
        try {
            input.writeText("%PDF-" + " ".repeat(32_768))
            val request = DocumentUploadRequest(
                "local-id", "fixture.pdf", "application/pdf", listOf(Uri.fromFile(input).toString()), sessionKey = "test-session"
            )

            val success = repository.upload(request).toList()
            assertEquals(listOf("POST /contracts", "POST /documents/upload"), requests)
            assertTrue(success.filterIsInstance<ProcessingState.Running>().any { (it.progressPercent ?: 0) in 1..99 })
            assertEquals(ProcessingState.Uploaded("42", "8"), success.last())

            for ((status, reason) in listOf(400 to UploadFailure.UNSUPPORTED_FORMAT, 413 to UploadFailure.TOO_LARGE)) {
                requests.clear()
                uploadStatus = status
                val failure = repository.upload(request).toList()
                assertEquals(listOf("POST /contracts", "POST /documents/upload", "DELETE /contracts/42"), requests)
                assertEquals(ProcessingState.Failed(isRetryable = false, reason = reason), failure.last())
            }
        } finally {
            input.delete()
        }
    }
}
