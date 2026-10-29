package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.repository.SessionScopedEmploymentRepositories
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EmploymentAccountIsolationTest {
    @Test fun accountsKeepSeparateContractsAndDocuments() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val auth = AuthLocalDataSource(context, "test_auth_${UUID.randomUUID()}")
        val repositories = SessionScopedEmploymentRepositories(context, auth)
        assertTrue(auth.saveSession("a@example.test"))
        repositories.contracts.createContract(EmploymentContract("unique-a", "doc-a", "A", "private", 1, 1))
        assertTrue(auth.clearSession())
        try { repositories.contracts.getContract("unique-a"); fail("expected no session") }
        catch (_: IllegalStateException) { }
        assertTrue(auth.saveSession("b@example.test"))
        assertNull(repositories.contracts.getContract("unique-a"))
        repositories.documents.saveDocument(EmploymentDocument("unique-b", null, "B", null, "text/plain", "private B", 1))
        assertTrue(auth.saveSession("a@example.test"))
        assertEquals("private", repositories.contracts.getContract("unique-a")?.content)
        assertNotNull(repositories.documents.getDocument("doc-a"))
        assertNull(repositories.documents.getDocument("unique-b"))
    }
}
