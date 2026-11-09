package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.chatbot.ChatbotReply
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage

class FakeChatbotRepositoryTest {
    @Test fun retryRequiresExistingLaborConversation() = runBlocking {
        val repository = FakeChatbotRepository()
        assertTrue(repository.retryMessage("missing", "question") is ChatbotReply.RetryableError)
        val other = repository.openConversation("Other", "Loại khác")
        assertTrue(repository.retryMessage(other.id, "question") is ChatbotReply.NoEvidenceAnswer)
    }
    @Test fun englishContractTypeIsSupported() = runBlocking {
        val repository = FakeChatbotRepository()
        val conversation = repository.openConversation("Contract", "Employment contract")
        assertTrue(repository.sendMessage(conversation.id, "Question") is ChatbotReply.Answer)
    }

    @Test fun attachmentReplySaysTheFileWasNotRead() = runBlocking {
        val repository = FakeChatbotRepository()
        val conversation = repository.openConversation("Contract", "Employment contract")
        val attachment = com.example.lagallens.domain.model.chatbot.ChatbotAttachment("content://file", "file.pdf")
        val reply = repository.sendForSession(null, conversation.id, "Question", attachment) as ChatbotReply.Answer
        assertTrue(reply.content.contains("chưa được đọc"))
    }

    @Test fun contractDeleteAndRenameReachItsConversationsOnly() = runBlocking {
        val repository = FakeChatbotRepository()
        val message = ChatbotTranscriptMessage("1", true, "Q", 1)
        repository.saveMessages("a", "contract-a", "Old A", "Employment contract", listOf(message))
        repository.saveMessages("b", "contract-b", "Old B", "Employment contract", listOf(message))
        repository.renameContract("contract-a", "New A")
        assertEquals("New A", repository.getConversationsIncludingShort().first { it.conversationId == "a" }.contractName)
        repository.deleteConversationsForContract("contract-a")
        assertEquals(listOf("b"), repository.getConversationsIncludingShort().map { it.conversationId })
    }

    private suspend fun FakeChatbotRepository.getConversationsIncludingShort() =
        listOf("a", "b").mapNotNull { id -> getMessages(id).takeIf { it.isNotEmpty() }?.let { id } }.let { ids ->
            ids.map { id -> com.example.lagallens.domain.model.chatbot.ChatbotTranscript(id, null, memoryName(id), "", 0, emptyList()) }
        }

    private fun FakeChatbotRepository.memoryName(id: String) = FakeChatbotRepository::class.java.getDeclaredField("memory").let { field ->
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(this) as Map<String, com.example.lagallens.domain.model.chatbot.ChatbotTranscript>).getValue(id).contractName
    }

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
