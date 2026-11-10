package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.NotificationRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.NotificationApi
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.NotificationPageResponse
import com.example.lagallens.data.model.response.NotificationResponse
import com.example.lagallens.domain.model.NotificationKind
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RemoteNotificationRepositoryTest {
    private val session = RemoteRepositorySession()
    private val api = NotificationFixture()
    private val repository = RemoteNotificationRepository(session, NotificationRemoteDataSource(lazyOf(api))) { type -> "title:$type" }

    private fun notification(id: Long, type: String?, isRead: Boolean = false, contractId: Long? = null) =
        NotificationResponse(id, type, "Hợp đồng \"A\" đã được phân tích.", isRead, contractId, "2026-11-10T09:30:00.000Z")

    @Test fun mapsBackendFieldsToTheAppModel() = runTest {
        api.items = listOf(notification(7, "ANALYSIS_DONE", isRead = false, contractId = 12))

        val record = repository.getAllForSession("session").single()

        assertEquals("7", record.id)
        assertEquals("12", record.contractId)
        assertEquals(NotificationKind.ANALYSIS, record.kind)
        assertTrue(record.isUnread)
        assertEquals("title:ANALYSIS_DONE", record.title)
        assertEquals("Hợp đồng \"A\" đã được phân tích.", record.description)
        assertEquals(1_794_303_000_000L, record.createdAtMillis)
    }

    @Test fun mapsEveryBackendTypeToAKind() = runTest {
        api.items = listOf(
            notification(1, "ANALYSIS_DONE"),
            notification(2, "HIGH_RISK_FOUND"),
            notification(3, "CONTRACT_EXPIRING", isRead = true),
            notification(4, "SYSTEM"),
            notification(5, "SOMETHING_NEW"),
            notification(6, null)
        )

        val records = repository.getAll()

        assertEquals(
            listOf(NotificationKind.ANALYSIS, NotificationKind.ANALYSIS, NotificationKind.REMINDER,
                NotificationKind.SYSTEM, NotificationKind.SYSTEM, NotificationKind.SYSTEM),
            records.map { it.kind }
        )
        assertFalse(records[2].isUnread)
        assertNull(records[0].contractId)
    }

    @Test fun readsEveryPage() = runTest {
        api.items = (1L..150L).map { notification(it, "SYSTEM") }

        val records = repository.getAll()

        assertEquals(150, records.size)
        assertEquals(listOf(1, 2), api.pages)
    }

    @Test fun markReadAndMarkAllReadCallTheBackend() = runTest {
        repository.markReadForSession("session", "7")
        repository.markAllReadForSession("session")

        assertEquals(listOf("7"), api.readIds)
        assertEquals(1, api.readAllCalls)
    }

    @Test fun failedMarkReadIsReported() = runTest {
        api.markReadCode = 404
        try {
            repository.markRead("9")
            fail("Expected HttpException")
        } catch (error: HttpException) {
            assertEquals(404, error.code())
        }
    }

    @Test fun requestForAnotherSessionIsRejectedBeforeCallingTheBackend() = runTest {
        val failure = runCatching { repository.getAllForSession("old-session") }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(api.pages.isEmpty())
    }

    private class NotificationFixture : NotificationApi {
        var items = emptyList<NotificationResponse>()
        val pages = mutableListOf<Int>()
        val readIds = mutableListOf<String>()
        var readAllCalls = 0
        var markReadCode = 204

        override suspend fun list(scope: AuthSessionScope, page: Int, limit: Int): ApiEnvelope<NotificationPageResponse> {
            pages += page
            val slice = items.drop((page - 1) * limit).take(limit)
            return ApiEnvelope(true, NotificationPageResponse(slice, items.size.toLong(), page, limit))
        }

        override suspend fun markRead(scope: AuthSessionScope, id: String): Response<Unit> {
            readIds += id
            return if (markReadCode == 204) Response.success(204, Unit)
            else Response.error(markReadCode, "".toResponseBody())
        }

        override suspend fun markAllRead(scope: AuthSessionScope): Response<Unit> {
            readAllCalls++
            return Response.success(204, Unit)
        }
    }
}
