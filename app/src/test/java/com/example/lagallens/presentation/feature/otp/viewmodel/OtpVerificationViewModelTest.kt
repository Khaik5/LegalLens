package com.example.lagallens.presentation.feature.otp.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.model.auth.OtpVerification
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.usecase.ResendOtpUseCase
import com.example.lagallens.domain.usecase.VerifyOtpUseCase
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEffect
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtpVerificationViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    @Test fun resendUsesServerTimingAndNewChallengeSurvivesRecreation() = runTest {
        val auth = TestAuthRepository()
        val savedState = SavedStateHandle()
        val first = viewModel(auth, savedState) { testScheduler.currentTime }
        first.onEvent(initialized())
        assertEquals(2, first.uiState.value.remainingSeconds)
        first.onEvent(OtpVerificationUiEvent.ResendClicked)
        assertEquals(0, auth.resendCount)

        advanceTimeBy(2000)
        runCurrent()
        assertTrue(first.uiState.value.isResendAvailable)
        first.onEvent(OtpVerificationUiEvent.ResendClicked)
        runCurrent()
        assertEquals(1, auth.resendCount)
        assertEquals(3, first.uiState.value.remainingSeconds)
        first.viewModelScope.cancel()

        val restored = viewModel(auth, savedState) { testScheduler.currentTime }
        restored.onEvent(initialized())
        assertEquals(3, restored.uiState.value.remainingSeconds)
        enterCode(restored)
        runCurrent()

        assertEquals("replacement", auth.verifiedChallengeId)
        assertEquals(
            OtpVerificationUiEffect.NavigateToResetPassword("replacement", "test-proof"),
            restored.uiEffect.first()
        )
        restored.viewModelScope.cancel()
    }

    @Test fun expiredChallengeIsRejectedWithoutCallingVerify() = runTest {
        val auth = TestAuthRepository()
        val model = viewModel(auth, SavedStateHandle()) { testScheduler.currentTime }
        model.onEvent(initialized(expiresIn = 1))
        advanceTimeBy(1000)
        runCurrent()

        enterCode(model)
        runCurrent()
        assertEquals(null, auth.verifiedChallengeId)
        assertEquals(R.string.otp_invalid, model.uiState.value.errorResId)
        model.viewModelScope.cancel()
    }

    @Test fun otpRateLimitIsShownAndDoesNotStartAResendCountdown() = runTest {
        val auth = TestAuthRepository().apply { resendResult = AuthResult.Failed(AuthFailure.OTP_RATE_LIMITED) }
        val model = viewModel(auth, SavedStateHandle()) { testScheduler.currentTime }
        model.onEvent(initialized(resendAfter = 0))
        model.onEvent(OtpVerificationUiEvent.ResendClicked)
        runCurrent()

        assertEquals(R.string.otp_rate_limited, model.uiState.value.errorResId)
        assertEquals(0, model.uiState.value.remainingSeconds)
        assertFalse(model.uiState.value.isResending)
        model.viewModelScope.cancel()
    }

    private fun initialized(expiresIn: Int = 600, resendAfter: Int = 2) =
        OtpVerificationUiEvent.Initialized(
            "user@example.test", OtpPurpose.PASSWORD_RESET, OtpChallenge("first", expiresIn, resendAfter)
        )

    private fun enterCode(model: OtpVerificationViewModel) {
        "012345".forEachIndexed { index, digit ->
            model.onEvent(OtpVerificationUiEvent.DigitChanged(index, digit.toString()))
        }
        model.onEvent(OtpVerificationUiEvent.SubmitClicked)
    }

    private fun viewModel(auth: TestAuthRepository, state: SavedStateHandle, now: () -> Long) =
        OtpVerificationViewModel(VerifyOtpUseCase(auth), ResendOtpUseCase(auth), state, now)

    private class TestAuthRepository : AuthRepository {
        var resendResult: AuthResult<OtpChallenge> = AuthResult.Success(OtpChallenge("replacement", 600, 3))
        var resendCount = 0
        var verifiedChallengeId: String? = null

        override suspend fun resendOtp(email: String, purpose: OtpPurpose): AuthResult<OtpChallenge> {
            resendCount++
            assertEquals(OtpPurpose.PASSWORD_RESET, purpose)
            return resendResult
        }

        override suspend fun verifyOtp(challengeId: String, code: String, purpose: OtpPurpose): AuthResult<OtpVerification> {
            verifiedChallengeId = challengeId
            assertEquals("012345", code)
            assertEquals(OtpPurpose.PASSWORD_RESET, purpose)
            return AuthResult.Success(OtpVerification.PasswordResetVerified("test-proof"))
        }

        override suspend fun restoreSession() = unexpected()
        override fun currentSessionKey() = null
        override fun observeSessionKeys() = flowOf(null)
        override suspend fun authenticate(username: String, password: String) = unexpected()
        override suspend fun register(fullName: String, email: String, password: String) = unexpected()
        override suspend fun requestPasswordReset(email: String) = unexpected()
        override suspend fun confirmPasswordReset(challengeId: String, verificationToken: String, newPassword: String) = unexpected()
        override suspend fun logout() = unexpected()
        override suspend fun changePassword(currentPassword: String, newPassword: String) = unexpected()

        private fun unexpected(): Nothing = error("Unexpected auth operation")
    }
}
