package com.example.lagallens.presentation.feature.otp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOtpVerificationBinding
import com.example.lagallens.presentation.feature.success.ui.SuccessActivity
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEffect
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEvent
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiState
import com.example.lagallens.presentation.feature.otp.viewmodel.OtpVerificationViewModel
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOtpVerificationBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<OtpVerificationViewModel>()
    private val otpInputs by lazy {
        listOf(
            binding.otpInput1,
            binding.otpInput2,
            binding.otpInput3,
            binding.otpInput4,
            binding.otpInput5,
            binding.otpInput6
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        bindView()
        viewModel.onEvent(OtpVerificationUiEvent.Initialized(intent.getStringExtra(EXTRA_EMAIL).orEmpty()))
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
            viewModel.onEvent(OtpVerificationUiEvent.BackClicked)
        }
        otpInputs.forEachIndexed(::bindOtpInput)
        binding.resendBlock.setOnClickListener {
            viewModel.onEvent(OtpVerificationUiEvent.ResendClicked)
        }
        binding.submitButton.setOnClickListener {
            viewModel.onEvent(OtpVerificationUiEvent.SubmitClicked)
        }
    }

    private fun bindOtpInput(index: Int, input: EditText) {
        input.doAfterTextChanged { text ->
            val digit = text?.toString().orEmpty()
            viewModel.onEvent(OtpVerificationUiEvent.DigitChanged(index, digit))
            if (digit.isNotEmpty() && index < otpInputs.lastIndex) {
                otpInputs[index + 1].requestFocus()
            }
        }
        input.setOnFocusChangeListener { _, _ -> updateOtpInputBackgrounds() }
        input.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN && input.text.isEmpty() && index > 0) {
                otpInputs[index - 1].requestFocus()
            }
            false
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.emailText.text = state.email.ifBlank { getString(R.string.email_hint) }
                    renderOtpInputs(state)
                    binding.errorMessage.isVisible = state.hasError
                    binding.resendText.text = getString(
                        if (state.isResendAvailable) R.string.otp_resend else R.string.otp_resend_after,
                        formatSeconds(state.remainingSeconds)
                    )
                    binding.resendBlock.isEnabled = state.isResendAvailable
                    binding.resendBlock.alpha = if (state.isResendAvailable) 1f else 0.8f
                }
            }
        }
    }

    private fun renderOtpInputs(state: OtpVerificationUiState) {
        otpInputs.forEachIndexed { index, input ->
            if (input.text?.toString() != state.digits[index]) {
                input.setText(state.digits[index])
                input.setSelection(state.digits[index].length)
            }
        }
        updateOtpInputBackgrounds()
    }

    private fun updateOtpInputBackgrounds() {
        otpInputs.forEach { input ->
            input.setBackgroundResource(
                if (input.isFocused) R.drawable.bg_otp_box_focused else R.drawable.bg_otp_box
            )
        }
    }

    private fun formatSeconds(seconds: Int): String {
        return String.format("00:%02d", seconds)
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        OtpVerificationUiEffect.CloseScreen -> finish()
                        OtpVerificationUiEffect.NavigateToSuccess -> {
                            startActivity(Intent(this@OtpVerificationActivity, SuccessActivity::class.java))
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_EMAIL = "extra_email"

        fun createIntent(context: Context, email: String): Intent {
            return Intent(context, OtpVerificationActivity::class.java).putExtra(EXTRA_EMAIL, email)
        }
    }
}
