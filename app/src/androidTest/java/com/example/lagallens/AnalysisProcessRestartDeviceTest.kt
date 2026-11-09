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

/** Two individually invoked tests with a process kill between them; no data is removed. */
@RunWith(AndroidJUnit4::class)
class AnalysisProcessRestartDeviceTest {
    @Test fun prepare() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        check(context.packageName == "com.example.lagallens.testsandbox")
        val auth = AuthLocalDataSource(context)
        check(auth.saveSession("restart-${UUID.randomUUID()}@example.invalid"))
        val repo = SessionScopedEmploymentRepositories(context, auth).analysisIdentities
        val session = checkNotNull(repo.sessionKey())
        val identity = repo.create(UUID.randomUUID().toString(), "collaborator", "document-collaborator", session)
        check(context.getSharedPreferences("analysis_process_test_fixture", android.content.Context.MODE_PRIVATE).edit()
            .putString("result", identity.resultId).putString("session", session).commit())
    }
    @Test fun verifyAfterProcessRestart() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        check(context.packageName == "com.example.lagallens.testsandbox")
        val preferences = context.getSharedPreferences("analysis_process_test_fixture", android.content.Context.MODE_PRIVATE)
        val resultId = checkNotNull(preferences.getString("result", null))
        val session = checkNotNull(preferences.getString("session", null))
        val repo = SessionScopedEmploymentRepositories(context).analysisIdentities
        val identity = checkNotNull(repo.get(resultId, session))
        assertEquals(resultId, identity.resultId)
        assertTrue(runCatching { com.example.lagallens.data.mapper.DemoAnalysisSnapshotMapper("vi", "unavailable").toDetail(identity) }.isSuccess)
        assertNotNull(identity.snapshot?.get("vi:analysis_summary_contract_type_title"))
    }
}
