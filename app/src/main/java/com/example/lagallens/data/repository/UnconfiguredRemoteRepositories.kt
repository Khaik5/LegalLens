package com.example.lagallens.data.repository

import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.repository.AnalysisRepository
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

class UnconfiguredRemoteAuthRepository : AuthRepository {
    override fun authenticate(username: String, password: String): Boolean = throw BackendNotConfiguredException("Auth")
    override fun currentUserEmail(): String? = null
    override fun logout(): Boolean = true
    override fun changePassword(currentPassword: String, newPassword: String): Boolean = throw BackendNotConfiguredException("Auth")
}

class UnconfiguredRemoteChatbotRepository : ChatbotRepository {
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
    override suspend fun getResult(documentName: String, documentDetails: String): OcrResult =
        throw BackendNotConfiguredException("OCR")
}

class UnconfiguredRemoteAnalysisRepository : AnalysisRepository {
    override suspend fun getOverview(documentName: String, documentMetadata: String): AnalysisOverview =
        throw BackendNotConfiguredException("Analysis")
}

class UnconfiguredRemoteNotificationRepository : NotificationRepository {
    override suspend fun getAll(): List<NotificationRecord> = throw BackendNotConfiguredException("Notification")
    override suspend fun markRead(notificationId: String) = throw BackendNotConfiguredException("Notification")
    override suspend fun markAllRead() = throw BackendNotConfiguredException("Notification")
}

class UnconfiguredRemoteContractRepository : ContractRepository {
    override fun observeContracts(): Flow<List<EmploymentContract>> = throw BackendNotConfiguredException("Contract")
    override suspend fun getContract(contractId: String): EmploymentContract? = throw BackendNotConfiguredException("Contract")
    override suspend fun createContract(contract: EmploymentContract): EmploymentContract = throw BackendNotConfiguredException("Contract")
    override suspend fun updateContract(contract: EmploymentContract): Boolean = throw BackendNotConfiguredException("Contract")
    override suspend fun renameContract(contractId: String, title: String): Boolean = throw BackendNotConfiguredException("Contract")
    override suspend fun deleteContract(contractId: String): Boolean = throw BackendNotConfiguredException("Contract")
    override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = throw BackendNotConfiguredException("Contract")
}

class UnconfiguredRemoteDocumentRepository : DocumentRepository {
    override suspend fun getDocument(documentId: String): EmploymentDocument? = throw BackendNotConfiguredException("Document")
    override suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument = throw BackendNotConfiguredException("Document")
    override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean = throw BackendNotConfiguredException("Document")
    override suspend fun deleteDocument(documentId: String): Boolean = throw BackendNotConfiguredException("Document")
}

class UnconfiguredRemoteDraftRepository : DraftRepository {
    override suspend fun getDrafts(): List<Draft> = throw BackendNotConfiguredException("Draft")
    override suspend fun getDraft(draftId: String): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun getLatestDraft(stage: DraftStage): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun saveDraft(stage: DraftStage, title: String, content: String, contractId: String?, sourceDraftId: String?): Draft =
        throw BackendNotConfiguredException("Draft")
    override suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun updateDraft(draftId: String, title: String, content: String): Draft? = throw BackendNotConfiguredException("Draft")
    override suspend fun deleteDraft(draftId: String): Boolean = throw BackendNotConfiguredException("Draft")
}

class UnconfiguredRemoteCreditRepository : CreditRepository {
    override val isConfigured: Boolean = false
    override val balance = MutableStateFlow(0).asStateFlow()
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Credit")
    override suspend fun getTransactions(): List<CreditTransaction> = unavailable()
    override suspend fun addPurchasedCredits(paymentId: String, packageId: String, amountVnd: Long, credits: Int): CreditReceipt = unavailable()
    override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = unavailable()
}

/** REAL profile calls stay disabled until the Backend confirms its profile contract. */
class UnconfiguredRemoteProfileRepository : ProfileRepository {
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Profile")
    override fun getProfile(): UserProfile = unavailable()
    override fun saveProfile(name: String, phone: String, photoUri: String?): UserProfile = unavailable()
    override fun clearSessionData(): Boolean = true
}
