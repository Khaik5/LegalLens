package com.example.lagallens.presentation.feature.forgotpassword.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.databinding.ActivityForgotPasswordBinding
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.otp.ui.OtpVerificationActivity
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiEffect
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiEvent
import com.example.lagallens.presentation.feature.forgotpassword.viewmodel.ForgotPasswordViewModel
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private val binding by lazy { ActivityForgotPasswordBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<ForgotPasswordViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
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
        binding.backButton.setOnClickListener {
            viewModel.onEvent(ForgotPasswordUiEvent.BackClicked)
        }
        binding.emailInput.doAfterTextChanged { text ->
            viewModel.onEvent(ForgotPasswordUiEvent.EmailChanged(text?.toString().orEmpty()))
        }
        binding.submitButton.setOnClickListener {
            viewModel.onEvent(ForgotPasswordUiEvent.SubmitClicked)
        }
        binding.loginText.setOnClickListener {
            viewModel.onEvent(ForgotPasswordUiEvent.LoginClicked)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (binding.emailInput.text?.toString() != state.email) {
                        binding.emailInput.setText(state.email)
                        binding.emailInput.setSelection(state.email.length)
                    }
                    binding.emailInput.error = state.emailError
                    binding.submitButton.isEnabled = !state.isLoading
                }
            }
        }
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        ForgotPasswordUiEffect.CloseScreen -> finish()
                        ForgotPasswordUiEffect.NavigateToLogin -> {
                            startActivity(
                                Intent(this@ForgotPasswordActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                            )
                            finish()
                        }
                        is ForgotPasswordUiEffect.NavigateToOtp -> {
                            startActivity(OtpVerificationActivity.createIntent(this@ForgotPasswordActivity, effect.email))
                        }
                    }
                }
            }
        }
    }
}
