package com.example.lagallens.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.model.auth.*
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.EmailChangeChallenge
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.domain.usecase.*
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiEvent
import com.example.lagallens.presentation.feature.forgotpassword.viewmodel.ForgotPasswordViewModel
import com.example.lagallens.presentation.feature.login.contract.LoginUiEvent
import com.example.lagallens.presentation.feature.login.viewmodel.LoginViewModel
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEvent
import com.example.lagallens.presentation.feature.otp.viewmodel.OtpVerificationViewModel
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiEvent
import com.example.lagallens.presentation.feature.profile.email.viewmodel.ProfileEmailChangeViewModel
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiEvent
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiState
import com.example.lagallens.presentation.feature.profile.password.viewmodel.ProfilePasswordViewModel
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEvent
import com.example.lagallens.presentation.feature.register.viewmodel.RegisterViewModel
import com.example.lagallens.presentation.feature.resetpassword.contract.ResetPasswordUiEvent
import com.example.lagallens.presentation.feature.resetpassword.viewmodel.ResetPasswordViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GroupAValidationRegressionTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun currentPasswordLengthErrorsReferSpecificallyToTheCurrentPassword() = runTest {
        for (length in listOf(7, 73)) {
            val auth = Auth()
            val vm = ProfilePasswordViewModel(ChangePasswordUseCase(auth))
            vm.onEvent(ProfilePasswordUiEvent.CurrentPasswordChanged("a".repeat(length)))
            vm.onEvent(ProfilePasswordUiEvent.NewPasswordChanged("new-password"))
            vm.onEvent(ProfilePasswordUiEvent.ConfirmationChanged("new-password"))
            vm.onEvent(ProfilePasswordUiEvent.SubmitClicked)
            assertEquals(R.string.profile_password_current_length_invalid, vm.uiState.value.currentPasswordErrorRes)
            assertNull(vm.uiState.value.newPasswordErrorRes)
            assertEquals(0, auth.calls)
            vm.viewModelScope.cancel()
        }
    }

    @Test fun changePasswordAcceptsAnyCompositionWithinEightToSeventyTwoCharacters() {
        listOf("abcdefgh", "12345678", "A".repeat(72)).forEach { password ->
            assertTrue(password.length.toString(), ProfilePasswordUiState(newPassword = password).meetsSecurityRequirements)
        }
        listOf("a".repeat(7), "Ab1".repeat(25)).forEach { password ->
            assertFalse(password.length.toString(), ProfilePasswordUiState(newPassword = password).meetsSecurityRequirements)
        }
    }

    @Test fun resetRejectsOverlongPasswordBeforeCallingBackend() = runTest {
        val auth = Auth()
        val vm = ResetPasswordViewModel(ConfirmPasswordResetUseCase(auth))
        vm.onEvent(ResetPasswordUiEvent.Initialized("challenge", "proof"))
        vm.onEvent(ResetPasswordUiEvent.PasswordChanged("a".repeat(73)))
        vm.onEvent(ResetPasswordUiEvent.ConfirmPasswordChanged("a".repeat(73)))
        vm.onEvent(ResetPasswordUiEvent.SubmitClicked)
        assertNotNull(vm.uiState.value.passwordErrorResId)
        assertEquals(0, auth.calls)
        vm.viewModelScope.cancel()
    }

    @Test fun registerRejectsOverlongPasswordBeforeCallingBackend() = runTest {
        val auth = Auth()
        val vm = registration(auth, "user@example.com", "a".repeat(73))
        vm.onEvent(RegisterUiEvent.SubmitClicked)
        assertNotNull(vm.uiState.value.passwordErrorResId)
        assertEquals(0, auth.calls)
        vm.viewModelScope.cancel()
    }

    @Test fun changeRejectsOverlongCurrentAndNewPasswordsBeforeCallingBackend() = runTest {
        val auth = Auth()
        val vm = ProfilePasswordViewModel(ChangePasswordUseCase(auth))
        vm.onEvent(ProfilePasswordUiEvent.CurrentPasswordChanged("a".repeat(73)))
        vm.onEvent(ProfilePasswordUiEvent.NewPasswordChanged("Ab1".repeat(25)))
        vm.onEvent(ProfilePasswordUiEvent.ConfirmationChanged("Ab1".repeat(25)))
        vm.onEvent(ProfilePasswordUiEvent.SubmitClicked)
        assertNotNull(vm.uiState.value.currentPasswordErrorRes)
        assertNotNull(vm.uiState.value.newPasswordErrorRes)
        assertEquals(0, auth.calls)
        vm.viewModelScope.cancel()
    }

    @Test fun registerResetAndChangeAcceptBoundaryPasswordsAndLoginHasNoMaximum() = runTest {
        for (length in listOf(8, 72)) {
            val auth = Auth()
            val register = registration(auth, "user@example.com", "a".repeat(length))
            register.onEvent(RegisterUiEvent.SubmitClicked)
            assertNull(register.uiState.value.passwordErrorResId)
            val reset = ResetPasswordViewModel(ConfirmPasswordResetUseCase(auth))
            reset.onEvent(ResetPasswordUiEvent.Initialized("challenge", "proof"))
            reset.onEvent(ResetPasswordUiEvent.PasswordChanged("a".repeat(length)))
            reset.onEvent(ResetPasswordUiEvent.ConfirmPasswordChanged("a".repeat(length)))
            reset.onEvent(ResetPasswordUiEvent.SubmitClicked)
            assertNull(reset.uiState.value.passwordErrorResId)
            val change = ProfilePasswordViewModel(ChangePasswordUseCase(auth))
            change.onEvent(ProfilePasswordUiEvent.CurrentPasswordChanged("old-password"))
            change.onEvent(ProfilePasswordUiEvent.NewPasswordChanged("a".repeat(length)))
            change.onEvent(ProfilePasswordUiEvent.ConfirmationChanged("a".repeat(length)))
            change.onEvent(ProfilePasswordUiEvent.SubmitClicked)
            assertEquals(3, auth.calls)
            listOf(register, reset, change).forEach { it.viewModelScope.cancel() }
        }
        val auth = Auth()
        val login = LoginViewModel(AuthenticateUserUseCase(auth), ResendOtpUseCase(auth))
        login.onEvent(LoginUiEvent.EmailChanged("user@example.com"))
        login.onEvent(LoginUiEvent.PasswordChanged("a".repeat(73)))
        login.onEvent(LoginUiEvent.SubmitClicked)
        assertNull(login.uiState.value.passwordErrorResId)
        assertEquals(1, auth.calls)
        login.viewModelScope.cancel()
    }

    @Test fun allFourEmailScreensAgreeOnValidAndInvalidAddresses() = runTest {
        val cases = mapOf(
            "user@example.com" to true,
            "user+tag@example.com" to true,
            "o'connor@example.com" to true,
            "a@b" to false,
            "example.com" to false,
            "user name@example.com" to false
        )
        cases.forEach { (email, expected) ->
            val auth = Auth()
            val register = registration(auth, email)
            register.onEvent(RegisterUiEvent.SubmitClicked)
            assertEquals("Register: $email", expected, register.uiState.value.emailErrorResId == null)
            val forgot = ForgotPasswordViewModel(RequestPasswordResetUseCase(auth))
            forgot.onEvent(ForgotPasswordUiEvent.EmailChanged(email))
            forgot.onEvent(ForgotPasswordUiEvent.SubmitClicked)
            assertEquals("Forgot: $email", expected, forgot.uiState.value.emailErrorResId == null)
            val login = LoginViewModel(AuthenticateUserUseCase(auth), ResendOtpUseCase(auth))
            login.onEvent(LoginUiEvent.EmailChanged(email))
            login.onEvent(LoginUiEvent.PasswordChanged("12345678"))
            login.onEvent(LoginUiEvent.SubmitClicked)
            assertEquals("Login: $email", expected, login.uiState.value.usernameErrorResId == null)
            val change = ProfileEmailChangeViewModel(Profiles, "previous@example.com")
            change.onEvent(ProfileEmailChangeUiEvent.NewEmailChanged(email))
            change.onEvent(ProfileEmailChangeUiEvent.CurrentPasswordChanged("12345678"))
            change.onEvent(ProfileEmailChangeUiEvent.SendCodeClicked)
            assertEquals("Change: $email", expected, change.uiState.value.emailErrorRes == null)
            listOf(register, forgot, login, change).forEach { it.viewModelScope.cancel() }
        }
    }

    @Test fun pastedOtpIsDistributedAndSubmittedWithoutDroppingLeadingZero() = runTest {
        val auth = Auth()
        val vm = otp(auth, SavedStateHandle())
        vm.onEvent(OtpVerificationUiEvent.DigitChanged(0, "012345"))
        assertEquals(listOf("0", "1", "2", "3", "4", "5"), vm.uiState.value.digits)
        vm.onEvent(OtpVerificationUiEvent.SubmitClicked)
        assertEquals("012345", auth.verifiedCode)
        vm.viewModelScope.cancel()
    }

    @Test fun partialOtpWithGapsSurvivesProcessStateRecreation() = runTest {
        val state = SavedStateHandle()
        val vm = otp(Auth(), state)
        vm.onEvent(OtpVerificationUiEvent.DigitChanged(0, "0"))
        vm.onEvent(OtpVerificationUiEvent.DigitChanged(3, "7"))
        vm.viewModelScope.cancel()
        val restoredState = SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) })
        val restored = otp(Auth(), restoredState)
        assertEquals(listOf("0", "", "", "7", "", ""), restored.uiState.value.digits)
        restored.viewModelScope.cancel()
    }

    @Test fun pasteAtMiddleStopsAtLastBoxAndIgnoresNonAsciiDigits() = runTest {
        val vm = otp(Auth(), SavedStateHandle())
        vm.onEvent(OtpVerificationUiEvent.DigitChanged(2, "12x34567"))
        assertEquals(listOf("", "", "1", "2", "3", "4"), vm.uiState.value.digits)
        vm.onEvent(OtpVerificationUiEvent.DigitChanged(2, ""))
        assertEquals("", vm.uiState.value.digits[2])
        vm.onEvent(OtpVerificationUiEvent.DigitChanged(1, "９"))
        assertEquals("", vm.uiState.value.digits[1])
        vm.viewModelScope.cancel()
    }

    @Test fun resendingClearsSavedOtpSoOldDigitsDoNotReturn() = runTest {
        val state = SavedStateHandle()
        val vm = otp(Auth(), state)
        "012345".forEachIndexed { index, digit -> vm.onEvent(OtpVerificationUiEvent.DigitChanged(index, digit.toString())) }
        vm.onEvent(OtpVerificationUiEvent.ResendClicked)
        vm.viewModelScope.cancel()
        val restored = otp(Auth(), SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) }))
        assertEquals(List(6) { "" }, restored.uiState.value.digits)
        restored.viewModelScope.cancel()
    }

    private fun registration(auth: Auth, email: String, password: String = "12345678") =
        RegisterViewModel(RegisterAccountUseCase(auth), ResendOtpUseCase(auth)).apply {
            onEvent(RegisterUiEvent.FullNameChanged("Test User"))
            onEvent(RegisterUiEvent.EmailChanged(email))
            onEvent(RegisterUiEvent.PasswordChanged(password))
            onEvent(RegisterUiEvent.ConfirmPasswordChanged(password))
        }

    private fun otp(auth: Auth, state: SavedStateHandle) =
        OtpVerificationViewModel(VerifyOtpUseCase(auth), ResendOtpUseCase(auth), state) { 0L }.apply {
            onEvent(OtpVerificationUiEvent.Initialized("user@example.com", OtpPurpose.REGISTRATION, OtpChallenge("challenge", 600, 0)))
        }

    private object Profiles : ProfileRepository {
        override suspend fun getProfile() = UserProfile(name = "Test", email = "previous@example.com")
        override suspend fun saveProfile(name: String, phone: String, photoUri: String?) = getProfile()
        override suspend fun removeAvatar() = getProfile()
        override suspend fun requestEmailChange(newEmail: String, currentPassword: String) = EmailChangeChallenge("challenge", 600)
    }

    private class Auth : AuthRepository {
        var calls = 0
        var verifiedCode: String? = null
        override fun currentSessionKey(): String? = null
        override fun observeSessionKeys() = flowOf(null)
        override suspend fun restoreSession(): SessionRestoreResult = error("unused")
        override suspend fun authenticate(username: String, password: String): Boolean { calls++; return true }
        override suspend fun register(fullName: String, email: String, password: String): AuthResult<OtpChallenge?> { calls++; return AuthResult.Success(null) }
        override suspend fun requestPasswordReset(email: String): AuthResult<OtpChallenge> { calls++; return AuthResult.Success(OtpChallenge("challenge", 600, 0)) }
        override suspend fun resendOtp(email: String, purpose: OtpPurpose) = AuthResult.Success(OtpChallenge("replacement", 600, 0))
        override suspend fun verifyOtp(challengeId: String, code: String, purpose: OtpPurpose): AuthResult<OtpVerification> { verifiedCode = code; return AuthResult.Success(OtpVerification.Verified) }
        override suspend fun confirmPasswordReset(challengeId: String, verificationToken: String, newPassword: String): AuthResult<Unit> { calls++; return AuthResult.Success(Unit) }
        override suspend fun logout() = false
        override suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResult { calls++; return ChangePasswordResult.Updated }
    }
}
