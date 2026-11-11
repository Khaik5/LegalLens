package com.example.lagallens.di

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.data.datasource.local.datastore.ProfileSettingsLocalDataSource
import com.example.lagallens.data.datasource.local.ProfileAvatarFiles
import com.example.lagallens.data.datasource.local.datastore.ProfileLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.data.repository.FakeAnalysisRepository
import com.example.lagallens.data.repository.FakeOcrRepository
import com.example.lagallens.data.repository.OnboardingPreferencesRepository
import com.example.lagallens.data.repository.ProfileSettingsRepositoryImpl
import com.example.lagallens.data.repository.ProfileRepositoryImpl
import com.example.lagallens.data.repository.UnconfiguredRemoteAnalysisRepository
import com.example.lagallens.core.network.NetworkModule
import com.example.lagallens.data.datasource.local.datastore.EncryptedTokenStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.AuthRemoteDataSource
import com.example.lagallens.data.datasource.remote.ContractRemoteDataSource
import com.example.lagallens.data.datasource.remote.UserRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.AuthApi
import com.example.lagallens.data.datasource.remote.api.AuthSessionApi
import com.example.lagallens.data.datasource.remote.api.ContractApi
import com.example.lagallens.data.datasource.remote.api.UserApi
import com.example.lagallens.data.repository.RemoteAuthRepository
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
import com.example.lagallens.R
import com.example.lagallens.data.repository.RemoteNotificationRepository
import com.example.lagallens.data.datasource.remote.NotificationRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.NotificationApi
import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.data.repository.FakeContractRepository
import com.example.lagallens.data.repository.FakeDocumentRepository
import com.example.lagallens.data.repository.RemoteContractRepository
import com.example.lagallens.data.repository.RemoteDocumentRepository
import com.example.lagallens.data.repository.RemoteDocumentUploadRepository
import com.example.lagallens.data.repository.FakeDocumentUploadRepository
import com.example.lagallens.data.datasource.remote.DocumentRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.DocumentApi
import com.example.lagallens.domain.repository.DocumentUploadRepository
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
import com.example.lagallens.data.repository.RemoteProfileRepository
import com.example.lagallens.data.repository.SessionScopedDraftRepository
import com.example.lagallens.data.datasource.local.datastore.AccountStorageName
import com.example.lagallens.data.repository.SessionScopedEmploymentRepositories
import com.example.lagallens.data.datasource.remote.rotateTokens
import retrofit2.Retrofit

object RepositoryModule {
    private val publicAuthApi = lazy { NetworkModule.provideRetrofit().create(AuthApi::class.java) }
    private var authenticatedRetrofitInstance: Retrofit? = null

    @Synchronized
    private fun authenticatedRetrofit(context: Context): Retrofit =
        authenticatedRetrofitInstance ?: NetworkModule.provideAuthenticatedRetrofit(
            EncryptedTokenStore(context.applicationContext)
        ) { publicAuthApi.value.rotateTokens(it) }.also { authenticatedRetrofitInstance = it }

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
        AppDataMode.REAL -> RemoteContractRepository(
            EncryptedTokenStore(context),
            ContractRemoteDataSource(lazy { authenticatedRetrofit(context).create(ContractApi::class.java) }),
            documentRemote(context)
        )
    }

    fun provideDocumentRepository(context: Context): DocumentRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> employmentRepositories(context).documents
        AppDataMode.REAL -> RemoteDocumentRepository(context.applicationContext, EncryptedTokenStore(context), documentRemote(context))
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

    fun provideAuthRepository(context: Context, onDemoAccountCreated: suspend (String) -> Unit = {}): AuthRepository {
        return when (DataModeConfig.current) {
            AppDataMode.DEMO -> LocalAuthRepository(AuthLocalDataSource(context), onDemoAccountCreated)
            AppDataMode.REAL -> {
                val tokens = EncryptedTokenStore(context)
                val remote = AuthRemoteDataSource(
                    api = publicAuthApi,
                    sessionApi = lazy {
                        authenticatedRetrofit(context).create(AuthSessionApi::class.java)
                    }
                )
                RemoteAuthRepository(tokens, remote)
            }
        }
    }

    /** REAL session identity shared by remote repositories across token rotation. */
    private fun remoteSessionKey(context: Context): () -> String? {
        val tokens = EncryptedTokenStore(context)
        return { tokens.currentSessionScope()?.key }
    }

    fun provideChatbotRepository(context: Context): ChatbotRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> FakeChatbotRepository(context.applicationContext)
        AppDataMode.REAL -> UnconfiguredRemoteChatbotRepository(remoteSessionKey(context))
    }

    private fun documentRemote(context: Context) =
        DocumentRemoteDataSource(lazy { authenticatedRetrofit(context).create(DocumentApi::class.java) })

    fun provideDocumentUploadRepository(context: Context, contracts: ContractRepository): DocumentUploadRepository =
        when (DataModeConfig.current) {
            AppDataMode.DEMO -> FakeDocumentUploadRepository()
            AppDataMode.REAL -> RemoteDocumentUploadRepository(
                context.applicationContext, EncryptedTokenStore(context), contracts, documentRemote(context)
            )
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
        AppDataMode.REAL -> {
            val appContext = context.applicationContext
            RemoteNotificationRepository(
                EncryptedTokenStore(context),
                NotificationRemoteDataSource(lazy { authenticatedRetrofit(context).create(NotificationApi::class.java) })
            ) { type ->
                // Resolved on each load so a language change applies to the next refresh.
                appContext.getString(
                    when (type) {
                        "ANALYSIS_DONE" -> R.string.notification_type_analysis_done_title
                        "HIGH_RISK_FOUND" -> R.string.notification_type_high_risk_title
                        "CONTRACT_EXPIRING" -> R.string.notification_type_contract_expiring_title
                        else -> R.string.notification_type_system_title
                    }
                )
            }
        }
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
        AppDataMode.REAL -> RemoteProfileRepository(
            EncryptedTokenStore(context),
            UserRemoteDataSource(lazy { authenticatedRetrofit(context).create(UserApi::class.java) }),
            ProfileAvatarFiles(context),
            ProfileLocalDataSource(context)
        )
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
