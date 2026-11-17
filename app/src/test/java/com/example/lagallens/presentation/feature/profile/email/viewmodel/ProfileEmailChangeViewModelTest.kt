package com.example.lagallens.presentation.feature.profile.email.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.UserProfile
import com.example.lagallens.domain.repository.EmailAlreadyUsedException
import com.example.lagallens.domain.repository.EmailChangeChallenge
import com.example.lagallens.domain.repository.EmailChangeCodeRejectedException
import com.example.lagallens.domain.repository.EmailChangeRejectedException
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeStep
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiEffect
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class ProfileEmailChangeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class Profiles : ProfileRepository {
        var requestFailure: Exception? = null
        var confirmFailure: Exception? = null
        val requests = mutableListOf<Pair<String, String>>()
        val confirmations = mutableListOf<Pair<String, String>>()

        override suspend fun getProfile() = UserProfile("A", "old@example.test")
        override suspend fun saveProfile(name: String, phone: String, photoUri: String?) = getProfile()
        override suspend fun removeAvatar() = getProfile()
        override val canChangeEmail = true

        override suspend fun requestEmailChange(newEmail: String, currentPassword: String): EmailChangeChallenge {
            requests += newEmail to currentPassword
            requestFailure?.let { throw it }
            return EmailChangeChallenge("challenge-1", 600)
        }

        override suspend fun confirmEmailChange(challengeId: String, code: String): String {
            confirmations += challengeId to code
            confirmFailure?.let { throw it }
            return "new@example.test"
        }
    }

    private val profiles = Profiles()
    private val viewModel = ProfileEmailChangeViewModel(profiles, "old@example.test")

    private fun fillRequest(email: String = "new@example.test", password: String = "secret") {
        viewModel.onEvent(ProfileEmailChangeUiEvent.NewEmailChanged(email))
        viewModel.onEvent(ProfileEmailChangeUiEvent.CurrentPasswordChanged(password))
        viewModel.onEvent(ProfileEmailChangeUiEvent.SendCodeClicked)
    }

    @Test fun anInvalidEmailOrMissingPasswordIsRejectedWithoutACall() = runTest {
        fillRequest(email = "not-an-email", password = "")

        assertEquals(R.string.profile_email_change_email_invalid, viewModel.uiState.value.emailErrorRes)
        assertEquals(R.string.profile_password_current_required, viewModel.uiState.value.passwordErrorRes)
        assertEquals(emptyList<Pair<String, String>>(), profiles.requests)
    }

    @Test fun theCurrentEmailIsRejectedEvenInAnotherCase() = runTest {
        fillRequest(email = "OLD@example.test")

        assertEquals(R.string.profile_email_change_email_same, viewModel.uiState.value.emailErrorRes)
        assertEquals(emptyList<Pair<String, String>>(), profiles.requests)
    }

    @Test fun aValidRequestMovesToTheCodeStepAndForgetsThePassword() = runTest {
        fillRequest(email = " new@example.test ")

        val state = viewModel.uiState.value
        assertEquals(ProfileEmailChangeStep.CONFIRM, state.step)
        assertEquals("new@example.test", state.newEmail)
        assertEquals("", state.currentPassword)
        assertEquals(listOf("new@example.test" to "secret"), profiles.requests)
    }

    @Test fun aWrongPasswordIsShownOnThePasswordField() = runTest {
        profiles.requestFailure = EmailChangeRejectedException(Exception())

        fillRequest()

        assertEquals(ProfileEmailChangeStep.REQUEST, viewModel.uiState.value.step)
        assertEquals(R.string.profile_email_change_password_incorrect, viewModel.uiState.value.passwordErrorRes)
    }

    @Test fun anEmailUsedByAnotherAccountIsShownOnTheEmailField() = runTest {
        profiles.requestFailure = EmailAlreadyUsedException(Exception())

        fillRequest()

        assertEquals(R.string.profile_email_change_email_used, viewModel.uiState.value.emailErrorRes)
    }

    @Test fun aCodeThatIsNotSixDigitsIsRejectedWithoutACall() = runTest {
        fillRequest()
        viewModel.onEvent(ProfileEmailChangeUiEvent.CodeChanged("12ab"))
        viewModel.onEvent(ProfileEmailChangeUiEvent.ConfirmClicked)

        assertEquals(R.string.profile_email_change_code_invalid, viewModel.uiState.value.codeErrorRes)
        assertEquals(emptyList<Pair<String, String>>(), profiles.confirmations)
    }

    @Test fun aRejectedCodeStaysOnTheCodeStep() = runTest {
        fillRequest()
        profiles.confirmFailure = EmailChangeCodeRejectedException(Exception())
        viewModel.onEvent(ProfileEmailChangeUiEvent.CodeChanged("123456"))
        viewModel.onEvent(ProfileEmailChangeUiEvent.ConfirmClicked)

        assertEquals(ProfileEmailChangeStep.CONFIRM, viewModel.uiState.value.step)
        assertEquals(R.string.profile_email_change_code_invalid, viewModel.uiState.value.codeErrorRes)
    }

    @Test fun aConfirmedChangeSendsTheUserToSignIn() = runTest {
        fillRequest()
        viewModel.onEvent(ProfileEmailChangeUiEvent.CodeChanged("123456"))
        viewModel.onEvent(ProfileEmailChangeUiEvent.ConfirmClicked)

        assertEquals(ProfileEmailChangeUiEffect.NavigateToLogin, viewModel.uiEffect.first())
        assertEquals(listOf("challenge-1" to "123456"), profiles.confirmations)
    }

    @Test fun anAddressTakenMeanwhileReturnsToTheFirstStep() = runTest {
        fillRequest()
        profiles.confirmFailure = EmailAlreadyUsedException(HttpException(Response.error<Any>(409, "{}".toResponseBody())))
        viewModel.onEvent(ProfileEmailChangeUiEvent.CodeChanged("123456"))
        viewModel.onEvent(ProfileEmailChangeUiEvent.ConfirmClicked)

        val state = viewModel.uiState.value
        assertEquals(ProfileEmailChangeStep.REQUEST, state.step)
        assertEquals(R.string.profile_email_change_email_used, state.emailErrorRes)
        assertNull(state.codeErrorRes)
    }
}
