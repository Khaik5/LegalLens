package com.example.lagallens.presentation.feature.ocr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
        viewModel.onEvent(OcrReviewEvent.ScreenOpened(
            intent.getStringExtra(EXTRA_DOCUMENT_ID).orEmpty(),
            intent.getStringExtra(EXTRA_CONTRACT_ID),
            intent.getStringExtra(EXTRA_DOCUMENT_TEXT).orEmpty()
        ))
        bindEvents()
        observeViewModel()
    }

    private fun bindEvents() = with(binding) {
        ocrReviewBack.setOnClickListener { viewModel.onEvent(OcrReviewEvent.BackClicked) }
        ocrReviewConfirm.setOnClickListener { viewModel.onEvent(OcrReviewEvent.ConfirmClicked) }
        ocrReviewDocumentText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onEvent(OcrReviewEvent.DocumentTextChanged(s?.toString().orEmpty()))
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
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
        if (ocrReviewDocumentText.text.toString() != state.documentText) {
            ocrReviewDocumentText.setText(state.documentText)
            ocrReviewDocumentText.setSelection(state.documentText.length)
        }
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
            is OcrReviewEffect.FinishWithConfirmedReview -> {
                setResult(RESULT_OK, Intent()
                    .putExtra(EXTRA_REVIEW_CONFIRMED, true)
                    .putExtra(EXTRA_DOCUMENT_ID, effect.documentId)
                    .putExtra(EXTRA_CONTRACT_ID, effect.contractId)
                    .putExtra(EXTRA_DOCUMENT_TEXT, effect.documentText))
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_REVIEW_CONFIRMED = "extra_review_confirmed"
        const val EXTRA_DOCUMENT_ID = "ocr_review_document_id"
        const val EXTRA_CONTRACT_ID = "ocr_review_contract_id"
        const val EXTRA_DOCUMENT_TEXT = "ocr_review_document_text"

        fun createIntent(context: Context, documentId: String = "", contractId: String? = null, documentText: String = ""): Intent =
            Intent(context, OcrReviewActivity::class.java)
                .putExtra(EXTRA_DOCUMENT_ID, documentId)
                .putExtra(EXTRA_CONTRACT_ID, contractId)
                .putExtra(EXTRA_DOCUMENT_TEXT, documentText)
    }
}
