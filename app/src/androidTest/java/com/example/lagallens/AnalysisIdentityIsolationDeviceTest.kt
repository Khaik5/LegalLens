package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.SessionScopedEmploymentRepositories
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Uses a fresh auth preference and random account identities; does not delete app data. */
@RunWith(AndroidJUnit4::class)
class AnalysisIdentityIsolationDeviceTest {
    @Test fun snapshotsPersistAndRemainScopedToTheAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        check(context.packageName == "com.example.lagallens.testsandbox")
        val unique = UUID.randomUUID().toString()
        val auth = AuthLocalDataSource(context, "analysis_test_auth_$unique")
        check(auth.saveSession("$unique-a@example.invalid"))
        val firstRepo = SessionScopedEmploymentRepositories(context, auth).analysisIdentities
        val sessionA = checkNotNull(firstRepo.sessionKey())
        val first = firstRepo.create("operation-a-$unique", "collaborator", "document-collaborator", sessionA)
        assertNotNull(first.snapshot)
        assertEquals(auth.currentSessionUserId(), first.snapshot?.get("identity:accountId"))
        assertEquals(first.resultId, first.snapshot?.get("identity:resultId"))
        val mapper = com.example.lagallens.data.mapper.DemoAnalysisSnapshotMapper("vi", "unavailable")
        assertTrue(runCatching { mapper.toDetail(first) }.isSuccess)
        assertFalse(runCatching { mapper.toDetail(first.copy(snapshot = first.snapshot!! - "layout:summary")) }.isSuccess)
        val recreated = SessionScopedEmploymentRepositories(context, AuthLocalDataSource(context, "analysis_test_auth_$unique")).analysisIdentities
        assertEquals(first, recreated.get(first.resultId, sessionA))
        check(auth.saveSession("$unique-b@example.invalid"))
        val sessionB = checkNotNull(recreated.sessionKey())
        val second = recreated.create("operation-b-$unique", "collaborator", "document-collaborator", sessionB)
        assertNotEquals(first.resultId, second.resultId)
        assertNull(recreated.get(first.resultId, sessionB))
        assertTrue(runCatching { recreated.get(first.resultId, sessionA) }.isFailure)
        check(auth.saveSession("$unique-a@example.invalid"))
        val newSessionA = checkNotNull(recreated.sessionKey())
        assertEquals(first, recreated.get(first.resultId, newSessionA))
        assertNull(recreated.get("missing-$unique", newSessionA))
    }
}
