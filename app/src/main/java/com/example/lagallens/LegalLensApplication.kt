package com.example.lagallens

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.lagallens.di.AppDataMode
import com.example.lagallens.di.RepositoryModule
import com.example.lagallens.di.DataModeConfig
import com.example.lagallens.di.UseCaseModule
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.OcrRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.domain.usecase.OpenChatbotConversationUseCase
import com.example.lagallens.domain.usecase.ObserveProfileSettingsUseCase
import com.example.lagallens.domain.usecase.UpdateProfileAppearanceUseCase
import com.example.lagallens.domain.usecase.UpdateProfileLanguageUseCase
import com.example.lagallens.domain.usecase.RetryChatbotMessageUseCase
import com.example.lagallens.domain.usecase.SendChatbotMessageUseCase
import com.example.lagallens.domain.usecase.GetAnalysisOverviewUseCase
import com.example.lagallens.domain.usecase.GetOcrResultUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LegalLensApplication : Application() {
    val dataMode get() = DataModeConfig.current
    val usesBackendAuthentication get() = dataMode == AppDataMode.REAL
    val authRepository: AuthRepository by lazy {
        RepositoryModule.provideAuthRepository(applicationContext, onDemoAccountCreated = ::saveDemoAccountName)
    }

    private suspend fun saveDemoAccountName(fullName: String) {
        val profile = userProfileRepository.getProfile()
        userProfileRepository.saveProfile(fullName, profile.phone, profile.photoUri)
    }
    val notificationRepository: com.example.lagallens.domain.repository.NotificationRepository by lazy {
        RepositoryModule.provideNotificationRepository(applicationContext)
    }
    val chatbotRepository: ChatbotRepository by lazy { RepositoryModule.provideChatbotRepository(applicationContext) }
    val ocrRepository: OcrRepository by lazy { RepositoryModule.provideOcrRepository() }
    val documentUploadRepository by lazy { RepositoryModule.provideDocumentUploadRepository() }
    val analysisRepository: AnalysisRepository by lazy { RepositoryModule.provideAnalysisRepository(applicationContext) }
    private val onboardingRepository: OnboardingRepository by lazy {
        RepositoryModule.provideOnboardingRepository(applicationContext)
    }
    private val profileSettingsRepository: ProfileSettingsRepository by lazy {
        RepositoryModule.provideProfileSettingsRepository(applicationContext)
    }
    val userProfileRepository: ProfileRepository by lazy {
        RepositoryModule.provideProfileRepository(applicationContext, sessionEmail = { authRepository.currentUserEmail() }, sessionUserId = { authRepository.currentUserId() })
    }
    val analysisIdentityRepository by lazy { RepositoryModule.provideAnalysisIdentityRepository(applicationContext) }
    val analysisUnlockRepository by lazy { RepositoryModule.provideAnalysisUnlockRepository(applicationContext) }
    val contractRepository: ContractRepository by lazy { RepositoryModule.provideContractRepository(applicationContext) }
    val documentRepository: DocumentRepository by lazy { RepositoryModule.provideDocumentRepository(applicationContext) }
    private val defaultDraftRepository: DraftRepository by lazy { RepositoryModule.provideDraftRepository(applicationContext) }
    @Volatile private var sandboxDraftRepository: DraftRepository? = null
    @Volatile internal var sandboxDraftPreferences: String? = null
        private set
    @Volatile internal var sandboxDraftPdfDirectory: String? = null
        private set
    private val sandboxSession = MutableStateFlow<String?>(null)
    val draftRepository: DraftRepository get() = sandboxDraftRepository ?: defaultDraftRepository

    fun draftAiRepository(session: String?) = RepositoryModule.provideDraftAiRepository(applicationContext, draftRepository.forSession(session))

    internal fun setSandboxDraftFixture(repository: DraftRepository?, session: String?, preferences: String?, pdfDirectory: String?) {
        check(BuildConfig.BUILD_TYPE == "sandbox" && packageName == "com.example.lagallens.testsandbox")
        require((repository == null && session == null && preferences == null && pdfDirectory == null) ||
            (repository != null && session != null && preferences != null && pdfDirectory != null))
        if (pdfDirectory != null) {
            require(java.util.UUID.fromString(pdfDirectory).toString() == pdfDirectory)
            require(preferences == "draft_legacy_fixture_$pdfDirectory")
        }
        sandboxDraftPreferences = preferences
        sandboxDraftPdfDirectory = pdfDirectory
        sandboxSession.value = session
        sandboxDraftRepository = repository
    }
    private val defaultCreditRepository: CreditRepository by lazy { RepositoryModule.provideCreditRepository(applicationContext) }
    private val defaultPaymentRepository: PaymentRepository by lazy { RepositoryModule.providePaymentRepository(applicationContext, defaultCreditRepository) }
    @Volatile private var sandboxFinance: Pair<CreditRepository, PaymentRepository>? = null
    val creditRepository: CreditRepository get() = sandboxFinance?.first ?: defaultCreditRepository
    val paymentRepository: PaymentRepository get() = sandboxFinance?.second ?: defaultPaymentRepository

    internal fun setSandboxFinance(credit: CreditRepository?, payment: PaymentRepository?) {
        check(BuildConfig.BUILD_TYPE == "sandbox" && packageName == "com.example.lagallens.testsandbox")
        require((credit == null) == (payment == null))
        sandboxFinance = if (credit != null && payment != null) credit to payment else null
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val restoreSessionUseCase by lazy { UseCaseModule.provideRestoreSessionUseCase(authRepository) }

    val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        UseCaseModule.provideAuthenticateUserUseCase(authRepository)
    }

    fun currentDemoUserEmail(): String? = authRepository.currentUserEmail()

    fun currentSessionKey(): String? = if (sandboxDraftRepository != null) sandboxSession.value else authRepository.currentSessionKey()

    internal fun <T> withSession(key: String?, action: () -> T): T {
        if (sandboxDraftRepository != null) {
            check(key != null && sandboxSession.value == key) { "DEMO session changed" }
            return action()
        }
        if (dataMode != com.example.lagallens.di.AppDataMode.DEMO) return action()
        val auth = com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource(this)
        val scope = checkNotNull(auth.currentSessionScope()) { "No active DEMO session" }
        return auth.withSession(scope) {
            check("${scope.userId}:${scope.generation}" == key) { "DEMO session changed" }
            action()
        }
    }

    fun observeSessionKeys(): kotlinx.coroutines.flow.Flow<String?> =
        if (sandboxDraftRepository != null) sandboxSession else authRepository.observeSessionKeys()

    val changePasswordUseCase by lazy { UseCaseModule.provideChangePasswordUseCase(authRepository) }

    /**
     * Every session change (start, logout, expiry, account switch) removes temporary scans and PDFs written before
     * it. Running here rather than in a screen means the cleanup also happens when no screen is open.
     */
    private fun clearSessionCachesOnSessionChange() {
        applicationScope.launch(Dispatchers.IO) {
            val cleaner = com.example.lagallens.data.datasource.local.SessionCacheCleaner(this@LegalLensApplication)
            authRepository.observeSessionKeys().distinctUntilChanged().collect {
                cleaner.clear(createdBefore = System.currentTimeMillis())
            }
        }
    }

    val syncContractRelatedDataUseCase by lazy {
        if (dataMode == AppDataMode.DEMO) {
            com.example.lagallens.domain.usecase.SyncContractRelatedDataUseCase(chatbotRepository) { session -> draftRepository.forSession(session) }
        } else null
    }
    val logoutUseCase by lazy { UseCaseModule.provideLogoutUseCase(authRepository, chatbotRepository, userProfileRepository) }
    val registerAccountUseCase by lazy { com.example.lagallens.domain.usecase.RegisterAccountUseCase(authRepository) }
    val verifyOtpUseCase by lazy { com.example.lagallens.domain.usecase.VerifyOtpUseCase(authRepository) }
    val resendOtpUseCase by lazy { com.example.lagallens.domain.usecase.ResendOtpUseCase(authRepository) }
    val requestPasswordResetUseCase by lazy { com.example.lagallens.domain.usecase.RequestPasswordResetUseCase(authRepository) }
    val confirmPasswordResetUseCase by lazy { com.example.lagallens.domain.usecase.ConfirmPasswordResetUseCase(authRepository) }

    val openChatbotConversationUseCase: OpenChatbotConversationUseCase by lazy {
        UseCaseModule.provideOpenChatbotConversationUseCase(chatbotRepository)
    }
    val sendChatbotMessageUseCase: SendChatbotMessageUseCase by lazy {
        UseCaseModule.provideSendChatbotMessageUseCase(chatbotRepository)
    }
    val retryChatbotMessageUseCase: RetryChatbotMessageUseCase by lazy {
        UseCaseModule.provideRetryChatbotMessageUseCase(chatbotRepository)
    }
    val getOcrResultUseCase: GetOcrResultUseCase by lazy {
        UseCaseModule.provideGetOcrResultUseCase(ocrRepository)
    }
    val getAnalysisDetailUseCase by lazy { UseCaseModule.provideGetAnalysisDetailUseCase(analysisRepository) }
    val getAnalysisOverviewUseCase: GetAnalysisOverviewUseCase by lazy {
        UseCaseModule.provideGetAnalysisOverviewUseCase(analysisRepository)
    }
    val completeOnboardingUseCase: CompleteOnboardingUseCase by lazy {
        UseCaseModule.provideCompleteOnboardingUseCase(onboardingRepository)
    }
    val isOnboardingCompletedUseCase: IsOnboardingCompletedUseCase by lazy {
        UseCaseModule.provideIsOnboardingCompletedUseCase(onboardingRepository)
    }
    val observeProfileSettingsUseCase: ObserveProfileSettingsUseCase by lazy {
        UseCaseModule.provideObserveProfileSettingsUseCase(profileSettingsRepository)
    }
    val updateProfileAppearanceUseCase: UpdateProfileAppearanceUseCase by lazy {
        UseCaseModule.provideUpdateProfileAppearanceUseCase(profileSettingsRepository)
    }
    val updateNotificationPreferencesUseCase by lazy {
        UseCaseModule.provideUpdateNotificationPreferencesUseCase(profileSettingsRepository)
    }
    val updateProfileLanguageUseCase: UpdateProfileLanguageUseCase by lazy {
        UseCaseModule.provideUpdateProfileLanguageUseCase(profileSettingsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        // The dark palette is incomplete and has no approved design, so the app stays light until it does.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        clearSessionCachesOnSessionChange()
        applicationScope.launch {
            observeProfileSettingsUseCase().collectLatest { settings ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val manager = getSystemService(LocaleManager::class.java)
                    if (manager != null) manager.applicationLocales = LocaleList.forLanguageTags(settings.language.toLocaleList().toLanguageTags())
                    else AppCompatDelegate.setApplicationLocales(settings.language.toLocaleList())
                } else {
                    AppCompatDelegate.setApplicationLocales(settings.language.toLocaleList())
                }
            }
        }
    }

    private fun ProfileLanguage.toLocaleList(): LocaleListCompat = when (this) {
        ProfileLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        ProfileLanguage.VIETNAMESE -> LocaleListCompat.forLanguageTags("vi")
    }
}
