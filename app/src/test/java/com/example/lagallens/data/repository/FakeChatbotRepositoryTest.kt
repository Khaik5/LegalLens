package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.chatbot.ChatbotReply
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage

class FakeChatbotRepositoryTest {
    @Test
    fun demoConversationUsesUniqueIdsAndClearlyLabelsNonLegalAnswer() = runBlocking {
        val repository = FakeChatbotRepository()
        val first = repository.openConversation("HĐ Lao động mẫu", "Hợp đồng lao động")
        val second = repository.openConversation("HĐ Lao động khác", "Hợp đồng lao động")
        val question = "Thời hạn hợp đồng là bao lâu?"

        assertTrue(first.id != second.id)
        val reply = repository.sendMessage(first.id, question) as ChatbotReply.Answer
        assertTrue(reply.content.contains("[DEMO]"))
        assertTrue(reply.content.contains("không phải tư vấn pháp lý"))
    }

    @Test
    fun savedTranscriptCanBeReadAgainByConversationId() = runBlocking {
        val repository = FakeChatbotRepository()
        val conversation = repository.openConversation("HĐ Lao động mẫu", "Hợp đồng lao động", "office-lease")
        val messages = listOf(
            ChatbotTranscriptMessage("1", false, "[DEMO] Xin chào", 1L),
            ChatbotTranscriptMessage("2", true, "Thời hạn hợp đồng thế nào?", 2L)
        )

        repository.saveMessages(conversation.id, "office-lease", "HĐ Lao động mẫu", "Hợp đồng lao động", messages)

        assertEquals(messages, repository.getMessages(conversation.id))
        assertEquals("office-lease", repository.getConversations().single().contractId)
    }
}
