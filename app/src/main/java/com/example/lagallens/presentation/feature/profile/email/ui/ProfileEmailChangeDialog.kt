package com.example.lagallens.presentation.feature.profile.email.ui

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.DialogProfileEmailChangeBinding
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeStep
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiEffect
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiEvent
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiState
import com.example.lagallens.presentation.feature.profile.email.viewmodel.ProfileEmailChangeViewModel
import kotlinx.coroutines.launch

/** Two steps in one dialog: the new address with the current password, then the code sent to that address. */
class ProfileEmailChangeDialog : DialogFragment() {
    private var _binding: DialogProfileEmailChangeBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<ProfileEmailChangeViewModel> {
        val app = requireActivity().application as LegalLensApplication
        ProfileEmailChangeViewModel.Factory(app.userProfileRepository, requireArguments().getString(CURRENT_EMAIL_KEY).orEmpty())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(DIM_AMOUNT)
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogProfileEmailChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // A sign-in credential or code on screen must not be lost to a stray tap outside the dialog.
        isCancelable = false
        binding.etEmailChangeNewEmail.doAfterTextChanged { viewModel.onEvent(ProfileEmailChangeUiEvent.NewEmailChanged(it?.toString().orEmpty())) }
        binding.etEmailChangePassword.doAfterTextChanged { viewModel.onEvent(ProfileEmailChangeUiEvent.CurrentPasswordChanged(it?.toString().orEmpty())) }
        binding.etEmailChangeCode.doAfterTextChanged { viewModel.onEvent(ProfileEmailChangeUiEvent.CodeChanged(it?.toString().orEmpty())) }
        binding.btnEmailChangeCancel.setOnClickListener { dismiss() }
        binding.btnEmailChangeSubmit.setOnClickListener {
            val state = viewModel.uiState.value
            viewModel.onEvent(
                if (state.step == ProfileEmailChangeStep.REQUEST) ProfileEmailChangeUiEvent.SendCodeClicked
                else ProfileEmailChangeUiEvent.ConfirmClicked
            )
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.uiEffect.collect(::handle) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun render(state: ProfileEmailChangeUiState) {
        val isRequest = state.step == ProfileEmailChangeStep.REQUEST
        binding.groupEmailRequest.isVisible = isRequest
        binding.groupEmailConfirm.isVisible = !isRequest
        binding.tvEmailChangeCodeMessage.text = getString(R.string.profile_email_change_code_message, state.newEmail)
        binding.btnEmailChangeSubmit.setText(
            if (isRequest) R.string.profile_email_change_send_code else R.string.profile_email_change_confirm
        )
        binding.btnEmailChangeSubmit.isEnabled = !state.isSubmitting
        binding.btnEmailChangeSubmit.alpha = if (state.isSubmitting) DISABLED_ALPHA else 1f
        updateInput(binding.etEmailChangeNewEmail, state.newEmail)
        updateInput(binding.etEmailChangePassword, state.currentPassword)
        updateInput(binding.etEmailChangeCode, state.code)
        renderError(binding.tvEmailChangeEmailError, state.emailErrorRes)
        renderError(binding.tvEmailChangePasswordError, state.passwordErrorRes)
        renderError(binding.tvEmailChangeCodeError, state.codeErrorRes)
    }

    private fun handle(effect: ProfileEmailChangeUiEffect) {
        when (effect) {
            is ProfileEmailChangeUiEffect.ShowError -> Toast.makeText(requireContext(), effect.messageRes, Toast.LENGTH_SHORT).show()
            ProfileEmailChangeUiEffect.NavigateToLogin -> {
                val host = requireActivity()
                Toast.makeText(host.applicationContext, R.string.profile_email_change_success, Toast.LENGTH_LONG).show()
                host.startActivity(Intent(host, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                host.finish()
            }
        }
    }

    private fun updateInput(input: EditText, value: String) {
        if (input.text.toString() != value) {
            input.setText(value)
            input.setSelection(value.length)
        }
    }

    private fun renderError(errorView: TextView, errorRes: Int?) {
        errorView.isVisible = errorRes != null
        if (errorRes != null) errorView.setText(errorRes)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "profile_email_change"
        private const val CURRENT_EMAIL_KEY = "current_email"
        private const val DIM_AMOUNT = 0.4f
        private const val DISABLED_ALPHA = 0.5f

        fun newInstance(currentEmail: String) = ProfileEmailChangeDialog().apply {
            arguments = Bundle().apply { putString(CURRENT_EMAIL_KEY, currentEmail) }
        }
    }
}
