package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.AnalysisUnlockStatus
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.OcrRepository
import com.example.lagallens.domain.model.NotificationRecord
import com.example.lagallens.domain.repository.NotificationRepository
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.core.network.BackendNotConfiguredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import com.example.lagallens.domain.model.processing.AnalysisJobState
import com.example.lagallens.domain.model.processing.ProcessingState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.example.lagallens.data.datasource.local.datastore.EncryptedTokenStore
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpPurpose
import java.security.MessageDigest

/** Opaque session identity for one access token; the token itself is never exposed. */
internal fun tokenSessionKey(tokens: EncryptedTokenStore): String? = tokens.accessToken()?.let { token ->
    MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

class UnconfiguredRemoteAuthRepository(private val tokens: EncryptedTokenStore) : AuthRepository {
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Auth")
    override fun currentSessionKey(): String? = tokenSessionKey(tokens)
    override fun observeSessionKeys(): Flow<String?> = tokens.observeTokenChanges().map { tokenSessionKey(tokens) }.distinctUntilChanged()
    override suspend fun authenticate(username: String, password: String): Boolean = unavailable()
    override suspend fun register(fullName: String, email: String, password: String): AuthResult = unavailable()
    override suspend fun resendOtp(email: String, purpose: OtpPurpose): AuthResult = unavailable()
    override suspend fun verifyOtp(email: String, code: String, purpose: OtpPurpose): AuthResult = unavailable()
    override suspend fun requestPasswordReset(email: String): AuthResult = unavailable()
    override suspend fun confirmPasswordReset(email: String, newPassword: String): AuthResult = unavailable()
    override suspend fun logout(): Boolean { tokens.clear(); return true }
    override suspend fun changePassword(currentPassword: String, newPassword: String): Boolean = unavailable()
}

class UnconfiguredRemoteChatbotRepository(private val sessionKey: () -> String? = { null }) : ChatbotRepository {
    override fun currentSessionKey(): String? = sessionKey()
    override suspend fun openConversation(contractName: String, contractType: String, contractId: String?, conversationId: String?): ChatbotConversation =
        throw BackendNotConfiguredException("Chatbot")
    override suspend fun sendMessage(conversationId: String, message: String): ChatbotReply =
        throw BackendNotConfiguredException("Chatbot")
    override suspend fun retryMessage(conversationId: String, message: String): ChatbotReply =
        throw BackendNotConfiguredException("Chatbot")
    override suspend fun getConversations(): List<com.example.lagallens.domain.model.chatbot.ChatbotTranscript> =
        throw BackendNotConfiguredException("Chatbot")
    override suspend fun getMessages(conversationId: String): List<com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage> =
        throw BackendNotConfiguredException("Chatbot")
    override suspend fun saveMessages(
        conversationId: String,
        contractId: String?,
        contractName: String,
        contractType: String,
        messages: List<com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage>
    ) = throw BackendNotConfiguredException("Chatbot")
}

class UnconfiguredRemoteOcrRepository : OcrRepository {
    override fun process(documentId: String): Flow<ProcessingState> = flow { throw BackendNotConfiguredException("OCR") }
    override suspend fun getResult(documentId: String): OcrResult =
        throw BackendNotConfiguredException("OCR")
}

class UnconfiguredRemoteAnalysisRepository : AnalysisRepository {
    override suspend fun getOverview(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity): AnalysisOverview =
        throw BackendNotConfiguredException("Analysis")
    override suspend fun getDetail(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, language: String): com.example.lagallens.domain.model.analysis.AnalysisDetail =
        throw BackendNotConfiguredException("Analysis")
    override fun process(operationId: String, contractId: String, documentId: String, sessionKey: String): Flow<AnalysisJobState> =
        flow { throw BackendNotConfiguredException("Analysis") }
}

class UnconfiguredRemoteNotificationRepository : NotificationRepository {
    override suspend fun getAll(): List<NotificationRecord> = throw BackendNotConfiguredException("Notification")
    override suspend fun markRead(notificationId: String) = throw BackendNotConfiguredException("Notification")
    override suspend fun markAllRead() = throw BackendNotConfiguredException("Notification")
}

class UnconfiguredRemoteContractRepository(private val sessionKeyProvider: () -> String? = { null }) : ContractRepository {
    override fun forSession(sessionKey: String): ContractRepository = this
    override fun activeSessionKey(): String? = sessionKeyProvider()
    override fun observeContracts(): Flow<List<EmploymentContract>> = throw BackendNotConfiguredException("Contract")
    override suspend fun getContract(contractId: String): EmploymentContract? = throw BackendNotConfiguredException("Contract")
    override suspend fun createContract(contract: EmploymentContract): EmploymentContract = throw BackendNotConfiguredException("Contract")
    override suspend fun updateContract(contract: EmploymentContract): Boolean = throw BackendNotConfiguredException("Contract")
    override suspend fun renameContract(contractId: String, title: String): Boolean = throw BackendNotConfiguredException("Contract")
    override suspend fun deleteContract(contractId: String): Boolean = throw BackendNotConfiguredException("Contract")
    override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = throw BackendNotConfiguredException("Contract")
}

class UnconfiguredRemoteDocumentRepository : DocumentRepository {
    override fun forSession(sessionKey: String): DocumentRepository = this
    override suspend fun getDocument(documentId: String): EmploymentDocument? = throw BackendNotConfiguredException("Document")
    override suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument = throw BackendNotConfiguredException("Document")
    override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean = throw BackendNotConfiguredException("Document")
    override suspend fun deleteDocument(documentId: String): Boolean = throw BackendNotConfiguredException("Document")
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

/** REAL profile calls stay disabled until the Backend confirms its profile contract. */
class UnconfiguredRemoteProfileRepository : ProfileRepository {
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Profile")
    override suspend fun getProfile(): UserProfile = unavailable()
    override suspend fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile = unavailable()
    override suspend fun clearSessionData(): Boolean = true
}
