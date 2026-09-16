package com.example.lagallens.presentation.feature.register.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityRegisterBinding
import com.example.lagallens.presentation.feature.login.ui.LoginActivity
import com.example.lagallens.presentation.feature.otp.ui.OtpVerificationActivity
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEffect
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEvent
import com.example.lagallens.presentation.feature.register.viewmodel.RegisterViewModel
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private val binding by lazy { ActivityRegisterBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<RegisterViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        bindView()
        bindText()
        collectState()
        collectEffect()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() {
        binding.backButton.setOnClickListener {
            viewModel.onEvent(RegisterUiEvent.BackClicked)
        }
        binding.submitButton.setOnClickListener {
            viewModel.onEvent(RegisterUiEvent.SubmitClicked)
        }
        binding.fullNameInput.doAfterTextChanged { text ->
            viewModel.onEvent(RegisterUiEvent.FullNameChanged(text?.toString().orEmpty()))
        }
        binding.emailInput.doAfterTextChanged { text ->
            viewModel.onEvent(RegisterUiEvent.EmailChanged(text?.toString().orEmpty()))
        }
        binding.passwordInput.doAfterTextChanged { text ->
            viewModel.onEvent(RegisterUiEvent.PasswordChanged(text?.toString().orEmpty()))
        }
        binding.confirmPasswordInput.doAfterTextChanged { text ->
            viewModel.onEvent(RegisterUiEvent.ConfirmPasswordChanged(text?.toString().orEmpty()))
        }
        binding.loginText.setOnClickListener {
            viewModel.onEvent(RegisterUiEvent.LoginClicked)
        }
    }

    private fun bindText() {
        binding.termsText.text = styleText(
            getString(R.string.register_terms),
            listOf(getString(R.string.terms_of_service), getString(R.string.privacy_policy))
        )
        binding.loginText.text = styleText(
            getString(R.string.register_login_prompt),
            listOf(getString(R.string.login))
        )
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateText(binding.fullNameInput, state.fullName)
                    if (binding.emailInput.text?.toString() != state.email) {
                        binding.emailInput.setText(state.email)
                        binding.emailInput.setSelection(state.email.length)
                    }
                    updateText(binding.passwordInput, state.password)
                    updateText(binding.confirmPasswordInput, state.confirmPassword)
                    binding.fullNameInput.error = state.fullNameError
                    binding.emailInput.error = state.emailError
                    binding.passwordInput.error = state.passwordError
                    binding.confirmPasswordInput.error = state.confirmPasswordError
                }
            }
        }
    }

    private fun updateText(input: android.widget.EditText, value: String) {
        if (input.text?.toString() != value) {
            input.setText(value)
            input.setSelection(value.length)
        }
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        RegisterUiEffect.CloseScreen -> finish()
                        RegisterUiEffect.NavigateToLogin -> {
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        }
                        is RegisterUiEffect.NavigateToOtp -> {
                            startActivity(OtpVerificationActivity.createIntent(this@RegisterActivity, effect.email))
                        }
                    }
                }
            }
        }
    }

    private fun styleText(text: String, highlights: List<String>): SpannableString {
        val spannable = SpannableString(text)
        val highlightColor = ContextCompat.getColor(this, R.color.legal_lens_link)
        highlights.forEach { highlight ->
            val start = text.indexOf(highlight)
            if (start >= 0) {
                val end = start + highlight.length
                spannable.setSpan(ForegroundColorSpan(highlightColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }
}
