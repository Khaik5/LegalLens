package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.data.repository.FakeNotificationRepository
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AccountScopedChatNotificationTest {
    @Test fun switchesAccountsWithoutLosingChatOrReadState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val a = UUID.randomUUID().toString()
        val b = UUID.randomUUID().toString()
        var current: String? = a
        val chat = FakeChatbotRepository(context) { current }
        val notifications = FakeNotificationRepository(context) { current }
        chat.saveMessages("conversation-a", null, "A", "lao động", listOf(
            ChatbotTranscriptMessage("1", true, "private A", 1),
            ChatbotTranscriptMessage("2", false, "demo", 2)
        ))
        val firstId = notifications.getAll().first { it.isUnread }.id
        notifications.markRead(firstId)
        chat.clearSessionData()
        current = b
        assertTrue(chat.getConversations().isEmpty())
        assertTrue(notifications.getAll().first { it.id == firstId }.isUnread)
        current = a
        assertEquals("private A", chat.getMessages("conversation-a").first().content)
        assertFalse(notifications.getAll().first { it.id == firstId }.isUnread)
        current = null
        var rejected = false
        try { chat.getConversations() } catch (_: IllegalStateException) { rejected = true }
        assertTrue(rejected)
    }
}
