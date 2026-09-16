package com.example.lagallens.presentation.feature.login.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.text.method.SingleLineTransformationMethod
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
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.databinding.ActivityLoginBinding
import com.example.lagallens.presentation.feature.forgotpassword.ui.ForgotPasswordActivity
import com.example.lagallens.presentation.feature.register.ui.RegisterActivity
import com.example.lagallens.presentation.feature.main.ui.MainActivity
import com.example.lagallens.presentation.feature.login.contract.LoginUiEffect
import com.example.lagallens.presentation.feature.login.contract.LoginUiEvent
import com.example.lagallens.presentation.feature.login.contract.LoginUiState
import com.example.lagallens.presentation.feature.login.viewmodel.LoginViewModel
import com.example.lagallens.presentation.feature.login.viewmodel.LoginViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<LoginViewModel> {
        LoginViewModelFactory(
            (application as LegalLensApplication).authenticateUserUseCase
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        bindView()
        bindText()
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
            viewModel.onEvent(LoginUiEvent.BackClicked)
        }
        binding.emailInput.doAfterTextChanged { text ->
            viewModel.onEvent(LoginUiEvent.EmailChanged(text?.toString().orEmpty()))
        }
        binding.passwordInput.doAfterTextChanged { text ->
            viewModel.onEvent(LoginUiEvent.PasswordChanged(text?.toString().orEmpty()))
        }
        binding.passwordVisibilityText.setOnClickListener {
            viewModel.onEvent(LoginUiEvent.PasswordVisibilityClicked)
        }
        binding.forgotPasswordText.setOnClickListener {
            viewModel.onEvent(LoginUiEvent.ForgotPasswordClicked)
        }
        binding.submitButton.setOnClickListener {
            viewModel.onEvent(LoginUiEvent.SubmitClicked)
        }
        binding.registerText.setOnClickListener {
            viewModel.onEvent(LoginUiEvent.RegisterClicked)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateTextInputs(state)
                    updatePasswordVisibility(state)
                    binding.emailInput.error = state.usernameError
                    binding.passwordInput.error = state.passwordError
                    binding.submitButton.isEnabled = !state.isLoading
                }
            }
        }
    }

    private fun bindText() {
        val text = getString(R.string.login_register_prompt)
        val highlight = getString(R.string.register_now)
        val start = text.indexOf(highlight)
        binding.registerText.text = SpannableString(text).apply {
            if (start >= 0) {
                val end = start + highlight.length
                val accentColor = ContextCompat.getColor(this@LoginActivity, R.color.legal_lens_auth_accent)
                setSpan(ForegroundColorSpan(accentColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun updateTextInputs(state: LoginUiState) {
        if (binding.emailInput.text?.toString() != state.email) {
            binding.emailInput.setText(state.email)
            binding.emailInput.setSelection(state.email.length)
        }
        if (binding.passwordInput.text?.toString() != state.password) {
            binding.passwordInput.setText(state.password)
            binding.passwordInput.setSelection(state.password.length)
        }
    }

    private fun updatePasswordVisibility(state: LoginUiState) {
        val selection = binding.passwordInput.selectionStart.coerceAtLeast(0)
        binding.passwordInput.transformationMethod = if (state.isPasswordVisible) {
            SingleLineTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        binding.passwordInput.inputType = if (state.isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.passwordInput.setSelection(selection.coerceAtMost(state.password.length))
        binding.passwordVisibilityText.text = getString(
            if (state.isPasswordVisible) R.string.hide_password else R.string.show_password
        )
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        LoginUiEffect.CloseScreen -> finish()
                        LoginUiEffect.NavigateToForgotPassword -> {
                            startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
                        }
                        LoginUiEffect.NavigateToRegister -> {
                            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                        }
                        LoginUiEffect.NavigateToMain -> {
                            startActivity(
                                Intent(this@LoginActivity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
