package com.example.lagallens.presentation.feature.forgotpassword

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityForgotPasswordBinding
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
            viewModel.onEvent(ForgotPasswordContract.Event.BackClicked)
        }
        binding.emailInput.doAfterTextChanged { text ->
            viewModel.onEvent(ForgotPasswordContract.Event.EmailChanged(text?.toString().orEmpty()))
        }
        binding.submitButton.setOnClickListener {
            viewModel.onEvent(ForgotPasswordContract.Event.SubmitClicked)
        }
        binding.loginText.setOnClickListener {
            viewModel.onEvent(ForgotPasswordContract.Event.LoginClicked)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (binding.emailInput.text?.toString() != state.email) {
                        binding.emailInput.setText(state.email)
                        binding.emailInput.setSelection(state.email.length)
                    }
                    binding.submitButton.isEnabled = !state.isLoading
                }
            }
        }
    }

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        ForgotPasswordContract.Effect.CloseScreen -> finish()
                        ForgotPasswordContract.Effect.ShowResetLinkComingSoon -> {
                            Toast.makeText(
                                this@ForgotPasswordActivity,
                                getString(R.string.reset_password_coming_soon),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
