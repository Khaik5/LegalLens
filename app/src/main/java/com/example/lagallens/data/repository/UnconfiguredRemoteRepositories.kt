package com.example.lagallens.data.repository

import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.core.network.BackendNotConfiguredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnconfiguredRemoteDraftRepository : DraftRepository {
    override suspend fun getDrafts(): List<Draft> = throw BackendNotConfiguredException("Draft")
    override suspend fun getDraft(draftId: String): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun getLatestDraft(stage: DraftStage): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun saveDraft(stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?, conversationId: String?, isSaved: Boolean, isTitleGenerated: Boolean): Draft =
        throw BackendNotConfiguredException("Draft")
    override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage, isTitleGenerated: Boolean): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun updateDraft(draftId: String, title: String, content: String): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun deleteDraft(draftId: String): Boolean = throw BackendNotConfiguredException("Draft")
}

class UnconfiguredRemoteCreditRepository(private val sessionKey: () -> String? = { null }) : CreditRepository {
    override fun activeSessionKey(): String? = sessionKey()
    override val isConfigured: Boolean = false
    override val balance = MutableStateFlow(0).asStateFlow()
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Credit")
    override suspend fun getTransactions(): List<CreditTransaction> = unavailable()
    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = unavailable()
}
