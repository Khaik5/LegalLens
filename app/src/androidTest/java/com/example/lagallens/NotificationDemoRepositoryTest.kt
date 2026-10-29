package com.example.lagallens

import androidx.test.platform.app.InstrumentationRegistry
import com.example.lagallens.data.repository.FakeNotificationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDemoRepositoryTest {
    @Test fun readStatePersistsAcrossRepositoryInstances() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("notification_demo_state", 0)
        val previous = preferences.getStringSet("read_notification_ids", emptySet()).orEmpty().toSet()
        try {
            preferences.edit().remove("read_notification_ids").commit()
            val first = FakeNotificationRepository(context)
            assertTrue(first.getAll().any { it.isUnread })
            first.markRead("analysis-bitexco")
            assertFalse(FakeNotificationRepository(context).getAll().first { it.id == "analysis-bitexco" }.isUnread)
            FakeNotificationRepository(context).markAllRead()
            assertTrue(FakeNotificationRepository(context).getAll().none { it.isUnread })
        } finally {
            preferences.edit().putStringSet("read_notification_ids", previous).commit()
        }
    }
}
