package com.example.lagallens.presentation.feature.resetpassword.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityResetPasswordBinding
import com.example.lagallens.presentation.common.utils.animateLoadingFeedback
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.resetpassword.contract.ResetPasswordUiEffect
import com.example.lagallens.presentation.feature.resetpassword.contract.ResetPasswordUiEvent
import com.example.lagallens.presentation.feature.resetpassword.viewmodel.ResetPasswordViewModel
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {
    private val binding by lazy { ActivityResetPasswordBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<ResetPasswordViewModel> {
        ResetPasswordViewModel.Factory((application as LegalLensApplication).confirmPasswordResetUseCase)
    }
    private var wasLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        viewModel.onEvent(ResetPasswordUiEvent.Initialized(intent.getStringExtra(EXTRA_EMAIL).orEmpty()))
        bindView()
        collectState()
        collectEffect()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() {
        binding.backButton.bindPressFeedback()
        binding.submitButton.bindPressFeedback()
        binding.backButton.setOnClickListener { viewModel.onEvent(ResetPasswordUiEvent.BackClicked) }
        binding.passwordInput.doAfterTextChanged {
            viewModel.onEvent(ResetPasswordUiEvent.PasswordChanged(it?.toString().orEmpty()))
        }
        binding.confirmPasswordInput.doAfterTextChanged {
            viewModel.onEvent(ResetPasswordUiEvent.ConfirmPasswordChanged(it?.toString().orEmpty()))
        }
        binding.submitButton.setOnClickListener { viewModel.onEvent(ResetPasswordUiEvent.SubmitClicked) }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.passwordInput.error = state.passwordErrorResId?.let(::getString)
                    binding.confirmPasswordInput.error = state.confirmPasswordErrorResId?.let(::getString)
                    binding.submitButton.isEnabled = !state.isLoading
                    if (wasLoading != state.isLoading) {
                        binding.submitButton.animateLoadingFeedback(state.isLoading)
                        wasLoading = state.isLoading
                    }
                }
            }
        }
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        ResetPasswordUiEffect.CloseScreen -> finish()
                        ResetPasswordUiEffect.NavigateToLogin -> {
                            Toast.makeText(this@ResetPasswordActivity, R.string.reset_password_success, Toast.LENGTH_LONG).show()
                            startActivity(
                                Intent(this@ResetPasswordActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_EMAIL = "extra_email"

        fun createIntent(context: Context, email: String): Intent =
            Intent(context, ResetPasswordActivity::class.java).putExtra(EXTRA_EMAIL, email)
    }
}
