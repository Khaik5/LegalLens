package com.example.lagallens.presentation.feature.profile.edit.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEffect
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ProfileEditViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private val viewModel = ProfileEditViewModel()

    private fun open(phone: String) =
        viewModel.onEvent(ProfileEditUiEvent.Initialize("Nguyễn Văn A", "a@example.com", phone, null))

    @Test fun accountWithoutPhoneCanSaveANewName() = runTest {
        open(phone = "")
        viewModel.onEvent(ProfileEditUiEvent.NameChanged(" Trần Thị B "))
        viewModel.onEvent(ProfileEditUiEvent.SaveClicked)

        assertNull(viewModel.uiState.value.phoneErrorRes)
        assertEquals(ProfileEditUiEffect.ProfileSaved("Trần Thị B", "", null), viewModel.uiEffect.first())
    }

    @Test fun enteredPhoneMustStillBeValid() = runTest {
        open(phone = "")
        viewModel.onEvent(ProfileEditUiEvent.PhoneChanged("0901"))
        viewModel.onEvent(ProfileEditUiEvent.SaveClicked)

        assertEquals(R.string.profile_edit_phone_invalid, viewModel.uiState.value.phoneErrorRes)
        assertNull(withTimeoutOrNull(100) { viewModel.uiEffect.first() })
    }

    @Test fun blankNameIsRejected() = runTest {
        open(phone = "0901234567")
        viewModel.onEvent(ProfileEditUiEvent.NameChanged("  "))
        viewModel.onEvent(ProfileEditUiEvent.SaveClicked)

        assertEquals(R.string.profile_edit_name_required, viewModel.uiState.value.nameErrorRes)
    }
}
