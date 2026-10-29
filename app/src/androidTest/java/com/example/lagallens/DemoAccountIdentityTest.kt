package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DemoAccountIdentityTest {
    @Test fun accountIdsAndCredentialsSurviveSwitching() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "test_auth_" + UUID.randomUUID()
        val source = AuthLocalDataSource(context, name)
        assertTrue(source.savePasswordCredential("a@example.test", "salt-a", "hash-a"))
        assertTrue(source.savePasswordCredential("b@example.test", "salt-b", "hash-b"))
        assertTrue(source.saveSession("a@example.test"))
        val a = source.currentSessionUserId()
        assertNotNull(a)
        assertTrue(source.clearSession())
        assertNull(source.currentSessionUserId())
        assertTrue(source.saveSession("b@example.test"))
        assertNotEquals(a, source.currentSessionUserId())
        assertTrue(source.saveSession("a@example.test"))
        assertEquals(a, source.currentSessionUserId())
        assertEquals("salt-a" to "hash-a", source.readPasswordCredential("a@example.test"))
        assertEquals("salt-b" to "hash-b", source.readPasswordCredential("b@example.test"))
    }
}
