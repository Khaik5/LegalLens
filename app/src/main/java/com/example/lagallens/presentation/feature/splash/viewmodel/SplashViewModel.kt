package com.example.lagallens.presentation.feature.splash.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.splash.contract.SplashEffect
import com.example.lagallens.presentation.feature.splash.contract.SplashEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    private val _effects = MutableSharedFlow<SplashEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: SplashEvent) {
        when (event) {
            SplashEvent.ScreenStarted -> openOnboardingAfterSplash()
        }
    }

    private fun openOnboardingAfterSplash() {
        viewModelScope.launch {
            delay(SPLASH_DURATION_MILLIS)
            _effects.emit(SplashEffect.OpenOnboarding)
        }
    }

    private companion object {
        const val SPLASH_DURATION_MILLIS = 2000L
    }
}
