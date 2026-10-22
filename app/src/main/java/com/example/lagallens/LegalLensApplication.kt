package com.example.lagallens

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.lagallens.di.RepositoryModule
import com.example.lagallens.di.UseCaseModule
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.OcrRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
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
import kotlinx.coroutines.launch

class LegalLensApplication : Application() {
    private val authRepository: AuthRepository by lazy { RepositoryModule.provideAuthRepository() }
    private val chatbotRepository: ChatbotRepository by lazy { RepositoryModule.provideChatbotRepository() }
    private val ocrRepository: OcrRepository by lazy { RepositoryModule.provideOcrRepository() }
    private val analysisRepository: AnalysisRepository by lazy { RepositoryModule.provideAnalysisRepository() }
    private val onboardingRepository: OnboardingRepository by lazy {
        RepositoryModule.provideOnboardingRepository(applicationContext)
    }
    private val profileSettingsRepository: ProfileSettingsRepository by lazy {
        RepositoryModule.provideProfileSettingsRepository(applicationContext)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        UseCaseModule.provideAuthenticateUserUseCase(authRepository)
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
                AppCompatDelegate.setApplicationLocales(settings.language.toLocaleList())
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
        ProfileLanguage.VIETNAMESE,
        ProfileLanguage.JAPANESE,
        ProfileLanguage.FRENCH,
        ProfileLanguage.CHINESE,
        ProfileLanguage.KOREAN -> LocaleListCompat.forLanguageTags("vi")
    }
}
