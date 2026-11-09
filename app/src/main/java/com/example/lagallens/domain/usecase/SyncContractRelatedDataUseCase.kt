package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.DraftRepository

/** Keeps chats and drafts consistent with their contract. */
class SyncContractRelatedDataUseCase(
    private val chatbot: ChatbotRepository,
    private val drafts: (String?) -> DraftRepository
) {
    suspend fun contractDeleted(session: String?, contractId: String) {
        chatbot.deleteConversationsForContract(contractId)
        val repository = drafts(session)
        repository.getDrafts().filter { it.contractId == contractId }.forEach { repository.deleteDraft(it.draftId) }
    }

    suspend fun contractRenamed(contractId: String, title: String) = chatbot.renameContract(contractId, title)
}
