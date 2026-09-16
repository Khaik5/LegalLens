package com.example.lagallens.presentation.feature.profile.edit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentProfileEditBinding
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEvent
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEffect
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEvent
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiState
import com.example.lagallens.presentation.feature.profile.edit.viewmodel.ProfileEditViewModel
import com.example.lagallens.presentation.feature.profile.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileEditFragment : Fragment() {
    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel by activityViewModels<ProfileViewModel>()
    private val viewModel by viewModels<ProfileEditViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
        collectProfileState()
        collectUiState()
        collectUiEffect()
    }

    private fun bindView() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnChangePhoto.setOnClickListener {
            viewModel.onEvent(ProfileEditUiEvent.ChangePhotoClicked)
        }
        binding.etProfileName.doAfterTextChanged {
            viewModel.onEvent(ProfileEditUiEvent.NameChanged(it?.toString().orEmpty()))
        }
        binding.etProfilePhone.doAfterTextChanged {
            viewModel.onEvent(ProfileEditUiEvent.PhoneChanged(it?.toString().orEmpty()))
        }
        binding.btnSaveProfile.setOnClickListener {
            viewModel.onEvent(ProfileEditUiEvent.SaveClicked)
        }
    }

    private fun collectProfileState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.uiState.collect { profile ->
                    viewModel.onEvent(
                        ProfileEditUiEvent.Initialize(profile.name, profile.email, profile.phone)
                    )
                }
            }
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun collectUiEffect() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        ProfileEditUiEffect.ShowPhotoPickerUnavailable -> showPhotoPickerUnavailable()
                        is ProfileEditUiEffect.ProfileSaved -> saveProfile(effect)
                    }
                }
            }
        }
    }

    private fun render(state: ProfileEditUiState) {
        binding.tvProfileEditAvatar.text = state.name.initials()
        updateInput(binding.etProfileName, state.name)
        updateInput(binding.etProfileEmail, state.email)
        updateInput(binding.etProfilePhone, state.phone)
        renderError(binding.tvProfileNameError, state.nameErrorRes)
        renderError(binding.tvProfilePhoneError, state.phoneErrorRes)
    }

    private fun updateInput(input: android.widget.EditText, value: String) {
        if (input.text.toString() != value) {
            input.setText(value)
            input.setSelection(value.length)
        }
    }

    private fun renderError(errorView: android.widget.TextView, errorRes: Int?) {
        errorView.isVisible = errorRes != null
        if (errorRes != null) {
            errorView.setText(errorRes)
        }
    }

    private fun showPhotoPickerUnavailable() {
        Toast.makeText(
            requireContext(),
            R.string.profile_edit_photo_unavailable,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveProfile(effect: ProfileEditUiEffect.ProfileSaved) {
        profileViewModel.onEvent(ProfileUiEvent.ProfileUpdated(effect.name, effect.phone))
        Toast.makeText(requireContext(), R.string.profile_edit_save_success, Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    private fun String.initials(): String =
        trim().split(" ").filter(String::isNotBlank).takeLast(2).joinToString("") { it.first().toString() }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
