package com.example.lagallens.presentation.feature.profile.password.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.lagallens.R
import com.example.lagallens.databinding.FragmentProfilePasswordBinding
import com.example.lagallens.databinding.ItemProfilePasswordRequirementBinding
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiEffect
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiEvent
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiState
import com.example.lagallens.presentation.feature.profile.password.viewmodel.ProfilePasswordViewModel
import kotlinx.coroutines.launch

class ProfilePasswordFragment : Fragment() {
    private var _binding: FragmentProfilePasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfilePasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews()
        observeViewModel()
    }

    private fun bindViews() = with(binding) {
        btnBack.setOnClickListener {
            viewModel.onEvent(ProfilePasswordUiEvent.BackClicked)
        }
        btnUpdatePassword.setOnClickListener {
            viewModel.onEvent(ProfilePasswordUiEvent.SubmitClicked)
        }
        etCurrentPassword.doAfterTextChanged {
            viewModel.onEvent(ProfilePasswordUiEvent.CurrentPasswordChanged(it.toString()))
        }
        etNewPassword.doAfterTextChanged {
            viewModel.onEvent(ProfilePasswordUiEvent.NewPasswordChanged(it.toString()))
        }
        etConfirmationPassword.doAfterTextChanged {
            viewModel.onEvent(ProfilePasswordUiEvent.ConfirmationChanged(it.toString()))
        }

        requirementMinimumLength.tvPasswordRequirement.setText(R.string.profile_password_minimum_requirement)
        requirementMixedCase.tvPasswordRequirement.setText(R.string.profile_password_case_requirement)
        requirementNumberOrSpecial.tvPasswordRequirement.setText(R.string.profile_password_number_requirement)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: ProfilePasswordUiState) = with(binding) {
        updateText(etCurrentPassword, state.currentPassword)
        updateText(etNewPassword, state.newPassword)
        updateText(etConfirmationPassword, state.confirmation)
        renderError(tvCurrentPasswordError, state.currentPasswordErrorRes)
        renderError(tvNewPasswordError, state.newPasswordErrorRes)
        renderError(tvConfirmationPasswordError, state.confirmationErrorRes)
        renderRequirement(requirementMinimumLength, state.hasMinimumLength)
        renderRequirement(requirementMixedCase, state.hasMixedCase)
        renderRequirement(requirementNumberOrSpecial, state.hasNumberOrSpecial)
    }

    private fun updateText(editText: EditText, value: String) {
        if (editText.text.toString() != value) {
            editText.setText(value)
            editText.setSelection(value.length)
        }
    }

    private fun renderError(view: TextView, errorRes: Int?) {
        view.isVisible = errorRes != null
        if (errorRes != null) {
            view.setText(errorRes)
        }
    }

    private fun renderRequirement(
        requirement: ItemProfilePasswordRequirementBinding,
        isSatisfied: Boolean
    ) {
        requirement.ivPasswordRequirement.setImageResource(
            if (isSatisfied) R.drawable.ic_profile_password_check else R.drawable.ic_profile_password_circle
        )
    }

    private fun handleEffect(effect: ProfilePasswordUiEffect) {
        when (effect) {
            ProfilePasswordUiEffect.NavigateBack -> findNavController().navigateUp()
            ProfilePasswordUiEffect.ShowBackendUnavailable -> {
                Toast.makeText(
                    requireContext(),
                    R.string.profile_password_backend_unavailable,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}


