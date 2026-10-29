package com.example.lagallens

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.lagallens.di.RepositoryModule
import com.example.lagallens.di.DataModeConfig
import com.example.lagallens.di.UseCaseModule
import com.example.lagallens.domain.model.ProfileAppearance
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LegalLensApplication : Application() {
    val dataMode get() = DataModeConfig.current
    private val authRepository: AuthRepository by lazy { RepositoryModule.provideAuthRepository(applicationContext) }
    val chatbotRepository: ChatbotRepository by lazy { RepositoryModule.provideChatbotRepository(applicationContext) }
    private val ocrRepository: OcrRepository by lazy { RepositoryModule.provideOcrRepository() }
    private val analysisRepository: AnalysisRepository by lazy { RepositoryModule.provideAnalysisRepository() }
    private val onboardingRepository: OnboardingRepository by lazy {
        RepositoryModule.provideOnboardingRepository(applicationContext)
    }
    private val profileSettingsRepository: ProfileSettingsRepository by lazy {
        RepositoryModule.provideProfileSettingsRepository(applicationContext)
    }
    val userProfileRepository: ProfileRepository by lazy {
        RepositoryModule.provideProfileRepository(applicationContext, sessionEmail = { authRepository.currentUserEmail() }, sessionUserId = { authRepository.currentUserId() })
    }
    val contractRepository: ContractRepository by lazy { RepositoryModule.provideContractRepository(applicationContext) }
    val documentRepository: DocumentRepository by lazy { RepositoryModule.provideDocumentRepository(applicationContext) }
    val draftRepository: DraftRepository by lazy { RepositoryModule.provideDraftRepository(applicationContext) }
    val creditRepository: CreditRepository by lazy { RepositoryModule.provideCreditRepository(applicationContext) }
    val paymentRepository: PaymentRepository by lazy { RepositoryModule.providePaymentRepository(applicationContext, creditRepository) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        UseCaseModule.provideAuthenticateUserUseCase(authRepository)
    }

    fun currentDemoUserEmail(): String? = authRepository.currentUserEmail()

    fun currentDemoSessionKey(): String? = if (dataMode == com.example.lagallens.di.AppDataMode.DEMO)
        com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource(this).currentSessionScope()?.let { "${it.userId}:${it.generation}" } else null

    fun observeDemoSessionKeys() = com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource(this)
        .observeSessionScope().map { if (dataMode == com.example.lagallens.di.AppDataMode.DEMO) it?.let { scope -> "${scope.userId}:${scope.generation}" } else null }

    fun changeCurrentDemoPassword(currentPassword: String, newPassword: String): Boolean =
        authRepository.changePassword(currentPassword, newPassword)

    fun logoutDemoSession(): Boolean {
        chatbotRepository.clearSessionData()
        val profileCleared = userProfileRepository.clearSessionData()
        return authRepository.logout() && profileCleared
    }
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
    val updateProfileLanguageUseCase: UpdateProfileLanguageUseCase by lazy {
        UseCaseModule.provideUpdateProfileLanguageUseCase(profileSettingsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            observeProfileSettingsUseCase().collectLatest { settings ->
                AppCompatDelegate.setDefaultNightMode(settings.appearance.toNightMode())
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

    private fun ProfileAppearance.toNightMode(): Int {
        return when (this) {
            ProfileAppearance.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ProfileAppearance.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ProfileAppearance.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
    }

    private fun ProfileLanguage.toLocaleList(): LocaleListCompat = when (this) {
        ProfileLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        ProfileLanguage.VIETNAMESE -> LocaleListCompat.forLanguageTags("vi")
    }
}
