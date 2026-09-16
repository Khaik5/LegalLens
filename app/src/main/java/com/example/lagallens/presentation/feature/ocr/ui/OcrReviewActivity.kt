package com.example.lagallens.presentation.feature.ocr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.style.StyleSpan
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.lagallens.R
import com.example.lagallens.databinding.ActivityOcrReviewBinding
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEffect
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEvent
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewUiState
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewWarning
import com.example.lagallens.presentation.feature.ocr.viewmodel.OcrReviewViewModel
import kotlinx.coroutines.launch

class OcrReviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOcrReviewBinding
    private val viewModel by viewModels<OcrReviewViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityOcrReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.ocrReviewRoot) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.ocrReviewRoot)
        bindEvents()
        observeViewModel()
    }

    private fun bindEvents() = with(binding) {
        ocrReviewBack.setOnClickListener { viewModel.onEvent(OcrReviewEvent.BackClicked) }
        ocrReviewConfirm.setOnClickListener { viewModel.onEvent(OcrReviewEvent.ConfirmClicked) }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.effects.collect(::handleEffect) }
            }
        }
    }

    private fun render(state: OcrReviewUiState) = with(binding) {
        state.warnings.getOrNull(0)?.let { warning ->
            renderWarning(
                warning,
                ocrReviewWarningOneSection,
                ocrReviewWarningOneConfidence,
                ocrReviewWarningOneRecognized,
                ocrReviewWarningOneSource
            )
        }
        state.warnings.getOrNull(1)?.let { warning ->
            renderWarning(
                warning,
                ocrReviewWarningTwoSection,
                ocrReviewWarningTwoConfidence,
                ocrReviewWarningTwoRecognized,
                ocrReviewWarningTwoSource
            )
        }
    }

    private fun renderWarning(
        warning: OcrReviewWarning,
        section: android.widget.TextView,
        confidence: android.widget.TextView,
        recognized: android.widget.TextView,
        source: android.widget.TextView
    ) {
        section.setText(warning.sectionRes)
        confidence.setText(warning.confidenceRes)
        val recognizedValue = getString(warning.recognizedValueRes)
        val renderedRecognized = getString(R.string.ocr_review_recognized_text, recognizedValue)
        recognized.text = Spannable.Factory.getInstance().newSpannable(renderedRecognized).apply {
            setSpan(
                StyleSpan(android.graphics.Typeface.BOLD),
                renderedRecognized.length - recognizedValue.length,
                renderedRecognized.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        source.setText(warning.sourceRes)
    }

    private fun handleEffect(effect: OcrReviewEffect) {
        when (effect) {
            OcrReviewEffect.NavigateBack -> finish()
            OcrReviewEffect.FinishWithConfirmedReview -> {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_REVIEW_CONFIRMED, true))
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_REVIEW_CONFIRMED = "extra_review_confirmed"

        fun createIntent(context: Context): Intent = Intent(context, OcrReviewActivity::class.java)
    }
}
