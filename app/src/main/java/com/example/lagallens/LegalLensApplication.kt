package com.example.lagallens

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.lagallens.di.RepositoryModule
import com.example.lagallens.di.UseCaseModule
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.repository.AuthRepository
<<<<<<< Updated upstream
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
=======
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.domain.usecase.ObserveProfileSettingsUseCase
import com.example.lagallens.domain.usecase.UpdateProfileAppearanceUseCase
import com.example.lagallens.domain.usecase.UpdateProfileLanguageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
>>>>>>> Stashed changes

class LegalLensApplication : Application() {
    private val authRepository: AuthRepository by lazy { RepositoryModule.provideAuthRepository() }
    private val profileSettingsRepository: ProfileSettingsRepository by lazy {
        RepositoryModule.provideProfileSettingsRepository(applicationContext)
    }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        UseCaseModule.provideAuthenticateUserUseCase(authRepository)
    }
<<<<<<< Updated upstream
=======
    private val onboardingRepository: OnboardingRepository by lazy {
        RepositoryModule.provideOnboardingRepository(applicationContext)
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
>>>>>>> Stashed changes
}
