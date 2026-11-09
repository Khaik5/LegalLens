package com.example.lagallens

import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.model.analysis.AnalysisDetail
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.domain.model.processing.AnalysisJobState
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.AnalysisUnlockStatus
import com.example.lagallens.domain.repository.DraftAiRepository
import com.example.lagallens.domain.repository.DraftEditProposal
import com.example.lagallens.domain.repository.DraftEditRequest
import com.example.lagallens.domain.repository.DraftGenerationRequest
import com.example.lagallens.domain.repository.PaymentRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

internal open class TestAnalysisRepository : AnalysisRepository {
    override fun process(operationId: String, contractId: String, documentId: String, sessionKey: String): Flow<AnalysisJobState> = emptyFlow()
    override suspend fun getOverview(identity: AnalysisIdentity): AnalysisOverview = error("unexpected")
    override suspend fun getDetail(identity: AnalysisIdentity, language: String): AnalysisDetail = error("unexpected")
}

internal open class TestUnlockRepository(private val unlocked: Boolean = true, private val cost: Int = 2) : AnalysisUnlockRepository {
    override suspend fun status(sessionKey: String, resultId: String) = AnalysisUnlockStatus(unlocked, cost)
    override suspend fun unlock(sessionKey: String, resultId: String): AnalysisUnlockOutcome = AnalysisUnlockOutcome.Unlocked(0)
}

internal open class TestDraftAiRepository : DraftAiRepository {
    var generationRequests = mutableListOf<DraftGenerationRequest>()
    private val draft = Draft("draft-1", null, "D1", "content", DraftStage.ORIGINAL, 1, 0, 0, null, "lineage")
    override suspend fun generateDraft(request: DraftGenerationRequest): Draft {
        generationRequests += request
        delay(1)
        return draft
    }
    override suspend fun resolveSource(sourceDraftId: String?, sourceKey: String, contractId: String?) = draft
    override suspend fun regenerate(sourceDraftId: String?, sourceKey: String, contractId: String?) = draft
    val editRequests = mutableListOf<DraftEditRequest>()
    val appliedProposals = mutableListOf<String>()
    override suspend fun proposeEdit(request: DraftEditRequest): DraftEditProposal {
        editRequests += request
        return DraftEditProposal("proposal-1", "Clause", "before", "after", "reason")
    }
    override suspend fun applyEdit(proposalId: String, sourceDraftId: String?, contractId: String?): Draft {
        appliedProposals += proposalId
        return draft
    }
    override suspend fun copyVersion(sourceDraftId: String?, contractId: String?) = draft
}

internal open class TestPaymentRepository : PaymentRepository {
    val offers = listOf(5 to 29_000L, 15 to 79_000L, 30 to 149_000L, 100 to 449_000L).map { (credits, price) ->
        CreditOffer("credits_$credits", credits, price, "$credits Credits", "", credits == 15)
    }
    override suspend fun getPackages(): List<CreditOffer> = offers
    override suspend fun createPayment(paymentId: String, packageId: String) = PaymentRecord(paymentId, packageId, 0, 1, PaymentStatus.PENDING, 0)
    override fun observePayment(paymentId: String): Flow<PaymentRecord> = flowOf(PaymentRecord(paymentId, "test", 0, 1, PaymentStatus.SUCCESS, 0))
    override suspend fun cancelPayment(paymentId: String) = PaymentRecord(paymentId, "test", 0, 1, PaymentStatus.CANCELLED, 0)
    override suspend fun get(paymentId: String): PaymentRecord? = null
}
