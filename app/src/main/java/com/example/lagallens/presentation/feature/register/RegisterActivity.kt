package com.example.lagallens.presentation.feature.register

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityRegisterBinding
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
        collectEffect()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun bindView() {
        binding.backButton.setOnClickListener {
            viewModel.onEvent(RegisterContract.Event.BackClicked)
        }
        binding.submitButton.setOnClickListener {
            viewModel.onEvent(RegisterContract.Event.SubmitClicked)
        }
        binding.loginText.setOnClickListener {
            viewModel.onEvent(RegisterContract.Event.LoginClicked)
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

    private fun collectEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        RegisterContract.Effect.CloseScreen -> finish()
                        RegisterContract.Effect.ShowRegisterComingSoon -> {
                            Toast.makeText(this@RegisterActivity, getString(R.string.register_coming_soon), Toast.LENGTH_SHORT).show()
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
