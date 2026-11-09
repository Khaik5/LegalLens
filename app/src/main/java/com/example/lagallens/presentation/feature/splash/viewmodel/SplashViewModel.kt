package com.example.lagallens.presentation.feature.splash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEffect
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    private val isOnboardingCompleted: IsOnboardingCompletedUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiEffect = Channel<SplashUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.ScreenStarted -> openOnboardingAfterSplash()
        }
    }

    private fun openOnboardingAfterSplash() {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MILLIS)
            val destination = when {
                !isOnboardingCompleted() -> SplashUiEffect.NavigateToOnboarding
                authRepository.currentUserEmail() != null -> SplashUiEffect.NavigateToMain
                else -> SplashUiEffect.NavigateToHome
            }
            _uiEffect.send(destination)
        }
    }

    class Factory(
        private val isOnboardingCompleted: IsOnboardingCompletedUseCase,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(SplashViewModel::class.java))
            return SplashViewModel(isOnboardingCompleted, authRepository) as T
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 2000L
    }
}
