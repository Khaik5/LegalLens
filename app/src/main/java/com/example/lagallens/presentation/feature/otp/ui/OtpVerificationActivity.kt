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
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.presentation.feature.resetpassword.ui.ResetPasswordActivity
import com.example.lagallens.databinding.ActivityOtpVerificationBinding
import com.example.lagallens.presentation.feature.success.ui.SuccessActivity
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEffect
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEvent
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiState
import com.example.lagallens.presentation.feature.otp.viewmodel.OtpVerificationViewModel
import com.example.lagallens.presentation.common.utils.MotionTokens
import com.example.lagallens.presentation.common.utils.animateContentEntrance
import com.example.lagallens.presentation.common.utils.bindPressFeedback
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {
    private val binding by lazy { ActivityOtpVerificationBinding.inflate(layoutInflater) }
    private val viewModel by viewModels<OtpVerificationViewModel> {
        val app = application as com.example.lagallens.LegalLensApplication
        OtpVerificationViewModel.Factory(app.verifyOtpUseCase, app.resendOtpUseCase)
    }
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
        playEntranceMotion()
        bindView()
        viewModel.onEvent(OtpVerificationUiEvent.Initialized(
            intent.getStringExtra(EXTRA_EMAIL).orEmpty(),
            OtpPurpose.valueOf(intent.getStringExtra(EXTRA_PURPOSE) ?: OtpPurpose.REGISTRATION.name),
            OtpChallenge(
                intent.getStringExtra(EXTRA_CHALLENGE_ID).orEmpty(),
                intent.getIntExtra(EXTRA_EXPIRES_IN, 0),
                intent.getIntExtra(EXTRA_RESEND_AFTER, 0)
            )
        ))
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
        binding.resendBlock.bindPressFeedback()
        binding.submitButton.bindPressFeedback()
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

    private fun playEntranceMotion() {
        binding.otpRoot.doOnPreDraw {
            val offset = resources.getDimension(R.dimen.dp_16)
            binding.headerLayout.animateContentEntrance(offset)
            binding.emailText.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.FAST_DURATION_MILLIS
            )
            otpInputs.forEachIndexed { index, input ->
                input.animateContentEntrance(
                    translationDistanceY = offset,
                    startDelayMillis = MotionTokens.FAST_DURATION_MILLIS * (index + 2)
                )
            }
            binding.footerLayout.animateContentEntrance(
                translationDistanceY = offset,
                startDelayMillis = MotionTokens.STANDARD_DURATION_MILLIS
            )
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
                    val shouldAnimateError = state.errorResId != null && !binding.errorMessage.isVisible
                    state.errorResId?.let(binding.otpErrorText::setText)
                    binding.errorMessage.isVisible = state.errorResId != null
                    binding.submitButton.isEnabled = !state.isVerifying && !state.isResending && !state.isVerified
                    if (shouldAnimateError) {
                        binding.errorMessage.doOnPreDraw {
                            binding.errorMessage.animateContentEntrance(resources.getDimension(R.dimen.dp_8))
                        }
                    }
                    binding.resendText.text = if (state.remainingSeconds > 0) {
                        getString(R.string.otp_resend_after, formatSeconds(state.remainingSeconds))
                    } else {
                        getString(R.string.otp_resend)
                    }
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
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEffect.collect { effect ->
                    when (effect) {
                        OtpVerificationUiEffect.CloseScreen -> finish()
                        OtpVerificationUiEffect.NavigateToSuccess -> {
                            startActivity(Intent(this@OtpVerificationActivity, SuccessActivity::class.java))
                            finish()
                        }
                        is OtpVerificationUiEffect.NavigateToResetPassword -> startActivity(
                            ResetPasswordActivity.createIntent(this@OtpVerificationActivity, effect.challengeId, effect.verificationToken)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_EMAIL = "extra_email"
        private const val EXTRA_PURPOSE = "extra_otp_purpose"
        private const val EXTRA_CHALLENGE_ID = "extra_challenge_id"
        private const val EXTRA_EXPIRES_IN = "extra_expires_in"
        private const val EXTRA_RESEND_AFTER = "extra_resend_after"

        fun createIntent(context: Context, email: String, purpose: OtpPurpose, challenge: OtpChallenge): Intent {
            return Intent(context, OtpVerificationActivity::class.java)
                .putExtra(EXTRA_EMAIL, email)
                .putExtra(EXTRA_PURPOSE, purpose.name)
                .putExtra(EXTRA_CHALLENGE_ID, challenge.challengeId)
                .putExtra(EXTRA_EXPIRES_IN, challenge.expiresInSeconds)
                .putExtra(EXTRA_RESEND_AFTER, challenge.resendAfterSeconds)
        }
    }
}
