package com.example.lagallens.di

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.data.datasource.local.datastore.ProfileSettingsLocalDataSource
import com.example.lagallens.data.datasource.local.datastore.ProfileLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.data.repository.FakeAnalysisRepository
import com.example.lagallens.data.repository.FakeOcrRepository
import com.example.lagallens.data.repository.OnboardingPreferencesRepository
import com.example.lagallens.data.repository.ProfileSettingsRepositoryImpl
import com.example.lagallens.data.repository.ProfileRepositoryImpl
import com.example.lagallens.data.repository.UnconfiguredRemoteAnalysisRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteAuthRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteChatbotRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteOcrRepository
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.OcrRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.domain.repository.NotificationRepository
import com.example.lagallens.data.repository.FakeNotificationRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteNotificationRepository
import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.data.repository.FakeContractRepository
import com.example.lagallens.data.repository.FakeDocumentRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteContractRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteDocumentRepository
import com.example.lagallens.data.repository.FakeDraftRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteDraftRepository
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.data.repository.UnconfiguredRemotePaymentRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteCreditRepository
import com.example.lagallens.data.repository.FakePaymentRepository
import com.example.lagallens.data.repository.FakeCreditRepository
import com.example.lagallens.data.repository.UnconfiguredRemoteProfileRepository
import com.example.lagallens.data.repository.SessionScopedDraftRepository
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.repository.SessionScopedEmploymentRepositories
import com.example.lagallens.data.repository.tokenSessionKey

object RepositoryModule {
    fun provideAnalysisUnlockRepository(context: Context): com.example.lagallens.domain.repository.AnalysisUnlockRepository =
        when (DataModeConfig.current) {
            AppDataMode.DEMO -> com.example.lagallens.data.repository.DemoAnalysisUnlockRepository(
                AuthLocalDataSource(context.applicationContext),
                provideAnalysisIdentityRepository(context),
                finance(context)
            )
            AppDataMode.REAL -> com.example.lagallens.data.repository.UnconfiguredRemoteAnalysisUnlockRepository()
        }

    private var financeRepository: com.example.lagallens.data.repository.DemoFinanceRepository? = null

    @Synchronized
    private fun finance(context: Context): com.example.lagallens.data.repository.DemoFinanceRepository =
        financeRepository ?: run {
            val auth = AuthLocalDataSource(context.applicationContext)
            val repository = com.example.lagallens.data.repository.DemoFinanceRepository(
                com.example.lagallens.data.datasource.local.datastore.AtomicFinanceStorage(context),
                auth::currentSessionScope,
                { scope, action -> auth.withSession(scope, action) },
                catalog = { com.example.lagallens.data.repository.demoCreditOffers(context.applicationContext) }
            )
            auth.withSessionState {
                auth.addSessionListener { repository.refreshSession() }
                repository.refreshSession()
            }
            financeRepository = repository
            repository
        }
    @Volatile private var employmentRepositories: SessionScopedEmploymentRepositories? = null

    @Synchronized
    private fun employmentRepositories(context: Context): SessionScopedEmploymentRepositories =
        employmentRepositories ?: SessionScopedEmploymentRepositories(context.applicationContext).also { employmentRepositories = it }

    fun provideAnalysisIdentityRepository(context: Context): com.example.lagallens.domain.repository.AnalysisIdentityRepository {
        if (DataModeConfig.current == AppDataMode.DEMO) return employmentRepositories(context).analysisIdentities
        return object : com.example.lagallens.domain.repository.AnalysisIdentityRepository {
            private val session = remoteSessionKey(context)
            override fun sessionKey(): String? = session()
            override suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String): com.example.lagallens.domain.model.analysis.AnalysisIdentity =
                throw IllegalStateException("Analysis backend unavailable")
            override suspend fun get(resultId: String, sessionKey: String): com.example.lagallens.domain.model.analysis.AnalysisIdentity? = null
            override suspend fun latest(contractId: String, sessionKey: String): com.example.lagallens.domain.model.analysis.AnalysisIdentity? = null
        }
    }

    fun provideContractRepository(context: Context): ContractRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> employmentRepositories(context).contracts
        AppDataMode.REAL -> UnconfiguredRemoteContractRepository(remoteSessionKey(context))
    }

    fun provideDocumentRepository(context: Context): DocumentRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> employmentRepositories(context).documents
        AppDataMode.REAL -> UnconfiguredRemoteDocumentRepository()
    }

    fun provideDraftRepository(context: Context): DraftRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> {
            val appContext = context.applicationContext
            val auth = AuthLocalDataSource(appContext)
            SessionScopedDraftRepository(auth::currentSessionScope) { scope ->
                FakeDraftRepository(appContext,
                    AccountStorageName.forUser("draft_repository_v1", scope.userId),
                    AccountStorageName.forUser("saved_draft_copies", scope.userId),
                    { action -> auth.withSession(scope, action) })
            }
        }
        AppDataMode.REAL -> UnconfiguredRemoteDraftRepository()
    }

    fun provideDraftAiRepository(context: Context, drafts: DraftRepository): com.example.lagallens.domain.repository.DraftAiRepository =
        when (DataModeConfig.current) {
            AppDataMode.DEMO -> com.example.lagallens.data.repository.FakeDraftAiRepository(context.applicationContext, drafts)
            AppDataMode.REAL -> com.example.lagallens.data.repository.UnconfiguredRemoteDraftAiRepository()
        }

    fun provideAuthRepository(context: Context, onAccountCreated: suspend (String) -> Unit = {}): AuthRepository {
        return when (DataModeConfig.current) {
            AppDataMode.DEMO -> LocalAuthRepository(AuthLocalDataSource(context), onAccountCreated)
            AppDataMode.REAL -> UnconfiguredRemoteAuthRepository(
                com.example.lagallens.data.datasource.local.datastore.EncryptedTokenStore(context)
            )
        }
    }

    /** REAL session identity shared by remote repositories; derived from the stored token without exposing it. */
    private fun remoteSessionKey(context: Context): () -> String? {
        val tokens = com.example.lagallens.data.datasource.local.datastore.EncryptedTokenStore(context)
        return { tokenSessionKey(tokens) }
    }

    fun provideChatbotRepository(context: Context): ChatbotRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> FakeChatbotRepository(context.applicationContext)
        AppDataMode.REAL -> UnconfiguredRemoteChatbotRepository(remoteSessionKey(context))
    }

    fun provideDocumentUploadRepository(): com.example.lagallens.domain.repository.DocumentUploadRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> com.example.lagallens.data.repository.FakeDocumentUploadRepository()
        AppDataMode.REAL -> com.example.lagallens.data.repository.UnconfiguredRemoteDocumentUploadRepository()
    }

    fun provideOcrRepository(): OcrRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> FakeOcrRepository()
        AppDataMode.REAL -> UnconfiguredRemoteOcrRepository()
    }

    fun provideAnalysisRepository(context: Context): AnalysisRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> {
            val app = context.applicationContext
            FakeAnalysisRepository(provideAnalysisIdentityRepository(app), { language ->
                app.getString(if (language == "en") com.example.lagallens.R.string.analysis_snapshot_english_unavailable else com.example.lagallens.R.string.analysis_result_unavailable)
            })
        }
        AppDataMode.REAL -> UnconfiguredRemoteAnalysisRepository()
    }

    fun provideNotificationRepository(context: Context): NotificationRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> FakeNotificationRepository(context)
        AppDataMode.REAL -> UnconfiguredRemoteNotificationRepository()
    }

    fun provideOnboardingRepository(context: Context): OnboardingRepository {
        return OnboardingPreferencesRepository(OnboardingPreferencesDataSource(context))
    }

    fun provideProfileSettingsRepository(context: Context): ProfileSettingsRepository {
        return ProfileSettingsRepositoryImpl(ProfileSettingsLocalDataSource(context))
    }

    fun provideProfileRepository(
        context: Context,
        sessionEmail: () -> String?,
        mode: AppDataMode = DataModeConfig.current,
        sessionUserId: () -> String? = { null }
    ): ProfileRepository = when (mode) {
        AppDataMode.DEMO -> ProfileRepositoryImpl(ProfileLocalDataSource(context, sessionUserId, sessionEmail))
        AppDataMode.REAL -> UnconfiguredRemoteProfileRepository()
    }

    fun provideCreditRepository(context: Context): CreditRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> com.example.lagallens.data.repository.IoFinanceRepository(finance(context))
        AppDataMode.REAL -> UnconfiguredRemoteCreditRepository(remoteSessionKey(context))
    }

    fun providePaymentRepository(context: Context, creditRepository: CreditRepository): PaymentRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> com.example.lagallens.data.repository.IoFinanceRepository(finance(context))
        AppDataMode.REAL -> UnconfiguredRemotePaymentRepository(remoteSessionKey(context))
    }
}
