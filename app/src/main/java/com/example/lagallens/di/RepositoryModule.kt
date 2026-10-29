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

object RepositoryModule {
    private var financeRepository: com.example.lagallens.data.repository.DemoFinanceRepository? = null

    @Synchronized
    private fun finance(context: Context): com.example.lagallens.data.repository.DemoFinanceRepository =
        financeRepository ?: run {
            val auth = AuthLocalDataSource(context.applicationContext)
            val repository = com.example.lagallens.data.repository.DemoFinanceRepository(
                com.example.lagallens.data.datasource.local.datastore.AtomicFinanceStorage(context),
                auth::currentSessionScope,
                { scope, action -> auth.withSession(scope, action) }
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

    fun provideContractRepository(context: Context): ContractRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> employmentRepositories(context).contracts
        AppDataMode.REAL -> UnconfiguredRemoteContractRepository()
    }

    fun provideDocumentRepository(context: Context): DocumentRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> employmentRepositories(context).documents
        AppDataMode.REAL -> UnconfiguredRemoteDocumentRepository()
    }

    fun provideDraftRepository(context: Context): DraftRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> {
            val appContext = context.applicationContext
            val auth = AuthLocalDataSource(appContext)
            SessionScopedDraftRepository(auth::currentSessionScope) { userId ->
                FakeDraftRepository(appContext,
                    AccountStorageName.forUser("draft_repository_v1", userId),
                    AccountStorageName.forUser("saved_draft_copies", userId))
            }
        }
        AppDataMode.REAL -> UnconfiguredRemoteDraftRepository()
    }

    fun provideAuthRepository(context: Context): AuthRepository {
        return when (DataModeConfig.current) {
            AppDataMode.DEMO -> LocalAuthRepository(AuthLocalDataSource(context))
            AppDataMode.REAL -> UnconfiguredRemoteAuthRepository()
        }
    }

    fun provideChatbotRepository(context: Context): ChatbotRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> FakeChatbotRepository(context.applicationContext)
        AppDataMode.REAL -> UnconfiguredRemoteChatbotRepository()
    }

    fun provideOcrRepository(): OcrRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> FakeOcrRepository()
        AppDataMode.REAL -> UnconfiguredRemoteOcrRepository()
    }

    fun provideAnalysisRepository(): AnalysisRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> FakeAnalysisRepository()
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
        AppDataMode.DEMO -> finance(context)
        AppDataMode.REAL -> UnconfiguredRemoteCreditRepository()
    }

    fun providePaymentRepository(context: Context, creditRepository: CreditRepository): PaymentRepository = when (DataModeConfig.current) {
        AppDataMode.DEMO -> finance(context)
        AppDataMode.REAL -> UnconfiguredRemotePaymentRepository()
    }
}
