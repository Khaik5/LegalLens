package com.example.lagallens.presentation.feature.splash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.domain.model.auth.SessionRestoreResult
import com.example.lagallens.domain.usecase.RestoreSessionUseCase
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEffect
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEvent
import com.example.lagallens.presentation.feature.splash.contract.SplashUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel(
    private val isOnboardingCompleted: IsOnboardingCompletedUseCase,
    private val restoreSession: RestoreSessionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<SplashUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var started = false

    fun onEvent(event: SplashUiEvent) {
        when (event) {
            SplashUiEvent.ScreenStarted -> start()
            SplashUiEvent.RetryClicked -> retry()
        }
    }

    private fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            delay(SPLASH_DURATION_MILLIS)
            if (isOnboardingCompleted()) restore()
            else _uiEffect.send(SplashUiEffect.NavigateToOnboarding)
        }
    }

    private fun retry() {
        if (!_uiState.value.isRetryVisible) return
        _uiState.value = SplashUiState()
        viewModelScope.launch { restore() }
    }

    private suspend fun restore() {
        when (restoreSession()) {
            SessionRestoreResult.Restored -> _uiEffect.send(SplashUiEffect.NavigateToMain)
            SessionRestoreResult.NoSession,
            SessionRestoreResult.Expired -> _uiEffect.send(SplashUiEffect.NavigateToHome)
            is SessionRestoreResult.Failed -> _uiState.value = SplashUiState(isLoading = false, isRetryVisible = true)
        }
    }

    class Factory(
        private val isOnboardingCompleted: IsOnboardingCompletedUseCase,
        private val restoreSession: RestoreSessionUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(SplashViewModel::class.java))
            return SplashViewModel(isOnboardingCompleted, restoreSession) as T
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 2000L
    }
}
