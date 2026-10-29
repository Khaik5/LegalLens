package com.example.lagallens.presentation.feature.onboarding.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiEffect
import com.example.lagallens.presentation.feature.onboarding.contract.OnboardingUiEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun skipCompletesOnboardingOnlyOnceAndNavigates() = runTest {
        val repository = FakeOnboardingRepository()
        val viewModel = OnboardingViewModel(CompleteOnboardingUseCase(repository))
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(OnboardingUiEvent.SkipClicked)
        viewModel.onEvent(OnboardingUiEvent.SkipClicked)

        assertEquals(OnboardingUiEffect.NavigateToHome, effect.await())
        assertEquals(1, repository.markCompletedCalls)
    }

    @Test
    fun selectedPageControlsTheRenderedPageIndex() {
        val viewModel = OnboardingViewModel(CompleteOnboardingUseCase(FakeOnboardingRepository()))

        viewModel.onEvent(OnboardingUiEvent.PageSelected(2))
        viewModel.onEvent(OnboardingUiEvent.PageSelected(1))

        assertEquals(1, viewModel.uiState.value.pageIndex)
    }

    private class FakeOnboardingRepository : OnboardingRepository {
        var markCompletedCalls = 0

        override suspend fun isCompleted(): Boolean = false

        override suspend fun markCompleted() {
            markCompletedCalls++
        }
    }
}
