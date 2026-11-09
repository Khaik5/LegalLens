package com.example.lagallens

import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.repository.FakeNotificationRepository
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDemoRepositoryTest {
    @Test fun readStatePersistsAcrossRepositoryInstances() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val account = java.util.UUID.randomUUID().toString()
        val name = AccountStorageName.forUser("notification_demo_state", account)
        try {
            val first = FakeNotificationRepository(context) { account }
            assertTrue(first.getAll().any { it.isUnread })
            first.markRead("analysis-bitexco")
            assertFalse(FakeNotificationRepository(context) { account }.getAll().first { it.id == "analysis-bitexco" }.isUnread)
            FakeNotificationRepository(context) { account }.markAllRead()
            assertTrue(FakeNotificationRepository(context) { account }.getAll().none { it.isUnread })
        } finally {
            context.deleteSharedPreferences(name)
        }
    }
}
