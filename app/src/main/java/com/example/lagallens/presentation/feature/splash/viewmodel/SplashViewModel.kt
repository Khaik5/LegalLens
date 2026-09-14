package com.example.lagallens.presentation.feature.splash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEffect
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val isOnboardingCompleted: IsOnboardingCompletedUseCase
) : ViewModel() {
    private val _uiEffect = MutableSharedFlow<SplashUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.ScreenStarted -> openOnboardingAfterSplash()
        }
    }

    private fun openOnboardingAfterSplash() {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MILLIS)
            _uiEffect.emit(
                if (isOnboardingCompleted()) SplashUiEffect.NavigateToHome
                else SplashUiEffect.NavigateToOnboarding
            )
        }
    }

    class Factory(
        private val isOnboardingCompleted: IsOnboardingCompletedUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(SplashViewModel::class.java))
            return SplashViewModel(isOnboardingCompleted) as T
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 2000L
    }
}
