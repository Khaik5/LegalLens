package com.example.lagallens.presentation.feature.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingEffect
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingEvent
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingPage
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.NextClicked -> {
                val nextPageIndex = _uiState.value.pageIndex + 1
                if (nextPageIndex < _uiState.value.pages.size) {
                    _uiState.update { it.copy(pageIndex = nextPageIndex) }
                } else {
                    emitEffect(OnboardingEffect.StartRequested)
                }
            }
            OnboardingEvent.SkipClicked -> emitEffect(OnboardingEffect.SkipRequested)
            is OnboardingEvent.PageSelected -> {
                if (event.pageIndex in _uiState.value.pages.indices) {
                    _uiState.update { state ->
                        if (state.pageIndex == event.pageIndex) state
                        else state.copy(pageIndex = event.pageIndex)
                    }
                }
            }
        }
    }

    private fun emitEffect(effect: OnboardingEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
