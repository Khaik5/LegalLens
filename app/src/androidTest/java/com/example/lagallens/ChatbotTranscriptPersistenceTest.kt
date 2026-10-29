package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChatbotTranscriptPersistenceTest {
    @Test fun transcriptRestoresByConversationAndContractIds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val conversationId = UUID.randomUUID().toString()
        val contractId = "contract-$conversationId"
        val repository = FakeChatbotRepository(context)
        val messages = listOf(
            ChatbotTranscriptMessage("1", false, "[DEMO] Xin chào", 1L),
            ChatbotTranscriptMessage("2", true, "Hợp đồng lao động có thời hạn thế nào?", 2L)
        )

        repository.openConversation("HĐ lao động mẫu", "Hợp đồng lao động", contractId, conversationId)
        repository.saveMessages(conversationId, contractId, "HĐ lao động mẫu", "Hợp đồng lao động", messages)
        val restored = FakeChatbotRepository(context)
        assertEquals(messages, restored.getMessages(conversationId))
        assertEquals(contractId, restored.getConversations().single { it.conversationId == conversationId }.contractId)
        restored.deleteConversation(conversationId)
        assertEquals(emptyList<ChatbotTranscriptMessage>(), FakeChatbotRepository(context).getMessages(conversationId))
    }
}
