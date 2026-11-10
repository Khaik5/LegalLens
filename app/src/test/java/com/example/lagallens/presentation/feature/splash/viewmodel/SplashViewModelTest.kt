package com.example.lagallens.presentation.feature.splash.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.model.auth.SessionRestoreResult
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.domain.usecase.RestoreSessionUseCase
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEffect
import com.example.lagallens.presentation.feature.splash.contract.SplashUiEvent
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test
    fun incompleteOnboardingSkipsSessionRestore() = runTest {
        val viewModel = viewModel(onboarded = false) { error("Unexpected session check") }

        viewModel.onEvent(SplashUiEvent.ScreenStarted)
        advanceUntilIdle()

        assertEquals(SplashUiEffect.NavigateToOnboarding, viewModel.uiEffect.first())
    }

    @Test
    fun restoredSessionOpensMain() = runTest {
        val viewModel = viewModel { SessionRestoreResult.Restored }

        viewModel.onEvent(SplashUiEvent.ScreenStarted)
        advanceUntilIdle()

        assertEquals(SplashUiEffect.NavigateToMain, viewModel.uiEffect.first())
    }

    @Test
    fun missingOrExpiredSessionOpensHome() = runTest {
        for (result in listOf(SessionRestoreResult.NoSession, SessionRestoreResult.Expired)) {
            val viewModel = viewModel { result }
            viewModel.onEvent(SplashUiEvent.ScreenStarted)
            advanceUntilIdle()
            assertEquals(SplashUiEffect.NavigateToHome, viewModel.uiEffect.first())
        }
    }

    @Test
    fun networkFailureShowsRetryAndRetryRestoresSession() = runTest {
        val results = ArrayDeque(
            listOf(SessionRestoreResult.Failed(IOException("Offline")), SessionRestoreResult.Restored)
        )
        val viewModel = viewModel { results.removeFirst() }

        viewModel.onEvent(SplashUiEvent.ScreenStarted)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isRetryVisible)
        assertFalse(viewModel.uiState.value.isLoading)

        viewModel.onEvent(SplashUiEvent.ScreenStarted)
        viewModel.onEvent(SplashUiEvent.RetryClicked)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRetryVisible)
        assertEquals(SplashUiEffect.NavigateToMain, viewModel.uiEffect.first())
        assertTrue(results.isEmpty())
    }

    private fun viewModel(
        onboarded: Boolean = true,
        restore: suspend () -> SessionRestoreResult
    ): SplashViewModel {
        val onboarding = object : OnboardingRepository {
            override suspend fun isCompleted() = onboarded
            override suspend fun markCompleted() = Unit
        }
        val auth = object : AuthRepository {
            override suspend fun restoreSession() = restore()
            override fun currentSessionKey() = null
            override fun observeSessionKeys() = flowOf(null)
            override suspend fun authenticate(username: String, password: String) = unexpected()
            override suspend fun register(fullName: String, email: String, password: String) = unexpected()
            override suspend fun resendOtp(email: String, purpose: OtpPurpose) = unexpected()
            override suspend fun verifyOtp(challengeId: String, code: String, purpose: OtpPurpose) = unexpected()
            override suspend fun requestPasswordReset(email: String) = unexpected()
            override suspend fun confirmPasswordReset(challengeId: String, verificationToken: String, newPassword: String) = unexpected()
            override suspend fun logout() = unexpected()
            override suspend fun changePassword(currentPassword: String, newPassword: String) = unexpected()

            private fun unexpected(): Nothing = error("Unexpected auth operation")
        }
        return SplashViewModel(IsOnboardingCompletedUseCase(onboarding), RestoreSessionUseCase(auth))
    }
}
