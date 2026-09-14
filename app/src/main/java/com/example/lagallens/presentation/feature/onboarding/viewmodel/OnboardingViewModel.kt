package com.example.lagallens.presentation.feature.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiEffect
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiEvent
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingPage
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val completeOnboardingUseCase: CompleteOnboardingUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OnboardingUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.NextClicked -> {
                val nextPageIndex = _uiState.value.pageIndex + 1
                if (nextPageIndex < _uiState.value.pages.size) {
                    _uiState.update { it.copy(pageIndex = nextPageIndex) }
                } else {
                    completeOnboarding()
                }
            }
            OnboardingUiEvent.SkipClicked -> completeOnboarding()
            is OnboardingUiEvent.PageSelected -> {
                if (event.pageIndex in _uiState.value.pages.indices) {
                    _uiState.update { state ->
                        if (state.pageIndex == event.pageIndex) state
                        else state.copy(pageIndex = event.pageIndex)
                    }
                }
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            completeOnboardingUseCase()
            _uiEffect.emit(OnboardingUiEffect.NavigateToHome)
        }
    }

    class Factory(
        private val completeOnboarding: CompleteOnboardingUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(OnboardingViewModel::class.java))
            return OnboardingViewModel(completeOnboarding) as T
        }
    }
}
