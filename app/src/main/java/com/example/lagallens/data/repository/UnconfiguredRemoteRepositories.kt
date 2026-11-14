package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.AnalysisUnlockStatus
import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.core.network.BackendNotConfiguredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.lagallens.domain.model.processing.AnalysisJobState

/** The backend has no user API to create or read analysis jobs yet. */
class UnconfiguredRemoteAnalysisRepository : AnalysisRepository {
    override val isAvailable: Boolean = false
    override suspend fun getOverview(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity): AnalysisOverview =
        throw BackendNotConfiguredException("Analysis")
    override suspend fun getDetail(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, language: String): com.example.lagallens.domain.model.analysis.AnalysisDetail =
        throw BackendNotConfiguredException("Analysis")
    override fun process(operationId: String, contractId: String, documentId: String, sessionKey: String): Flow<AnalysisJobState> =
        flow { throw BackendNotConfiguredException("Analysis") }
}


class UnconfiguredRemoteAnalysisUnlockRepository : AnalysisUnlockRepository {
    override suspend fun status(sessionKey: String, resultId: String): AnalysisUnlockStatus = throw BackendNotConfiguredException("Analysis unlock")
    override suspend fun unlock(sessionKey: String, resultId: String): AnalysisUnlockOutcome = AnalysisUnlockOutcome.Unavailable
}

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
