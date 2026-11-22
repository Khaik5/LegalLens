package com.example.lagallens.presentation.feature.register.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.ChangePasswordResult
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.model.auth.OtpVerification
import com.example.lagallens.domain.model.auth.SessionRestoreResult
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.usecase.RegisterAccountUseCase
import com.example.lagallens.domain.usecase.ResendOtpUseCase
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEffect
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEvent
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private class Auth(var registration: AuthResult<OtpChallenge?>) : AuthRepository {
        override fun currentSessionKey(): String? = null
        override fun observeSessionKeys() = flowOf(null)
        override suspend fun restoreSession(): SessionRestoreResult = error("unused")
        override suspend fun authenticate(username: String, password: String): Boolean = error("unused")
        override suspend fun register(fullName: String, email: String, password: String) = registration
        override suspend fun requestPasswordReset(email: String): AuthResult<OtpChallenge> = error("unused")
        override suspend fun resendOtp(email: String, purpose: OtpPurpose): AuthResult<OtpChallenge> = error("unused")
        override suspend fun verifyOtp(challengeId: String, code: String, purpose: OtpPurpose): AuthResult<OtpVerification> = error("unused")
        override suspend fun confirmPasswordReset(challengeId: String, verificationToken: String, newPassword: String): AuthResult<Unit> = error("unused")
        override suspend fun logout() = false
        override suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResult = error("unused")
    }

    private fun viewModel(auth: Auth) = RegisterViewModel(RegisterAccountUseCase(auth), ResendOtpUseCase(auth)).apply {
        onEvent(RegisterUiEvent.FullNameChanged("Test User"))
        onEvent(RegisterUiEvent.EmailChanged("user@example.com"))
        onEvent(RegisterUiEvent.PasswordChanged("Abcd1234x"))
        onEvent(RegisterUiEvent.ConfirmPasswordChanged("Abcd1234x"))
    }

    @Test fun aFailureThatIsNotAboutThePasswordIsShownAsAMessageNotAsAPasswordError() = runTest {
        val vm = viewModel(Auth(AuthResult.Failed(AuthFailure.REJECTED)))

        vm.onEvent(RegisterUiEvent.SubmitClicked)
        val effect = vm.uiEffect.first()

        assertTrue(effect is RegisterUiEffect.ShowMessage)
        assertNull(vm.uiState.value.passwordErrorResId)
        assertEquals(false, vm.uiState.value.isLoading)
        vm.viewModelScope.cancel()
    }

    @Test fun aTakenEmailIsAFieldErrorAndOffersToResendTheCode() = runTest {
        val vm = viewModel(Auth(AuthResult.Failed(AuthFailure.EMAIL_TAKEN)))

        vm.onEvent(RegisterUiEvent.SubmitClicked)

        assertEquals(R.string.register_email_taken, vm.uiState.value.emailErrorResId)
        assertNull(vm.uiState.value.passwordErrorResId)
        assertTrue(vm.uiState.value.canResendVerification)
        vm.viewModelScope.cancel()
    }
}
